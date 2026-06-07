package com.thy.fss.common.inmemory.datasource;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.specification.Specification;

/**
 * Defines a time window rule for a datasource.
 * Uses a {@link Supplier} of {@link Specification} so that {@code Instant.now()}
 * is re-evaluated on each validation call, keeping the time window current.
 *
 * @param <T> entity type implementing Identifiable
 */
public class TimeWindowRule<T extends Identifiable<?>> {

    private final String dataSourceName;
    private final Supplier<Specification<T>> specificationFactory;

    /**
     * Creates a new TimeWindowRule.
     *
     * @param dataSourceName       the associated datasource name
     * @param specificationFactory factory that produces a fresh Specification on each call,
     *                             allowing {@code Instant.now()} to be re-evaluated every time
     * @throws NullPointerException if any parameter is null
     */
    public TimeWindowRule(String dataSourceName,
                          Supplier<Specification<T>> specificationFactory) {
        this.dataSourceName = Objects.requireNonNull(dataSourceName, "dataSourceName must not be null");
        this.specificationFactory = Objects.requireNonNull(specificationFactory, "specificationFactory must not be null");
    }

    /**
     * Creates a new TimeWindowRule (deprecated — Duration parameter is ignored).
     *
     * @param dataSourceName       the associated datasource name
     * @param timeWindow           ignored — filtering is handled entirely by the specification
     * @param specificationFactory factory that produces a fresh Specification on each call
     * @throws NullPointerException if dataSourceName or specificationFactory is null
     * @deprecated Use {@link #TimeWindowRule(String, Supplier)} instead. Duration is no longer used.
     */
    @Deprecated
    public TimeWindowRule(String dataSourceName, Duration timeWindow,
                          Supplier<Specification<T>> specificationFactory) {
        this(dataSourceName, specificationFactory);
    }

    /**
     * Validates whether the given entity is within the time window.
     * Each call creates a fresh Specification via {@code specificationFactory.get()}.
     *
     * @param entity the entity to validate
     * @return true if the entity is valid (within the window), false if expired
     */
    public boolean isValid(T entity) {
        return specificationFactory.get().test(entity);
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public Supplier<Specification<T>> getSpecificationFactory() {
        return specificationFactory;
    }
}
