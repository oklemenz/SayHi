var fs = require("fs");
var _ = require("underscore");

var sScoresDirectory = "scores";

var oSettings = require("../settings");
var oConnection = require("../connection");

var sSpace = process.argv[2];

Promise.resolve().then(() => {
    if (sSpace !== oSettings.space) {
        return Promise.reject("Confirm clearing by providing space name!");
    }
    return oConnection.open();
}).then((db) => {
    return Promise.resolve().then(() => {
        var scoresRef = db.ref(`${oSettings.space}/${sScoresDirectory}`);
        return scoresRef.remove();
    }).then(() => {
        console.log("Done");
        if (db) {
            db.goOffline();
        }
    });
}).catch((oError) => {
    console.log(oError);
});