package com.vnc.service;


import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xmlrpc.XmlRpcException;

import com.xensource.xenapi.APIVersion;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Session;
import com.xensource.xenapi.VM;
import com.xensource.xenapi.VM.Record;

public class VncServlet extends HttpServlet{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private List<String> lables = new ArrayList<String>();
	private String xensession = null;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        handleTunnelRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        handleTunnelRequest(request, response);
    }
    
    /**
     * Dispatches every HTTP GET and POST request to the appropriate handler
     * function based on the query string.
     *
     * @param request The HttpServletRequest associated with the GET or POST
     *                request received.
     * @param response The HttpServletResponse associated with the GET or POST
     *                 request received.
     * @throws ServletException If an error occurs while servicing the request.
     */
    protected void handleTunnelRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException {
    	
        try {
        	
            String query = request.getQueryString();
            if (query == null) {
                throw new ServletException("No query string provided.");
            }
            
            if(query.startsWith("login")) {
				try {
					doLogin(request, response);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            } else {
                throw new ServletException("Invalid operation: " + query);
            }
            
        } catch (ServletException e) {
        	e.printStackTrace();
        }
    }
    
    private void doLogin(HttpServletRequest request, HttpServletResponse response) throws XmlRpcException, ServletException, IOException {
     	String login_server = request.getParameter("login_server");
    	String login_username = request.getParameter("login_username");
    	String login_password = request.getParameter("login_password");
    	
    	Connection Conn = new Connection(new URL("http://" + login_server)); 
		Session.loginWithPassword(Conn, login_username, login_password, APIVersion.latest().toString());
		
		xensession = Conn.getSessionReference();
		
		Map<VM, Record> rec = VM.getAllRecords(Conn);
		for (Map.Entry<VM, Record> e:rec.entrySet())
		{
			if (e.getValue().domid > 0) {
				lables.add(e.getValue().nameLabel + ":" + e.getValue().uuid + ":" + e.getValue().residentOn.getAddress(Conn));
			}
		}
		request.removeAttribute("lables");
		request.setAttribute("xensession", xensession);
		request.setAttribute("lables", lables);
		
    	request.getRequestDispatcher("/consoles.jsp").forward(request, response);
    }
}
