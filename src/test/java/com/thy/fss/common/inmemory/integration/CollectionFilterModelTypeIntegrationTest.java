package com.thy.fss.common.inmemory.integration;

import com.thy.fss.common.inmemory.filter.CollectionFilter;
import com.thy.fss.common.inmemory.filter.StringFilter;
import com.thy.fss.common.inmemory.filter.BooleanFilter;
import com.thy.fss.common.inmemory.filter.IntegerFilter;
import com.thy.fss.common.inmemory.integration.testentities.*;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for collection filter model type support.
 * Tests end-to-end functionality including:
 * - Single-level model type filtering (users.any.name.eq=John)
 * - Multi-level nested paths (users.any.address.city.eq=Istanbul)
 * - Multiple operators (users.any.name.eq=John&users.all.active.eq=true)
 * - Specification service validation with model types
 * 
 * Validates Requirements: 1.1, 1.2, 1.3, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3
 */
@DisplayName("Collection Filter Model Type Integration Tests")
class CollectionFilterModelTypeIntegrationTest {

    private static final String JOHN = "John";
    private static final String JANE = "Jane";
    private static final String ALICE = "Alice";
    private static final String USER_ROLE = "USER";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String JOHN_EMAIL = "john@example.com";
    private static final String JANE_EMAIL = "jane@example.com";
    private static final String ISTANBUL = "Istanbul";

    private SpecificationService<IntegrationTestCompany> companyService;
    private SpecificationService<IntegrationTestUser> userService;
    
    @BeforeEach
    void setUp() {
        // Get specification services
        companyService = IntegrationTestCompanySpecificationService.INSTANCE;
        userService = IntegrationTestUserSpecificationService.INSTANCE;
        
        assertNotNull(companyService, "Company specification service should be available");
        assertNotNull(userService, "User specification service should be available");
    }

    /**
     * Test 1: Single-level model type filtering
     * Query: users.any.name.eq=John
     * Expected: UserFilter created with name.equals=JOHN, bound to collectionAny
     * Validates: Requirements 1.1, 1.2
     */
    @Test
    @DisplayName("Test single-level model type filtering: users.any.name.eq=John")
    void testSingleLevelModelTypeFiltering() {
        // Given: A company with multiple users
        IntegrationTestCompany company = createCompanyWithUsers(
            createUser(1L, JOHN, JOHN_EMAIL, true, 30, USER_ROLE),
            createUser(2L, JANE, JANE_EMAIL, true, 25, ADMIN_ROLE),
            createUser(3L, "Bob", "bob@example.com", false, 35, USER_ROLE)
        );
        
        // When: Filter for companies where any user has name JOHN
        IntegrationTestCompanyFilter filter = new IntegrationTestCompanyFilter();
        CollectionFilter<IntegrationTestUser> usersFilter = new CollectionFilter<>();
        IntegrationTestUserFilter userFilter = new IntegrationTestUserFilter();
        
        StringFilter nameFilter = new StringFilter();
        nameFilter.setEquals(JOHN);
        userFilter.setName(nameFilter);
        
        usersFilter.setCollectionAny(userFilter);
        filter.setUsers(usersFilter);
        
        // Then: Company should match the filter
        boolean matches = companyService.validateFilter(company, filter);
        assertTrue(matches, "Company with user named 'John' should match the filter");
        
        System.out.println("✓ Single-level model type filtering test passed");
    }

    /**
     * Test 2: Multi-level nested model type filtering
     * Query: users.any.address.city.eq=Istanbul
     * Expected: UserFilter → AddressFilter → StringFilter chain created and bound
     * Validates: Requirements 3.1, 3.2, 3.3
     */
    @Test
    @DisplayName("Test multi-level nested model type filtering: users.any.address.city.eq=Istanbul")
    void testMultiLevelNestedModelTypeFiltering() {
        // Given: A company with users having addresses
        IntegrationTestUser user1 = createUser(1L, JOHN, JOHN_EMAIL, true, 30, USER_ROLE);
        user1.setAddress(createAddress("123 Main St", ISTANBUL, "TR", "34000", "Turkey"));
        
        IntegrationTestUser user2 = createUser(2L, JANE, JANE_EMAIL, true, 25, ADMIN_ROLE);
        user2.setAddress(createAddress("456 Oak Ave", "London", "UK", "SW1A", "UK"));
        
        IntegrationTestCompany company = createCompanyWithUsers(user1, user2);
        
        // When: Filter for companies where any user's address city is ISTANBUL
        IntegrationTestCompanyFilter filter = new IntegrationTestCompanyFilter();
        CollectionFilter<IntegrationTestUser> usersFilter = new CollectionFilter<>();
        IntegrationTestUserFilter userFilter = new IntegrationTestUserFilter();
        IntegrationTestAddressFilter addressFilter = new IntegrationTestAddressFilter();
        
        StringFilter cityFilter = new StringFilter();
        cityFilter.setEquals(ISTANBUL);
        addressFilter.setCity(cityFilter);
        userFilter.setAddress(addressFilter);
        
        usersFilter.setCollectionAny(userFilter);
        filter.setUsers(usersFilter);
        
        // Then: Company should match the filter
        boolean matches = companyService.validateFilter(company, filter);
        assertTrue(matches, "Company with user in Istanbul should match the filter");
        
        System.out.println("✓ Multi-level nested model type filtering test passed");
    }

