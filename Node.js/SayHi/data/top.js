var _ = require("underscore");

var Common = require("../util/common");

var oSettings = require("../settings");
var oConnection = require("../connection");

Promise.resolve().then(() => {
    return oConnection.open();
}).then((db) => {
    return Promise.resolve().then(() => {
        return Promise.all(_.map(oSettings.languages, (sLanguage) => {
            var aCategory = [];
            var aTag = [];

            var stageCategoryRef = db.ref(`${oSettings.space}/data/${sLanguage}/stage/categories`);
            var stageCategoryQuery = stageCategoryRef.orderByChild("counter").limitToLast(oSettings.queryLimit);
            var stageTagRef = db.ref(`${oSettings.space}/data/${sLanguage}/stage/tags`);
            var stageTagQuery = stageTagRef.orderByChild("counter").limitToLast(oSettings.queryLimit);

            return stageCategoryQuery.once("value").then((snapshot) => {
                return Promise.all(_.map(snapshot.val() || {}, (oStageCategory, sCategoryKey) => {
                    oStageCategory.key = sCategoryKey;
                    aCategory.push(oStageCategory);
                }));
            }).then(() => {
                return stageTagQuery.once("value").then((snapshot) => {
                    return Promise.all(_.map(snapshot.val() || {}, (oStageTag, sTagKey) => {
                        oStageTag.key = sTagKey;
                        aTag.push(oStageTag);
                    }));
                })
            }).then(() => {
                aCategory = _.sortBy(aCategory, (oCategory) => {
                    return -oCategory.counter;
                });
                aTag = _.sortBy(aTag, (oTag) => {
                    return -oTag.counter;
                });

                console.log("[Language: " + sLanguage + "]\n");

                console.log("Categories:\n");
                _.each(aCategory, (oCategory) => {
                    var duration = Common.durationToString(new Date(oCategory.changedAt) - new Date(oCategory.createdAt));
                    console.log("# " + oCategory.counter, oCategory.key, `'${oCategory.name}'`, "Δ: " + duration);
                });
                console.log("\nTags:\n");
                _.each(aTag, (oTag) => {
                    var duration = Common.durationToString(new Date(oTag.changedAt) - new Date(oTag.createdAt));
                    console.log("# " + oTag.counter, oTag.key, `'${oTag.name}'`, "Δ: " + duration);
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