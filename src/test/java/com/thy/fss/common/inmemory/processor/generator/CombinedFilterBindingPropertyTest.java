package com.thy.fss.common.inmemory.processor.generator;

import com.thy.fss.common.inmemory.filter.CollectionFilter;
import com.thy.fss.common.inmemory.filter.StringFilter;
import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry;
import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistryImpl;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializerImpl;
import com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandler;
import com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandlerImpl;
import com.thy.fss.common.inmemory.testmodel.CollectionTestEntityFilter;
import com.thy.fss.common.inmemory.testmodel.CollectionTestEntityFilterDeserializer;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based test for combined filter binding.
 * 
 * **Feature: collection-filter-web-binding, Property 8: Combined filter binding**
 * **Validates: Requirements 5.4**
 * 
 * Property: For any query containing both collection and non-collection filter parameters,
 * the system should correctly bind all filter properties without interference.
 */
class CombinedFilterBindingPropertyTest {

    private final FilterValueDeserializer deserializer = new FilterValueDeserializerImpl();
    private final CollectionParameterHandler collectionHandler = new CollectionParameterHandlerImpl(deserializer);
    private final DeserializerRegistry registry = new DeserializerRegistryImpl();

    /**
     * Property: Combined collection and non-collection filters bind correctly.
     * 
     * For any combination of collection filter parameters and non-collection filter parameters,
     * both should be correctly bound to the filter object without interference.
     */
    @Property(tries = 100)
    void combinedFiltersBindCorrectly(
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String nameValue,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String tagValue,
            @ForAll("collectionOperators") String collectionOp
    ) {
        // Create parameter map with both collection and non-collection parameters
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("name.eq", new String[]{nameValue});
        parameterMap.put("tags." + collectionOp, new String[]{tagValue});
        
        // Bind parameters
        CollectionTestEntityFilter filter = CollectionTestEntityFilterDeserializer.bindQueryParameters(
            parameterMap, deserializer, collectionHandler, registry
        );
        
        // Verify non-collection filter is bound
        assertThat(filter.getName())
                .as("Non-collection filter (name) should be bound")
                .isNotNull();
        assertThat(filter.getName().getEquals())
                .as("Non-collection filter value should match")
                .isEqualTo(nameValue);
        
        // Verify collection filter is bound
        assertThat(filter.getTags())
                .as("Collection filter (tags) should be bound")
                .isNotNull();
        
        // Verify the specific collection operator is set
        switch (collectionOp) {
            case "cont" -> assertThat(filter.getTags().getCollectionContains())
                    .as("Collection contains should be set")
                    .isEqualTo(tagValue);
            case "empty" -> assertThat(filter.getTags().getIsEmpty())
                    .as("Collection isEmpty should be set")
                    .isNotNull();
            case "nempty" -> assertThat(filter.getTags().getIsNotEmpty())
                    .as("Collection isNotEmpty should be set")
                    .isNotNull();
        }
    }

    /**
     * Property: Multiple collection filters bind independently.
     * 
     * For any combination of multiple collection filter parameters on different fields,
     * each should be bound independently without interference.
     */
    @Property(tries = 100)
    void multipleCollectionFiltersBindIndependently(
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String tagValue,
            @ForAll int scoreValue
    ) {
        // Create parameter map with multiple collection filters
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.cont", new String[]{tagValue});
        parameterMap.put("scores.cont", new String[]{String.valueOf(scoreValue)});
        
        // Bind parameters
        CollectionTestEntityFilter filter = CollectionTestEntityFilterDeserializer.bindQueryParameters(
            parameterMap, deserializer, collectionHandler, registry
        );
        
        // Verify both collection filters are bound independently
        assertThat(filter.getTags())
                .as("Tags collection filter should be bound")
                .isNotNull();
        assertThat(filter.getTags().getCollectionContains())
                .as("Tags contains value should match")
                .isEqualTo(tagValue);
        
        assertThat(filter.getScores())
                .as("Scores collection filter should be bound")
                .isNotNull();
        assertThat(filter.getScores().getCollectionContains())
                .as("Scores contains value should match")
                .isEqualTo(scoreValue);
    }

    /**
     * Property: Collection and nested filters coexist correctly.
     * 
     * For any combination of direct collection operators and nested operators,
     * both should be bound correctly on the same field.
     */
    @Property(tries = 100)
    void collectionAndNestedFiltersCoexist(
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String containsValue,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String anyValue
    ) {
        // Create parameter map with both direct and nested operators
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.cont", new String[]{containsValue});
        parameterMap.put("tags.any.eq", new String[]{anyValue});
        
        // Bind parameters
        CollectionTestEntityFilter filter = CollectionTestEntityFilterDeserializer.bindQueryParameters(
            parameterMap, deserializer, collectionHandler, registry
        );
        
        // Verify both operators are set on the same field
        assertThat(filter.getTags())
                .as("Tags collection filter should be bound")
                .isNotNull();
        
        assertThat(filter.getTags().getCollectionContains())
                .as("Direct operator (cont) should be set")
                .isEqualTo(containsValue);
        
        assertThat(filter.getTags().getCollectionAny())
                .as("Nested operator (any) should be set")
                .isNotNull();
        
        StringFilter anyFilter = (StringFilter) filter.getTags().getCollectionAny();
        assertThat(anyFilter.getEquals())
                .as("Nested filter value should match")
                .isEqualTo(anyValue);
    }

    /**
     * Property: Empty parameter map produces empty filter.
     * 
     * For an empty parameter map, the filter should be created but all fields should be null.
     */
    @Property(tries = 10)
    void emptyParameterMapProducesEmptyFilter() {
        Map<String, String[]> parameterMap = new HashMap<>();
        
        CollectionTestEntityFilter filter = CollectionTestEntityFilterDeserializer.bindQueryParameters(
            parameterMap, deserializer, collectionHandler, registry
        );
        
        assertThat(filter)
                .as("Filter should be created")
                .isNotNull();
        
        assertThat(filter.getName())
                .as("Name filter should be null")
                .isNull();
        
        assertThat(filter.getTags())
                .as("Tags filter should be null")
                .isNull();
        
        assertThat(filter.getScores())
                .as("Scores filter should be null")
                .isNull();
    }

    @Provide
    Arbitrary<String> collectionOperators() {
        return Arbitraries.of("cont", "empty", "nempty");
    }
}
