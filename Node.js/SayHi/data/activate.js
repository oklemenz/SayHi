var _ = require("underscore");

var Common = require("../util/common");

var oSettings = require("../settings");
var oConnection = require("../connection");

var aActivateKey = _.compact((process.argv[2] || "").split(","));

Promise.resolve().then(() => {
    return oConnection.open();
}).then((db) => {
    return Promise.resolve().then(() => {
        var aCategory = [];
        var mActiveCategory = {};
        var aTag = [];
        var mActiveTag = {};

        return Promise.all(_.map(oSettings.languages, (sLanguage) => {
            var stageCategoryRef = db.ref(`${oSettings.space}/data/${sLanguage}/stage/categories`);
            var stageCategoryQuery = stageCategoryRef.orderByChild("counter").startAt(oSettings.categoryActiveThreshold);
            var stageTagRef = db.ref(`${oSettings.space}/data/${sLanguage}/stage/tags`);
            var stageTagQuery = stageTagRef.orderByChild("counter").startAt(oSettings.tagActiveThreshold);

            return stageCategoryQuery.once("value").then((snapshot) => {
                return Promise.all(_.map(snapshot.val() || {}, (oStageCategory, sCategoryKey) => {
                    return activateCategory(oStageCategory, sCategoryKey, sLanguage);
                }));
            }).then(() => {
                return stageTagQuery.once("value").then((snapshot) => {
                    return Promise.all(_.map(snapshot.val() || {}, (oStageTag, sTagKey) => {
                        return activateTag(oStageTag, sTagKey, sLanguage);
                    }));
                })
            }).then(() => {
                return Promise.all(_.map(aActivateKey, (sActivateKey) => {
                    if (!mActiveCategory[sActivateKey]) {
                        return stageCategoryRef.child(sActivateKey).once("value").then((snapshot) => {
                            if (snapshot.exists()) {
                                var oStageCategory = snapshot.val();
                                return activateCategory(oStageCategory, sActivateKey, sLanguage);
                            }
                        });
                    }
                    if (!mActiveTag[sActivateKey]) {
                        return stageTagRef.child(sActivateKey).once("value").then((snapshot) => {
                            if (snapshot.exists()) {
                                var oStageTag = snapshot.val();
                                return activateTag(oStageTag, sActivateKey, sLanguage);
                            }
                        });
                    }
                }));
            });
        })).then(() => {
            console.log("Categories:\n");
            _.each(aCategory, (oCategory) => {
                var duration = Common.durationToString(oCategory.duration);
                console.log(oCategory.active ? "[X]" : "[ ]", oCategory.key, `'${oCategory.name}'(${oCategory.language})`, "# " + oCategory.counter, " Δ " + duration, "- '" + oCategory.info + "'" || "");
            });
            console.log("\nTags:\n");
            _.each(aTag, (oTag) => {
                var duration = Common.durationToString(oTag.duration);
                console.log(oTag.active ? "[X]" : "[ ]", oTag.key, `'${oTag.name}'(${oTag.language})`, "# " + oTag.counter, " Δ " + duration, oTag.info ? "- '" + oTag.info + "'" : "");
            });
        });

        function activateCategory(oStageCategory, sCategoryKey, sLanguage) {
            var stageCategoryRef = db.ref(`${oSettings.space}/data/${sLanguage}/stage/categories`);
            var stagePrimaryLangCategoryRef = db.ref(`${oSettings.space}/data/${oSettings.primaryLanguage}/stage/categories`);
            var activeCategoryRef = db.ref(`${oSettings.space}/data/${sLanguage}/active/categories`);
            var activeCategoryQuery = activeCategoryRef.orderByChild("name").equalTo(oStageCategory.name);
            var activePrimaryLangCategoryRef = db.ref(`${oSettings.space}/data/${oSettings.primaryLanguage}/active/categories`);
            var activeTagRef = db.ref(`${oSettings.space}/data/${sLanguage}/active/tags`);

            var oCategory = _.find(aCategory, (oCategory) => {
                return oCategory.key === sCategoryKey
            });
            var bActivate = (!oCategory || !oCategory.active) && _.contains(aActivateKey, sCategoryKey);
            if (!oCategory) {
                oCategory = {
                    key: sCategoryKey,
                    name: oStageCategory.name,
                    search: oStageCategory.search,
                    language: sLanguage,
                    counter: oStageCategory.counter,
                    primary: sLanguage === oSettings.primaryLanguage || !!oStageCategory.primaryLangKey,
                    duration: new Date(oStageCategory.changedAt) - new Date(oStageCategory.createdAt)
                };
                if (!oCategory.primary) {
                    oCategory.info = 'Primary language reference missing';
                }
                aCategory.push(oCategory);
            }
            if (bActivate) {
                oCategory.active = true;
                var oActiveCategory = {
                    name: oStageCategory.name,
                    language: sLanguage,
                    search: oStageCategory.search,
                    color: null,
                    icon: oStageCategory.icon || "tag",
                    order: 0,
                    favorite: false,
                    ignore: false
                };
                if (oStageCategory.primaryLangKey) {
                    oActiveCategory.primaryLangKey = oStageCategory.primaryLangKey;
                } else {
                    oActiveCategory.primaryLangKey = "";
                }
                if (!oCategory.primary) {
                    return Promise.resolve();
                }
                return Promise.resolve().then(() => {
                    if (oActiveCategory.primaryLangKey) {
                        return activePrimaryLangCategoryRef.child(oActiveCategory.primaryLangKey).once("value").then((snapshot) => {
                            if (snapshot.exists()) {
                                var oActivePrimaryLangCategory = snapshot.val();
                                oActiveCategory.color = oActivePrimaryLangCategory.color;
                                if (oActivePrimaryLangCategory.favorite) {
                                    oActiveCategory.favorite = true;
                                }
                                return true
                            }
                            return false
                        }).then((bActivePrimaryLangCategoryExists) => {
                            if (!bActivePrimaryLangCategoryExists) {
                                return stagePrimaryLangCategoryRef.child(oActiveCategory.primaryLangKey).once("value").then((snapshot) => {
                                    if (snapshot.exists()) {
                                        aActivateKey.push(snapshot.key);
                                        return activateCategory(snapshot.val(), snapshot.key, oSettings.primaryLanguage).then((oActivePrimaryLangCategory) => {
                                            oActiveCategory.color = oActivePrimaryLangCategory.color;
                                            if (oActivePrimaryLangCategory.favorite) {
                                                oActiveCategory.favorite = true;
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                }).then(() => {
                    if (!oActiveCategory.color) {
                        oActiveCategory.color = Common.randomPastelColor();
                    }
                    return activeCategoryRef.child(sCategoryKey).update(oActiveCategory).then(() => {
                        return stageCategoryRef.child(sCategoryKey).remove();
                    }).then(() => {
                        var activeTagQuery = activeTagRef.orderByChild("categoryKey").equalTo(sCategoryKey);
                        return activeTagQuery.once("value").then((snapshot) => {
                            return Promise.all(_.map(snapshot.val() || {}, (oActiveTag, sTagKey) => {
                                if (oActiveTag.categoryStaged) {
                                    return activeTagRef.child(sTagKey).child("categoryStaged").remove();
                                }
                            }));
                        });
                    });
                }).then(() => {
                    mActiveCategory[sCategoryKey] = oActiveCategory;
                    return oActiveCategory;
                });
            } else {
                return activeCategoryQuery.once("value").then((snapshot) => {
                    if (snapshot.exists()) {
                        oCategory.info = "Active Category with same name already exists: " + _.keys(snapshot.val());
                    }
                    return Promise.resolve(mActiveCategory[sCategoryKey]);
                });
            }
        }

        function activateTag(oStageTag, sTagKey, sLanguage) {
            var stageTagRef = db.ref(`${oSettings.space}/data/${sLanguage}/stage/tags`);
            var stagePrimaryLangTagRef = db.ref(`${oSettings.space}/data/${oSettings.primaryLanguage}/stage/tags`);
            var activeTagRef = db.ref(`${oSettings.space}/data/${sLanguage}/active/tags`);
            var activeTagQuery = activeTagRef.orderByChild("name").equalTo(oStageTag.name);
            var activePrimaryLangTagRef = db.ref(`${oSettings.space}/data/${oSettings.primaryLanguage}/active/tags`);
            var activeCategoryRef = db.ref(`${oSettings.space}/data/${sLanguage}/active/categories`);

            var oTag = _.find(aTag, (oTag) => {
                return oTag.key === sTagKey
            });
            var bActivate = (!oTag || !oTag.active) && _.contains(aActivateKey, sTagKey);
            if (!oTag) {
                oTag = {
                    key: sTagKey,
                    name: oStageTag.name,
                    search: oStageTag.search,
                    language: sLanguage,
                    categoryKey: oStageTag.categoryKey,
                    counter: oStageTag.counter,
                    primary: sLanguage === oSettings.primaryLanguage || !!oStageTag.primaryLangKey,
                    duration: new Date(oStageTag.changedAt) - new Date(oStageTag.createdAt)
                };
                if (!oTag.primary) {
                    oTag.info = 'Primary language reference missing';
                }
                aTag.push(oTag);
            }
            if (bActivate) {
                oTag.active = true;
                var oActiveTag = {
                    name: oStageTag.name,
                    language: sLanguage,
                    search: oStageTag.search,
                    categoryKey: oStageTag.categoryKey,
                    favorite: false,
                    ignore: false,

                };
                if (oStageTag.primaryLangKey) {
                    oActiveTag.primaryLangKey = oStageTag.primaryLangKey;
                } else {
                    oActiveTag.primaryLangKey = "";
                }
                if (!oTag.primary) {
                    return Promise.resolve();
                }
                return Promise.resolve().then(() => {
                    if (oActiveTag.primaryLangKey) {
                        return activePrimaryLangTagRef.child(oActiveTag.primaryLangKey).once("value").then((snapshot) => {
                            if (snapshot.exists()) {
                                var oActivePrimaryLangTag = snapshot.val();
                                if (oActivePrimaryLangTag.favorite) {
                                    oActiveTag.favorite = true;
                                }
                                return true
                            }
                            return false
                        }).then((bActivePrimaryLangTagExists) => {
                            if (!bActivePrimaryLangTagExists) {
                                return stagePrimaryLangTagRef.child(oActiveTag.primaryLangKey).once("value").then((snapshot) => {
                                    if (snapshot.exists()) {
                                        aActivateKey.push(snapshot.key);
                                        return activateTag(snapshot.val(), snapshot.key, oSettings.primaryLanguage).then((oActivePrimaryLangTag) => {
                                            if (oActivePrimaryLangTag.favorite) {
                                                oActiveTag.favorite = true;
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                }).then(() => {
                    return activeCategoryRef.child(oStageTag.categoryKey).once("value").then((snapshot) => {
                        if (!snapshot.exists()) {
                            oActiveTag.categoryStaged = true;
                        }
                    }).then(() => {
                        return activeTagRef.child(sTagKey).update(oActiveTag).then(() => {
                            return stageTagRef.child(sTagKey).remove();
                        });
                    });
                }).then(() => {
                    mActiveTag[sTagKey] = oActiveTag;
                    return oActiveTag;
                });
            } else {
                return activeTagQuery.once("value").then((snapshot) => {
                    if (snapshot.exists()) {
                        var sActiveTagKey;
                        var sActiveTagKeySameCategory;
                        snapshot.forEach((snapshot) => {
                            sActiveTagKey = snapshot.key;
                            if (snapshot.val().categoryKey === oTag.categoryKey) {
                                sActiveTagKeySameCategory = snapshot.key;
                            }
                        });
                        if (sActiveTagKeySameCategory) {
                            oTag.info = "Active Tag with same name/category already exists: " + sActiveTagKeySameCategory;
                        } else if (sActiveTagKey) {
                            oTag.info = "Active Tag with same name already exists: " + sActiveTagKey;
                        }
                    }
                    return Promise.resolve(mActiveTag[sTagKey]);
                });
            }
        }
    }).then(() => {
        console.log("\nDone");
        if (db) {
            db.goOffline();
        }
    })
}).catch((error) => {
    console.log(error);
});