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

package me.lucko.luckperms.common.dependencies.classloader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * A classloader "isolated" from the rest of the Minecraft server.
 *
 * <p>Used to load specific LuckPerms dependencies without causing conflicts
 * with other plugins, or libraries provided by the server implementation.</p>
 */
public class IsolatedClassLoader extends URLClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    public IsolatedClassLoader(URL[] urls) {
        /*
         * ClassLoader#getSystemClassLoader returns the AppClassLoader
         *
         * Calling #getParent on this returns the ExtClassLoader (Java 8) or
         * the PlatformClassLoader (Java 9). Since we want this classloader to
         * be isolated from the Minecraft server (the app), we set the parent
         * to be the platform class loader.
         */
        super(urls, ClassLoader.getSystemClassLoader().getParent());
    }

}
