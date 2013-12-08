var rfb;

function accessvm(host, uuid, sessionid) {

        function passwordRequired(rfb) {
            var msg;
            msg = '<form onsubmit="return setPassword();"';
            msg += '  style="margin-bottom: 0px">';
            msg += 'Password Required: ';
            msg += '<input type=password size=10 id="password_input" class="noVNC_status">';
            msg += '<\/form>';
            $D('noVNC_status_bar').setAttribute("class", "noVNC_status_warn");
            $D('noVNC_status').innerHTML = msg;
        }
        function setPassword() {
            rfb.sendPassword($D('password_input').value);
            return false;
        }
        function sendCtrlAltDel() {
            rfb.sendCtrlAltDel();
            return false;
        }
        function updateState(rfb, state, oldstate, msg) {
            var s, sb, cad, level;
            s = $D('noVNC_status');
            sb = $D('noVNC_status_bar');
            cad = $D('sendCtrlAltDelButton');
            switch (state) {
                case 'failed':       level = "error";  break;
                case 'fatal':        level = "error";  break;
                case 'normal':       level = "normal"; break;
                case 'disconnected': level = "normal"; break;
                case 'loaded':       level = "normal"; break;
                default:             level = "warn";   break;
            }

            if (state === "normal") { cad.disabled = false; }
            else                    { cad.disabled = true; }

            if (typeof(msg) !== 'undefined') {
                sb.setAttribute("class", "noVNC_status_" + level);
                s.innerHTML = msg;
            }
        }

        var port = 80;
        
		if(rfb) 
			try {rfb.disconnect();} catch(err) {};
		
		rfb = new RFB({'target':       $D('noVNC_canvas'),
                       'encrypt':      (port==443),
                       'true_color':   WebUtil.getQueryVar('true_color', true),
                       'local_cursor': WebUtil.getQueryVar('cursor', true),
                       'shared':       WebUtil.getQueryVar('shared', true),
                       'view_only':    WebUtil.getQueryVar('view_only', false),
                       'updateState':  updateState,
                       'onPasswordRequired':  passwordRequired});

		rfb.connect(host, port, "", "uuid=" + uuid + "&authid=" + sessionid + "");
}