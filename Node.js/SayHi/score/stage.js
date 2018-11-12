var fs = require("fs");
var _ = require("underscore");

var sScoresDirectory = "scores";
var sStageDirectory = "stage";

var oSettings = require("../settings");
var oConnection = require("../connection");

Promise.resolve().then(() => {
    return oConnection.open();
}).then((db) => {
    return Promise.resolve().then(() => {
        var scoresRef = db.ref(`${oSettings.space}/scores`);
        return Promise.resolve().then(() => {
            return scoresRef.once("value").then((snapshot) => {
                var aScore = _.reduce(snapshot.val() || {}, (aScore, oScore, sKey) => {
                    oScore.key = sKey;
                    aScore.push(oScore);
                    return aScore;
                }, []);
                if (snapshot) {
                    var sDirPath = `./${sScoresDirectory}/${sStageDirectory}/${oSettings.space}`;
                    if (!fs.existsSync(sDirPath)) {
                        fs.mkdirSync(sDirPath);
                    }
                    fs.writeFileSync(`${sDirPath}/scores.json`, JSON.stringify(aScore, null, 2));
                }
            });
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