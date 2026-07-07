package com.thy.fss.common.inmemory.filter;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.testmodel.EntityWithEnum;
import com.thy.fss.common.inmemory.testmodel.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for EnumFilter operations.
 * Tests all enum-specific filtering operations including getters, setters, and utility methods.
 * Requirements: 4.5, 15.9
 */
class EnumFilterTest {

    private final LargeDatasetGenerator generator = new LargeDatasetGenerator();

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    // ==================== Constructor Tests ====================

    @Test
    void testDefaultConstructorCreatesEmptyFilter() {
        EnumFilter<Status> filter = new EnumFilter<>();

        assertThat(filter.getEquals()).isNull();
        assertThat(filter.getNotEquals()).isNull();
        assertThat(filter.getIn()).isNull();
        assertThat(filter.getNotIn()).isNull();
        assertThat(filter.getIsNull()).isNull();
        assertThat(filter.getIsNotNull()).isNull();
    }

    @Test
    void testCopyConstructorCopiesAllFields() {
        EnumFilter<Status> original = new EnumFilter<Status>()
            .setEquals(Status.ACTIVE)
            .setNotEquals(Status.INACTIVE)
            .setIn(Arrays.asList(Status.ACTIVE, Status.PENDING))
            .setNotIn(Arrays.asList(Status.INACTIVE))
            .setIsNull(true)
            .setIsNotNull(false);

        EnumFilter<Status> copy = new EnumFilter<>(original);

        assertThat(copy.getEquals()).isEqualTo(Status.ACTIVE);
        assertThat(copy.getNotEquals()).isEqualTo(Status.INACTIVE);
        assertThat(copy.getIn()).containsExactly(Status.ACTIVE, Status.PENDING);
        assertThat(copy.getNotIn()).containsExactly(Status.INACTIVE);
        assertThat(copy.getIsNull()).isTrue();
        assertThat(copy.getIsNotNull()).isFalse();
    }

    @Test
    void testCopyConstructorCreatesIndependentCopy() {
        EnumFilter<Status> original = new EnumFilter<Status>().setEquals(Status.ACTIVE);
        EnumFilter<Status> copy = new EnumFilter<>(original);

        copy.setEquals(Status.INACTIVE);

        assertThat(original.getEquals()).isEqualTo(Status.ACTIVE);
        assertThat(copy.getEquals()).isEqualTo(Status.INACTIVE);
    }

    // ==================== setEquals Tests ====================

    @Test
    void testSetEqualsWithActiveValue() {
        EnumFilter<Status> filter = new EnumFilter<>();
        filter.setEquals(Status.ACTIVE);

        assertThat(filter.getEquals()).isEqualTo(Status.ACTIVE);
    }

    @Test
    void testSetEqualsWithInactiveValue() {
        EnumFilter<Status> filter = new EnumFilter<>();
        filter.setEquals(Status.INACTIVE);

        assertThat(filter.getEquals()).isEqualTo(Status.INACTIVE);
    }

    @Test
    void testSetEqualsWithPendingValue() {
        EnumFilter<Status> filter = new EnumFilter<>();
        filter.setEquals(Status.PENDING);

        assertThat(filter.getEquals()).isEqualTo(Status.PENDING);
    }

    @Test
    void testSetEqualsWithNullValue() {
        EnumFilter<Status> filter = new EnumFilter<>();
        filter.setEquals(null);

        assertThat(filter.getEquals()).isNull();
    }

