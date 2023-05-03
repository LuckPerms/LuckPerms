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

package me.lucko.luckperms.bungee;

import me.lucko.luckperms.common.api.LuckPermsApiProvider;
import me.lucko.luckperms.common.event.AbstractEventBus;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class BungeeEventBus extends AbstractEventBus<Plugin> implements Listener {
    private final LPBungeeBootstrap bootstrap;

    public BungeeEventBus(LPBungeePlugin plugin, LuckPermsApiProvider apiProvider) {
        super(plugin, apiProvider);
        this.bootstrap = plugin.getBootstrap();
    }

    @Override
    protected Plugin checkPlugin(Object plugin) throws IllegalArgumentException {
        if (plugin instanceof Plugin) {
            Plugin bungeePlugin = (Plugin) plugin;

            // add a custom log handler to effectively listen for the plugin being disabled.
            // BungeeCord doesn't really support enabling/disabling plugins at runtime, and as
            // such doesn't have a PluginDisableEvent. However, some plugins do exist to reload
            // plugins at runtime. We rely on these plugins following the BungeeCord behaviour,
            // and #close ing the plugins logger, so we can unregister the listeners. :)
            Handler[] handlers = bungeePlugin.getLogger().getHandlers();
            for (Handler handler : handlers) {
                if (handler instanceof UnloadHookLoggerHandler) {
                    return bungeePlugin;
                }
            }

            bungeePlugin.getLogger().addHandler(new UnloadHookLoggerHandler(bungeePlugin));
            return bungeePlugin;
        }

        throw new IllegalArgumentException("Object " + plugin + " (" + plugin.getClass().getName() + ") is not a plugin.");
    }

    @Override
    public void close() {
        for (Plugin plugin : this.bootstrap.getProxy().getPluginManager().getPlugins()) {
            for (Handler handler : plugin.getLogger().getHandlers()) {
                if (handler instanceof UnloadHookLoggerHandler) {
                    plugin.getLogger().removeHandler(handler);
                }
            }
        }

        super.close();
    }

    private final class UnloadHookLoggerHandler extends Handler {
        private final Plugin plugin;

        private UnloadHookLoggerHandler(Plugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void close() {
            unregisterHandlers(this.plugin);
        }

        @Override public void publish(LogRecord record) {}
        @Override public void flush() {}
    }
}
