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

package net.luckperms.api.model.data;

import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.node.Node;
import net.luckperms.api.track.Track;
import net.luckperms.api.util.Result;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents the result of a data mutation call on a LuckPerms object.
 *
 * <p>Usually as the result to a call on a {@link PermissionHolder} or {@link Track}.</p>
 */
public enum DataMutateResult implements Result {

    /**
     * Indicates the mutation was a success
     */
    SUCCESS(true),

    /**
     * Indicates the mutation failed
     */
    FAIL(false),

    /**
     * Indicates the mutation failed because the subject of the action already has something
     */
    FAIL_ALREADY_HAS(false),

    /**
     * Indicates the mutation failed because the subject of the action lacks something
     */
    FAIL_LACKS(false);

    private final boolean successful;

    DataMutateResult(boolean successful) {
        this.successful = successful;
    }

    @Override
    public boolean wasSuccessful() {
        return this.successful;
    }

    /**
     * Extension of {@link DataMutateResult} for temporary set operations.
     */
    public interface WithMergedNode {

        /**
         * Gets the underlying result.
         *
         * @return the result
         */
        @NonNull DataMutateResult getResult();

        /**
         * Gets the node that resulted from any {@link TemporaryNodeMergeStrategy}
         * processing.
         *
         * <p>If no processing took place, the same instance will be returned by
         * this method.</p>
         *
         * @return the resultant node
         */
        @NonNull Node getMergedNode();

    }
}
