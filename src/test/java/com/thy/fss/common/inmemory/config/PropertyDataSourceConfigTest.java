package com.thy.fss.common.inmemory.config;

import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive test class for PropertyDataSourceConfig and its builder pattern.
 * Tests configuration validation, aggregation settings, and error handling.
 */
class PropertyDataSourceConfigTest {
    private static final String TEST_PROPERTY = "testProperty";
    private static final String TEST_SOURCE = "test-source";
    private static final String ID = "id";
    private static final String USER_ID = "userId";
    private static final String AMOUNT = "amount";
    private static final String PROPERTY_NAME_CANNOT_BE_NULL_OR_EMPTY = "Property name cannot be null or empty";
    private static final String AGGREGATION_FIELD_CANNOT_BE_NULL_OR_EMPTY = "Aggregation field cannot be null or empty for numeric aggregation types";
    

    private InMemoryDataSource<String> dataSource;
    private Function<String, Object> mapper;
    private Predicate<String> aggregationFilter;
    private Function<List<String>, Object> customAggregationFunction;

    @BeforeEach
    void setUp() {
        dataSource = new InMemoryDataSource<>(TEST_SOURCE, String.class);
        mapper = String::toUpperCase;
        aggregationFilter = s -> s.length() > 3;
        customAggregationFunction = list -> String.join(",", list);
    }

    @Test
    @DisplayName("Should create PropertyDataSourceConfig with required fields")
    void shouldCreatePropertyDataSourceConfigWithRequiredFields() {
        // When
        PropertyDataSourceConfig<String> config = PropertyDataSourceConfig.<String>builder()
                .propertyName(TEST_PROPERTY)
                .dataSource(dataSource)
                .dataSourceName(TEST_SOURCE)
                .primaryKeyField(ID)
                .foreignKeyField(USER_ID)
                .mapper(mapper)
                .build();

        // Then
        assertThat(config).isNotNull();
        assertThat(config.getPropertyName()).isEqualTo(TEST_PROPERTY);
        assertThat(config.getDataSource()).isEqualTo(dataSource);
        assertThat(config.getDataSourceName()).isEqualTo(TEST_SOURCE);
        assertThat(config.getPrimaryKeyField()).isEqualTo(ID);
        assertThat(config.getForeignKeyField()).isEqualTo(USER_ID);
        assertThat(config.getMapper()).isEqualTo(mapper);
        assertThat(config.isCollection()).isFalse(); // default
        assertThat(config.isAggregation()).isFalse(); // default
    }

    @Test
    @DisplayName("Should create PropertyDataSourceConfig with all optional fields")
    void shouldCreatePropertyDataSourceConfigWithAllOptionalFields() {
        // When
        PropertyDataSourceConfig<String> config = PropertyDataSourceConfig.<String>builder()
                .propertyName(TEST_PROPERTY)
                .dataSource(dataSource)
                .dataSourceName(TEST_SOURCE)
                .primaryKeyField(ID)
                .foreignKeyField(USER_ID)
                .mapper(mapper)
                .isCollection(true)
                .isAggregation(true)
                .aggregationType(AggregationType.COUNT)
                .aggregationField(AMOUNT)
                .aggregationFilter(aggregationFilter)
                .customAggregationFunction(customAggregationFunction)
                .build();

        // Then
        assertThat(config.isCollection()).isTrue();
        assertThat(config.isAggregation()).isTrue();
        assertThat(config.getAggregationType()).isEqualTo(AggregationType.COUNT);
        assertThat(config.getAggregationField()).isEqualTo(AMOUNT);
        assertThat(config.getAggregationFilter()).isEqualTo(aggregationFilter);
        assertThat(config.getCustomAggregationFunction()).isEqualTo(customAggregationFunction);
    }

