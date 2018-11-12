var _ = require("underscore");
var pg = require("pg");

var pool = new pg.Pool({
    host: "localhost",
    port: 5432,
    user: "postgres",
    password: "postgres",
    database: "sayhi",
    max: 10
});

var db;
var dbDone;

function connect() {
    if (db) {
        return Promise.resolve(db)
    } else {
        return new Promise((resolve, reject) => {
            var aSetupSQL = [
                `CREATE TABLE IF NOT EXISTS tags_json(
                    key TEXT PRIMARY KEY, 
                    space TEXT, 
                    data JSONB)`,
                `CREATE TABLE IF NOT EXISTS categories_json(
                    key TEXT PRIMARY KEY, 
                    space TEXT, 
                    data JSONB)`,
                `CREATE TABLE IF NOT EXISTS analytics_json(
                    key TEXT PRIMARY KEY, 
                    space TEXT, 
                    data JSONB)`,
                `CREATE TABLE IF NOT EXISTS scores_json(
                    key TEXT PRIMARY KEY,
                    space TEXT, 
                    data JSONB, 
                    CONSTRAINT key_space UNIQUE (key, space))`,
                `CREATE TABLE IF NOT EXISTS tags(
                    key TEXT PRIMARY KEY, 
                    space TEXT,
                    language TEXT,
                    name TEXT,
                    search TEXT,
                    category TEXT,
                    category_key TEXT,
                    category_name TEXT,
                    primary_lang_key TEXT,
                    favorite BOOLEAN,
                    active BOOLEAN,
                    created_at TIMESTAMP,
                    changed_at TIMESTAMP,
                    counter INTEGER,
                    hash TEXT,
                    ignore BOOLEAN)
                `,
                `CREATE TABLE IF NOT EXISTS categories(
                    key TEXT PRIMARY KEY,
                    space TEXT,
                    language TEXT,
                    name TEXT,
                    "order" INTEGER,
                    search TEXT,
                    color TEXT,
                    icon TEXT,
                    favorite BOOLEAN,
                    primary_lang_key TEXT,
                    active BOOLEAN,
                    created_at TIMESTAMP,
                    changed_at TIMESTAMP,
                    counter INTEGER,
                    hash TEXT,
                    ignore BOOLEAN)
                `,
                `CREATE TABLE IF NOT EXISTS analytics(
                    key TEXT PRIMARY KEY,
                    parent_key TEXT, 
                    space TEXT,
                    event TEXT,
                    date TIMESTAMP,
                    year INTEGER,
                    month INTEGER,
                    day INTEGER,
                    cluster TEXT,
                    installation TEXT,
                    language TEXT,
                    gender TEXT,
                    birth_year INTEGER,
                    age INTEGER,
                    value TEXT,
                    count INTEGER,
                    previous_value TEXT,
                    default_match_mode TEXT,
                    default_match_code INTEGER,
                    device TEXT,
                    device_language TEXT,
                    device_locale TEXT,
                    tag_key TEXT,
                    tag_name TEXT,
                    tag_primary_lang_key TEXT,
                    tag_ref_key TEXT,
                    tag_ref_primary_lang_key TEXT,
                    tag_effective_key TEXT,
                    category_key TEXT,
                    category_name TEXT,
                    category_primary_lang_key TEXT,
                    category_ref_key TEXT,
                    category_ref_prim_lang_key TEXT,
                    category_effective_key TEXT,
                    counter INTEGER,
                    profile_match_mode TEXT,
                    profile_match_code INTEGER,
                    profile_relation_type TEXT,
                    profile_pos_tag_count INTEGER,
                    profile_neg_tag_count INTEGER,
                    match_date TIMESTAMP,
                    match_mode TEXT,
                    match_code INTEGER,
                    match_handshake BOOLEAN,
                    match_tag_key TEXT,
                    match_side TEXT,
                    location_longitude DOUBLE PRECISION,
                    location_latitude DOUBLE PRECISION,
                    location_street TEXT,
                    location_city TEXT,
                    location_country TEXT,
                    message_language TEXT,
                    message_gender TEXT,
                    message_birth_year INTEGER,
                    message_age INTEGER,
                    message_installation TEXT,
                    message_pos_tag_count INTEGER,
                    message_neg_tag_count INTEGER,
                    match_left_left_tag_keys TEXT,
                    match_right_right_tag_keys TEXT,
                    match_left_right_tag_keys TEXT,
                    match_right_left_tag_keys TEXT)
                `,
                `CREATE TABLE IF NOT EXISTS scores(
                    key TEXT PRIMARY KEY, 
                    space TEXT,
                    alias TEXT,
                    count BIGINT,
                    value BIGINT)
                `
            ];
            pool.connect((err, client, done) => {
                if (err) {
                    reject(err);
                    return;
                }
                db = client;
                dbDone = done;
                return Promise.all(_.map(aSetupSQL, (sStatement) => {
                    return db.query(sStatement);
                })).then(() => {
                    resolve();
                });
            });
        }).then(() => {
            return db;
        });
    }
}

module.exports = () => {
    return connect();
};

module.exports.run = function (sStatement) {
    var aArgument = _.toArray(arguments);
    aArgument.shift();
    return db.query(sStatement, aArgument);
};

module.exports.query = function (sStatement, aValue) {
    return db.query(sStatement, aValue);
};

module.exports.close = () => {
    if (db && dbDone) {
        dbDone();
        db = null;
        dbDone = null;
    }
};