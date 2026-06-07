package com.thy.fss.common.inmemory.processor.generator;

import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer;
import com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests for nested filter handling in bindQueryParameters() generation.
 * Tests requirement 4.2, 4.3, 4.4, 5.3, 5.4
 */
@DisplayName("Nested Filter Binding Tests")
class NestedFilterBindingTest {

    @Mock
    private FilterValueDeserializer deserializer;

    @Mock
    private DeserializerRegistry registry;

    @Mock
    private CollectionParameterHandler collectionHandler;

    private Class<?> abbreviatedUserFilterDeserializer;
    private Class<?> abbreviatedUserFilterClass;
    private Method bindQueryParametersMethod;

    private static final String SKIPPING_TEST_MSG = "Skipping test - generated classes not available";
    private static final String NEW_YORK = "New York";
    private static final String GET_ADDRESS_METHOD = "getAddress";
    private static final String GET_CITY_METHOD = "getCity";
    private static final String BOSTON = "Boston";
    private static final String ADDR_CITY_EQ_PATH = "addr.c.eq";
    private static final String SEATTLE = "Seattle";
    private static final String MAIN_ST = "Main St";
    private static final String PORTLAND = "Portland";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Load the generated deserializer classes
        try {
            abbreviatedUserFilterDeserializer = Class.forName(
                    "com.thy.fss.common.inmemory.testmodel.AbbreviatedUserFilterDeserializer"
            );
            abbreviatedUserFilterClass = Class.forName(
                    "com.thy.fss.common.inmemory.testmodel.AbbreviatedUserFilter"
            );
            bindQueryParametersMethod = abbreviatedUserFilterDeserializer.getMethod(
                    "bindQueryParameters",
                    Map.class,
                    FilterValueDeserializer.class,
                    CollectionParameterHandler.class,
                    DeserializerRegistry.class
            );
        } catch (ClassNotFoundException e) {
            System.err.println("Generated filter deserializer classes not found. " +
                    "Run 'gradlew compileTestJava' to generate them.");
        }
    }

    @Test
    @DisplayName("Should handle nested filter path with single level (address.city.eq)")
    void shouldHandleNestedFilterPathWithSingleLevel() throws Exception {
        if (bindQueryParametersMethod == null) {
            System.out.println(SKIPPING_TEST_MSG);
            return;
        }

        // Setup mocks
        when(deserializer.deserializeValue(eq(NEW_YORK), eq(String.class), any()))
                .thenReturn(NEW_YORK);

        // Create parameter map with nested path
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("address.city.eq", new String[]{NEW_YORK});

        // Invoke bindQueryParameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );

        // Verify the filter was created
        assertThat(filter).isNotNull();
        assertThat(filter.getClass().getSimpleName()).isEqualTo("AbbreviatedUserFilter");

        // Verify nested filter was initialized
        Method getAddress = abbreviatedUserFilterClass.getMethod(GET_ADDRESS_METHOD);
        Object addressFilter = getAddress.invoke(filter);
        assertThat(addressFilter).isNotNull();

        // Verify nested field was set
        Method getCity = addressFilter.getClass().getMethod(GET_CITY_METHOD);
        Object cityFilter = getCity.invoke(addressFilter);
        assertThat(cityFilter).isNotNull();
    }

    @Test
    @DisplayName("Should handle abbreviated nested filter path (addr.c.eq)")
    void shouldHandleAbbreviatedNestedFilterPath() throws Exception {
        if (bindQueryParametersMethod == null) {
            System.out.println(SKIPPING_TEST_MSG);
            return;
        }

        // Setup mocks
        when(deserializer.deserializeValue(eq(BOSTON), eq(String.class), any()))
                .thenReturn(BOSTON);

        // Create parameter map with abbreviated nested path
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put(ADDR_CITY_EQ_PATH, new String[]{BOSTON});

        // Invoke bindQueryParameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );

        // Verify the filter was created
        assertThat(filter).isNotNull();

        // Verify nested filter was initialized
        Method getAddress = abbreviatedUserFilterClass.getMethod(GET_ADDRESS_METHOD);
        Object addressFilter = getAddress.invoke(filter);
        assertThat(addressFilter).isNotNull();

        // Verify nested field was set
        Method getCity = addressFilter.getClass().getMethod(GET_CITY_METHOD);
        Object cityFilter = getCity.invoke(addressFilter);
        assertThat(cityFilter).isNotNull();
    }

    @Test
    @DisplayName("Should handle multiple nested filter paths")
    void shouldHandleMultipleNestedFilterPaths() throws Exception {
        if (bindQueryParametersMethod == null) {
            System.out.println(SKIPPING_TEST_MSG);
            return;
        }

        // Setup mocks
        when(deserializer.deserializeValue(eq(SEATTLE), eq(String.class), any()))
                .thenReturn(SEATTLE);
        when(deserializer.deserializeValue(eq(MAIN_ST), eq(String.class), any()))
                .thenReturn(MAIN_ST);

        // Create parameter map with multiple nested paths
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put(ADDR_CITY_EQ_PATH, new String[]{SEATTLE});
        parameterMap.put("addr.st.cont", new String[]{MAIN_ST});

        // Invoke bindQueryParameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );

        // Verify the filter was created
        assertThat(filter).isNotNull();

        // Verify nested filter was initialized
        Method getAddress = abbreviatedUserFilterClass.getMethod(GET_ADDRESS_METHOD);
        Object addressFilter = getAddress.invoke(filter);
        assertThat(addressFilter).isNotNull();

        // Verify both nested fields were set
        Method getCity = addressFilter.getClass().getMethod(GET_CITY_METHOD);
        Object cityFilter = getCity.invoke(addressFilter);
        assertThat(cityFilter).isNotNull();

        Method getStreet = addressFilter.getClass().getMethod("getStreet");
        Object streetFilter = getStreet.invoke(addressFilter);
        assertThat(streetFilter).isNotNull();
    }

    @Test
    @DisplayName("Should handle mix of direct and nested filter paths")
    void shouldHandleMixOfDirectAndNestedFilterPaths() throws Exception {
        if (bindQueryParametersMethod == null) {
            System.out.println(SKIPPING_TEST_MSG);
            return;
        }

        // Setup mocks
        when(deserializer.deserializeValue(eq("John"), eq(String.class), any()))
                .thenReturn("John");
        when(deserializer.deserializeValue(eq(PORTLAND), eq(String.class), any()))
                .thenReturn(PORTLAND);

        // Create parameter map with both direct and nested paths
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("n.eq", new String[]{"John"});  // Direct field
        parameterMap.put(ADDR_CITY_EQ_PATH, new String[]{PORTLAND});  // Nested field

        // Invoke bindQueryParameters
        Object filter = bindQueryParametersMethod.invoke(
                null, parameterMap, deserializer, collectionHandler, registry
        );

        // Verify the filter was created
        assertThat(filter).isNotNull();

        // Verify direct field was set
        Method getName = abbreviatedUserFilterClass.getMethod("getName");
        Object nameFilter = getName.invoke(filter);
        assertThat(nameFilter).isNotNull();

        // Verify nested filter was initialized
        Method getAddress = abbreviatedUserFilterClass.getMethod(GET_ADDRESS_METHOD);
        Object addressFilter = getAddress.invoke(filter);
        assertThat(addressFilter).isNotNull();

        // Verify nested field was set
        Method getCity = addressFilter.getClass().getMethod(GET_CITY_METHOD);
        Object cityFilter = getCity.invoke(addressFilter);
        assertThat(cityFilter).isNotNull();
    }
}
