package me.lucko.luckperms.api;

import me.lucko.luckperms.api.caching.MetaContexts;
import me.lucko.luckperms.api.caching.UserData;
import me.lucko.luckperms.api.context.ContextSet;

import javax.annotation.Nonnull;

/**
 * A special instance of {@link Contexts}, which when passed to:
 *
 * <p></p>
 * <ul>
 *     <li>{@link UserData#getPermissionData(Contexts)}</li>
 *     <li>{@link UserData#getMetaData(Contexts)}</li>
 *     <li>{@link UserData#getMetaData(MetaContexts)}</li>
 * </ul>
 *
 * <p>... will always satisfy all contextual requirements.</p>
 *
 * <p>This effectively allows you to do lookups which ignore context.</p>
 *
 * @since 3.3
 */
public final class FullySatisfiedContexts extends Contexts {
    private static final FullySatisfiedContexts INSTANCE = new FullySatisfiedContexts();

    @Nonnull
    public static Contexts getInstance() {
        return INSTANCE;
    }

    private FullySatisfiedContexts() {
        super(ContextSet.empty(), true, true, true, true, true, false);
    }

    @Nonnull
    @Override
    public String toString() {
        return "FullySatisfiedContexts";
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
