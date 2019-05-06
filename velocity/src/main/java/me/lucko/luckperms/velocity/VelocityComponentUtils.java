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

package me.lucko.luckperms.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.ResultedEvent;

import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Utility for handling text components on Velocity.
 *
 * <p>Velocity bundles an old, incompatible version of the text library used by LuckPerms.
 * The package remains the same - so we have to use this reflection hack to convert our
 * relocated Component object to one Velocity will accept.</p>
 */
public class VelocityComponentUtils {
    private static final String KYORI_TEXT_PACKAGE_NO_RELOCATION = "net#kyori#text#".replace("#", ".");
    private static final Class<?> COMPONENT_CLASS;
    private static final Method DESERIALIZE_METHOD;
    private static final Object SERIALIZER;

    private static final Method SEND_MESSAGE_METHOD;
    private static final Method COMPONENT_RESULT_DENIED_CONSTRUCTOR;

    static {
        try {
            COMPONENT_CLASS = kyoriClass("Component");
            Class<?> componentSerializerClass = kyoriClass("serializer.ComponentSerializer");
            Class<?> componentSerializersClass = kyoriClass("serializer.ComponentSerializers");
            DESERIALIZE_METHOD = componentSerializerClass.getMethod("deserialize", Object.class);
            Field jsonSerializerField = componentSerializersClass.getField("JSON");
            SERIALIZER = jsonSerializerField.get(null);

            SEND_MESSAGE_METHOD = CommandSource.class.getMethod("sendMessage", COMPONENT_CLASS);
            COMPONENT_RESULT_DENIED_CONSTRUCTOR = ResultedEvent.ComponentResult.class.getMethod("denied", COMPONENT_CLASS);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Class<?> kyoriClass(String name) throws ClassNotFoundException {
        return Class.forName(KYORI_TEXT_PACKAGE_NO_RELOCATION + name);
    }

    private static Object convertComponent(Component component) {
        String json = GsonComponentSerializer.INSTANCE.serialize(component);
        try {
            return DESERIALIZE_METHOD.invoke(SERIALIZER, json);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendMessage(CommandSource source, Component message) {
        try {
            SEND_MESSAGE_METHOD.invoke(source, convertComponent(message));
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static ResultedEvent.ComponentResult createDeniedResult(Component component) {
        try {
            return (ResultedEvent.ComponentResult) COMPONENT_RESULT_DENIED_CONSTRUCTOR.invoke(null, convertComponent(component));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

}
