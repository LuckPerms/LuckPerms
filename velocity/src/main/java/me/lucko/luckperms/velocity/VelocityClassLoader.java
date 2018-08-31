package me.lucko.luckperms.velocity;

import me.lucko.luckperms.common.dependencies.classloader.PluginClassLoader;

import java.nio.file.Path;

public class VelocityClassLoader implements PluginClassLoader {
    private final LPVelocityBootstrap bootstrap;

    public VelocityClassLoader(LPVelocityBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public void loadJar(Path file) {
        this.bootstrap.getProxy().getPluginManager().addToClasspath(bootstrap, file);
    }
}
