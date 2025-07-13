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

package me.lucko.luckperms.library.sender;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import me.lucko.luckperms.library.LuckPermsLibraryManager;
import net.kyori.adventure.text.Component;
import net.luckperms.api.util.Tristate;

public class PlayerLibrarySender implements LibrarySender {

    private final Supplier<LuckPermsLibraryManager> manager;
    private final UUID uuid;
    private final String username;
    private final Function<String, Tristate> getPermissionValue;

    public PlayerLibrarySender(Supplier<LuckPermsLibraryManager> manager, UUID uuid, String username, Function<String, Tristate> getPermissionValue) {
        this.manager = manager;
        this.uuid = uuid;
        this.username = username;
        this.getPermissionValue = getPermissionValue;
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public void sendMessage(Component component) {
        manager.get().onPlayerMessage(uuid, component);
    }

    @Override
    public Tristate getPermissionValue(String permission) {
        return getPermissionValue.apply(permission);
    }

    @Override
    public boolean hasPermission(String permission) {
        return getPermissionValue(permission).asBoolean();
    }

    @Override
    public void performCommand(String command) {
        manager.get().performPlayerCommand(uuid, command);
    }

    @Override
    public boolean isConsole() {
        return false;
    }

    @Override
    public Locale getLocale() {
        return manager.get().getPlayerLocale(uuid);
    }

    @Override
    public boolean shouldSplitNewlines() {
        return manager.get().shouldPlayerSplitNewlines(uuid);
    }

}