    @Test
    void testSetEqualsReturnsFilterForChaining() {
        EnumFilter<Status> filter = new EnumFilter<>();
        EnumFilter<Status> result = filter.setEquals(Status.ACTIVE);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setNotEquals Tests ====================

    @Test
    void testSetNotEqualsWithActiveValue() {
        EnumFilter<Status> filter = new EnumFilter<>();
        filter.setNotEquals(Status.ACTIVE);

        assertThat(filter.getNotEquals()).isEqualTo(Status.ACTIVE);
    }

    @Test
    void testSetNotEqualsWithInactiveValue() {
        EnumFilter<Status> filter = new EnumFilter<>();
        filter.setNotEquals(Status.INACTIVE);

        assertThat(filter.getNotEquals()).isEqualTo(Status.INACTIVE);
    }

    @Test
    void testSetNotEqualsWithNullValue() {
        EnumFilter<Status> filter = new EnumFilter<>();
        filter.setNotEquals(null);

        assertThat(filter.getNotEquals()).isNull();
    }

    @Test
    void testSetNotEqualsReturnsFilterForChaining() {
        EnumFilter<Status> filter = new EnumFilter<>();
        EnumFilter<Status> result = filter.setNotEquals(Status.INACTIVE);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setIn Tests ====================

    @Test
    void testSetInWithMultipleValues() {
        EnumFilter<Status> filter = new EnumFilter<>();
        List<Status> values = Arrays.asList(Status.ACTIVE, Status.PENDING);
        filter.setIn(values);

        assertThat(filter.getIn()).containsExactly(Status.ACTIVE, Status.PENDING);
    }

    @Test
    void testSetInWithSingleValue() {
        EnumFilter<Status> filter = new EnumFilter<>();
        filter.setIn(Arrays.asList(Status.ACTIVE));

        assertThat(filter.getIn()).containsExactly(Status.ACTIVE);
    }

    @Test
    void testSetInWithAllEnumValues() {
        EnumFilter<Status> filter = new EnumFilter<>();
        List<Status> allValues = Arrays.asList(Status.values());
        filter.setIn(allValues);

        assertThat(filter.getIn()).containsExactly(Status.ACTIVE, Status.INACTIVE, Status.PENDING);
    }

    @Test
    void testSetInWithNullValue() {
        EnumFilter<Status> filter = new EnumFilter<>();
        filter.setIn(null);

        assertThat(filter.getIn()).isNull();
    }

    @Test
    void testSetInReturnsFilterForChaining() {
        EnumFilter<Status> filter = new EnumFilter<>();
        EnumFilter<Status> result = filter.setIn(Arrays.asList(Status.ACTIVE));

        assertThat(result).isSameAs(filter);
    }

    // ==================== setNotIn Tests ====================

    @Test
    void testSetNotInWithMultipleValues() {
        EnumFilter<Status> filter = new EnumFilter<>();
        List<Status> values = Arrays.asList(Status.ACTIVE, Status.PENDING);
        filter.setNotIn(values);

        assertThat(filter.getNotIn()).containsExactly(Status.ACTIVE, Status.PENDING);
    }

    @Test
    void testSetNotInWithSingleValue() {
        EnumFilter<Status> filter = new EnumFilter<>();
        filter.setNotIn(Arrays.asList(Status.INACTIVE));

        assertThat(filter.getNotIn()).containsExactly(Status.INACTIVE);
    }

    @Test
    void testSetNotInWithNullValue() {
        EnumFilter<Status> filter = new EnumFilter<>();
        filter.setNotIn(null);

        assertThat(filter.getNotIn()).isNull();
    }

    @Test
    void testSetNotInReturnsFilterForChaining() {
        EnumFilter<Status> filter = new EnumFilter<>();
        EnumFilter<Status> result = filter.setNotIn(Arrays.asList(Status.INACTIVE));

        assertThat(result).isSameAs(filter);
    }

    // ==================== setIsNull Tests ====================

    @Test
    void testSetIsNullWithTrueValue() {
        EnumFilter<Status> filter = new EnumFilter<>();
        filter.setIsNull(true);

        assertThat(filter.getIsNull()).isTrue();
    }

    @Test
    void testSetIsNullWithFalseValue() {
        EnumFilter<Status> filter = new EnumFilter<>();
        filter.setIsNull(false);

        assertThat(filter.getIsNull()).isFalse();
    }

    @Test
    void testSetIsNullWithNullValue() {
        EnumFilter<Status> filter = new EnumFilter<>();
        filter.setIsNull(null);

        assertThat(filter.getIsNull()).isNull();
    }

    @Test
    void testSetIsNullReturnsFilterForChaining() {
        EnumFilter<Status> filter = new EnumFilter<>();
        EnumFilter<Status> result = filter.setIsNull(true);

        assertThat(result).isSameAs(filter);
    }

    // ==================== setIsNotNull Tests ====================

    @Test
    void testSetIsNotNullWithTrueValue() {
        EnumFilter<Status> filter = new EnumFilter<>();
        filter.setIsNotNull(true);

        assertThat(filter.getIsNotNull()).isTrue();
    }

    @Test
    void testSetIsNotNullWithFalseValue() {
        EnumFilter<Status> filter = new EnumFilter<>();
        filter.setIsNotNull(false);

        assertThat(filter.getIsNotNull()).isFalse();
    }

    @Test
    void testSetIsNotNullWithNullValue() {
        EnumFilter<Status> filter = new EnumFilter<>();
        filter.setIsNotNull(null);

        assertThat(filter.getIsNotNull()).isNull();
    }

    @Test
    void testSetIsNotNullReturnsFilterForChaining() {
        EnumFilter<Status> filter = new EnumFilter<>();
        EnumFilter<Status> result = filter.setIsNotNull(true);

        assertThat(result).isSameAs(filter);
    }

    // ==================== Method Chaining Tests ====================

    @Test
    void testMethodChainingAllMethods() {
        EnumFilter<Status> filter = new EnumFilter<Status>()
            .setEquals(Status.ACTIVE)
            .setNotEquals(Status.INACTIVE)
            .setIn(Arrays.asList(Status.ACTIVE, Status.PENDING))
            .setNotIn(Arrays.asList(Status.INACTIVE))
            .setIsNull(false)
            .setIsNotNull(true);

        assertThat(filter.getEquals()).isEqualTo(Status.ACTIVE);
        assertThat(filter.getNotEquals()).isEqualTo(Status.INACTIVE);
        assertThat(filter.getIn()).containsExactly(Status.ACTIVE, Status.PENDING);
        assertThat(filter.getNotIn()).containsExactly(Status.INACTIVE);
        assertThat(filter.getIsNull()).isFalse();
        assertThat(filter.getIsNotNull()).isTrue();
    }

    // ==================== Large Dataset Tests ====================

    @Test
    void testLargeDatasetFilterWithEqualsActive() {
        List<EntityWithEnum> entities = generator.generateEntitiesWithEnum(10_000);

        EnumFilter<Status> statusFilter = new EnumFilter<Status>().setEquals(Status.ACTIVE);
        
        long count = entities.stream()
            .filter(e -> e.getStatus() != null && e.getStatus().equals(Status.ACTIVE))
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(statusFilter.getEquals()).isEqualTo(Status.ACTIVE);
    }

    @Test
    void testLargeDatasetFilterWithEqualsInactive() {
        List<EntityWithEnum> entities = generator.generateEntitiesWithEnum(10_000);

        EnumFilter<Status> statusFilter = new EnumFilter<Status>().setEquals(Status.INACTIVE);
        
        long count = entities.stream()
            .filter(e -> e.getStatus() != null && e.getStatus().equals(Status.INACTIVE))
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(statusFilter.getEquals()).isEqualTo(Status.INACTIVE);
    }

    @Test
    void testLargeDatasetFilterWithEqualsPending() {
        List<EntityWithEnum> entities = generator.generateEntitiesWithEnum(10_000);

        EnumFilter<Status> statusFilter = new EnumFilter<Status>().setEquals(Status.PENDING);
        
        long count = entities.stream()
            .filter(e -> e.getStatus() != null && e.getStatus().equals(Status.PENDING))
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(statusFilter.getEquals()).isEqualTo(Status.PENDING);
    }

    @Test
    void testLargeDatasetFilterWithNotEquals() {
        List<EntityWithEnum> entities = generator.generateEntitiesWithEnum(10_000);

        EnumFilter<Status> statusFilter = new EnumFilter<Status>().setNotEquals(Status.ACTIVE);
        
        long count = entities.stream()
            .filter(e -> e.getStatus() != null && !e.getStatus().equals(Status.ACTIVE))
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(statusFilter.getNotEquals()).isEqualTo(Status.ACTIVE);
    }

    @Test
    void testLargeDatasetFilterWithInList() {
        List<EntityWithEnum> entities = generator.generateEntitiesWithEnum(10_000);
        List<Status> targetStatuses = Arrays.asList(Status.ACTIVE, Status.PENDING);

        EnumFilter<Status> statusFilter = new EnumFilter<Status>().setIn(targetStatuses);
        
        long count = entities.stream()
            .filter(e -> e.getStatus() != null && targetStatuses.contains(e.getStatus()))
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(statusFilter.getIn()).containsExactlyElementsOf(targetStatuses);
    }

    @Test
    void testLargeDatasetFilterWithAllEnumValues() {
        List<EntityWithEnum> entities = generator.generateEntitiesWithEnum(10_000);
        List<Status> allStatuses = Arrays.asList(Status.values());

        EnumFilter<Status> statusFilter = new EnumFilter<Status>().setIn(allStatuses);
        
        long count = entities.stream()
            .filter(e -> e.getStatus() != null && allStatuses.contains(e.getStatus()))
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(statusFilter.getIn()).containsExactly(Status.ACTIVE, Status.INACTIVE, Status.PENDING);
    }

    @Test
    void testLargeDatasetFilterWithNotInList() {
        List<EntityWithEnum> entities = generator.generateEntitiesWithEnum(10_000);
        List<Status> excludeStatuses = Arrays.asList(Status.INACTIVE);

        EnumFilter<Status> statusFilter = new EnumFilter<Status>().setNotIn(excludeStatuses);
        
        long count = entities.stream()
            .filter(e -> e.getStatus() != null && !excludeStatuses.contains(e.getStatus()))
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(statusFilter.getNotIn()).containsExactlyElementsOf(excludeStatuses);
    }

    @Test
    void testLargeDatasetFilterWithIsNull() {
        List<EntityWithEnum> entities = generator.generateEntitiesWithEnum(10_000);

        EnumFilter<Status> statusFilter = new EnumFilter<Status>().setIsNull(true);
        
        entities.stream()
            .filter(e -> e.getStatus() == null)
            .count();

        assertThat(statusFilter.getIsNull()).isTrue();
    }

    @Test
    void testLargeDatasetFilterWithIsNotNull() {
        List<EntityWithEnum> entities = generator.generateEntitiesWithEnum(10_000);

        EnumFilter<Status> statusFilter = new EnumFilter<Status>().setIsNotNull(true);
        
        long count = entities.stream()
            .filter(e -> e.getStatus() != null)
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(statusFilter.getIsNotNull()).isTrue();
    }

    @Test
    void testLargeDatasetFilterPerformance() {
        List<EntityWithEnum> entities = generator.generateEntitiesWithEnum(10_000);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 1000; i++) {
            new EnumFilter<Status>()
                .setEquals(Status.ACTIVE)
                .setIsNotNull(true);
            
            entities.stream()
                .filter(e -> e.getStatus() != null && e.getStatus().equals(Status.ACTIVE))
                .count();
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        assertThat(duration).isLessThan(5000);
    }

    @Test
    void testLargeDatasetComplexFilterCombination() {
        List<EntityWithEnum> entities = generator.generateEntitiesWithEnum(10_000);

        EnumFilter<Status> statusFilter = new EnumFilter<Status>()
            .setNotEquals(Status.INACTIVE)
            .setIsNotNull(true)
            .setIn(Arrays.asList(Status.ACTIVE, Status.PENDING));
        
        long count = entities.stream()
            .filter(e -> e.getStatus() != null && 
                        !e.getStatus().equals(Status.INACTIVE) &&
                        (e.getStatus().equals(Status.ACTIVE) || e.getStatus().equals(Status.PENDING)))
            .count();

        assertThat(count).isGreaterThan(0);
        assertThat(statusFilter.getNotEquals()).isEqualTo(Status.INACTIVE);
        assertThat(statusFilter.getIsNotNull()).isTrue();
        assertThat(statusFilter.getIn()).containsExactly(Status.ACTIVE, Status.PENDING);
    }
}
