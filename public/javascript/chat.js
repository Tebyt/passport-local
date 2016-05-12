//for local test purpose only
var initial_Json = '{'+
  '"description": "https://t.co/lSxI17zL1V",'+
    '"create_date": "2016-05-09T02:38:49.315Z",'+
    '"create_coordinates": {'+
      '"lat": 40.75447922515537,'+
      '"lng": -73.97589108054672'+
    '},'+
    '"id": "572ff839a72ff852189eec58",'+
  '"userId": "572e835ad3055492087911bf",'+
  '"creater": {'+
    '"username": "facebook-login.1834243556803365",'+
    '"email": "1834243556803365@loopback.facebook.com",'+
    '"id": "572e835ad3055492087911bf"'+
  '},'+
  '"participants": ['+
    '{'+
      '"username": "facebook-login.1834243556803365",'+
    '"email": "1834243556803365@loopback.facebook.com",'+
    '"id": "572e835ad3055492087911bf",'+
    '"nickname":"Tom",'+
    '"avatar_url":"http://d2.christiantoday.com/en/full/21113/zoe-saldana-as-neytiri-in-avatar.jpg"'+
    '},'+
      '{'+
      '"username": "facebook-login.1834243556803365",'+
    '"email": "1834243556803365@loopback.facebook.com",'+
    '"id": "5730eb162160ea202000cb2c",'+
    '"nickname":"Jack",'+
    '"avatar_url":"http://d2.christiantoday.com/en/full/21113/zoe-saldana-as-neytiri-in-avatar.jpg"'+
    '}'+
  '],'+
  '"messages": ['+
    '{'+
     '"create_date": "2016-05-10T00:00:00.000Z",'+
      '"content": "Lets move!!!",'+
      '"id": "5730ea7fb362b20820afbc7d",'+
      '"userId": "572e835ad3055492087911bf"'+
    '},'+
    '{'+
      '"create_date": "2016-05-10T00:00:00.000Z",'+
      '"content": "Great!!!",'+
      '"id": "5730eb6a5c71393d20cb8e5e",'+
      '"userId": "5730eb162160ea202000cb2c"'+
    '},'+
    '{'+
     '"create_date": "2016-05-10T00:00:00.000Z",'+
      '"content": "wait!!!",'+
      '"id": "5730ea7fb362b20820afbc7d",'+
      '"userId": "572e835ad3055492087911bf"'+
    '},'+
    '{'+
     '"create_date": "2016-05-10T00:00:00.000Z",'+
      '"content": "wait!!!",'+
      '"id": "5730ea7fb362b20820afbc7d",'+
      '"userId": "572e835ad3055492087911bf"'+
    '},'+
    '{'+
     '"create_date": "2016-05-10T00:00:00.000Z",'+
      '"content": "wait!!!",'+
      '"id": "5730ea7fb362b20820afbc7d",'+
      '"userId": "572e835ad3055492087911bf"'+
    '},'+
    '{'+
     '"create_date": "2016-05-10T00:00:00.000Z",'+
      '"content": "wait!!!",'+
      '"id": "5730ea7fb362b20820afbc7d",'+
      '"userId": "572e835ad3055492087911bf"'+
    '},'+
    '{'+
      '"create_date": "2016-05-10T00:00:00.000Z",'+
      '"content": "WAT!!!",'+
      '"id": "5730eb6a5c71393d20cb8e5e",'+
      '"userId": "5730eb162160ea202000cb2c"'+
    '},'+
    '{'+
     '"create_date": "2016-05-10T00:00:00.000Z",'+
      '"content": "I hate you",'+
      '"id": "5730ea7fb362b20820afbc7d",'+
      '"userId": "572e835ad3055492087911bf"'+
    '},'+
    '{'+
      '"create_date": "2016-05-10T00:00:00.000Z",'+
      '"content": "java is so hard!!!",'+
      '"id": "5730eb6a5c71393d20cb8e5e",'+
      '"userId": "5730eb162160ea202000cb2c"'+
    '},'+
    '{'+
     '"create_date": "2016-05-10T00:00:00.000Z",'+
      '"content": "Lets move!!!",'+
      '"id": "5730ea7fb362b20820afbc7d",'+
      '"userId": "572e835ad3055492087911bf"'+
    '},'+
    '{'+
      '"create_date": "2016-05-10T00:00:00.000Z",'+
      '"content": "Great!!!",'+
      '"id": "5730eb6a5c71393d20cb8e5e",'+
      '"userId": "5730eb162160ea202000cb2c"'+
    '}'+
  ']'+
