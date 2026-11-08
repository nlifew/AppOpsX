package com.zzzmode.appopsx.common;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by zl on 2016/11/6.
 */

public class ReflectUtils {

  private static final Map<String, Field> sFieldCache =
      Collections.synchronizedMap(new HashMap<String, Field>());
  private static final Map<String, Method> sMethodCache =
      Collections.synchronizedMap(new HashMap<String, Method>());

  /** AppOpsManager.OP_FLAGS_ALL：统计所有来源（自身、代理、被代理）的访问 */
  private static final int OP_FLAGS_ALL = 0x1F;

  /**
   * 把系统的 AppOpsManager.PackageOps 转换成我们自己的 PackageOps。
   * 只使用 Android 11 起稳定的 @SystemApi getter，不读私有字段（私有字段在 10、11、13 都变过）。
   */
  public static PackageOps opsConvert(Object packageOps) throws ReflectiveOperationException {
    String packageName = (String) callPublic(packageOps, "getPackageName");
    int uid = (Integer) callPublic(packageOps, "getUid");
    List<?> ops = (List<?>) callPublic(packageOps, "getOps");

    List<OpEntry> entries = new ArrayList<>();
    if (ops != null) {
      for (Object o : ops) {
        int op = (Integer) callPublic(o, "getOp");
        int mode = (Integer) callPublic(o, "getMode");
        long time = (Long) callPublic(o, "getLastAccessTime", OP_FLAGS_ALL);
        long rejectTime = (Long) callPublic(o, "getLastRejectTime", OP_FLAGS_ALL);
        long duration = (Long) callPublic(o, "getLastDuration", OP_FLAGS_ALL);
        boolean running = (Boolean) callPublic(o, "isRunning");

        int proxyUid = -1;
        String proxyPackageName = null;
        Object proxyInfo = callPublic(o, "getLastProxyInfo", OP_FLAGS_ALL);
        if (proxyInfo != null) {
          proxyUid = (Integer) callPublic(proxyInfo, "getUid");
          proxyPackageName = (String) callPublic(proxyInfo, "getPackageName");
        }

        // 我们的 OpEntry 用 duration == -1 表示正在使用
        entries.add(new OpEntry(op, mode, time, rejectTime, running ? -1 : (int) duration,
            proxyUid, proxyPackageName));
      }
    }

    return new PackageOps(packageName, uid, entries);
  }

  /** 调用 public 方法，Method 按“类名#方法名”缓存；找不到方法时直接抛出，不返回 null */
  private static Object callPublic(Object obj, String methodName, Object... args)
      throws ReflectiveOperationException {
    Class<?> cls = obj.getClass();
    String key = cls.getName() + "#" + methodName + "/" + args.length;
    Method method;
    synchronized (sMethodCache) {
      method = sMethodCache.get(key);
      if (method == null) {
        for (Method m : cls.getMethods()) {
          if (m.getName().equals(methodName) && m.getParameterTypes().length == args.length) {
            method = m;
            break;
          }
        }
        if (method == null) {
          throw new NoSuchMethodException(key);
        }
        sMethodCache.put(key, method);
      }
    }
    return method.invoke(obj, args);
  }

  public static Object getFieldValue(Object obj, String fieldName) {
    Class<?> cls = obj instanceof Class ? (Class<?>) obj : obj.getClass();
    String key = cls.getName() + "#" + fieldName;
    Field field = sFieldCache.get(key);
    if (field == null && !sFieldCache.containsKey(key)) {
      try {
        field = cls.getDeclaredField(fieldName);
        field.setAccessible(true);
      } catch (Exception e) {
        e.printStackTrace();
      }
      // 找不到的字段也记下来（值为 null），避免每次调用都重新抛异常
      sFieldCache.put(key, field);
    }
    if (field != null) {
      try {
        return field.get(obj);
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  public static Object invokMethod(Object object, String methodName, List<Class> paramsTypes,
      List<Object> params) {
    StringBuilder sb = new StringBuilder(methodName);
    if (paramsTypes != null && !paramsTypes.isEmpty()) {
      sb.append("-");
      for (Class aClass : paramsTypes) {
        sb.append(aClass.getSimpleName()).append(",");
      }
    }

    Method method = sMethodCache.get(sb.toString());
    if (method == null) {
      try {
        Class cls = null;

        if (object instanceof Class) {
          //static method
          cls = ((Class) object);
        } else {
          cls = object.getClass();
        }

        if (paramsTypes != null && !paramsTypes.isEmpty()) {
          method = cls.getDeclaredMethod(methodName, paramsTypes.toArray(new Class[paramsTypes.size()]));
        } else {
          method = cls.getDeclaredMethod(methodName);

        }
        method.setAccessible(true);


        sMethodCache.put(sb.toString(), method);
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
      }
    }
    if (method != null) {
      try {
        if (params != null && !params.isEmpty()) {
          return method.invoke(object, params.toArray());
        } else {
          return method.invoke(object);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return null;
  }



  public static boolean hasField(Class cls,String fieldName){
    try {
      Field field = cls.getDeclaredField(fieldName);
      return true;
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
    }
    return false;
  }
}
