var _ = require("underscore");

var oSettings = require("../settings");
var oConnection = require("../connection");

Promise.resolve().then(() => {
    return oConnection.open();
}).then((db) => {
    return Promise.resolve().then(() => {
        return Promise.all(_.map(oSettings.languages, (sLanguage) => {
            var activeTagRef = db.ref(`${oSettings.space}/data/${sLanguage}/active/tags`);
            var activeCategoryRef = db.ref(`${oSettings.space}/data/${sLanguage}/active/categories`);
            var activeTagQuery = activeTagRef.orderByChild("categoryStaged").equalTo(true);

            return activeTagQuery.once("value").then((snapshot) => {
                return Promise.all(_.map(snapshot.val() || {}, (oTag, sTagKey) => {
                    return activeCategoryRef.child(oTag.categoryKey).once("value").then((snapshot) => {
                        var bActive = snapshot.exists();
                        console.log(bActive ? "[X]" : "[ ]", sTagKey, `'${oTag.name}' (${oTag.language})`);
                        if (bActive) {
                            return activeTagRef.child(sTagKey).child("categoryStaged").remove();
                        }
                    });
                }));
            });
        }))
    }).then(() => {
        console.log("\nDone");
        if (db) {
            db.goOffline();
        }
    })
}).catch((oError) => {
    console.log(oError);
});