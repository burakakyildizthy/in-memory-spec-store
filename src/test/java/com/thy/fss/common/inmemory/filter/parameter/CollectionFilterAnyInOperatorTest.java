package com.thy.fss.common.inmemory.filter.parameter;

import com.thy.fss.common.inmemory.filter.StringFilter;
import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry;
import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistryImpl;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializerImpl;
import com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandlerImpl;
import com.thy.fss.common.inmemory.testmodel.CollectionEntityFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for collection filter any.in operator binding.
 * Validates that query parameters like ?tags.any.in=work,personal are correctly bound.
 */
@DisplayName("Collection Filter any.in Operator Tests")
class CollectionFilterAnyInOperatorTest {

    private FilterValueDeserializer deserializer;
    private CollectionParameterHandlerImpl collectionHandler;
    private DeserializerRegistry registry;

    @BeforeEach
    void setUp() {
        deserializer = new FilterValueDeserializerImpl();
        collectionHandler = new CollectionParameterHandlerImpl(deserializer);
        registry = new DeserializerRegistryImpl();
    }

    @Test
    @DisplayName("Should bind tags.any.in operator with comma-separated values")
    void shouldBindAnyInOperatorWithCommaSeparatedValues() {
        // Given: Query parameter with any.in operator
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.any.in", new String[]{"work,personal,urgent"});

        // When: We bind the query parameters
        CollectionEntityFilter filter = com.thy.fss.common.inmemory.testmodel.CollectionEntityFilterDeserializer
                .bindQueryParameters(parameterMap, deserializer, collectionHandler, registry);

        // Then: The filter should have a CollectionFilter with any operator set
        assertThat(filter.getTags()).isNotNull();
        assertThat(filter.getTags().getCollectionAny()).isNotNull();
        assertThat(filter.getTags().getCollectionAny()).isInstanceOf(StringFilter.class);

        StringFilter anyFilter = (StringFilter) filter.getTags().getCollectionAny();
        assertThat(anyFilter.getIn()).isNotNull();
        assertThat(anyFilter.getIn()).containsExactlyInAnyOrder("work", "personal", "urgent");
    }

    @Test
    @DisplayName("Should bind tags.any.in operator with single value")
    void shouldBindAnyInOperatorWithSingleValue() {
        // Given: Query parameter with any.in operator and single value
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.any.in", new String[]{"important"});

        // When: We bind the query parameters
        CollectionEntityFilter filter = com.thy.fss.common.inmemory.testmodel.CollectionEntityFilterDeserializer
                .bindQueryParameters(parameterMap, deserializer, collectionHandler, registry);

        // Then: The filter should have a CollectionFilter with any operator set
        assertThat(filter.getTags()).isNotNull();
        assertThat(filter.getTags().getCollectionAny()).isNotNull();

        StringFilter anyFilter = (StringFilter) filter.getTags().getCollectionAny();
        assertThat(anyFilter.getIn()).isNotNull();
        assertThat(anyFilter.getIn()).containsExactly("important");
    }

    @Test
    @DisplayName("Should bind tags.any.nin operator with comma-separated values")
    void shouldBindAnyNinOperatorWithCommaSeparatedValues() {
        // Given: Query parameter with any.nin operator
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.any.nin", new String[]{"spam,junk,deleted"});

        // When: We bind the query parameters
        CollectionEntityFilter filter = com.thy.fss.common.inmemory.testmodel.CollectionEntityFilterDeserializer
                .bindQueryParameters(parameterMap, deserializer, collectionHandler, registry);

        // Then: The filter should have a CollectionFilter with any operator set
        assertThat(filter.getTags()).isNotNull();
        assertThat(filter.getTags().getCollectionAny()).isNotNull();

        StringFilter anyFilter = (StringFilter) filter.getTags().getCollectionAny();
        assertThat(anyFilter.getNotIn()).isNotNull();
        assertThat(anyFilter.getNotIn()).containsExactlyInAnyOrder("spam", "junk", "deleted");
    }

    @Test
    @DisplayName("Should bind tags.all.in operator with comma-separated values")
    void shouldBindAllInOperatorWithCommaSeparatedValues() {
        // Given: Query parameter with all.in operator
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.all.in", new String[]{"verified,active"});

        // When: We bind the query parameters
        CollectionEntityFilter filter = com.thy.fss.common.inmemory.testmodel.CollectionEntityFilterDeserializer
                .bindQueryParameters(parameterMap, deserializer, collectionHandler, registry);

        // Then: The filter should have a CollectionFilter with all operator set
        assertThat(filter.getTags()).isNotNull();
        assertThat(filter.getTags().getCollectionAll()).isNotNull();

        StringFilter allFilter = (StringFilter) filter.getTags().getCollectionAll();
        assertThat(allFilter.getIn()).isNotNull();
        assertThat(allFilter.getIn()).containsExactlyInAnyOrder("verified", "active");
    }

    @Test
    @DisplayName("Should bind tags.none.in operator with comma-separated values")
    void shouldBindNoneInOperatorWithCommaSeparatedValues() {
        // Given: Query parameter with none.in operator
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.none.in", new String[]{"forbidden,blocked"});

        // When: We bind the query parameters
        CollectionEntityFilter filter = com.thy.fss.common.inmemory.testmodel.CollectionEntityFilterDeserializer
                .bindQueryParameters(parameterMap, deserializer, collectionHandler, registry);

        // Then: The filter should have a CollectionFilter with none operator set
        assertThat(filter.getTags()).isNotNull();
        assertThat(filter.getTags().getCollectionNone()).isNotNull();

        StringFilter noneFilter = (StringFilter) filter.getTags().getCollectionNone();
        assertThat(noneFilter.getIn()).isNotNull();
        assertThat(noneFilter.getIn()).containsExactlyInAnyOrder("forbidden", "blocked");
    }

    @Test
    @DisplayName("Should combine any.in with other operators")
    void shouldCombineAnyInWithOtherOperators() {
        // Given: Multiple query parameters including any.in
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.any.in", new String[]{"work,personal"});
        parameterMap.put("tags.any.cont", new String[]{"important"});

        // When: We bind the query parameters
        CollectionEntityFilter filter = com.thy.fss.common.inmemory.testmodel.CollectionEntityFilterDeserializer
                .bindQueryParameters(parameterMap, deserializer, collectionHandler, registry);

        // Then: Both operators should be set on the same any filter
        assertThat(filter.getTags()).isNotNull();
        assertThat(filter.getTags().getCollectionAny()).isNotNull();

        StringFilter anyFilter = (StringFilter) filter.getTags().getCollectionAny();
        assertThat(anyFilter.getIn()).containsExactlyInAnyOrder("work", "personal");
        assertThat(anyFilter.getContains()).isEqualTo("important");
    }
}
