package com.thy.fss.common.inmemory.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AggregationTypeAdvancedTest {

    private static final String COUNT = "COUNT";
    private static final String SUM = "SUM";
    private static final String AVG = "AVG";
    private static final String MIN = "MIN";
    private static final String MAX = "MAX";
    private static final String CUSTOM = "CUSTOM";
    private static final String COUNT_LOWERCASE = "count";
    private static final String SUM_LOWERCASE = "sum";
    

    @Test
    @DisplayName("Should have all expected aggregation types")
    void shouldHaveAllExpectedAggregationTypes() {
        AggregationType[] types = AggregationType.values();

        assertEquals(6, types.length);

        // Verify all expected types exist
        assertTrue(java.util.Arrays.asList(types).contains(AggregationType.COUNT));
        assertTrue(java.util.Arrays.asList(types).contains(AggregationType.SUM));
        assertTrue(java.util.Arrays.asList(types).contains(AggregationType.AVG));
        assertTrue(java.util.Arrays.asList(types).contains(AggregationType.MIN));
        assertTrue(java.util.Arrays.asList(types).contains(AggregationType.MAX));
        assertTrue(java.util.Arrays.asList(types).contains(AggregationType.CUSTOM));
    }

    @Test
    @DisplayName("Should convert to string correctly")
    void shouldConvertToStringCorrectly() {
        assertEquals(COUNT, AggregationType.COUNT.toString());
        assertEquals(SUM, AggregationType.SUM.toString());
        assertEquals(AVG, AggregationType.AVG.toString());
        assertEquals(MIN, AggregationType.MIN.toString());
        assertEquals(MAX, AggregationType.MAX.toString());
        assertEquals(CUSTOM, AggregationType.CUSTOM.toString());
    }

    @Test
    @DisplayName("Should support valueOf operations")
    void shouldSupportValueOfOperations() {
        assertEquals(AggregationType.COUNT, AggregationType.valueOf(COUNT));
        assertEquals(AggregationType.SUM, AggregationType.valueOf(SUM));
        assertEquals(AggregationType.AVG, AggregationType.valueOf(AVG));
        assertEquals(AggregationType.MIN, AggregationType.valueOf(MIN));
        assertEquals(AggregationType.MAX, AggregationType.valueOf(MAX));
        assertEquals(AggregationType.CUSTOM, AggregationType.valueOf(CUSTOM));
    }

    @Test
    @DisplayName("Should throw exception for invalid valueOf")
    void shouldThrowExceptionForInvalidValueOf() {
        assertThrows(IllegalArgumentException.class, () -> {
            AggregationType.valueOf("INVALID");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            AggregationType.valueOf(COUNT_LOWERCASE); // lowercase
        });

        assertThrows(NullPointerException.class, () -> {
            AggregationType.valueOf(null);
        });
    }

    @Test
    @DisplayName("Should support ordinal operations")
    void shouldSupportOrdinalOperations() {
        // Test that ordinals are consistent
        assertTrue(AggregationType.COUNT.ordinal() >= 0);
        assertTrue(AggregationType.SUM.ordinal() >= 0);
        assertTrue(AggregationType.AVG.ordinal() >= 0);
        assertTrue(AggregationType.MIN.ordinal() >= 0);
        assertTrue(AggregationType.MAX.ordinal() >= 0);
        assertTrue(AggregationType.CUSTOM.ordinal() >= 0);

        // All ordinals should be different
        AggregationType[] types = AggregationType.values();
        for (int i = 0; i < types.length; i++) {
            for (int j = i + 1; j < types.length; j++) {
                assertNotEquals(types[i].ordinal(), types[j].ordinal());
            }
        }
    }

    @Test
    @DisplayName("Should support equality operations")
    void shouldSupportEqualityOperations() {
        assertNotEquals(AggregationType.COUNT, AggregationType.SUM);
        assertNotEquals(null, AggregationType.COUNT);
        assertNotEquals(COUNT, AggregationType.COUNT);
    }

    @Test
    @DisplayName("Should support switch statements")
    void shouldSupportSwitchStatements() {
        for (AggregationType type : AggregationType.values()) {
            String result = switch (type) {
                case COUNT -> COUNT_LOWERCASE;
                case SUM -> SUM_LOWERCASE;
                case AVG -> "average";
                case MIN -> "minimum";
                case MAX -> "maximum";
                case CUSTOM -> "custom";
            };

            assertNotNull(result);
            assertFalse(result.isEmpty());
        }
    }

    @Test
    @DisplayName("Should maintain consistent hash codes")
    void shouldMaintainConsistentHashCodes() {
        for (AggregationType type : AggregationType.values()) {
            int hashCode1 = type.hashCode();
            int hashCode2 = type.hashCode();
            assertEquals(hashCode1, hashCode2);
        }
    }

    @Test
    @DisplayName("Should work in collections")
    void shouldWorkInCollections() {
        java.util.Set<AggregationType> typeSet = java.util.EnumSet.allOf(AggregationType.class);
        assertEquals(6, typeSet.size());

        java.util.List<AggregationType> typeList = java.util.Arrays.asList(AggregationType.values());
        assertEquals(6, typeList.size());

        java.util.Map<AggregationType, String> typeMap = new java.util.EnumMap<>(AggregationType.class);
        typeMap.put(AggregationType.COUNT, COUNT_LOWERCASE);
        typeMap.put(AggregationType.SUM, SUM_LOWERCASE);
        assertEquals(2, typeMap.size());
    }
}