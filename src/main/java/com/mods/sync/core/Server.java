package com.mods.sync.core;

import com.mods.sync.SyncMod;
import com.mods.sync.config.ConfigRef;
import com.mods.sync.utils.PlatformUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.traffic.GlobalChannelTrafficShapingHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraftforge.fml.common.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.StringFormattedMessage;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;


public class Server {
    private int port;
    private ConfigRef configRef;
    private Logger logger;
    private ChannelFuture channelFuture;

    private EventLoopGroup boss;
    private EventLoopGroup worker;

    private volatile static int state;
    private static long MAX_MEMORY;
    final int M = 1024 * 1024;


    public Server(){
        init(null);
    }

    public Server(@Nonnull ConfigRef configRef){
        init(configRef);
    }

    private void init(ConfigRef configRef){
        MAX_MEMORY = Runtime.getRuntime().freeMemory();
        if(configRef == null)
            configRef = new ConfigRef();
        this.configRef = configRef;
        Optional.ofNullable(SyncMod.Instance).ifPresent(syncMod -> logger = SyncMod.logger);
        logger = Optional.ofNullable(logger).orElse(LogManager.getLogger());
        state = -1;
    }

    private void bind() {
        state = 0;
        boss = PlatformUtil.OSinfo.isLinux() ?new EpollEventLoopGroup():new NioEventLoopGroup();
        worker = PlatformUtil.OSinfo.isLinux() ?new EpollEventLoopGroup():new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
                    .channel(PlatformUtil.OSinfo.isLinux() ? EpollServerSocketChannel.class:NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, configRef.getConnectionNum())
                    //set water mark to avoid OOM
                    .option(ChannelOption.WRITE_BUFFER_WATER_MARK,new WriteBufferWaterMark(0, (int) (MAX_MEMORY * 0.3f)))//30% of MAX_MEMORY can be used
                    .childOption(ChannelOption.SO_KEEPALIVE, configRef.isKeepAlive())
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel socketChannel)
                                    throws Exception {
                                ChannelPipeline p = socketChannel.pipeline();
                                GlobalChannelTrafficShapingHandler handler = new GlobalChannelTrafficShapingHandler(socketChannel.eventLoop().parent(),100);
                                handler.setReadLimit(20*M);
                                long writeLimit = (long) SyncMod.CONFIG_REF.getBandWidth()* M / 8 ;
                                handler.setWriteLimit(writeLimit);
                                long writeSingleLimit = (long) (writeLimit / (double)SyncMod.CONFIG_REF.getConnectionNum())* 2;
                                handler.setWriteChannelLimit(writeSingleLimit);
                                handler.setReadChannelLimit(2*M);

                                p.addLast(new LoggingHandler(LogLevel.DEBUG))
                                        .addLast(handler)
                                        .addLast(new HttpRequestDecoder())
                                        .addLast(new HttpObjectAggregator(65536))
                                        .addLast(new HttpResponseEncoder())
                                        .addLast(new ChunkedWriteHandler())
                                        .addLast(new HttpRouterHandler());
                            }
                        });

            channelFuture = bootstrap.bind(configRef.getPort()).sync();
            channelFuture.addListener(new GenericFutureListener<Future<? super Void>>() {
                @Override
                public void operationComplete(Future<? super Void> future) throws Exception {
                    if (future.isSuccess()) {
                        logger.info(new StringFormattedMessage("server start at "+ channelFuture.channel().localAddress().toString()));
                    } else {
                        Throwable cause = future.cause();
                        cause.printStackTrace();
                        // do something....
                    }
                }
            });
            state = 1;
            channelFuture.channel().closeFuture().sync();

        } catch (Exception e) {
            logger.error(new StringFormattedMessage("server stop"));
            e.printStackTrace();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
            state = -1;
        }
    }

    public void startSync(){
        bind();
    }

    public void start(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                bind();
            }
        });
        thread.start();
    }

    public void stop(){
        if(boss != null)
            boss.shutdownGracefully();
        if(worker != null)
            worker.shutdownGracefully();
        logger.info("Stop Http Server");
    }

    public int getState(){
        return state;
    }

}
