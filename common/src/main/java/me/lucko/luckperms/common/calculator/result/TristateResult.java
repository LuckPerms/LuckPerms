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

package me.lucko.luckperms.common.calculator.result;

import me.lucko.luckperms.common.calculator.PermissionCalculator;
import me.lucko.luckperms.common.calculator.processor.PermissionProcessor;

import net.luckperms.api.util.Tristate;

/**
 * Represents the result of a {@link PermissionCalculator} lookup.
 */
public final class TristateResult {

    private static final Factory NULL_FACTORY = new Factory(null, null);
    public static final TristateResult UNDEFINED = new TristateResult(Tristate.UNDEFINED, null, null);

    public static TristateResult of(Tristate result) {
        return NULL_FACTORY.result(result);
    }
    
    private final Tristate result;
    private final Class<? extends PermissionProcessor> processorClass;
    private final String cause;

    private TristateResult(Tristate result, Class<? extends PermissionProcessor> processorClass, String cause) {
        this.result = result;
        this.processorClass = processorClass;
        this.cause = cause;
    }

    public Tristate result() {
        return this.result;
    }

    public Class<? extends PermissionProcessor> processorClass() {
        return this.processorClass;
    }

    public String cause() {
        return this.cause;
    }

    @Override
    public String toString() {
        return "TristateResult(" +
                "result=" + this.result + ", " +
                "processorClass=" + this.processorClass + ", " +
                "cause=" + this.cause + ')';
    }

    public static final class Factory {
        private final Class<? extends PermissionProcessor> processorClass;

        private final TristateResult trueResult;
        private final TristateResult falseResult;

        public Factory(Class<? extends PermissionProcessor> processorClass, String defaultCause) {
            this.processorClass = processorClass;

            this.trueResult = new TristateResult(Tristate.TRUE, processorClass, defaultCause);
            this.falseResult = new TristateResult(Tristate.FALSE, processorClass, defaultCause);
        }

        public Factory(Class<? extends PermissionProcessor> processorClass) {
            this(processorClass, null);
        }

        public TristateResult result(Tristate result) {
            switch (result) {
                case TRUE:
                    return this.trueResult;
                case FALSE:
                    return this.falseResult;
                case UNDEFINED:
                    return UNDEFINED;
                default:
                    throw new AssertionError();
            }
        }

        public TristateResult result(Tristate result, String cause) {
            if (result == Tristate.UNDEFINED) {
                return UNDEFINED;
            }
            return new TristateResult(result, this.processorClass, cause);
        }
    }
}
