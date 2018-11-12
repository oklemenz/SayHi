var firebase = require("firebase");

var oConfig = require("./config.json");
var oAdmin = require("./admin");

var oSettings = require("./settings");

process.env.HTTP_PROXY = "";
process.env.HTTPS_PROXY = "";
process.env.http_proxy = "";
process.env.https_proxy = "";

function open() {
    var db;
    return Promise.resolve().then(() => {
        if (oSettings.admin) {
            db = oAdmin.database();
        } else {
            firebase.initializeApp(oConfig);
            db = firebase.database();
            if (oSettings.anonymously) {
                return firebase.auth().signInAnonymously();
            } else {
                return firebase.auth().createUserWithEmailAndPassword(oSettings.username, oSettings.password).catch((oError) => {
                    if (oError.code === "auth/email-already-in-use") {
                        return firebase.auth().signInWithEmailAndPassword(oSettings.username, oSettings.password);
                    }
                    return Promise.reject(oError);
                });
            }
        }
    }).then(() => {
        return Promise.resolve(db);
    }).catch((oError) => {
        console.log(oError);
        if (db) {
            db.goOffline();
        }
        return Promise.reject(oError);
    });
}

module.exports.open = open;
module.exports.TIMESTAMP = firebase.database.ServerValue.TIMESTAMP;