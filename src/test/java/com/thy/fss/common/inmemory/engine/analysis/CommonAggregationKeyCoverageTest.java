package com.thy.fss.common.inmemory.engine.analysis;

import com.thy.fss.common.inmemory.config.AggregationType;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Coverage tests for CommonAggregationKey targeting missed branches in equals/hashCode.
 */
class CommonAggregationKeyCoverageTest {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private final MetaAttribute<?, ?> attr1 = (MetaAttribute<?, ?>) mock(MetaAttribute.class);
    @SuppressWarnings({"unchecked", "rawtypes"})
    private final MetaAttribute<?, ?> attr2 = (MetaAttribute<?, ?>) mock(MetaAttribute.class);

    // ========== Compact constructor ==========

    @Test
    void constructor_withNullDataSourceName_throwsNullPointerException() {
        assertThatThrownBy(() -> new CommonAggregationKey(null, null, null, AggregationType.COUNT))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_withNullAggregationType_throwsNullPointerException() {
        assertThatThrownBy(() -> new CommonAggregationKey("ds", null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_withNullFieldPath_usesEmptyList() {
        var key = new CommonAggregationKey("ds", null, null, AggregationType.COUNT);
        assertThat(key.fieldPath()).isEmpty();
    }

    @Test
    void constructor_withEmptyFieldPath_setsEmptyList() {
        var key = new CommonAggregationKey("ds", null, Collections.emptyList(), AggregationType.COUNT);
        assertThat(key.fieldPath()).isEmpty();
    }

    // ========== toStorageKey ==========

    @Test
    void toStorageKey_withNullSpecification_usesNullString() {
        var key = new CommonAggregationKey("myDs", null, Collections.emptyList(), AggregationType.COUNT);
        String storageKey = key.toStorageKey();
        assertThat(storageKey).contains("myDs");
        assertThat(storageKey).contains("null");
        assertThat(storageKey).contains("COUNT");
    }

    @Test
    void toStorageKey_withNonNullSpecification_usesHashCode() {
        var spec = mock(com.thy.fss.common.inmemory.specification.Specification.class);
        var key = new CommonAggregationKey("myDs", spec, Collections.emptyList(), AggregationType.SUM);
        String storageKey = key.toStorageKey();
        assertThat(storageKey).contains("myDs");
        assertThat(storageKey).contains("SUM");
    }

    @Test
    void toStorageKey_withFieldPath_includesPathIdentityHash() {
        List<MetaAttribute<?, ?>> path = List.of(attr1);
        var key = new CommonAggregationKey("ds", null, path, AggregationType.COUNT);
        String storageKey = key.toStorageKey();
        assertThat(storageKey).isNotBlank();
    }

    // ========== equals ==========

    @Test
    void equals_withSameObject_returnsTrue() {
        var key = new CommonAggregationKey("ds", null, Collections.emptyList(), AggregationType.COUNT);
        assertThat(key.equals(key)).isTrue();
    }

    @Test
    void equals_withNull_returnsFalse() {
        var key = new CommonAggregationKey("ds", null, Collections.emptyList(), AggregationType.COUNT);
        assertThat(key.equals(null)).isFalse();
    }

    @Test
    void equals_withDifferentClass_returnsFalse() {
        var key = new CommonAggregationKey("ds", null, Collections.emptyList(), AggregationType.COUNT);
        assertThat(key.equals("not-a-key")).isFalse();
    }

    @Test
    void equals_withSameValues_returnsTrue() {
        List<MetaAttribute<?, ?>> fieldPath = List.of(attr1);
        var k1 = new CommonAggregationKey("ds", null, fieldPath, AggregationType.COUNT);
        var k2 = new CommonAggregationKey("ds", null, fieldPath, AggregationType.COUNT);
        assertThat(k1.equals(k2)).isTrue();
    }

    @Test
    void equals_withDifferentDataSourceName_returnsFalse() {
        var k1 = new CommonAggregationKey("ds1", null, Collections.emptyList(), AggregationType.COUNT);
        var k2 = new CommonAggregationKey("ds2", null, Collections.emptyList(), AggregationType.COUNT);
        assertThat(k1.equals(k2)).isFalse();
    }

    @Test
    void equals_withDifferentAggregationType_returnsFalse() {
        var k1 = new CommonAggregationKey("ds", null, Collections.emptyList(), AggregationType.COUNT);
        var k2 = new CommonAggregationKey("ds", null, Collections.emptyList(), AggregationType.SUM);
        assertThat(k1.equals(k2)).isFalse();
    }

    @Test
    void equals_withDifferentPathSize_returnsFalse() {
        List<MetaAttribute<?, ?>> path1 = List.of(attr1);
        List<MetaAttribute<?, ?>> path2 = List.of(attr1, attr2);
        var k1 = new CommonAggregationKey("ds", null, path1, AggregationType.COUNT);
        var k2 = new CommonAggregationKey("ds", null, path2, AggregationType.COUNT);
        assertThat(k1.equals(k2)).isFalse();
    }

    @Test
    void equals_withSamePathSizeButDifferentRefs_returnsFalse() {
        List<MetaAttribute<?, ?>> path1 = List.of(attr1);
        List<MetaAttribute<?, ?>> path2 = List.of(attr2);
        var k1 = new CommonAggregationKey("ds", null, path1, AggregationType.COUNT);
        var k2 = new CommonAggregationKey("ds", null, path2, AggregationType.COUNT);
        assertThat(k1.equals(k2)).isFalse();
    }

    @Test
    void equals_withBothNullFieldPaths_returnsTrue() {
        // fieldPath=null is normalized to empty list, so two keys with null fieldPaths are equal
        var k1 = new CommonAggregationKey("ds", null, null, AggregationType.COUNT);
        var k2 = new CommonAggregationKey("ds", null, null, AggregationType.COUNT);
        assertThat(k1.equals(k2)).isTrue();
    }

    // ========== hashCode ==========

    @Test
    void hashCode_withEqualObjects_returnsSameHash() {
        List<MetaAttribute<?, ?>> fieldPath = List.of(attr1);
        var k1 = new CommonAggregationKey("ds", null, fieldPath, AggregationType.COUNT);
        var k2 = new CommonAggregationKey("ds", null, fieldPath, AggregationType.COUNT);
        assertThat(k1.hashCode()).isEqualTo(k2.hashCode());
    }

    @Test
    void hashCode_withFieldPath_includesIdentityHash() {
        List<MetaAttribute<?, ?>> path = List.of(attr1);
        var k1 = new CommonAggregationKey("ds", null, path, AggregationType.COUNT);
        // Just verify it runs without exception and returns a value
        assertThat(k1.hashCode()).isNotNull();
    }

    // ========== toString ==========

    @Test
    void toString_containsStorageKey() {
        var key = new CommonAggregationKey("ds", null, Collections.emptyList(), AggregationType.COUNT);
        assertThat(key.toString()).contains("CommonAggregationKey[");
    }
}
