var mongoose = require('mongoose')
    , Schema = mongoose.Schema;
var messageSchema = new mongoose.Schema({
    content: String,
    userId: { type: Schema.Types.ObjectId, ref: 'Account' },
    activityId: { type: Schema.Types.ObjectId, ref: 'activities' },
    create_date: Date
});
module.exports = mongoose.model('messages', messageSchema);