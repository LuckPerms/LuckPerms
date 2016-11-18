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

package me.lucko.luckperms.common.utils;

import lombok.Getter;
import lombok.ToString;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;

@Getter
@ToString
public class ExtractedContexts {
    public static ExtractedContexts generate(Contexts contexts) {
        return new ExtractedContexts(contexts);
    }

    public static ExtractedContexts generate(ContextSet contexts) {
        return new ExtractedContexts(contexts);
    }

    private Contexts contexts;
    private ContextSet contextSet;
    private String server;
    private String world;

    private ExtractedContexts(Contexts context) {
        this.contexts = context;
        setup(context.getContexts());
    }

    private ExtractedContexts(ContextSet contexts) {
        setup(contexts);
    }

    private void setup(ContextSet contexts) {
        MutableContextSet contextSet = MutableContextSet.fromSet(contexts);
        server = contextSet.getValues("server").stream().findAny().orElse(null);
        world = contextSet.getValues("world").stream().findAny().orElse(null);
        contextSet.removeAll("server");
        contextSet.removeAll("world");

        this.contextSet = contextSet.makeImmutable();
    }
}