    @Test
    @DisplayName("Should validate required fields are not null or empty")
    void shouldValidateRequiredFieldsAreNotNullOrEmpty() {
        // When & Then - null property name
        PropertyDataSourceConfig.Builder<String> nullPropertyNameBuilder = PropertyDataSourceConfig.<String>builder()
                .propertyName(null)
                .dataSource(dataSource)
                .dataSourceName(TEST_SOURCE)
                .primaryKeyField(ID)
                .foreignKeyField(USER_ID)
                .mapper(mapper);

        assertThatThrownBy(nullPropertyNameBuilder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(PROPERTY_NAME_CANNOT_BE_NULL_OR_EMPTY);

        // When & Then - empty property name
        PropertyDataSourceConfig.Builder<String> emptyPropertyNameBuilder = PropertyDataSourceConfig.<String>builder()
                .propertyName("   ")
                .dataSource(dataSource)
                .dataSourceName(TEST_SOURCE)
                .primaryKeyField(ID)
                .foreignKeyField(USER_ID)
                .mapper(mapper);

        assertThatThrownBy(emptyPropertyNameBuilder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(PROPERTY_NAME_CANNOT_BE_NULL_OR_EMPTY);

        // When & Then - null data source
        PropertyDataSourceConfig.Builder<String> nullDataSourceBuilder = PropertyDataSourceConfig.<String>builder()
                .propertyName(TEST_PROPERTY)
                .dataSource(null)
                .dataSourceName(TEST_SOURCE)
                .primaryKeyField(ID)
                .foreignKeyField(USER_ID)
                .mapper(mapper);

        assertThatThrownBy(nullDataSourceBuilder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("DataSource cannot be null");

        // When & Then - null data source name
        PropertyDataSourceConfig.Builder<String> nullDataSourceNameBuilder = PropertyDataSourceConfig.<String>builder()
                .propertyName(TEST_PROPERTY)
                .dataSource(dataSource)
                .dataSourceName(null)
                .primaryKeyField(ID)
                .foreignKeyField(USER_ID)
                .mapper(mapper);

        assertThatThrownBy(nullDataSourceNameBuilder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("DataSource name cannot be null or empty");

        // When & Then - null primary key field
        PropertyDataSourceConfig.Builder<String> nullPrimaryKeyFieldBuilder = PropertyDataSourceConfig.<String>builder()
                .propertyName(TEST_PROPERTY)
                .dataSource(dataSource)
                .dataSourceName(TEST_SOURCE)
                .primaryKeyField(null)
                .foreignKeyField(USER_ID)
                .mapper(mapper);

        assertThatThrownBy(nullPrimaryKeyFieldBuilder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Primary key field cannot be null or empty");

        // When & Then - null foreign key field
        PropertyDataSourceConfig.Builder<String> nullForeignKeyFieldBuilder = PropertyDataSourceConfig.<String>builder()
                .propertyName(TEST_PROPERTY)
                .dataSource(dataSource)
                .dataSourceName(TEST_SOURCE)
                .primaryKeyField(ID)
                .foreignKeyField(null)
                .mapper(mapper);

        assertThatThrownBy(nullForeignKeyFieldBuilder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Foreign key field cannot be null or empty");

        // When & Then - null mapper
        PropertyDataSourceConfig.Builder<String> nullMapperBuilder = PropertyDataSourceConfig.<String>builder()
                .propertyName(TEST_PROPERTY)
                .dataSource(dataSource)
                .dataSourceName(TEST_SOURCE)
                .primaryKeyField(ID)
                .foreignKeyField(USER_ID)
                .mapper(null);

        assertThatThrownBy(nullMapperBuilder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mapper function cannot be null");
    }

    @Test
    @DisplayName("Should validate aggregation configuration")
    void shouldValidateAggregationConfiguration() {
        // When & Then - aggregation enabled but no aggregation type
        PropertyDataSourceConfig.Builder<String> noAggregationTypeBuilder = PropertyDataSourceConfig.<String>builder()
                .propertyName(TEST_PROPERTY)
                .dataSource(dataSource)
                .dataSourceName(TEST_SOURCE)
                .primaryKeyField(ID)
                .foreignKeyField(USER_ID)
                .mapper(mapper)
                .isAggregation(true);

        assertThatThrownBy(noAggregationTypeBuilder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Aggregation type cannot be null when isAggregation is true");

        // When & Then - custom aggregation type but no custom function
        PropertyDataSourceConfig.Builder<String> customWithoutFunctionBuilder = PropertyDataSourceConfig.<String>builder()
                .propertyName(TEST_PROPERTY)
                .dataSource(dataSource)
                .dataSourceName(TEST_SOURCE)
                .primaryKeyField(ID)
                .foreignKeyField(USER_ID)
                .mapper(mapper)
                .isAggregation(true)
                .aggregationType(AggregationType.CUSTOM);

        assertThatThrownBy(customWithoutFunctionBuilder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Custom aggregation function cannot be null when aggregation type is CUSTOM");

        // When & Then - numeric aggregation type but no aggregation field
        PropertyDataSourceConfig.Builder<String> numericWithoutFieldBuilder = PropertyDataSourceConfig.<String>builder()
                .propertyName(TEST_PROPERTY)
                .dataSource(dataSource)
                .dataSourceName(TEST_SOURCE)
                .primaryKeyField(ID)
                .foreignKeyField(USER_ID)
                .mapper(mapper)
                .isAggregation(true)
                .aggregationType(AggregationType.SUM);

        assertThatThrownBy(numericWithoutFieldBuilder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(AGGREGATION_FIELD_CANNOT_BE_NULL_OR_EMPTY);
    }

    @Test
    @DisplayName("Should validate all numeric aggregation types require aggregation field")
    void shouldValidateAllNumericAggregationTypesRequireAggregationField() {
        AggregationType[] numericTypes = {AggregationType.SUM, AggregationType.AVG, AggregationType.MIN, AggregationType.MAX};

        for (AggregationType type : numericTypes) {
            PropertyDataSourceConfig.Builder<String> builder = PropertyDataSourceConfig.<String>builder()
                    .propertyName(TEST_PROPERTY)
                    .dataSource(dataSource)
                    .dataSourceName(TEST_SOURCE)
                    .primaryKeyField(ID)
                    .foreignKeyField(USER_ID)
                    .mapper(mapper)
                    .isAggregation(true)
                    .aggregationType(type);

            assertThatThrownBy(builder::build)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(AGGREGATION_FIELD_CANNOT_BE_NULL_OR_EMPTY);
        }
    }

    @Test
    @DisplayName("Should allow COUNT aggregation without aggregation field")
    void shouldAllowCountAggregationWithoutAggregationField() {
        // When & Then - should not throw exception
        assertThatCode(() -> PropertyDataSourceConfig.<String>builder()
                .propertyName(TEST_PROPERTY)
                .dataSource(dataSource)
                .dataSourceName(TEST_SOURCE)
                .primaryKeyField(ID)
                .foreignKeyField(USER_ID)
                .mapper(mapper)
                .isAggregation(true)
                .aggregationType(AggregationType.COUNT)
                .build())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should create valid custom aggregation configuration")
    void shouldCreateValidCustomAggregationConfiguration() {
        // When
        PropertyDataSourceConfig<String> config = PropertyDataSourceConfig.<String>builder()
                .propertyName(TEST_PROPERTY)
                .dataSource(dataSource)
                .dataSourceName(TEST_SOURCE)
                .primaryKeyField(ID)
                .foreignKeyField(USER_ID)
                .mapper(mapper)
                .isAggregation(true)
                .aggregationType(AggregationType.CUSTOM)
                .customAggregationFunction(customAggregationFunction)
                .aggregationFilter(aggregationFilter)
                .build();

        // Then
        assertThat(config.isAggregation()).isTrue();
        assertThat(config.getAggregationType()).isEqualTo(AggregationType.CUSTOM);
        assertThat(config.getCustomAggregationFunction()).isEqualTo(customAggregationFunction);
        assertThat(config.getAggregationFilter()).isEqualTo(aggregationFilter);
    }

    @Test
    @DisplayName("Should implement equals and hashCode correctly")
    void shouldImplementEqualsAndHashCodeCorrectly() {
        // Given
        PropertyDataSourceConfig<String> config1 = PropertyDataSourceConfig.<String>builder()
                .propertyName(TEST_PROPERTY)
                .dataSource(dataSource)
                .dataSourceName(TEST_SOURCE)
                .primaryKeyField(ID)
                .foreignKeyField(USER_ID)
                .mapper(mapper)
                .isCollection(true)
                .build();

        PropertyDataSourceConfig<String> config2 = PropertyDataSourceConfig.<String>builder()
                .propertyName(TEST_PROPERTY)
                .dataSource(dataSource)
                .dataSourceName(TEST_SOURCE)
                .primaryKeyField(ID)
                .foreignKeyField(USER_ID)
                .mapper(mapper)
                .isCollection(true)
                .build();

        PropertyDataSourceConfig<String> config3 = PropertyDataSourceConfig.<String>builder()
                .propertyName("differentProperty")
                .dataSource(dataSource)
                .dataSourceName(TEST_SOURCE)
                .primaryKeyField(ID)
                .foreignKeyField(USER_ID)
                .mapper(mapper)
                .build();

        // Then
        assertThat(config1).isEqualTo(config2)
                .isNotEqualTo(config3)
                .hasSameHashCodeAs(config2)
                .isEqualTo(config1)
                .isNotEqualTo(null)
                .isNotEqualTo("not a config");
    }

    @Test
    @DisplayName("Should provide meaningful toString representation")
    void shouldProvideMeaningfulToStringRepresentation() {
        // Given
        PropertyDataSourceConfig<String> config = PropertyDataSourceConfig.<String>builder()
                .propertyName(TEST_PROPERTY)
                .dataSource(dataSource)
                .dataSourceName(TEST_SOURCE)
                .primaryKeyField(ID)
                .foreignKeyField(USER_ID)
                .mapper(mapper)
                .isCollection(true)
                .isAggregation(true)
                .aggregationType(AggregationType.COUNT)
                .build();

        // When
        String toString = config.toString();

        // Then
        assertThat(toString).contains("PropertyDataSourceConfig")
                .contains(TEST_PROPERTY)
                .contains(TEST_SOURCE)
                .contains("isCollection=true")
                .contains("isAggregation=true")
                .contains("COUNT");
    }

    @Test
    @DisplayName("Should handle builder method chaining")
    void shouldHandleBuilderMethodChaining() {
        // When & Then - should be able to chain all methods
        assertThatCode(() -> {
            PropertyDataSourceConfig.<String>builder()
                    .propertyName(TEST_PROPERTY)
                    .dataSource(dataSource)
                    .dataSourceName(TEST_SOURCE)
                    .primaryKeyField(ID)
                    .foreignKeyField(USER_ID)
                    .mapper(mapper)
                    .isCollection(false)
                    .isAggregation(false)
                    .aggregationType(AggregationType.COUNT)
                    .aggregationField("field")
                    .aggregationFilter(aggregationFilter)
                    .customAggregationFunction(customAggregationFunction)
                    .build();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should validate aggregation field for all numeric types")
    void shouldValidateAggregationFieldForAllNumericTypes() {
        // Given - valid configuration for numeric aggregation
        PropertyDataSourceConfig<String> config = PropertyDataSourceConfig.<String>builder()
                .propertyName(TEST_PROPERTY)
                .dataSource(dataSource)
                .dataSourceName(TEST_SOURCE)
                .primaryKeyField(ID)
                .foreignKeyField(USER_ID)
                .mapper(mapper)
                .isAggregation(true)
                .aggregationType(AggregationType.SUM)
                .aggregationField(AMOUNT)
                .build();

        // Then
        assertThat(config.getAggregationType()).isEqualTo(AggregationType.SUM);
        assertThat(config.getAggregationField()).isEqualTo(AMOUNT);
    }

    @Test
    @DisplayName("Should handle empty string validation for aggregation field")
    void shouldHandleEmptyStringValidationForAggregationField() {
        // When & Then - empty aggregation field for numeric type
        PropertyDataSourceConfig.Builder<String> builder = PropertyDataSourceConfig.<String>builder()
                .propertyName(TEST_PROPERTY)
                .dataSource(dataSource)
                .dataSourceName(TEST_SOURCE)
                .primaryKeyField(ID)
                .foreignKeyField(USER_ID)
                .mapper(mapper)
                .isAggregation(true)
                .aggregationType(AggregationType.AVG)
                .aggregationField("   ");

        assertThatThrownBy(builder::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(AGGREGATION_FIELD_CANNOT_BE_NULL_OR_EMPTY);
    }
}