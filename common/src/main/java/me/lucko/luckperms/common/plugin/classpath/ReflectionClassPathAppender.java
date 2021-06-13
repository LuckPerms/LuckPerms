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

package me.lucko.luckperms.common.plugin.classpath;

import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

@Deprecated // TODO: no longer works on Java 16 - Sponge needs to switch to JarInJar or find an API to add to classpath at runtime
public class ReflectionClassPathAppender implements ClassPathAppender {
    private static final Method ADD_URL_METHOD;

    static {
        // If on Java 9+, open the URLClassLoader module to this module
        // so we can access its API via reflection without producing a warning.
        try {
            openUrlClassLoaderModule();
        } catch (Throwable e) {
            // ignore
        }

        try {
            ADD_URL_METHOD = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        try {
            ADD_URL_METHOD.setAccessible(true);
        } catch (Throwable e) {
            new RuntimeException("LuckPerms is unable to access the URLClassLoader#addURL method using reflection. \n" +
                    "You may be able to fix this problem by adding the following command-line argument " +
                    "directly after the 'java' command in your start script: \n'--add-opens java.base/java.lang=ALL-UNNAMED'", e).printStackTrace();
        }
    }

    /**
     * Adds the given {@link URL} to the class loader.
     *
     * @param classLoader the class loader
     * @param url the url to add
     */
    public static void addUrl(URLClassLoader classLoader, URL url) throws ReflectiveOperationException {
        ADD_URL_METHOD.invoke(classLoader, url);
    }

    private final URLClassLoader classLoader;

    public ReflectionClassPathAppender(LuckPermsBootstrap bootstrap) throws IllegalStateException {
        ClassLoader classLoader = bootstrap.getClass().getClassLoader();
        if (classLoader instanceof URLClassLoader) {
            this.classLoader = (URLClassLoader) classLoader;
        } else {
            throw new IllegalStateException("ClassLoader is not instance of URLClassLoader");
        }
    }

    @Override
    public void addJarToClasspath(Path file) {
        try {
            addUrl(this.classLoader, file.toUri().toURL());
        } catch (ReflectiveOperationException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void openUrlClassLoaderModule() throws Exception {
        // This is effectively calling:
        //
        // URLClassLoader.class.getModule().addOpens(
        //     URLClassLoader.class.getPackageName(),
        //     ReflectionClassLoader.class.getModule()
        // );
        //
        // We use reflection since we build against Java 8.

        Class<?> moduleClass = Class.forName("java.lang.Module");
        Method getModuleMethod = Class.class.getMethod("getModule");
        Method addOpensMethod = moduleClass.getMethod("addOpens", String.class, moduleClass);

        Object urlClassLoaderModule = getModuleMethod.invoke(URLClassLoader.class);
        Object thisModule = getModuleMethod.invoke(ReflectionClassPathAppender.class);

        addOpensMethod.invoke(urlClassLoaderModule, URLClassLoader.class.getPackage().getName(), thisModule);
    }
}
