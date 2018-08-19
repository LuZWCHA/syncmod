package com.mods.sync.config;

import net.minecraftforge.fml.common.Loader;

//@Config(modid = SyncMod.MODID,name = SyncMod.NAME,type = Config.Type.INSTANCE)
public class ConfigRef {
    public static final String Category = "Server";
    public static final String PORT = "port";
    public static final String CONNECTION_NUM = "connectionNum";
    public static final String KEEP_ALIVE = "keepAlive";
    public static final String SOURCE_PATH = "sourcePath";
    public static final String BAND_WIDTH = "bandWidth";

    private int port;
    private int connectionNum;
    private boolean isKeepAlive;
    private String sourcePath;
    private int bandWidth;// MB/S

    public ConfigRef(){
        port = 0;
        connectionNum = 20;
        isKeepAlive = true;
        bandWidth = 2;

        if(Loader.instance().getConfigDir()!= null)
            sourcePath = Loader.instance().getConfigDir().getParent().concat("/mods");
        else
            setSourcePath("");
    }

    public int getConnectionNum() {
        return connectionNum;
    }

    public boolean isKeepAlive() {
        return isKeepAlive;
    }

    public void setConnectionNum(int connectionNum) {
        this.connectionNum = connectionNum;
    }

    public void setKeepAlive(boolean keepAlive) {
        isKeepAlive = keepAlive;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public int getBandWidth() {
        return bandWidth;
    }

    public void setBandWidth(int bandWidth) {
        this.bandWidth = bandWidth;
    }
}
