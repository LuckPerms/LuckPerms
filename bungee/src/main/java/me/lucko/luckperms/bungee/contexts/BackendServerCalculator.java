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

package me.lucko.luckperms.bungee.contexts;

import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.context.ContextCalculator;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import net.md_5.bungee.api.connection.ProxiedPlayer;

@RequiredArgsConstructor
public class BackendServerCalculator implements ContextCalculator<ProxiedPlayer> {

    private static String getServer(ProxiedPlayer player) {
        return player.getServer() == null ? null : (player.getServer().getInfo() == null ? null : player.getServer().getInfo().getName().toLowerCase());
    }

    private final LuckPermsPlugin plugin;

    @Override
    public MutableContextSet giveApplicableContext(ProxiedPlayer subject, MutableContextSet accumulator) {
        String server = getServer(subject);
        while (server != null && !accumulator.has(Contexts.WORLD_KEY, server)) {
            accumulator.add(Contexts.WORLD_KEY, server);
            server = plugin.getConfiguration().get(ConfigKeys.WORLD_REWRITES).getOrDefault(server, server).toLowerCase();
        }

        return accumulator;
    }
}
