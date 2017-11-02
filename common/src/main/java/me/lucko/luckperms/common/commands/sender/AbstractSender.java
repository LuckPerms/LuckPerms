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
import lombok.EqualsAndHashCode;
import lombok.Getter;

import com.google.common.base.Splitter;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.TextUtils;

import net.kyori.text.Component;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.UUID;

/**
 * Simple implementation of {@link Sender} using a {@link SenderFactory}
 *
 * @param <T> the command sender type
 */
@Getter
@EqualsAndHashCode(of = "uuid")
public final class AbstractSender<T> implements Sender {
    private static final Splitter NEW_LINE_SPLITTER = Splitter.on("\n");

    private final LuckPermsPlugin platform;

    @Getter(AccessLevel.NONE)
    private final SenderFactory<T> factory;

    @Getter(AccessLevel.NONE)
    private final WeakReference<T> reference;

    private final UUID uuid;
    private final String name;

    AbstractSender(LuckPermsPlugin platform, SenderFactory<T> factory, T t) {
        this.platform = platform;
        this.factory = factory;
        this.reference = new WeakReference<>(t);
        this.uuid = factory.getUuid(t);
        this.name = factory.getName(t);
    }

    @Override
    public void sendMessage(String message) {
        final T t = reference.get();
        if (t != null) {

            if (!isConsole()) {
                factory.sendMessage(t, message);
                return;
            }

            // if it is console, split up the lines and send individually.
            for (String line : NEW_LINE_SPLITTER.split(message)) {
                factory.sendMessage(t, line);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendMessage(Component message) {
        if (isConsole()) {
            sendMessage(TextUtils.toLegacy(message));
            return;
        }

        final T t = reference.get();
        if (t != null) {
            factory.sendMessage(t, message);
        }
    }

    @Override
    public Tristate getPermissionValue(String permission) {
        T t = reference.get();
        if (t != null) {
            return factory.getPermissionValue(t, permission);
        }

        return isConsole() ? Tristate.TRUE : Tristate.UNDEFINED;
    }

    @Override
    public boolean hasPermission(String permission) {
        T t = reference.get();
        if (t != null) {
            if (factory.hasPermission(t, permission)) {
                return true;
            }
        }

        return isConsole();
    }

    @Override
    public boolean isValid() {
        return reference.get() != null;
    }

    @Override
    public Optional<Object> getHandle() {
        return Optional.ofNullable(reference.get());
    }
}
