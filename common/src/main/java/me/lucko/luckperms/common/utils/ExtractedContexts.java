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

package me.lucko.luckperms.common.utils;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;

@Getter
@EqualsAndHashCode
@ToString
public final class ExtractedContexts {
    public static ExtractedContexts generate(Contexts contexts) {
        return new ExtractedContexts(contexts);
    }

    public static ExtractedContexts generate(ContextSet contexts) {
        return new ExtractedContexts(contexts);
    }

    private final Contexts contexts;
    private final ImmutableContextSet contextSet;
    private String server;
    private String world;

    private ExtractedContexts(Contexts context) {
        this.contexts = context;
        this.contextSet = context.getContexts().makeImmutable();
        setup(context.getContexts());
    }

    private ExtractedContexts(ContextSet contexts) {
        this.contexts = null;
        this.contextSet = contexts.makeImmutable();
        setup(contexts);
    }

    private void setup(ContextSet contexts) {
        server = contexts.getAnyValue("server").orElse(null);
        world = contexts.getAnyValue("world").orElse(null);
    }

    public Contexts getContexts() {
        if (contexts == null) {
            throw new NullPointerException("contexts");
        }
        return contexts;
    }
}
