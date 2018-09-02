package com.mods.sync.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mods.sync.SyncMod;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class UrlSourceRef {
    private final static String SAVE_PATH = SyncMod.CONFIG_REF.getSourcePath();
    private final static String FILE_NAME="sync_urls.usr";
    private static Map<String,String> urlSource = new HashMap<>();

    public static Map<String, String> getUrlSource() {
        return urlSource;
    }

    public static boolean read(){
        File file = new File(SAVE_PATH+"/"+FILE_NAME);

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        try(Reader reader = new FileReader(file)) {
            Map<String,String> temp = gson.fromJson(reader,new TypeToken<HashMap<String,String>>() {}.getType());
            if(temp != null)
                temp.forEach((s, s2) -> {
                    if(urlSource.containsKey(s)){
                        urlSource.put(s,s2);
                    }
                });
            else{
                return false;
            }
        } catch (IOException e) {
            //read failed
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean save(){
        File file = new File(SAVE_PATH+"/"+FILE_NAME);
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try (Writer writer = new FileWriter(file)){
            gson.toJson(urlSource,new TypeToken<HashMap<String,String>>() {}.getType(),writer);

        } catch (IOException e) {
            //save failed
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
