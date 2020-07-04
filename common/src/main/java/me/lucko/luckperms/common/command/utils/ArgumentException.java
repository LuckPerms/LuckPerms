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

package me.lucko.luckperms.common.command.utils;

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.Command;
import me.lucko.luckperms.common.command.abstraction.CommandException;
import me.lucko.luckperms.common.command.abstraction.GenericChildCommand;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.sender.Sender;

public abstract class ArgumentException extends CommandException {

    public static class DetailedUsage extends ArgumentException {
        @Override
        protected CommandResult handle(Sender sender) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CommandResult handle(Sender sender, String label, Command<?> command) {
            command.sendDetailedUsage(sender, label);
            return CommandResult.INVALID_ARGS;
        }

        @Override
        public CommandResult handle(Sender sender, GenericChildCommand command) {
            command.sendDetailedUsage(sender);
            return CommandResult.INVALID_ARGS;
        }
    }

    public static class PastDate extends ArgumentException {
        @Override
        protected CommandResult handle(Sender sender) {
            Message.PAST_DATE_ERROR.send(sender);
            return CommandResult.INVALID_ARGS;
        }
    }

    public static class InvalidDate extends ArgumentException {
        private final String invalidDate;

        public InvalidDate(String invalidDate) {
            this.invalidDate = invalidDate;
        }

        @Override
        protected CommandResult handle(Sender sender) {
            Message.ILLEGAL_DATE_ERROR.send(sender, this.invalidDate);
            return CommandResult.INVALID_ARGS;
        }
    }

    public static class InvalidPriority extends ArgumentException {
        private final String invalidPriority;

        public InvalidPriority(String invalidPriority) {
            this.invalidPriority = invalidPriority;
        }

        @Override
        public CommandResult handle(Sender sender) {
            Message.META_INVALID_PRIORITY.send(sender, this.invalidPriority);
            return CommandResult.INVALID_ARGS;
        }
    }
}
