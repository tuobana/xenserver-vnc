<%@ page language="java" contentType="text/html"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>VM</title>
<script type="text/javascript" src="<%=request.getContextPath()%>/js/xenapi.js"> </script>
<script type="text/javascript" src="<%=request.getContextPath()%>/js/accessvm.js"> </script>
<script type="text/javascript" src="<%=request.getContextPath()%>/js/util.js"> </script>
<script type="text/javascript" src="<%=request.getContextPath()%>/js/webutil.js"> </script>
<script type="text/javascript" src="<%=request.getContextPath()%>/js/base64.js"> </script>
<script type="text/javascript" src="<%=request.getContextPath()%>/js/websock.js"> </script>
<script type="text/javascript" src="<%=request.getContextPath()%>/js/des.js"> </script>
<script type="text/javascript" src="<%=request.getContextPath()%>/js/input.js"> </script>
<script type="text/javascript" src="<%=request.getContextPath()%>/js/display.js"> </script>
<script type="text/javascript" src="<%=request.getContextPath()%>/js/jsunzip.js"> </script>
<script type="text/javascript" src="<%=request.getContextPath()%>/js/rfb.js"> </script>
<script type="text/javascript" src="<%=request.getContextPath()%>/js/ui.js"> </script>

<style>
body {TEXT-ALIGN: center;}
#noVNC_screen { MARGIN-RIGHT: auto; MARGIN-LEFT: auto; }
</style>

<script type="text/javascript">
window.onload = function() {
	var host = "${requestScope.host}";
	var uuid = "${requestScope.uuid}";
	var sessionid = "${requestScope.sessionid}";
	accessvm(host, uuid, sessionid);
};
</script>

</head>
<body>	
        <div id="noVNC_screen">
			<div class="well">
              <canvas id="noVNC_canvas" width="640px" height="20px">
				Canvas not supported.
              </canvas>
              <div id="noVNC_status_bar" class="noVNC_status_bar" display="none">
                <table class="table bordered-table"><tr>
                    <td><div id="noVNC_status">Loading</div></td>
                    <td><div id="noVNC_buttons">
                        <input type=button value="Send CtrlAltDel" id="sendCtrlAltDelButton">
                      </div>
					</td>
                </tr></table>
              </div>
			</div>
        </div>	        
</body>
</html>