package com.thy.fss.common.inmemory.engine;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.testmodel.Customer;
import com.thy.fss.common.inmemory.testmodel.CustomerSpecificationService;
import com.thy.fss.common.inmemory.testmodel.Customer_;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Mapping Bug Reproduction Test")
class MappingBugReproductionTest {

    // Constants for duplicate string literals
    private static final String TEST_CUSTOMER_NAME = "John Doe";
    private static final String TEST_CUSTOMER_EMAIL = "john@example.com";
    private static final String LOG_CUSTOMER_ID = "Customer ID: ";
    private static final String LOG_CUSTOMER_NAME = "Customer Name: ";
    private static final String LOG_CUSTOMER_EMAIL = "Customer Email: ";
    private static final String LOG_CUSTOMER_TOTAL_SPENT = "Customer TotalSpent: ";
    private static final String ASSERT_ID_NO_CHANGE = "Customer ID should not change";
    private static final String ASSERT_NAME_NO_CHANGE = "Customer name should not change";
    private static final String ASSERT_EMAIL_NO_CHANGE = "Customer email should not change";

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    @Test
    @DisplayName("Simple test: Verify setValueByPath only changes target field")
    void testSetValueByPathOnlyChangesTargetField() {
        // Create a customer with all fields populated
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setName(TEST_CUSTOMER_NAME);
        customer.setEmail(TEST_CUSTOMER_EMAIL);
        customer.setTotalSpent(0.0);
        
        System.out.println("=== BEFORE setValueByPath ===");
        System.out.println(LOG_CUSTOMER_ID + customer.getIdentity());
        System.out.println(LOG_CUSTOMER_NAME + customer.getName());
        System.out.println(LOG_CUSTOMER_EMAIL + customer.getEmail());
        System.out.println(LOG_CUSTOMER_TOTAL_SPENT + customer.getTotalSpent());

        // Use SpecificationService to set only totalSpent
        com.thy.fss.common.inmemory.specification.SpecificationService<Customer> service = 
            CustomerSpecificationService.INSTANCE;
        
        java.util.List<com.thy.fss.common.inmemory.specification.attribute.MetaAttribute<?, ?>> path = 
            java.util.Arrays.asList(Customer_.totalSpent);
        
        service.setValueByPath(customer, path, 300.0);
        
        System.out.println("\n=== AFTER setValueByPath ===");
        System.out.println(LOG_CUSTOMER_ID + customer.getIdentity());
        System.out.println(LOG_CUSTOMER_NAME + customer.getName());
        System.out.println(LOG_CUSTOMER_EMAIL + customer.getEmail());
        System.out.println(LOG_CUSTOMER_TOTAL_SPENT + customer.getTotalSpent());

        // Verify that ONLY totalSpent was changed
        assertEquals(1L, customer.getIdentity(), ASSERT_ID_NO_CHANGE);
        assertEquals(TEST_CUSTOMER_NAME, customer.getName(), ASSERT_NAME_NO_CHANGE);
        assertEquals(TEST_CUSTOMER_EMAIL, customer.getEmail(), ASSERT_EMAIL_NO_CHANGE);
        assertEquals(300.0, customer.getTotalSpent(), 0.001, "TotalSpent should be updated");
    }
}
