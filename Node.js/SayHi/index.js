var firebase = require("firebase");

var config = require("./config.json");
var oSettings = require("./settings.json");

process.env.HTTP_PROXY = "";
process.env.HTTPS_PROXY = "";
process.env.http_proxy = "";
process.env.https_proxy = "";

firebase.initializeApp(config);
var ref = firebase.database().ref();

firebase.auth().signInAnonymously().then((oResult) => {
    console.log(oResult.uid);

    return ref.child(`${oSettings.space}/settings`).once("value").then((snapshot) => {
        console.log(snapshot.val());
    }).then(() => {
        var query = ref.child(`${oSettings.space}/data/${oSettings.primaryLanguage}/stage/tags`).orderByChild("hash").equalTo("#T:Test:#C:Test");
        query.once("value").then((snapshot) => {
            console.log(snapshot.val());
        });
    });
}).catch((error) => {
    console.log(oError);
});