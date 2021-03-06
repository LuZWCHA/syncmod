package com.mods.sync.core;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.mods.sync.SyncMod;
import com.mods.sync.beans.TransMod;
import com.mods.sync.config.UrlSourceRef;
import com.mods.sync.utils.HttpUtils;
import com.mods.sync.utils.PathUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import io.nettyrouter.annotation.NettyRouter;
import io.nettyrouter.utils.HttpMethod;
import io.nettyrouter.utils.UrlParameterObtain;
import joptsimple.internal.Strings;
import net.minecraftforge.fml.common.ModContainer;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

@NettyRouter.Router
public class ModHandler{

    private static final org.apache.logging.log4j.Logger log = LogManager.getLogger(ModHandler.class);

    @NettyRouter.RouterHandler(routerUri = "/modList")
    public void handle1(ChannelHandlerContext ctx,FullHttpRequest msg){
        Gson gson = new Gson();
        Map<String,ModContainer> modContainers = SyncMod.Instance.getModList();

        List<TransMod> mods = new ArrayList<>();
        modContainers.forEach((key, modContainer) -> mods.add(TransMod.create(modContainer)));

        String jsonString = gson.toJson(mods);
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeBytes(jsonString.getBytes());
        HttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,OK,byteBuf);
        response.headers().set(CONTENT_TYPE,HttpHeaderValues.APPLICATION_JSON);
        if (HttpUtil.isKeepAlive(msg)) {
            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @NettyRouter.RouterHandler(routerUri = "/mod/*")
    public void handle3(ChannelHandlerContext ctx,FullHttpRequest msg){
        try {
            String uri = URLDecoder.decode(msg.uri(), "UTF-8");
            String modName = UrlParameterObtain.getMetaDataOfUrl(uri,2).orElse("");
            String url = UrlSourceRef.getUrlSource().get(modName);
            // TODO: 2018/8/31 add a statement to respond with HTTP code 302 if possible
            if(!Strings.isNullOrEmpty(url)){
                String location = UrlSourceRef.getUrlSource().get(modName);
                HttpResponse response = new DefaultHttpResponse(HTTP_1_1,FOUND);
                response.headers().set(LOCATION,location);
                if (HttpUtil.isKeepAlive(msg)) {
                    response.headers().set(CONNECTION, KEEP_ALIVE);
                }

                ChannelFuture channelFuture = ctx.writeAndFlush(response);

                if (!HttpUtil.isKeepAlive(msg)) {
                    channelFuture.addListener(ChannelFutureListener.CLOSE);
                }
                return;
            }

            File file = SyncMod.Instance.getModList().get(modName).getSource();

            if(!HttpUtils.checkFileAccession(file)) {
                sendError(ctx, NO_CONTENT);
                return;
            }
            RandomAccessFile randomAccessFile;

            try {
                randomAccessFile = new RandomAccessFile(file,"r");
            } catch (FileNotFoundException e) {
                sendError(ctx, NOT_FOUND);
                return;
            }
            final long fileLength = randomAccessFile.length();
            final HttpUtils.Range range = HttpUtils.getRang(msg).orElse(new HttpUtils.Range());

            HttpResponse response = new DefaultHttpResponse(HTTP_1_1, range.isEmpty() ? OK:PARTIAL_CONTENT);
            long start = 0;
            long end = fileLength - 1;
            if(!range.isEmpty()) {
                start = range.getStart() == -1 ? fileLength - range.getEnd() : range.getStart();
                end = range.getEnd() == -1 ? end : range.getEnd();
                HttpUtils.setRangeAccept(response,start ,end, fileLength);
            }

            HttpUtil.setContentLength(response, end - start + 1);
            HttpUtils.setContentTypeHeader(response, file);

            if (HttpUtil.isKeepAlive(msg)) {
                response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }

            log.info("download "+ file.getName() +" from:" + getIpAddress(ctx,msg) + " range:"+start+"-"+end);
            ctx.write(response);

            ctx.write(new HttpChunkedInput(new ChunkedFile(randomAccessFile, start, end - start + 1, 8192)), ctx.newProgressivePromise())
                    .addListener(
                            new ChannelProgressiveFutureListener() {
                                @Override
                                public void operationProgressed(ChannelProgressiveFuture future,
                                                                long progress, long total) {
                                }

                                @Override
                                public void operationComplete(ChannelProgressiveFuture future)
                                        throws Exception {
                                    if(future.isDone() && future.isSuccess())
                                        log.info(file.getName()+" download complete.");
                                    else {
                                        log.warn("stopped");
                                        sendError(ctx,INTERNAL_SERVER_ERROR);
                                    }
                                }
            });
            ChannelFuture lastContentFuture = ctx
                    .writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);

            if (!HttpUtil.isKeepAlive(msg)) {
                lastContentFuture.addListener(ChannelFutureListener.CLOSE);
            }

        } catch (IOException e) {
            e.printStackTrace();
            sendError(ctx,HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @NettyRouter.RouterHandler(httpMethod = HttpMethod.HEAD,routerUri = "/mod/*")
    public void handle4(ChannelHandlerContext ctx,FullHttpRequest msg){
        try {
            String uri = URLDecoder.decode(msg.uri(), "UTF-8");
            String modName = UrlParameterObtain.getMetaDataOfUrl(uri,2).orElse("");

            File file = SyncMod.Instance.getModList().get(modName).getSource();

            if(!HttpUtils.checkFileAccession(file)) {
                sendError(ctx, NO_CONTENT);
                return;
            }

            final long fileLength = file.length();
            final HttpUtils.Range range = HttpUtils.getRang(msg).orElse(new HttpUtils.Range());

            HttpResponse response = new DefaultHttpResponse(HTTP_1_1, range.isEmpty() ? OK:PARTIAL_CONTENT);
            long start = 0;
            long end = fileLength - 1;
            if(!range.isEmpty()) {
                start = range.getStart() == -1 ? fileLength - range.getEnd() : range.getStart();
                end = range.getEnd() == -1 ? end : range.getEnd();
                HttpUtils.setRangeAccept(response,start ,end, fileLength);
            }

            HttpUtil.setContentLength(response, end - start + 1);
            HttpUtils.setContentTypeHeader(response, file);

            if (HttpUtil.isKeepAlive(msg)) {
                response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            }

            ChannelFuture channelFuture = ctx.writeAndFlush(response);

            if (!HttpUtil.isKeepAlive(msg)) {
                channelFuture.addListener(ChannelFutureListener.CLOSE);
            }
        } catch (IOException e) {
            e.printStackTrace();
            sendError(ctx,HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }

    }


    @NettyRouter.RouterHandler(routerUri = "/favicon.ico")
    public void handle2(ChannelHandlerContext ctx,FullHttpRequest msg){

        ByteBuf byteBuf = Unpooled.buffer();
        final byte[] iconBytes = loadIconBytes("images/favicon.ico");
        if(iconBytes == null)
            return;
        final DefaultHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.copiedBuffer(iconBytes));
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, iconBytes.length);
        response.headers().set(CONTENT_TYPE, "image/x-icon");
        byteBuf.writeBytes(iconBytes);

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static byte[] loadIconBytes(final String iconFilePath) {
        String jarPath = PathUtils.getJarPath();
        File file = new File(jarPath+"/"+iconFilePath);
        InputStream inputStream ;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            log.error("Failed to find icon file {}", iconFilePath);
            return null;
        }

        try {
            return ByteStreams.toByteArray(inputStream);
        } catch (IOException e) {
            log.error("Failed to load icon file {}", iconFilePath);
            return null;
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void sendError(ChannelHandlerContext ctx,
                                  HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1,
                status);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    //copy from internet
    private static String getIpAddress(ChannelHandlerContext ctx,FullHttpRequest request) {
        String Xip = request.headers().get("X-Real-IP");
        String XFor = request.headers().get("X-Forwarded-For");
        if(StringUtils.isNotEmpty(XFor) && !"unKnown".equalsIgnoreCase(XFor)){
            //多次反向代理后会有多个ip值，第一个ip才是真实ip
            int index = XFor.indexOf(",");
            if(index != -1){
                return XFor.substring(0,index);
            }else{
                return XFor;
            }
        }
        XFor = Xip;
        if(StringUtils.isNotEmpty(XFor) && !"unKnown".equalsIgnoreCase(XFor)){
            return XFor;
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.headers().get("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.headers().get("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.headers().get("HTTP_CLIENT_IP");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            XFor = request.headers().get("HTTP_X_FORWARDED_FOR");
        }
        if (StringUtils.isBlank(XFor) || "unknown".equalsIgnoreCase(XFor)) {
            InetSocketAddress insocket = (InetSocketAddress) ctx.channel()
                    .remoteAddress();
            XFor = insocket.getAddress().getHostAddress();
        }
        return XFor;
    }
}
