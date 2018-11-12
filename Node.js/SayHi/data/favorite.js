var _ = require("underscore");

var oSettings = require("../settings");
var oConnection = require("../connection");

Promise.resolve().then(() => {
    return oConnection.open();
}).then((db) => {
    return Promise.resolve().then(() => {
        var aCategory = [];
        var aTag = [];

        var activeCategoryRef = db.ref(`${oSettings.space}/data/${oSettings.primaryLanguage}/active/categories`);
        var activeCategoryQuery = activeCategoryRef.orderByChild("favorite").equalTo(true);
        var activeTagRef = db.ref(`${oSettings.space}/data/${oSettings.primaryLanguage}/active/tags`);
        var activeTagQuery = activeTagRef.orderByChild("favorite").equalTo(true);

        return activeCategoryQuery.once("value").then((snapshot) => {
            return Promise.all(_.map(snapshot.val() || {}, (oCategory, sCategoryKey) => {
                return Promise.all(_.map(oSettings.languages, (sLanguage) => {
                    var activeCategoryLangRef = db.ref(`${oSettings.space}/data/${sLanguage}/active/categories`);
                    var activeCategoryLangQuery = activeCategoryLangRef.orderByChild("primaryLangKey").equalTo(sCategoryKey);
                    return activeCategoryLangQuery.once("value").then((snapshot) => {
                        return Promise.all(_.map(snapshot.val() || {}, (oLangCategory, sLangCategoryKey) => {
                            var oCategory = {
                                key: sLangCategoryKey,
                                name: oLangCategory.name,
                                language: sLanguage,
                                status: " "
                            };
                            aCategory.push(oCategory);
                            if (oLangCategory.favorite !== false && oLangCategory.favorite !== true) {
                                oCategory.status = "X";
                                return activeCategoryLangRef.child(sLangCategoryKey).update({
                                    favorite: true
                                });
                            } else {
                                oCategory.status = oLangCategory.favorite ? " " : "*";
                            }
                        }));
                    });
                }));
            }));
        }).then(() => {
            return activeTagQuery.once("value").then((snapshot) => {
                return Promise.all(_.map(snapshot.val() || {}, (oTag, sTagKey) => {
                    return Promise.all(_.map(oSettings.languages, (sLanguage) => {
                        var activeTagLangRef = db.ref(`${oSettings.space}/data/${sLanguage}/active/tags`);
                        var activeTagLangQuery = activeTagLangRef.orderByChild("primaryLangKey").equalTo(sTagKey);
                        return activeTagLangQuery.once("value").then((snapshot) => {
                            return Promise.all(_.map(snapshot.val() || {}, (oLangTag, sLangTagKey) => {
                                var oTag = {
                                    key: sLangTagKey,
                                    name: oLangTag.name,
                                    language: sLanguage,
                                    status: " "
                                };
                                aTag.push(oTag);
                                if (oLangTag.favorite !== false && oLangTag.favorite !== true) {
                                    oTag.status = "X";
                                    return activeTagLangRef.child(sLangTagKey).update({
                                        favorite: true
                                    });
                                } else {
                                    oTag.status = oLangTag.favorite ? " " : "*";
                                }
                            }));
                        });
                    }));
                }));
            });
        }).then(() => {
            console.log("Categories:\n");
            _.each(aCategory, (oCategory) => {
                console.log(`[${oCategory.status}]`, oCategory.key, `'${oCategory.name}' (${oCategory.language})`);
            });
            console.log("\nTags:\n");
            _.each(aTag, (oTag) => {
                console.log(`[${oTag.status}]`, oTag.key, `'${oTag.name}' (${oTag.language})`);
            });
        });
    }).then(() => {
        console.log("\nDone");
        if (db) {
            db.goOffline();
        }
    });
}).catch((error) => {
    console.log(error);
});