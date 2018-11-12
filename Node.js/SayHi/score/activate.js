var fs = require("fs");
var _ = require("underscore");
var db = require("../db");

var oSettings = require("../settings");

var sScoresDirectory = "scores";
var sStageDirectory = "stage";
var sActiveDirectory = "active";

Promise.resolve().then(() => {
    return db().then(() => {
        var sDirPath = `./${sScoresDirectory}/${sStageDirectory}/${oSettings.space}`;
        var aFile = fs.readdirSync(sDirPath);
        return Promise.all(_.map(aFile, (sFile) => {
            var sFilepath = `${sDirPath}/${sFile}`;
            return Promise.resolve().then(() => {
                var aContent = require(`./${sStageDirectory}/${oSettings.space}/${sFile}`);
                if (sFile === "scores.json") {
                    return Promise.all(_.map(aContent, (oContent) => {
                        return Promise.all([
                            jsonInsert(db, oContent, "scores_json", oSettings.space),
                            flatInsert(db, oContent, "scores", oSettings.space)
                        ]);
                    }));
                }
            }).then(() => {
                var sDirPath = `./${sScoresDirectory}/${sActiveDirectory}/${oSettings.space}`;
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
            VALUES ($1, $2, $3) ON CONFLICT (key, space) 
            DO UPDATE SET data = $3 
            WHERE ${sTable}.key = $1 AND ${sTable}.space = $2;`,
        oContent.key, sSpace, oContent);
}

function flatInsert(db, oContent, sTable, sSpace) {
    return db.run(
        `INSERT INTO ${sTable} (key, space, alias, count, value) 
            VALUES ($1, $2, $3, $4, $5) ON CONFLICT (key, space) 
            DO UPDATE SET alias = $3, count = $4, value = $5 
            WHERE ${sTable}.key = $1 AND ${sTable}.space = $2;`,
        oContent.key, sSpace, oContent.alias, oContent.count, oContent.value);
}