'}';

var activityDetail_Json = '{'+
      '"description": "Accountemps: Accounting Clerk (#NEWYORK, NY) https://t.co/fOGxMHkqKN #Accounting #Accountemps #Job #Jobs #Hiring",'+
      '"create_date": "2016-05-09T00:54:57.896Z",'+
      '"create_coordinates": {'+
        '"lat": -73.9697795,'+
        '"lng": 40.7519846'+
      '},'+
      '"id": "728299440298033152"'+
    '}';
///////////////
var chatForm = $("#chatform"),
	chats = $(".chats"),
	textarea = $('#message'),
	title=$('.titlebar'),
	titlebutton=$("#titleContainer"),
	detailed_activity = $('.detailedActivity'),
	footer=$('.footer');

//chat content variables
var initial = JSON.parse(initial_Json);  //read from local
var activity_Id = initial.id;
var idArray = [];
var nickArray = [];
var avatarArray=[];
var participantLength = 0;

//activity_detail
var activity_detail = JSON.parse(activityDetail_Json);
var title = activity_detail.description;
detailed_activity.append(title);

//management variable
var userNickName = null;
var userAvatar = null;
var userId = null;
//front end
var chatScreenHeight=0;

function loadTitle(msg){
	if(msg.length>20){
		msg=msg.substring(0,20)+"...";
	}
	titlebutton.append(msg);
}

function createChatMessage(msg,speakerName,imgg){

		var who = '';
		if(speakerName!==userNickName) {
			who = 'me';
		}
		else {
			who = 'you';
		}

		var li = $(
			'<li class=' + who + '>'+
				'<div class="image">' +
					'<img src=' + imgg + ' />' +
					'<b></b>' +
				'</div>' +
				'<p></p>' +
			'</li>');

		// use the 'text' method to escape malicious user input
		li.find('p').text(msg);
		li.find('b').text(speakerName);

		chats.append(li);
		chatScreenHeight+=li.height();		
}

function scrollMessage(){
	$("html,body").animate({scrollTop:chatScreenHeight},1000); //animation
}

function sendMsgToServer(content,send_Activity_Id,userID){
  var sendJson = {"content": content, "activityId": activityId, "userId":userID};
  console.log(sendJson);
  //$.post('/api/activities/'+send_Activity_Id+'/messages',sendJson,function(data,status){console.log("send msg to server")});
  //$.post('/api/activities/'+send_Activity_Id+'/messages',function(sendJson){});
  $.ajax({
       url: '/messages',
       type: 'POST',
       contentType:'json',
       data: sendJson,
       dataType:'json'
  });
}

function receiveMsg(){
  var revMsg;

  createChatMessage(revMsg,userNickName,userAvatar);  
  scrollMessage();
}


//the send button on the bottom
$("#submit").click(function(){
  sendMsgToServer(textarea.val(),activity_Id,userId);
  textarea.val("");
});



titlebutton.click(function(){
	detailed_activity.toggleClass('detailedActivity-show');
});

function loadParticipant(json){
	var participant = json.participants;
	participantLength = participant.length;
	for(var i=0;i<participantLength;i++){
		idArray[i] = participant[i].id;
		nickArray[i] = participant[i].nickname;
		avatarArray[i] = participant[i].avatar_url;
	}

	//TO Change
  //the logined in user
	userNickName = nickArray[0];
	userAvatar = avatarArray[0];
  userId = idArray[0];
}


function findNickAndAvatar(id){
	for(var i=0;i<participantLength;i++){
		if(id===idArray[i]){
			return [nickArray[i], avatarArray[i]];
		}
	}
}

function loadHistory(json){
	var msg = json.messages;
	for(var i=0;i<msg.length;i++){
		var content = msg[i].content;
		var output= findNickAndAvatar(msg[i].userId);
		createChatMessage(content,output[0],output[1])
	}

	scrollMessage();  //scroll after loading completed
}

//load activity title, participant and chat history
loadTitle(title);
loadParticipant(initial);
loadHistory(initial);