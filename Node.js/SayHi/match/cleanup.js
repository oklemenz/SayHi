var _ = require("underscore");

var oSettings = require("../settings");
var oConnection = require("../connection");

Promise.resolve().then(() => {
    return oConnection.open();
}).then((db) => {
    return Promise.resolve().then(() => {
        var iRefDate = Date.now() - 24 * 60 * 60 * 1000; // Cleanup older than a day
        var messagesRef = db.ref(`${oSettings.space}/messages`);
        var messagesQuery = messagesRef.orderByChild("date").endAt(iRefDate);
        var matchesRef = db.ref(`${oSettings.space}/matches`);
        var matchesQuery = matchesRef.orderByChild("date").endAt(iRefDate);

        return messagesQuery.once("value").then((snapshot) => {
            return Promise.all(_.map(snapshot.val() || {}, (oMessage, sMessageKey) => {
                return messagesRef.child(sMessageKey).remove();
            }));
        }).then(() => {
            return matchesQuery.once("value").then((snapshot) => {
                return Promise.all(_.map(snapshot.val() || {}, (oMatch, sMatchKey) => {
                    return matchesRef.child(sMatchKey).remove();
                }));
            });
        });
    }).then(() => {
        console.log("Done");
        if (db) {
            db.goOffline();
        }
    })
}).catch((error) => {
    console.log(error);
});