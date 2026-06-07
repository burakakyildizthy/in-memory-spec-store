package com.thy.fss.common.inmemory.datasource;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

import com.thy.fss.common.inmemory.entity.Identifiable;
import com.thy.fss.common.inmemory.specification.Specification;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

// Feature: streaming-datasource-support, Property 5: TimeWindowRule Specification Filtreleme Doğruluğu

/**
 * Property-based tests for {@link TimeWindowRule}.
 *
 * <p><b>Validates: Requirements 4.2, 4.5</b></p>
 */
class TimeWindowRuleTest {

    /**
     * Property 5a: For any entity where specificationFactory.get().test(entity) returns true,
     * isValid(entity) must also return true (entity is within the time window).
     *
     * Validates: Requirements 4.2, 4.5
     */
    @Property(tries = 100)
    void isValidReturnsTrueWhenSpecificationPasses(
            @ForAll @IntRange(min = 1, max = 10000) int entityId) {

        // Specification that always returns true (entity within window)
        Supplier<Specification<TestEntity>> factory = () -> (Specification<TestEntity>) () -> t -> true;
        TimeWindowRule<TestEntity> rule = new TimeWindowRule<>("ds", Duration.ofHours(2), factory);

        TestEntity entity = new TestEntity(entityId, Instant.now());

        assertThat(rule.isValid(entity)).isTrue();
        assertThat(factory.get().test(entity)).isTrue();
    }

    /**
     * Property 5b: For any entity where specificationFactory.get().test(entity) returns false,
     * isValid(entity) must also return false (entity is expired).
     *
     * Validates: Requirements 4.2, 4.5
     */
    @Property(tries = 100)
    void isValidReturnsFalseWhenSpecificationFails(
            @ForAll @IntRange(min = 1, max = 10000) int entityId) {

        // Specification that always returns false (entity expired)
        Supplier<Specification<TestEntity>> factory = () -> (Specification<TestEntity>) () -> t -> false;
        TimeWindowRule<TestEntity> rule = new TimeWindowRule<>("ds", Duration.ofHours(2), factory);

        TestEntity entity = new TestEntity(entityId, Instant.now());

        assertThat(rule.isValid(entity)).isFalse();
        assertThat(factory.get().test(entity)).isFalse();
    }

    /**
     * Property 5c: The Supplier pattern produces a fresh Specification on each call.
     * Verify that specificationFactory.get() is called every time isValid() is invoked.
     *
     * Validates: Requirements 4.2, 4.5
     */
    @Property(tries = 100)
    void supplierIsCalledOnEachIsValidInvocation(
            @ForAll @IntRange(min = 1, max = 50) int callCount) {

        AtomicInteger factoryCallCount = new AtomicInteger(0);

        Supplier<Specification<TestEntity>> factory = () -> {
            factoryCallCount.incrementAndGet();
            return (Specification<TestEntity>) () -> t -> true;
        };

        TimeWindowRule<TestEntity> rule = new TimeWindowRule<>("ds", Duration.ofHours(2), factory);
        TestEntity entity = new TestEntity(1, Instant.now());

        for (int i = 0; i < callCount; i++) {
            rule.isValid(entity);
        }

        assertThat(factoryCallCount.get()).isEqualTo(callCount);
    }

    /**
     * Simple test entity implementing Identifiable with a timestamp field.
     */
    private static class TestEntity implements Identifiable<Integer> {
        private final int id;
        private final Instant timestamp;

        TestEntity(int id, Instant timestamp) {
            this.id = id;
            this.timestamp = timestamp;
        }

        @Override
        public Integer getIdentity() {
            return id;
        }

        public Instant getTimestamp() {
            return timestamp;
        }
    }
}
