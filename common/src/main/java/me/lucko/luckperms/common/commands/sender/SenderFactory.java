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

package me.lucko.luckperms.common.commands.sender;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import net.kyori.text.Component;

import java.util.UUID;

/**
 * Factory class to make a thread-safe sender instance
 *
 * @param <T> the command sender type
 */
@RequiredArgsConstructor
public abstract class SenderFactory<T> {

    @Getter(AccessLevel.PROTECTED)
    private final LuckPermsPlugin plugin;

    protected abstract String getName(T t);

    protected abstract UUID getUuid(T t);

    protected abstract void sendMessage(T t, String s);

    protected abstract void sendMessage(T t, Component message);

    protected abstract Tristate getPermissionValue(T t, String node);

    protected abstract boolean hasPermission(T t, String node);

    public final Sender wrap(@NonNull T sender) {
        return new AbstractSender<>(plugin, this, sender);
    }
}
