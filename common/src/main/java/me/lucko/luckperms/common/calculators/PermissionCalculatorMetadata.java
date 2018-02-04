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

package me.lucko.luckperms.common.calculators;

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.references.HolderType;

/**
 * Metadata about a given {@link PermissionCalculator}.
 */
public class PermissionCalculatorMetadata {

    public static PermissionCalculatorMetadata of(HolderType holderType, String objectName, ContextSet context) {
        return new PermissionCalculatorMetadata(holderType, objectName, context);
    }

    /**
     * The type of the object which owns the permission calculator
     */
    private final HolderType holderType;

    /**
     * The name of the object which owns the permission calculator
     */
    private final String objectName;

    /**
     * The context the permission calculator works with
     */
    private final ContextSet context;

    private PermissionCalculatorMetadata(HolderType holderType, String objectName, ContextSet context) {
        this.holderType = holderType;
        this.objectName = objectName;
        this.context = context;
    }

    public HolderType getHolderType() {
        return this.holderType;
    }

    public String getObjectName() {
        return this.objectName;
    }

    public ContextSet getContext() {
        return this.context;
    }
}
