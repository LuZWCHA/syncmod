package io.nettyrouter.nettyhandlers;

import io.nettyrouter.annotation.NettyRouter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public enum  RouterMap {
    INSTANCE;
    private NettyRouter.RouterSource source;
    private HashSet<String> metadataSet;
    private static String WILDCARD = null;
    public final static String ROUTER_SOURCE_NAME = "com.router.RouterSource";

    public boolean init(String wildcard) throws Exception{
        if(WILDCARD != null)
            return true;
        WILDCARD = wildcard;
        return metadataSet.add(WILDCARD);
    }

    RouterMap(){
        source = createSource();
        metadataSet = new HashSet<>(64,1);

        //create metadata table
        if(source != null && source.methodNameMap() != null && !source.methodNameMap().isEmpty()){
            source.methodNameMap().forEach(new BiConsumer<String, String>() {
                @Override
                public void accept(String s, String s2) {
                    String[] slist = s.split("/");
                    metadataSet.addAll(Arrays.asList(slist));
                }
            });
        }
    }

    public NettyRouter.RouterSource getSource() {
        return source;
    }

    static NettyRouter.RouterSource createSource(){
        final NettyRouter.RouterSource[] source = new NettyRouter.RouterSource[1];
        try {
            Class clazz = NettyRouterHandler.class.getClassLoader().loadClass(ROUTER_SOURCE_NAME);
            Optional.ofNullable(clazz).ifPresent(new Consumer<Class>() {

                @Override
                public void accept(Class aClass) {
                    try {
                        source[0] = (NettyRouter.RouterSource) clazz.newInstance();
                        System.out.println("proxy created");

                    } catch (InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                        System.out.println("proxy create failed");
                    }
                }
            });
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("proxy create failed");
        }
        return source[0];
    }

    // O(2*length)
    public Optional<String> find(String path){

        String[] metadataList = path.split("/");

        StringBuilder temp = new StringBuilder();
        for(int i = 0;i<metadataList.length;i++){
            temp.append(metadataSet.contains(metadataList[i]) ? metadataList[i] : WILDCARD );
            if(i != metadataList.length - 1){
                temp.append("/");
            }
        }

        return Optional.ofNullable(source.methodNameMap().get(temp.toString()));
    }

    public boolean isEmpty(){
        return source.methodNameMap().isEmpty() || source.classInstances().isEmpty() || metadataSet.isEmpty();
    }
}
