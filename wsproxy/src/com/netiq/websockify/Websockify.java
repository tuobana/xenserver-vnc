package com.netiq.websockify;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.xensource.xenapi.APIVersion;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Console;
import com.xensource.xenapi.Session;
import com.xensource.xenapi.VM;
import com.xensource.xenapi.Types.ConsoleProtocol;
import com.xensource.xenapi.VM.Record;

public class Websockify {
	
	public static Connection Conn = null;
	public static HashMap<String, Console> ConsoleMap = new HashMap<String, Console>();
	
	//@Option(name="--direct-proxy-timeout",usage="connection timeout before a direct proxy connection is established in milliseconds. Default is 5000 (5 seconds). With the VNC protocol the server sends the first message. This means that a client that wants a direct proxy connection will connect and not send a message. Websockify will wait the specified number of milliseconds for an incoming connection to send a message. If no message is recieved it initiates a direct proxy connection. Setting this value too low will cause connection attempts that aren't direct proxy connections to fail. Set this to 0 to disable direct proxy connections.")
	private int directProxyTimeout = 5000;

	public Websockify ( ) {
	}

    public static void main(String[] args) throws Exception {
    	new Websockify().doMain(args);
    }
    
    public void doMain(String[] args) throws Exception {    	
    	
    	int sourcePort = 10000;
    	
        System.out.println("Websockify Proxying *:" + sourcePort + " to XenServer VM Console ...");
        
        PortUnificationHandler.setConnectionToFirstMessageTimeout(directProxyTimeout);
        WebsockifyServer wss = new WebsockifyServer ( );
        wss.connect ( sourcePort);
        
        doInit();
    }
    
    public void doInit() {
    	
    	try {
    		//Conn = new Connection(new URL("http://192.168.18.12")); 
    		//Session.loginWithPassword(Conn, "root", "bjhit2012$", APIVersion.latest().toString());
    		
    		Conn = new Connection(new URL("http://172.19.105.14")); 
    		Session.loginWithPassword(Conn, "root", "erange2013$*", APIVersion.latest().toString());
    		
    		Map<VM, Record> rec = VM.getAllRecords(Conn);
			for (Map.Entry<VM, Record> e:rec.entrySet())
			{
				if (e.getValue().domid > 0) {
					Set<Console> consoles = e.getValue().consoles;
					Iterator<Console> i = consoles.iterator();
					Console console = i.next();
					while (console.getProtocol(Conn) != ConsoleProtocol.RFB)
						console = i.next();
					
					ConsoleMap.put(e.getValue().uuid, console);
				}
			}
			
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }

}
