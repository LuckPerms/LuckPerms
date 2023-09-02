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

package me.lucko.luckperms.minestom;

import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.event.AbstractEventBus;
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin;
import net.minestom.server.extensions.Extension;

public class MinestomEventBus extends AbstractEventBus<Extension> {
    private static final Extension DUMMY_EXTENSION;

    public MinestomEventBus(LPMinestomPlugin plugin, LuckPermsApiProvider apiProvider) {
        super(plugin, apiProvider);
    }

    @Override
    protected Extension checkPlugin(Object plugin) throws IllegalArgumentException {
        if (plugin instanceof Extension extension) {
            return extension;
        }
        if(plugin instanceof AbstractLuckPermsPlugin) {
            return DUMMY_EXTENSION;
        }

        throw new IllegalArgumentException("Object " + plugin + " (" + plugin.getClass().getName() + ") is not a plugin.");
    }

    @Override
    public void close() {
//        for (Extension extension : MinecraftServer.getExtensionManager().getExtensions()) {
//            for (Handler handler : extension.getDescription().getLogger().getHandlers()) {
//                if (handler instanceof UnloadHookLoggerHandler) {
//                    plugin.getLogger().removeHandler(handler);
//                }
//            }
//        }

        super.close();
    }

    static {
        DUMMY_EXTENSION = new Extension() {
            @Override
            public void initialize() {
            }

            @Override
            public void terminate() {}
        };
    }
}
