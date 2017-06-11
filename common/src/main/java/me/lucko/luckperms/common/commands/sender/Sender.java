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

import me.lucko.luckperms.common.constants.Constants;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import net.kyori.text.Component;

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
     * Gets the sender's unique id. See {@link Constants#CONSOLE_UUID} for the console's UUID representation.
     *
     * @return the sender's uuid
     */
    UUID getUuid();

    /**
     * Send a message back to the Sender
     *
     * @param s the message to send. Supports '§' for message formatting.
     */
    void sendMessage(String s);

    /**
     * Send a json message to the Sender.
     *
     * @param message the message to send.
     */
    void sendMessage(Component message);

    /**
     * Check if the Sender has a permission.
     *
     * @param permission the permission to check for
     * @return true if the sender has the permission
     */
    boolean hasPermission(Permission permission);

    /**
     * Gets whether this sender is the console
     *
     * @return if the sender is the console
     */
    boolean isConsole();

    /**
     * Gets whether this sender is an import process
     *
     * @return if the sender is an import process
     */
    boolean isImport();

}
