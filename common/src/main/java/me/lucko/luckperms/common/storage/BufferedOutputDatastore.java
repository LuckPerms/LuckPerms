/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.common.storage;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import me.lucko.luckperms.common.groups.Group;
import me.lucko.luckperms.common.tracks.Track;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.common.users.UserIdentifier;
import me.lucko.luckperms.common.utils.Buffer;
import me.lucko.luckperms.common.utils.LPFuture;

import java.util.UUID;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class BufferedOutputDatastore implements Datastore, Runnable {
    public static BufferedOutputDatastore wrap(Datastore datastore, long flushTime) {
        return new BufferedOutputDatastore(datastore, flushTime);
    }

    @Getter
    @Delegate(excludes = Exclude.class)
    private final Datastore backing;

    private final long flushTime;

    private final Buffer<User, Boolean> userOutputBuffer = new Buffer<User, Boolean>() {
        @Override
        public Boolean dequeue(User user) {
            return backing.saveUser(user).getOrDefault(false);
        }
    };

    private final Buffer<Group, Boolean> groupOutputBuffer = new Buffer<Group, Boolean>() {
        @Override
        public Boolean dequeue(Group group) {
            return backing.saveGroup(group).getOrDefault(false);
        }
    };

    private final Buffer<Track, Boolean> trackOutputBuffer = new Buffer<Track, Boolean>() {
        @Override
        public Boolean dequeue(Track track) {
            return backing.saveTrack(track).getOrDefault(false);
        }
    };

    private final Buffer<UserIdentifier, Boolean> uuidDataOutputBuffer = new Buffer<UserIdentifier, Boolean>() {
        @Override
        protected Boolean dequeue(UserIdentifier userIdentifier) {
            return backing.saveUUIDData(userIdentifier.getUsername(), userIdentifier.getUuid()).getOrDefault(false);
        }
    };

    @Override
    public void run() {
        flush(flushTime);
    }

    public void forceFlush() {
        flush(-1);
    }

    public void flush(long flushTime) {
        userOutputBuffer.flush(flushTime);
        groupOutputBuffer.flush(flushTime);
        trackOutputBuffer.flush(flushTime);
        userOutputBuffer.flush(flushTime);
    }

    public Datastore force() {
        return backing;
    }

    @Override
    public LPFuture<Void> shutdown() {
        forceFlush();
        return backing.shutdown();
    }

    @Override
    public LPFuture<Boolean> saveUser(User user) {
        return userOutputBuffer.enqueue(user);
    }

    @Override
    public LPFuture<Boolean> saveGroup(Group group) {
        return groupOutputBuffer.enqueue(group);
    }

    @Override
    public LPFuture<Boolean> saveTrack(Track track) {
        return trackOutputBuffer.enqueue(track);
    }

    @Override
    public LPFuture<Boolean> saveUUIDData(String username, UUID uuid) {
        return uuidDataOutputBuffer.enqueue(UserIdentifier.of(uuid, username));
    }

    private interface Exclude {
        Datastore force();
        LPFuture<Void> shutdown();
        LPFuture<Boolean> saveUser(User user);
        LPFuture<Boolean> saveGroup(Group group);
        LPFuture<Boolean> saveTrack(Track track);
        LPFuture<Boolean> saveUUIDData(String username, UUID uuid);
    }
}
