package com.mods.sync.beans;

import net.minecraftforge.fml.common.ModContainer;

import java.io.Serializable;
import java.util.Objects;

public class TransMod implements Serializable {
    private String id;
    private String version;
    private short mode;
    private boolean fromServer;

    private TransMod(){

    }

    private TransMod(String id, String version, short mode, boolean fromServer){
        this.id = id;
        this.version = version;
        this.mode = mode;
        this.fromServer = fromServer;
    }

    public static TransMod create(){
        return new TransMod();
    }

    public static TransMod create(ModContainer container){
        return new TransMod(container.getModId(),container.getVersion(),Mod.MODE.UNKNOWN_MOD_MODE, true);//may be tweak mod or forge mod
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public short getMode() {
        return mode;
    }

    public void setMode(short mode) {
        this.mode = mode;
    }

    public void setFromServer(boolean fromServer){
        this.fromServer = fromServer;
    }

    public boolean isFromServer() {
        return fromServer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransMod)) return false;
        TransMod transMod = (TransMod) o;
        return Objects.equals(id, transMod.id) &&
                Objects.equals(version, transMod.version);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, version);
    }
}
