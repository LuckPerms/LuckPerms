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

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.cache.LoadingMap;
import me.lucko.luckperms.common.util.ImmutableCollectors;
import me.lucko.luckperms.common.util.PrivateMethodHandles;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.LuckPermsEvent;
import net.luckperms.api.event.util.Param;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Represents the generated specification for an instance of a given {@link LuckPermsEvent}.
 */
public class GeneratedEventSpec {

    private static final Method TO_STRING_METHOD;
    private static final Method EQUALS_METHOD;
    private static final Method HASHCODE_METHOD;
    private static final Method GET_LUCKPERMS_METHOD;
    private static final Method GET_EVENT_TYPE_METHOD;
    static {
        try {
            TO_STRING_METHOD = Object.class.getMethod("toString");
            EQUALS_METHOD = Object.class.getMethod("equals", Object.class);
            HASHCODE_METHOD = Object.class.getMethod("hashCode");
            GET_LUCKPERMS_METHOD = LuckPermsEvent.class.getMethod("getLuckPerms");
            GET_EVENT_TYPE_METHOD = LuckPermsEvent.class.getMethod("getEventType");
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static final Map<Class<? extends LuckPermsEvent>, GeneratedEventSpec> CACHE = LoadingMap.of(GeneratedEventSpec::new);

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
            if (GET_LUCKPERMS_METHOD.equals(method) || GET_EVENT_TYPE_METHOD.equals(method)) {
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

    public LuckPermsEvent newInstance(LuckPerms api, Object... params) {
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
        private final LuckPerms api;
        private final Object[] fields;

        EventInvocationHandler(LuckPerms api, Object[] fields) {
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
            if (GET_LUCKPERMS_METHOD.equals(method)) {
                return this.api;
            }
            if (GET_EVENT_TYPE_METHOD.equals(method)) {
                return GeneratedEventSpec.this.eventClass;
            }

            if (method.getDeclaringClass() == Object.class || method.isDefault()) {
                return PrivateMethodHandles.privateLookup(method.getDeclaringClass())
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
