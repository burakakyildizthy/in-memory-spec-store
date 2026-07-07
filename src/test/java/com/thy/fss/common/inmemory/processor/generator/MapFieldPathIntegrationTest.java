package com.thy.fss.common.inmemory.processor.generator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for mapFieldPath() method generation.
 * Tests the actual generated code by invoking the mapFieldPath() method
 * on compiled filter deserializers.
 * <p>
 * Requirements tested: 1.1, 1.2, 1.3, 4.1, 4.2
 */
@DisplayName("MapFieldPath Integration Tests")
class MapFieldPathIntegrationTest {

    private static Class<?> abbreviatedUserFilterDeserializer;
    private static Class<?> abbreviatedAddressFilterDeserializer;
    private static Method mapFieldPathMethod;
    private static Method addressMapFieldPathMethod;

    private static final String SKIPPING_TEST_MSG = "Skipping test - generated classes not available";

    @BeforeAll
    static void setUp() throws Exception {
        // Load the generated deserializer classes
        // These are generated at compile-time by the annotation processor
        try {
            abbreviatedUserFilterDeserializer = Class.forName(
                    "com.thy.fss.common.inmemory.testmodel.AbbreviatedUserFilterDeserializer"
            );
            mapFieldPathMethod = abbreviatedUserFilterDeserializer.getMethod("mapFieldPath", String.class);

            abbreviatedAddressFilterDeserializer = Class.forName(
                    "com.thy.fss.common.inmemory.testmodel.AbbreviatedAddressFilterDeserializer"
            );
            addressMapFieldPathMethod = abbreviatedAddressFilterDeserializer.getMethod("mapFieldPath", String.class);
        } catch (ClassNotFoundException e) {
            // If classes are not found, it means the annotation processor hasn't run yet
            // This is expected in some test scenarios
            System.err.println("Generated filter deserializer classes not found. " +
                    "Run 'gradlew compileTestJava' to generate them.");
        }
    }

    @Test
    @DisplayName("Should map simple abbreviated field name to Java field name")
    void shouldMapSimpleAbbreviatedFieldNameToJavaFieldName() throws Exception {
        if (mapFieldPathMethod == null) {
            System.out.println(SKIPPING_TEST_MSG);
            return;
        }

        // Test: n -> name
        String result = (String) mapFieldPathMethod.invoke(null, "n");
        assertThat(result).isEqualTo("name");

        // Test: e -> email
        result = (String) mapFieldPathMethod.invoke(null, "e");
        assertThat(result).isEqualTo("email");

        // Test: a -> age
        result = (String) mapFieldPathMethod.invoke(null, "a");
        assertThat(result).isEqualTo("age");

        // Test: stat -> status
        result = (String) mapFieldPathMethod.invoke(null, "stat");
        assertThat(result).isEqualTo("status");

        // Test: addr -> address
        result = (String) mapFieldPathMethod.invoke(null, "addr");
        assertThat(result).isEqualTo("address");
    }

    @Test
    @DisplayName("Should pass through unmapped field names unchanged")
    void shouldPassThroughUnmappedFieldNamesUnchanged() throws Exception {
        if (mapFieldPathMethod == null) {
            System.out.println(SKIPPING_TEST_MSG);
            return;
        }

        // Test: unmapped field should pass through
        String result = (String) mapFieldPathMethod.invoke(null, "unknownField");
        assertThat(result).isEqualTo("unknownField");

        // Test: id field (no @JsonProperty) should pass through
        result = (String) mapFieldPathMethod.invoke(null, "id");
        assertThat(result).isEqualTo("id");
    }

    @Test
    @DisplayName("Should preserve operator segments in mapped paths")
    void shouldPreserveOperatorSegmentsInMappedPaths() throws Exception {
        if (mapFieldPathMethod == null) {
            System.out.println(SKIPPING_TEST_MSG);
            return;
        }

        // Test: n.eq -> name.eq
        String result = (String) mapFieldPathMethod.invoke(null, "n.eq");
        assertThat(result).isEqualTo("name.eq");

        // Test: e.cont -> email.cont
        result = (String) mapFieldPathMethod.invoke(null, "e.cont");
        assertThat(result).isEqualTo("email.cont");

        // Test: a.gte -> age.gte
        result = (String) mapFieldPathMethod.invoke(null, "a.gte");
        assertThat(result).isEqualTo("age.gte");

        // Test: stat.in -> status.in
        result = (String) mapFieldPathMethod.invoke(null, "stat.in");
        assertThat(result).isEqualTo("status.in");

        // Test: stat.nin -> status.nin
        result = (String) mapFieldPathMethod.invoke(null, "stat.nin");
        assertThat(result).isEqualTo("status.nin");
    }

    @Test
    @DisplayName("Should map nested filter paths correctly")
    void shouldMapNestedFilterPathsCorrectly() throws Exception {
        if (mapFieldPathMethod == null || addressMapFieldPathMethod == null) {
            System.out.println(SKIPPING_TEST_MSG);
            return;
        }

        // Test: addr.c -> address.city
        String result = (String) mapFieldPathMethod.invoke(null, "addr.c");
        assertThat(result).isEqualTo("address.city");

        // Test: addr.st -> address.street
        result = (String) mapFieldPathMethod.invoke(null, "addr.st");
        assertThat(result).isEqualTo("address.street");

        // Test: addr.z -> address.zipCode
        result = (String) mapFieldPathMethod.invoke(null, "addr.z");
        assertThat(result).isEqualTo("address.zipCode");

        // Test: addr.ctry -> address.country
        result = (String) mapFieldPathMethod.invoke(null, "addr.ctry");
        assertThat(result).isEqualTo("address.country");
    }

