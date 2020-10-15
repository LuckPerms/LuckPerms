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

package me.lucko.luckperms.velocity.util;

import com.velocitypowered.api.event.ResultedEvent.ComponentResult;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.lang.reflect.Method;

/**
 * Converts between platform and native adventure objects.
 *
 * We shade + relocate adventure in LuckPerms. This is because we use a slightly modified version.
 * Unfortunately (for us), Velocity also uses the same library. This class converts between "our"
 * adventure components and Velocity's.
 */
public final class AdventureCompat {
    private AdventureCompat() {}

    private static final String PLATFORM_ADVENTURE_PACKAGE;

    private static final Class<?> PLATFORM_AUDIENCE;
    private static final Class<?> PLATFORM_IDENTITY;
    private static final Class<?> PLATFORM_COMPONENT;
    private static final Class<?> PLATFORM_SERIALIZER;

    private static final Method PLATFORM_SERIALIZER_GETTER;
    private static final Method PLATFORM_SERIALIZER_DESERIALIZE;
    private static final Method PLATFORM_IDENTITY_NIL_GETTER;
    private static final Method PLATFORM_SEND_MESSAGE;
    private static final Method PLATFORM_COMPONENT_RESULT_DENIED;

    private static final Object PLATFORM_SERIALIZER_INSTANCE;
    private static final Object PLATFORM_IDENTITY_INSTANCE;

    static {
        PLATFORM_ADVENTURE_PACKAGE = "net.kyo".concat("ri.adventure.");
        try {
            PLATFORM_AUDIENCE = Class.forName(PLATFORM_ADVENTURE_PACKAGE + "audience.Audience");
            PLATFORM_IDENTITY = Class.forName(PLATFORM_ADVENTURE_PACKAGE + "identity.Identity");
            PLATFORM_COMPONENT = Class.forName(PLATFORM_ADVENTURE_PACKAGE + "text.Component");
            PLATFORM_SERIALIZER = Class.forName(PLATFORM_ADVENTURE_PACKAGE + "text.serializer.gson.GsonComponentSerializer");

            PLATFORM_SERIALIZER_GETTER = PLATFORM_SERIALIZER.getMethod("gson");
            PLATFORM_SERIALIZER_DESERIALIZE = PLATFORM_SERIALIZER.getMethod("deserialize", Object.class);
            PLATFORM_IDENTITY_NIL_GETTER = PLATFORM_IDENTITY.getMethod("nil");
            PLATFORM_SEND_MESSAGE = PLATFORM_AUDIENCE.getMethod("sendMessage", PLATFORM_IDENTITY, PLATFORM_COMPONENT);
            PLATFORM_COMPONENT_RESULT_DENIED = ComponentResult.class.getMethod("denied", PLATFORM_COMPONENT);

            PLATFORM_SERIALIZER_INSTANCE = PLATFORM_SERIALIZER_GETTER.invoke(null);
            PLATFORM_IDENTITY_INSTANCE = PLATFORM_IDENTITY_NIL_GETTER.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Object toPlatformComponent(Component component) {
        String json = GsonComponentSerializer.gson().serialize(component);
        try {
            return PLATFORM_SERIALIZER_DESERIALIZE.invoke(PLATFORM_SERIALIZER_INSTANCE, json);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendMessage(Object audience, Component message) {
        try {
            PLATFORM_SEND_MESSAGE.invoke(audience, PLATFORM_IDENTITY_INSTANCE, toPlatformComponent(message));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static ComponentResult deniedResult(Component message) {
        try {
            return (ComponentResult) PLATFORM_COMPONENT_RESULT_DENIED.invoke(null, toPlatformComponent(message));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

}
