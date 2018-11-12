var fs = require("fs");
var _ = require("underscore");

var sAnalyticsDirectory = "analytics";
var sStageDirectory = "stage";

var oSettings = require("../settings");
var oConnection = require("../connection");

var sCluster = process.argv[2];
if (sCluster) {

    Promise.resolve().then(() => {
        return oConnection.open();
    }).then((db) => {

        return Promise.resolve().then(() => {
            var dataRef = db.ref(`${oSettings.space}/data`);
            var analyticsRef = db.ref(`${oSettings.space}/analytics`);
            return Promise.resolve().then(() => {
                if (sCluster === "data") {
                    return dataRef.once("value").then((snapshot) => {
                        if (snapshot) {
                            var aCategory = [];
                            var aTag = [];
                            _.each(snapshot.val() || {}, (oData, sLangCode) => {
                                aCategory = _.union(aCategory, _.reduce(oData.active && oData.active.categories, (aCategory, oCategory, sKey) => {
                                    oCategory.active = true;
                                    oCategory.key = sKey;
                                    oCategory.space = oSettings.space;
                                    oCategory.language = sLangCode;
                                    aCategory.push(oCategory);
                                    return aCategory;
                                }, []));
                                aCategory = _.union(aCategory, _.reduce(oData.stage && oData.stage.categories, (aCategory, oCategory, sKey) => {
                                    oCategory.active = false;
                                    oCategory.key = sKey;
                                    oCategory.space = oSettings.space;
                                    oCategory.language = sLangCode;
                                    aCategory.push(oCategory);
                                    return aCategory;
                                }, []));
                                aTag = _.union(aTag, _.reduce(oData.active && oData.active.tags, (aTag, oTag, sKey) => {
                                    oTag.active = true;
                                    oTag.key = sKey;
                                    oTag.space = oSettings.space;
                                    oTag.language = sLangCode;
                                    aTag.push(oTag);
                                    return aTag;
                                }, []));
                                aTag = _.union(aTag, _.reduce(oData.stage && oData.stage.tags, (aTag, oTag, sKey) => {
                                    oTag.active = false;
                                    oTag.key = sKey;
                                    oTag.space = oSettings.space;
                                    oTag.language = sLangCode;
                                    aTag.push(oTag);
                                    return aTag;
                                }, []));
                            });
                            var sDirPath = `./${sAnalyticsDirectory}/${sStageDirectory}/${oSettings.space}`;
                            if (!fs.existsSync(sDirPath)) {
                                fs.mkdirSync(sDirPath);
                            }
                            fs.writeFileSync(`${sDirPath}/categories.json`, JSON.stringify(aCategory, null, 2));
                            fs.writeFileSync(`${sDirPath}/tags.json`, JSON.stringify(aTag, null, 2));
                        }
                    });
                } else {
                    var analyticsQuery;
                    if (sCluster === "all") {
                        analyticsQuery = analyticsRef;
                    } else if (String(parseInt(sCluster)) === sCluster) {
                        analyticsQuery = analyticsRef.orderByChild("year").equalTo(parseInt(sCluster));
                    } else {
                        analyticsQuery = analyticsRef.orderByChild("cluster").equalTo(sCluster);
                    }
                    if (analyticsQuery) {
                        return analyticsQuery.once("value").then((snapshot) => {
                            var aAnalytic = _.reduce(snapshot.val() || {}, (aAnalytic, oAnalytic, sKey) => {
                                oAnalytic.key = sKey;
                                oAnalytic.space = oSettings.space;
                                aAnalytic.push(oAnalytic);
                                return aAnalytic;
                            }, []);
                            if (snapshot) {
                                var sDirPath = `./${sAnalyticsDirectory}/${sStageDirectory}/${oSettings.space}`;
                                if (!fs.existsSync(sDirPath)) {
                                    fs.mkdirSync(sDirPath);
                                }
                                fs.writeFileSync(`${sDirPath}/${sCluster.replace("/", "_")}.json`, JSON.stringify(aAnalytic, null, 2));
                            }
                        });
                    }
                }
            });
        }).then(() => {
            console.log("Done");
            if (db) {
                db.goOffline();
            }
        });
    }).catch((oError) => {
        console.log(oError);
    });
} else {
    console.log("Specify: 'data', 'all', <year>, <cluster>)");
}