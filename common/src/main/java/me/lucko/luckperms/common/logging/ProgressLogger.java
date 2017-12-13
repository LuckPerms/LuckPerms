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

package me.lucko.luckperms.common.logging;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.locale.Message;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public class ProgressLogger {
    private static final int NOTIFY_FREQUENCY = 500;

    private final String pluginName;
    private final Message logMessage;
    private final Message logProgressMessage;

    @Getter
    private final Set<Sender> listeners = new HashSet<>();

    public ProgressLogger(String pluginName) {
        this(pluginName, Message.MIGRATION_LOG, Message.MIGRATION_LOG_PROGRESS);
    }

    public void addListener(Sender sender) {
        listeners.add(sender);
    }

    public void log(String msg) {
        if (pluginName == null) {
            listeners.forEach(s -> logMessage.send(s, msg));
        } else {
            listeners.forEach(s -> logMessage.send(s, pluginName, msg));
        }
    }

    public void logErr(String msg) {
        if (pluginName == null) {
            listeners.forEach(s -> logMessage.send(s, "Error -> " + msg));
        } else {
            listeners.forEach(s -> logMessage.send(s, pluginName, "Error -> " + msg));
        }
    }

    public void logAllProgress(String msg, int amount) {
        if (pluginName == null) {
            listeners.forEach(s -> logProgressMessage.send(s, msg.replace("{}", Integer.toString(amount))));
        } else {
            listeners.forEach(s -> logProgressMessage.send(s, pluginName, msg.replace("{}", Integer.toString(amount))));
        }
    }

    public void logProgress(String msg, int amount) {
        if (amount % NOTIFY_FREQUENCY == 0) {
            // migrated {} groups so far.
            logAllProgress(msg, amount);
        }
    }
}
