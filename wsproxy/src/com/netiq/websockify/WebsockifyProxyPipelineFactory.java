package com.netiq.websockify;

import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;

public class WebsockifyProxyPipelineFactory implements ChannelPipelineFactory {

    private final ClientSocketChannelFactory cf;

    public WebsockifyProxyPipelineFactory(ClientSocketChannelFactory cf) {
        this.cf = cf;
    }

    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline p = pipeline(); // Note the static import.
        
        p.addLast("unification", new PortUnificationHandler(cf));
        return p;

    }

}
