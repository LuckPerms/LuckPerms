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

package me.lucko.luckperms.common.locale.command;

import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

/**
 * The localized data for a {@link CommandSpec}.
 */
public final class CommandSpecData {
    private final String description;
    private final String usage;
    private final Map<String, String> args;

    public CommandSpecData(@Nullable String description, @Nullable String usage, @Nullable Map<String, String> args) {
        this.description = description;
        this.usage = usage;
        this.args = args;
    }

    @Nullable
    public String getDescription() {
        return this.description;
    }

    @Nullable
    public String getUsage() {
        return this.usage;
    }

    @Nullable
    public Map<String, String> getArgs() {
        return this.args;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof CommandSpecData)) return false;
        final CommandSpecData that = (CommandSpecData) o;

        return Objects.equals(this.getDescription(), that.getDescription()) &&
                Objects.equals(this.getUsage(), that.getUsage()) &&
                Objects.equals(this.getArgs(), that.getArgs());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDescription(), getUsage(), getArgs());
    }

    @Override
    public String toString() {
        return "CommandSpecData(" +
                "description=" + this.getDescription() + ", " +
                "usage=" + this.getUsage() + ", " +
                "args=" + this.getArgs() + ")";
    }
}
