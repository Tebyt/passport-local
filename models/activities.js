var mongoose = require('mongoose')
    , Schema = mongoose.Schema;
var activitySchema = new mongoose.Schema({
    description: String,
    userId: { type: Schema.Types.ObjectId, ref: 'Account' },
    type: String,
    price: Number
});
module.exports = mongoose.model('activities', activitySchema);