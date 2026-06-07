package com.thy.fss.common.inmemory.factory;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.testmodel.User;
import com.thy.fss.common.inmemory.testmodel.UserSpecificationService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for AbstractRootBuilder base class.
 * Tests common functionality shared by InMemoryStoreBuilder and DashboardBuilder.
 */
class AbstractRootBuilderTest {

    private InMemorySpecStoreFactory factory;
    private TestRootBuilder testBuilder;

    @BeforeEach
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        testBuilder = new TestRootBuilder(factory, UserSpecificationService.INSTANCE, "test-consumer-id", false);
    }

    @AfterEach
    void tearDown() {
        DataSyncTestHelper.clearStaticRegistries();
    }

    @Test
    @DisplayName("Should create AbstractRootBuilder with valid parameters")
    void shouldCreateWithValidParameters() {
        assertThat(testBuilder).isNotNull();
        Assertions.assertThat(testBuilder.getEntityClass()).isEqualTo(User.class);
        Assertions.assertThat(testBuilder.getConsumerId()).isEqualTo("test-consumer-id");
        Assertions.assertThat(testBuilder.getFactory()).isEqualTo(factory);
    }

    @Test
    @DisplayName("Should reject null factory")
    void shouldRejectNullFactory() {
        assertThatThrownBy(() -> new TestRootBuilder(null, UserSpecificationService.INSTANCE, "test-id", false))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Factory cannot be null");
    }

    @Test
    @DisplayName("Should reject null entity class")
    void shouldRejectNullEntityClass() {
        assertThatThrownBy(() -> new TestRootBuilder(factory, null, "test-id", false))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Target service cannot be null");
    }

    @Test
    @DisplayName("Should reject null consumer ID")
    void shouldRejectNullConsumerId() {
        assertThatThrownBy(() -> new TestRootBuilder(factory, UserSpecificationService.INSTANCE, null, false))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Consumer ID cannot be null");
    }

    @Test
    @DisplayName("Should initialize with empty property mappings")
    void shouldInitializeWithEmptyPropertyMappings() {
        Assertions.assertThat(testBuilder.getPropertyMappings()).isEmpty();
    }

    @Test
    @DisplayName("Should reject null property mapping")
    void shouldRejectNullPropertyMapping() {
        assertThatThrownBy(() -> testBuilder.addPropertyMapping(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Property mapping cannot be null");
    }

    @Test
    @DisplayName("Should provide access to factory")
    void shouldProvideAccessToFactory() {
        Assertions.assertThat(testBuilder.getFactory()).isNotNull();
        Assertions.assertThat(testBuilder.getFactory()).isSameAs(factory);
    }

    @Test
    @DisplayName("Should provide access to entity class")
    void shouldProvideAccessToEntityClass() {
        Assertions.assertThat(testBuilder.getEntityClass()).isNotNull();
        Assertions.assertThat(testBuilder.getEntityClass()).isEqualTo(User.class);
    }

    @Test
    @DisplayName("Should provide access to consumer ID")
    void shouldProvideAccessToConsumerId() {
        Assertions.assertThat(testBuilder.getConsumerId()).isNotNull();
        Assertions.assertThat(testBuilder.getConsumerId()).isEqualTo("test-consumer-id");
    }

    /**
     * Test implementation of AbstractRootBuilder for testing purposes.
     */
    private static class TestRootBuilder extends AbstractRootBuilder<User> {
        TestRootBuilder(InMemorySpecStoreFactory factory, SpecificationService<User> entityClass, String consumerId, boolean isForDashboard) {
            super(factory, entityClass, consumerId, isForDashboard);
        }
    }
}
