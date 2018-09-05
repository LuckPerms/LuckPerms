package me.lucko.luckperms.api;

import javax.annotation.Nonnull;

/**
 * Extension of {@link DataMutateResult} for temporary set operations.
 *
 * @since 4.3
 */
public interface TemporaryDataMutateResult {

    /**
     * Gets the underlying result.
     *
     * @return the result
     */
    @Nonnull
    DataMutateResult getResult();

    /**
     * Gets the node that resulted from any {@link TemporaryMergeBehaviour}
     * processing.
     *
     * <p>If no processing took place, the same instance will be returned by
     * this method.</p>
     *
     * @return the resultant node
     */
    @Nonnull
    Node getMergedNode();

}
