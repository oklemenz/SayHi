var fs = require("fs");
var path = require("path");
var _ = require("underscore");

var oConnection = require("../connection");
var Data = require("./_data");

var oSettings = require("../settings");

var sDataDirectory = "data";

var bUpdate = (process.argv[2] || "").trim() === "true";

Promise.resolve().then(() => {
    return oConnection.open();
}).then((db) => {
    var metaRef = db.ref(`${oSettings.space}/meta`);
    var protectionRef = db.ref(`${oSettings.space}/protection`);
    var settingsRef = db.ref(`${oSettings.space}/settings`);
    var dataRef = db.ref(`${oSettings.space}/data`);

    return Promise.resolve().then(() => {
        var oMeta;
        try {
            oMeta = require(`./${oSettings.space}/meta`);
        } catch (oException) {
        }
        if (oMeta) {
            return metaRef.update(oMeta).then(() => {
                console.log("Meta set");
            });
        }
    }).then(() => {
        var oProtection;
        try {
            oProtection = require(`./${oSettings.space}/protection`);
        } catch (oException) {
        }
        if (oProtection) {
            return protectionRef.update(oProtection).then(() => {
                console.log("Protection set");
            });
        }
    }).then(() => {
        var oDefaults = require(`./${oSettings.space}/settings`);
        if (oDefaults) {
            var sAssetsPath = `./${sDataDirectory}/${oSettings.space}/assets`;
            if (fs.existsSync(sAssetsPath)) {
                var aAsset = fs.readdirSync(sAssetsPath);
                _.each(aAsset, (sAsset) => {
                    var oPath = path.parse(sAsset);
                    if (oPath.ext === ".png" || oPath.ext === ".jpg") {
                        addFile(oDefaults, oPath.name, `${sAssetsPath}/${sAsset}`);
                    }
                });
            }
            return settingsRef.update(oDefaults).then(() => {
                console.log("Default settings set");
            });
        }
    }).then(() => {
        return Data.run(bUpdate, dataRef, []).then((oResult) => {
            if (bUpdate) {
                console.log("\nCategories:");
                console.log(_.countBy(oResult.categories, (oCategory) => {
                    return oCategory.new ? "created" : "updated";
                }));
                console.log("\nTags:");
                console.log(_.countBy(oResult.tags, (oTag) => {
                    return oTag.new ? "created" : "updated";
                }));
                console.log("\n");
                console.log(`Default data set: ${oResult.categories.length} categories, ${oResult.tags.length} tags`);
            } else {
                console.log(`Default data processed: ${oResult.categories.length} categories, ${oResult.tags.length} tags`);
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

function addFile(oData, sName, sPath) {
    if (fs.existsSync(sPath)) {
        var fileData = fs.readFileSync(sPath);
        var base64Data = new Buffer(fileData).toString("base64");
        oData[sName] = base64Data;
    }
}