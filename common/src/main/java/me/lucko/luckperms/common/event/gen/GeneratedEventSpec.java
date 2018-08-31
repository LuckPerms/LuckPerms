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

package me.lucko.luckperms.common.event.gen;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.event.LuckPermsEvent;
import me.lucko.luckperms.api.event.Param;
import me.lucko.luckperms.common.utils.ImmutableCollectors;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Represents the generated specification for an instance of a given {@link LuckPermsEvent}.
 */
public class GeneratedEventSpec {

    private static final Method TO_STRING_METHOD;
    private static final Method EQUALS_METHOD;
    private static final Method HASHCODE_METHOD;
    private static final Method GET_API_METHOD;
    static {
        try {
            TO_STRING_METHOD = Object.class.getMethod("toString");
            EQUALS_METHOD = Object.class.getMethod("equals", Object.class);
            HASHCODE_METHOD = Object.class.getMethod("hashCode");
            GET_API_METHOD = LuckPermsEvent.class.getMethod("getApi");
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final LoadingCache<Class<? extends LuckPermsEvent>, GeneratedEventSpec> CACHE = Caffeine.newBuilder()
            .build(GeneratedEventSpec::new);

    public static GeneratedEventSpec lookup(Class<? extends LuckPermsEvent> event) {
        return CACHE.get(event);
    }

    private final Class<? extends LuckPermsEvent> eventClass;
    private final List<Method> methods;
    private final List<Class<?>> returnTypes;

    private GeneratedEventSpec(Class<? extends LuckPermsEvent> eventClass) {
        this.eventClass = eventClass;

        List<Method> methods = new ArrayList<>();
        for (Method method : eventClass.getMethods()) {
            if (method.isDefault()) {
                continue;
            }
            if (GET_API_METHOD.equals(method)) {
                continue;
            }

            methods.add(method);
        }
        methods.sort(Comparator.comparingInt(o -> o.isAnnotationPresent(Param.class) ? o.getAnnotation(Param.class).value() : 0));
        this.methods = ImmutableList.copyOf(methods);

        this.returnTypes = this.methods.stream()
                .map(Method::getReturnType)
                .collect(ImmutableCollectors.toList());
    }

    public LuckPermsEvent newInstance(LuckPermsApi api, Object... params) {
        if (params.length != this.methods.size()) {
            throw new IllegalStateException("param length differs from number of methods. expected " + this.methods.size() + " - " + this.methods);
        }

        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            Class<?> expectedType = this.returnTypes.get(i);
            if (!expectedType.isInstance(param)) {
                throw new IllegalArgumentException("Parameter at index " + i + " cannot be assigned to " + expectedType);
            }
        }

        EventInvocationHandler eventInvocationHandler = new EventInvocationHandler(api, params);
        return (LuckPermsEvent) Proxy.newProxyInstance(GeneratedEventSpec.class.getClassLoader(), new Class[]{this.eventClass}, eventInvocationHandler);
    }

    /**
     * An invocation handler bound to a set of parameters, used to implement the event as a proxy.
     */
    private final class EventInvocationHandler implements InvocationHandler {
        private final LuckPermsApi api;
        private final Object[] fields;

        EventInvocationHandler(LuckPermsApi api, Object[] fields) {
            this.api = api;
            this.fields = fields;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (TO_STRING_METHOD.equals(method)) {
                return "GeneratedEvent(" +
                        "proxy=" + proxy.getClass().getName() + "@" + Integer.toHexString(proxy.hashCode()) + ", " +
                        "class=" + GeneratedEventSpec.this.eventClass.getName() + ", " +
                        "fields=" + Arrays.toString(this.fields) + ")";
            }
            if (EQUALS_METHOD.equals(method)) {
                return proxy == args[0];
            }
            if (HASHCODE_METHOD.equals(method)) {
                return System.identityHashCode(proxy);
            }
            if (GET_API_METHOD.equals(method)) {
                return this.api;
            }

            if (method.getDeclaringClass() == Object.class || method.isDefault()) {
                return MethodHandles.lookup()
                        .in(method.getDeclaringClass())
                        .unreflectSpecial(method, method.getDeclaringClass())
                        .bindTo(proxy)
                        .invokeWithArguments(args);
            }

            int methodIndex = GeneratedEventSpec.this.methods.indexOf(method);
            if (methodIndex == -1) {
                throw new UnsupportedOperationException("Method not supported: " + method);
            }

            return this.fields[methodIndex];
        }
    }

}
