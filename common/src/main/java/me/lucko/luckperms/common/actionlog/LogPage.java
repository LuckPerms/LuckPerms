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

package me.lucko.luckperms.common.actionlog;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

public class LogPage {
    private static final LogPage EMPTY = new LogPage(ImmutableList.of());

    public static LogPage.Builder builder() {
        return new LogPage.Builder();
    }

    public static LogPage of(List<LoggedAction> content) {
        return content.isEmpty() ? EMPTY : new LogPage(content);
    }

    public static LogPage empty() {
        return EMPTY;
    }

    private final List<LoggedAction> content;

    LogPage(List<LoggedAction> content) {
        this.content = ImmutableList.copyOf(content);
    }

    public List<LoggedAction> getContent() {
        return this.content;
    }

    public static class Builder {
        private final List<LoggedAction> content = new ArrayList<>();

        public Builder add(LoggedAction e) {
            this.content.add(e);
            return this;
        }

        public LogPage build() {
            if (this.content.isEmpty()) {
                return EMPTY;
            }
            return new LogPage(this.content);
        }
    }

}
