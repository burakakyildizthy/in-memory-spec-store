package com.thy.fss.common.inmemory.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Identifiable interface.
 * Tests the contract and usage patterns of the Identifiable interface.
 */
class IdentifiableTest {
    
    private static final String XYZ_789 = "XYZ789";
    private static final String EXTRA_DATA = "Extra Data";
    private static final String ABC_123 = "ABC123";

    @Test
    void testIdentifiableContract() {
        // Given: A test implementation of Identifiable
        TestEntity entity = new TestEntity(123L, "Test Entity");

        // When: Getting the ID
        Long id = entity.getIdentity();

        // Then: The ID should be returned correctly
        assertEquals(123L, id);
        assertNotNull(id);
    }

    @Test
    void testIdentifiableWithStringGetGetId() {
        // Given: An entity with String ID
        StringIdEntity entity = new StringIdEntity(ABC_123, "String ID Entity");

        // When: Getting the ID
        String id = entity.getIdentity();

        // Then: The ID should be returned correctly
        assertEquals(ABC_123, id);
        assertNotNull(id);
    }

    @Test
    void testIdentifiableWithNullGetGetId() {
        // Given: An entity with null ID
        TestEntity entity = new TestEntity(null, "Null ID Entity");

        // When: Getting the ID
        Long id = entity.getIdentity();

        // Then: The ID should be null
        assertNull(id);
    }

    @Test
    void testIdentifiableInheritance() {
        // Given: A subclass implementing Identifiable
        ExtendedEntity entity = new ExtendedEntity(456L, "Extended Entity", EXTRA_DATA);

        // When: Getting the ID through the interface
        Identifiable<Long> identifiable = entity;
        Long id = identifiable.getIdentity();

        // Then: The ID should be accessible through the interface
        assertEquals(456L, id);
        assertEquals(EXTRA_DATA, entity.getExtraData());
    }

    @Test
    void testIdentifiablePolymorphism() {
        // Given: Different implementations of Identifiable
        Identifiable<Long> longIdEntity = new TestEntity(789L, "Long ID");
        Identifiable<String> stringIdEntity = new StringIdEntity(XYZ_789, "String ID");

        // When: Using them polymorphically
        Long longId = longIdEntity.getIdentity();
        String stringId = stringIdEntity.getIdentity();

        // Then: Each should return the correct type
        assertEquals(789L, longId);
        assertEquals(XYZ_789, stringId);
    }

    // Test implementations
    private static class TestEntity implements Identifiable<Long> {
        private final Long id;
        private final String name;

        public TestEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public Long getIdentity() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    private record StringIdEntity(String getIdentity, String name) implements Identifiable<String> {
    }

    private static class ExtendedEntity extends TestEntity {
        private final String extraData;

        public ExtendedEntity(Long id, String name, String extraData) {
            super(id, name);
            this.extraData = extraData;
        }

        public String getExtraData() {
            return extraData;
        }
    }
}