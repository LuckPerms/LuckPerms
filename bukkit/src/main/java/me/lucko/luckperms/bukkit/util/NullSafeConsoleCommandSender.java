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

package me.lucko.luckperms.bukkit.util;

import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * The {@link Server#getConsoleSender()} method returns null during onEnable
 * in older CraftBukkit builds. This prevents LuckPerms from loading correctly.
 */
public class NullSafeConsoleCommandSender implements ConsoleCommandSender {
    private final Server server;

    public NullSafeConsoleCommandSender(Server server) {
        this.server = server;
    }

    private Optional<ConsoleCommandSender> get() {
        //noinspection ConstantConditions
        return Optional.ofNullable(this.server.getConsoleSender());
    }

    @Override
    public @NonNull Server getServer() {
        return this.server;
    }

    @Override
    public @NonNull String getName() {
        return "CONSOLE";
    }

    @Override
    public void sendMessage(@NonNull String message) {
        Optional<ConsoleCommandSender> console = get();
        if (console.isPresent()) {
            console.get().sendMessage(message);
        } else {
            this.server.getLogger().info(ChatColor.stripColor(message));
        }
    }

    @Override
    public void sendMessage(String[] messages) {
        for (String msg : messages) {
            sendMessage(msg);
        }
    }

    @Override
    public boolean isPermissionSet(@NonNull String s) {
        return get().map(c -> c.isPermissionSet(s)).orElse(true);
    }

    @Override
    public boolean isPermissionSet(@NonNull Permission permission) {
        return get().map(c -> c.isPermissionSet(permission)).orElse(true);
    }

    @Override
    public boolean hasPermission(@NonNull String s) {
        return get().map(c -> c.hasPermission(s)).orElse(true);
    }

    @Override
    public boolean hasPermission(@NonNull Permission permission) {
        return get().map(c -> c.hasPermission(permission)).orElse(true);
    }

    @Override
    public boolean isOp() {
        return true;
    }

    // just throw UnsupportedOperationException - we never use any of these methods
    @Override public @NonNull Spigot spigot() { throw new UnsupportedOperationException(); }
    @Override public void sendMessage(@Nullable UUID uuid, @NotNull String s) { throw new UnsupportedOperationException(); }
    @Override public void sendMessage(@Nullable UUID uuid, @NotNull String... strings) { throw new UnsupportedOperationException(); }
    @Override public @NotNull Component name() { throw new UnsupportedOperationException(); }
    @Override public boolean isConversing() { throw new UnsupportedOperationException(); }
    @Override public void acceptConversationInput(@NonNull String s) { throw new UnsupportedOperationException(); }
    @Override public boolean beginConversation(@NonNull Conversation conversation) { throw new UnsupportedOperationException(); }
    @Override public void abandonConversation(@NonNull Conversation conversation) { throw new UnsupportedOperationException(); }
    @Override public void abandonConversation(@NonNull Conversation conversation, @NonNull ConversationAbandonedEvent conversationAbandonedEvent) { throw new UnsupportedOperationException(); }
    @Override public void sendRawMessage(@NonNull String s) { throw new UnsupportedOperationException(); }
    @Override public void sendRawMessage(@Nullable UUID uuid, @NotNull String s) { throw new UnsupportedOperationException(); }
    @Override public @NonNull PermissionAttachment addAttachment(@NonNull Plugin plugin, @NonNull String s, boolean b) { throw new UnsupportedOperationException(); }
    @Override public @NonNull PermissionAttachment addAttachment(@NonNull Plugin plugin) { throw new UnsupportedOperationException(); }
    @Override public PermissionAttachment addAttachment(@NonNull Plugin plugin, @NonNull String s, boolean b, int i) { throw new UnsupportedOperationException(); }
    @Override public PermissionAttachment addAttachment(@NonNull Plugin plugin, int i) { throw new UnsupportedOperationException(); }
    @Override public void removeAttachment(@NonNull PermissionAttachment permissionAttachment) { throw new UnsupportedOperationException(); }
    @Override public void recalculatePermissions() { throw new UnsupportedOperationException(); }
    @Override public @NonNull Set<PermissionAttachmentInfo> getEffectivePermissions() { throw new UnsupportedOperationException(); }
    @Override public void setOp(boolean b) { throw new UnsupportedOperationException(); }
}