    /**
     * Test 3: Multiple nested operators on same field
     * Query: users.any.name.eq=John&users.all.active.eq=true
     * Expected: Separate UserFilter instances for any and all operators
     * Validates: Requirements 1.3
     */
    @Test
    @DisplayName("Test multiple nested operators: users.any.name.eq=John&users.all.active.eq=true")
    void testMultipleNestedOperators() {
        // Given: A company with all active users, one named John
        IntegrationTestCompany company = createCompanyWithUsers(
            createUser(1L, JOHN, JOHN_EMAIL, true, 30, USER_ROLE),
            createUser(2L, JANE, JANE_EMAIL, true, 25, ADMIN_ROLE),
            createUser(3L, "Bob", "bob@example.com", true, 35, USER_ROLE)
        );
        
        // When: Filter for companies where any user is named JOHN AND all users are active
        IntegrationTestCompanyFilter filter = new IntegrationTestCompanyFilter();
        CollectionFilter<IntegrationTestUser> usersFilter = new CollectionFilter<>();
        
        // Any user named JOHN
        IntegrationTestUserFilter anyUserFilter = new IntegrationTestUserFilter();
        StringFilter nameFilter = new StringFilter();
        nameFilter.setEquals(JOHN);
        anyUserFilter.setName(nameFilter);
        usersFilter.setCollectionAny(anyUserFilter);
        
        // All users active
        IntegrationTestUserFilter allUserFilter = new IntegrationTestUserFilter();
        BooleanFilter activeFilter = new BooleanFilter();
        activeFilter.setEquals(true);
        allUserFilter.setActive(activeFilter);
        usersFilter.setCollectionAll(allUserFilter);
        
        filter.setUsers(usersFilter);
        
        // Then: Company should match the filter
        boolean matches = companyService.validateFilter(company, filter);
        assertTrue(matches, "Company with user named 'John' and all users active should match");
        
        System.out.println("✓ Multiple nested operators test passed");
    }

    /**
     * Test 4: Specification service validation with any operator
     * Validates: Requirements 4.1
     */
    @Test
    @DisplayName("Test specification service validation with any operator")
    void testSpecificationServiceValidationWithAnyOperator() {
        // Given: A company with users
        IntegrationTestCompany company = createCompanyWithUsers(
            createUser(1L, JOHN, JOHN_EMAIL, true, 30, USER_ROLE),
            createUser(2L, JANE, JANE_EMAIL, false, 25, ADMIN_ROLE)
        );
        
        // When: Filter for companies where any user is active
        IntegrationTestCompanyFilter filter = new IntegrationTestCompanyFilter();
        CollectionFilter<IntegrationTestUser> usersFilter = new CollectionFilter<>();
        IntegrationTestUserFilter userFilter = new IntegrationTestUserFilter();
        
        BooleanFilter activeFilter = new BooleanFilter();
        activeFilter.setEquals(true);
        userFilter.setActive(activeFilter);
        
        usersFilter.setCollectionAny(userFilter);
        filter.setUsers(usersFilter);
        
        // Then: Company should match (John is active)
        boolean matches = companyService.validateFilter(company, filter);
        assertTrue(matches, "Company with at least one active user should match");
        
        System.out.println("✓ Specification service validation with any operator test passed");
    }

    /**
     * Test 5: Specification service validation with all operator
     * Validates: Requirements 4.2
     */
    @Test
    @DisplayName("Test specification service validation with all operator")
    void testSpecificationServiceValidationWithAllOperator() {
        // Given: A company where all users are active
        IntegrationTestCompany company = createCompanyWithUsers(
            createUser(1L, JOHN, JOHN_EMAIL, true, 30, USER_ROLE),
            createUser(2L, JANE, JANE_EMAIL, true, 25, ADMIN_ROLE)
        );
        
        // When: Filter for companies where all users are active
        IntegrationTestCompanyFilter filter = new IntegrationTestCompanyFilter();
        CollectionFilter<IntegrationTestUser> usersFilter = new CollectionFilter<>();
        IntegrationTestUserFilter userFilter = new IntegrationTestUserFilter();
        
        BooleanFilter activeFilter = new BooleanFilter();
        activeFilter.setEquals(true);
        userFilter.setActive(activeFilter);
        
        usersFilter.setCollectionAll(userFilter);
        filter.setUsers(usersFilter);
        
        // Then: Company should match (all users are active)
        boolean matches = companyService.validateFilter(company, filter);
        assertTrue(matches, "Company with all active users should match");
        
        System.out.println("✓ Specification service validation with all operator test passed");
    }

