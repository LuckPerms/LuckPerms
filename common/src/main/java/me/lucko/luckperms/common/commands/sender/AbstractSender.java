/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

import lombok.Getter;

import me.lucko.luckperms.common.LuckPermsPlugin;
import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.constants.Permission;

import java.lang.ref.WeakReference;
import java.util.UUID;

import io.github.mkremins.fanciful.FancyMessage;

/**
 * Simple implementation of {@link Sender} using a {@link SenderFactory}
 *
 * @param <T> the command sender type
 */
@Getter
public class AbstractSender<T> implements Sender {
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
            factory.sendMessage(t, s);
        }
    }

    @Override
    public void sendMessage(FancyMessage message) {
        final T t = ref.get();
        if (t != null) {
            factory.sendMessage(t, message);
        }
    }

    @Override
    public boolean hasPermission(Permission permission) {
        if (isConsole()) return true;

        T t = ref.get();
        if (t != null) {
            for (String s : permission.getNodes()) {
                if (factory.hasPermission(t, s)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isConsole() {
        return this.uuid.equals(Constants.getConsoleUUID()) || this.uuid.equals(Constants.getImporterUUID());
    }

}
