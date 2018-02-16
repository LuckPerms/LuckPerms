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

package me.lucko.luckperms.common.storage.dao.file;

import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.common.commands.CommandManager;

import java.io.File;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class FileActionLogger {
    private static final String LOG_FORMAT = "%s(%s): [%s] %s(%s) --> %s";
    private final Logger actionLogger = Logger.getLogger("luckperms_actions");

    public void init(File file) {
        try {
            FileHandler fh = new FileHandler(file.getAbsolutePath(), 0, 1, true);
            fh.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return new Date(record.getMillis()).toString() + ": " + record.getMessage() + "\n";
                }
            });
            this.actionLogger.addHandler(fh);
            this.actionLogger.setUseParentHandlers(false);
            this.actionLogger.setLevel(Level.ALL);
            this.actionLogger.setFilter(record -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void logAction(LogEntry entry) {
        this.actionLogger.info(String.format(LOG_FORMAT,
                (entry.getActor().equals(CommandManager.CONSOLE_UUID) ? "" : entry.getActor() + " "),
                entry.getActorName(),
                Character.toString(entry.getType().getCode()),
                entry.getActed().map(e -> e.toString() + " ").orElse(""),
                entry.getActedName(),
                entry.getAction())
        );
    }

}
