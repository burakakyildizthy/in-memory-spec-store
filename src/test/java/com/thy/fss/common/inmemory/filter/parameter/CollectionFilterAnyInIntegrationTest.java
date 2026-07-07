package com.thy.fss.common.inmemory.filter.parameter;

import com.thy.fss.common.inmemory.filter.CollectionFilter;
import com.thy.fss.common.inmemory.filter.StringFilter;
import com.thy.fss.common.inmemory.testmodel.CollectionEntity;
import com.thy.fss.common.inmemory.testmodel.CollectionEntityFilter;
import com.thy.fss.common.inmemory.testmodel.CollectionEntitySpecificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for collection filter any.in operator with actual filtering.
 * Tests that the filter is correctly applied to entities.
 */
@DisplayName("Collection Filter any.in Integration Tests")
class CollectionFilterAnyInIntegrationTest {

    private static final String WORK = "work";
    private static final String PERSONAL = "personal";
    private static final String JUNK = "junk";
    private static final String SPAM = "spam";
    private static final String ARCHIVED = "archived";
    private static final String IMPORTANT = "important";

    private List<CollectionEntity> testEntities;
    private CollectionEntitySpecificationService specService;

    @BeforeEach
    void setUp() {
        specService = CollectionEntitySpecificationService.INSTANCE;
        
        // Create test entities
        testEntities = new ArrayList<>();
        
        // Entity 1: tags = [WORK, IMPORTANT]
        CollectionEntity entity1 = new CollectionEntity();
        entity1.setId(1L);
        entity1.setTags(Arrays.asList(WORK, IMPORTANT));
        testEntities.add(entity1);
        
        // Entity 2: tags = [PERSONAL, "urgent"]
        CollectionEntity entity2 = new CollectionEntity();
        entity2.setId(2L);
        entity2.setTags(Arrays.asList(PERSONAL, "urgent"));
        testEntities.add(entity2);
        
        // Entity 3: tags = [WORK, PERSONAL]
        CollectionEntity entity3 = new CollectionEntity();
        entity3.setId(3L);
        entity3.setTags(Arrays.asList(WORK, PERSONAL));
        testEntities.add(entity3);
        
        // Entity 4: tags = [ARCHIVED]
        CollectionEntity entity4 = new CollectionEntity();
        entity4.setId(4L);
        entity4.setTags(Arrays.asList(ARCHIVED));
        testEntities.add(entity4);
        
        // Entity 5: tags = [SPAM, JUNK]
        CollectionEntity entity5 = new CollectionEntity();
        entity5.setId(5L);
        entity5.setTags(Arrays.asList(SPAM, JUNK));
        testEntities.add(entity5);
    }

    @Test
    @DisplayName("Should filter entities where any tag is in the specified list")
    void shouldFilterByAnyTagInList() {
        // Given: Filter for entities where any tag is WORK or PERSONAL
        StringFilter anyFilter = new StringFilter();
        anyFilter.setIn(Arrays.asList(WORK, PERSONAL));
        
        CollectionFilter<String> tagsFilter = new CollectionFilter<>();
        tagsFilter.setCollectionAny(anyFilter);
        
        CollectionEntityFilter filter = new CollectionEntityFilter();
        filter.setTags(tagsFilter);
        
        // When: Apply filter
        List<CollectionEntity> result = testEntities.stream()
                .filter(entity -> specService.validateFilter(entity, filter))
                .toList();
        
        // Then: Should find entities 1, 2, and 3 (they have WORK or PERSONAL)
        assertThat(result).hasSize(3);
        assertThat(result).extracting(CollectionEntity::getIdentity)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("Should filter entities where any tag is NOT in the specified list")
    void shouldFilterByAnyTagNotInList() {
        // Given: Filter for entities where any tag is NOT SPAM or JUNK
        StringFilter anyFilter = new StringFilter();
        anyFilter.setNotIn(Arrays.asList(SPAM, JUNK));
        
        CollectionFilter<String> tagsFilter = new CollectionFilter<>();
        tagsFilter.setCollectionAny(anyFilter);
        
        CollectionEntityFilter filter = new CollectionEntityFilter();
        filter.setTags(tagsFilter);
        
        // When: Apply filter
        List<CollectionEntity> result = testEntities.stream()
                .filter(entity -> specService.validateFilter(entity, filter))
                .toList();
        
        // Then: Should find entities 1, 2, 3, and 4 (they have tags other than spam/junk)
        assertThat(result).hasSize(4);
        assertThat(result).extracting(CollectionEntity::getIdentity)
                .containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
    }

    @Test
    @DisplayName("Should filter entities where all tags are in the specified list")
    void shouldFilterByAllTagsInList() {
        // Given: Filter for entities where all tags are in [WORK, IMPORTANT, PERSONAL]
        StringFilter allFilter = new StringFilter();
        allFilter.setIn(Arrays.asList(WORK, IMPORTANT, PERSONAL));
        
        CollectionFilter<String> tagsFilter = new CollectionFilter<>();
        tagsFilter.setCollectionAll(allFilter);
        
        CollectionEntityFilter filter = new CollectionEntityFilter();
        filter.setTags(tagsFilter);
        
        // When: Apply filter
        List<CollectionEntity> result = testEntities.stream()
                .filter(entity -> specService.validateFilter(entity, filter))
                .toList();
        
        // Then: Should find entities 1 and 3 (all their tags are in the list)
        assertThat(result).hasSize(2);
        assertThat(result).extracting(CollectionEntity::getIdentity)
                .containsExactlyInAnyOrder(1L, 3L);
    }

    @Test
    @DisplayName("Should filter entities where no tags are in the specified list")
    void shouldFilterByNoTagsInList() {
        // Given: Filter for entities where no tags are in [SPAM, JUNK, ARCHIVED]
        StringFilter noneFilter = new StringFilter();
        noneFilter.setIn(Arrays.asList(SPAM, JUNK, ARCHIVED));
        
        CollectionFilter<String> tagsFilter = new CollectionFilter<>();
        tagsFilter.setCollectionNone(noneFilter);
        
        CollectionEntityFilter filter = new CollectionEntityFilter();
        filter.setTags(tagsFilter);
        
        // When: Apply filter
        List<CollectionEntity> result = testEntities.stream()
                .filter(entity -> specService.validateFilter(entity, filter))
                .toList();
        
        // Then: Should find entities 1, 2, and 3 (they don't have spam/junk/archived)
        assertThat(result).hasSize(3);
        assertThat(result).extracting(CollectionEntity::getIdentity)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    @DisplayName("Should combine any.in with other operators")
    void shouldCombineAnyInWithOtherOperators() {
        // Given: Filter for entities where any tag is in [WORK, PERSONAL] AND contains WORK
        StringFilter anyFilter = new StringFilter();
        anyFilter.setIn(Arrays.asList(WORK, PERSONAL));
        anyFilter.setContains(WORK);
        
        CollectionFilter<String> tagsFilter = new CollectionFilter<>();
        tagsFilter.setCollectionAny(anyFilter);
        
        CollectionEntityFilter filter = new CollectionEntityFilter();
        filter.setTags(tagsFilter);
        
        // When: Apply filter
        List<CollectionEntity> result = testEntities.stream()
                .filter(entity -> specService.validateFilter(entity, filter))
                .toList();
        
        // Then: Should find entities 1 and 3 (they have WORK which is in the list and contains WORK)
        assertThat(result).hasSize(2);
        assertThat(result).extracting(CollectionEntity::getIdentity)
                .containsExactlyInAnyOrder(1L, 3L);
    }
}
