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

package me.lucko.luckperms.nukkit.contexts;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.context.ContextCalculator;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import cn.nukkit.Player;

import javax.annotation.Nonnull;

public class WorldCalculator implements ContextCalculator<Player> {
    private final LuckPermsPlugin plugin;

    public WorldCalculator(LuckPermsPlugin plugin) {
        this.plugin = plugin;
    }

    @Nonnull
    @Override
    public MutableContextSet giveApplicableContext(@Nonnull Player subject, @Nonnull MutableContextSet accumulator) {
        String world = subject.getLevel().getName().toLowerCase();
        while (!accumulator.has(Contexts.WORLD_KEY, world)) {
            accumulator.add(Contexts.WORLD_KEY, world);
            world = this.plugin.getConfiguration().get(ConfigKeys.WORLD_REWRITES).getOrDefault(world, world).toLowerCase();
        }

        return accumulator;
    }
}
