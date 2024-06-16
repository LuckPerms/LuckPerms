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

package me.lucko.luckperms.common.storage.misc;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Objects;

public class StorageCredentials {

    private final String address;
    private final String database;
    private final String username;
    private final String password;
    private final int maxPoolSize;
    private final int minIdleConnections;
    private final int maxLifetime;
    private final int keepAliveTime;
    private final int connectionTimeout;
    private final Map<String, String> properties;

    public StorageCredentials(String address, String database, String username, String password, int maxPoolSize, int minIdleConnections, int maxLifetime, int keepAliveTime, int connectionTimeout, Map<String, String> properties) {
        this.address = address;
        this.database = database;
        this.username = username;
        this.password = password;
        this.maxPoolSize = maxPoolSize;
        this.minIdleConnections = minIdleConnections;
        this.maxLifetime = maxLifetime;
        this.keepAliveTime = keepAliveTime;
        this.connectionTimeout = connectionTimeout;
        this.properties = properties;
    }

    public StorageCredentials(String address, String database, String username, String password) {
        this(address, database, username, password, 10, 10, 1800000, 0, 5000, ImmutableMap.of());
    }

    public String getAddress() {
        return Objects.requireNonNull(this.address, "address");
    }

    public String getDatabase() {
        return Objects.requireNonNull(this.database, "database");
    }

    public String getUsername() {
        return Objects.requireNonNull(this.username, "username");
    }

    public String getPassword() {
        return Objects.requireNonNull(this.password, "password");
    }

    public int getMaxPoolSize() {
        return this.maxPoolSize;
    }

    public int getMinIdleConnections() {
        return this.minIdleConnections;
    }

    public int getMaxLifetime() {
        return this.maxLifetime;
    }

    public int getKeepAliveTime() {
        return this.keepAliveTime;
    }

    public int getConnectionTimeout() {
        return this.connectionTimeout;
    }

    public Map<String, String> getProperties() {
        return this.properties;
    }
}
