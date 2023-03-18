package org.jetbrains.java.decompiler.main.plugins;

import org.jetbrains.java.decompiler.api.Plugin;
import org.jetbrains.java.decompiler.api.java.JavaPassLocation;
import org.jetbrains.java.decompiler.api.java.JavaPassRegistrar;
import org.jetbrains.java.decompiler.api.language.LanguageSpec;
import org.jetbrains.java.decompiler.api.passes.NamedPass;
import org.jetbrains.java.decompiler.api.passes.PassContext;
import org.jetbrains.java.decompiler.struct.StructClass;

import java.util.*;

public class PluginContext {
  private final List<Plugin> plugins = new ArrayList<>();
  private boolean initialized = false;
  private Map<JavaPassLocation, List<NamedPass>> passes = new HashMap<>();

  public void registerPlugin(Plugin plugin) {
    plugins.add(plugin);
  }

  public void initialize() {
    if (initialized) {
      return;
    }

    initialized = true;

    JavaPassRegistrar registrar = new JavaPassRegistrar();
    for (Plugin plugin : plugins) {
      plugin.registerJavaPasses(registrar);
    }

    passes = registrar.getPasses();
  }

  // Returns whether any passes were run
  public boolean runPasses(JavaPassLocation location, PassContext ctx) {
    List<NamedPass> passes = this.passes.getOrDefault(location, Collections.emptyList());

    for (NamedPass pass : passes) {
      if (pass.run(ctx) && location.isLoop()) {
        return true;
      }
    }

    return false;
  }

  public LanguageSpec getLanguageSpec(StructClass cl) {
    for (Plugin plugin : plugins) {
      LanguageSpec spec = plugin.getLanguageSpec();
      if (spec != null && spec.chooser.isLanguage(cl)) {
        return spec;
      }
    }
    return null;
  }

  public List<Plugin> getPlugins() {
    return Collections.unmodifiableList(plugins);
  }
}