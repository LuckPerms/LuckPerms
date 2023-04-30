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

package me.lucko.luckperms.common.model;

import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.metadata.types.InheritanceOriginMetadata;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

public class InheritanceOrigin implements InheritanceOriginMetadata {
    private final PermissionHolderIdentifier origin;
    private final DataType dataType;

    public InheritanceOrigin(PermissionHolderIdentifier origin, DataType dataType) {
        this.origin = Objects.requireNonNull(origin, "origin");
        this.dataType = Objects.requireNonNull(dataType, "dataType");
    }

    @Override
    public @NonNull PermissionHolderIdentifier getOrigin() {
        return this.origin;
    }

    @Override
    public @NonNull DataType getDataType() {
        return this.dataType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InheritanceOriginMetadata)) return false;
        InheritanceOriginMetadata that = (InheritanceOriginMetadata) o;
        return this.origin.equals(that.getOrigin()) && this.dataType == that.getDataType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.origin, getDataType());
    }

    @Override
    public String toString() {
        return this.origin + " (" + this.dataType + ")";
    }
}
