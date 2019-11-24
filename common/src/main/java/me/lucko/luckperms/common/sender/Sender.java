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

package me.lucko.luckperms.common.sender;

import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.context.ContextManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.util.TextUtils;

import net.kyori.text.Component;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.util.Tristate;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Wrapper interface to represent a CommandSender/CommandSource within the common command implementations.
 */
public interface Sender {

    /** The uuid used by the console sender. */
    UUID CONSOLE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    /** The name used by the console sender. */
    String CONSOLE_NAME = "Console";
    /** The uuid used by the 'import' sender. */
    UUID IMPORT_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    /** The name used by the 'import' sender. */
    String IMPORT_NAME = "Import";

    /**
     * Gets the plugin instance the sender is from.
     *
     * @return the plugin
     */
    LuckPermsPlugin getPlugin();

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

        ContextManager<?> contextManager = getPlugin().getContextManager();
        if (contextManager == null) {
            return name;
        }

        ImmutableContextSet staticContext = contextManager.getStaticContext();

        String location;
        if (staticContext.isEmpty()) {
            return name;
        } else if (staticContext.size() == 1) {
            location = staticContext.iterator().next().getValue();
        } else {
            Set<String> servers = staticContext.getValues(DefaultContextKeys.SERVER_KEY);
            if (servers.size() == 1) {
                location = servers.iterator().next();
            } else {
                location = staticContext.toSet().stream().map(pair -> pair.getKey() + "=" + pair.getValue()).collect(Collectors.joining(";"));
            }
        }

        return name + "@" + location;
    }

    /**
     * Gets the sender's unique id.
     *
     * <p>See {@link #CONSOLE_UUID} for the console's UUID representation.</p>
     *
     * @return the sender's uuid
     */
    UUID getUniqueId();

    /**
     * Send a message to the Sender.
     *
     * <p>Supports {@link TextUtils#SECTION_CHAR} for message formatting.</p>
     *
     * @param message the message to send.
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
        return CONSOLE_UUID.equals(getUniqueId()) || IMPORT_UUID.equals(getUniqueId());
    }

    /**
     * Gets whether this sender is an import process
     *
     * @return if the sender is an import process
     */
    default boolean isImport() {
        return IMPORT_UUID.equals(getUniqueId());
    }

    /**
     * Gets whether this sender is still valid & receiving messages.
     *
     * @return if this sender is valid
     */
    default boolean isValid() {
        return true;
    }

}
