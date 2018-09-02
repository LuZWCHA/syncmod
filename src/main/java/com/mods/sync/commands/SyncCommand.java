package com.mods.sync.commands;

import com.mods.sync.SyncMod;
import com.mods.sync.config.UrlSourceRef;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SyncCommand extends CommandBase {
    private final static String START = "start";
    private final static String STOP = "stop";
    private final static String READ_URLS = "readurls";
    private final static String SAVE_URLS = "saveurls";
    private final static String MODIFY_URL = "modifyurl";
    private final static String LIST_URL = "listurl";
    private final static String REMOVE_URL = "removeurl";

    @Override
    public String getName() {
        return "sync";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 3;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "com.com.mods.sync.usage";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

        if (args.length <= 0)
        {
            throw new WrongUsageException("com.com.mods.sync.usage");
        }
        if(server.worlds!=null){
            if(args.length == 1) {
                if (START.equals(args[0])) {
                    if(startServer())
                        notifyCommandListener(sender,this,"com.mods.sync.start.success", new TextComponentTranslation("com.mods.sync.server.text"));
                    else
                        notifyCommandListener(sender,this,"com.mods.sync.start.failed", new TextComponentTranslation("com.mods.sync.server.text"));

                } else if (STOP.equals(args[0])) {
                    if(stopServer())
                        notifyCommandListener(sender,this,"com.mods.sync.stop.success", new TextComponentTranslation("com.mods.sync.server.text"));
                    else
                        notifyCommandListener(sender,this,"com.mods.sync.stop.failed", new TextComponentTranslation("com.mods.sync.server.text"));

                }else if(LIST_URL.equals(args[0])){
                    StringBuilder builder = new StringBuilder();
                    builder.append("----------------------------------------------\n");
                    UrlSourceRef.getUrlSource().forEach(new BiConsumer<String, String>() {
                        @Override
                        public void accept(String s, String s2) {
                            builder.append(s)
                                    .append(":")
                                    .append(s2)
                                    .append("\n");
                        }
                    });
                    builder.append("----------------------------------------------\n");
                    String list = builder.toString().replaceAll("%","%%");

                    notifyCommandListener(sender, this, "com.mods.sync.url.list:\n" + list, new TextComponentTranslation("com.mods.sync.server.text"));
                }else if(READ_URLS.equals(args[0])){
                    boolean rs = UrlSourceRef.read();
                    notifyCommandListener(sender,this,"com.mods.sync.url.read "+(rs?"success":"failed"), new TextComponentTranslation("com.mods.sync.server.text"));
                }else if(SAVE_URLS.equals(args[0])) {
                    boolean rs = UrlSourceRef.save();
                    notifyCommandListener(sender, this, "com.mods.sync.url.save " + (rs ? "success" : "failed"), new TextComponentTranslation("com.mods.sync.server.text"));
                } else {
                    throw new WrongUsageException("com.com.mods.sync.usage");
                }
            }else if(args.length == 2){
                if (REMOVE_URL.equals(args[0])){
                    if(UrlSourceRef.getUrlSource().remove(args[1]) != null){
                        notifyCommandListener(sender, this, "com.mods.sync.url.removed successful", new TextComponentTranslation("com.mods.sync.server.text"));
                    }
                } else {
                    throw new WrongUsageException("modify failed:not find "+args[1]);
                }
            }
            else if(args.length == 3){
                if(MODIFY_URL.equals(args[0])){
                    if(SyncMod.Instance.getModList().containsKey(args[1])) {
                        UrlSourceRef.getUrlSource().put(args[1], args[2]);
                        String url = args[2].replaceAll("%","%%");
                        notifyCommandListener(sender, this, "com.mods.sync.url.modify "+args[1] +"'s url to "+url, new TextComponentTranslation("com.mods.sync.server.text"));
                    } else {
                        throw new WrongUsageException("modify failed:not find "+args[1]);
                    }
                }else{
                    throw new WrongUsageException("com.com.mods.sync.usage");
                }
            } else {
                throw new WrongUsageException("com.com.mods.sync.usage");
            }
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if(args.length == 1) {
            return getListOfStringsMatchingLastWord(args, START, STOP, MODIFY_URL, LIST_URL, READ_URLS, SAVE_URLS, REMOVE_URL);
        }else if(args.length == 2 && (args[0].equals(MODIFY_URL)|| args[0].equals(REMOVE_URL))){
            return getListOfStringsMatchingLastWord(args,SyncMod.Instance.getModList().keySet());
        }

        return Collections.emptyList();
    }

    private boolean startServer(){
        if(SyncMod.Instance.HTTP_SERVER.getState() == -1) {
            SyncMod.Instance.HTTP_SERVER.start();
            return true;
        }
        return false;
    }

    private boolean stopServer(){
        if(SyncMod.Instance.HTTP_SERVER.getState() == 1) {
            SyncMod.Instance.HTTP_SERVER.stop();
            return true;
        }
        return false;
    }

}
