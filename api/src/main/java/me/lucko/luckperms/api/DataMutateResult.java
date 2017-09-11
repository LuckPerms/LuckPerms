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

package me.lucko.luckperms.api;

import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.function.Supplier;

/**
 * Represents the result of a mutation call.
 */
public enum DataMutateResult {

    /**
     * Indicates the mutation was a success
     */
    SUCCESS(true, null),

    /**
     * Indicates the mutation failed because the subject already has something
     */
    ALREADY_HAS(false, ObjectAlreadyHasException::new),

    /**
     * Indicates the mutation failed because the subject lacks something
     */
    LACKS(false, ObjectLacksException::new),

    /**
     * Indicates the mutation failed
     */
    FAIL(false, RuntimeException::new);

    private boolean value;
    private final Supplier<? extends Exception> exceptionSupplier;

    DataMutateResult(boolean value, Supplier<? extends Exception> exceptionSupplier) {
        this.value = value;
        this.exceptionSupplier = exceptionSupplier;
    }

    public void throwException() {
        if (exceptionSupplier != null) {
            sneakyThrow(exceptionSupplier.get());
        }
    }

    /**
     * Gets a boolean representation of the result.
     *
     * @return a boolean representation
     */
    public boolean asBoolean() {
        return value;
    }

    /**
     * Gets if the result indicates a success
     *
     * @return if the result indicates a success
     * @since 3.4
     */
    public boolean wasSuccess() {
        return value;
    }

    /**
     * Gets if the result indicates a failure
     *
     * @return if the result indicates a failure
     * @since 3.4
     */
    public boolean wasFailure() {
        return !value;
    }

    // allows us to throw checked exceptions without declaring it, as #throwException throws a number of
    // exception types.
    private static void sneakyThrow(Throwable t) {
        sneakyThrow0(t);
    }

    private static <T extends Throwable> void sneakyThrow0(Throwable t) throws T {
        throw (T) t;
    }

}
