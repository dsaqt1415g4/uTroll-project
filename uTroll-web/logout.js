function LogOut() {
	window.alert('Hasta la proxima!');
	setCookie('username', '', -1);
	setCookie('name', '', -1);
	setCookie('password', '', -1);
	setCookie('email', '', -1);
	setCookie('groupid', '', -1);
	setCookie('troll', '', -1);
	setCookie('votedBy', '', -1);
	setCookie('vote', '', -1);
	setCookie('points', '', -1);
	setCookie('points_max', '', -1);

	window.location = "/index.html";
}

function setCookie(cname, cvalue, exdays) {
	var d = new Date();
	d.setTime(d.getTime() + (exdays * 24 * 60 * 60 * 1000));
	var expires = "expires=" + d.toUTCString();
	document.cookie = cname + "=" + cvalue + "; " + expires;
}

function reloadCookies() {

	var u = getCookie('username');
	var p = getCookie('password');

	var url = API_BASE_URL + '/users/byUsername/' + u;

	$.ajax({
		headers : {
			'Authorization' : "Basic " + btoa(u + ':' + p)
		},
		url : url,
		type : 'GET',
		crossDomain : true,
		dataType : 'json',
		contentType : 'application/vnd.uTroll.api.user+json',
	}).done(function(data, status, jqxhr) {
		var uProf = data;
		// Cambiar por el uso de setCookie
		setCookie("name", uProf.name, 1);
		setCookie("email", uProf.email, 1);
		setCookie("points", uProf.points, 1);
		setCookie("points_max", uProf.points_max, 1);
		setCookie("troll", uProf.troll, 1);
		setCookie("votedBy", uProf.votedBy, 1);
		setCookie("vote", uProf.vote, 1);
		setCookie("groupid", uProf.groupid, 1);
	}).fail(function() {
		window.alert("ERROR: Cookies Error");
	});
}

function voteTroll(voted) {
	var u = getCookie('username');
	var p = getCookie('password');

	var url = API_BASE_URL + '/users/vote/' + voted;
	var data = JSON.stringify("");

	$.ajax({
		headers : {
			'Authorization' : "Basic " + btoa(u + ':' + p)
		},
		url : url,
		type : 'PUT',
		crossDomain : true,
		dataType : 'json',
		contentType : 'application/vnd.uTroll.api.user+json',
		data : data,
	}).done(function(data, status, jqxhr) {
		$("#comments_space").text('');
		getComments();
	}).fail(function() {
		window.alert("FAIL Vote");
	});
}

function getCookie(cname) {
	var name = cname + "=";
	var ca = document.cookie.split(';');
	for (var i = 0; i < ca.length; i++) {
		var c = ca[i];
		while (c.charAt(0) == ' ')
			c = c.substring(1);
		{
			if (c.indexOf(name) == 0)
				return c.substring(name.length, c.length);
			{
			}
		}
	}
	return "";
}