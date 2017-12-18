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

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.commands.CommandManager;
import me.lucko.luckperms.common.commands.CommandPermission;
import me.lucko.luckperms.common.contexts.ContextManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import net.kyori.text.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Wrapper interface to represent a CommandSender/CommandSource within the common command implementations.
 */
public interface Sender {

    /**
     * Gets the platform where the sender is from.
     *
     * @return the plugin
     */
    LuckPermsPlugin getPlatform();

    /**
     * Gets the sender's username
     *
     * @return a friendly username for the sender
     */
    String getName();

    /**
     * Gets a string representing the senders username, and their current location
     * within the network.
     *
     * @return a friendly identifier for the sender
     */
    default String getNameWithLocation() {
        String name = getName();

        ContextManager<?> contextManager = getPlatform().getContextManager();
        if (contextManager == null) {
            return name;
        }


        String location = contextManager.getStaticContextString().orElse(null);
        if (location == null) {
            return name;
        }

        if (isConsole()) {
            return name.toLowerCase() + "@" + location;
        } else {
            return name + "@" + location;
        }
    }

    /**
     * Gets the sender's unique id. See {@link CommandManager#CONSOLE_UUID} for the console's UUID representation.
     *
     * @return the sender's uuid
     */
    UUID getUuid();

    /**
     * Send a message back to the Sender
     *
     * @param message the message to send. Supports '§' for message formatting.
     */
    void sendMessage(String message);

    /**
     * Send a json message to the Sender.
     *
     * @param message the message to send.
     */
    void sendMessage(Component message);

    /**
     * Gets the tristate a permission is set to.
     *
     * @param permission the permission to check for
     * @return a tristate
     */
    Tristate getPermissionValue(String permission);

    /**
     * Check if the Sender has a permission.
     *
     * @param permission the permission to check for
     * @return true if the sender has the permission
     */
    boolean hasPermission(String permission);

    /**
     * Check if the Sender has a permission.
     *
     * @param permission the permission to check for
     * @return true if the sender has the permission
     */
    default boolean hasPermission(CommandPermission permission) {
        return hasPermission(permission.getPermission());
    }

    /**
     * Gets whether this sender is the console
     *
     * @return if the sender is the console
     */
    default boolean isConsole() {
        return CommandManager.CONSOLE_UUID.equals(getUuid()) || CommandManager.IMPORT_UUID.equals(getUuid());
    }

    /**
     * Gets whether this sender is an import process
     *
     * @return if the sender is an import process
     */
    default boolean isImport() {
        return CommandManager.IMPORT_UUID.equals(getUuid());
    }

    /**
     * Gets whether this sender is still valid & receiving messages.
     *
     * @return if this sender is valid
     */
    default boolean isValid() {
        return true;
    }

    /**
     * Gets the handle object for this sender. (In most cases, the real
     * CommandSender/CommandSource object from the platform)
     *
     * @return the handle
     */
    default Optional<Object> getHandle() {
        return Optional.empty();
    }

}
