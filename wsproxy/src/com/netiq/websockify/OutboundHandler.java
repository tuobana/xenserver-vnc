package com.netiq.websockify;

import java.util.logging.Logger;

import org.jboss.netty.buffer.BigEndianHeapChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.base64.Base64;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.util.CharsetUtil;

public class OutboundHandler extends SimpleChannelUpstreamHandler {

    private final Channel inboundChannel;
    private final Object trafficLock;
    
    OutboundHandler(Channel inboundChannel, Object trafficLock) {
        this.inboundChannel = inboundChannel;
        this.trafficLock = trafficLock;
    }
    
    protected Object processMessage ( ChannelBuffer buffer ) {
    	return buffer;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e)
            throws Exception {

        ChannelBuffer msg = (ChannelBuffer) e.getMessage();
        String data = msg.toString(CharsetUtil.US_ASCII);
    	Object outMsg = processMessage ( msg );

    	// First time is the http reply, don't send the data to browser.
		if(data.startsWith("HTTP")) {
			System.out.println(data);
			// Avoid the http response received with RFB Version, So need deal with the case.
			if(data.indexOf("RFB") != -1) {
				String tmp = data.substring(data.indexOf("RFB"));
				String versions[] = tmp.split(" ");
				String version = "RFB " + versions[1];
				ChannelBuffer msgdata = new BigEndianHeapChannelBuffer(version.getBytes());
	    		ChannelBuffer base64Msg = Base64.encode(msgdata, false);
	    		Object SelfMsg = new TextWebSocketFrame(base64Msg);
	            synchronized (trafficLock) {
	                inboundChannel.write(SelfMsg);
	                // If inboundChannel is saturated, do not read until notified in
	                // HexDumpProxyInboundHandler.channelInterestChanged().
	                if (!inboundChannel.isWritable()) {
	                    e.getChannel().setReadable(false);
	                }
	            }
	    		return;
			} else {
				return;
			}
		}
		
		if(data.startsWith("\r\n") && data.length() == 2) {
			System.out.println("space char");
			return;
		}

		if(data.indexOf("RFB") != -1 && (data.length() > 12 && data.length() < 15)) {
			System.out.println("length = " + data.length() + " " + data);
			String tmp[] = data.split(" ");
			String version = "RFB " + tmp[1];
			System.out.println(version);
			ChannelBuffer msgdata = new BigEndianHeapChannelBuffer(version.getBytes());
    		ChannelBuffer base64Msg = Base64.encode(msgdata, false);
    		Object SelfMsg = new TextWebSocketFrame(base64Msg);
            synchronized (trafficLock) {
                inboundChannel.write(SelfMsg);
                // If inboundChannel is saturated, do not read until notified in
                // HexDumpProxyInboundHandler.channelInterestChanged().
                if (!inboundChannel.isWritable()) {
                    e.getChannel().setReadable(false);
                }
            }
    		return;
		}
    	
        synchronized (trafficLock) {
            inboundChannel.write(outMsg);
            // If inboundChannel is saturated, do not read until notified in
            // HexDumpProxyInboundHandler.channelInterestChanged().
            if (!inboundChannel.isWritable()) {
                e.getChannel().setReadable(false);
            }
        }
    }

    @Override
    public void channelInterestChanged(ChannelHandlerContext ctx,
            ChannelStateEvent e) throws Exception {
        // If outboundChannel is not saturated anymore, continue accepting
        // the incoming traffic from the inboundChannel.
        synchronized (trafficLock) {
            if (e.getChannel().isWritable()) {
                inboundChannel.setReadable(true);
            }
        }
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
		Logger.getLogger(WebsockifyProxyHandler.class.getName()).info("Outbound proxy connection to " + ctx.getChannel().getRemoteAddress() + " closed.");
        WebsockifyProxyHandler.closeOnFlush(inboundChannel);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        e.getCause().printStackTrace();
		Logger.getLogger(WebsockifyProxyHandler.class.getName()).severe("Exception on outbound proxy connection to " + e.getChannel().getRemoteAddress() + ": " + e.getCause().getMessage());
        WebsockifyProxyHandler.closeOnFlush(e.getChannel());
    }
}
