<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<link href="css/bootstrap.min.css" rel="stylesheet">
<title>Login</title>

<script>
	function login() {
		document.forms[0].action = "<%=request.getContextPath()%>/vnc?login";
		document.forms[0].submit();
	}
	
</script>

</head>

<body>
<div id="content" class="container" style="margin-top:40px">
	<div class="content well">
		<form class="form-horizontal">
		  <fieldset>
			<legend>Login to an XenServer</legend>
          <div class="control-group">
		  <label class="control-label">Server</label>
		  <div class="controls"><input type="text" id="login_server" name="login_server" placeholder="Server" /></div>
		  </div>
          <div class="control-group">
		  <label class="control-label">Username</label>
		  <div class="controls"><input type="text" id="login_username" name="login_username" placeholder="Username"/></div>
		  </div>
          <div class="control-group">
		  <label class="control-label">Password</label>
		  <div class="controls"><input type="password" id="login_password" name="login_password" placeholder="Password"/></div>
		  </div>

		  <div class="control-group">
            <div class="controls">
			<button class="btn" type="button" onclick="login()">Login</button>
			</div>
		  </div>
		  </fieldset>
		  </form>
     </div>
</div>
</body>
</html>