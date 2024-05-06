package org.vineflower.kotlin.util;

import org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericType;
import org.jetbrains.java.decompiler.util.future.MoreMap;
import org.jetbrains.java.decompiler.util.future.MoreString;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KTypes {
  private static final int MAX_KOTLIN_FUNCTION_ARITY = 22;

  private static final Pattern GENERICS_PATTERN = Pattern.compile("(.+)<(.+)>");
  private static final Map<String, String> JAVA_CLASS_TRANSLATIONS = MoreMap.of(
    "java/lang/Integer", "Int",
    "java/lang/Object", "Any"
  );
  
  private static final Map<String, String> JAVA_GENERIC_TRANSLATIONS = MoreMap.of(
    "java/util/Map", "MutableMap",
    "java/util/HashMap", "HashMap",
    "java/util/List", "MutableList",
    "java/util/ArrayList", "ArrayList",
    "java/util/Set", "MutableSet",
    "java/util/HashSet", "HashSet",
    "java/util/Collection", "MutableCollection",
    "java/util/Iterator", "MutableIterator",
    "java/util/Map$Entry", "MutableMap.MutableEntry",
    "java/lang/Iterable", "MutableIterable"
  );
  
  public static final Map<String, String> KOTLIN_TO_JAVA_LANG = MoreMap.ofEntries(
    MoreMap.entry("kotlin/Any", "java/lang/Object"),
    MoreMap.entry("kotlin/Boolean", "java/lang/Boolean"),
    MoreMap.entry("kotlin/Byte", "java/lang/Byte"),
    MoreMap.entry("kotlin/Char", "java/lang/Character"),
    MoreMap.entry("kotlin/CharSequence", "java/lang/CharSequence"),
    MoreMap.entry("kotlin/Comparable", "java/lang/Comparable"),
    MoreMap.entry("kotlin/Double", "java/lang/Double"),
    MoreMap.entry("kotlin/Enum", "java/lang/Enum"),
    MoreMap.entry("kotlin/Float", "java/lang/Float"),
    MoreMap.entry("kotlin/Int", "java/lang/Integer"),
    MoreMap.entry("kotlin/Long", "java/lang/Long"),
    MoreMap.entry("kotlin/Nothing", "java/lang/Void"),
    MoreMap.entry("kotlin/Number", "java/lang/Number"),
    MoreMap.entry("kotlin/Short", "java/lang/Short"),
    MoreMap.entry("kotlin/String", "java/lang/String"),
    MoreMap.entry("kotlin/Throwable", "java/lang/Throwable"),
    MoreMap.entry("kotlin/collections/MutableIterable", "java/lang/Iterable"),
    MoreMap.entry("kotlin/collections/Iterable", "java/lang/Iterable")
  );

  public static final Map<String, String> KOTLIN_TO_JAVA_UTIL = MoreMap.ofEntries(
    MoreMap.entry("kotlin/collections/MutableCollection", "java/util/Collection"),
    MoreMap.entry("kotlin/collections/MutableMap", "java/util/Map"),
    MoreMap.entry("kotlin/collections/MutableList", "java/util/List"),
    MoreMap.entry("kotlin/collections/MutableSet", "java/util/Set"),
    MoreMap.entry("kotlin/collections/MutableIterator", "java/util/Iterator"),
    MoreMap.entry("kotlin/collections/MutableListIterator", "java/util/ListIterator"),
    MoreMap.entry("kotlin/collections/MutableMap$MutableEntry", "java/util/Map$Entry"),
    MoreMap.entry("kotlin/collections/Collection", "java/util/Collection"),
    MoreMap.entry("kotlin/collections/Map", "java/util/Map"),
    MoreMap.entry("kotlin/collections/List", "java/util/List"),
    MoreMap.entry("kotlin/collections/ListIterator", "java/util/ListIterator"),
    MoreMap.entry("kotlin/collections/Set", "java/util/Set"),
    MoreMap.entry("kotlin/collections/Iterator", "java/util/Iterator"),
    MoreMap.entry("kotlin/collections/Map$Entry", "java/util/Map$Entry")
  );

  private static final Map<String, String> KOTLIN_PRIMITIVE_TYPES = MoreMap.of(
    "kotlin/Int", "I",
    "kotlin/Long", "J",
    "kotlin/Short", "S",
    "kotlin/Byte", "B",
    "kotlin/Boolean", "Z",
    "kotlin/Char", "C",
    "kotlin/Float", "F",
    "kotlin/Double", "D",
    "kotlin/Unit", "V"
  );

  public static String getJavaSignature(String kotlinType, boolean isNullable) {
    if (kotlinType.startsWith("L") && kotlinType.endsWith(";")) {
      kotlinType = kotlinType.substring(1, kotlinType.length() - 1);
    }

    // Kotlin uses . instead of $ when referring to inner classes in metadata but keeps $ in the bytecode
    if (kotlinType.contains(".")) {
      kotlinType = kotlinType.replace('.', '$');
    }

    if (kotlinType.startsWith("kotlin/")) {
      if (KOTLIN_PRIMITIVE_TYPES.containsKey(kotlinType) && !isNullable) {
        return KOTLIN_PRIMITIVE_TYPES.get(kotlinType);
      } else if (kotlinType.endsWith("$Companion") && KOTLIN_PRIMITIVE_TYPES.containsKey(kotlinType.replace("$Companion", ""))) {
        return "Lkotlin/jvm/internal/" + kotlinType.split("/")[1].split("\\$")[0] + "CompanionObject;";
      } else if (kotlinType.endsWith("Array") && KOTLIN_PRIMITIVE_TYPES.containsKey(kotlinType.replace("Array", ""))) {
        return "[" + KOTLIN_PRIMITIVE_TYPES.get(kotlinType.replace("Array", ""));
      } else if (KOTLIN_TO_JAVA_LANG.containsKey(kotlinType)) {
        return "L" + KOTLIN_TO_JAVA_LANG.get(kotlinType) + ";";
      } else if (KOTLIN_TO_JAVA_UTIL.containsKey(kotlinType)) {
        return "L" + KOTLIN_TO_JAVA_UTIL.get(kotlinType) + ";";
      } else if (kotlinType.startsWith("kotlin/Function")) {
        try {
          if (Integer.parseInt(kotlinType.substring("kotlin/Function".length())) > MAX_KOTLIN_FUNCTION_ARITY) {
            return "Lkotlin/jvm/functions/FunctionN;";
          }
          return "Lkotlin/jvm/functions" + kotlinType.substring("kotlin".length()) + ";";
        } catch (NumberFormatException e) {
          // Not a function type with arity
        }
      }
    }
    return "L" + kotlinType + ";";
  }

  public static String getKotlinType(VarType type) {
    return getKotlinType(type, true);
  }

  public static String getKotlinType(VarType type, boolean includeOuterClasses) {
    if (type == null) {
      //TODO: prevent passing null
      return "*";
    }

    String typeStr;
    if (isFunctionType(type)) {
      typeStr = functionTypeToKotlin(type);
    } else if (JAVA_CLASS_TRANSLATIONS.containsKey(type.value)) {
      typeStr = JAVA_CLASS_TRANSLATIONS.get(type.value);
    } else if (JAVA_GENERIC_TRANSLATIONS.containsKey(type.value) && type.isGeneric()) {
      GenericType genericType = ((GenericType) type);
      List<VarType> arguments = genericType.getArguments();
      String genericName = JAVA_GENERIC_TRANSLATIONS.get(type.value);
      if (arguments.isEmpty()) {
        typeStr = genericName;
      } else {
        StringBuilder builder = new StringBuilder(genericName);
        builder.append("<");
        for (int i = 0; i < arguments.size(); i++) {
          if (i != 0) {
            builder.append(", ");
          }
          builder.append(getKotlinType(arguments.get(i)));
        }
        builder.append(">");
        typeStr = builder.toString();
      }
    } else {
      String s = ExprProcessor.getCastTypeName(type);
      if (ExprProcessor.UNDEFINED_TYPE_STRING.equals(s)) {
        s = "Any";
      }
      // When going this path, arrays already get captured so no need to fall through to the array handling
      if (!includeOuterClasses) {
        s = s.substring(s.lastIndexOf('.') + 1);
      }

      return mapJavaTypeToKotlin(s);
    }
    return MoreString.repeat("Array<", type.arrayDim) + typeStr + MoreString.repeat(">", type.arrayDim);
  }

  private static String mapJavaTypeToKotlin(String type) {
    if (type.endsWith("[]")) {
      String baseType = type.substring(0, type.length() - 2);
      switch (baseType) {
        case "boolean":
        case "byte":
        case "char":
        case "short":
        case "int":
        case "long":
        case "float":
        case "double":
          return mapJavaTypeToKotlin(baseType) + "Array";
        default:
          return "Array<" + mapJavaTypeToKotlin(baseType) + ">";
      }
    }

    switch (type) {
      case "boolean":
        return "Boolean";
      case "byte":
        return "Byte";
      case "char":
        return "Char";
      case "short":
        return "Short";
      case "int":
        return "Int";
      case "long":
        return "Long";
      case "float":
        return "Float";
      case "double":
        return "Double";
      case "void":
        return "Unit";

      default:
        String[] parts = type.split("\\.");
        StringBuilder sb = new StringBuilder();
        boolean appendDot = false;
        for (String part : parts) {
          Matcher matcher = GENERICS_PATTERN.matcher(part);
          if (matcher.matches()) {
            sb.append(mapJavaTypeToKotlin(matcher.group(1)));
            sb.append("<");
            boolean appendComma = false;
            for (String generic : matcher.group(2).split(",")) {
              if (appendComma) {
                sb.append(", ");
              }
              sb.append(mapJavaTypeToKotlin(generic.trim()));
              appendComma = true;
            }
            sb.append(">");
          } else {
            if (appendDot) {
              sb.append(".");
            }
            sb.append(part);
            appendDot = true;
          }
        }
        return sb.toString();
    }
  }

  public static boolean isFunctionType(VarType type) {
    return type.value.startsWith("kotlin/jvm/functions/Function");
  }

  private static String functionTypeToKotlin(VarType type) {
    if (!isFunctionType(type)) {
      throw new IllegalArgumentException("Not a function type: " + type);
    }

    String paramCount = type.value.substring("kotlin/jvm/functions/Function".length());
    if (paramCount.equals("N") || !(type instanceof GenericType)) {
      // TODO: support FunctionN properly
      String typeStr = ExprProcessor.getCastTypeName(type);
      if (ExprProcessor.UNDEFINED_TYPE_STRING.equals(typeStr)) {
        return "Any";
      }
      return mapJavaTypeToKotlin(typeStr);
    }
    
    GenericType genericType = (GenericType) type;
    List<VarType> params = genericType.getArguments();

    int paramCountInt = Integer.parseInt(paramCount);
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    for (int i = 0; i < paramCountInt; i++) {
      if (i > 0) {
        sb.append(", ");
      }

      VarType param = params.get(i);
      String paramStr = getKotlinType(param);
      if (isFunctionType(param)) {
        paramStr = "(" + paramStr + ")";
      }
      sb.append(paramStr);
      // TODO: check for nullability in types
      sb.append("?");
    }

    sb.append(") -> ");
    VarType returnType = params.get(paramCountInt);
    String returnStr = getKotlinType(returnType);
    if (isFunctionType(returnType)) {
      returnStr = "(" + returnStr + ")";
    }
    sb.append(returnStr);

    return sb.toString();
  }
}
