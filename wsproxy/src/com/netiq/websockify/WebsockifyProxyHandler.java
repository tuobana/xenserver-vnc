package com.netiq.websockify;

import static org.jboss.netty.handler.codec.http.HttpMethod.GET;


import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import org.apache.xmlrpc.XmlRpcException;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.BigEndianHeapChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.handler.codec.base64.Base64;

import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

import com.xensource.xenapi.Console;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.XenAPIException;

public class WebsockifyProxyHandler extends SimpleChannelUpstreamHandler {

	public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;
    public static final String REDIRECT_PATH = "/redirect";
    
    private final ClientSocketChannelFactory cf;

    private WebSocketServerHandshaker handshaker = null;

    // This lock guards against the race condition that overrides the
    // OP_READ flag incorrectly.
    // See the related discussion: http://markmail.org/message/x7jc6mqx6ripynqf
    final Object trafficLock = new Object();

    private volatile Channel outboundChannel;

    public WebsockifyProxyHandler(ClientSocketChannelFactory cf) {
        this.cf = cf;
        this.outboundChannel = null;
    }

    private void ensureTargetConnection(ChannelEvent e, boolean websocket, final Object sendMsg, String para)
            throws Exception {
    	if(outboundChannel == null) {
    		String[] paras = para.split("&");
    		String[] uuids   = paras[0].split("=");
    		String[] authids = paras[1].split("=");

	        // Suspend incoming traffic until connected to the remote host.
	        final Channel inboundChannel = e.getChannel();
	        inboundChannel.setReadable(false);
			Logger.getLogger(WebsockifyProxyHandler.class.getName()).info("Inbound proxy connection from " + inboundChannel.getRemoteAddress() 
					+ " uuid=" + uuids[1] + " authid=" + authids[1] + ".");
	        
	        // resolve the target
			Console console = Websockify.ConsoleMap.get(uuids[1]);
			final String location = console.getLocation(Websockify.Conn);
	        InetSocketAddress target = new InetSocketAddress(new URL(location).getHost(), 80);
	        
	        // Start the connection attempt.
	        ClientBootstrap cb = new ClientBootstrap(cf);
	        cb.getPipeline().addLast("aggregator", new HttpChunkAggregator(65536));
	        if ( websocket ) {
	        	cb.getPipeline().addLast("handler", new OutboundWebsocketHandler(e.getChannel(), trafficLock));
	        }
	        else {
	        	cb.getPipeline().addLast("handler", new OutboundHandler(e.getChannel(), trafficLock));	        	
	        }

	        ChannelFuture f = cb.connect(target);
	        outboundChannel = f.getChannel();
	        
	        if ( sendMsg != null ) outboundChannel.write(sendMsg);
	        f.addListener(new ChannelFutureListener() {
	            public void operationComplete(ChannelFuture future) throws Exception {
	                if (future.isSuccess()) {
	                    // Connection attempt succeeded:
	                    // Begin to accept incoming traffic.
	                	
	                	connectVmConsole(location);
	                	
                    	inboundChannel.setReadable(true);
	    				Logger.getLogger(WebsockifyProxyHandler.class.getName()).info("Created outbound connection to " + location + ".");
	                    
	                } else {
	    				Logger.getLogger(WebsockifyProxyHandler.class.getName()).severe("Failed to create outbound connection to " + location + ".");
	                    inboundChannel.close();
	                }
	            }
	        });   
    	} else {
	        if ( sendMsg != null ) outboundChannel.write(sendMsg);
    	}
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e)
            throws Exception {
        Object msg = e.getMessage();
        // An HttpRequest means either an initial websocket connection
        // or a web server request
        if (msg instanceof HttpRequest) {
            handleHttpRequest(ctx, (HttpRequest) msg, e);
        // A WebSocketFrame means a continuation of an established websocket connection
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg, e);
            // A channel buffer we treat as a VNC protocol request
        } else if (msg instanceof ChannelBuffer) {
            handleVncDirect(ctx, (ChannelBuffer) msg, e, null);
        }
    }

    private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req, final MessageEvent e) throws Exception {
        // Allow only GET methods.
        if (req.getMethod() != GET) {
        	Logger.getLogger(WebsockifyProxyHandler.class.getName()).info("Just support GET Method.");
            return;
        }

        String upgradeHeader = req.getHeader("Upgrade");
        if(upgradeHeader != null && upgradeHeader.toUpperCase().equals("WEBSOCKET")){
			Logger.getLogger(WebsockifyProxyHandler.class.getName()).fine("Websocket request from " + e.getRemoteAddress() + ".");
	        // Handshake
	        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
	                this.getWebSocketLocation(req), "base64", false);
	        this.handshaker = wsFactory.newHandshaker(req);
	        if (this.handshaker == null) {
	            wsFactory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
	        } else {
	        	// deal with a bug in the flash websocket emulation
	        	// it specifies WebSocket-Protocol when it seems it should specify Sec-WebSocket-Protocol
	        	String protocol = req.getHeader("WebSocket-Protocol");
	        	String secProtocol = req.getHeader("Sec-WebSocket-Protocol");
	        	if(protocol != null && secProtocol == null )
	        	{
	        		req.addHeader("Sec-WebSocket-Protocol", protocol);
	        	}
	            this.handshaker.handshake(ctx.getChannel(), req);
	        }
	    	ensureTargetConnection (e, true, null, req.getUri());
        }
    }

    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame, final MessageEvent e) {

        // Check for closing frame
        if (frame instanceof CloseWebSocketFrame) {
            this.handshaker.close(ctx.getChannel(), (CloseWebSocketFrame) frame);
            return;
        } else if (frame instanceof PingWebSocketFrame) {
            ctx.getChannel().write(new PongWebSocketFrame(frame.getBinaryData()));
            return;
        } else if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass()
                    .getName()));
        }
        
        ChannelBuffer msg = ((TextWebSocketFrame) frame).getBinaryData();
        ChannelBuffer decodedMsg = Base64.decode(msg);
        synchronized (trafficLock) {
            outboundChannel.write(decodedMsg);
            if (!outboundChannel.isWritable()) {
                e.getChannel().setReadable(false);
            }  	
        }
    }

    private void handleVncDirect(ChannelHandlerContext ctx, ChannelBuffer buffer, final MessageEvent e, String para) throws Exception {
    	// ensure the target connection is open and send the data
    	ensureTargetConnection(e, false, buffer, para);
    }

    private String getWebSocketLocation(HttpRequest req) {
        String prefix = "ws";
        String origin = req.getHeader(HttpHeaders.Names.ORIGIN).toLowerCase();
        if(origin.contains("https")){
            prefix = "wss";
        }
        return prefix + "://" + req.getHeader(HttpHeaders.Names.HOST) + req.getUri();
    }

    @Override
    public void channelInterestChanged(ChannelHandlerContext ctx,
            ChannelStateEvent e) throws Exception {
        // If inboundChannel is not saturated anymore, continue accepting
        // the incoming traffic from the outboundChannel.
        synchronized (trafficLock) {
            if (e.getChannel().isWritable() && outboundChannel != null) {
                outboundChannel.setReadable(true);
            }
        }
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {

		Logger.getLogger(WebsockifyProxyHandler.class.getName()).info("Inbound proxy connection from " + ctx.getChannel().getRemoteAddress() + " closed.");
        if (outboundChannel != null) {
            closeOnFlush(outboundChannel);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        e.getCause().printStackTrace();
		Logger.getLogger(WebsockifyProxyHandler.class.getName()).severe("Exception on inbound proxy connection from " + e.getChannel().getRemoteAddress() + ": " + e.getCause().getMessage());
        closeOnFlush(e.getChannel());
    }

    /**
     * Closes the specified channel after all queued write requests are flushed.
     */
    static void closeOnFlush(Channel ch) {
        if (ch.isConnected()) {
            ch.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
    
    private void connectVmConsole(String location) throws BadServerResponse, XenAPIException, XmlRpcException, MalformedURLException {
		URL uri = new URL(location);
		
		String headers[] = makeHeaders(uri.getPath().concat("?").concat(uri.getQuery()), uri.getHost(), Websockify.Conn.getSessionReference());
    	for (String header : headers) {
    		ChannelBuffer msgdata = new BigEndianHeapChannelBuffer(header.getBytes());
        	outboundChannel.write(msgdata);
        	outboundChannel.write(new BigEndianHeapChannelBuffer("\r\n".getBytes())); 
        } 
    }
    
    private String[] makeHeaders(String path, String host, String session) {
    	
        String[] headers = { String.format("CONNECT %s HTTP/1.1", path),
                String.format("Host: %s", host),
                String.format("Cookie: session_id=%s", session), "" };
        return headers;
    }
}
