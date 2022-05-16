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

package me.lucko.luckperms.forge.util;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.GenericEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.IGenericEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.IModBusEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Consumer;

public class EventBusUtil {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static void register(Object target) {
        for (Method method : target.getClass().getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            SubscribeEvent subscribeEvent = method.getAnnotation(SubscribeEvent.class);
            if (subscribeEvent == null) {
                continue;
            }

            Type[] parameterTypes = method.getGenericParameterTypes();
            if (parameterTypes.length != 1) {
                throw new IllegalArgumentException(""
                        + "Method " + method + " has @SubscribeEvent annotation. "
                        + "It has " + parameterTypes.length + " arguments, "
                        + "but event handler methods require a single argument only."
                );
            }

            Type parameterType = parameterTypes[0];
            Class<?> eventType;
            Class<?> genericType;
            if (parameterType instanceof Class) {
                eventType = (Class<?>) parameterType;
                genericType = null;
            } else if (parameterType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) parameterType;

                Type rawType = parameterizedType.getRawType();
                if (rawType instanceof Class) {
                    eventType = (Class<?>) rawType;
                } else {
                    throw new UnsupportedOperationException("Raw Type " + rawType.getClass() + " is not supported");
                }

                Type[] typeArguments = parameterizedType.getActualTypeArguments();
                if (typeArguments.length != 1) {
                    throw new IllegalArgumentException(""
                            + "Method " + method + " has @SubscribeEvent annotation. "
                            + "It has a " + eventType + " argument, "
                            + "but generic events require a single type argument only."
                    );
                }

                Type typeArgument = typeArguments[0];
                if (typeArgument instanceof Class<?>) {
                    genericType = (Class<?>) typeArgument;
                } else {
                    throw new UnsupportedOperationException("Type Argument " + typeArgument.getClass() + " is not supported");
                }
            } else {
                throw new UnsupportedOperationException("Parameter Type " + parameterType.getClass() + " is not supported");
            }

            if (GenericEvent.class.isAssignableFrom(eventType) && genericType == null) {
                throw new IllegalArgumentException(""
                        + "Method " + method + " has @SubscribeEvent annotation, "
                        + "but the generic argument type cannot be determined for "
                        + "for the GenericEvent subtype: " + eventType
                );
            }

            if (!Event.class.isAssignableFrom(eventType)) {
                throw new IllegalArgumentException(""
                        + "Method " + method + " has @SubscribeEvent annotation, "
                        + "but takes an argument that is not an Event subtype: " + eventType
                );
            }

            Consumer<?> consumer;
            try {
                MethodHandle methodHandle = LOOKUP.unreflect(method);
                CallSite callSite = LambdaMetafactory.metafactory(
                        LOOKUP,
                        "accept",
                        MethodType.methodType(Consumer.class, target.getClass()),
                        MethodType.methodType(void.class, Object.class),
                        methodHandle,
                        MethodType.methodType(void.class, eventType)
                );

                consumer = (Consumer<?>) callSite.getTarget().bindTo(target).invokeExact();
            } catch (Throwable t) {
                throw new RuntimeException("Error whilst registering " + method, t);
            }

            IEventBus eventBus;
            if (IModBusEvent.class.isAssignableFrom(eventType)) {
                eventBus = FMLJavaModLoadingContext.get().getModEventBus();
            } else {
                eventBus = MinecraftForge.EVENT_BUS;
            }

            if (IGenericEvent.class.isAssignableFrom(eventType)) {
                addGenericListener(eventBus, genericType, subscribeEvent, eventType, consumer);
            } else {
                addListener(eventBus, subscribeEvent, eventType, consumer);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends GenericEvent<? extends F>, F> void addGenericListener(IEventBus eventBus, Class<?> genericClassFilter, SubscribeEvent subscribeEvent, Class<?> eventType, Consumer<?> consumer) {
        eventBus.addGenericListener((Class<F>) genericClassFilter, subscribeEvent.priority(), subscribeEvent.receiveCanceled(), (Class<T>) eventType, (Consumer<T>) consumer);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event> void addListener(IEventBus eventBus, SubscribeEvent subscribeEvent, Class<?> eventType, Consumer<?> consumer) {
        eventBus.addListener(subscribeEvent.priority(), subscribeEvent.receiveCanceled(), (Class<T>) eventType, (Consumer<T>) consumer);
    }

}
