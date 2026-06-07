package com.thy.fss.common.inmemory.engine.index;

import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.SpecificationServices;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.*;

/**
 * Immutable configuration for an index. Defines which fields should be used as
 * keys and how they should be compared. Uses MetaAttribute for type-safe field
 * references without reflection.
 *
 * @param <T> The entity type
 */
public final class IndexDefinition<T> {

    private final Class<T> entityClass;
    private final List<MetaAttribute<T, ?>> keyFields;
    private final List<Comparator<Object>> comparators;
    private final List<KeyExtractor<T, ? extends Comparable<?>>> keyExtractors;

    private IndexDefinition(Builder<T> builder) {
        this.entityClass = builder.entityClass;
        this.keyFields = Collections.unmodifiableList(new ArrayList<>(builder.keyFields));
        this.comparators = Collections.unmodifiableList(new ArrayList<>(builder.comparators));
        this.keyExtractors = Collections.unmodifiableList(new ArrayList<>(builder.keyExtractors));
    }

    /**
     * Creates a new builder for the given entity class.
     *
     * @param <T> The entity type
     * @param entityClass The entity class
     * @return A new builder instance
     */
    public static <T> Builder<T> builder(Class<T> entityClass) {
        return new Builder<>(entityClass);
    }

    /**
     * Gets the entity class.
     *
     * @return The entity class
     */
    public Class<T> getEntityClass() {
        return entityClass;
    }

    /**
     * Gets the key fields.
     *
     * @return An unmodifiable list of key fields
     */
    public List<MetaAttribute<T, ?>> getKeyFields() {
        return keyFields;
    }

    /**
     * Gets the depth (number of key levels).
     *
     * @return The number of key fields
     */
    public int getDepth() {
        return keyFields.size();
    }

    /**
     * Gets the comparator for a specific level.
     *
     * @param level The level (0-based index)
     * @return The comparator for that level
     */
    public Comparator<Object> getComparatorForLevel(int level) {
        if (level < 0 || level >= comparators.size()) {
            throw new IndexOutOfBoundsException("Invalid level: " + level);
        }
        return comparators.get(level);
    }

    /**
     * Extracts the key value at a specific level from an entity.
     *
     * @param entity The entity to extract from
     * @param level The level (0-based index)
     * @return The extracted key value (raw Comparable)
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Comparable extractKeyValue(T entity, int level) {
        if (level < 0 || level >= keyExtractors.size()) {
            throw new IndexOutOfBoundsException("Invalid level: " + level);
        }
        KeyExtractor<T, ? extends Comparable<?>> extractor = keyExtractors.get(level);
        return extractor.extract(entity);
    }

    /**
     * Gets the key field names.
     *
     * @return A list of key field names
     */
    public List<String> getKeyFieldNames() {
        List<String> names = new ArrayList<>(keyFields.size());
        for (MetaAttribute<T, ?> field : keyFields) {
            names.add(field.getName());
        }
        return names;
    }

    /**
     * Builder for creating IndexDefinition instances.
     *
     * @param <T> The entity type
     */
    public static final class Builder<T> {

        private final Class<T> entityClass;
        private final List<MetaAttribute<T, ?>> keyFields;
        private final List<Comparator<Object>> comparators;
        private final List<KeyExtractor<T, ? extends Comparable<?>>> keyExtractors;

        private Builder(Class<T> entityClass) {
            this.entityClass = Objects.requireNonNull(entityClass, "entityClass cannot be null");
            this.keyFields = new ArrayList<>();
            this.comparators = new ArrayList<>();
            this.keyExtractors = new ArrayList<>();
        }

        /**
         * Adds a key field with natural ordering. The field type must implement
         * Comparable.
         *
         * @param <V> The field type (must be Comparable)
         * @param keyField The meta attribute for the field
         * @return This builder for method chaining
         */
        @SuppressWarnings("unchecked")
        public <V extends Comparable<V>> Builder<T> addKeyField(MetaAttribute<T, V> keyField) {
            Objects.requireNonNull(keyField, "keyField cannot be null");

            keyFields.add(keyField);

            // Natural ordering comparator with null handling
            Comparator<Object> comparator = (o1, o2) -> {
                if (o1 == null && o2 == null) {
                    return 0;
                }
                if (o1 == null) {
                    return -1;
                }
                if (o2 == null) {
                    return 1;
                }
                return ((Comparable<Object>) o1).compareTo(o2);
            };
            comparators.add(comparator);

            // Create key extractor using SpecificationService
            KeyExtractor<T, V> extractor = entity -> {
                SpecificationService<T> service = SpecificationServices.getService(entityClass);
                Object value = service.getFieldValue(entity, keyField);
                return (V) value;
            };
            keyExtractors.add(extractor);

            return this;
        }

