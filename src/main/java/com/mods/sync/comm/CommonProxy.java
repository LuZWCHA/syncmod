package com.mods.sync.comm;

import com.mods.sync.SyncMod;
import com.mods.sync.commands.SyncCommand;
import com.mods.sync.config.ConfigRef;
import com.mods.sync.core.Server;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.command.server.CommandBanPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.io.File;
import java.io.IOException;

import static com.mods.sync.SyncMod.CONFIG_REF;

public class CommonProxy {


    public void preInit(FMLPreInitializationEvent event) {
        File file =event.getSuggestedConfigurationFile();
        //Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        try {
            boolean notExited = file.createNewFile();
            Configuration config = new Configuration(file);
            if(notExited){
                config.addCustomCategoryComment(ConfigRef.Category,"set the server info");
                config.load();
                config.get(ConfigRef.Category,ConfigRef.PORT, CONFIG_REF.getPort());
                config.get(ConfigRef.Category,ConfigRef.CONNECTION_NUM, CONFIG_REF.getConnectionNum());
                config.get(ConfigRef.Category,ConfigRef.KEEP_ALIVE, CONFIG_REF.isKeepAlive());
                config.get(ConfigRef.Category,ConfigRef.SOURCE_PATH,CONFIG_REF.getSourcePath());
                config.get(ConfigRef.Category,ConfigRef.BAND_WIDTH,CONFIG_REF.getBandWidth());
                config.save();
            }else{
                config.load();
                CONFIG_REF.setPort(config.get(ConfigRef.Category,ConfigRef.PORT, CONFIG_REF.getPort()).getInt());
                CONFIG_REF.setConnectionNum(config.get(ConfigRef.Category,ConfigRef.CONNECTION_NUM, CONFIG_REF.getConnectionNum()).getInt());
                CONFIG_REF.setKeepAlive(config.get(ConfigRef.Category,ConfigRef.KEEP_ALIVE, CONFIG_REF.isKeepAlive()).getBoolean());
                CONFIG_REF.setSourcePath(config.get(ConfigRef.Category,ConfigRef.SOURCE_PATH,CONFIG_REF.getSourcePath()).getString());
                CONFIG_REF.setBandWidth(config.get(ConfigRef.Category,ConfigRef.BAND_WIDTH,CONFIG_REF.getBandWidth()).getInt());
                config.save();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void init(FMLInitializationEvent event) {

    }

    public void postInit(FMLPostInitializationEvent event) {
        SyncMod.Instance.HTTP_SERVER.start();
    }


}
