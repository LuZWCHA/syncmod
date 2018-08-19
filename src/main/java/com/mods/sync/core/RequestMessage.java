package com.mods.sync.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.reactivex.annotations.NonNull;

import java.nio.charset.Charset;

public class RequestMessage {
    //private boolean EMPTY = true;

    public String requestCommand;//20 byte

    public byte[] data;

    private RequestMessage(){
        requestCommand = null;
        data = null;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    public boolean isEmpty(){
        return requestCommand == null || requestCommand.isEmpty();
    }

    public static RequestMessage createRMObj(ByteBuf byteBuf){
        return createRMObj(byteBuf,"UTF-8");
    }

    public static RequestMessage createRMObj(@NonNull ByteBuf byteBuf, String charSetName){
        RequestMessage requestMessage = new RequestMessage();
        if(byteBuf.capacity() <= 0 || !byteBuf.isReadable())
            return requestMessage;

        int commandLength = byteBuf.readInt();
        requestMessage.requestCommand = byteBuf.readBytes(commandLength).toString(Charset.forName(charSetName));
        requestMessage.data = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(requestMessage.data);
        return requestMessage;
    }

    public ByteBuf asByteBuf(){
        if(requestCommand == null || requestCommand.isEmpty() || data.length <= 0)
            return Unpooled.EMPTY_BUFFER;
        ByteBuf byteBuf = Unpooled.buffer();
        int length = requestCommand.getBytes().length;
        byteBuf.writeByte(length);
        byteBuf.writeBytes(requestCommand.getBytes());
        byteBuf.writeBytes(data);
        return byteBuf;
    }
}
