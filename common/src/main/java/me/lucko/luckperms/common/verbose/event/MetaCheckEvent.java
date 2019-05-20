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

package me.lucko.luckperms.common.verbose.event;

import me.lucko.luckperms.api.query.QueryOptions;
import me.lucko.luckperms.common.util.gson.JObject;

public class MetaCheckEvent extends VerboseEvent {

    /**
     * The origin of the check
     */
    private final Origin origin;

    /**
     * The meta key which was checked for
     */
    private final String key;

    /**
     * The result of the meta check
     */
    private final String result;

    public MetaCheckEvent(Origin origin, String checkTarget, QueryOptions checkQueryOptions, StackTraceElement[] checkTrace, String key, String result) {
        super(checkTarget, checkQueryOptions, checkTrace);
        this.origin = origin;
        this.key = key;
        this.result = result;
    }

    public Origin getOrigin() {
        return this.origin;
    }

    public String getKey() {
        return this.key;
    }

    public String getResult() {
        return this.result;
    }

    @Override
    protected void serializeTo(JObject object) {
        object.add("type", "meta")
                .add("key", this.key)
                .add("result", this.result)
                .add("origin", this.origin.name().toLowerCase());
    }

    /**
     * Represents the origin of a meta check
     */
    public enum Origin {

        /**
         * Indicates the check was caused by a lookup in a platform API
         */
        PLATFORM_API,

        /**
         * Indicates the check was caused by a 3rd party API call
         */
        THIRD_PARTY_API,

        /**
         * Indicates the check was caused by a LuckPerms API call
         */
        LUCKPERMS_API,

        /**
         * Indicates the check was caused by a LuckPerms internal
         */
        INTERNAL

    }
}
