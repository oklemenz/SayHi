var mongoose = require('mongoose');

var statisticsSchema = new mongoose.Schema({
    type: String,
    userID: String,
    userName: String,
    ip: String,
    companyID: {type: String},
    relationedID: {type: String},
    relationedName: {type: String},
    action: {type: String}, //'create', 'update', 'delete'
    createdOn: { type: Date, default: Date.now },
    createdBy: {type: String}
}, { collection: 'wst_Statistics' });

statisticsSchema.statics.save = function(req, data, done){

        var companyID = req.user.companyID;


        var statistic = {
            type: data.type,
            relationedID: data.relationedID,
            relationedName: data.relationedName,
            action: data.action,
            companyID: companyID,
            userID : (req.isAuthenticated()) ? req.user._id : null,
            userName : (req.isAuthenticated()) ? req.user.userName : null,
            ip : req.headers['x-forwarded-for'] || req.connection.remoteAddress,
            createdBy:  (req.isAuthenticated()) ? req.user._id : null
        };


    this.create(statistic, function(err, statistic){
        if(err) throw err;

        if (typeof done != 'undefined') done({result: 1, msg: "Statistic created", statistic: statistic.toObject()});
    });
}

var statistics = connection.model('statistics', statisticsSchema);
module.exports = statistics;
