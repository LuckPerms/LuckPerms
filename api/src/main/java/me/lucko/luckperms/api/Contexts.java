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

package me.lucko.luckperms.api;

import com.google.common.collect.ImmutableSet;

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Encapsulates the {@link ContextSet contexts} and {@link LookupSetting settings} for
 * a permission or meta lookup.
 *
 * <p>This class is immutable.</p>
 *
 * @since 2.11
 */
@Immutable
public class Contexts {

    /**
     * The context key used to denote the subjects server
     */
    public static final String SERVER_KEY = "server";

    /**
     * The context key used to denote the subjects world
     */
    public static final String WORLD_KEY = "world";

    /**
     * The default {@link LookupSetting}s.
     */
    private static final EnumSet<LookupSetting> DEFAULT_SETTINGS = EnumSet.of(
            LookupSetting.INCLUDE_NODES_SET_WITHOUT_SERVER,
            LookupSetting.INCLUDE_NODES_SET_WITHOUT_WORLD,
            LookupSetting.RESOLVE_INHERITANCE,
            LookupSetting.APPLY_PARENTS_SET_WITHOUT_SERVER,
            LookupSetting.APPLY_PARENTS_SET_WITHOUT_WORLD
    );

    /**
     * A 'global' or default contexts instance.
     *
     * <p>Formed of an empty {@link ContextSet} and all inclusion and
     * inheritance {@link LookupSetting}s applied.</p>
     */
    private static final Contexts GLOBAL = new Contexts(ImmutableContextSet.empty(), ImmutableSet.copyOf(DEFAULT_SETTINGS));

    /**
     * Gets the {@link FullySatisfiedContexts} instance.
     *
     * @return a context that will satisfy all contextual requirements.
     */
    @Nonnull
    public static Contexts allowAll() {
        return FullySatisfiedContexts.getInstance();
    }

    /**
     * Returns a 'global' or default contexts instance.
     *
     * <p>Formed of an empty {@link ContextSet} and all inclusion and
     * inheritance {@link LookupSetting}s applied.</p>
     *
     * @return the global contexts
     * @since 3.3
     */
    @Nonnull
    public static Contexts global() {
        return GLOBAL;
    }

    /**
     * Creates a new {@link Contexts} instance.
     *
     * @param contextSet the context set
     * @param includeNodesSetWithoutServer the value of {@link LookupSetting#INCLUDE_NODES_SET_WITHOUT_SERVER}
     * @param includeNodesSetWithoutWorld the value of {@link LookupSetting#INCLUDE_NODES_SET_WITHOUT_WORLD}
     * @param resolveInheritance the value of {@link LookupSetting#RESOLVE_INHERITANCE}
     * @param applyParentsWithoutServer the value of {@link LookupSetting#APPLY_PARENTS_SET_WITHOUT_SERVER}
     * @param applyParentsWithoutWorld the value of {@link LookupSetting#APPLY_PARENTS_SET_WITHOUT_WORLD}
     * @param isOp the value of {@link LookupSetting#IS_OP}
     * @return a new instance
     */
    @Nonnull
    public static Contexts of(@Nonnull ContextSet contextSet, boolean includeNodesSetWithoutServer, boolean includeNodesSetWithoutWorld, boolean resolveInheritance, boolean applyParentsWithoutServer, boolean applyParentsWithoutWorld, boolean isOp) {
        Objects.requireNonNull(contextSet, "contextSet");
        EnumSet<LookupSetting> settings = formSettings(
                includeNodesSetWithoutServer,
                includeNodesSetWithoutWorld,
                resolveInheritance,
                applyParentsWithoutServer,
                applyParentsWithoutWorld,
                isOp
        );
        if (contextSet.isEmpty() && DEFAULT_SETTINGS.equals(settings)) {
            return GLOBAL;
        }
        return new Contexts(contextSet.makeImmutable(), ImmutableSet.copyOf(settings));
    }

    private static EnumSet<LookupSetting> asEnumSet(Set<LookupSetting> settings) {
        if (settings instanceof EnumSet<?>) {
            return ((EnumSet<LookupSetting>) settings);
        } else {
            return EnumSet.copyOf(settings);
        }
    }

