var fs = require("fs");
var path = require("path");
var _ = require("underscore");

var oSettings = require("../settings");
var oConnection = require("../connection");

var sDataDirectory = "data";
var sIconStageDirectory = `./${sDataDirectory}/${oSettings.space}/icons/stage`;
var sIconActiveDirectory = `./${sDataDirectory}/${oSettings.space}/icons/active`;

Promise.resolve().then(() => {
    return oConnection.open();
}).then((db) => {
    var iconRef = db.ref(`${oSettings.space}/icons`);
    var aFile = fs.readdirSync(sIconStageDirectory);

    return Promise.all(_.map(aFile, (sFile) => {
        var sFilepath = `${sIconStageDirectory}/${sFile}`;
        if (fs.existsSync(sFilepath)) {
            var oPath = path.parse(sFile);
            if (oPath.ext === ".png") {
                var icon = fs.readFileSync(sFilepath);
                var base64Data = new Buffer(icon).toString("base64");
                return iconRef.child(oPath.name).update({
                    data: base64Data,
                    date: oConnection.TIMESTAMP
                }).then(() => {
                    fs.renameSync(sFilepath, `./${sIconActiveDirectory}/${sFile}`);
                });
            }
        }
    })).then(() => {
        console.log("Done");
        if (db) {
            db.goOffline();
        }
    });
}).catch((oError) => {
    console.log(oError);
});