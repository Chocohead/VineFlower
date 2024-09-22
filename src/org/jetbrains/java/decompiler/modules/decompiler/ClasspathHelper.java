// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler;

import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.DataInputFullStream;
import org.jetbrains.java.decompiler.util.InterpreterUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ClasspathHelper {

  private static final Map<String, StructClass> EXTRA_CLASS_CACHE = Collections.synchronizedMap(new HashMap<>());
  private static final Map<String, Method> METHOD_CACHE = Collections.synchronizedMap(new HashMap<>());

  public static StructClass findClass(String name) {
    if (EXTRA_CLASS_CACHE.containsKey(name)) {
      return EXTRA_CLASS_CACHE.get(name);
    }

    StructClass out = null;
    try (ZipFile classes = new ZipFile("/java.zip")) {
      String target = '/' + name + ".class";

      for (Enumeration<? extends ZipEntry> it = classes.entries(); it.hasMoreElements();) {
        ZipEntry entry = it.nextElement();
        if (entry.isDirectory() || !entry.getName().endsWith(target)) continue;

        out = StructClass.create(new DataInputFullStream(InterpreterUtil.getBytes(classes, entry)), false);
        break;
      }
    } catch (IOException e) {
      //Never mind
    }

    EXTRA_CLASS_CACHE.put(name, out);
    return out;
  }

  public static Method findMethod(String classname, String methodName, MethodDescriptor descriptor) {
    String targetClass = classname.replace('/', '.');
    String methodSignature = buildMethodSignature(targetClass + '.' + methodName, descriptor);

    Method method;
    if (METHOD_CACHE.containsKey(methodSignature)) {
      method = METHOD_CACHE.get(methodSignature);
    }
    else {
      method = findMethodOnClasspath(targetClass, methodSignature);
      METHOD_CACHE.put(methodSignature, method);
    }

    return method;
  }

  private static Method findMethodOnClasspath(String targetClass, String methodSignature) {
    try {
      // use bootstrap classloader to only provide access to JRE classes
      Class cls = new ClassLoader(null) {}.loadClass(targetClass);
      for (Method mtd : cls.getMethods()) {
        // use contains() to ignore access modifiers and thrown exceptions
        if (mtd.toString().contains(methodSignature)) {
          return mtd;
        }
      }
    }
    catch (Exception e) {
      // ignore
    }
    return null;
  }

  private static String buildMethodSignature(String name, MethodDescriptor md) {
    StringBuilder sb = new StringBuilder();

    appendType(sb, md.ret);
    sb.append(' ').append(name).append('(');
    for (VarType param : md.params) {
      appendType(sb, param);
      sb.append(',');
    }
    if (sb.charAt(sb.length() - 1) == ',') {
      sb.setLength(sb.length() - 1);
    }
    sb.append(')');

    return sb.toString();
  }

  private static void appendType(StringBuilder sb, VarType type) {
    sb.append(type.value.replace('/', '.'));
    for (int i = 0; i < type.arrayDim; i++) {
      sb.append("[]");
    }
  }
}
