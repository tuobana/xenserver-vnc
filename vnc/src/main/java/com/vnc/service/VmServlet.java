package com.vnc.service;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VmServlet extends HttpServlet{
    /**
	 * 
	 */
	private static final long serialVersionUID = 4607272085572782658L;

	private String host = null;
	private String uuid = null;
	private String sessionid = null;
	
	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        String label = request.getParameter("vmlable");
        sessionid = request.getParameter("xensession");
        
        String msg[] = label.split(":");
        host = msg[2];
        uuid = msg[1];
        
        request.setAttribute("host", host);
		request.setAttribute("uuid", uuid);
		request.setAttribute("sessionid", sessionid);
		
    	try {
			request.getRequestDispatcher("/vm.jsp").forward(request, response);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
