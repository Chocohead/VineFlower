package org.jetbrains.java.decompiler.main.plugins;

import org.jetbrains.java.decompiler.api.plugin.Plugin;
import org.jetbrains.java.decompiler.api.plugin.PluginSource;
/*import org.vineflower.ideanotnull.IdeaNotNullPlugin;
import org.vineflower.kotlin.KotlinPlugin;
import org.vineflower.scala.ScalaPlugin;
import org.vineflower.variablerenaming.VariableRenamingPlugin;*/

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class JarPluginSource implements PluginSource {
    public JarPluginSource() {
    }

    public List<Plugin> findPlugins() {
      List<Plugin> plugins = new ArrayList<>();
      /*plugins.add(new KotlinPlugin()); //TODO: Plugins
      plugins.add(new ScalaPlugin());
      plugins.add(new VariableRenamingPlugin());
      plugins.add(new IdeaNotNullPlugin());*/

      return plugins;
    }
}
