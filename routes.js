var passport = require('passport');
var Account = require('./models/account');
var mongoose = require('mongoose');

var Activities = require('./models/activities')
var Messages = require('./models/messages')

module.exports = function (app) {
    


  app.get('/register', function(req, res) {
      res.render('register', { });
  });

  app.post('/register', function(req, res) {
      console.log(req.body);
      Account.register(new Account(req.body), req.body.password, function(err, account) {
          if (err) {
            return res.render("register", {info: "Sorry. That username already exists. Try again."});
          }

          passport.authenticate('local')(req, res, function () {
            res.redirect('/');
          });
      });
  });

  app.get('/login', function(req, res) {
      res.render('login', { user : req.user });
  });

  app.post('/login', passport.authenticate('local'), function(req, res) {
      res.redirect('/');
  });

  app.get('/logout', function(req, res) {
      req.logout();
      res.redirect('/');
  });

  app.get('/ping', function(req, res){
      res.send("pong!", 200);
  });

  app.get('/activities', function(req, res) {
      Activities.find({}).populate('userId').exec(function(err, activities) {
          res.json(activities);
      })
  })

    app.post('/activities', function(req, res){
        Activities.create(req.body, function(err, activity) {
            res.json(activity);
        })
    })
    app.get('/post', function(req, res) {
        res.render('post', {userId: req.user._id});
    })
    app.post('/messages', function(req, res) {
        console.log(req.body);
        Messages.create(req.body, function(err, message) {
            res.json(message);
        })
    })
    app.get('/chat/:activityId', function(req, res) {
        res.render("chat", {activityId: req.params.activityId});
    })
    app.get('/messages/:activityId', function(req, res) {
        Messages.find({activityId: req.params.activityId}).populate('userId').exec(function (err, messages) {
            res.json({
                activityId: req.params.activityId,
                userId: req.user._id,
                messages: messages
            });
        });
    })
};
