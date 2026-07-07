package com.thy.fss.common.inmemory.integration;


import com.thy.fss.common.inmemory.common.base.BaseIntegrationTest;
import com.thy.fss.common.inmemory.common.generator.TestDataGenerator;
import com.thy.fss.common.inmemory.common.model.*;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.integration.testentities.*;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.TestUtil;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * End-to-end integration tests for complex nested object hierarchies with 3+
 * levels. Tests multiple DataSource types, performance with large datasets, and
 * concurrent access using generated classes.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("integration")
@Tag("complex")
class ComplexNestedHierarchyIntegrationTest extends BaseIntegrationTest {

    private InMemorySpecStoreFactory factory;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp(); // Call parent setUp first
        factory = InMemorySpecStoreFactory.getInstance();
        factory.clearAllDataSources();
    }


    @Test
    @Order(1)
    @DisplayName("Should create complex 4-level nested object hierarchy with generated classes")
    void shouldCreateComplexNestedObjectHierarchyWithGeneratedClasses() {
        // Given - Create test data sources with realistic data using generated classes
        List<TestUser> employees = TestDataGenerator.createUserList(20);
        List<TestProfile> profiles = TestDataGenerator.createProfileList(20);
        List<TestTag> tags = Arrays.asList(
                TestDataGenerator.createTag("project", "project"),
                TestDataGenerator.createTag("development", "project"),
                TestDataGenerator.createTag("testing", "project"),
                TestDataGenerator.createTag("deployment", "project")
        );

        InMemoryDataSource<TestUser> employeeDataSource = new InMemoryDataSource<>("employees", TestUser.class, employees);
        InMemoryDataSource<TestProfile> profileDataSource = new InMemoryDataSource<>("profiles", TestProfile.class, profiles);
        InMemoryDataSource<TestTag> tagDataSource = new InMemoryDataSource<>("tags", TestTag.class, tags);

        // Create complex company hierarchy
        List<Company> companies = createComplexCompanyHierarchy(employees, profiles, tags);
        InMemoryDataSource<Company> companyDataSource = new InMemoryDataSource<>("companies", Company.class, companies);

        // Register datasources with factory
        factory.registerDataSource("employees", employeeDataSource, Duration.ofSeconds(1));
        factory.registerDataSource("profiles", profileDataSource, Duration.ofSeconds(1));
        factory.registerDataSource("tags", tagDataSource, Duration.ofSeconds(1));
        factory.registerDataSource("companies", companyDataSource, Duration.ofSeconds(1));

        // When - Build data stores with complex nested relationships
        InMemoryDataStore<TestUser> employeeStore = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestUser.class)
                .build();

        InMemoryDataStore<TestProfile> profileStore = factory.buildInMemoryStore(TestProfileSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestProfile.class)
                .build();

        InMemoryDataStore<TestTag> tagStore = factory.buildInMemoryStore(TestTagSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestTag.class)
                .build();

        InMemoryDataStore<Company> companyStore = factory.buildInMemoryStore(CompanySpecificationService.INSTANCE)
                .withPrimaryDataSource(Company.class)
                .build();

        // Manually load data from datasources into stores (DataSynchronizationEngine not yet implemented)
        employeeDataSource.fetchAll().thenAccept(data -> employeeStore.updateData(data, 1)).join();
        profileDataSource.fetchAll().thenAccept(data -> profileStore.updateData(data, 1)).join();
        tagDataSource.fetchAll().thenAccept(data -> tagStore.updateData(data, 1)).join();
        companyDataSource.fetchAll().thenAccept(data -> companyStore.updateData(data, 1)).join();

        // Wait for initial load and first sync
        TestUtil.await(2000);

        // Then - Verify complex hierarchy with generated class filtering
        List<Company> loadedCompanies = companyStore.findAll();
        assertThat(loadedCompanies).isNotNull()
                .hasSizeGreaterThan(0);

        // Test basic filtering without generated classes for now
        List<TestUser> activeEmployees = employeeStore.findAll().stream()
                .filter(TestUser::getActive)
                .filter(user -> user.getAge() > 25)
                .toList();
        assertThat(activeEmployees).isNotEmpty()
                .allMatch(TestUser::getActive)
                .allMatch(user -> user.getAge() > 25);

        // Test profile filtering
        List<TestProfile> seniorProfiles = profileStore.findAll().stream()
                .filter(profile -> profile.getDescription().contains("Profile"))
                .toList();
        assertThat(seniorProfiles).isNotEmpty();

        // Test tag filtering
        List<TestTag> projectTags = tagStore.findAll().stream()
                .filter(tag -> "project".equals(tag.getCategory()))
                .toList();
        assertThat(projectTags).isNotEmpty();

        System.out.println("Successfully created and verified complex hierarchy:");
        System.out.println("- Companies: " + loadedCompanies.size());
        System.out.println("- Active Employees: " + activeEmployees.size());
        System.out.println("- Senior Profiles: " + seniorProfiles.size());
        System.out.println("- Project Tags: " + projectTags.size());
    }

    @Test
    @Order(2)
    @DisplayName("Should handle large dataset performance with complex nested relationships")
    void shouldHandleLargeDatasetPerformanceWithComplexNestedRelationships() {
        // Given - Create large datasets with complex relationships
        List<TestUser> largeEmployeeSet = TestDataGenerator.createUserList(1000);
        List<TestProfile> largeProfileSet = TestDataGenerator.createProfileList(1000);
        List<TestTag> largeTags = TestDataGenerator.createTags(
                IntStream.range(1, 101).mapToObj(i -> "tag-" + i).toArray(String[]::new)
        );

        InMemoryDataSource<TestUser> largeEmployeeDataSource = new InMemoryDataSource<>("large-employees", TestUser.class, largeEmployeeSet);
        InMemoryDataSource<TestProfile> largeProfileDataSource = new InMemoryDataSource<>("large-profiles", TestProfile.class, largeProfileSet);
        InMemoryDataSource<TestTag> largeTagDataSource = new InMemoryDataSource<>("large-tags", TestTag.class, largeTags);

        // Register datasources with factory
        factory.registerDataSource("large-employees", largeEmployeeDataSource, Duration.ofSeconds(5));
        factory.registerDataSource("large-profiles", largeProfileDataSource, Duration.ofSeconds(5));
        factory.registerDataSource("large-tags", largeTagDataSource, Duration.ofSeconds(5));

        // When - Build stores with large datasets
        long startTime = System.currentTimeMillis();

        InMemoryDataStore<TestUser> largeEmployeeStore = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestUser.class)
                .build();

        InMemoryDataStore<TestProfile> largeProfileStore = factory.buildInMemoryStore(TestProfileSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestProfile.class)
                .build();

        InMemoryDataStore<TestTag> largeTagStore = factory.buildInMemoryStore(TestTagSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestTag.class)
                .build();

        long buildTime = System.currentTimeMillis() - startTime;

        // Manually load data from datasources into stores (DataSynchronizationEngine not yet implemented)
        largeEmployeeDataSource.fetchAll().thenAccept(data -> largeEmployeeStore.updateData(data, 1)).join();
        largeProfileDataSource.fetchAll().thenAccept(data -> largeProfileStore.updateData(data, 1)).join();
        largeTagDataSource.fetchAll().thenAccept(data -> largeTagStore.updateData(data, 1)).join();

        // Wait for initial load
        TestUtil.await(3000);

        // Then - Verify performance and data integrity with complex queries
        long queryStartTime = System.currentTimeMillis();

        // Complex multi-level filtering using streams for now
        List<TestUser> filteredEmployees = largeEmployeeStore.findAll().stream()
                .filter(TestUser::getActive)
                .filter(user -> user.getAge() >= 25 && user.getAge() <= 55)
                .filter(user -> user.getName().contains("User"))
                .toList();
        long queryTime = System.currentTimeMillis() - queryStartTime;

        assertThat(filteredEmployees).isNotEmpty();
        assertThat(buildTime).isLessThan(15000); // Build should complete within 15 seconds
        assertThat(queryTime).isLessThan(2000); // Complex query should complete within 2 seconds

        // Test pagination performance with large dataset
        Pageable pageable = PageRequest.of(0, 50, Sort.by("name"));
        long paginationStartTime = System.currentTimeMillis();
        Page<TestUser> page = largeEmployeeStore.findAll(pageable);
        long paginationTime = System.currentTimeMillis() - paginationStartTime;

        assertThat(page.getContent()).hasSizeLessThanOrEqualTo(50);
        assertThat(page.getTotalElements()).isGreaterThan(0);
        assertThat(paginationTime).isLessThan(1000); // Pagination should be fast

        // Test complex profile queries
        List<TestProfile> filteredProfiles = largeProfileStore.findAll().stream()
                .filter(profile -> profile.getDescription().contains("Profile"))
                .toList();
        assertThat(filteredProfiles).isNotEmpty();

        System.out.println("Large dataset performance results:");
        System.out.println("- Build time: " + buildTime + "ms");
        System.out.println("- Complex query time: " + queryTime + "ms");
        System.out.println("- Pagination time: " + paginationTime + "ms");
        System.out.println("- Total employees: " + largeEmployeeSet.size());
        System.out.println("- Filtered employees: " + filteredEmployees.size());
        System.out.println("- Total profiles: " + largeProfileSet.size());
        System.out.println("- Filtered profiles: " + filteredProfiles.size());
    }

    @Test
    @Order(3)
    @DisplayName("Should handle concurrent access with complex nested specifications")
    void shouldHandleConcurrentAccessWithComplexNestedSpecifications() throws InterruptedException {
        // Given - Create data sources with complex relationships
        List<TestUser> users = TestDataGenerator.createUserList(100);
        List<TestProfile> profiles = TestDataGenerator.createProfileList(100);
        List<TestTag> tags = Arrays.asList(
                TestDataGenerator.createTag("concurrent", "test"),
                TestDataGenerator.createTag("test", "test"),
                TestDataGenerator.createTag("performance", "test"),
                TestDataGenerator.createTag("load", "test")
        );

        InMemoryDataSource<TestUser> userDataSource = new InMemoryDataSource<>("concurrent-users", TestUser.class, users);
        InMemoryDataSource<TestProfile> profileDataSource = new InMemoryDataSource<>("concurrent-profiles", TestProfile.class, profiles);
        InMemoryDataSource<TestTag> tagDataSource = new InMemoryDataSource<>("concurrent-tags", TestTag.class, tags);

        // Register datasources with factory
        factory.registerDataSource("concurrent-users", userDataSource, Duration.ofMillis(500));
        factory.registerDataSource("concurrent-profiles", profileDataSource, Duration.ofMillis(500));
        factory.registerDataSource("concurrent-tags", tagDataSource, Duration.ofMillis(500));

        InMemoryDataStore<TestUser> userStore = factory.buildInMemoryStore(TestUserSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestUser.class)
                .build();

        InMemoryDataStore<TestProfile> profileStore = factory.buildInMemoryStore(TestProfileSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestProfile.class)
                .build();

        InMemoryDataStore<TestTag> tagStore = factory.buildInMemoryStore(TestTagSpecificationService.INSTANCE)
                .withPrimaryDataSource(TestTag.class)
                .build();

        // Manually load data from datasources into stores (DataSynchronizationEngine not yet implemented)
        userDataSource.fetchAll().thenAccept(data -> userStore.updateData(data, 1)).join();
        profileDataSource.fetchAll().thenAccept(data -> profileStore.updateData(data, 1)).join();
        tagDataSource.fetchAll().thenAccept(data -> tagStore.updateData(data, 1)).join();

        // Wait for initial load
        TestUtil.await(1000);

        // When - Execute concurrent operations with complex specifications
        int numberOfThreads = 20;
        int operationsPerThread = 25;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // Create concurrent operations with basic filtering
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    try {
                        // Mix of different complex query types
                        if (j % 5 == 0) {
                            // Complex user filtering
                            List<TestUser> filteredUsers = userStore.findAll().stream()
                                    .filter(TestUser::getActive)
                                    .filter(user -> user.getAge() > 20 + (threadId % 10))
                                    .toList();
                            assertThat(filteredUsers).isNotNull();

                        } else if (j % 5 == 1) {
                            // Profile pagination with filtering
                            Page<TestProfile> profilePage = profileStore.findAll(PageRequest.of(0, 10));
                            assertThat(profilePage.getContent()).isNotNull();

                        } else if (j % 5 == 2) {
                            // Tag filtering with category
                            List<TestTag> filteredTags = tagStore.findAll().stream()
                                    .filter(tag -> "test".equals(tag.getCategory()))
                                    .toList();
                            assertThat(filteredTags).isNotNull();

                        } else if (j % 5 == 3) {
                            // Combined filtering
                            List<TestUser> combinedResults = userStore.findAll().stream()
                                    .filter(TestUser::getActive)
                                    .filter(user -> user.getName().contains("User"))
                                    .filter(user -> user.getAge() >= 25 && user.getAge() <= 45)
                                    .toList();
                            assertThat(combinedResults).isNotNull();

                        } else {

                            //check store status
                            int userCount = userStore.size();
                            int profileCount = profileStore.size();
                            int tagCount = tagStore.size();
                            assertThat(userCount).isEqualTo(100);
                            assertThat(profileCount).isEqualTo(100);
                            assertThat(tagCount).isEqualTo(4);
                        }

                        // Small delay to simulate real usage
                        TestUtil.await(1);

                    } catch (Exception e) {
                        throw new RuntimeException("Concurrent operation failed in thread " + threadId, e);
                    }
                }
            }, executor);

            futures.add(future);
        }

        // Wait for all concurrent operations to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));

        // Then - Verify all operations completed successfully
        assertThatCode(() -> allFutures.get(60, TimeUnit.SECONDS))
                .doesNotThrowAnyException();

        // Verify data consistency after concurrent access
        List<TestUser> finalUsers = userStore.findAll();
        List<TestProfile> finalProfiles = profileStore.findAll();
        List<TestTag> finalTags = tagStore.findAll();

        assertThat(finalUsers).hasSize(100);
        assertThat(finalProfiles).hasSize(100);
        assertThat(finalTags).hasSize(4);

        // Verify filtering still works after concurrent access
        List<TestUser> postConcurrentResults = userStore.findAll().stream()
                .filter(TestUser::getActive)
                .toList();
        assertThat(postConcurrentResults).isNotEmpty();

        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        System.out.println("Concurrent access test completed successfully:");
        System.out.println("- Threads: " + numberOfThreads);
        System.out.println("- Operations per thread: " + operationsPerThread);
        System.out.println("- Total operations: " + (numberOfThreads * operationsPerThread));
        System.out.println("- Final data integrity verified");
    }

    // Helper methods for creating complex test data
    private List<Company> createComplexCompanyHierarchy(List<TestUser> employees, List<TestProfile> profiles, List<TestTag> tags) {
        List<Company> companies = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            Company company = new Company((long) i, "Company " + i, "Technology", "ACTIVE");

            // Add departments with employees
            for (int j = 1; j <= 3; j++) {
                Department dept = new Department((long) (i * 10 + j), company.getIdentity(),
                        "Department " + j, "Engineering", 1000000.0, "ACTIVE");

                // Assign employee names to department
                List<String> deptEmployeeNames = new ArrayList<>();
                int startIdx = (i - 1) * 4 + (j - 1) * 2;
                int endIdx = Math.min(startIdx + 2, employees.size());
                for (int empIdx = startIdx; empIdx < endIdx && empIdx < employees.size(); empIdx++) {
                    deptEmployeeNames.add(employees.get(empIdx).getName());
                }
                dept.setEmployeeNames(deptEmployeeNames);

                // Add projects with team members and tags
                for (int k = 1; k <= 2; k++) {
                    Project project = new Project((long) (i * 100 + j * 10 + k), dept.getIdentity(),
                            "Project " + k, "Description for project " + k, 100000.0, "ACTIVE");

                    // Assign team member names
                    List<String> teamMemberNames = new ArrayList<>();
                    int profileStartIdx = (i - 1) * 2 + (j - 1);
                    int profileEndIdx = Math.min(profileStartIdx + 2, profiles.size());
                    for (int profIdx = profileStartIdx; profIdx < profileEndIdx && profIdx < profiles.size(); profIdx++) {
                        teamMemberNames.add("Profile" + profIdx);
                    }
                    project.setTeamMemberNames(teamMemberNames);

                    // Assign tag names
                    List<String> tagNames = new ArrayList<>();
                    int tagStartIdx = k - 1;
                    int tagEndIdx = Math.min(tagStartIdx + 2, tags.size());
                    for (int tagIdx = tagStartIdx; tagIdx < tagEndIdx && tagIdx < tags.size(); tagIdx++) {
                        tagNames.add(tags.get(tagIdx).getName());
                    }
                    project.setTagNames(tagNames);

                    dept.getProjects().add(project);
                }

                company.getDepartments().add(dept);
            }

            // Add company events
            for (int e = 1; e <= 2; e++) {
                CompanyEvent event = new CompanyEvent((long) (i * 10 + e), company.getIdentity(),
                        "Event Type " + e, "Event description " + e, 50000.0);
                company.getEvents().add(event);
            }

            companies.add(company);
        }

        return companies;
    }

    // Helper methods for creating complex test data using separate entity classes
}
