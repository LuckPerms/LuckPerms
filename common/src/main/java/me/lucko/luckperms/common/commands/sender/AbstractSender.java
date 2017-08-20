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

import lombok.EqualsAndHashCode;
import lombok.Getter;

import com.google.common.base.Splitter;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.constants.CommandPermission;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import net.kyori.text.Component;
import net.kyori.text.LegacyComponent;

import java.lang.ref.WeakReference;
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
    private final SenderFactory<T> factory;
    private final WeakReference<T> ref;
    private final String name;
    private final UUID uuid;

    AbstractSender(LuckPermsPlugin platform, SenderFactory<T> factory, T t) {
        this.platform = platform;
        this.factory = factory;
        this.ref = new WeakReference<>(t);
        this.name = factory.getName(t);
        this.uuid = factory.getUuid(t);
    }

    @Override
    public void sendMessage(String s) {
        final T t = ref.get();
        if (t != null) {

            if (!isConsole()) {
                factory.sendMessage(t, s);
                return;
            }

            // if it is console, split up the lines and send individually.
            for (String line : NEW_LINE_SPLITTER.split(s)) {
                factory.sendMessage(t, line);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendMessage(Component message) {
        if (isConsole()) {
            sendMessage(LegacyComponent.to(message));
            return;
        }

        final T t = ref.get();
        if (t != null) {
            factory.sendMessage(t, message);
        }
    }

    @Override
    public Tristate getPermissionValue(String permission) {
        if (isConsole()) return Tristate.TRUE;

        T t = ref.get();
        if (t != null) {
            return factory.getPermissionValue(t, permission);
        }

        return Tristate.UNDEFINED;
    }

    @Override
    public boolean hasPermission(String permission) {
        if (isConsole()) return true;

        T t = ref.get();
        if (t != null) {
            if (factory.hasPermission(t, permission)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean hasPermission(CommandPermission permission) {
        return hasPermission(permission.getPermission());
    }

    @Override
    public boolean isConsole() {
        return this.uuid.equals(Constants.CONSOLE_UUID) || this.uuid.equals(Constants.IMPORT_UUID);
    }

    @Override
    public boolean isImport() {
        return this.uuid.equals(Constants.IMPORT_UUID);
    }

    @Override
    public boolean isValid() {
        return ref.get() != null;
    }

}
