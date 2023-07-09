package org.jetbrains.java.decompiler.util;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructContext;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JrtFinder {
    public static final String CURRENT = "current";

    // https://openjdk.java.net/jeps/220 for runtime image structure and JRT filesystem

  public static void addRuntime(final StructContext ctx) {
    try {
        URL objectUrl = Object.class.getResource("/java/lang/Object.class");
        if (objectUrl == null) {
          DecompilerContext.getLogger().writeMessage("Could not locate Java runtime", IFernflowerLogger.Severity.WARN);
          return;
        }
        URI objectUri = objectUrl.toURI();
        if ("jar".equals(objectUri.getScheme())) {
          String fileUri = objectUri.getRawSchemeSpecificPart();
          fileUri = fileUri.substring(0, fileUri.indexOf('!'));
          Path jarPath = Paths.get(new URI(fileUri));
          if (jarPath.getNameCount() >= 2 && "lib".equals(jarPath.getParent().getFileName().toString())) {
            addRuntime(ctx, jarPath.subpath(0, jarPath.getNameCount() - 2).toFile()); //Speculate this is Java home
          } else {
            ctx.addSpace(jarPath.toFile(), false); //Just add the runtime jar we're sure about
          }
        } else {
          DecompilerContext.getLogger().writeMessage("Unexpected file system for Java runtime: " + objectUri.getScheme(), IFernflowerLogger.Severity.WARN);
        }
      } catch (URISyntaxException e) {
        DecompilerContext.getLogger().writeMessage("Could not locate Java runtime", IFernflowerLogger.Severity.WARN, e);
      }
  }

  public static void addRuntime(final StructContext ctx, final File javaHome) {
    if (new File(javaHome, "lib/jrt-fs.jar").isFile()) {
      // Java 9+
      DecompilerContext.getLogger().writeMessage("Cannot read Java 9+ runtime at " + javaHome, IFernflowerLogger.Severity.WARN);
      return;
    } else if (javaHome.exists()) {
      // legacy runtime, add all jars from the lib and jre/lib folders
      boolean anyAdded = false;
      final List<File> jrt = new ArrayList<>();
      Collections.addAll(jrt, new File(javaHome, "jre/lib").listFiles());
      Collections.addAll(jrt, new File(javaHome, "lib").listFiles());
      for (final File lib : jrt) {
        if (lib.isFile() && lib.getName().endsWith(".jar")) {
          ctx.addSpace(lib, false);
          anyAdded = true;
        }
      }
      if (anyAdded) return;
    }

    // does not exist
    DecompilerContext.getLogger().writeMessage("Unable to detect a java runtime at " + javaHome, IFernflowerLogger.Severity.ERROR);
  }
}
