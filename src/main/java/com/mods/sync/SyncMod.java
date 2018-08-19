package com.mods.sync;

import com.mods.sync.comm.CommonProxy;
import com.mods.sync.commands.SyncCommand;
import com.mods.sync.config.ConfigRef;
import com.mods.sync.core.Server;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;
@SuppressWarnings({"unused", "WeakerAccess"})
@Mod(modid = SyncMod.MODID,version = SyncMod.VERSION,name = SyncMod.NAME,acceptedMinecraftVersions = "[1.7.10,1.12.2]" )
public class SyncMod{
    public static final String MODID = "synctool";
    public final static String NAME = "SyncTool";
    public final static String VERSION = "1.0";
    public static Logger logger;
    private Map<String,ModContainer> modList;
    public static final ConfigRef CONFIG_REF = new ConfigRef();
    public Server HTTP_SERVER;

    @Mod.Instance
    public static SyncMod Instance;

    @SidedProxy(clientSide = "com.mods.sync.client.ClientProxy",
            serverSide = "com.mods.sync.comm.CommonProxy")
    public static CommonProxy proxy;

    public Map<String,ModContainer> getModList() {
        return Optional.ofNullable(modList).orElse(new HashMap<>());
    }

    @NetworkCheckHandler
    public boolean checkModLists(Map<String,String> modList, Side side)
    {
        //always not needed on a client
        return true;
    }

    @Mod.EventHandler
    public void FMLStart(FMLServerStartingEvent event){
        event.registerServerCommand(new SyncCommand());
        //event.getServer().getServerThread().getId();
    }

    @Mod.EventHandler
    public void FMLStop(FMLServerStoppingEvent event){
        HTTP_SERVER.stop();
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
        HTTP_SERVER = new Server(CONFIG_REF);
        logger = event.getModLog();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {

        Map<String,String> map = new HashMap<>();
        modList = new HashMap<>();
        List<Pair<ModContainer,Boolean>> list = NetworkRegistry.INSTANCE.registry().entrySet().stream()
                .map(modContainerNetworkModHolderEntry -> Pair.of(modContainerNetworkModHolderEntry.getKey(), modContainerNetworkModHolderEntry.getValue().check(map, Side.CLIENT)))
                .filter(stringStringPair -> !stringStringPair.getValue()).collect(Collectors.toList());

        if(!list.isEmpty())
            logger.info("-----| the mods list below must be installed on client |-----");

        list.forEach(modContainerStringPair -> {

            modList.put(modContainerStringPair.getKey().getModId().trim().toLowerCase(),modContainerStringPair.getKey());
            logger.info(modContainerStringPair.getKey().getModId().toLowerCase().trim()
                    +" version: "+modContainerStringPair.getKey().getVersion().trim());
        });
        if(!modList.isEmpty())
            logger.info("-------------------------------------------------------------");

        proxy.postInit(event);
    }



}
