var _ = require("underscore");

var Common = require("../util/common");

var oSettings = require("../settings");
var oConnection = require("../connection");

var aKeyParam = _.compact((process.argv[2] || "").split(","));
var oKeyPair = _.reduce(aKeyParam, (oResult, sKeyParam) => {
    var aKey = sKeyParam.split("=");
    oResult[aKey[0].trim()] = aKey[1].trim();
    return oResult;
}, {});

Promise.resolve().then(() => {
    return oConnection.open();
}).then((db) => {
    return Promise.resolve().then(() => {
        return Promise.all(_.map(oSettings.languages, (sLanguage) => {
            if (sLanguage === oSettings.primaryLanguage) {
                return;
            }
            var aCategory = [];
            var aTag = [];

            var stageCategoryRef = db.ref(`${oSettings.space}/data/${sLanguage}/stage/categories`);
            var stageCategoryQuery = stageCategoryRef.orderByChild("primaryLangKey").equalTo("");
            var stageTagRef = db.ref(`${oSettings.space}/data/${sLanguage}/stage/tags`);
            var stageTagQuery = stageTagRef.orderByChild("primaryLangKey").equalTo("");

            return stageCategoryQuery.once("value").then((snapshot) => {
                return Promise.all(_.map(snapshot.val() || {}, (oStageCategory, sCategoryKey) => {
                    oStageCategory.key = sCategoryKey;
                    aCategory.push(oStageCategory);
                    if (oKeyPair[oStageCategory.key]) {
                        return stageCategoryRef.child(oStageCategory.key).update({
                            primaryLangKey: oKeyPair[oStageCategory.key],
                            hash: `#T:${oStageCategory.name}#P:${oKeyPair[oStageCategory.key]}`
                        }).then(() => {
                            oStageCategory.updated = true;
                        });
                    }
                }));
            }).then(() => {
                return stageTagQuery.once("value").then((snapshot) => {
                    return Promise.all(_.map(snapshot.val() || {}, (oStageTag, sTagKey) => {
                        oStageTag.key = sTagKey;
                        aTag.push(oStageTag);
                        if (oKeyPair[oStageTag.key]) {
                            return stageTagRef.child(oStageTag.key).update({
                                primaryLangKey: oKeyPair[oStageTag.key],
                                hash: `#T:${oStageTag.name}#C:${oStageTag.categoryKey}#P:${oKeyPair[oStageTag.key]}`
                            }).then(() => {
                                oStageTag.updated = true;
                            });
                        }
                    }));
                })
            }).then(() => {
                console.log("[Language: " + sLanguage + "]\n");

                console.log("Categories:\n");
                _.each(aCategory, (oCategory) => {
                    var duration = Common.durationToString(new Date(oCategory.changedAt) - new Date(oCategory.createdAt));
                    console.log(oCategory.updated ? "[X]" : "[ ]" + " " + oCategory.key, `'${oCategory.name}'`, "Δ: " + duration);
                });
                console.log("\nTags:\n");
                _.each(aTag, (oTag) => {
                    var duration = Common.durationToString(new Date(oTag.changedAt) - new Date(oTag.createdAt));
                    console.log(oTag.updated ? "[X]" : "[ ]" + " " + oTag.key, `'${oTag.name}'`, "Δ: " + duration);
                });

                console.log("\n");
            });
        }));
    }).then(() => {
        console.log("Done");
        if (db) {
            db.goOffline();
        }
    })
}).catch((error) => {
    console.log(error);
});