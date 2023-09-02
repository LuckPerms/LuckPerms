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

package me.lucko.luckperms.standalone.utils;

import me.lucko.luckperms.standalone.app.integration.CommandExecutor;
import me.lucko.luckperms.standalone.app.integration.SingletonPlayer;
import me.lucko.luckperms.standalone.utils.TestPluginBootstrap.TestPlugin;
import me.lucko.luckperms.standalone.utils.TestPluginBootstrap.TestSenderFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.util.Tristate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Utility for testing LuckPerms commands with BDD-like given/when/then assertions.
 */
public final class CommandTester implements Consumer<Component>, Function<String, Tristate> {

    private static final Logger LOGGER = LogManager.getLogger(CommandTester.class);

    /** The test plugin */
    private final TestPlugin plugin;

    /** The LuckPerms command executor */
    private final CommandExecutor executor;

    /** The current map of permissions held by the fake executor */
    private Map<String, Tristate> permissions = null;

    /** A set of the permissions that have been checked for */
    private final Set<String> checkedPermissions = Collections.synchronizedSet(new HashSet<>());

    /** A buffer of messages received by the test tool */
    private final List<Component> messageBuffer = Collections.synchronizedList(new ArrayList<>());

    public CommandTester(TestPlugin plugin, CommandExecutor executor) {
        this.plugin = plugin;
        this.executor = executor;
    }

    /**
     * Accept a message and add it to the buffer.
     *
     * @param component the message
     */
    @Override
    public void accept(Component component) {
        this.messageBuffer.add(component);
    }

    /**
     * Perform a permission check for the fake executor
     *
     * @param permission the permission
     * @return the result of the permission check
     */
    @Override
    public Tristate apply(String permission) {
        if (this.permissions == null) {
            this.checkedPermissions.add(permission);
            return Tristate.TRUE;
        } else {
            Tristate result = this.permissions.getOrDefault(permission, Tristate.UNDEFINED);
            if (result != Tristate.UNDEFINED) {
                this.checkedPermissions.add(permission);
            }
            return result;
        }
    }

    /**
     * Marks that the fake executor should have all permissions
     *
     * @return this
     */
    public CommandTester givenHasAllPermissions() {
        this.permissions = null;
        return this;
    }

    /**
     * Marks that the fake executor should have the given permissions
     *
     * @return this
     */
    public CommandTester givenHasPermissions(String... permissions) {
        this.permissions = new HashMap<>();
        for (String permission : permissions) {
            this.permissions.put(permission, Tristate.TRUE);
        }
        return this;
    }

    /**
     * Execute a command using the {@link CommandExecutor} and capture output to this test instance.
     *
     * @param command the command to run
     * @return this
     */
    public CommandTester whenRunCommand(String command) {
        LOGGER.info("Executing test command: " + command);

        TestSenderFactory senderFactory = this.plugin.getSenderFactory();
        senderFactory.setPermissionChecker(this);
        SingletonPlayer.INSTANCE.addMessageSink(this);

        this.executor.execute(command).join();

        SingletonPlayer.INSTANCE.removeMessageSink(this);
        senderFactory.resetPermissionChecker();
        return this;
    }

    /**
     * Asserts that the current contents of the message buffer matches the given input string.
     *
     * @param expected the expected contents
     * @return this
     */
    public CommandTester thenExpect(String expected) {
        String actual = this.renderBuffer();
        assertEquals(expected.trim(), actual.trim());

        if (this.permissions != null) {
            assertEquals(this.checkedPermissions, this.permissions.keySet());
        }

        return this.clearMessageBuffer();
    }

    /**
     * Asserts that the current contents of the message buffer starts with the given input string.
     *
     * @param expected the expected contents
     * @return this
     */
    public CommandTester thenExpectStartsWith(String expected) {
        String actual = this.renderBuffer();
        assertTrue(actual.trim().startsWith(expected.trim()), "expected '" + actual + "' to start with '" + expected + "'");

        if (this.permissions != null) {
            assertEquals(this.checkedPermissions, this.permissions.keySet());
        }

        return this.clearMessageBuffer();
    }

    /**
     * Clears the message buffer.
     *
     * @return this
     */
    public CommandTester clearMessageBuffer() {
        this.messageBuffer.clear();
        this.checkedPermissions.clear();
        return this;
    }

    /**
     * Renders the contents of the message buffer as a stream of lines.
     *
     * @return rendered copy of the buffer
     */
    public Stream<String> renderBufferStream() {
        return this.messageBuffer.stream().map(component -> PlainTextComponentSerializer.plainText().serialize(component));
    }

    /**
     * Renders the contents of the message buffer as a joined string.
     *
     * @return rendered copy of the buffer
     */
    public String renderBuffer() {
        return this.renderBufferStream().map(String::trim).collect(Collectors.joining("\n"));
    }

    /**
     * Prints test case source code to stdout to test the given command.
     *
     * @param cmd the command
     * @return this
     */
    public CommandTester outputTest(String cmd) {
        this.whenRunCommand(cmd);

        String checkedPermissions = this.checkedPermissions.stream()
                .map(s -> "\"" + s + "\"")
                .collect(Collectors.joining(", "));

        System.out.printf(".givenHasPermissions(%s)%n", checkedPermissions);
        System.out.printf(".whenRunCommand(\"%s\")%n", cmd);

        List<String> render = this.renderBufferStream().toList();
        if (render.size() == 1) {
            System.out.printf(".thenExpect(\"%s\")%n", render.get(0));
        } else {
            System.out.println(".thenExpect(\"\"\"");
            for (String s : render) {
                System.out.println("        " + s);
            }
            System.out.println("        \"\"\"");
            System.out.println(")");
        }

        System.out.println();
        return this.clearMessageBuffer();
    }

}
