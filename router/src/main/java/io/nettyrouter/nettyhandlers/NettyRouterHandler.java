package io.nettyrouter.nettyhandlers;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.nettyrouter.annotation.NettyRouter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Logger;

public class NettyRouterHandler<T> implements NettyRouter.RouterProxy<T>{
    private Logger logger = Logger.getLogger(getClass().getSimpleName());
    private Class<T> TClass;
    private NettyRouter.PreHandler<T> preHandler;

    public NettyRouterHandler(Class<T> tClass){
        TClass = tClass;
    }

    @Override
    public void routerURL(String url, ChannelHandlerContext ctx, T msg) throws Exception{
        if(RouterMap.INSTANCE.isEmpty()) {
            routerMatchedFailed(ctx,url);
            return;
        }
        url = preHandler(url);
        Optional<String> methodWrapName = RouterMap.INSTANCE.find(url);

        if(!methodWrapName.isPresent()){
            routerMatchedFailed(ctx,url);
        }else {
            String[] strings = methodWrapName.get().split("\\u0024");//split by '$'
            if(strings.length != 2) {
                routerMethodExecuteFailed(ctx,url);
                return;
            }
            String className = strings[0];
            String methodName = strings[1];
            Object instance = RouterMap.INSTANCE.getSource().classInstances().get(className);
            if(instance == null) {
                routerMethodExecuteFailed(ctx,url);
                return;
            }
            try {
                //method is known as a nonnull Method
                Method method = instance.getClass().getMethod(methodName,ChannelHandlerContext.class,TClass);
                if(!check(method,ctx,msg))
                    throw new RuntimeException("source check failed");
                method.invoke(instance,ctx,msg);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                routerMethodExecuteFailed(ctx,url);
                throw new RuntimeException("method invoke failed:" + e.getCause().getMessage());
            }
        }
    }

    private boolean check(Method method,ChannelHandlerContext ctx, T msg)throws Exception {
        if(preHandler != null)
            return preHandler.check(msg);
        return true;
    }

    private String preHandler(String uri){
        if(preHandler != null)
            uri = preHandler.handlerUri(uri);
        return uri;
    }

    //below methods are HTTP 1.1 Respond
    private void routerMethodExecuteFailed(ChannelHandlerContext ctx,String url){
        logger.warning(url+": matched, but exec failed,check the fun and make true it's para is right,and it shouldn't throws exceptions");
        respond(ctx,HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    private void routerMatchedFailed(ChannelHandlerContext ctx,String url){
        logger.warning(url+": nothing matched");
        respond(ctx,HttpResponseStatus.NOT_FOUND);
    }

    private void respond(ChannelHandlerContext ctx,HttpResponseStatus status){
        logger.warning("failed response send");
        ctx.writeAndFlush(new DefaultHttpResponse(HttpVersion.HTTP_1_1,status))
                .addListener(ChannelFutureListener.CLOSE);
    }

    public void setPreHandler(NettyRouter.PreHandler<T> preHandler) {
        this.preHandler = preHandler;
    }
}