        /**
         * Adds a key field with a custom path and extractor for nested property
         * access. This method is used when the key field is accessed through a
         * nested path.
         *
         * @param <V> The field type
         * @param keyPath The path of meta attributes to navigate
         * @param extractor The custom key extractor function
         * @return This builder for method chaining
         */
        @SuppressWarnings("unchecked")
        public <V extends Comparable<V>> Builder<T> addKeyFieldWithPath(
                java.util.List<MetaAttribute<?, ?>> keyPath,
                java.util.function.Function<T, Object> extractor) {

            Objects.requireNonNull(keyPath, "keyPath cannot be null");
            Objects.requireNonNull(extractor, "extractor cannot be null");

            if (keyPath.isEmpty()) {
                throw new IllegalArgumentException("keyPath cannot be empty");
            }

            // Store the last attribute as the key field for reference (with unchecked cast)
            keyFields.add((MetaAttribute<T, ?>) keyPath.get(keyPath.size() - 1));

            // Natural ordering comparator with null handling
            Comparator<Object> comparator = (o1, o2) -> {
                if (o1 == null && o2 == null) {
                    return 0;
                }
                if (o1 == null) {
                    return -1;
                }
                if (o2 == null) {
                    return 1;
                }
                return ((Comparable<Object>) o1).compareTo(o2);
            };
            comparators.add(comparator);

            // Use the provided extractor
            KeyExtractor<T, V> keyExtractor = entity -> (V) extractor.apply(entity);
            keyExtractors.add(keyExtractor);

            return this;
        }

        /**
         * Adds a key field with a custom comparator.
         *
         * @param <V> The field type
         * @param keyField The meta attribute for the field
         * @param comparator The custom comparator for this field
         * @return This builder for method chaining
         */
        @SuppressWarnings("unchecked")
        public <V> Builder<T> addKeyField(MetaAttribute<T, V> keyField, Comparator<V> comparator) {
            Objects.requireNonNull(keyField, "keyField cannot be null");
            Objects.requireNonNull(comparator, "comparator cannot be null");

            keyFields.add(keyField);

            // Wrap comparator with null handling
            Comparator<Object> nullSafeComparator = (o1, o2) -> {
                if (o1 == null && o2 == null) {
                    return 0;
                }
                if (o1 == null) {
                    return -1;
                }
                if (o2 == null) {
                    return 1;
                }
                return comparator.compare((V) o1, (V) o2);
            };
            comparators.add(nullSafeComparator);

            // Create key extractor using SpecificationService
            KeyExtractor<T, ComparableWrapper<V>> extractor = entity -> {
                SpecificationService<T> service = SpecificationServices.getService(entityClass);
                Object value = service.getFieldValue(entity, keyField);
                // Wrap value in a Comparable that uses the custom comparator
                V typedValue = (V) value;
                return new ComparableWrapper<>(typedValue, comparator);
            };
            keyExtractors.add(extractor);

            return this;
        }

        /**
         * Builds the immutable IndexDefinition.
         *
         * @return The IndexDefinition instance
         * @throws IllegalStateException if no key fields have been added
         */
        public IndexDefinition<T> build() {
            if (keyFields.isEmpty()) {
                throw new IllegalStateException("At least one key field must be specified");
            }
            return new IndexDefinition<>(this);
        }
    }

    /**
     * Wrapper class to make any value Comparable using a custom Comparator.
     */
    private static class ComparableWrapper<V> implements Comparable<ComparableWrapper<V>> {

        private final V value;
        private final Comparator<V> comparator;

        ComparableWrapper(V value, Comparator<V> comparator) {
            this.value = value;
            this.comparator = comparator;
        }

        @Override
        public int compareTo(ComparableWrapper<V> other) {
            if (value == null && other.value == null) {
                return 0;
            }
            if (value == null) {
                return -1;
            }
            if (other.value == null) {
                return 1;
            }
            return comparator.compare(value, other.value);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ComparableWrapper<?> other = (ComparableWrapper<?>) obj;
            return Objects.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }
    }
}
