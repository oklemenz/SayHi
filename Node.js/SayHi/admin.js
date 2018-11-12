var admin = require("firebase-admin");

process.env.HTTP_PROXY = "";
process.env.HTTPS_PROXY = "";
process.env.http_proxy = "";
process.env.https_proxy = "";

admin.initializeApp({
    credential: admin.credential.cert("serviceAccountKey.json"),
    databaseURL: "https://sayhi-12315.firebaseio.com",
    databaseAuthVariableOverride: {
        uid: "admin"
    }
});

module.exports = admin;