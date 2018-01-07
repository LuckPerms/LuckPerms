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

package me.lucko.luckperms.common.verbose;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ImmutableContextSet;

/**
 * Holds the data from a permission check
 */
public class CheckData {

    /**
     * The origin of the check
     */
    private final CheckOrigin checkOrigin;

    /**
     * The name of the entity which was checked
     */
    private final String checkTarget;

    /**
     * The contexts where the check took place
     */
    private final ImmutableContextSet checkContext;

    /**
     * The stack trace when the check took place
     */
    private final StackTraceElement[] checkTrace;

    /**
     * The permission which was checked for
     */
    private final String permission;

    /**
     * The result of the permission check
     */
    private final Tristate result;

    public CheckData(CheckOrigin checkOrigin, String checkTarget, ImmutableContextSet checkContext, StackTraceElement[] checkTrace, String permission, Tristate result) {
        this.checkOrigin = checkOrigin;
        this.checkTarget = checkTarget;
        this.checkContext = checkContext;
        this.checkTrace = checkTrace;
        this.permission = permission;
        this.result = result;
    }

    public CheckOrigin getCheckOrigin() {
        return this.checkOrigin;
    }

    public String getCheckTarget() {
        return this.checkTarget;
    }

    public ImmutableContextSet getCheckContext() {
        return this.checkContext;
    }

    public StackTraceElement[] getCheckTrace() {
        return this.checkTrace;
    }

    public String getPermission() {
        return this.permission;
    }

    public Tristate getResult() {
        return this.result;
    }
}
