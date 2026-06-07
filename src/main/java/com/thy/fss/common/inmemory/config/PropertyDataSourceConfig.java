package com.thy.fss.common.inmemory.config;

import com.thy.fss.common.inmemory.datasource.DataSource;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Configuration for mapping a property from a DataSource to a target object property.
 * Provides type-safe mapping with generic DataSource type preservation.
 *
 * @param <D> The DataSource data type
 */
public class PropertyDataSourceConfig<D> {


    private final String propertyName;
    private final DataSource<D> dataSource;
    private final String dataSourceName;
    private final String primaryKeyField;
    private final String foreignKeyField;
    private final Function<D, Object> mapper;
    private final boolean isCollection;

    // Aggregation support fields
    private final boolean isAggregation;
    private final AggregationType aggregationType;
    private final String aggregationField;
    private final Predicate<D> aggregationFilter;
    private final Function<List<D>, Object> customAggregationFunction;

    private PropertyDataSourceConfig(Builder<D> builder) {
        this.propertyName = builder.propertyName;
        this.dataSource = builder.dataSource;
        this.dataSourceName = builder.dataSourceName;
        this.primaryKeyField = builder.primaryKeyField;
        this.foreignKeyField = builder.foreignKeyField;
        this.mapper = builder.mapper;
        this.isCollection = builder.isCollection;

        // Aggregation fields
        this.isAggregation = builder.isAggregation;
        this.aggregationType = builder.aggregationType;
        this.aggregationField = builder.aggregationField;
        this.aggregationFilter = builder.aggregationFilter;
        this.customAggregationFunction = builder.customAggregationFunction;
    }

    // Builder pattern
    public static <D> Builder<D> builder() {
        return new Builder<>();
    }

    // Getters
    public String getPropertyName() {
        return propertyName;
    }

