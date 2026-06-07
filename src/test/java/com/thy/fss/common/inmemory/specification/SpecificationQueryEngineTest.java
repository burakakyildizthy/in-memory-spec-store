package com.thy.fss.common.inmemory.specification;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.common.LargeDatasetGenerator;
import com.thy.fss.common.inmemory.filter.BooleanFilter;
import com.thy.fss.common.inmemory.filter.StringFilter;
import com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder;
import com.thy.fss.common.inmemory.testmodel.TestUser;
import com.thy.fss.common.inmemory.testmodel.TestUserFilter;
import com.thy.fss.common.inmemory.testmodel.TestUserSpecificationService;
import com.thy.fss.common.inmemory.testmodel.TestUser_;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Comprehensive tests for SpecificationQueryEngine functionality.
 * Tests all public methods with various scenarios including large datasets.
 */
class SpecificationQueryEngineTest {

    // String literals used multiple times
    private static final String USER_1 = "User1";
    private static final String USER_2 = "User2";
    private static final String DATA_CANNOT_BE_NULL = "Data cannot be null";
    private static final String NAME_FIELD = "name";
    
    private SpecificationQueryEngine<TestUser> queryEngine;
    private LargeDatasetGenerator dataGenerator;

    @BeforeEach
    void setUp() {
        queryEngine = new SpecificationQueryEngine<>(TestUser.class);
        dataGenerator = LargeDatasetGenerator.create();
    }

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    // ==================== Constructor Tests (Task 3.1) ====================

    @Test
    void testConstructorWithValidEntityClass() {
        SpecificationQueryEngine<TestUser> engine = new SpecificationQueryEngine<>(TestUser.class);
        
        assertThat(engine).isNotNull();
    }

