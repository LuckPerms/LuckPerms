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

package me.lucko.luckperms.common.primarygroup;

import me.lucko.luckperms.common.buffers.Cache;
import me.lucko.luckperms.common.caching.handlers.StateListener;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeFactory;

/**
 * Abstract implementation of {@link StateListener} which caches all lookups.
 */
public abstract class CachedPrimaryGroupHolder extends StoredHolder implements StateListener {

    // cache lookups
    private final Cache<String> cache = new Cache<String>() {
        @Override
        protected String supply() {
            return calculateValue();
        }
    };

    public CachedPrimaryGroupHolder(User user) {
        super(user);
        user.getStateListeners().add(this);
    }

    protected abstract String calculateValue();

    @Override
    public String getValue() {
        String s = cache.get();
        return s != null ? s : getStoredValue().orElse(NodeFactory.DEFAULT_GROUP_NAME);
    }

    @Override
    public void onStateChange() {
        cache.invalidate();
    }

}
