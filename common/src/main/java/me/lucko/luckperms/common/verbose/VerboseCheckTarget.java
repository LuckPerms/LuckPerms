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

package me.lucko.luckperms.common.verbose;

import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.User;
import net.luckperms.api.model.PermissionHolder;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;
import java.util.UUID;

public class VerboseCheckTarget {

    public static final String USER_TYPE = PermissionHolder.Identifier.USER_TYPE;
    public static final String GROUP_TYPE = PermissionHolder.Identifier.GROUP_TYPE;
    public static final String INTERNAL_TYPE = "internal";

    public static VerboseCheckTarget user(User user) {
        return new VerboseCheckTarget(USER_TYPE, user.getPlainDisplayName(), user.getUniqueId());
    }

    public static VerboseCheckTarget group(Group group) {
        return new VerboseCheckTarget(GROUP_TYPE, group.getPlainDisplayName(), null);
    }

    public static VerboseCheckTarget internal(String name) {
        return new VerboseCheckTarget(INTERNAL_TYPE, name, null);
    }

    public static VerboseCheckTarget of(String type, String name) {
        return new VerboseCheckTarget(type, name, null);
    }

    private final String type;
    private final String name;
    private final UUID id;
    private final String desc;

    private VerboseCheckTarget(String type, String name, UUID id) {
        this.type = type;
        this.name = name;
        this.id = id;

        if (this.type.equals(USER_TYPE)) {
            this.desc = this.name;
        } else {
            this.desc = this.type + '/' + this.name;
        }
    }

    public String getType() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public @Nullable UUID getId() {
        return this.id;
    }

    public String describe() {
        return this.desc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VerboseCheckTarget that = (VerboseCheckTarget) o;
        return this.type.equals(that.type) && this.name.equals(that.name) && Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.type, this.name, this.id);
    }

    @Override
    public String toString() {
        return "VerboseCheckTarget{" +
                "type=" + this.type + ", " +
                "name=" + this.name + ", " +
                "id=" + this.id + '}';
    }
}
