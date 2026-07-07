package com.thy.fss.common.inmemory.filter.parameter;

import com.thy.fss.common.inmemory.filter.FilterConstants;
import com.thy.fss.common.inmemory.filter.StringFilter;
import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry;
import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistryImpl;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.validation.DataBinder;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for string query parameter deserialization using Spring DataBinder and PropertyEditor.
 * Verifies that string filter operations are correctly deserialized from query parameters
 * using Spring's property binding mechanism. Tests ensure operator names match FilterConstants values.
 *
 * <p>These tests simulate how Spring MVC would bind query parameters to filter objects,
 * including support for nested field structures like user.name.eq=john.
 */
@DisplayName("String Parameter Deserialization Tests")
class StringParameterDeserializationTest {

    private static final String JOHN = "john";

    private FilterValueDeserializer valueDeserializer;
    private CollectionParameterHandler collectionHandler;
    private DeserializerRegistry registry;
    private FilterPropertyEditor stringEditor;
    private FilterPropertyEditor collectionEditor;

    @BeforeEach
    void setUp() {
        valueDeserializer = new FilterValueDeserializerImpl();
        collectionHandler = new CollectionParameterHandlerImpl(valueDeserializer);
        registry = new DeserializerRegistryImpl();
        stringEditor = new FilterPropertyEditor(valueDeserializer, collectionHandler, registry, String.class);
        collectionEditor = new FilterPropertyEditor(valueDeserializer, collectionHandler, registry, List.class, String.class);
    }

    /**
     * Helper to deserialize a single string value using PropertyEditor.
     */
    private String deserializeSingleValue(String value) {
        stringEditor.setAsText(value);
        return (String) stringEditor.getValue();
    }

    /**
     * Helper to deserialize a collection of string values using PropertyEditor.
     */
    @SuppressWarnings("unchecked")
    private List<String> deserializeCollectionValue(String value) {
        collectionEditor.setAsText(value);
        return (List<String>) collectionEditor.getValue();
    }

    /**
     * Helper to manually set a property on StringFilter using PropertyEditor.
     * This simulates what Spring DataBinder does internally.
     */
    private StringFilter setFilterProperty(String propertyName, String value) {
        StringFilter filter = new StringFilter();
        stringEditor.setAsText(value);
        String convertedValue = (String) stringEditor.getValue();

        // Manually set the property based on the property name
        switch (propertyName) {
            case "eq" -> filter.setEquals(convertedValue);
            case "neq" -> filter.setNotEquals(convertedValue);
            case "cont" -> filter.setContains(convertedValue);
            case "start" -> filter.setStartsWith(convertedValue);
            case "end" -> filter.setEndsWith(convertedValue);
            default -> throw new IllegalArgumentException("Unsupported property: " + propertyName);
        }

        return filter;
    }

    /**
     * Helper to manually set a collection property on StringFilter using PropertyEditor.
     */
    @SuppressWarnings("unchecked")
    private StringFilter setFilterCollectionProperty(String propertyName, String value) {
        StringFilter filter = new StringFilter();
        collectionEditor.setAsText(value);
        Collection<String> convertedValue = (Collection<String>) collectionEditor.getValue();

        // Manually set the property based on the property name
        switch (propertyName) {
            case "in" -> filter.setIn(convertedValue);
            case "nin" -> filter.setNotIn(convertedValue);
            default -> throw new IllegalArgumentException("Unsupported collection property: " + propertyName);
        }

        return filter;
    }

    @Nested
    @DisplayName("Single Value Operations")
    class SingleValueOperations {

        @Test
        @DisplayName("Should deserialize equals operation using FilterConstants.FIELD_EQ")
        void shouldDeserializeEqualsOperation() {
            // Given - simulating query parameter: ?name.eq=john
            String value = "john";

            // When
            StringFilter filter = setFilterProperty(FilterConstants.FIELD_EQ, value);

            // Then
            assertThat(filter.getEquals()).isEqualTo(JOHN);
            assertThat(FilterConstants.FIELD_EQ).isEqualTo("eq");
        }

