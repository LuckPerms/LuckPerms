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

package me.lucko.luckperms.common.storage.implementation.sql.connection.file;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.MethodDelegation;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.sql.Connection;
import java.sql.SQLException;

import static net.bytebuddy.matcher.ElementMatchers.isDeclaredBy;
import static net.bytebuddy.matcher.ElementMatchers.isFinal;
import static net.bytebuddy.matcher.ElementMatchers.not;

/**
 * A wrapper around a {@link Connection} which blocks usage of the default {@link #close()} method.
 */
public abstract class NonClosableConnection implements Connection {

    private static final MethodHandle CONSTRUCTOR;
    static {
        // construct an implementation of NonClosableConnection
        Class<? extends NonClosableConnection> implClass = new ByteBuddy()
                .subclass(NonClosableConnection.class, ConstructorStrategy.Default.IMITATE_SUPER_CLASS_OPENING)
                .name(NonClosableConnection.class.getName() + "Impl")
                .method(not(isFinal()).and(not(isDeclaredBy(Object.class))))
                    .intercept(MethodDelegation.toField("delegate"))
                .make()
                .load(NonClosableConnection.class.getClassLoader())
                .getLoaded();

        try {
            CONSTRUCTOR = MethodHandles.publicLookup().in(implClass)
                    .findConstructor(implClass, MethodType.methodType(void.class, Connection.class))
                    .asType(MethodType.methodType(NonClosableConnection.class, Connection.class));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Creates a {@link NonClosableConnection} that delegates calls to the given {@link Connection}.
     *
     * @param connection the connection to wrap
     * @return a non closable connection
     */
    static NonClosableConnection wrap(Connection connection) {
        try {
            return (NonClosableConnection) CONSTRUCTOR.invokeExact(connection);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    protected final Connection delegate;

    protected NonClosableConnection(Connection delegate) {
        this.delegate = delegate;
    }

    /**
     * Actually {@link #close() closes} the underlying connection.
     */
    public final void shutdown() throws SQLException {
        this.delegate.close();
    }

    @Override
    public final void close() throws SQLException {
        // do nothing
    }

    @Override
    public final boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this.delegate) || this.delegate.isWrapperFor(iface);
    }

    @SuppressWarnings("unchecked")
    @Override
    public final <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this.delegate)) {
            return (T) this.delegate;
        }
        return this.delegate.unwrap(iface);
    }
}
