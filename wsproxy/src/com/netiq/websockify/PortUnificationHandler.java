/*
 * Copyright 2011 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.netiq.websockify;

import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

/**
 * Manipulates the current pipeline dynamically to switch protocols or enable
 * SSL or GZIP.
 */
public class PortUnificationHandler extends FrameDecoder {
    protected static long connectionToFirstMessageTimeout = 5000;

	private final ClientSocketChannelFactory cf;
    private Timer msgTimer = null;
    private long directConnectTimerStart = 0;
    
    public PortUnificationHandler(ClientSocketChannelFactory cf) {
    	this.cf = cf;
    }

	public static long getConnectionToFirstMessageTimeout() {
		return connectionToFirstMessageTimeout;
	}

	public static void setConnectionToFirstMessageTimeout(long connectionToFirstMessageTimeout) {
		PortUnificationHandler.connectionToFirstMessageTimeout = connectionToFirstMessageTimeout;
	}
    
    // In cases where there will be a direct VNC proxy connection
    // The client won't send any message because VNC servers talk first
    // So we'll set a timer on the connection - if there's no message by the time
    // the timer fires we'll create the proxy connection to the target
    @Override
    public void channelOpen(final ChannelHandlerContext ctx, final ChannelStateEvent e)
            throws Exception {
    	startDirectConnectionTimer( ctx );
    }
    
    private void startDirectConnectionTimer ( final ChannelHandlerContext ctx )
    {
    	// cancel any outstanding timer
        cancelDirectConnectionTimer ( );
        
        // direct proxy connection disabled
        if ( connectionToFirstMessageTimeout <= 0 ) return;
        
        directConnectTimerStart = System.currentTimeMillis();
    
    	// cancelling a timer makes it unusable again, so we have to create another one
    	msgTimer = new Timer();
    	msgTimer.schedule(new TimerTask ( ) {

			@Override
			public void run() {
		        switchToDirectProxy(ctx);				
			}
    		
    	}, connectionToFirstMessageTimeout);
    	
    }
    
    private void cancelDirectConnectionTimer ( )
    {
    	if ( directConnectTimerStart > 0 ) {
	    	long directConnectTimerCancel = System.currentTimeMillis();
			Logger.getLogger(PortUnificationHandler.class.getName()).finer("Direct connection timer canceled after " + (directConnectTimerCancel - directConnectTimerStart) + " milliseconds.");
    	}
    	
    	if ( msgTimer != null ) {
    		msgTimer.cancel();
    		msgTimer = null;
    	}
    	
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        // Will use the first two bytes to detect a protocol.
        if (buffer.readableBytes() < 2) {
            return null;
        }
        
        cancelDirectConnectionTimer ( );        

        final int magic1 = buffer.getUnsignedByte(buffer.readerIndex());
        final int magic2 = buffer.getUnsignedByte(buffer.readerIndex() + 1);

        Logger.getLogger(PortUnificationHandler.class.getName()).info(magic1 + " " + magic2);
        
        switchToWebsocketProxy(ctx);

        // Forward the current read buffer as is to the new handlers.
        return buffer.readBytes(buffer.readableBytes());
    }

    private void switchToWebsocketProxy(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.getPipeline();

		Logger.getLogger(PortUnificationHandler.class.getName()).info("Websocket proxy request from " + ctx.getChannel().getRemoteAddress() + ".");

        p.addLast("decoder", new HttpRequestDecoder());
        p.addLast("aggregator", new HttpChunkAggregator(65536));
        p.addLast("encoder", new HttpResponseEncoder());
        p.addLast("chunkedWriter", new ChunkedWriteHandler());
        
        p.addLast("handler", new WebsockifyProxyHandler(cf));
        p.remove(this);
    }


    private void switchToDirectProxy(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.getPipeline();

		Logger.getLogger(PortUnificationHandler.class.getName()).fine("Direct proxy request from " + ctx.getChannel().getRemoteAddress() + ".");
		
        p.addLast("proxy", new DirectProxyHandler( ctx.getChannel(), cf));

        p.remove(this);
    }

    // cancel the timer if channel is closed - prevents useless stack traces
    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
        cancelDirectConnectionTimer ( );
    }

    // cancel the timer if exception is caught - prevents useless stack traces
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        cancelDirectConnectionTimer ( );
		Logger.getLogger(PortUnificationHandler.class.getName()).severe("Exception on connection to " + ctx.getChannel().getRemoteAddress() + ": " + e.getCause().getMessage() );
    }
}
