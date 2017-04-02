/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.common.core;

import lombok.AllArgsConstructor;

import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

import java.util.function.Supplier;

@AllArgsConstructor
public enum DataMutateResult {

    SUCCESS(true, null),
    ALREADY_HAS(false, ObjectAlreadyHasException::new),
    LACKS(false, ObjectLacksException::new),
    FAIL(false, RuntimeException::new);

    private boolean bool;
    private final Supplier<? extends Exception> exceptionSupplier;

    public void throwException() {
        if (exceptionSupplier != null) {
            sneakyThrow(exceptionSupplier.get());
        }
    }

    public boolean asBoolean() {
        return bool;
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
