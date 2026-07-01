package com.thy.fss.common.inmemory.engine.index;

import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestTarget;
import com.thy.fss.common.inmemory.factory.navigation.testmodel.TestTarget_;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Coverage tests for IndexDefinition and its inner classes targeting missed branches.
 */
class IndexDefinitionCoverageTest {

    // ========== Builder validation ==========

    @Test
    void builder_withNullEntityClass_throwsNullPointerException() {
        assertThatThrownBy(() -> IndexDefinition.builder(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void addKeyField_withNullField_throwsNullPointerException() {
        assertThatThrownBy(() -> IndexDefinition.builder(TestTarget.class)
                .addKeyField(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void addKeyFieldWithComparator_withNullComparator_throwsNullPointerException() {
        Comparator<Long> nullComparator = null;
        assertThatThrownBy(() -> IndexDefinition.builder(TestTarget.class)
                .addKeyField(TestTarget_.id, nullComparator))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void addKeyFieldWithPath_withNullPath_throwsNullPointerException() {
        assertThatThrownBy(() -> IndexDefinition.builder(TestTarget.class)
                .addKeyFieldWithPath(null, t -> t))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void addKeyFieldWithPath_withNullExtractor_throwsNullPointerException() {
        assertThatThrownBy(() -> IndexDefinition.builder(TestTarget.class)
                .addKeyFieldWithPath(List.of(TestTarget_.id), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void addKeyFieldWithPath_withEmptyPath_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> IndexDefinition.builder(TestTarget.class)
                .addKeyFieldWithPath(Collections.emptyList(), t -> t))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("keyPath cannot be empty");
    }

    @Test
    void build_withNoKeyFields_throwsIllegalStateException() {
        assertThatThrownBy(() -> IndexDefinition.builder(TestTarget.class).build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("At least one key field must be specified");
    }

    @Test
    void build_withOneField_createsIndexDefinition() {
        IndexDefinition<TestTarget> def = IndexDefinition.builder(TestTarget.class)
                .addKeyField(TestTarget_.id)
                .build();

        assertThat(def).isNotNull();
        assertThat(def.getEntityClass()).isEqualTo(TestTarget.class);
        assertThat(def.getDepth()).isEqualTo(1);
        assertThat(def.getKeyFields()).hasSize(1);
        assertThat(def.getKeyFieldNames()).containsExactly("id");
    }

    @Test
    void build_withMultipleFields_createsMultiLevelIndex() {
        IndexDefinition<TestTarget> def = IndexDefinition.builder(TestTarget.class)
                .addKeyField(TestTarget_.id)
                .addKeyField(TestTarget_.code)
                .build();

        assertThat(def.getDepth()).isEqualTo(2);
    }

    @Test
    void build_withCustomComparator_createsIndexDefinition() {
        IndexDefinition<TestTarget> def = IndexDefinition.builder(TestTarget.class)
                .addKeyField(TestTarget_.code, Comparator.naturalOrder())
                .build();

        assertThat(def.getDepth()).isEqualTo(1);
    }

    @Test
    void build_withKeyFieldWithPath_createsIndexDefinition() {
        IndexDefinition<TestTarget> def = IndexDefinition.builder(TestTarget.class)
                .addKeyFieldWithPath(List.of(TestTarget_.id), t -> t)
                .build();

        assertThat(def.getDepth()).isEqualTo(1);
    }

    // ========== getComparatorForLevel ==========

    @Test
    void getComparatorForLevel_withInvalidNegativeLevel_throwsIndexOutOfBoundsException() {
        IndexDefinition<TestTarget> def = IndexDefinition.builder(TestTarget.class)
                .addKeyField(TestTarget_.id)
                .build();

        assertThatThrownBy(() -> def.getComparatorForLevel(-1))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessageContaining("Invalid level");
    }

    @Test
    void getComparatorForLevel_withLevelTooLarge_throwsIndexOutOfBoundsException() {
        IndexDefinition<TestTarget> def = IndexDefinition.builder(TestTarget.class)
                .addKeyField(TestTarget_.id)
                .build();

        assertThatThrownBy(() -> def.getComparatorForLevel(5))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessageContaining("Invalid level");
    }

    @Test
    void getComparatorForLevel_withValidLevel_returnsComparator() {
        IndexDefinition<TestTarget> def = IndexDefinition.builder(TestTarget.class)
                .addKeyField(TestTarget_.id)
                .build();

        assertThat(def.getComparatorForLevel(0)).isNotNull();
    }

    // ========== extractKeyValue ==========

    @Test
    void extractKeyValue_withInvalidNegativeLevel_throwsIndexOutOfBoundsException() {
        IndexDefinition<TestTarget> def = IndexDefinition.builder(TestTarget.class)
                .addKeyField(TestTarget_.id)
                .build();

        TestTarget entity = new TestTarget();
        entity.setId(1L);

        assertThatThrownBy(() -> def.extractKeyValue(entity, -1))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessageContaining("Invalid level");
    }

    @Test
    void extractKeyValue_withLevelTooLarge_throwsIndexOutOfBoundsException() {
        IndexDefinition<TestTarget> def = IndexDefinition.builder(TestTarget.class)
                .addKeyField(TestTarget_.id)
                .build();

        TestTarget entity = new TestTarget();
        entity.setId(1L);

        assertThatThrownBy(() -> def.extractKeyValue(entity, 5))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessageContaining("Invalid level");
    }

    // ========== null-safe comparator branches ==========

    @Test
    void comparator_withBothNullValues_returnsZero() {
        IndexDefinition<TestTarget> def = IndexDefinition.builder(TestTarget.class)
                .addKeyField(TestTarget_.id)
                .build();

        Comparator<Object> comparator = def.getComparatorForLevel(0);
        assertThat(comparator.compare(null, null)).isEqualTo(0);
    }

    @Test
    void comparator_withLeftNullValue_returnsNegative() {
        IndexDefinition<TestTarget> def = IndexDefinition.builder(TestTarget.class)
                .addKeyField(TestTarget_.id)
                .build();

        Comparator<Object> comparator = def.getComparatorForLevel(0);
        assertThat(comparator.compare(null, 1L)).isLessThan(0);
    }

    @Test
    void comparator_withRightNullValue_returnsPositive() {
        IndexDefinition<TestTarget> def = IndexDefinition.builder(TestTarget.class)
                .addKeyField(TestTarget_.id)
                .build();

        Comparator<Object> comparator = def.getComparatorForLevel(0);
        assertThat(comparator.compare(1L, null)).isGreaterThan(0);
    }

    @Test
    void comparator_withBothNonNullValues_comparesCorrectly() {
        IndexDefinition<TestTarget> def = IndexDefinition.builder(TestTarget.class)
                .addKeyField(TestTarget_.id)
                .build();

        Comparator<Object> comparator = def.getComparatorForLevel(0);
        assertThat(comparator.compare(1L, 2L)).isLessThan(0);
        assertThat(comparator.compare(2L, 1L)).isGreaterThan(0);
        assertThat(comparator.compare(1L, 1L)).isEqualTo(0);
    }

    // ========== null-safe comparator for custom comparator variant ==========

    @Test
    void customComparatorWithPath_bothNullValues_returnsZero() {
        IndexDefinition<TestTarget> def = IndexDefinition.builder(TestTarget.class)
                .addKeyField(TestTarget_.code, Comparator.naturalOrder())
                .build();

        Comparator<Object> comparator = def.getComparatorForLevel(0);
        assertThat(comparator.compare(null, null)).isEqualTo(0);
    }

    @Test
    void customComparatorWithPath_leftNullValue_returnsNegative() {
        IndexDefinition<TestTarget> def = IndexDefinition.builder(TestTarget.class)
                .addKeyField(TestTarget_.code, Comparator.naturalOrder())
                .build();

        Comparator<Object> comparator = def.getComparatorForLevel(0);
        assertThat(comparator.compare(null, "A")).isLessThan(0);
    }

    @Test
    void customComparatorWithPath_rightNullValue_returnsPositive() {
        IndexDefinition<TestTarget> def = IndexDefinition.builder(TestTarget.class)
                .addKeyField(TestTarget_.code, Comparator.naturalOrder())
                .build();

        Comparator<Object> comparator = def.getComparatorForLevel(0);
        assertThat(comparator.compare("A", null)).isGreaterThan(0);
    }

    @Test
    void customComparatorWithPath_bothNonNull_comparesCorrectly() {
        IndexDefinition<TestTarget> def = IndexDefinition.builder(TestTarget.class)
                .addKeyField(TestTarget_.code, Comparator.naturalOrder())
                .build();

        Comparator<Object> comparator = def.getComparatorForLevel(0);
        assertThat(comparator.compare("A", "B")).isLessThan(0);
    }
}
