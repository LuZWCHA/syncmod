package io.nettyrouter.annotation;

import io.netty.channel.ChannelHandlerContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import static io.nettyrouter.utils.HttpMethod.GET;

public interface NettyRouter {

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.TYPE)
    @interface Router {

    };

    @Retention(RetentionPolicy.CLASS)
    @Target(ElementType.METHOD)
    @interface RouterHandler {
        String routerUri()default "/";
        String httpMethod()default GET;
        String extraContent() default "";
    }

    interface PreHandler<T> {
        String handlerUri(String uri);
        boolean check(T msg);
    }

    interface RouterProxy<T>{
        void routerURL(String url, ChannelHandlerContext ctx, T msg) throws Exception;
    }

    interface RouterSource{
        Map<String,Object> classInstances();
        Map<String,String> methodNameMap();
    }
}
