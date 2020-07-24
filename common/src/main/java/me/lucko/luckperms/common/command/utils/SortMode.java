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

public class SortMode {

    public static SortMode determine(ArgumentList args) {
        SortType type = SortType.PRIORITY;
        boolean ascending = true;
        for (String arg : args) {
            if (arg.equals("!") || arg.equalsIgnoreCase("reverse") || arg.equalsIgnoreCase("reversed")) {
                ascending = false;
            } else if (arg.equalsIgnoreCase("priority")) {
                type = SortType.PRIORITY;
                ascending = true;
            } else if (arg.equalsIgnoreCase("!priority")) {
                type = SortType.PRIORITY;
                ascending = false;
            } else if (arg.equalsIgnoreCase("alphabetically") || arg.equalsIgnoreCase("abc")) {
                type = SortType.ALPHABETICALLY;
                ascending = true;
            } else if (arg.equalsIgnoreCase("!alphabetically") || arg.equalsIgnoreCase("!abc")) {
                type = SortType.ALPHABETICALLY;
                ascending = false;
            } else {
                continue;
            }
            break;
        }
        return new SortMode(type, ascending);
    }

    private final SortType type;
    private final boolean ascending;

    public SortMode(SortType type, boolean ascending) {
        this.type = type;
        this.ascending = ascending;
    }

    public SortType getType() {
        return this.type;
    }

    public boolean isAscending() {
        return this.ascending;
    }
}
