var fs = require("fs");
var _ = require("underscore");
var lodash = require("lodash");
var diacritics = require("diacritics");

var oSettings = require("../settings");

var sDataDirectory = "data";

function run(bUpdate, dataRef, aSelectedLang) {
    var oActiveData = {};
    var aCategory = [];
    var aTag = [];
    var mPrimaryRef = {};
    aSelectedLang = aSelectedLang || [];

    return Promise.resolve().then(() => {
        if (bUpdate) {
            return dataRef.once("value").then((snapshot) => {
                oActiveData = snapshot.val() || {};
            });
        }
    }).then(() => {
        return _.reduce(oSettings.languages, (oPromise, sLangCode) => {
            return oPromise.then(() => {
                if (!_.isEmpty(aSelectedLang) && !_.contains(aSelectedLang, sLangCode)) {
                    return;
                }

                var activeCategoryRef = dataRef && dataRef.child(`${sLangCode}/active/categories`);
                var activeTagRef = dataRef && dataRef.child(`${sLangCode}/active/tags`);
                var sFile = `content.${sLangCode}.txt`;
                var sFilepath = `./${sDataDirectory}/${oSettings.space}/${sFile}`;
                if (fs.existsSync(sFilepath)) {
                    var sContent = fs.readFileSync(sFilepath, "utf8");
                    var aCategoryEntry = sContent.split(/\r?\n/);

                    return _.reduce(aCategoryEntry, (oPromise, sCategoryEntry, iCategoryIndex) => {
                        return oPromise.then(() => {
                            if (!sCategoryEntry.trim()) {
                                return;
                            }
                            var aPart = sCategoryEntry.split(":");
                            var sCategory = aPart[0];
                            var sCategoryName = sCategory.substr(0, sCategory.indexOf("(")).trim();
                            var bFavorite = false;
                            if (lodash.endsWith(sCategoryName, "*")) {
                                bFavorite = true;
                                sCategoryName = sCategoryName.slice(0, -1);
                            }
                            var sCategoryColor = sCategory.substr(sCategory.indexOf("(") + 1, 7).trim();
                            if (sCategoryColor === ")") {
                                sCategoryColor = "";
                            }

                            var oCategoryContent = {
                                name: sCategoryName,
                                language: sLangCode,
                                search: searchNormalized(sCategoryName),
                                color: sCategoryColor,
                                icon: sCategoryName.replace(/[^a-zA-Z0-9]/, "_").toLowerCase(),
                                order: iCategoryIndex + 1,
                                favorite: bFavorite,
                                ignore: false,
                                primaryLangKey: ""
                            };

                            if (sLangCode === oSettings.primaryLanguage) {
                                mPrimaryRef[`c-${iCategoryIndex}`] = _.clone(oCategoryContent);
                            } else {
                                if (!mPrimaryRef[`c-${iCategoryIndex}`]) {
                                    return Promise.reject(`No primary ref category found for entry (${iCategoryIndex + 1})`)
                                }
                                if (!oCategoryContent.color) {
                                    oCategoryContent.color = mPrimaryRef[`c-${iCategoryIndex}`].color;
                                }
                                oCategoryContent.icon = mPrimaryRef[`c-${iCategoryIndex}`].icon;
                                oCategoryContent.primaryLangKey = mPrimaryRef[`c-${iCategoryIndex}`].key;
                            }

                            return Promise.resolve().then(() => {
                                if (bUpdate) {
                                    var oActiveCategory = _.find(oActiveData[sLangCode] && oActiveData[sLangCode].active && oActiveData[sLangCode].active.categories || [], (oCategory, sKey) => {
                                        oCategory.key = sKey;
                                        return oCategory.name === sCategoryName;
                                    });
                                    var oCategory = oActiveCategory ? activeCategoryRef.child(oActiveCategory.key) : activeCategoryRef.push();
                                    return oCategory.update(oCategoryContent).then(() => {
                                        oCategoryContent.key = oCategory.key;
                                        oCategoryContent.new = !oActiveCategory;
                                        console.log(`${oActiveCategory ? "" : "*"}C: ${oCategoryContent.name} (${sLangCode})`);
                                    });
                                } else {
                                    oCategoryContent.key = `${sLangCode}-c-${iCategoryIndex}`;
                                }
                            }).then(() => {
                                if (sLangCode === oSettings.primaryLanguage) {
                                    mPrimaryRef[`c-${iCategoryIndex}`].key = oCategoryContent.key;
                                }
                            }).then(() => {
                                aCategory.push(oCategoryContent);

                                var sTags = aPart[1];
                                var aTagEntry = sTags.split(",");

                                return Promise.all(_.map(aTagEntry, (sTagEntry, iTagIndex) => {
                                    if (!sTagEntry.trim()) {
                                        return;
                                    }

                                    var sTagName = sTagEntry.trim();
                                    var bFavorite = false;
                                    if (lodash.endsWith(sTagName, "*")) {
                                        bFavorite = true;
                                        sTagName = sTagName.slice(0, -1);
                                    }

                                    var oTagContent = {
                                        name: sTagName,
                                        language: sLangCode,
                                        search: searchNormalized(sTagName),
                                        categoryKey: oCategoryContent.key,
                                        favorite: bFavorite,
                                        ignore: false,
                                        primaryLangKey: ""
                                    };

                                    if (sLangCode === oSettings.primaryLanguage) {
                                        mPrimaryRef[`t-${iCategoryIndex}-${iTagIndex}`] = _.clone(oTagContent);
                                    } else {
                                        if (!mPrimaryRef[`t-${iCategoryIndex}-${iTagIndex}`]) {
                                            return Promise.reject(`No primary ref tag found for entry (${iCategoryIndex + 1}, ${iTagIndex + 1})`);
                                        }
                                        oTagContent.primaryLangKey = mPrimaryRef[`t-${iCategoryIndex}-${iTagIndex}`].key;
                                    }

                                    return Promise.resolve().then(() => {
                                        if (bUpdate) {
                                            var oActiveTag = _.find(oActiveData[sLangCode] && oActiveData[sLangCode].active && oActiveData[sLangCode].active.tags || [], (oTag, sKey) => {
                                                oTag.key = sKey;
                                                return oTag.name === sTagName && oTag.categoryKey === oCategoryContent.key;
                                            });
                                            var oTag = oActiveTag ? activeTagRef.child(oActiveTag.key) : activeTagRef.push();
                                            return oTag.update(oTagContent).then(() => {
                                                oTagContent.key = oTag.key;
                                                oTagContent.new = !oActiveTag;
                                                console.log(`${oActiveTag ? "" : "*"}T: ${oTagContent.name} (${sLangCode})`);
                                            });
                                        } else {
                                            oTagContent.key = `${sLangCode}-t-${iCategoryIndex}-${iTagIndex}`;
                                        }
                                    }).then(() => {
                                        if (sLangCode === oSettings.primaryLanguage) {
                                            if (!mPrimaryRef[`t-${iCategoryIndex}-${iTagIndex}`]) {
                                                return Promise.reject(`No primary ref tag found for entry (${iCategoryIndex + 1}, ${iTagIndex + 1})`);
                                            }
                                            mPrimaryRef[`t-${iCategoryIndex}-${iTagIndex}`].key = oTagContent.key;
                                        }
                                        aTag.push(oTagContent);
                                    });
                                }));
                            });
                        });
                    }, Promise.resolve());
                }
            });
        }, Promise.resolve()).then(() => {
            return {
                categories: aCategory,
                tags: aTag
            }
        });
    });
}

function searchNormalized(text) {
    var searchNormalized = text.toLowerCase();
    searchNormalized = diacritics.remove(searchNormalized);
    searchNormalized = searchNormalized.replace(/\s+/gi, "");
    searchNormalized = searchNormalized.replace(/(.)\1+/gi, "$1");
    searchNormalized = searchNormalized.trim();
    if (searchNormalized !== "") {
        return searchNormalized;
    }
    return text.trim().toLowerCase();
}

module.exports.run = run;