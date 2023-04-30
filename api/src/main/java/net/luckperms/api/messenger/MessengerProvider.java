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

package net.luckperms.api.messenger;

import net.luckperms.api.LuckPerms;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;

/**
 * Represents a provider for {@link Messenger} instances.
 *
 * <p>Users wishing to provide their own implementation for the plugins
 * "Messaging Service" should implement and register this interface.</p>
 *
 * @see LuckPerms#registerMessengerProvider(MessengerProvider)
 */
@OverrideOnly
public interface MessengerProvider {

    /**
     * Gets the name of this provider.
     *
     * @return the provider name
     */
    @NonNull String getName();

    /**
     * Creates and returns a new {@link Messenger} instance, which passes
     * incoming messages to the provided {@link IncomingMessageConsumer}.
     *
     * <p>As the agent should pass incoming messages to the given consumer,
     * this method should always return a new object.</p>
     *
     * @param incomingMessageConsumer the consumer the new instance should pass
     *                                incoming messages to
     * @return a new messenger agent instance
     */
    @NonNull Messenger obtain(@NonNull IncomingMessageConsumer incomingMessageConsumer);

}
