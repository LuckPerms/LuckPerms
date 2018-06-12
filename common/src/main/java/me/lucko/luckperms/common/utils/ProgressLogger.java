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

package me.lucko.luckperms.common.utils;

import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.sender.Sender;

import java.util.HashSet;
import java.util.Set;

public class ProgressLogger {
    private static final int NOTIFY_FREQUENCY = 500;

    private final String pluginName;
    private final Message logMessage;
    private final Message logProgressMessage;

    private final Set<Sender> listeners = new HashSet<>();

    public ProgressLogger(String pluginName) {
        this(pluginName, Message.MIGRATION_LOG, Message.MIGRATION_LOG_PROGRESS);
    }

    public ProgressLogger(String pluginName, Message logMessage, Message logProgressMessage) {
        this.pluginName = pluginName;
        this.logMessage = logMessage;
        this.logProgressMessage = logProgressMessage;
    }

    public void addListener(Sender sender) {
        this.listeners.add(sender);
    }

    public void log(String msg) {
        if (this.pluginName == null) {
            this.listeners.forEach(s -> this.logMessage.send(s, msg));
        } else {
            this.listeners.forEach(s -> this.logMessage.send(s, this.pluginName, msg));
        }
    }

    public void logError(String msg) {
        if (this.pluginName == null) {
            this.listeners.forEach(s -> this.logMessage.send(s, "Error -> " + msg));
        } else {
            this.listeners.forEach(s -> this.logMessage.send(s, this.pluginName, "Error -> " + msg));
        }
    }

    public void logAllProgress(String msg, int amount) {
        if (this.pluginName == null) {
            this.listeners.forEach(s -> this.logProgressMessage.send(s, msg.replace("{}", Integer.toString(amount))));
        } else {
            this.listeners.forEach(s -> this.logProgressMessage.send(s, this.pluginName, msg.replace("{}", Integer.toString(amount))));
        }
    }

    public void logProgress(String msg, int amount) {
        if (amount % NOTIFY_FREQUENCY == 0) {
            logAllProgress(msg, amount);
        }
    }

    public Set<Sender> getListeners() {
        return this.listeners;
    }
}
