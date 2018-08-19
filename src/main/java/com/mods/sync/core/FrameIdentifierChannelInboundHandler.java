package com.mods.sync.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import javafx.fxml.LoadException;

public class FrameIdentifierChannelInboundHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private byte[] frameHead;
    private int frameHeadLength;
    private int frameBodyLength;
    private FrameReceivedEnum frameStatus = FrameReceivedEnum.READY;
    private ByteBuf holdByteBuf = Unpooled.buffer(4100);//4096+2+1+ext

    private FrameIdentifierChannelInboundHandler(){

    }

    public FrameIdentifierChannelInboundHandler(byte... frameHead) {
        this();
        this.frameHead = frameHead;
        frameHeadLength = frameHead.length;
    }

    @Override
    protected void channelRead0 (ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        holdByteBuf.writeBytes(msg);
        while (true) {
            if (frameStatus == FrameReceivedEnum.READY) {
                if (!matchFrameHead(holdByteBuf)) {
                    holdByteBuf.clear();
                    break;
                }
            }

            if (frameStatus == FrameReceivedEnum.READING_LENGTH) {
                if (holdByteBuf.readableBytes() <= 1) break;
                int currentFrameLength = holdByteBuf.getUnsignedShort(holdByteBuf.readerIndex());

                if (currentFrameLength + 2 <= holdByteBuf.readableBytes()) {
                    frameBodyLength = holdByteBuf.readUnsignedShort();
                    frameStatus = FrameReceivedEnum.READING_BODY;
                } else {
                    break;
                }
            }

            if (frameStatus == FrameReceivedEnum.READING_BODY) {
                if (frameBodyLength == 0) {
                    frameStatus = FrameReceivedEnum.READY;
                    frameBodyLength = -1;
                    holdByteBuf.discardReadBytes();
                } else if (frameBodyLength > 0) {
                    ByteBuf returnBuf = Unpooled.buffer(frameBodyLength);
                    holdByteBuf.readBytes(returnBuf);
                    frameStatus = FrameReceivedEnum.READY;
                    ctx.writeAndFlush(returnBuf);
                    frameBodyLength = -1;
                    holdByteBuf.discardReadBytes();

                } else {
                    throw new FrameLoadException("自定义帧长度计数异常");
                }
            } else {
                throw new FrameLoadException("自定义帧读取异常");
            }
        }
    }

    private boolean matchFrameHead(ByteBuf byteBuf) {
        while (true) {
            if (byteBuf.readableBytes() < frameHeadLength) {
                return false;
            }
            if (frameHead[0] == byteBuf.readByte()) {
                frameStatus = FrameReceivedEnum.READING_LENGTH;
                return true;
            }
        }
    }

    private enum  FrameReceivedEnum{
        READING_LENGTH,
        READING_BODY,
        READY
    }

    public static class FrameLoadException extends Exception{

        public FrameLoadException(){
            super();
        }

        public FrameLoadException(String s){
            super(s);
        }
    }
}