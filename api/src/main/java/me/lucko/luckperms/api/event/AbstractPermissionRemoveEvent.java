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

package me.lucko.luckperms.api.event;

import me.lucko.luckperms.api.PermissionHolder;

import java.util.Optional;

public class AbstractPermissionRemoveEvent extends TargetedEvent<PermissionHolder> {

    private final String server;
    private final String world;
    private final boolean temporary;

    protected AbstractPermissionRemoveEvent(String eventName, PermissionHolder target, String server, String world, boolean temporary) {
        super(eventName, target);
        this.server = server;
        this.world = world;
        this.temporary = temporary;
    }

    public Optional<String> getServer() {
        return Optional.ofNullable(server);
    }

    public Optional<String> getWorld() {
        return Optional.ofNullable(world);
    }

    /**
     * @deprecated use {@link #isTemporary()}
     */
    @Deprecated
    public boolean getTemporary() {
        return temporary;
    }

    public boolean isTemporary() {
        return temporary;
    }

}
