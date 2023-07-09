package org.jetbrains.java.decompiler.util;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructContext;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructModuleAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructModuleAttribute.ExportsEntry;
import org.jetbrains.java.decompiler.struct.attr.StructModuleAttribute.OpensEntry;
import org.jetbrains.java.decompiler.struct.attr.StructModuleAttribute.ProvidesEntry;
import org.jetbrains.java.decompiler.struct.attr.StructModuleAttribute.RequiresEntry;
import org.jetbrains.java.decompiler.util.future.MoreCollectors;
import org.jetbrains.java.decompiler.util.future.MoreSet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JrtFinder {
    public static final String CURRENT = "current";

    // https://openjdk.java.net/jeps/220 for runtime image structure and JRT filesystem

  public static void addRuntime(final StructContext ctx) {
    try {
      ctx.addSpace(new JavaRuntimeContextSource(null), false);
    } catch (final IOException ex) {
      DecompilerContext.getLogger().writeMessage("Failed to open current java runtime for inspection", ex);
    }
  }

  public static void addRuntime(final StructContext ctx, final File javaHome) {
    if (new File(javaHome, "lib/jrt-fs.jar").isFile()) {
      // Java 9+
      try {
        ctx.addSpace(new JavaRuntimeContextSource(javaHome), false);
      } catch (final IOException ex) {
        DecompilerContext.getLogger().writeMessage("Failed to open java runtime at " + javaHome, ex);
      }
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

  static final class JavaRuntimeModuleContextSource extends ModuleBasedContextSource {
    private Path module;

    JavaRuntimeModuleContextSource(final ModuleDescriptor descriptor, final Path moduleRoot) {
      super(descriptor);
      this.module = moduleRoot;
    }

    @Override
    public InputStream getInputStream(String resource) throws IOException {
      return Files.newInputStream(this.module.resolve(resource));
    }

    @Override
    protected Stream<String> entryNames() throws IOException {
      try (final Stream<Path> dir = Files.walk(this.module)) {
        return dir.map(it -> this.module.relativize(it).toString()).collect(Collectors.toList()).stream();
      }
    }
  }

  static final class JavaRuntimeContextSource implements IContextSource, AutoCloseable {
    private final String identifier;
    private final FileSystem jrtFileSystem;

    public JavaRuntimeContextSource(final File javaHome) throws IOException {
      final URI url = URI.create("jrt:/");
      if (javaHome == null) {
        this.identifier = "current";
        this.jrtFileSystem = FileSystems.newFileSystem(url, Map.of());
      } else {
        this.identifier = javaHome.getAbsolutePath();
        this.jrtFileSystem = FileSystems.newFileSystem(url, Map.of("java.home", javaHome.getAbsolutePath()));
      }
    }

    @Override
    public String getName() {
      return "Java runtime " + this.identifier;
    }

    private static ModuleDescriptor asDescriptor(StructModuleAttribute moduleAttr) {
      Set<ModuleDescriptor.Modifier> mods = EnumSet.noneOf(ModuleDescriptor.Modifier.class);
      if ((moduleAttr.moduleFlags & CodeConstants.ACC_OPEN) != 0) mods.add(ModuleDescriptor.Modifier.OPEN);
      if ((moduleAttr.moduleFlags & CodeConstants.ACC_SYNTHETIC) != 0) mods.add(ModuleDescriptor.Modifier.SYNTHETIC);
      if ((moduleAttr.moduleFlags & CodeConstants.ACC_MANDATED) != 0) mods.add(ModuleDescriptor.Modifier.MANDATED);

      ModuleDescriptor.Builder builder = ModuleDescriptor.newModule(moduleAttr.moduleName, mods);
      if (moduleAttr.moduleVersion != null) builder.version(moduleAttr.moduleVersion);

      for (final RequiresEntry requires : moduleAttr.requires) {
        Set<ModuleDescriptor.Requires.Modifier> rMods = EnumSet.noneOf(ModuleDescriptor.Requires.Modifier.class);
        if ((requires.flags & CodeConstants.ACC_TRANSITIVE) != 0) rMods.add(ModuleDescriptor.Requires.Modifier.TRANSITIVE);
        if ((requires.flags & CodeConstants.ACC_STATIC_PHASE) != 0) rMods.add(ModuleDescriptor.Requires.Modifier.STATIC);
        if ((requires.flags & CodeConstants.ACC_SYNTHETIC) != 0) rMods.add(ModuleDescriptor.Requires.Modifier.SYNTHETIC);
        if ((requires.flags & CodeConstants.ACC_MANDATED) != 0) rMods.add(ModuleDescriptor.Requires.Modifier.MANDATED);
        if (requires.moduleVersion != null) {
          builder.requires(rMods, requires.moduleName, ModuleDescriptor.Version.parse(requires.moduleVersion));
        } else {
          builder.requires(rMods, requires.moduleName);
        }
      }

      for (final ExportsEntry exports : moduleAttr.exports) {
        Set<ModuleDescriptor.Exports.Modifier> eMods = EnumSet.noneOf(ModuleDescriptor.Exports.Modifier.class);
        if ((exports.flags & CodeConstants.ACC_SYNTHETIC) != 0) eMods.add(ModuleDescriptor.Exports.Modifier.SYNTHETIC);
        if ((exports.flags & CodeConstants.ACC_MANDATED) != 0) eMods.add(ModuleDescriptor.Exports.Modifier.MANDATED);
        if (exports.exportToModules.isEmpty()) {
          builder.exports(eMods, exports.packageName.replace('/', '.'));
        } else {
          builder.exports(eMods, exports.packageName.replace('/', '.'), MoreSet.copyOf(exports.exportToModules));
        }
      }

      for (final OpensEntry opens : moduleAttr.opens) {
        Set<ModuleDescriptor.Opens.Modifier> oMods = EnumSet.noneOf(ModuleDescriptor.Opens.Modifier.class);
        if ((opens.flags & CodeConstants.ACC_SYNTHETIC) != 0) oMods.add(ModuleDescriptor.Opens.Modifier.SYNTHETIC);
        if ((opens.flags & CodeConstants.ACC_MANDATED) != 0) oMods.add(ModuleDescriptor.Opens.Modifier.MANDATED);

        if (opens.opensToModules.isEmpty()) {
          builder.opens(oMods, opens.packageName.replace('/', '.'));
        } else {
          builder.opens(oMods, opens.packageName.replace('/', '.'), MoreSet.copyOf(opens.opensToModules));
        }
      }

      for (final String uses : moduleAttr.uses) {
        builder.uses(uses.replace('/', '.'));
      }

      for (final ProvidesEntry provides : moduleAttr.provides) {
        builder.provides(
          provides.interfaceName.replace('/', '.'),
          provides.implementationNames.stream().map(name -> name.replace('/', '.')).collect(MoreCollectors.toUnmodifiableList())
        );
      }

      return builder.build();
    }

    @Override
    public Entries getEntries() {
      // One child source for every module in the runtime
      final List<IContextSource> children = new ArrayList<>();
      try {
      final List<Path> modules = Files.list(this.jrtFileSystem.getPath("modules")).collect(Collectors.toList());
      for (final Path module : modules) {
        ModuleDescriptor descriptor;
        try (final InputStream is = Files.newInputStream(module.resolve("module-info.class"))) {
          StructClass clazz = StructClass.create(new DataInputFullStream(is.readAllBytes()), false);
          StructModuleAttribute moduleAttr = clazz.getAttribute(StructGeneralAttribute.ATTRIBUTE_MODULE);
          if (moduleAttr == null) continue;

          descriptor = asDescriptor(moduleAttr);
        } catch (final IOException ex) {
          continue;
        }
        children.add(new JavaRuntimeModuleContextSource(descriptor, module));
      }

        return new Entries(List.of(), List.of(), List.of(), children);
      } catch (final IOException ex) {
        DecompilerContext.getLogger().writeMessage("Failed to read modules from runtime " + this.identifier, ex);
        return Entries.EMPTY;
      }
    }

    @Override
    public InputStream getInputStream(String resource) throws IOException {
      return null; // all resources are part of a child provider
    }

    @Override
    public void close() throws IOException {
      this.jrtFileSystem.close();
    }
  }
}
