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

package me.lucko.luckperms.standalone.utils;

import me.lucko.luckperms.standalone.app.integration.StandaloneSender;
import me.lucko.luckperms.standalone.app.integration.StandaloneUser;
import net.kyori.adventure.text.Component;
import net.luckperms.api.util.Tristate;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Function;

public class TestSender implements StandaloneSender {

    private final Set<Consumer<Component>> messageSinks;

    private String name = "StandaloneUser";
    private UUID uniqueId = UUID.randomUUID();
    private boolean isConsole = false;

    private Function<String, Tristate> permissionChecker;

    public TestSender() {
        this.messageSinks = new CopyOnWriteArraySet<>();
        this.messageSinks.add(StandaloneUser.INSTANCE::sendMessage);
    }

    @Override
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public void setUniqueId(UUID uuid) {
        this.uniqueId = uuid;
    }

    @Override
    public void sendMessage(Component component) {
        for (Consumer<Component> sink : this.messageSinks) {
            sink.accept(component);
        }
    }

    @Override
    public Tristate getPermissionValue(String permission) {
        return this.permissionChecker == null
                ? Tristate.TRUE
                : this.permissionChecker.apply(permission);
    }

    @Override
    public boolean hasPermission(String permission) {
        return getPermissionValue(permission).asBoolean();
    }

    @Override
    public boolean isConsole() {
        return this.isConsole;
    }

    public void setConsole(boolean console) {
        this.isConsole = console;
    }

    @Override
    public Locale getLocale() {
        return Locale.ENGLISH;
    }

    public void setPermissionChecker(Function<String, Tristate> permissionChecker) {
        this.permissionChecker = permissionChecker;
    }

    public void addMessageSink(Consumer<Component> sink) {
        this.messageSinks.add(sink);
    }
}
