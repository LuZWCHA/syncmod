package com.mods.sync.core;

import com.mods.sync.SyncMod;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.nettyrouter.nettyhandlers.NettyRouterHandler;
import io.nettyrouter.nettyhandlers.RouterChannelInboundHandler;

public class HttpRouterHandler extends RouterChannelInboundHandler<FullHttpRequest> {

    protected HttpRouterHandler() throws Exception {
        super();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        //System.out.println(cause.toString());
    }

    @Override
    protected String getURL(FullHttpRequest msg) {
        String url = msg.uri();
        String method = msg.method().name();

        return method + ":" + msg.uri();
    }

    @Override
    protected NettyRouterHandler<FullHttpRequest> initControl() {
        return new NettyRouterHandler<>(FullHttpRequest.class);
    }


    @Override
    public boolean check(FullHttpRequest msg) {
        return SyncMod.Instance != null;
    }
}
