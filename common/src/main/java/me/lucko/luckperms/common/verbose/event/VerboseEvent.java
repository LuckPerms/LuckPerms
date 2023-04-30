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
import me.lucko.luckperms.common.util.StackTracePrinter;
import me.lucko.luckperms.common.util.gson.JArray;
import me.lucko.luckperms.common.util.gson.JObject;
import me.lucko.luckperms.common.verbose.VerboseCheckTarget;
import me.lucko.luckperms.common.verbose.expression.BooleanExpressionCompiler.VariableEvaluator;
import net.luckperms.api.cacheddata.Result;
import net.luckperms.api.context.Context;
import net.luckperms.api.query.QueryMode;
import net.luckperms.api.query.QueryOptions;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a verbose event.
 */
public abstract class VerboseEvent implements VariableEvaluator {

    /**
     * The origin of the check
     */
    private final CheckOrigin origin;

    /**
     * The name of the entity which was checked
     */
    private final VerboseCheckTarget checkTarget;

    /**
     * The query options used for the check
     */
    private final QueryOptions checkQueryOptions;

    /**
     * The time when the check took place
     */
    private final long checkTime;

    /**
     * The throwable created when the check took place
     */
    private final Throwable checkTrace;

    /**
     * The name of the thread where the check took place
     */
    private final String checkThread;

    protected VerboseEvent(CheckOrigin origin, VerboseCheckTarget checkTarget, QueryOptions checkQueryOptions, long checkTime, Throwable checkTrace, String checkThread) {
        this.origin = origin;
        this.checkTarget = checkTarget;
        this.checkQueryOptions = checkQueryOptions;
        this.checkTime = checkTime;
        this.checkTrace = checkTrace;
        this.checkThread = checkThread;
    }

    public CheckOrigin getOrigin() {
        return this.origin;
    }

    public VerboseCheckTarget getCheckTarget() {
        return this.checkTarget;
    }

    public abstract Result<?, ?> getResult();

    public QueryOptions getCheckQueryOptions() {
        return this.checkQueryOptions;
    }

    public long getCheckTime() {
        return this.checkTime;
    }

    public StackTraceElement[] getCheckTrace() {
        return this.checkTrace.getStackTrace();
    }

    public String getCheckThread() {
        return this.checkThread;
    }

    public abstract VerboseEventType getType();

    protected abstract void serializeTo(JObject object);

    public JsonObject toJson(StackTracePrinter tracePrinter) {
        return new JObject()
                .add("type", getType().toString())
                .add("origin", this.origin.name().toLowerCase(Locale.ROOT))
                .add("who", new JObject()
                        .add("identifier", this.checkTarget.describe())
                        .add("type", this.checkTarget.getType())
                        .add("name", this.checkTarget.getName())
                        .consume(obj -> {
                            UUID uuid = this.checkTarget.getId();
                            if (uuid != null) {
                                obj.add("uuid", uuid.toString());
                            }
                        })
                )
                .add("queryMode", this.checkQueryOptions.mode().name().toLowerCase(Locale.ROOT))
                .consume(obj -> {
                    if (this.checkQueryOptions.mode() == QueryMode.CONTEXTUAL) {
                        obj.add("context", new JArray()
                                .consume(arr -> {
                                    for (Context contextPair : Objects.requireNonNull(this.checkQueryOptions.context())) {
                                        arr.add(new JObject().add("key", contextPair.getKey()).add("value", contextPair.getValue()));
                                    }
                                })
                        );
                    }
                })
                .add("time", this.checkTime)
                .add("trace", new JArray()
                        .consume(arr -> {
                            int overflow = tracePrinter.process(getCheckTrace(), StackTracePrinter.elementToString(arr::add));
                            if (overflow != 0) {
                                arr.add("... and " + overflow + " more");
                            }
                        })
                )
                .add("thread", this.checkThread)
                .consume(this::serializeTo)
                .toJson();
    }
}
