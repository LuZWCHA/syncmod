package com.mods.sync.utils;

import java.io.UnsupportedEncodingException;

public class PathUtils {
    public static String getJarPath(){
        String jarWholePath = PathUtils.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        try {
            jarWholePath = java.net.URLDecoder.decode(jarWholePath, "UTF-8");
        } catch (UnsupportedEncodingException e) { System.out.println(e.toString()); }
        return jarWholePath;
    }
}