        @Test
        @DisplayName("Should deserialize not equals operation using FilterConstants.FIELD_NEQ")
        void shouldDeserializeNotEqualsOperation() {
            // Given - simulating query parameter: ?name.neq=admin
            String value = "admin";

            // When
            StringFilter filter = setFilterProperty(FilterConstants.FIELD_NEQ, value);

            // Then
            assertThat(filter.getNotEquals()).isEqualTo("admin");
            assertThat(FilterConstants.FIELD_NEQ).isEqualTo("neq");
        }

        @Test
        @DisplayName("Should deserialize contains operation using FilterConstants.FIELD_CONT")
        void shouldDeserializeContainsOperation() {
            // Given - simulating query parameter: ?name.cont=search
            String value = "search";

            // When
            StringFilter filter = setFilterProperty(FilterConstants.FIELD_CONT, value);

            // Then
            assertThat(filter.getContains()).isEqualTo("search");
            assertThat(FilterConstants.FIELD_CONT).isEqualTo("cont");
        }

        @Test
        @DisplayName("Should deserialize startsWith operation using FilterConstants.FIELD_START")
        void shouldDeserializeStartsWithOperation() {
            // Given - simulating query parameter: ?name.start=prefix
            String value = "prefix";

            // When
            StringFilter filter = setFilterProperty(FilterConstants.FIELD_START, value);

            // Then
            assertThat(filter.getStartsWith()).isEqualTo("prefix");
            assertThat(FilterConstants.FIELD_START).isEqualTo("start");
        }

        @Test
        @DisplayName("Should deserialize endsWith operation using FilterConstants.FIELD_END")
        void shouldDeserializeEndsWithOperation() {
            // Given - simulating query parameter: ?name.end=suffix
            String value = "suffix";

            // When
            StringFilter filter = setFilterProperty(FilterConstants.FIELD_END, value);

            // Then
            assertThat(filter.getEndsWith()).isEqualTo("suffix");
            assertThat(FilterConstants.FIELD_END).isEqualTo("end");
        }
    }

    @Nested
    @DisplayName("Special Characters and Encoding")
    class SpecialCharactersAndEncoding {

        @ParameterizedTest
        @MethodSource("provideStringTestCases")
        @DisplayName("Should handle various string formats")
        void shouldHandleVariousStringFormats(String testCase, String inputValue, String expectedValue) {
            // When
            String result = deserializeSingleValue(inputValue);

            // Then
            assertThat(result).isEqualTo(expectedValue);
        }

        private static Stream<Arguments> provideStringTestCases() {
            return Stream.of(
                    Arguments.of("spaces", "John Doe", "John Doe"),
                    Arguments.of("special characters", "user@example.com", "user@example.com"),
                    Arguments.of("URL-encoded characters", "hello world", "hello world"),
                    Arguments.of("quotes", "it's", "it's"),
                    Arguments.of("ampersands", "Tom & Jerry", "Tom & Jerry"),
                    Arguments.of("forward slashes", "/api/users", "/api/users"),
                    Arguments.of("backslashes", "C:\\Users\\Admin", "C:\\Users\\Admin"),
                    Arguments.of("Unicode characters", "Café ☕", "Café ☕"),
                    Arguments.of("empty strings", "", ""),
                    Arguments.of("only whitespace", "   ", "   ")
            );
        }

