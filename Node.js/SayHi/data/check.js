var _ = require("underscore");

var Data = require("./_data");

var oSettings = require("../settings");
var oConnection = require("../connection");

var aSelectedLang = _.compact((process.argv[2] || "").split(","));
if (!_.isEmpty(aSelectedLang)) {
    aSelectedLang.splice(0, 0, oSettings.primaryLanguage);
}

var oActiveData = {};

Promise.resolve().then(() => {
    return oConnection.open();
}).then((db) => {

    var iMarkCount = 0;
    var dataRef = db.ref(`${oSettings.space}/data`);
    return dataRef.once("value").then((snapshot) => {
        oActiveData = snapshot.val() || {};
    }).then(() => {
        return Data.run(false, undefined, aSelectedLang).then((oResult) => {

            var mCategory = {};
            var mTag = {};

            _.each(oSettings.languages, (sLangCode) => {
                if (!_.isEmpty(aSelectedLang) && !_.contains(aSelectedLang, sLangCode)) {
                    return;
                }

                _.each(oResult.categories, (oCategory) => {
                    mCategory[oCategory.key] = oCategory;
                });
                _.each(oResult.tags, (oTag) => {
                    mTag[oTag.key] = oTag;
                });
            });

            _.each(oSettings.languages, (sLangCode) => {
                if (!_.isEmpty(aSelectedLang) && !_.contains(aSelectedLang, sLangCode)) {
                    return;
                }

                var aCategory = [];
                var aTag = [];
                var mCategoryTag = {};
                var mTagCount = {};

                _.each(oResult.categories, (oCategory) => {
                    if (oCategory.language === sLangCode) {
                        aCategory.push(oCategory);
                    }
                });

                _.each(oResult.tags, (oTag) => {
                    if (oTag.language === sLangCode) {
                        aTag.push(oTag);
                        if (!mCategoryTag[oTag.categoryKey]) {
                            mCategoryTag[oTag.categoryKey] = [];
                        }
                        mCategoryTag[oTag.categoryKey].push(oTag);
                    }
                });

                if (oActiveData[sLangCode]) {
                    _.each(oActiveData[sLangCode].active.tags, (oActiveTag, sActiveTagKey) => {
                        if (oActiveTag.primaryLangKey) {
                            var oActiveTagCategory = oActiveData[sLangCode].active.categories[oActiveTag.categoryKey];
                            var oActivePrimaryLangTag = oActiveData[oSettings.primaryLanguage].active.tags[oActiveTag.primaryLangKey];
                            var oTag = _.find(aTag, (oTag) => {
                                var oCategory = mCategory[oTag.categoryKey];
                                return oTag.name === oActiveTag.name &&
                                    oCategory.name === oActiveTagCategory.name &&
                                    oTag.language === sLangCode;
                            });
                            if (oTag) {
                                var oPrimaryLangTag = mTag[oTag.primaryLangKey];
                                var sMark = oActiveTag.name !== oTag.name || oActivePrimaryLangTag.name !== oPrimaryLangTag.name;
                                if (sMark) {
                                    iMarkCount++;
                                }
                                console.log(`${sMark ? "* " : ""}${oActiveTag.name} -> ${oActivePrimaryLangTag.name} : ${oTag.name} -> ${oPrimaryLangTag.name}`);
                            } else {
                                console.log(`!${oActiveTag.name} -> ${oActivePrimaryLangTag.name} (${sActiveTagKey})`);
                            }
                        }
                    });
                }
            });
        });
    }).then(() => {
        if (iMarkCount === 0) {
            console.log("\n0 issues found");
        } else {
            console.log(`${iMarkCount} issue(s) found`);
        }
        console.log("\nDone");
        if (db) {
            db.goOffline();
        }
    });
}).catch((oError) => {
    console.log(oError);
});