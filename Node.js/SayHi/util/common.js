function durationToString(milliseconds) {
    var seconds = milliseconds / 1000;
    var years = Math.floor(seconds / 31536000);
    var days = Math.floor((seconds % 31536000) / 86400);
    var hours = Math.floor(((seconds % 31536000) % 86400) / 3600);
    var minutes = Math.floor((((seconds % 31536000) % 86400) % 3600) / 60);
    seconds = (((seconds % 31536000) % 86400) % 3600) % 60;
    var sResult = "";
    if (years > 0) {
        sResult += ` ${Math.round(years)} years`;
    }
    if (days > 0) {
        sResult += ` ${Math.round(days)} days`;
    }
    if (hours > 0) {
        sResult += ` ${Math.round(hours)} hours`;
    }
    if (minutes > 0) {
        sResult += ` ${Math.round(minutes)} minutes`;
    }
    if (seconds >= 0) {
        sResult += ` ${Math.round(seconds)} seconds`;
    }
    return sResult.trim();
}

var _ = require("underscore");

function randomPastelColor() {
    var r = (Math.round(Math.random() * 127) + 127).toString(16);
    var g = (Math.round(Math.random() * 127) + 127).toString(16);
    var b = (Math.round(Math.random() * 127) + 127).toString(16);
    return '#' + r + g + b;
}

function randomColor() {
    var letters = '0123456789abcdef';
    var color = '#';
    for (var i = 0; i < 6; i++) {
        color += letters[Math.floor(Math.random() * letters.length)];
    }
    return color;
}

module.exports.durationToString = durationToString;
module.exports.randomPastelColor = randomPastelColor;
module.exports.randomColor = randomColor;