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

import com.velocitypowered.api.command.CommandSource;
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

    private static final Method PLATFORM_SERIALIZER_DESERIALIZE;
    private static final Method PLATFORM_SEND_MESSAGE;
    private static final Method PLATFORM_COMPONENT_RESULT_DENIED;
    private static final Object PLATFORM_SERIALIZER_INSTANCE;

    static {
        String adventurePkg = "net.kyo".concat("ri.adventure.");
        try {
            Class<?> audienceClass = Class.forName(adventurePkg + "audience.Audience");
            Class<?> componentClass = Class.forName(adventurePkg + "text.Component");
            Class<?> serializerClass = Class.forName(adventurePkg + "text.serializer.gson.GsonComponentSerializer");

            PLATFORM_SERIALIZER_DESERIALIZE = serializerClass.getMethod("deserialize", Object.class);
            PLATFORM_SEND_MESSAGE = audienceClass.getMethod("sendMessage", componentClass);
            PLATFORM_COMPONENT_RESULT_DENIED = ComponentResult.class.getMethod("denied", componentClass);
            PLATFORM_SERIALIZER_INSTANCE = serializerClass.getMethod("gson").invoke(null);
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

    public static void sendMessage(CommandSource audience, Component message) {
        try {
            PLATFORM_SEND_MESSAGE.invoke(audience, toPlatformComponent(message));
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
