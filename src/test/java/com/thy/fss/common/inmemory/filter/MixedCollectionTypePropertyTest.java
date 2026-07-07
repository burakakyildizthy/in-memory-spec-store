package com.thy.fss.common.inmemory.filter;

import com.thy.fss.common.inmemory.testmodel.*;
import net.jqwik.api.*;
import org.junit.jupiter.api.DisplayName;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for mixed collection type support.
 * 
 * Feature: collection-filter-model-type-support
 * Property 7: Mixed collection type support
 * Validates: Requirements 8.1, 8.2, 8.3
 * 
 * Tests that filters with both basic type and model type collection fields
 * generate correct binding code for both types without interference.
 */
@DisplayName("Property 7: Mixed Collection Type Support")
class MixedCollectionTypePropertyTest {

    /**
     * Property: For any filter with both basic type and model type collection fields,
     * the system should generate correct binding code for both types without interference.
     * 
     * This test verifies that:
     * 1. Basic type collections (String, Integer) can be bound correctly
     * 2. Model type collections (User, Profile) can be bound correctly
     * 3. Both types can be used in the same filter without interference
     * 4. Type-safe casts using FilterBase work correctly
     */
    @Property(tries = 100)
    @DisplayName("Mixed basic and model type collections bind correctly without interference")
    void mixedCollectionTypesShouldBindWithoutInterference(
            @ForAll @net.jqwik.api.constraints.StringLength(min = 1, max = 20) String tagValue,
            @ForAll @net.jqwik.api.constraints.IntRange(min = 1, max = 100) int scoreValue,
            @ForAll @net.jqwik.api.constraints.StringLength(min = 1, max = 20) String userName,
            @ForAll @net.jqwik.api.constraints.StringLength(min = 1, max = 50) String profileBio
    ) {
        // Create parameter map with both basic and model type collection parameters
        Map<String, String[]> parameterMap = new HashMap<>();
        
        // Basic type collection parameters
        parameterMap.put("tags.any.cont", new String[]{tagValue});
        parameterMap.put("scores.any.eq", new String[]{String.valueOf(scoreValue)});
        
        // Model type collection parameters
        parameterMap.put("users.any.name.eq", new String[]{userName});
        parameterMap.put("profiles.any.bio.cont", new String[]{profileBio});
        
        // Create dependencies
        com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer deserializer = 
            new com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializerImpl();
        com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandler collectionHandler = 
            new com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandlerImpl(deserializer);
        com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry registry = 
            new com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistryImpl();
        
        // Bind parameters using generated deserializer
        MixedCollectionEntityFilter filter = MixedCollectionEntityFilterDeserializer.bindQueryParameters(
                parameterMap,
                deserializer,
                collectionHandler,
                registry
        );
        
        // Verify basic type collections are bound correctly
        assertThat(filter.getTags()).isNotNull();
        assertThat(filter.getTags().getCollectionAny()).isNotNull();
        assertThat(filter.getTags().getCollectionAny()).isInstanceOf(StringFilter.class);
        StringFilter tagFilter = (StringFilter) filter.getTags().getCollectionAny();
        assertThat(tagFilter.getContains()).isEqualTo(tagValue);
        
        assertThat(filter.getScores()).isNotNull();
        assertThat(filter.getScores().getCollectionAny()).isNotNull();
        assertThat(filter.getScores().getCollectionAny()).isInstanceOf(IntegerFilter.class);
        IntegerFilter scoreFilter = (IntegerFilter) filter.getScores().getCollectionAny();
        assertThat(scoreFilter.getEquals()).isEqualTo(scoreValue);
        
        // Verify model type collections are bound correctly
        assertThat(filter.getUsers()).isNotNull();
        assertThat(filter.getUsers().getCollectionAny()).isNotNull();
        assertThat(filter.getUsers().getCollectionAny()).isInstanceOf(UserFilter.class);
        UserFilter userFilter = (UserFilter) filter.getUsers().getCollectionAny();
        assertThat(userFilter.getName()).isNotNull();
        assertThat(userFilter.getName().getEquals()).isEqualTo(userName);
        
        assertThat(filter.getProfiles()).isNotNull();
        assertThat(filter.getProfiles().getCollectionAny()).isNotNull();
        assertThat(filter.getProfiles().getCollectionAny()).isInstanceOf(ProfileFilter.class);
        ProfileFilter profileFilter = (ProfileFilter) filter.getProfiles().getCollectionAny();
        assertThat(profileFilter.getBio()).isNotNull();
        assertThat(profileFilter.getBio().getContains()).isEqualTo(profileBio);
    }

    /**
     * Property: For any filter with mixed collection types, binding one type
     * should not affect the binding of another type.
     */
    @Property(tries = 100)
    @DisplayName("Binding basic type collections does not interfere with model type collections")
    void basicTypeBindingShouldNotInterfereWithModelType(
            @ForAll @net.jqwik.api.constraints.StringLength(min = 1, max = 20) String tagValue,
            @ForAll @net.jqwik.api.constraints.StringLength(min = 1, max = 20) String userName
    ) {
        // Create dependencies
        com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer deserializer = 
            new com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializerImpl();
        com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandler collectionHandler = 
            new com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandlerImpl(deserializer);
        com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry registry = 
            new com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistryImpl();
        
        // First bind basic type collection
        Map<String, String[]> parameterMap1 = new HashMap<>();
        parameterMap1.put("tags.any.cont", new String[]{tagValue});
        
        MixedCollectionEntityFilter filter1 = MixedCollectionEntityFilterDeserializer.bindQueryParameters(
                parameterMap1,
                deserializer,
                collectionHandler,
                registry
        );
        
        // Verify basic type is bound, model type is not
        assertThat(filter1.getTags()).isNotNull();
        assertThat(filter1.getTags().getCollectionAny()).isNotNull();
        assertThat(filter1.getUsers()).isNull();
        
        // Then bind model type collection
        Map<String, String[]> parameterMap2 = new HashMap<>();
        parameterMap2.put("users.any.name.eq", new String[]{userName});
        
        MixedCollectionEntityFilter filter2 = MixedCollectionEntityFilterDeserializer.bindQueryParameters(
                parameterMap2,
                deserializer,
                collectionHandler,
                registry
        );
        
        // Verify model type is bound, basic type is not
        assertThat(filter2.getUsers()).isNotNull();
        assertThat(filter2.getUsers().getCollectionAny()).isNotNull();
        assertThat(filter2.getTags()).isNull();
    }

