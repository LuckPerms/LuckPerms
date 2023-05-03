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

import com.google.gson.JsonArray;
import me.lucko.luckperms.common.cacheddata.result.StringResult;
import me.lucko.luckperms.common.node.utils.NodeJsonSerializer;
import me.lucko.luckperms.common.util.gson.JObject;
import me.lucko.luckperms.common.verbose.VerboseCheckTarget;
import net.luckperms.api.query.QueryOptions;

import java.util.Locale;

public class MetaCheckEvent extends VerboseEvent {

    /**
     * The meta key which was checked for
     */
    private final String key;

    /**
     * The result of the meta check
     */
    private final StringResult<?> result;

    public MetaCheckEvent(CheckOrigin origin, VerboseCheckTarget checkTarget, QueryOptions checkQueryOptions, long checkTime, Throwable checkTrace, String checkThread, String key, StringResult<?> result) {
        super(origin, checkTarget, checkQueryOptions, checkTime, checkTrace, checkThread);
        this.key = key;
        this.result = result;
    }

    public String getKey() {
        return this.key;
    }

    @Override
    public StringResult<?> getResult() {
        return this.result;
    }

    @Override
    public VerboseEventType getType() {
        return VerboseEventType.META;
    }

    @Override
    protected void serializeTo(JObject object) {
        object.add("key", this.key);

        object.add("result", String.valueOf(this.result.result()));
        if (this.result != StringResult.nullResult()) {
            object.add("resultInfo", serializeResult(this.result));
        }
    }

    private static JObject serializeResult(StringResult<?> result) {
        JObject object = new JObject();
        object.add("result", String.valueOf(result.result()));

        if (result.node() != null) {
            object.add("node", NodeJsonSerializer.serializeNode(result.node(), true));
        }

        if (result.overriddenResult() != null) {
            JsonArray overridden = new JsonArray();

            StringResult<?> next = result.overriddenResult();
            while (next != null) {
                overridden.add(serializeResult(next).toJson());
                next = next.overriddenResult();
            }

            object.add("overridden", overridden);
        }

        return object;
    }

    @Override
    public boolean eval(String variable) {
        return variable.equals("meta") ||
                getCheckTarget().describe().equalsIgnoreCase(variable) ||
                getKey().toLowerCase(Locale.ROOT).startsWith(variable.toLowerCase(Locale.ROOT)) ||
                String.valueOf(getResult().result()).equalsIgnoreCase(variable);
    }

}
