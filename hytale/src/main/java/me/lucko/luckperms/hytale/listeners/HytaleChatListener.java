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

package me.lucko.luckperms.hytale.listeners;

import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import me.lucko.luckperms.common.event.LuckPermsEventListener;
import me.lucko.luckperms.hytale.LPHytalePlugin;
import me.lucko.luckperms.hytale.chat.LuckPermsChatFormatter;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.sync.ConfigReloadEvent;

public class HytaleChatListener implements LuckPermsEventListener {
    private final LPHytalePlugin plugin;
    private LuckPermsChatFormatter formatter;

    public HytaleChatListener(LPHytalePlugin plugin) {
        this.plugin = plugin;
        this.formatter = new LuckPermsChatFormatter(plugin);
    }

    public void register(EventRegistry registry) {
        registry.registerGlobal(PlayerChatEvent.class, this::onPlayerChat);
    }

    @Override
    public void bind(EventBus bus) {
        bus.subscribe(ConfigReloadEvent.class, this::onConfigReload);
    }

    private void onPlayerChat(PlayerChatEvent e) {
        e.setFormatter(this.formatter);
    }

    private void onConfigReload(ConfigReloadEvent e) {
        this.formatter = new LuckPermsChatFormatter(this.plugin);
    }

}
