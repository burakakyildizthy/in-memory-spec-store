package com.thy.fss.common.inmemory.specification;

import com.thy.fss.common.inmemory.engine.mapping.CollectionSelector;
import com.thy.fss.common.inmemory.specification.attribute.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Additional coverage tests for BaseSpecificationService to reach 80%+ line coverage.
 * Focuses on: extractFromCollection with specification, getValueByPath, setValueByPath,
 * getValueByPathWithCollections, setValueByPathWithCollections, getElementTypeService,
 * validateCollectionElement, and private helper methods.
 */
@DisplayName("BaseSpecificationService Coverage Tests")
class BaseSpecificationServiceCoverageTest {

    // Simple entity for testing
    static class Item {
        String name;
        Integer price;
        Item nested;
        List<Item> children;

        Item() {}
        Item(String name, Integer price) { this.name = name; this.price = price; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getPrice() { return price; }
        public void setPrice(Integer price) { this.price = price; }
        public Item getNested() { return nested; }
        public void setNested(Item nested) { this.nested = nested; }
        public List<Item> getChildren() { return children; }
        public void setChildren(List<Item> children) { this.children = children; }
    }

    static final StringAttribute<Item> NAME_ATTR = new StringAttribute<>("name", Item.class);
    static final IntegerAttribute<Item> PRICE_ATTR = new IntegerAttribute<>("price", Item.class);

    // Concrete implementation for testing
    static class ItemSpecService extends BaseSpecificationService<Item> {

        static final ItemSpecService INSTANCE = new ItemSpecService();

        @Override
        public boolean validateSpecification(Item entity, MetaAttribute<Item, ?> attribute, Operator operator, Object value) {
            if (attribute == NAME_ATTR) {
                String fieldValue = entity.getName();
                if (operator == Operator.EQUALS) return Objects.equals(fieldValue, value);
                if (operator == Operator.IS_NULL) return fieldValue == null;
            }
            if (attribute == PRICE_ATTR) {
                Integer fieldValue = entity.getPrice();
                if (operator == Operator.EQUALS) return Objects.equals(fieldValue, value);
                if (operator == Operator.GREATER_THAN) return fieldValue != null && fieldValue > (Integer) value;
            }
            return false;
        }

        @Override
        public boolean validateFilter(Item entity, Object filter) { return false; }

        @Override
        public Class<Item> getEntityClass() { return Item.class; }

        @Override
        public Item createInstance() { return new Item(); }

