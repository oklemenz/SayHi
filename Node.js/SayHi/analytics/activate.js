var fs = require("fs");
var _ = require("underscore");
var db = require("../db");
var lodash = require("lodash");

var oSettings = require("../settings");

var sAnalyticsDirectory = "analytics";
var sStageDirectory = "stage";
var sActiveDirectory = "active";

Promise.resolve().then(() => {
    return db().then(() => {
        var sDirPath = `./${sAnalyticsDirectory}/${sStageDirectory}/${oSettings.space}`;
        var aFile = fs.readdirSync(sDirPath);
        return Promise.all(_.map(aFile, (sFile) => {
            var sFilepath = `${sDirPath}/${sFile}`;
            return Promise.resolve().then(() => {
                var aContent = require(`./${sStageDirectory}/${oSettings.space}/${sFile}`);
                if (sFile === "categories.json") {
                    return Promise.all(_.map(aContent, (oContent) => {
                        return Promise.all([
                            jsonInsert(db, oContent, "categories_json", oSettings.space),
                            flatInsert(db, oContent, "categories", oSettings.space)
                        ]);
                    }));
                } else if (sFile === "tags.json") {
                    return Promise.all(_.map(aContent, (oContent) => {
                        return Promise.all([
                            jsonInsert(db, oContent, "tags_json", oSettings.space),
                            flatInsert(db, oContent, "tags", oSettings.space)
                        ]);
                    }));
                } else {
                    return Promise.all(_.map(aContent, (oContent) => {
                        return Promise.all(_.map(transformAnalyticsContent(oContent), (oContent) => {
                            return Promise.all([
                                jsonInsert(db, oContent, "analytics_json", oSettings.space),
                                flatInsert(db, oContent, "analytics", oSettings.space)
                            ]);
                        }));
                    }));
                }
            }).then(() => {
                var sDirPath = `./${sAnalyticsDirectory}/${sActiveDirectory}/${oSettings.space}`;
                if (!fs.existsSync(sDirPath)) {
                    fs.mkdirSync(sDirPath);
                }
                fs.renameSync(sFilepath, `${sDirPath}/${sFile}`);
            });
        }));
    }).then(() => {
        console.log("Done");
        db.close();
    }).catch((oError) => {
        console.log(oError);
        db.close();
    });
});

function jsonInsert(db, oContent, sTable, sSpace) {
    return db.run(
        `INSERT INTO ${sTable} (key, space, data) 
            VALUES ($1, $2, $3) ON CONFLICT (key) 
            DO UPDATE SET data = $3 
            WHERE ${sTable}.key = $1;`,
        oContent.key, sSpace, oContent);
}

function flatInsert(db, oContent, sTable, sSpace) {
    oContent = transformDataContent(oContent, sTable);
    var aColumn = _.keys(oContent);
    var aParameter = [];
    var aParameterValue = [];
    _.map(aColumn, (sParameter) => {
        if (sParameter !== "key" && sParameter !== "space") {
            aParameter.push(`"${lodash.snakeCase(sParameter)}"`);
            aParameterValue.push(oContent[sParameter]);
        }
    });
    var aAllParameter = [`"key"`, `"space"`].concat(aParameter);
    var aAllParameterValue = [oContent.key, sSpace].concat(aParameterValue);
    var sSQL = `INSERT INTO ${sTable} (`;
    sSQL += aAllParameter.join(", ");
    sSQL += `) VALUES (`;
    sSQL += _.map(lodash.range(aAllParameter.length), (iIndex) => {
        return `$${1 + iIndex}`;
    }, "").join(", ");
    sSQL += ") ON CONFLICT (key) DO UPDATE SET ";
    sSQL += _.map(aParameter, (sParameter, iIndex) => {
        return `${sParameter} = $${1 + 2 + iIndex}`;
    }).join(", ");
    sSQL += ` WHERE ${sTable}.key = $1`;
    sSQL += ";";
    return db.query(sSQL, aAllParameterValue).catch((oError) => {
        console.log(sSQL);
        console.log(oError);
        console.log(aAllParameterValue);
        process.exit();
    });
}

function transformDataContent(oContent, sTable) {
    if (sTable === "categories") {
        if (oContent.createdAt) {
            oContent.createdAt = new Date(oContent.createdAt);
        }
        if (oContent.changedAt) {
            oContent.changedAt = new Date(oContent.changedAt);
        }
    } else if (sTable === "tags") {
        if (oContent.createdAt) {
            oContent.createdAt = new Date(oContent.createdAt);
        }
        if (oContent.changedAt) {
            oContent.changedAt = new Date(oContent.changedAt);
        }
    } else if (sTable === "analytics") {
        if (oContent.date) {
            oContent.date = new Date(oContent.date);
        }
        if (oContent.matchDate) {
            oContent.matchDate = new Date(oContent.matchDate);
        }
        if (!oContent.locationLatitude) {
            oContent.locationLatitude = "0";
        }
        if (!oContent.locationLongitude) {
            oContent.locationLongitude = "0";
        }
    }
    return oContent;
}

function transformAnalyticsContent(oContent) {
    var aContent = [];
    aContent.push(oContent);
    if (oContent.event === "match") {
        aContent = _.union(aContent, transformAnalyticsMatch(oContent));
    }
    return aContent;
}

function transformAnalyticsMatch(oMatch) {
    var aContent = [];
    if (oMatch.event === "match") {
        if (oMatch.matchLeftLeftTagKeys) {
            _.each(oMatch.matchLeftLeftTagKeys.split(","), (sTagKey) => {
                var oContent = _.clone(oMatch);
                oContent.key = oMatch.key + "/" + sTagKey + "/leftLeft";
                oContent.parentKey = oMatch.key;
                oContent.event = "match_tag";
                oContent.matchTagKey = sTagKey;
                oContent.matchSide = "leftLeft";
                aContent.push(oContent);
            });
        }
        if (oMatch.matchRightRightTagKeys) {
            _.each(oMatch.matchRightRightTagKeys.split(","), (sTagKey) => {
                var oContent = _.clone(oMatch);
                oContent.key = oMatch.key + "/" + sTagKey + "/rightRight";
                oContent.parentKey = oMatch.key;
                oContent.event = "match_tag";
                oContent.matchTagKey = sTagKey;
                oContent.matchSide = "rightRight";
                aContent.push(oContent);
            });
        }
        if (oMatch.matchLeftRightTagKeys) {
            _.each(oMatch.matchLeftRightTagKeys.split(","), (sTagKey) => {
                var oContent = _.clone(oMatch);
                oContent.key = oMatch.key + "/" + sTagKey + "/leftRight";
                oContent.parentKey = oMatch.key;
                oContent.event = "match_tag";
                oContent.matchTagKey = sTagKey;
                oContent.matchSide = "leftRight";
                aContent.push(oContent);
            });
        }
        if (oMatch.matchRightLeftTagKeys) {
            _.each(oMatch.matchRightLeftTagKeys.split(","), (sTagKey) => {
                var oContent = _.clone(oMatch);
                oContent.key = oMatch.key + "/" + sTagKey + "/rightLeft";
                oContent.parentKey = oMatch.key;
                oContent.event = "match_tag";
                oContent.matchTagKey = sTagKey;
                oContent.matchSide = "rightLeft";
                aContent.push(oContent);
            });
        }
    }
    return aContent;
}