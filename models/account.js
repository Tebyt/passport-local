var mongoose = require('mongoose'),
    Schema = mongoose.Schema,
    passportLocalMongoose = require('passport-local-mongoose'),
    bcrypt = require('bcrypt-nodejs');

var Account = new Schema({
    username: String,
    password: String,
    nickname: String,
    school: String,
    company: String,
    avatar: String
});

Account.plugin(passportLocalMongoose);

module.exports = mongoose.model('Account', Account);