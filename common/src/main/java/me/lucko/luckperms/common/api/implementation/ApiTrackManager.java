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

package me.lucko.luckperms.common.api.implementation;

import me.lucko.luckperms.common.api.ApiUtils;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.manager.track.TrackManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.util.ImmutableCollectors;
import net.luckperms.api.event.cause.CreationCause;
import net.luckperms.api.event.cause.DeletionCause;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ApiTrackManager extends ApiAbstractManager<Track, net.luckperms.api.track.Track, TrackManager<?>> implements net.luckperms.api.track.TrackManager {
    public ApiTrackManager(LuckPermsPlugin plugin, TrackManager<?> handle) {
        super(plugin, handle);
    }

    @Override
    protected net.luckperms.api.track.Track proxy(Track internal) {
        return internal == null ? null : internal.getApiProxy();
    }

    @Override
    public @NonNull CompletableFuture<net.luckperms.api.track.Track> createAndLoadTrack(@NonNull String name) {
        name = ApiUtils.checkName(Objects.requireNonNull(name, "name"));
        return this.plugin.getStorage().createAndLoadTrack(name, CreationCause.API)
                .thenApply(this::proxy);
    }

    @Override
    public @NonNull CompletableFuture<Optional<net.luckperms.api.track.Track>> loadTrack(@NonNull String name) {
        name = ApiUtils.checkName(Objects.requireNonNull(name, "name"));
        return this.plugin.getStorage().loadTrack(name).thenApply(opt -> opt.map(this::proxy));
    }

    @Override
    public @NonNull CompletableFuture<Void> saveTrack(net.luckperms.api.track.@NonNull Track track) {
        Objects.requireNonNull(track, "track");
        return this.plugin.getStorage().saveTrack(ApiTrack.cast(track));
    }

    @Override
    public @NonNull CompletableFuture<Void> deleteTrack(net.luckperms.api.track.@NonNull Track track) {
        Objects.requireNonNull(track, "track");
        return this.plugin.getStorage().deleteTrack(ApiTrack.cast(track), DeletionCause.API);
    }

    @Override
    public @NonNull CompletableFuture<Void> modifyTrack(@NonNull String name, @NonNull Consumer<? super net.luckperms.api.track.Track> action) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(action, "action");

        return this.plugin.getStorage().createAndLoadTrack(name, CreationCause.API)
                .thenApplyAsync(track -> {
                    action.accept(track.getApiProxy());
                    return track;
                }, this.plugin.getBootstrap().getScheduler().async())
                .thenCompose(track -> this.plugin.getStorage().saveTrack(track));
    }

    @Override
    public @NonNull CompletableFuture<Void> loadAllTracks() {
        return this.plugin.getStorage().loadAllTracks();
    }

    @Override
    public net.luckperms.api.track.Track getTrack(@NonNull String name) {
        Objects.requireNonNull(name, "name");
        return proxy(this.handle.getIfLoaded(name));
    }

    @Override
    public @NonNull Set<net.luckperms.api.track.Track> getLoadedTracks() {
        return this.handle.getAll().values().stream()
                .map(this::proxy)
                .collect(ImmutableCollectors.toSet());
    }

    @Override
    public boolean isLoaded(@NonNull String name) {
        Objects.requireNonNull(name, "name");
        return this.handle.isLoaded(name);
    }
}
