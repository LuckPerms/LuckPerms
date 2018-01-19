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

import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import java.util.Objects;

public class SenderLogger implements Logger {
    private final LuckPermsPlugin plugin;
    private final Sender console;

    public SenderLogger(LuckPermsPlugin plugin, Sender console) {
        this.plugin = plugin;
        this.console = console;
    }

    @Override
    public void info(String s) {
        msg(Message.LOG_INFO, Objects.requireNonNull(s));
    }

    @Override
    public void warn(String s) {
        msg(Message.LOG_WARN, Objects.requireNonNull(s));
    }

    @Override
    public void severe(String s) {
        msg(Message.LOG_ERROR, Objects.requireNonNull(s));
    }

    private void msg(Message message, String s) {
        String msg = message.asString(this.plugin.getLocaleManager(), s);
        if (this.plugin.getConfiguration() != null && !this.plugin.getConfiguration().get(ConfigKeys.USE_COLORED_LOGGER)) {
            msg = CommandUtils.stripColor(msg);
        }
        this.console.sendMessage(msg);
    }
}