    /**
     * Creates a new {@link Contexts} instance.
     *
     * @param contextSet the context set
     * @param settings the settings
     * @return a new instance
     */
    public static Contexts of(@Nonnull ContextSet contextSet, @Nonnull Set<LookupSetting> settings) {
        Objects.requireNonNull(contextSet, "contextSet");
        Objects.requireNonNull(settings, "settings");

        EnumSet<LookupSetting> settingsCopy = asEnumSet(settings);
        if (contextSet.isEmpty() && DEFAULT_SETTINGS.equals(settingsCopy)) {
            return GLOBAL;
        }

        return new Contexts(contextSet.makeImmutable(), ImmutableSet.copyOf(settingsCopy));
    }

    /**
     * The contexts that apply for this lookup
     */
    private final ImmutableContextSet contextSet;

    /**
     * The settings for this lookup
     */
    private final ImmutableSet<LookupSetting> settings;

    // cache hashcode - this class is immutable, and is used as an index in the permission cache.
    private final int hashCode;

    /**
     * Creates a new {@link Contexts} instance.
     *
     * @param contextSet the context set
     * @param includeNodesSetWithoutServer the value of {@link LookupSetting#INCLUDE_NODES_SET_WITHOUT_SERVER}
     * @param includeNodesSetWithoutWorld the value of {@link LookupSetting#INCLUDE_NODES_SET_WITHOUT_WORLD}
     * @param resolveInheritance the value of {@link LookupSetting#RESOLVE_INHERITANCE}
     * @param applyParentsWithoutServer the value of {@link LookupSetting#APPLY_PARENTS_SET_WITHOUT_SERVER}
     * @param applyParentsWithoutWorld the value of {@link LookupSetting#APPLY_PARENTS_SET_WITHOUT_WORLD}
     * @param isOp the value of {@link LookupSetting#IS_OP}
     * @deprecated in favour of {@link #of(ContextSet, boolean, boolean, boolean, boolean, boolean, boolean)}
     */
    @Deprecated
    public Contexts(@Nonnull ContextSet contextSet, boolean includeNodesSetWithoutServer, boolean includeNodesSetWithoutWorld, boolean resolveInheritance, boolean applyParentsWithoutServer, boolean applyParentsWithoutWorld, boolean isOp) {
        this.contextSet = Objects.requireNonNull(contextSet, "contextSet").makeImmutable();
        this.settings = ImmutableSet.copyOf(formSettings(
                includeNodesSetWithoutServer,
                includeNodesSetWithoutWorld,
                resolveInheritance,
                applyParentsWithoutServer,
                applyParentsWithoutWorld,
                isOp
        ));
        this.hashCode = calculateHashCode();
    }

    protected Contexts(@Nonnull ImmutableContextSet contextSet, @Nonnull ImmutableSet<LookupSetting> settings) {
        this.contextSet = contextSet;
        this.settings = settings;
        this.hashCode = calculateHashCode();
    }

    /**
     * Gets the {@link ContextSet} which represent these {@link Contexts}.
     *
     * @return an immutable context from this instance
     * @since 2.13
     */
    @Nonnull
    public ContextSet getContexts() {
        return this.contextSet;
    }

    /**
     * Gets the set of {@link LookupSetting}s which represent these {@link Contexts}.
     *
     * @return the settings
     * @since 4.2
     */
    @Nonnull
    public Set<LookupSetting> getSettings() {
        return this.settings;
    }

    /**
     * Gets if the given {@link LookupSetting} is set.
     *
     * @param setting the setting
     * @return the value
     * @since 4.2
     */
    public boolean hasSetting(@Nonnull LookupSetting setting) {
        return this.settings.contains(setting);
    }

    /**
     * Gets the value of {@link LookupSetting#IS_OP}.
     *
     * @return the value
     * @see LookupSetting#IS_OP
     * @deprecated in favour of {@link #hasSetting(LookupSetting)}
     */
    @Deprecated
    public boolean isOp() {
        return hasSetting(LookupSetting.IS_OP);
    }