    public DataSource<D> getDataSource() {
        return dataSource;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public String getPrimaryKeyField() {
        return primaryKeyField;
    }

    public String getForeignKeyField() {
        return foreignKeyField;
    }

    public Function<D, Object> getMapper() {
        return mapper;
    }

    public boolean isCollection() {
        return isCollection;
    }

    // Aggregation getters
    public boolean isAggregation() {
        return isAggregation;
    }

    public AggregationType getAggregationType() {
        return aggregationType;
    }

    public String getAggregationField() {
        return aggregationField;
    }

    public Predicate<D> getAggregationFilter() {
        return aggregationFilter;
    }

    public Function<List<D>, Object> getCustomAggregationFunction() {
        return customAggregationFunction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PropertyDataSourceConfig<?> that = (PropertyDataSourceConfig<?>) o;
        return isCollection == that.isCollection &&
                isAggregation == that.isAggregation &&
                Objects.equals(propertyName, that.propertyName) &&
                Objects.equals(dataSource, that.dataSource) &&
                Objects.equals(dataSourceName, that.dataSourceName) &&
                Objects.equals(primaryKeyField, that.primaryKeyField) &&
                Objects.equals(foreignKeyField, that.foreignKeyField) &&
                Objects.equals(aggregationType, that.aggregationType) &&
                Objects.equals(aggregationField, that.aggregationField);
    }

    @Override
    public int hashCode() {
        return Objects.hash(propertyName, dataSource, dataSourceName, primaryKeyField,
                foreignKeyField, isCollection, isAggregation,
                aggregationType, aggregationField);
    }

    @Override
    public String toString() {
        return "PropertyDataSourceConfig{" +
                "propertyName='" + propertyName + '\'' +
                ", dataSourceName='" + dataSourceName + '\'' +
                ", primaryKeyField='" + primaryKeyField + '\'' +
                ", foreignKeyField='" + foreignKeyField + '\'' +
                ", isCollection=" + isCollection +
                ", isAggregation=" + isAggregation +
                ", aggregationType=" + aggregationType +
                ", aggregationField='" + aggregationField + '\'' +
                '}';
    }

    public static class Builder<D> {
        private String propertyName;
        private DataSource<D> dataSource;
        private String dataSourceName;
        private String primaryKeyField;
        private String foreignKeyField;
        private Function<D, Object> mapper;
        private boolean isCollection = false;

        // Aggregation fields
        private boolean isAggregation = false;
        private AggregationType aggregationType;
        private String aggregationField;
        private Predicate<D> aggregationFilter;
        private Function<List<D>, Object> customAggregationFunction;

        public Builder<D> propertyName(String propertyName) {
            this.propertyName = propertyName;
            return this;
        }

        public Builder<D> dataSource(DataSource<D> dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        public Builder<D> dataSourceName(String dataSourceName) {
            this.dataSourceName = dataSourceName;
            return this;
        }

        public Builder<D> primaryKeyField(String primaryKeyField) {
            this.primaryKeyField = primaryKeyField;
            return this;
        }

        public Builder<D> foreignKeyField(String foreignKeyField) {
            this.foreignKeyField = foreignKeyField;
            return this;
        }

        public Builder<D> mapper(Function<D, Object> mapper) {
            this.mapper = mapper;
            return this;
        }

        public Builder<D> isCollection(boolean isCollection) {
            this.isCollection = isCollection;
            return this;
        }

        // Aggregation builder methods
        public Builder<D> isAggregation(boolean isAggregation) {
            this.isAggregation = isAggregation;
            return this;
        }

        public Builder<D> aggregationType(AggregationType aggregationType) {
            this.aggregationType = aggregationType;
            return this;
        }

        public Builder<D> aggregationField(String aggregationField) {
            this.aggregationField = aggregationField;
            return this;
        }

        public Builder<D> aggregationFilter(Predicate<D> aggregationFilter) {
            this.aggregationFilter = aggregationFilter;
            return this;
        }

        public Builder<D> customAggregationFunction(Function<List<D>, Object> customAggregationFunction) {
            this.customAggregationFunction = customAggregationFunction;
            return this;
        }

        public PropertyDataSourceConfig<D> build() {
            validate();
            return new PropertyDataSourceConfig<>(this);
        }

        private void validate() {
            validateRequiredFields();
            if (isAggregation) {
                validateAggregation();
            }
        }

        private void validateRequiredFields() {
            if (propertyName == null || propertyName.trim().isEmpty()) {
                throw new IllegalArgumentException("Property name cannot be null or empty");
            }
            if (dataSource == null) {
                throw new IllegalArgumentException("DataSource cannot be null");
            }
            if (dataSourceName == null || dataSourceName.trim().isEmpty()) {
                throw new IllegalArgumentException("DataSource name cannot be null or empty");
            }
            if (primaryKeyField == null || primaryKeyField.trim().isEmpty()) {
                throw new IllegalArgumentException("Primary key field cannot be null or empty");
            }
            if (foreignKeyField == null || foreignKeyField.trim().isEmpty()) {
                throw new IllegalArgumentException("Foreign key field cannot be null or empty");
            }
            if (mapper == null) {
                throw new IllegalArgumentException("Mapper function cannot be null");
            }
        }

        private void validateAggregation() {
            if (aggregationType == null) {
                throw new IllegalArgumentException("Aggregation type cannot be null when isAggregation is true");
            }

            if (aggregationType == AggregationType.CUSTOM && customAggregationFunction == null) {
                throw new IllegalArgumentException("Custom aggregation function cannot be null when aggregation type is CUSTOM");
            }

            if ((aggregationType == AggregationType.SUM || aggregationType == AggregationType.AVG ||
                    aggregationType == AggregationType.MIN || aggregationType == AggregationType.MAX)
                    && (aggregationField == null || aggregationField.trim().isEmpty())) {
                throw new IllegalArgumentException("Aggregation field cannot be null or empty for numeric aggregation types");
            }
        }
    }
}