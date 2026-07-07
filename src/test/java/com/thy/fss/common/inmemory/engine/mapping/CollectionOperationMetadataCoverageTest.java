package com.thy.fss.common.inmemory.engine.mapping;

import com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute;
import org.junit.jupiter.api.Test;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Coverage tests for CollectionOperationMetadata targeting missed branches.
 */
class CollectionOperationMetadataCoverageTest {

    private static final CollectionAttribute<Object, String> COLLECTION_ATTR =
            new CollectionAttribute<>("items", Object.class, String.class);

    // ========== Constructor validation ==========

    @Test
    void constructor_withNegativePathIndex_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new CollectionOperationMetadata<>(-1, COLLECTION_ATTR, CollectionSelector.ALL, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pathIndex must be non-negative");
    }

    @Test
    void constructor_withNullCollectionAttribute_throwsNullPointerException() {
        assertThatThrownBy(() -> new CollectionOperationMetadata<>(0, null, CollectionSelector.ALL, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_withNullSelector_throwsNullPointerException() {
        assertThatThrownBy(() -> new CollectionOperationMetadata<>(0, COLLECTION_ATTR, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructorWithComparator_withNegativePathIndex_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new CollectionOperationMetadata<>(-1, COLLECTION_ATTR, CollectionSelector.ALL, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_withValidArgs_createsInstance() {
        var metadata = new CollectionOperationMetadata<>(0, COLLECTION_ATTR, CollectionSelector.FIRST, null);
        assertThat(metadata.getPathIndex()).isEqualTo(0);
        assertThat(metadata.getCollectionAttribute()).isEqualTo(COLLECTION_ATTR);
        assertThat(metadata.getSelector()).isEqualTo(CollectionSelector.FIRST);
        assertThat(metadata.getSpecification()).isNull();
        assertThat(metadata.getComparator()).isNull();
    }

    @Test
    void constructorWithComparator_withValidArgs_createsInstance() {
        Comparator<String> comparator = Comparator.naturalOrder();
        var metadata = new CollectionOperationMetadata<>(1, COLLECTION_ATTR, CollectionSelector.LAST, null, comparator);
        assertThat(metadata.getPathIndex()).isEqualTo(1);
        assertThat(metadata.getComparator()).isEqualTo(comparator);
        assertThat(metadata.getSelector()).isEqualTo(CollectionSelector.LAST);
    }

    @Test
    void getElementType_returnsCollectionElementType() {
        var metadata = new CollectionOperationMetadata<>(0, COLLECTION_ATTR, CollectionSelector.ALL, null);
        assertThat(metadata.getElementType()).isEqualTo(String.class);
    }

    // ========== equals ==========

    @Test
    void equals_withSameObject_returnsTrue() {
        var metadata = new CollectionOperationMetadata<>(0, COLLECTION_ATTR, CollectionSelector.ALL, null);
        assertThat(metadata.equals(metadata)).isTrue();
    }

    @Test
    void equals_withNull_returnsFalse() {
        var metadata = new CollectionOperationMetadata<>(0, COLLECTION_ATTR, CollectionSelector.ALL, null);
        assertThat(metadata.equals(null)).isFalse();
    }

    @Test
    void equals_withDifferentClass_returnsFalse() {
        var metadata = new CollectionOperationMetadata<>(0, COLLECTION_ATTR, CollectionSelector.ALL, null);
        assertThat(metadata.equals("not-a-metadata")).isFalse();
    }

    @Test
    void equals_withSameValues_returnsTrue() {
        var m1 = new CollectionOperationMetadata<>(0, COLLECTION_ATTR, CollectionSelector.ALL, null);
        var m2 = new CollectionOperationMetadata<>(0, COLLECTION_ATTR, CollectionSelector.ALL, null);
        assertThat(m1.equals(m2)).isTrue();
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
    }

    @Test
    void equals_withDifferentPathIndex_returnsFalse() {
        var m1 = new CollectionOperationMetadata<>(0, COLLECTION_ATTR, CollectionSelector.ALL, null);
        var m2 = new CollectionOperationMetadata<>(1, COLLECTION_ATTR, CollectionSelector.ALL, null);
        assertThat(m1.equals(m2)).isFalse();
    }

    @Test
    void equals_withDifferentSelector_returnsFalse() {
        var m1 = new CollectionOperationMetadata<>(0, COLLECTION_ATTR, CollectionSelector.ALL, null);
        var m2 = new CollectionOperationMetadata<>(0, COLLECTION_ATTR, CollectionSelector.FIRST, null);
        assertThat(m1.equals(m2)).isFalse();
    }

    @Test
    void equals_withDifferentCollectionAttribute_returnsFalse() {
        var attr2 = new CollectionAttribute<>("other", Object.class, String.class);
        var m1 = new CollectionOperationMetadata<>(0, COLLECTION_ATTR, CollectionSelector.ALL, null);
        var m2 = new CollectionOperationMetadata<>(0, attr2, CollectionSelector.ALL, null);
        assertThat(m1.equals(m2)).isFalse();
    }

    // ========== hashCode ==========

    @Test
    void hashCode_withSameValues_returnsSameHash() {
        var m1 = new CollectionOperationMetadata<>(0, COLLECTION_ATTR, CollectionSelector.ALL, null);
        var m2 = new CollectionOperationMetadata<>(0, COLLECTION_ATTR, CollectionSelector.ALL, null);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
    }

    // ========== toString ==========

    @Test
    void toString_containsRelevantInfo() {
        var metadata = new CollectionOperationMetadata<>(0, COLLECTION_ATTR, CollectionSelector.ALL, null);
        String str = metadata.toString();
        assertThat(str).contains("pathIndex=0");
        assertThat(str).contains("selector=ALL");
    }
}
