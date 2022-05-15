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
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Consumer;

public class EventBusUtil {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    @SuppressWarnings("unchecked")
    public static <T extends Event> void register(Object target) {
        for (Method method : target.getClass().getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            SubscribeEvent subscribeEvent = method.getAnnotation(SubscribeEvent.class);
            if (subscribeEvent == null) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
                throw new IllegalArgumentException(""
                        + "Method " + method + " has @SubscribeEvent annotation. "
                        + "It has " + parameterTypes.length + " arguments, "
                        + "but event handler methods require a single argument only."
                );
            }

            Class<?> eventType = parameterTypes[0];
            if (!Event.class.isAssignableFrom(eventType)) {
                throw new IllegalArgumentException(""
                        + "Method " + method + " has @SubscribeEvent annotation, "
                        + "but takes an argument that is not an Event subtype: " + eventType
                );
            }

            Consumer<T> consumer;
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

                consumer = (Consumer<T>) callSite.getTarget().bindTo(target).invokeExact();
            } catch (Throwable t) {
                throw new RuntimeException("Error whilst registering " + method, t);
            }

            MinecraftForge.EVENT_BUS.addListener(subscribeEvent.priority(), subscribeEvent.receiveCanceled(), (Class<T>) eventType, consumer);
        }
    }

}
