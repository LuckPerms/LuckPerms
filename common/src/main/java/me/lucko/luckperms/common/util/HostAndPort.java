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

package me.lucko.luckperms.common.util;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * A wrapper around Guava's HostAndPort to account for the method name change of getHostText().
 */
@SuppressWarnings("UnstableApiUsage")
public class HostAndPort {
    private static final Method GET_HOST_METHOD;

    static {
        Method getHostMethod = null;
        try {
            getHostMethod = com.google.common.net.HostAndPort.class.getMethod("getHostText");
        } catch (NoSuchMethodException e) {
            try {
                getHostMethod = com.google.common.net.HostAndPort.class.getMethod("getHost");
            } catch (NoSuchMethodException ex) {
                // ignore
            }
        }
        Objects.requireNonNull(getHostMethod);
        GET_HOST_METHOD = getHostMethod;
    }

    private com.google.common.net.HostAndPort delegate;

    public HostAndPort(String hostAndPort) {
        this.delegate = com.google.common.net.HostAndPort.fromString(hostAndPort);
    }

    public HostAndPort withDefaultPort(int defaultPort) {
        this.delegate = this.delegate.withDefaultPort(defaultPort);
        return this;
    }

    public HostAndPort requireBracketsForIPv6() {
        this.delegate.requireBracketsForIPv6();
        return this;
    }

    public String getHost() {
        try {
            return (String) GET_HOST_METHOD.invoke(this.delegate);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public int getPort() {
        return this.delegate.getPort();
    }

}
