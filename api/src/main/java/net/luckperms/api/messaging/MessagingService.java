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

package net.luckperms.api.messaging;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.messaging.CustomMessageReceiveEvent;
import net.luckperms.api.model.user.User;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A means to send messages to other servers using the platforms networking
 */
public interface MessagingService {

    /**
     * Gets the name of this messaging service
     *
     * @return the name of this messaging service
     */
    @NonNull String getName();

    /**
     * Uses the messaging service to inform other servers about a general
     * change.
     *
     * <p>The standard response by other servers will be to execute a overall
     * sync of all live data, equivalent to calling
     * {@link LuckPerms#runUpdateTask()}.</p>
     *
     * <p>This will push the update asynchronously, and this method will return
     * immediately. Note that this method will not cause an update to be
     * processed on the local server.</p>
     */
    void pushUpdate();

    /**
     * Uses the messaging service to inform other servers about a change to a
     * specific user.
     *
     * <p>The standard response by other servers is undefined, however the
     * current implementation will reload the corresponding users data if they
     * are online.</p>
     *
     * <p>This will push the update asynchronously, and this method will return
     * immediately. Note that this method will not cause an update to be
     * processed on the local server.</p>
     *
     * @param user the user to push the update for
     */
    void pushUserUpdate(@NonNull User user);

    /**
     * Uses the messaging service to send a message with a custom payload.
     *
     * <p>The intended use case of this functionality is to allow plugins/mods
     * to send <b>lightweight</b> and <b>permissions-related</b> custom messages
     * between instances, piggy-backing on top of the messenger abstraction
     * already built into LuckPerms.</p>
     *
     * <p>It is <b>not</b> intended as a full message broker replacement/abstraction.
     * Note that some of the messenger implementations in LuckPerms cannot handle
     * a high volume of messages being sent (for example the SQL messenger).
     * Additionally, some implementations do not give any guarantees that a message
     * will be delivered on time or even at all (for example the plugin message
     * messengers).</p>
     *
     * <p>With all of that in mind, please consider that if you are using this
     * functionality to send messages that have nothing to do with LuckPerms or
     * permissions, or that require guarantees around delivery reliability, you
     * are most likely misusing the API and would be better off building your own
     * integration with a message broker.</p>
     *
     * <p>Whilst there is (currently) no strict validation, it is recommended
     * that the channel id should use the same format as Minecraft resource locations /
     * namespaced keys. For example, a plugin called "SuperRanks" sending rank-up
     * notifications using custom payload messages might use the channel id
     * {@code "superranks:notifications"} for this purpose.</p>
     *
     * <p>The payload can be any valid UTF-8 string.</p>
     *
     * <p>The message will be delivered asynchronously.</p>
     *
     * <p>Other LuckPerms instances that receive the message will publish it to API
     * consumers using the {@link CustomMessageReceiveEvent}.</p>
     *
     * @param channelId the channel id
     * @param payload the message payload
     * @since 5.5
     */
    void sendCustomMessage(@NonNull String channelId, @NonNull String payload);

}
