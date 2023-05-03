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

package me.lucko.luckperms.common.locale;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.command.spec.Argument;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.context.ImmutableContextSetImpl;
import me.lucko.luckperms.common.extension.SimpleExtensionManager;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.InheritanceOrigin;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.PermissionHolderIdentifier;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Meta;
import me.lucko.luckperms.common.node.types.Permission;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.storage.Storage;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentIteratorFlag;
import net.kyori.adventure.text.ComponentIteratorType;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.util.UTF8ResourceBundleControl;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.metadata.types.InheritanceOriginMetadata;
import net.luckperms.api.node.types.ChatMetaNode;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.platform.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class MessageTest {

    private static final Set<Class<?>> MESSAGE_CLASSES = ImmutableSet.of(
            Message.Args0.class,
            Message.Args1.class,
            Message.Args2.class,
            Message.Args3.class,
            Message.Args4.class,
            Message.Args5.class,
            Message.Args6.class
    );

    private static final Set<String> IGNORED_MISSING_TRANSLATION_KEYS = ImmutableSet.of(
            "luckperms.command.misc.invalid-input-empty-stub"
    );

    private static TranslationRegistry registry;
    private static Set<String> translationKeys;

    @BeforeAll
    public static void setupRenderer() {
        registry = TranslationRegistry.create(Key.key("luckperms", "test"));

        ResourceBundle bundle = ResourceBundle.getBundle("luckperms", Locale.ENGLISH, UTF8ResourceBundleControl.get());
        translationKeys = ImmutableSet.copyOf(bundle.keySet());
        registry.registerAll(Locale.ENGLISH, bundle, false);
    }

    private static Stream<Field> getMessageFields() {
        return Arrays.stream(Message.class.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .filter(f -> MESSAGE_CLASSES.contains(f.getType()));
    }

    @ParameterizedTest
    @MethodSource("getMessageFields")
    public void testMessage(Field field) {
        Component baseComponent = buildMessage(field);
        for (Component part : getNestedComponents(baseComponent)) {
            if (part instanceof TranslatableComponent) {
                TranslatableComponent component = (TranslatableComponent) part;
                assertTranslatableComponentValid(component);
            }
        }
    }

    @ParameterizedTest
    @EnumSource
    public void testCommandUsageMessages(CommandSpec commandSpec) {
        assertTranslatableComponentValid(commandSpec.description());

        List<Argument> args = commandSpec.args();
        if (args != null) {
            for (Argument arg : args) {
                assertTranslatableComponentValid(arg.getDescription());
            }
        }
    }

    private static void assertTranslatableComponentValid(TranslatableComponent component) {
        String key = component.key();

        if (IGNORED_MISSING_TRANSLATION_KEYS.contains(key)) {
            return;
        }

        assertTrue(translationKeys.contains(key), "unknown translation key: " + key);

        List<Component> args = component.args();
        MessageFormat fmt = registry.translate(key, Locale.ENGLISH);
        assertNotNull(fmt);
        assertEquals(fmt.getFormats().length, args.size(), "number of formats in translation for " + key + " does not match number of arguments");
    }

    private static Iterable<Component> getNestedComponents(Component component) {
        return component.iterable(
                ComponentIteratorType.BREADTH_FIRST,
                ImmutableSet.of(ComponentIteratorFlag.INCLUDE_TRANSLATABLE_COMPONENT_ARGUMENTS, ComponentIteratorFlag.INCLUDE_HOVER_SHOW_TEXT_COMPONENT)
        );
    }

    private static Component buildMessage(Field field) {
        Class<?> type = field.getType();

        List<Method> buildMethods = Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.getName().equals("build"))
                .collect(Collectors.toList());

        if (buildMethods.size() != 1) {
            throw new IllegalStateException("Expected exactly one build() method - " + buildMethods);
        }

        Method buildMethod = buildMethods.get(0);
        Object[] parameters = new Object[buildMethod.getParameterCount()];

        if (buildMethod.getParameterCount() != 0) {
            Type genericType = field.getGenericType();
            Type[] typeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
            for (int i = 0; i < typeArguments.length; i++) {
                Type typeArgument = typeArguments[i];
                parameters[i] = mockArgument(typeArgument);
            }
        }

        try {
            Object builder = field.get(null);
            return (Component) buildMethod.invoke(builder, parameters);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object mockArgument(Type type) {
        if (type instanceof ParameterizedType) {
            return mockArgument(((ParameterizedType) type).getRawType());
        }

        Class<?> clazz = (Class<?>) type;

        if (clazz == String.class) {
            return "stub";
        } else if (clazz == Integer.class) {
            return 0;
        } else if (clazz == Boolean.class) {
            return false;
        } else if (clazz == Double.class) {
            return 0d;
        } else if (clazz == LoggedAction.class) {
            return LoggedAction.build()
                    .source(UUID.randomUUID())
                    .sourceName("stub")
                    .targetType(Action.Target.Type.GROUP)
                    .targetName("stub")
                    .description("stub")
                    .build();
        } else if (clazz == Node.class) {
            return Permission.builder().permission("stub").expiry(1, TimeUnit.MINUTES).build();
        } else if (clazz == InheritanceNode.class) {
            return Inheritance.builder().group("stub").expiry(1, TimeUnit.MINUTES).build();
        } else if (clazz == MetaNode.class) {
            return Meta.builder("stub", "stub")
                    .withMetadata(InheritanceOriginMetadata.KEY, new InheritanceOrigin(
                            new PermissionHolderIdentifier(HolderType.GROUP, "stub"),
                            DataType.NORMAL
                    ))
                    .build();
        } else if (clazz == ChatMetaNode.class) {
            return Prefix.builder("stub", 1)
                    .withMetadata(InheritanceOriginMetadata.KEY, new InheritanceOrigin(
                            new PermissionHolderIdentifier(HolderType.GROUP, "stub"),
                            DataType.NORMAL
                    ))
                    .build();
        } else if (clazz == ContextSet.class) {
            return ImmutableContextSetImpl.of("stub", "stub");
        } else if (clazz == Component.class) {
            return Component.text("stub");
        } else if (clazz == List.class) {
            return ImmutableList.of();
        } else if (clazz == Collection.class) {
            return ImmutableList.of();
        }

        Object mock;
        if (clazz == LuckPermsPlugin.class) {
            mock = mock(clazz, Answers.RETURNS_DEEP_STUBS);
        } else {
            mock = mock(clazz, Answers.RETURNS_SMART_NULLS);
        }

        if (mock instanceof LuckPermsBootstrap) {
            LuckPermsBootstrap bootstrap = (LuckPermsBootstrap) mock;
            lenient().when(bootstrap.getType()).thenReturn(Platform.Type.BUKKIT);
            lenient().when(bootstrap.getStartupTime()).thenReturn(Instant.now());
        } else if (mock instanceof LuckPermsPlugin) {
            LuckPermsPlugin plugin = (LuckPermsPlugin) mock;

            LuckPermsBootstrap bootstrap = (LuckPermsBootstrap) mockArgument(LuckPermsBootstrap.class);
            lenient().when(plugin.getBootstrap()).thenReturn(bootstrap);

            Storage storage = (Storage) mockArgument(Storage.class);
            lenient().when(plugin.getStorage()).thenReturn(storage);

            SimpleExtensionManager extManager = (SimpleExtensionManager) mockArgument(SimpleExtensionManager.class);
            lenient().when(plugin.getExtensionManager()).thenReturn(extManager);

            lenient().when(plugin.getMessagingService()).thenReturn(Optional.empty());
        } else if (mock instanceof PermissionHolder) {
            PermissionHolder holder = (PermissionHolder) mock;

            LuckPermsPlugin plugin = (LuckPermsPlugin) mockArgument(LuckPermsPlugin.class);
            lenient().when(holder.getPlugin()).thenReturn(plugin);

            lenient().when(holder.getFormattedDisplayName()).thenReturn(Component.text("stub"));
        } else if (mock instanceof SimpleExtensionManager) {
            SimpleExtensionManager manager = (SimpleExtensionManager) mock;
            lenient().when(manager.getLoadedExtensions()).thenReturn(ImmutableList.of());
        }

        return mock;
    }

}
