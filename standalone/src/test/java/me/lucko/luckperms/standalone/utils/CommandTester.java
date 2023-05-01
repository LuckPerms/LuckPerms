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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Utility for testing LuckPerms commands with BDD-like given/when/then assertions.
 */
public final class CommandTester implements Consumer<Component> {

    private static final Logger LOGGER = LogManager.getLogger(CommandTester.class);

    /** The LuckPerms command executor */
    private final CommandExecutor executor;

    /** A buffer of messages received by the test tool */
    private final List<Component> messageBuffer = Collections.synchronizedList(new ArrayList<>());

    public CommandTester(CommandExecutor executor) {
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
     * Execute a command using the {@link CommandExecutor} and capture output to this test instance.
     *
     * @param command the command to run
     * @return this
     */
    public CommandTester givenCommand(String command) {
        LOGGER.info("Executing test command: " + command);

        SingletonPlayer.INSTANCE.addMessageSink(this);
        this.executor.execute(command).join();
        SingletonPlayer.INSTANCE.removeMessageSink(this);

        return this;
    }

    /**
     * Asserts that the current contents of the message buffer matches the given input string.
     *
     * @param expected the expected contents
     * @return this
     */
    public CommandTester thenExpect(String expected) {
        String actual = this.renderBuffer().stream()
                .map(String::trim)
                .collect(Collectors.joining("\n"));

        assertEquals(expected.trim(), actual.trim());

        return this.clearMessageBuffer();
    }

    /**
     * Clears the message buffer.
     *
     * @return this
     */
    public CommandTester clearMessageBuffer() {
        this.messageBuffer.clear();
        return this;
    }

    /**
     * Renders the contents of the message buffer.
     *
     * @return rendered copy of the buffer
     */
    public List<String> renderBuffer() {
        return this.messageBuffer.stream()
                .map(component -> PlainTextComponentSerializer.plainText().serialize(component))
                .collect(Collectors.toList());
    }

    /**
     * Prints test case source code to stdout to test the given command.
     *
     * @param cmd the command
     * @return this
     */
    public CommandTester outputTest(String cmd) {
        System.out.printf(".executeCommand(\"%s\")%n", cmd);
        this.givenCommand(cmd);

        List<String> render = this.renderBuffer();
        if (render.size() == 1) {
            System.out.printf(".expect(\"%s\")%n", render.get(0));
        } else {
            System.out.println(".expect(\"\"\"");
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
