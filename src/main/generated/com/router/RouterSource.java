package com.router;

import io.nettyrouter.annotation.NettyRouter;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.HashMap;
import java.util.Map;

public class RouterSource implements NettyRouter.RouterSource {
  private Map<String, Object> classInstances;

  private Map<String, String> methodMap;

  public RouterSource() {
    init();
  }

  private void init() {
    classInstances = new HashMap<>();
    methodMap = new HashMap<>();
    classInstances.put("ModHandler",new com.mods.sync.core.ModHandler());
    methodMap.put("GET:/modList","ModHandler$handle1");
    methodMap.put("GET:/mod/*","ModHandler$handle3");
    methodMap.put("GET:/favicon.ico","ModHandler$handle2");
  }

  @Override
  public Map<String, String> methodNameMap() {
    return methodMap;
  }

  @Override
  public Map<String, Object> classInstances() {
    return classInstances;
  }
}
