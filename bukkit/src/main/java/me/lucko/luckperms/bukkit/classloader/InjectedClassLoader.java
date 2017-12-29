/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.bukkit.classloader;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A fake classloader instance which sits in-front of a PluginClassLoader, which
 * attempts to load classes from it's own sources before allowing the PCL to load
 * the class.
 *
 * This allows us to inject extra URL sources into the plugin's classloader at
 * runtime.
 */
public class InjectedClassLoader extends URLClassLoader implements LPClassLoader {

    public static InjectedClassLoader inject(JavaPlugin plugin) throws Exception {
        // get the plugin's PluginClassLoader instance
        ClassLoader classLoader = plugin.getClass().getClassLoader();

        // get a ref to the PCL class
        Class<?> pclClass = Class.forName("org.bukkit.plugin.java.PluginClassLoader");

        // extract the 'classes' cache map
        Field classesField = pclClass.getDeclaredField("classes");
        classesField.setAccessible(true);

        // obtain the classes instance from the classloader
        //noinspection unchecked
        Map<String, Class<?>> old = (Map) classesField.get(classLoader);

        // init a new InjectedClassLoader to read from
        InjectedClassLoader newLoader = new InjectedClassLoader();

        // replace the 'classes' cache map with our own.
        classesField.set(classLoader, new DelegatingClassMap(newLoader, old));

        return newLoader;
    }

    static {
        try {
            Method method = ClassLoader.class.getDeclaredMethod("registerAsParallelCapable");
            if (method != null) {
                method.setAccessible(true);
                method.invoke(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final Map<String, Class<?>> cache = new ConcurrentHashMap<>();

    private InjectedClassLoader() {
        super(new URL[0]);
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    private Class<?> lookup(String name) {
        // try the cache
        Class<?> clazz = cache.get(name);
        if (clazz != null) {
            return clazz;
        }

        try {
            // attempt to load
            clazz = loadClass(name);

            // if successful, add to the cache file
            cache.put(name, clazz);
            return clazz;
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * A fake map instance which effectively allows us to override the behaviour
     * of #findClass in PluginClassLoader.
     */
    @RequiredArgsConstructor
    private static final class DelegatingClassMap implements Map<String, Class<?>> {
        private final InjectedClassLoader loader;

        // delegate all other calls to the original map
        @Delegate(excludes = Exclude.class)
        private final Map<String, Class<?>> delegate;

        // override the #get call, so we can attempt to load the class ourselves.
        @Override
        public Class<?> get(Object key) {
            String className = ((String) key);

            Class<?> clazz = loader.lookup(className);
            if (clazz != null) {
                return clazz;
            } else {
                return delegate.get(className);
            }
        }

        private interface Exclude {
            Class<?> get(Object key);
        }
    }

}
