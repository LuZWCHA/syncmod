package com.mods.sync.utils;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponse;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static io.netty.handler.codec.http.HttpHeaderNames.*;

public class HttpUtils {

    public static boolean checkFileAccession(File file){
        return file!= null && file.exists() && file.canRead() && file.isFile();
    }

    public static Optional<Range> getRang(FullHttpRequest request){
        String rs = request.headers().get(RANGE);

        Range range = null;
        if(rs != null){
            System.out.println("range:"+ rs);
            try {
                String[] rs2 = rs.split("[=\\-]",-1);
                if(rs2.length > 2){
                    long start = Long.parseLong(rs2[1].isEmpty() ? "-1":rs2[1]);
                    long end = Long.parseLong(rs2[2].isEmpty() ? "-1":rs2[2]);
                    range = new Range(start,end);
                }
            }catch (Exception ignore){

            }

        }
        return Optional.ofNullable(range);
    }

    public static void setRangeAccept(HttpResponse response,long from,long end,long length){
        response.headers().set(CONTENT_RANGE,String.format("bytes %d-%d/%d",from ,end,length));
        response.headers().set(ACCEPT_RANGES,"bytes");
    }

    public static void setContentTypeHeader(HttpResponse response, File file) {
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        response.headers().set(CONTENT_TYPE,
                mimeTypesMap.getContentType(file.getName()));
        response.headers().set(CONTENT_DISPOSITION,"attachment;filename="+file.getName());
    }

    public static class Range{
        private long start;
        private long end;
        private boolean isEmpty = true;

        public Range(long start,long end){
            this.start = start;
            this.end = end;
            isEmpty = false;
        }

        public Range(){

        }

        public boolean isEmpty() {
            return isEmpty;
        }

        public long getEnd() {
            return end;
        }

        public long getStart() {
            return start;
        }
    }
}
