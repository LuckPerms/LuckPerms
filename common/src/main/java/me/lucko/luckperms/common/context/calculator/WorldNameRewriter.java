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

package me.lucko.luckperms.common.context.calculator;

import me.lucko.luckperms.common.config.ConfigKeys;
import net.luckperms.api.context.Context;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.DefaultContextKeys;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Rewrites world names according to the {@link ConfigKeys#WORLD_REWRITES} option.
 */
public interface WorldNameRewriter {

    static WorldNameRewriter of(Map<String, String> rewrites) {
        if (rewrites.isEmpty()) {
            return new Empty();
        } else {
            return new NonEmpty(rewrites);
        }
    }

    void rewriteAndSubmit(String worldName, ContextConsumer consumer);

    class Empty implements WorldNameRewriter {
        @Override
        public void rewriteAndSubmit(String worldName, ContextConsumer consumer) {
            if (Context.isValidValue(worldName)) {
                consumer.accept(DefaultContextKeys.WORLD_KEY, worldName);
            }
        }
    }

    class NonEmpty implements WorldNameRewriter {
        private final Map<String, String> rewrites;

        public NonEmpty(Map<String, String> rewrites) {
            this.rewrites = rewrites;
        }

        @Override
        public void rewriteAndSubmit(String worldName, ContextConsumer consumer) {
            Set<String> seen = new HashSet<>();
            worldName = worldName.toLowerCase(Locale.ROOT);

            while (Context.isValidValue(worldName) && seen.add(worldName)) {
                consumer.accept(DefaultContextKeys.WORLD_KEY, worldName);

                worldName = this.rewrites.get(worldName);
                if (worldName == null) {
                    break;
                }
            }
        }
    }

}
