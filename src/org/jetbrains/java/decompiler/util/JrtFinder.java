package org.jetbrains.java.decompiler.util;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructContext;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructModuleAttribute;

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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

  static final class JavaRuntimeModuleContextSource extends ModuleBasedContextSource {
	private final ZipFile root;
    private final String module;

    JavaRuntimeModuleContextSource(final String descriptor, final ZipFile root, final String moduleRoot) {
      super(descriptor);
      this.root = root;
      this.module = moduleRoot;
    }

    @Override
    public InputStream getInputStream(String resource) throws IOException {
      ZipEntry entry = this.root.getEntry(this.module + resource);
      return entry != null ? root.getInputStream(entry) : null;
    }

    @Override
    public byte[] getBytes(Entry resource) throws IOException {
      return getBytes(resource.path());
    }

    @Override
    public byte[] getBytes(String resource) throws IOException {
      final ZipEntry entry = this.root.getEntry(resource);
      return entry != null ? InterpreterUtil.getBytes(this.root, entry) : null;
    }

    @Override
    protected Stream<String> entryNames() throws IOException {
	  //return this.root.stream().filter(entry -> entry.getName().startsWith(module)).map(entry -> entry.getName().substring(module.length()));
      return StreamSupport.stream(
        /*new AbstractSpliterator<>(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.NONNULL) {
          private final Enumeration<? extends ZipEntry> it = root.entries();

          private String next() {
            while (it.hasMoreElements()) {
              ZipEntry entry = it.nextElement();
              if (!entry.getName().startsWith(module)) continue;

              return entry.getName().substring(module.length());
            }
            return null;
          }

          @Override
          public boolean tryAdvance(Consumer<? super String> action) {
            String out = next();
            if (out != null) {
              action.accept(out);
              return true;
            }
            return false;
          }

          @Override
          public void forEachRemaining(Consumer<? super String> action) {
            for (String out = next(); out != null; out = next()) {
              action.accept(out);
            }
          }
        }, false);*/
        Spliterators.spliteratorUnknownSize(new Iterator<String>() {
          private final Enumeration<? extends ZipEntry> it = root.entries();
          private String next = nextElement();

          private String nextElement() {
            while (it.hasMoreElements()) {
              ZipEntry entry = it.nextElement();
              if (!entry.getName().startsWith(module)) continue;

              return entry.getName().substring(module.length());
            }
            return null;
          }

          @Override
          public String next() {
            String out = next;
            if (out == null) throw new NoSuchElementException();
            next = nextElement();
            return out;
          }

          @Override
          public boolean hasNext() {
            return next != null;
          }
        }, Spliterator.ORDERED | Spliterator.NONNULL), false);
    }
  }

  static final class JavaRuntimeContextSource implements IContextSource, AutoCloseable {
    private final String identifier;
    private final ZipFile jrtFileSystem;

    public JavaRuntimeContextSource(final File javaHome) throws IOException {
      if (javaHome == null) {
        this.identifier = "current";
        this.jrtFileSystem = new ZipFile("/java.zip");
      } else {
        throw new UnsupportedOperationException("Tried to use " + javaHome + " as a context source");
      }
    }

    @Override
    public String getName() {
      return "Java runtime " + this.identifier;
    }

    @Override
    public Entries getEntries() {
      // One child source for every module in the runtime
      final List<IContextSource> children = new ArrayList<>();
      {
        final List<String> modules = new ArrayList<>();
        for (Enumeration<? extends ZipEntry> it = this.jrtFileSystem.entries(); it.hasMoreElements();) {
          ZipEntry entry = it.nextElement();
          if (!entry.isDirectory()) continue;

          String name = entry.getName().substring(0, entry.getName().length() - 1);
          if (name.indexOf('/') >= 0) continue;
          modules.add(name);
        }
        for (final String module : modules) {
          ZipEntry entry = this.jrtFileSystem.getEntry(module + "/module-info.class");
          if (entry == null) continue; //Module doesn't have a module-info?
          String descriptor;
          try {
    	    byte[] bytes = InterpreterUtil.getBytes(this.jrtFileSystem, entry);
            StructClass clazz = StructClass.create(new DataInputFullStream(bytes), false);
            StructModuleAttribute moduleAttr = clazz.getAttribute(StructGeneralAttribute.ATTRIBUTE_MODULE);
            if (moduleAttr == null) continue;

            descriptor = moduleAttr.moduleName;
            if (moduleAttr.moduleVersion != null) descriptor += '@' + moduleAttr.moduleVersion;
          } catch (final IOException ex) {
            continue;
          }
          children.add(new JavaRuntimeModuleContextSource(descriptor, this.jrtFileSystem, module + '/'));
        }

        return new Entries(List.of(), List.of(), List.of(), children);
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
