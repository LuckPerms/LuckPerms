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

package me.lucko.luckperms.bungee.context;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import com.imaginarycode.minecraft.redisbungee.RedisBungeeAPI;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.context.StaticContextCalculator;
import org.checkerframework.checker.nullness.qual.NonNull;

public class RedisBungeeCalculator implements StaticContextCalculator {
    private static final String PROXY_KEY = "proxy";

    @Override
    public void calculate(@NonNull ContextConsumer consumer) {
        RedisBungeeAPI redisBungee = RedisBungee.getApi();
        if (redisBungee != null) {
            consumer.accept(PROXY_KEY, redisBungee.getServerId());
        }
    }

    @Override
    public @NonNull ContextSet estimatePotentialContexts() {
        RedisBungeeAPI redisBungee = RedisBungee.getApi();
        if (redisBungee == null) {
            return ImmutableContextSetImpl.EMPTY;
        }

        ImmutableContextSet.Builder builder = new ImmutableContextSetImpl.BuilderImpl();
        for (String server : redisBungee.getAllServers()) {
            builder.add(PROXY_KEY, server);
        }
        return builder.build();
    }
}
