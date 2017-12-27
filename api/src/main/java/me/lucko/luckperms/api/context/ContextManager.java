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

package me.lucko.luckperms.api.context;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.User;

import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * Manages {@link ContextCalculator}s, and calculates applicable contexts for a
 * given type.
 *
 * This interface accepts {@link Object} types as a parameter to avoid having to depend
 * on specific server implementations. In all cases, the "player" or "subject" type for
 * the platform must be used.
 *
 * Specifically:
 *
 * <p></p>
 * <ul>
 *     <li>{@code org.bukkit.entity.Player}</li>
 *     <li>{@code net.md_5.bungee.api.connection.ProxiedPlayer}</li>
 *     <li>{@code org.spongepowered.api.service.permission.Subject}</li>
 * </ul>
 *
 * @since 4.0
 */
public interface ContextManager {

    /**
     * Queries the ContextManager for current context values for the subject.
     *
     * @param subject the subject
     * @return the applicable context for the subject
     */
    @Nonnull
    ImmutableContextSet getApplicableContext(@Nonnull Object subject);

    /**
     * Queries the ContextManager for current context values for the subject.
     *
     * @param subject the subject
     * @return the applicable context for the subject
     */
    @Nonnull
    Contexts getApplicableContexts(@Nonnull Object subject);

    /**
     * Queries the ContextManager for current context values for the given User.
     *
     * <p>This will only return a value if the player corresponding to the
     * {@link User} is online.</p>
     *
     * <p>If you need to be a {@link Contexts} instance regardless, you should
     * initially try this method, and then fallback on {@link #getStaticContext()}.</p>
     *
     * @param user the user
     * @return the applicable context for the subject
     */
    @Nonnull
    Optional<ImmutableContextSet> lookupApplicableContext(@Nonnull User user);

    /**
     * Queries the ContextManager for current context values for the given User.
     *
     * <p>This will only return a value if the player corresponding to the
     * {@link User} is online.</p>
     *
     * <p>If you need to be a {@link Contexts} instance regardless, you should
     * initially try this method, and then fallback on {@link #getStaticContexts()}.</p>
     *
     * @param user the user
     * @return the applicable context for the subject
     */
    @Nonnull
    Optional<Contexts> lookupApplicableContexts(@Nonnull User user);

    /**
     * Gets the contexts from the static calculators in this manager.
     *
     * <p>Static calculators provide the same context for all subjects, and are
     * marked as such when registered.</p>
     *
     * @return the current active static contexts
     */
    @Nonnull
    ImmutableContextSet getStaticContext();

    /**
     * Gets the contexts from the static calculators in this manager.
     *
     * <p>Static calculators provide the same context for all subjects, and are
     * marked as such when registered.</p>
     *
     * @return the current active static contexts
     */
    @Nonnull
    Contexts getStaticContexts();

    /**
     * Forms a {@link Contexts} instance from an {@link ImmutableContextSet}.
     *
     * <p>This method relies on the plugins configuration to form the
     * {@link Contexts} instance.</p>
     *
     * @param subject the reference subject
     * @param contextSet the context set
     * @return a contexts instance
     */
    @Nonnull
    Contexts formContexts(@Nonnull Object subject, @Nonnull ImmutableContextSet contextSet);

    /**
     * Forms a {@link Contexts} instance from an {@link ImmutableContextSet}.
     *
     * <p>This method relies on the plugins configuration to form the
     * {@link Contexts} instance.</p>
     *
     * @param contextSet the context set
     * @return a contexts instance
     */
    @Nonnull
    Contexts formContexts(@Nonnull ImmutableContextSet contextSet);

    /**
     * Registers a context calculator with the manager.
     *
     * @param calculator the calculator
     */
    @Nonnull
    void registerCalculator(@Nonnull ContextCalculator<?> calculator);

    /**
     * Registers a static context calculator with the manager.
     *
     * <p>Static calculators provide the same context for all subjects.</p>
     *
     * @param calculator the calculator
     */
    @Nonnull
    void registerStaticCalculator(@Nonnull StaticContextCalculator calculator);

    /**
     * Invalidates the lookup cache for a given subject
     *
     * @param subject the subject
     */
    @Nonnull
    void invalidateCache(@Nonnull Object subject);

}
