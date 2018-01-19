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

package me.lucko.luckperms.common.api.delegates.misc;

import me.lucko.luckperms.api.MessagingService;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.common.api.delegates.model.ApiUser;
import me.lucko.luckperms.common.messaging.InternalMessagingService;

import java.util.Objects;

import javax.annotation.Nonnull;

public class ApiMessagingService implements MessagingService {
    private final InternalMessagingService handle;

    public ApiMessagingService(InternalMessagingService handle) {
        this.handle = handle;
    }

    @Override
    public String getName() {
        return this.handle.getName();
    }

    @Override
    public void pushUpdate() {
        this.handle.pushUpdate();
    }

    @Override
    public void pushUserUpdate(@Nonnull User user) {
        Objects.requireNonNull(user, "user");
        this.handle.pushUserUpdate(ApiUser.cast(user));
    }
}
