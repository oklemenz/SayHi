var _ = require("underscore");

var Data = require("./_data");

var oSettings = require("../settings");

var aSelectedLang = _.compact((process.argv[2] || "").split(","));
if (!_.isEmpty(aSelectedLang)) {
    aSelectedLang.splice(0, 0, oSettings.primaryLanguage);
}

Promise.resolve().then(() => {
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


            console.log(`Language ${sLangCode}\n`);

            console.log(`Reference\n\n`);

            _.each(aTag, (oTag) => {
                if (oTag.primaryLangKey) {
                    var oPrimaryLangTag = mTag[oTag.primaryLangKey];
                    console.log((oTag.favorite !== oPrimaryLangTag.favorite ? "* " : "") + `${oTag.name} -> ${oPrimaryLangTag.name}`);
                }
            });

            console.log(`\n\n`);
            console.log(`Category Tag Count\n`);

            _.each(mCategoryTag, (aTag, sCategoryKey) => {
                var oCategory = mCategory[sCategoryKey];
                console.log(`${oCategory.name}: ${aTag.length}`);
            });

            console.log(`\n\n`);
            console.log(`Duplicates\n`);

            _.each(aTag, (oTag) => {
                _.each(aTag, (oOtherTag) => {
                    if (oTag.name === oOtherTag.name) {
                        if (!mTagCount[oTag.name]) {
                            mTagCount[oTag.name] = [];
                        }
                        var oCategory = mCategory[oTag.categoryKey];
                        mTagCount[oTag.name].push(oCategory.name);
                    }
                });
            });

            _.each(mTagCount, (aCategoryName, sTagName) => {
                var iCount = aCategoryName.length;
                if (iCount > 3) {
                    var sMark = Math.sqrt(iCount) !== _.uniq(aCategoryName).length ? "* " : "";
                    console.log(`${sMark}${sTagName}: ${Math.sqrt(iCount)} - ${_.uniq(aCategoryName)}`);
                }
            });

            console.log(`\n\n\n`);
        });
    });
});