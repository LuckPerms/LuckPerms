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

package me.lucko.luckperms.api.event.events;

import me.lucko.luckperms.api.PermissionHolder;
import me.lucko.luckperms.api.event.AbstractPermissionAddEvent;

import java.util.AbstractMap;
import java.util.Map;

/**
 * Called whenever a user or group has a permission set.
 * @deprecated in favour of {@link PermissionNodeSetEvent}
 */
@Deprecated
public class PermissionSetEvent extends AbstractPermissionAddEvent {

    private final String node;
    private final boolean value;

    public PermissionSetEvent(PermissionHolder target, String node, boolean value, String server, String world, long expiry) {
        super("Permission Set Event", target, server, world, expiry);
        this.node = node;
        this.value = value;
    }

    public String getNode() {
        return node;
    }

    public boolean getValue() {
        return value;
    }

    public Map.Entry<String, Boolean> getEntry() {
        return new AbstractMap.SimpleEntry<>(node, value);
    }
}