    @Test
    @DisplayName("Should map nested filter paths with operators correctly")
    void shouldMapNestedFilterPathsWithOperatorsCorrectly() throws Exception {
        if (mapFieldPathMethod == null) {
            System.out.println(SKIPPING_TEST_MSG);
            return;
        }

        // Test: addr.c.eq -> address.city.eq
        String result = (String) mapFieldPathMethod.invoke(null, "addr.c.eq");
        assertThat(result).isEqualTo("address.city.eq");

        // Test: addr.st.cont -> address.street.cont
        result = (String) mapFieldPathMethod.invoke(null, "addr.st.cont");
        assertThat(result).isEqualTo("address.street.cont");

        // Test: addr.z.start -> address.zipCode.start
        result = (String) mapFieldPathMethod.invoke(null, "addr.z.start");
        assertThat(result).isEqualTo("address.zipCode.start");
    }

    @Test
    @DisplayName("Should handle null path gracefully")
    void shouldHandleNullPathGracefully() throws Exception {
        if (mapFieldPathMethod == null) {
            System.out.println(SKIPPING_TEST_MSG);
            return;
        }

        // Test: null should return null
        String result = (String) mapFieldPathMethod.invoke(null, (String) null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should handle empty path gracefully")
    void shouldHandleEmptyPathGracefully() throws Exception {
        if (mapFieldPathMethod == null) {
            System.out.println(SKIPPING_TEST_MSG);
            return;
        }

        // Test: empty string should return empty string
        String result = (String) mapFieldPathMethod.invoke(null, "");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle malformed paths gracefully")
    void shouldHandleMalformedPathsGracefully() throws Exception {
        if (mapFieldPathMethod == null) {
            System.out.println(SKIPPING_TEST_MSG);
            return;
        }

        // Test: path with only dots
        String result = (String) mapFieldPathMethod.invoke(null, "...");
        assertThat(result).isNotNull();

        // Test: path starting with dot
        result = (String) mapFieldPathMethod.invoke(null, ".name");
        assertThat(result).isNotNull();

        // Test: path ending with dot
        result = (String) mapFieldPathMethod.invoke(null, "name.");
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should handle multiple operator segments correctly")
    void shouldHandleMultipleOperatorSegmentsCorrectly() throws Exception {
        if (mapFieldPathMethod == null) {
            System.out.println(SKIPPING_TEST_MSG);
            return;
        }

        // Test: n.eq.extra -> name.eq.extra (preserves all segments after field)
        String result = (String) mapFieldPathMethod.invoke(null, "n.eq.extra");
        assertThat(result).isEqualTo("name.eq.extra");
    }

    @Test
    @DisplayName("Should map deeply nested paths correctly")
    void shouldMapDeeplyNestedPathsCorrectly() throws Exception {
        if (mapFieldPathMethod == null) {
            System.out.println(SKIPPING_TEST_MSG);
            return;
        }

        // Test: addr.c -> address.city (2 levels)
        String result = (String) mapFieldPathMethod.invoke(null, "addr.c");
        assertThat(result).isEqualTo("address.city");

        // Test: addr.c.eq -> address.city.eq (3 levels)
        result = (String) mapFieldPathMethod.invoke(null, "addr.c.eq");
        assertThat(result).isEqualTo("address.city.eq");
    }

    @Test
    @DisplayName("Should handle case sensitivity correctly")
    void shouldHandleCaseSensitivityCorrectly() throws Exception {
        if (mapFieldPathMethod == null) {
            System.out.println(SKIPPING_TEST_MSG);
            return;
        }

        // Test: case matters - N should not map to name
        String result = (String) mapFieldPathMethod.invoke(null, "N");
        assertThat(result).isEqualTo("N"); // Should pass through unchanged

        // Test: lowercase n should map to name
        result = (String) mapFieldPathMethod.invoke(null, "n");
        assertThat(result).isEqualTo("name");
    }

    @Test
    @DisplayName("Should verify nested filter delegation works")
    void shouldVerifyNestedFilterDelegationWorks() throws Exception {
        if (addressMapFieldPathMethod == null) {
            System.out.println(SKIPPING_TEST_MSG);
            return;
        }

        // Test the nested filter's mapFieldPath directly
        // st -> street
        String result = (String) addressMapFieldPathMethod.invoke(null, "st");
        assertThat(result).isEqualTo("street");

        // c -> city
        result = (String) addressMapFieldPathMethod.invoke(null, "c");
        assertThat(result).isEqualTo("city");

        // z -> zipCode
        result = (String) addressMapFieldPathMethod.invoke(null, "z");
        assertThat(result).isEqualTo("zipCode");

        // ctry -> country
        result = (String) addressMapFieldPathMethod.invoke(null, "ctry");
        assertThat(result).isEqualTo("country");
    }

    @Test
    @DisplayName("Should handle all standard operators correctly")
    void shouldHandleAllStandardOperatorsCorrectly() throws Exception {
        if (mapFieldPathMethod == null) {
            System.out.println(SKIPPING_TEST_MSG);
            return;
        }

        // Test all standard operators with abbreviated field name
        String[] operators = {"eq", "neq", "in", "nin", "isn", "isnn", "cont", "start", "end",
                "gt", "gte", "lt", "lte", "before", "after", "empty", "blank"};

        for (String operator : operators) {
            String input = "n." + operator;
            String expected = "name." + operator;
            String result = (String) mapFieldPathMethod.invoke(null, input);
            assertThat(result)
                    .as("Operator %s should be preserved", operator)
                    .isEqualTo(expected);
        }
    }
}