        @Override
        public Object getFieldValue(Item entity, String fieldName) {
            if ("name".equals(fieldName)) return entity.getName();
            if ("price".equals(fieldName)) return entity.getPrice();
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object getFieldValue(Item entity, MetaAttribute<?, ?> attr) {
            if (attr == NAME_ATTR) return entity.getName();
            if (attr == PRICE_ATTR) return entity.getPrice();
            return null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void setFieldValue(Item entity, MetaAttribute<?, ?> attr, Object value) {
            if (attr == NAME_ATTR) entity.setName((String) value);
            if (attr == PRICE_ATTR) entity.setPrice((Integer) value);
        }

        @Override
        public Comparator<Item> createComparator(String fieldName, boolean ascending) { return null; }

        @Override
        public Comparator<Item> createComparator(MetaAttribute<?, ?> attr, boolean ascending) { return null; }

        @Override
        public Comparator<Item> createMultiFieldComparator(List<String> fieldNames, List<Boolean> ascendingFlags) { return null; }

        @Override
        public Comparator<Item> createMultiFieldComparatorWithMetaAttributes(List<MetaAttribute<?, ?>> attrs, List<Boolean> flags) { return null; }

        @Override
        protected Object navigateNested(Object fieldValue, MetaAttribute<?, ?> attr, List<MetaAttribute<?, ?>> path, int nextIndex) {
            return null;
        }

        @Override
        protected void navigateNestedForSet(Object fieldValue, MetaAttribute<?, ?> attr, List<MetaAttribute<?, ?>> path, int nextIndex, Object value) {
            // no-op
        }

        @Override
        protected Object createIntermediateInstanceForField(MetaAttribute<?, ?> attr) {
            return new Item();
        }
    }

    private final ItemSpecService service = ItemSpecService.INSTANCE;

    // ==================== extractFromCollection with specification ====================

    @Nested
    @DisplayName("extractFromCollection with specification")
    class ExtractWithSpec {

        @Test
        @DisplayName("ALL with specification filters elements")
        void allWithSpec() {
            List<Item> items = List.of(new Item("a", 10), new Item("b", 20), new Item("c", 30));
            Specification<Item> spec = () -> item -> item.getPrice() > 15;

            Object result = service.extractFromCollection(items, CollectionSelector.ALL, spec);
            assertThat(result).isInstanceOf(List.class);
            assertThat((List<?>) result).hasSize(2);
        }

        @Test
        @DisplayName("FIRST with specification returns first matching")
        void firstWithSpec() {
            List<Item> items = List.of(new Item("a", 10), new Item("b", 20), new Item("c", 30));
            Specification<Item> spec = () -> item -> item.getPrice() > 15;

            Object result = service.extractFromCollection(items, CollectionSelector.FIRST, spec);
            assertThat(result).isNotNull();
            assertThat(((Item) result).getName()).isEqualTo("b");
        }

        @Test
        @DisplayName("LAST with specification returns last matching")
        void lastWithSpec() {
            List<Item> items = List.of(new Item("a", 10), new Item("b", 20), new Item("c", 30));
            Specification<Item> spec = () -> item -> item.getPrice() > 15;

            Object result = service.extractFromCollection(items, CollectionSelector.LAST, spec);
            assertThat(result).isNotNull();
            assertThat(((Item) result).getName()).isEqualTo("c");
        }

        @Test
        @DisplayName("ANY with specification returns any matching")
        void anyWithSpec() {
            List<Item> items = List.of(new Item("a", 10), new Item("b", 20));
            Specification<Item> spec = () -> item -> item.getPrice() > 15;

            Object result = service.extractFromCollection(items, CollectionSelector.ANY, spec);
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Null collection with ALL returns empty list")
        void nullCollectionAll() {
            Object result = service.extractFromCollection(null, CollectionSelector.ALL, null);
            assertThat((List<?>) result).isEmpty();
        }

        @Test
        @DisplayName("Null collection with FIRST returns null")
        void nullCollectionFirst() {
            Object result = service.extractFromCollection(null, CollectionSelector.FIRST, null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Empty collection with LAST returns null")
        void emptyCollectionLast() {
            Object result = service.extractFromCollection(new ArrayList<>(), CollectionSelector.LAST, null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Null specification passes all elements")
        void nullSpec() {
            List<Item> items = List.of(new Item("a", 10), new Item("b", 20));
            Object result = service.extractFromCollection(items, CollectionSelector.ALL, null);
            assertThat((List<?>) result).hasSize(2);
        }

        @Test
        @DisplayName("LAST with no matches returns null")
        void lastNoMatch() {
            List<Item> items = List.of(new Item("a", 10));
            Specification<Item> spec = () -> item -> item.getPrice() > 100;

            Object result = service.extractFromCollection(items, CollectionSelector.LAST, spec);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("FIRST with no matches returns null")
        void firstNoMatch() {
            List<Item> items = List.of(new Item("a", 10));
            Specification<Item> spec = () -> item -> item.getPrice() > 100;

            Object result = service.extractFromCollection(items, CollectionSelector.FIRST, spec);
            assertThat(result).isNull();
        }
    }

    // ==================== getValueByPath ====================

    @Nested
    @DisplayName("getValueByPath")
    class GetValueByPath {

        @Test
        @DisplayName("Returns null for null entity")
        void nullEntity() {
            assertThat(service.getValueByPath(null, List.of(NAME_ATTR))).isNull();
        }

        @Test
        @DisplayName("Returns null for null path")
        void nullPath() {
            assertThat(service.getValueByPath(new Item("a", 1), null)).isNull();
        }

        @Test
        @DisplayName("Returns null for empty path")
        void emptyPath() {
            assertThat(service.getValueByPath(new Item("a", 1), List.of())).isNull();
        }

        @Test
        @DisplayName("Returns field value for single-element path")
        void singleElementPath() {
            Item item = new Item("test", 42);
            Object result = service.getValueByPath(item, List.of(NAME_ATTR));
            assertThat(result).isEqualTo("test");
        }

        @Test
        @DisplayName("Returns null when field value is null")
        void nullFieldValue() {
            Item item = new Item(null, 42);
            Object result = service.getValueByPath(item, List.of(NAME_ATTR));
            assertThat(result).isNull();
        }
    }

    // ==================== setValueByPath ====================

    @Nested
    @DisplayName("setValueByPath")
    class SetValueByPath {

        @Test
        @DisplayName("Sets value for single-element path")
        void singleElementPath() {
            Item item = new Item();
            service.setValueByPath(item, List.of(NAME_ATTR), "hello");
            assertThat(item.getName()).isEqualTo("hello");
        }

        @Test
        @DisplayName("Sets integer value")
        void setIntegerValue() {
            Item item = new Item();
            service.setValueByPath(item, List.of(PRICE_ATTR), 99);
            assertThat(item.getPrice()).isEqualTo(99);
        }
    }

    // ==================== getValueByPathWithCollections ====================

    @Nested
    @DisplayName("getValueByPathWithCollections")
    class GetValueByPathWithCollections {

        @Test
        @DisplayName("Returns null for null entity")
        void nullEntity() {
            assertThat(service.getValueByPathWithCollections(null, List.of(NAME_ATTR), null)).isNull();
        }

        @Test
        @DisplayName("Returns null for null path")
        void nullPath() {
            assertThat(service.getValueByPathWithCollections(new Item(), null, null)).isNull();
        }

        @Test
        @DisplayName("Returns null for empty path")
        void emptyPath() {
            assertThat(service.getValueByPathWithCollections(new Item(), List.of(), null)).isNull();
        }

        @Test
        @DisplayName("Returns field value with null collection operations")
        void nullCollectionOps() {
            Item item = new Item("test", 42);
            Object result = service.getValueByPathWithCollections(item, List.of(NAME_ATTR), null);
            assertThat(result).isEqualTo("test");
        }

        @Test
        @DisplayName("Returns field value with empty collection operations")
        void emptyCollectionOps() {
            Item item = new Item("test", 42);
            Object result = service.getValueByPathWithCollections(item, List.of(NAME_ATTR), List.of());
            assertThat(result).isEqualTo("test");
        }
    }

    // ==================== setValueByPathWithCollections ====================

    @Nested
    @DisplayName("setValueByPathWithCollections")
    class SetValueByPathWithCollections {

        @Test
        @DisplayName("Sets value for single element path without collection ops")
        void singlePath() {
            Item item = new Item();
            service.setValueByPathWithCollections(item, List.of(NAME_ATTR), null, "world");
            assertThat(item.getName()).isEqualTo("world");
        }
    }

    // ==================== getCollectionSize / isCollectionEmpty edge cases ====================

    @Nested
    @DisplayName("Collection utility methods")
    class CollectionUtils {

        @Test
        @DisplayName("getCollectionSize with singleton")
        void sizeOne() {
            assertThat(service.getCollectionSize(List.of("a"))).isEqualTo(1);
        }

        @Test
        @DisplayName("isCollectionEmpty with singleton returns false")
        void notEmpty() {
            assertThat(service.isCollectionEmpty(List.of("a"))).isFalse();
        }

        @Test
        @DisplayName("extractFromCollection LAST with single element")
        void lastSingle() {
            Object result = service.extractFromCollection(List.of("only"), CollectionSelector.LAST);
            assertThat(result).isEqualTo("only");
        }

        @Test
        @DisplayName("extractFromCollection ANY with empty returns null")
        void anyEmpty() {
            Object result = service.extractFromCollection(new ArrayList<>(), CollectionSelector.ANY);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("extractFromCollection LAST with null returns null")
        void lastNull() {
            Object result = service.extractFromCollection(null, CollectionSelector.LAST);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("extractFromCollection ANY with null returns null")
        void anyNull() {
            Object result = service.extractFromCollection(null, CollectionSelector.ANY);
            assertThat(result).isNull();
        }
    }
}
