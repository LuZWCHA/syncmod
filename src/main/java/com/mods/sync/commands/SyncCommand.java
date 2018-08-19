package com.mods.sync.commands;

import com.mods.sync.SyncMod;
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

public class SyncCommand extends CommandBase {
    @Override
    public String getName() {
        return "com/mods/sync";
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
                if ("start".equals(args[0])) {
                    if(startServer())
                        notifyCommandListener(sender,this,"com.mods.sync.start.success", new TextComponentTranslation("com.mods.sync.server.text"));
                    else
                        notifyCommandListener(sender,this,"com.mods.sync.start.failed", new TextComponentTranslation("com.mods.sync.server.text"));

                } else if ("stop".equals(args[0])) {
                    if(stopServer())
                        notifyCommandListener(sender,this,"com.mods.sync.stop.success", new TextComponentTranslation("com.mods.sync.server.text"));
                    else
                        notifyCommandListener(sender,this,"com.mods.sync.stop.failed", new TextComponentTranslation("com.mods.sync.server.text"));

                }
            }else {
                throw new WrongUsageException("com.com.mods.sync.usage");
            }
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "start", "stop") : Collections.emptyList();
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
