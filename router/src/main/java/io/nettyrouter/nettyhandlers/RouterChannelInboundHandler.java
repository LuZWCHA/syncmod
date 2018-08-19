package io.nettyrouter.nettyhandlers;

import io.nettyrouter.annotation.NettyRouter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public abstract class RouterChannelInboundHandler<T> extends SimpleChannelInboundHandler<T> implements NettyRouter.PreHandler<T> {

    private NettyRouterHandler<T> routerControl;

    protected RouterChannelInboundHandler() throws Exception {
        super();
        RouterMap.INSTANCE.init("*");
        routerControl = initControl();
        routerControl.setPreHandler(this);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if(cause instanceof  RuntimeException)
            cause.printStackTrace();
        else
            super.exceptionCaught(ctx,cause);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, T msg) throws Exception {
        String command = getURL(msg);
        routerControl.routerURL(command,ctx,msg);
    }

    /**
     * @param msg msg to check
     * @return get the url to dispense to each handler
     *
     * make sure not use "/" to assemble the url otherwise the "/" will spilt your mark in the url
     * router may not find the current method to invoke
     */
    protected abstract  String getURL(T msg);

    /**
     * @return the specific Type RouterHandler instance
     * to init a NettyRouterHandler<T></T>
     * example:
     * NettyRouterHandler<RequestMessage> initControl(){
     *     return new NettyRouterHandler<RequestMessage>(RequestMessage.class);</>
     * }
     */
    protected abstract  NettyRouterHandler<T> initControl();

    /**
     * @param msg msg to check
     * @return the result weather the method will be exc next
     * if check failed will throw a RunTimeException
     */
    @Override
    public boolean check(T msg) {
        return true;
    }

    @Override
    public String handlerUri(String uri) {
        return uri;
    }

}