        @Test
        @DisplayName("Should handle empty strings")
        void shouldHandleEmptyStrings() {
            // Given
            String value = "";

            // When
            String result = deserializeSingleValue(value);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Collection Operations")
    class CollectionOperations {

        @Test
        @DisplayName("Should deserialize 'in' operation with comma-separated values using FilterConstants.FIELD_IN")
        void shouldDeserializeInOperation() {
            // Given - simulating query parameter: ?name.in=john,jane,bob
            String value = "john,jane,bob";

            // When
            StringFilter filter = setFilterCollectionProperty(FilterConstants.FIELD_IN, value);

            // Then
            assertThat(filter.getIn())
                    .isNotNull()
                    .hasSize(3)
                    .containsExactly(JOHN, "jane", "bob");
            assertThat(FilterConstants.FIELD_IN).isEqualTo("in");
        }

        @Test
        @DisplayName("Should deserialize 'notIn' operation with comma-separated values using FilterConstants.FIELD_NIN")
        void shouldDeserializeNotInOperation() {
            // Given - simulating query parameter: ?name.nin=admin,root,system
            String value = "admin,root,system";

            // When
            List<String> result = deserializeCollectionValue(value);

            // Then
            assertThat(result)
                    .isNotNull()
                    .hasSize(3)
                    .containsExactly("admin", "root", "system");
            assertThat(FilterConstants.FIELD_NIN).isEqualTo("nin");
        }

        @Test
        @DisplayName("Should handle collection with single value")
        void shouldHandleCollectionWithSingleValue() {
            // Given
            String value = "john";

            // When
            List<String> result = deserializeCollectionValue(value);

            // Then
            assertThat(result)
                    .isNotNull()
                    .hasSize(1)
                    .containsExactly(JOHN);
        }

        @Test
        @DisplayName("Should handle collection with values containing spaces")
        void shouldHandleCollectionWithValuesContainingSpaces() {
            // Given
            String value = "John Doe,Jane Smith,Bob Johnson";

            // When
            List<String> result = deserializeCollectionValue(value);

            // Then
            assertThat(result)
                    .isNotNull()
                    .hasSize(3)
                    .containsExactly("John Doe", "Jane Smith", "Bob Johnson");
        }

        @Test
        @DisplayName("Should trim whitespace from collection elements")
        void shouldTrimWhitespaceFromCollectionElements() {
            // Given
            String value = " john , jane , bob ";

            // When
            List<String> result = deserializeCollectionValue(value);

            // Then
            assertThat(result)
                    .isNotNull()
                    .hasSize(3)
                    .containsExactly(JOHN, "jane", "bob");
        }

        @Test
        @DisplayName("Should handle empty collection")
        void shouldHandleEmptyCollection() {
            // Given
            String value = "";

            // When
            collectionEditor.setAsText(value);

            // Then
            assertThat(collectionEditor.getValue())
                    .isNotNull();
        }

        @Test
        @DisplayName("Should skip empty elements in collection")
        void shouldSkipEmptyElementsInCollection() {
            // Given
            String value = "john,,jane,,bob";

            // When
            List<String> result = deserializeCollectionValue(value);

            // Then
            assertThat(result)
                    .isNotNull()
                    .hasSize(3)
                    .containsExactly(JOHN, "jane", "bob");
        }

        @Test
        @DisplayName("Should handle collection with special characters")
        void shouldHandleCollectionWithSpecialCharacters() {
            // Given
            String value = "user@example.com,admin@test.org,support@company.net";

            // When
            List<String> result = deserializeCollectionValue(value);

            // Then
            assertThat(result)
                    .isNotNull()
                    .hasSize(3)
                    .containsExactly("user@example.com", "admin@test.org", "support@company.net");
        }
    }

    @Nested
    @DisplayName("Nested Field Binding")
    class NestedFieldBinding {

        @Test
        @DisplayName("Should bind nested field: name.eq using DataBinder")
        void shouldBindNestedFieldName() {
            // Given - simulating query parameter: ?name.eq=john
            UserFilterDto dto = new UserFilterDto();
            DataBinder binder = new DataBinder(dto);
            binder.setAutoGrowNestedPaths(true);
            binder.registerCustomEditor(String.class, "name.equals", stringEditor);

            MutablePropertyValues pvs = new MutablePropertyValues();
            pvs.add("name.equals", JOHN);

            // When
            binder.bind(pvs);

            // Then
            assertThat(dto.getName()).isNotNull();
            assertThat(dto.getName().getEquals()).isEqualTo(JOHN);
        }

        @Test
        @DisplayName("Should bind deeply nested field: address.city.eq using DataBinder")
        void shouldBindDeeplyNestedFieldCity() {
            // Given - simulating query parameter: ?address.city.eq=Istanbul
            UserFilterDto dto = new UserFilterDto();
            DataBinder binder = new DataBinder(dto);
            binder.setAutoGrowNestedPaths(true);
            binder.registerCustomEditor(String.class, "address.city.equals", stringEditor);

            MutablePropertyValues pvs = new MutablePropertyValues();
            pvs.add("address.city.equals", "Istanbul");

            // When
            binder.bind(pvs);

            // Then
            assertThat(dto.getAddress()).isNotNull();
            assertThat(dto.getAddress().getCity()).isNotNull();
            assertThat(dto.getAddress().getCity().getEquals()).isEqualTo("Istanbul");
        }

        @Test
        @DisplayName("Should bind multiple nested fields simultaneously")
        void shouldBindMultipleNestedFields() {
            // Given - simulating multiple query parameters
            UserFilterDto dto = new UserFilterDto();
            DataBinder binder = new DataBinder(dto);
            binder.setAutoGrowNestedPaths(true);

            // Create separate editor instances for each property to avoid state conflicts
            FilterPropertyEditor nameEditor = new FilterPropertyEditor(valueDeserializer, collectionHandler, registry, String.class);
            FilterPropertyEditor emailEditor = new FilterPropertyEditor(valueDeserializer, collectionHandler, registry, String.class);
            FilterPropertyEditor cityCollectionEditor = new FilterPropertyEditor(valueDeserializer, collectionHandler, registry, List.class, String.class);

            binder.registerCustomEditor(String.class, "name.contains", nameEditor);
            binder.registerCustomEditor(String.class, "email.endsWith", emailEditor);
            binder.registerCustomEditor(Collection.class, "address.city.in", cityCollectionEditor);

            MutablePropertyValues pvs = new MutablePropertyValues();
            pvs.add("name.contains", JOHN);
            pvs.add("email.endsWith", "@example.com");
            pvs.add("address.city.in", "Istanbul,Ankara,Izmir");

            // When
            binder.bind(pvs);

            // Then
            assertThat(dto.getName()).isNotNull();
            assertThat(dto.getName().getContains()).isEqualTo(JOHN);
            assertThat(dto.getEmail()).isNotNull();
            assertThat(dto.getEmail().getEndsWith()).isEqualTo("@example.com");
            assertThat(dto.getAddress()).isNotNull();
            assertThat(dto.getAddress().getCity()).isNotNull();
            assertThat(dto.getAddress().getCity().getIn())
                    .containsExactly("Istanbul", "Ankara", "Izmir");
        }

        /**
         * Test DTO for nested filter binding.
         */
        static class UserFilterDto {
            private StringFilter name;
            private StringFilter email;
            private AddressFilterDto address;

            public StringFilter getName() {
                return name;
            }

            public void setName(StringFilter name) {
                this.name = name;
            }

            public StringFilter getEmail() {
                return email;
            }

            public void setEmail(StringFilter email) {
                this.email = email;
            }

            public AddressFilterDto getAddress() {
                return address;
            }

            public void setAddress(AddressFilterDto address) {
                this.address = address;
            }
        }

        static class AddressFilterDto {
            private StringFilter city;
            private StringFilter street;

            public StringFilter getCity() {
                return city;
            }

            public void setCity(StringFilter city) {
                this.city = city;
            }

            public StringFilter getStreet() {
                return street;
            }

            public void setStreet(StringFilter street) {
                this.street = street;
            }
        }
    }

    @Nested
    @DisplayName("Operator Name Consistency")
    class OperatorNameConsistency {

        @Test
        @DisplayName("Equals operator should use FilterConstants.FIELD_EQ value")
        void equalsOperatorShouldUseFilterConstantsValue() {
            assertThat(FilterConstants.FIELD_EQ).isEqualTo("eq");
        }

        @Test
        @DisplayName("Not equals operator should use FilterConstants.FIELD_NEQ value")
        void notEqualsOperatorShouldUseFilterConstantsValue() {
            assertThat(FilterConstants.FIELD_NEQ).isEqualTo("neq");
        }

        @Test
        @DisplayName("Contains operator should use FilterConstants.FIELD_CONT value")
        void containsOperatorShouldUseFilterConstantsValue() {
            assertThat(FilterConstants.FIELD_CONT).isEqualTo("cont");
        }

        @Test
        @DisplayName("StartsWith operator should use FilterConstants.FIELD_START value")
        void startsWithOperatorShouldUseFilterConstantsValue() {
            assertThat(FilterConstants.FIELD_START).isEqualTo("start");
        }

        @Test
        @DisplayName("EndsWith operator should use FilterConstants.FIELD_END value")
        void endsWithOperatorShouldUseFilterConstantsValue() {
            assertThat(FilterConstants.FIELD_END).isEqualTo("end");
        }

        @Test
        @DisplayName("In operator should use FilterConstants.FIELD_IN value")
        void inOperatorShouldUseFilterConstantsValue() {
            assertThat(FilterConstants.FIELD_IN).isEqualTo("in");
        }

        @Test
        @DisplayName("NotIn operator should use FilterConstants.FIELD_NIN value")
        void notInOperatorShouldUseFilterConstantsValue() {
            assertThat(FilterConstants.FIELD_NIN).isEqualTo("nin");
        }

        @Test
        @DisplayName("Match operator should use FilterConstants.FIELD_MATCH value")
        void matchOperatorShouldUseFilterConstantsValue() {
            assertThat(FilterConstants.FIELD_MATCH).isEqualTo("match");
        }

        @Test
        @DisplayName("IsNull operator should use FilterConstants.FIELD_ISN value")
        void isNullOperatorShouldUseFilterConstantsValue() {
            assertThat(FilterConstants.FIELD_ISN).isEqualTo("isn");
        }

        @Test
        @DisplayName("IsNotNull operator should use FilterConstants.FIELD_ISNN value")
        void isNotNullOperatorShouldUseFilterConstantsValue() {
            assertThat(FilterConstants.FIELD_ISNN).isEqualTo("isnn");
        }
    }

    @Nested
    @DisplayName("Multiple Operations")
    class MultipleOperations {

        @Test
        @DisplayName("Should deserialize multiple filter operations on same field")
        void shouldDeserializeMultipleOperations() {
            // Given - simulating multiple query parameters on same field
            StringFilter filter = new StringFilter();

            // When - applying multiple operations
            stringEditor.setAsText("search");
            filter.setContains((String) stringEditor.getValue());

            stringEditor.setAsText("prefix");
            filter.setStartsWith((String) stringEditor.getValue());

            collectionEditor.setAsText("admin,root");
            @SuppressWarnings("unchecked")
            Collection<String> notInValues = (Collection<String>) collectionEditor.getValue();
            filter.setNotIn(notInValues);

            // Then
            final String containsValue = "search";
            final String startsWithValue = "prefix";
            final String firstNotIn = "admin";
            final String secondNotIn = "root";
            assertThat(filter)
                    .satisfies(f -> {
                        assertThat(f.getContains()).isEqualTo(containsValue);
                        assertThat(f.getStartsWith()).isEqualTo(startsWithValue);
                        assertThat(f.getNotIn())
                                .hasSize(2)
                                .containsExactly(firstNotIn, secondNotIn);
                    });
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle very long strings")
        void shouldHandleVeryLongStrings() {
            // Given
            String longValue = "a".repeat(1000);

            // When
            String result = deserializeSingleValue(longValue);

            // Then
            assertThat(result).hasSize(1000);
        }

        @Test
        @DisplayName("Should handle collection with many elements")
        void shouldHandleCollectionWithManyElements() {
            // Given
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                if (i > 0) sb.append(",");
                sb.append("value").append(i);
            }

            // When
            List<String> result = deserializeCollectionValue(sb.toString());

            // Then
            assertThat(result).hasSize(100);
        }
    }
}