    /**
     * Gets the value of {@link LookupSetting#INCLUDE_NODES_SET_WITHOUT_SERVER}.
     *
     * @return the value
     * @see LookupSetting#INCLUDE_NODES_SET_WITHOUT_SERVER
     * @deprecated in favour of {@link #hasSetting(LookupSetting)}
     */
    @Deprecated
    public boolean isIncludeGlobal() {
        return hasSetting(LookupSetting.INCLUDE_NODES_SET_WITHOUT_SERVER);
    }

    /**
     * Gets the value of {@link LookupSetting#INCLUDE_NODES_SET_WITHOUT_WORLD}.
     *
     * @return the value
     * @see LookupSetting#INCLUDE_NODES_SET_WITHOUT_WORLD
     * @deprecated in favour of {@link #hasSetting(LookupSetting)}
     */
    @Deprecated
    public boolean isIncludeGlobalWorld() {
        return hasSetting(LookupSetting.INCLUDE_NODES_SET_WITHOUT_WORLD);
    }

    /**
     * Gets the value of {@link LookupSetting#RESOLVE_INHERITANCE}.
     *
     * @return the value
     * @see LookupSetting#RESOLVE_INHERITANCE
     * @deprecated in favour of {@link #hasSetting(LookupSetting)}
     */
    @Deprecated
    public boolean isApplyGroups() {
        return hasSetting(LookupSetting.RESOLVE_INHERITANCE);
    }

    /**
     * Gets the value of {@link LookupSetting#APPLY_PARENTS_SET_WITHOUT_SERVER}.
     *
     * @return the value
     * @see LookupSetting#APPLY_PARENTS_SET_WITHOUT_SERVER
     * @deprecated in favour of {@link #hasSetting(LookupSetting)}
     */
    @Deprecated
    public boolean isApplyGlobalGroups() {
        return hasSetting(LookupSetting.APPLY_PARENTS_SET_WITHOUT_SERVER);
    }

    /**
     * Gets the value of {@link LookupSetting#APPLY_PARENTS_SET_WITHOUT_WORLD}.
     *
     * @return the value
     * @see LookupSetting#APPLY_PARENTS_SET_WITHOUT_WORLD
     * @deprecated in favour of {@link #hasSetting(LookupSetting)}
     */
    @Deprecated
    public boolean isApplyGlobalWorldGroups() {
        return hasSetting(LookupSetting.APPLY_PARENTS_SET_WITHOUT_WORLD);
    }

    @Nonnull
    @Override
    public String toString() {
        return "Contexts(contextSet=" + this.contextSet + ", settings=" + this.settings + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == allowAll()) return false;
        if (!(o instanceof Contexts)) return false;
        final Contexts that = (Contexts) o;
        return this.contextSet.equals(that.contextSet) && this.settings.equals(that.settings);
    }

    private int calculateHashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.contextSet.hashCode();
        result = result * PRIME + this.settings.hashCode();
        return result;
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    private static EnumSet<LookupSetting> formSettings(boolean includeNodesSetWithoutServer, boolean includeNodesSetWithoutWorld, boolean resolveInheritance, boolean applyParentsWithoutServer, boolean applyParentsWithoutWorld, boolean isOp) {
        EnumSet<LookupSetting> settings = EnumSet.noneOf(LookupSetting.class);
        if (includeNodesSetWithoutServer) {
            settings.add(LookupSetting.INCLUDE_NODES_SET_WITHOUT_SERVER);
        }
        if (includeNodesSetWithoutWorld) {
            settings.add(LookupSetting.INCLUDE_NODES_SET_WITHOUT_WORLD);
        }
        if (resolveInheritance) {
            settings.add(LookupSetting.RESOLVE_INHERITANCE);
        }
        if (applyParentsWithoutServer) {
            settings.add(LookupSetting.APPLY_PARENTS_SET_WITHOUT_SERVER);
        }
        if (applyParentsWithoutWorld) {
            settings.add(LookupSetting.APPLY_PARENTS_SET_WITHOUT_WORLD);
        }
        if (isOp) {
            settings.add(LookupSetting.IS_OP);
        }
        return settings;
    }

}