    @Test
    void testConstructorWithNullEntityClassThrowsException() {
        assertThatThrownBy(() -> new SpecificationQueryEngine<>(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void testConstructorInitializesSpecificationService() {
        SpecificationQueryEngine<TestUser> engine = new SpecificationQueryEngine<>(TestUser.class);
        
        assertThat(engine).isNotNull();
        // Verify service is available via direct INSTANCE reference
        Assertions.assertThat(TestUserSpecificationService.INSTANCE).isNotNull();
    }

    // ==================== Query with Specification Tests (Task 3.2) ====================
    @Test
    void testQueryWithEmptyDataReturnsEmptyList() {
        Specification<TestUser> spec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
                .where(TestUser_.name).contains("John");
        
        List<TestUser> result = queryEngine.query(List.of(), spec);
        
        assertThat(result).isEmpty();
    }

    @Test
    void testQueryWithNullSpecificationReturnsAllData() {
        List<TestUser> data = dataGenerator.generateTestUsers(1000);
        
        List<TestUser> result = queryEngine.query(data, null);
        
        assertThat(result).hasSize(1000);
    }

    @Test
    void testQueryWithComplexSpecificationsAND() {
        List<TestUser> data = dataGenerator.generateTestUsers(10000);
        Specification<TestUser> spec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
                .where(TestUser_.name).contains(USER_1)
                .and(SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
                        .where(TestUser_.active).equalTo(true));
        
        List<TestUser> result = queryEngine.query(data, spec);
        
        assertThat(result).isNotEmpty().allMatch(u -> u.getName().contains(USER_1) && Boolean.TRUE.equals(u.getActive()));
    }

    @Test
    void testQueryWithComplexSpecificationsOR() {
        List<TestUser> data = dataGenerator.generateTestUsers(10000);
        Specification<TestUser> spec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
                .where(TestUser_.name).contains(USER_1)
                .or(SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
                        .where(TestUser_.name).contains(USER_2));
        
        List<TestUser> result = queryEngine.query(data, spec);
        
        assertThat(result).isNotEmpty().allMatch(u -> u.getName().contains(USER_1) || u.getName().contains(USER_2));
    }

    @Test
    void testQueryWithComplexSpecificationsNOT() {
        List<TestUser> data = dataGenerator.generateTestUsers(10000);
        Specification<TestUser> spec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
                .where(TestUser_.active).equalTo(true)
                .not();
        
        List<TestUser> result = queryEngine.query(data, spec);
        
        assertThat(result).isNotEmpty().allMatch(u -> !Boolean.TRUE.equals(u.getActive()));
    }

    @Test
    void testQueryWithLargeDataset100K() {
        List<TestUser> data = dataGenerator.generateTestUsers(100000);
        Specification<TestUser> spec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
                .where(TestUser_.name).contains(USER_1);
        
        long startTime = System.currentTimeMillis();
        List<TestUser> result = queryEngine.query(data, spec);
        long duration = System.currentTimeMillis() - startTime;
        
        assertThat(result).isNotEmpty();
        assertThat(duration).isLessThan(1000);
    }

    @Test
    void testCountWithSpecification() {
        List<TestUser> data = dataGenerator.generateTestUsers(10000);
        Specification<TestUser> spec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
                .where(TestUser_.active).equalTo(true);
        
        long count = queryEngine.count(data, spec);
        
        assertThat(count).isGreaterThan(0);
    }

    @Test
    void testCountWithNullDataThrowsException() {
        Specification<TestUser> spec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
                .where(TestUser_.active).equalTo(true);

        assertThrows(IllegalArgumentException.class,
                () -> queryEngine.count(null, spec));
    }

    @Test
    void testCountWithEmptyDataReturnsZero() {
        Specification<TestUser> spec = SpecificationBuilder.forService(TestUserSpecificationService.INSTANCE)
                .where(TestUser_.active).equalTo(true);
        
        long count = queryEngine.count(List.of(), spec);
        
        assertThat(count).isZero();
    }

    // ==================== Pagination Tests (Task 3.3) ====================

    @Test
    void testQueryWithFirstPage() {
        List<TestUser> data = dataGenerator.generateTestUsers(10000);
        PageRequest pageable = PageRequest.of(0, 100);
        
        Page<TestUser> page = queryEngine.query(data, null, pageable);
        
        assertThat(page.getContent()).hasSize(100);
        assertThat(page.getTotalElements()).isEqualTo(10000);
        assertThat(page.getNumber()).isZero();
        assertThat(page.isFirst()).isTrue();
    }

    @Test
    void testQueryWithMiddlePage() {
        List<TestUser> data = dataGenerator.generateTestUsers(10000);
        PageRequest pageable = PageRequest.of(50, 100);
        
        Page<TestUser> page = queryEngine.query(data, null, pageable);
        
        assertThat(page.getContent()).hasSize(100);
        assertThat(page.getTotalElements()).isEqualTo(10000);
        assertThat(page.getNumber()).isEqualTo(50);
        assertThat(page.isFirst()).isFalse();
        assertThat(page.isLast()).isFalse();
    }

    @Test
    void testQueryWithLastPage() {
        List<TestUser> data = dataGenerator.generateTestUsers(10000);
        PageRequest pageable = PageRequest.of(99, 100);
        
        Page<TestUser> page = queryEngine.query(data, null, pageable);
        
        assertThat(page.getContent()).hasSize(100);
        assertThat(page.getTotalElements()).isEqualTo(10000);
        assertThat(page.getNumber()).isEqualTo(99);
        assertThat(page.isLast()).isTrue();
    }

    @Test
    void testQueryWithOutOfBoundsPageReturnsEmptyPage() {
        List<TestUser> data = dataGenerator.generateTestUsers(1000);
        PageRequest pageable = PageRequest.of(100, 100);
        
        Page<TestUser> page = queryEngine.query(data, null, pageable);
        
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(1000);
        assertThat(page.getNumber()).isEqualTo(100);
    }

    @ParameterizedTest
    @CsvSource({
            "10, 1000",
            "50, 200",
            "100, 100",
            "1000, 10"
    })
    void testQueryWithVariousPageSizes(int pageSize, int expectedTotalPages) {
        List<TestUser> data = dataGenerator.generateTestUsers(10000);
        PageRequest pageable = PageRequest.of(0, pageSize);

        Page<TestUser> page = queryEngine.query(data, null, pageable);

        assertThat(page.getContent()).hasSize(pageSize);
        assertThat(page.getTotalPages()).isEqualTo(expectedTotalPages);
    }


    @Test
    void testQueryWithLargeDatasetPagination100K() {
        List<TestUser> data = dataGenerator.generateTestUsers(100000);
        PageRequest pageable = PageRequest.of(0, 100);
        
        long startTime = System.currentTimeMillis();
        Page<TestUser> page = queryEngine.query(data, null, pageable);
        long duration = System.currentTimeMillis() - startTime;
        
        assertThat(page.getContent()).hasSize(100);
        assertThat(page.getTotalElements()).isEqualTo(100000);
        assertThat(duration).isLessThan(500);
    }

    // ==================== Sorting Tests (Task 3.4) ====================

    @Test
    void testQueryWithSingleFieldSortingAscending() {
        List<TestUser> data = dataGenerator.generateTestUsers(10000);
        PageRequest pageable = PageRequest.of(0, 100, Sort.by(NAME_FIELD).ascending());
        
        Page<TestUser> page = queryEngine.query(data, null, pageable);
        
        assertThat(page.getContent()).hasSize(100);
        assertThat(page.getContent()).isSortedAccordingTo((u1, u2) -> u1.getName().compareTo(u2.getName()));
    }

    @Test
    void testQueryWithSingleFieldSortingDescending() {
        List<TestUser> data = dataGenerator.generateTestUsers(10000);
        PageRequest pageable = PageRequest.of(0, 100, Sort.by(NAME_FIELD).descending());
        
        Page<TestUser> page = queryEngine.query(data, null, pageable);
        
        assertThat(page.getContent()).hasSize(100);
        assertThat(page.getContent()).isSortedAccordingTo((u1, u2) -> u2.getName().compareTo(u1.getName()));
    }

    @Test
    void testQueryWithMultiFieldSorting() {
        List<TestUser> data = dataGenerator.generateTestUsers(10000);
        PageRequest pageable = PageRequest.of(0, 100, 
                Sort.by("active").descending().and(Sort.by(NAME_FIELD).ascending()));
        
        Page<TestUser> page = queryEngine.query(data, null, pageable);
        
        assertThat(page.getContent()).hasSize(100);
    }

    @Test
    void testQueryWithSortingLargeDataset100K() {
        List<TestUser> data = dataGenerator.generateTestUsers(100000);
        PageRequest pageable = PageRequest.of(0, 100, Sort.by(NAME_FIELD).ascending());
        
        long startTime = System.currentTimeMillis();
        Page<TestUser> page = queryEngine.query(data, null, pageable);
        long duration = System.currentTimeMillis() - startTime;
        
        assertThat(page.getContent()).hasSize(100);
        assertThat(duration).isLessThan(1000);
    }

    // ==================== QueryByFilter Tests (Task 3.5) ====================

    @Test
    void testQueryByFilterWithNullDataThrowsException() {
        TestUserFilter filter = new TestUserFilter();
        PageRequest pageable = PageRequest.of(0, 10);
        
        assertThatThrownBy(() -> queryEngine.queryByFilter(null, filter, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(DATA_CANNOT_BE_NULL);
    }

    @Test
    void testQueryByFilterWithEmptyDataReturnsEmptyPage() {
        TestUserFilter filter = new TestUserFilter();
        PageRequest pageable = PageRequest.of(0, 10);
        
        Page<TestUser> page = queryEngine.queryByFilter(List.of(), filter, pageable);
        
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void testQueryByFilterWithNullFilterReturnsAllData() {
        List<TestUser> data = dataGenerator.generateTestUsers(1000);
        PageRequest pageable = PageRequest.of(0, 100);
        
        Page<TestUser> page = queryEngine.queryByFilter(data, null, pageable);
        
        assertThat(page.getContent()).hasSize(100);
        assertThat(page.getTotalElements()).isEqualTo(1000);
    }

    @Test
    void testQueryByFilterWithComplexFilters() {
        List<TestUser> data = dataGenerator.generateTestUsers(10000);
        
        TestUserFilter filter = new TestUserFilter();
        filter.setName(new StringFilter().setContains(USER_1));
        filter.setActive(new BooleanFilter().setEquals(true));
        
        PageRequest pageable = PageRequest.of(0, 100);
        
        Page<TestUser> page = queryEngine.queryByFilter(data, filter, pageable);
        
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent()).allMatch(u -> 
                u.getName().contains(USER_1) && Boolean.TRUE.equals(u.getActive()));
    }

    @Test
    void testQueryByFilterWithPagination() {
        List<TestUser> data = dataGenerator.generateTestUsers(10000);
        
        TestUserFilter filter = new TestUserFilter();
        filter.setActive(new BooleanFilter().setEquals(true));
        
        PageRequest page1Request = PageRequest.of(0, 100);
        PageRequest page2Request = PageRequest.of(1, 100);
        
        Page<TestUser> page1 = queryEngine.queryByFilter(data, filter, page1Request);
        Page<TestUser> page2 = queryEngine.queryByFilter(data, filter, page2Request);
        
        assertThat(page1.getContent()).hasSize(100);
        assertThat(page2.getContent()).hasSize(100);
        assertThat(page1.getContent()).doesNotContainAnyElementsOf(page2.getContent());
    }

    @Test
    void testCountByFilter() {
        List<TestUser> data = dataGenerator.generateTestUsers(10000);
        
        TestUserFilter filter = new TestUserFilter();
        filter.setActive(new BooleanFilter().setEquals(true));
        
        long count = queryEngine.countByFilter(data, filter);
        
        assertThat(count).isGreaterThan(0);
    }

    @Test
    void testCountByFilterWithNullDataThrowsException() {
        TestUserFilter filter = new TestUserFilter();
        
        assertThatThrownBy(() -> queryEngine.countByFilter(null, filter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(DATA_CANNOT_BE_NULL);
    }

    @Test
    void testCountByFilterWithEmptyDataReturnsZero() {
        TestUserFilter filter = new TestUserFilter();
        
        long count = queryEngine.countByFilter(List.of(), filter);
        
        assertThat(count).isZero();
    }

    @Test
    void testQueryByFilterWithLargeDataset100K() {
        List<TestUser> data = dataGenerator.generateTestUsers(100000);
        
        TestUserFilter filter = new TestUserFilter();
        filter.setName(new StringFilter().setContains(USER_1));
        
        PageRequest pageable = PageRequest.of(0, 100);
        
        long startTime = System.currentTimeMillis();
        Page<TestUser> page = queryEngine.queryByFilter(data, filter, pageable);
        long duration = System.currentTimeMillis() - startTime;
        
        assertThat(page.getContent()).isNotEmpty();
        assertThat(duration).isLessThan(1000);
    }

    @Test
    void testQueryByFilterWithoutPagination() {
        List<TestUser> data = dataGenerator.generateTestUsers(10000);
        
        TestUserFilter filter = new TestUserFilter();
        filter.setActive(new BooleanFilter().setEquals(true));
        
        List<TestUser> result = queryEngine.queryByFilter(data, filter);
        
        assertThat(result).isNotEmpty().allMatch(u -> Boolean.TRUE.equals(u.getActive()));
    }

    @Test
    void testQueryByFilterWithoutPaginationNullDataThrowsException() {
        TestUserFilter filter = new TestUserFilter();
        
        assertThatThrownBy(() -> queryEngine.queryByFilter(null, filter))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(DATA_CANNOT_BE_NULL);
    }

    @Test
    void testQueryByFilterWithoutPaginationEmptyDataReturnsEmptyList() {
        TestUserFilter filter = new TestUserFilter();
        
        List<TestUser> result = queryEngine.queryByFilter(List.of(), filter);
        
        assertThat(result).isEmpty();
    }
}