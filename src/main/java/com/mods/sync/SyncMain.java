package com.mods.sync;

import com.mods.sync.core.Server;

public class SyncMain {
    public static void main(String[] args) {
        Server server = new Server();
        server.startSync();
    }
}
