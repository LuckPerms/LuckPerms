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

import com.google.gson.JsonObject;

import me.lucko.luckperms.api.query.QueryMode;
import me.lucko.luckperms.api.query.QueryOptions;
import me.lucko.luckperms.common.util.StackTracePrinter;
import me.lucko.luckperms.common.util.gson.JArray;
import me.lucko.luckperms.common.util.gson.JObject;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a verbose event.
 */
public abstract class VerboseEvent {

    /**
     * The name of the entity which was checked
     */
    private final String checkTarget;

    /**
     * The query options used for the check
     */
    private final QueryOptions checkQueryOptions;

    /**
     * The stack trace when the check took place
     */
    private final StackTraceElement[] checkTrace;

    protected VerboseEvent(String checkTarget, QueryOptions checkQueryOptions, StackTraceElement[] checkTrace) {
        this.checkTarget = checkTarget;
        this.checkQueryOptions = checkQueryOptions;
        this.checkTrace = checkTrace;
    }

    public String getCheckTarget() {
        return this.checkTarget;
    }

    public QueryOptions getCheckQueryOptions() {
        return this.checkQueryOptions;
    }

    public StackTraceElement[] getCheckTrace() {
        return this.checkTrace;
    }

    protected abstract void serializeTo(JObject object);

    private JObject formBaseJson() {
        return new JObject()
                .add("who", new JObject()
                        .add("identifier", this.checkTarget)
                )
                .add("queryMode", this.checkQueryOptions.mode().name().toLowerCase())
                .consume(obj -> {
                    if (this.checkQueryOptions.mode() == QueryMode.CONTEXTUAL) {
                        obj.add("context", new JArray()
                                .consume(arr -> {
                                    for (Map.Entry<String, String> contextPair : Objects.requireNonNull(this.checkQueryOptions.context())) {
                                        arr.add(new JObject().add("key", contextPair.getKey()).add("value", contextPair.getValue()));
                                    }
                                })
                        );
                    }
                })
                .consume(this::serializeTo);
    }

    public JsonObject toJson() {
        return formBaseJson().toJson();
    }

    public JsonObject toJson(StackTracePrinter tracePrinter) {
        return formBaseJson()
                .add("trace", new JArray()
                        .consume(arr -> {
                            int overflow = tracePrinter.process(this.checkTrace, StackTracePrinter.elementToString(arr::add));
                            if (overflow != 0) {
                                arr.add("... and " + overflow + " more");
                            }
                        })
                )
                .toJson();
    }
}