    /**
     * Property: For any filter with mixed collection types, all three operators
     * (any, all, none) should work correctly for both basic and model types.
     */
    @Property(tries = 100)
    @DisplayName("All collection operators work for both basic and model types")
    void allOperatorsShouldWorkForBothTypes(
            @ForAll @net.jqwik.api.constraints.StringLength(min = 1, max = 20) String tagValue,
            @ForAll @net.jqwik.api.constraints.StringLength(min = 1, max = 20) String userName
    ) {
        // Create dependencies
        com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer deserializer = 
            new com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializerImpl();
        com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandler collectionHandler = 
            new com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandlerImpl(deserializer);
        com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry registry = 
            new com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistryImpl();
        
        // Test all three operators for basic type
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.any.cont", new String[]{tagValue + "_any"});
        parameterMap.put("tags.all.cont", new String[]{tagValue + "_all"});
        parameterMap.put("tags.none.cont", new String[]{tagValue + "_none"});
        
        // Test all three operators for model type
        parameterMap.put("users.any.name.eq", new String[]{userName + "_any"});
        parameterMap.put("users.all.name.eq", new String[]{userName + "_all"});
        parameterMap.put("users.none.name.eq", new String[]{userName + "_none"});
        
        MixedCollectionEntityFilter filter = MixedCollectionEntityFilterDeserializer.bindQueryParameters(
                parameterMap,
                deserializer,
                collectionHandler,
                registry
        );
        
        // Verify basic type operators
        assertThat(filter.getTags()).isNotNull();
        assertThat(filter.getTags().getCollectionAny()).isNotNull();
        assertThat(((StringFilter) filter.getTags().getCollectionAny()).getContains()).isEqualTo(tagValue + "_any");
        
        assertThat(filter.getTags().getCollectionAll()).isNotNull();
        assertThat(((StringFilter) filter.getTags().getCollectionAll()).getContains()).isEqualTo(tagValue + "_all");
        
        assertThat(filter.getTags().getCollectionNone()).isNotNull();
        assertThat(((StringFilter) filter.getTags().getCollectionNone()).getContains()).isEqualTo(tagValue + "_none");
        
        // Verify model type operators
        assertThat(filter.getUsers()).isNotNull();
        assertThat(filter.getUsers().getCollectionAny()).isNotNull();
        assertThat(((UserFilter) filter.getUsers().getCollectionAny()).getName().getEquals()).isEqualTo(userName + "_any");
        
        assertThat(filter.getUsers().getCollectionAll()).isNotNull();
        assertThat(((UserFilter) filter.getUsers().getCollectionAll()).getName().getEquals()).isEqualTo(userName + "_all");
        
        assertThat(filter.getUsers().getCollectionNone()).isNotNull();
        assertThat(((UserFilter) filter.getUsers().getCollectionNone()).getName().getEquals()).isEqualTo(userName + "_none");
    }

    /**
     * Property: FilterBase type compatibility should work for both basic and model types.
     */
    @Property(tries = 100)
    @DisplayName("FilterBase provides type compatibility for both basic and model types")
    void filterBaseShouldProvideTypeCompatibility(
            @ForAll @net.jqwik.api.constraints.StringLength(min = 1, max = 20) String tagValue,
            @ForAll @net.jqwik.api.constraints.StringLength(min = 1, max = 20) String userName
    ) {
        // Create dependencies
        com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer deserializer = 
            new com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializerImpl();
        com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandler collectionHandler = 
            new com.thy.fss.common.inmemory.filter.parameter.CollectionParameterHandlerImpl(deserializer);
        com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry registry = 
            new com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistryImpl();
        
        Map<String, String[]> parameterMap = new HashMap<>();
        parameterMap.put("tags.any.cont", new String[]{tagValue});
        parameterMap.put("users.any.name.eq", new String[]{userName});
        
        MixedCollectionEntityFilter filter = MixedCollectionEntityFilterDeserializer.bindQueryParameters(
                parameterMap,
                deserializer,
                collectionHandler,
                registry
        );
        
        // Verify FilterBase compatibility for basic type
        FilterBase<String> tagFilterBase = filter.getTags().getCollectionAny();
        assertThat(tagFilterBase).isNotNull();
        assertThat(tagFilterBase).isInstanceOf(FilterBase.class);
        assertThat(tagFilterBase).isInstanceOf(Filter.class);
        assertThat(tagFilterBase).isInstanceOf(StringFilter.class);
        
        // Verify FilterBase compatibility for model type
        FilterBase<User> userFilterBase = filter.getUsers().getCollectionAny();
        assertThat(userFilterBase).isNotNull();
        assertThat(userFilterBase).isInstanceOf(FilterBase.class);
        assertThat(userFilterBase).isInstanceOf(EntityFilter.class);
        assertThat(userFilterBase).isInstanceOf(UserFilter.class);
    }
}
