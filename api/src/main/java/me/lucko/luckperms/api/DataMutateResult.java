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

/**
 * Represents the result of a mutation call.
 */
public enum DataMutateResult {

    /**
     * Indicates the mutation was a success
     */
    SUCCESS(true),

    /**
     * Indicates the mutation failed because the subject of the action already has something
     */
    ALREADY_HAS(false),

    /**
     * Indicates the mutation failed because the subject of the action lacks something
     */
    LACKS(false),

    /**
     * Indicates the mutation failed
     */
    FAIL(false);

    private final boolean value;

    DataMutateResult(boolean value) {
        this.value = value;
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

}
