var db = require("./db");

db().then(() => {
    console.log("Done");
    db.close();
}).catch((oError) => {
    console.log(oError);
    db.close();
});