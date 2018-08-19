package io.nettyrouter.utils;

import javax.annotation.Nonnull;
import java.util.Optional;

@SuppressWarnings({"unused"})
public class UrlParameterObtain {
    
    public static Optional<String> getMetaDataOfUrl(String origin, String w,int index)throws ArrayIndexOutOfBoundsException{
        return Optional.ofNullable(origin.split(w)[index]);
    }

    @Nonnull
    public static Optional<String[]> getAllMetaDataOfUrl(String origin, String w){
        return Optional.of(origin.split(w));
    }

    public static Optional<String> getMetaDataOfUrl(String origin,int index)throws ArrayIndexOutOfBoundsException{
        return Optional.ofNullable(origin.split("/")[index]);
    }

    @Nonnull
    public static Optional<String[]> getAllMetaDataOfUrl(String origin){
        return Optional.of(origin.split("/"));
    }
}
