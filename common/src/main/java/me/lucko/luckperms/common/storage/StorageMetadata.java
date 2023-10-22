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

package me.lucko.luckperms.common.storage;

public class StorageMetadata {

    // remote
    private Boolean connected;
    private Integer ping;

    // local
    private Long sizeBytes;

    public Boolean connected() {
        return this.connected;
    }

    public Integer ping() {
        return this.ping;
    }

    public Long sizeBytes() {
        return this.sizeBytes;
    }

    public StorageMetadata connected(boolean connected) {
        this.connected = connected;
        return this;
    }

    public StorageMetadata ping(int ping) {
        this.ping = ping;
        return this;
    }

    public StorageMetadata sizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
        return this;
    }

    public StorageMetadata combine(StorageMetadata other) {
        if (this.connected == null || (other.connected != null && !other.connected)) {
            this.connected = other.connected;
        }
        if (this.ping == null || (other.ping != null && other.ping > this.ping)) {
            this.ping = other.ping;
        }
        if (this.sizeBytes == null || (other.sizeBytes != null && other.sizeBytes > this.sizeBytes)) {
            this.sizeBytes = other.sizeBytes;
        }
        return this;
    }

}