    /**
     * Test 6: Specification service validation with none operator
     * Validates: Requirements 4.3
     */
    @Test
    @DisplayName("Test specification service validation with none operator")
    void testSpecificationServiceValidationWithNoneOperator() {
        // Given: A company with no admin users
        IntegrationTestCompany company = createCompanyWithUsers(
            createUser(1L, JOHN, JOHN_EMAIL, true, 30, USER_ROLE),
            createUser(2L, JANE, JANE_EMAIL, true, 25, USER_ROLE)
        );
        
        // When: Filter for companies where no users have role ADMIN_ROLE
        IntegrationTestCompanyFilter filter = new IntegrationTestCompanyFilter();
        CollectionFilter<IntegrationTestUser> usersFilter = new CollectionFilter<>();
        IntegrationTestUserFilter userFilter = new IntegrationTestUserFilter();
        
        StringFilter roleFilter = new StringFilter();
        roleFilter.setEquals(ADMIN_ROLE);
        userFilter.setRole(roleFilter);
        
        usersFilter.setCollectionNone(userFilter);
        filter.setUsers(usersFilter);
        
        // Then: Company should match (no admins)
        boolean matches = companyService.validateFilter(company, filter);
        assertTrue(matches, "Company with no admin users should match");
        
        System.out.println("✓ Specification service validation with none operator test passed");
    }

    /**
     * Test 7: Negative test - any operator should not match
     */
    @Test
    @DisplayName("Test negative case: any operator should not match when no users satisfy condition")
    void testAnyOperatorNoMatch() {
        // Given: A company with no users named ALICE
        IntegrationTestCompany company = createCompanyWithUsers(
            createUser(1L, JOHN, JOHN_EMAIL, true, 30, USER_ROLE),
            createUser(2L, JANE, JANE_EMAIL, true, 25, ADMIN_ROLE)
        );
        
        // When: Filter for companies where any user is named ALICE
        IntegrationTestCompanyFilter filter = new IntegrationTestCompanyFilter();
        CollectionFilter<IntegrationTestUser> usersFilter = new CollectionFilter<>();
        IntegrationTestUserFilter userFilter = new IntegrationTestUserFilter();
        
        StringFilter nameFilter = new StringFilter();
        nameFilter.setEquals(ALICE);
        userFilter.setName(nameFilter);
        
        usersFilter.setCollectionAny(userFilter);
        filter.setUsers(usersFilter);
        
        // Then: Company should not match
        boolean matches = companyService.validateFilter(company, filter);
        assertFalse(matches, "Company with no user named 'Alice' should not match");
        
        System.out.println("✓ Negative test for any operator passed");
    }

    /**
     * Test 8: Negative test - all operator should not match
     */
    @Test
    @DisplayName("Test negative case: all operator should not match when not all users satisfy condition")
    void testAllOperatorNoMatch() {
        // Given: A company with mixed active/inactive users
        IntegrationTestCompany company = createCompanyWithUsers(
            createUser(1L, JOHN, JOHN_EMAIL, true, 30, USER_ROLE),
            createUser(2L, JANE, JANE_EMAIL, false, 25, ADMIN_ROLE)
        );
        
        // When: Filter for companies where all users are active
        IntegrationTestCompanyFilter filter = new IntegrationTestCompanyFilter();
        CollectionFilter<IntegrationTestUser> usersFilter = new CollectionFilter<>();
        IntegrationTestUserFilter userFilter = new IntegrationTestUserFilter();
        
        BooleanFilter activeFilter = new BooleanFilter();
        activeFilter.setEquals(true);
        userFilter.setActive(activeFilter);
        
        usersFilter.setCollectionAll(userFilter);
        filter.setUsers(usersFilter);
        
        // Then: Company should not match (Jane is inactive)
        boolean matches = companyService.validateFilter(company, filter);
        assertFalse(matches, "Company with inactive users should not match all-active filter");
        
        System.out.println("✓ Negative test for all operator passed");
    }

    // Helper methods

    private IntegrationTestCompany createCompanyWithUsers(IntegrationTestUser... users) {
        IntegrationTestCompany company = new IntegrationTestCompany();
        company.setId(1L);
        company.setName("Test Company");
        company.setIndustry("Technology");
        company.setUsers(Arrays.asList(users));
        return company;
    }

    private IntegrationTestUser createUser(Long id, String name, String email, Boolean active, Integer age, String role) {
        IntegrationTestUser user = new IntegrationTestUser();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setActive(active);
        user.setAge(age);
        user.setRole(role);
        return user;
    }

    private IntegrationTestAddress createAddress(String street, String city, String state, String zipCode, String country) {
        IntegrationTestAddress address = new IntegrationTestAddress();
        address.setStreet(street);
        address.setCity(city);
        address.setState(state);
        address.setZipCode(zipCode);
        address.setCountry(country);
        return address;
    }
}
