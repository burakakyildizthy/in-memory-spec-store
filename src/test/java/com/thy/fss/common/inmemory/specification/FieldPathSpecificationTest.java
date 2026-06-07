package com.thy.fss.common.inmemory.specification;

import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import com.thy.fss.common.inmemory.testmodel.Customer;
import com.thy.fss.common.inmemory.testmodel.CustomerSpecificationService;
import com.thy.fss.common.inmemory.testmodel.Customer_;
import com.thy.fss.common.inmemory.testmodel.Order;
import com.thy.fss.common.inmemory.testmodel.OrderSpecificationService;
import com.thy.fss.common.inmemory.testmodel.Order_;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for FieldPathSpecification to verify nested property path navigation in specifications.
 */
@DisplayName("FieldPathSpecification Tests")
class FieldPathSpecificationTest {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private SpecificationService<Order> orderService;
    private SpecificationService<Customer> customerService;

    @BeforeEach
    void setUp() {
        orderService = OrderSpecificationService.INSTANCE;
        customerService = CustomerSpecificationService.INSTANCE;
    }

    @Test
    @DisplayName("Should navigate field path and apply specification on direct field")
    void shouldNavigateFieldPathAndApplySpecification() {
        // Given: Order with status ACTIVE
        Order order = new Order();
        order.setId(100L);
        order.setStatus(ACTIVE_STATUS);

        // Create path: Order.status (single-level path supported by FieldPathSpecification)
        List<MetaAttribute<?, ?>> path = Arrays.asList(Order_.status);

        // Create specification for order status equals "ACTIVE"
        Specification<String> statusSpec = new Specification<String>() {
            @Override
            public boolean test(String s) {
                return ACTIVE_STATUS.equals(s);
            }

            @Override
            public java.util.function.Predicate<String> toPredicate() {
                return this::test;
            }
        };

        // Create FieldPathSpecification
        FieldPathSpecification<Order> pathSpec = new FieldPathSpecification<>(path, statusSpec, orderService);

        // When & Then: Should match
        assertThat(pathSpec.test(order)).isTrue();
    }

    @Test
    @DisplayName("Should return false when direct value does not match")
    void shouldReturnFalseWhenDirectValueDoesNotMatch() {
        // Given: Order with non-matching status
        Order order = new Order();
        order.setId(100L);
        order.setStatus("INACTIVE");

        // Create path: Order.status
        List<MetaAttribute<?, ?>> path = Arrays.asList(Order_.status);

        // Create specification for order status equals "ACTIVE"
        Specification<String> statusSpec = new Specification<String>() {
            @Override
            public boolean test(String s) {
                return ACTIVE_STATUS.equals(s);
            }

            @Override
            public java.util.function.Predicate<String> toPredicate() {
                return this::test;
            }
        };

        FieldPathSpecification<Order> pathSpec = new FieldPathSpecification<>(path, statusSpec, orderService);

        // When & Then: Should not match
        assertThat(pathSpec.test(order)).isFalse();
    }

    @Test
    @DisplayName("Should handle null intermediate values")
    void shouldHandleNullIntermediateValues() {
        // Given: Order without customer
        Order order = new Order();
        order.setId(100L);
        order.setCustomerId(null);

        // Create path: Order.customer.name
        List<MetaAttribute<?, ?>> path = Arrays.asList(Order_.customerId, Customer_.name);

        // Create specification for customer name
        Specification<Customer> nameSpec = new Specification<Customer>() {
            @Override
            public boolean test(Customer c) {
                return customerService.validateSpecification(c, Customer_.name, Operator.EQUALS, "John Doe");
            }

            @Override
            public java.util.function.Predicate<Customer> toPredicate() {
                return this::test;
            }
        };

        FieldPathSpecification<Order> pathSpec = new FieldPathSpecification<>(path, nameSpec, orderService);

        // When & Then: Should return false for null intermediate value
        assertThat(pathSpec.test(order)).isFalse();
    }

    @Test
    @DisplayName("Should reject null or empty field path")
    void shouldRejectNullOrEmptyFieldPath() {
        Specification<Customer> dummySpec = new Specification<Customer>() {
            @Override
            public boolean test(Customer c) {
                return true;
            }

            @Override
            public java.util.function.Predicate<Customer> toPredicate() {
                return this::test;
            }
        };

        // Null path
        assertThatThrownBy(() -> new FieldPathSpecification<>(null, dummySpec, orderService))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Field path cannot be null or empty");

        // Empty path
        List<MetaAttribute<?, ?>> emptyPath = Arrays.asList();

        // Only call the constructor expected to throw
        assertThatThrownBy(() -> new FieldPathSpecification<>(emptyPath, dummySpec, orderService))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Field path cannot be null or empty");
    }

    @Test
    @DisplayName("Should reject null delegate specification")
    void shouldRejectNullDelegateSpecification() {
        List<MetaAttribute<?, ?>> path = Arrays.asList(Order_.customerId, Customer_.name);

        assertThatThrownBy(() -> new FieldPathSpecification<>(path, null, orderService))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Delegate specification cannot be null");
    }

    @Test
    @DisplayName("Should reject null specification service")
    void shouldRejectNullSpecificationService() {
        List<MetaAttribute<?, ?>> path = Arrays.asList(Order_.customerId, Customer_.name);
        Specification<Customer> dummySpec = new Specification<Customer>() {
            @Override
            public boolean test(Customer c) {
                return true;
            }

            @Override
            public java.util.function.Predicate<Customer> toPredicate() {
                return this::test;
            }
        };

        assertThatThrownBy(() -> new FieldPathSpecification<>(path, dummySpec, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Root specification service cannot be null");
    }
}
