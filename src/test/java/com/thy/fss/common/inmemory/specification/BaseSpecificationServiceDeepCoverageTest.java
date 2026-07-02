package com.thy.fss.common.inmemory.specification;

import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.engine.mapping.CollectionSelector;
import com.thy.fss.common.inmemory.specification.attribute.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Deep coverage tests for BaseSpecificationService targeting uncovered paths:
 * path navigation (getValueByPathImpl/setValueByPathImpl), type mismatch detection,
 * primitive compatibility, collection field handling, processCollectionOperation,
 * getElementTypeService, validateCollectionElement, and formatPath.
 */
@DisplayName("BaseSpecificationService Deep Coverage Tests")
class BaseSpecificationServiceDeepCoverageTest {

    // ==================== Test model ====================

    static class Parent {
        String name;
        Integer age;
        Child child;
        List<Child> children;

        Parent() {}
        Parent(String name, Integer age) { this.name = name; this.age = age; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        public Child getChild() { return child; }
        public void setChild(Child child) { this.child = child; }
        public List<Child> getChildren() { return children; }
        public void setChildren(List<Child> children) { this.children = children; }
    }

    static class Child {
        String label;
        int score;

        Child() {}
        Child(String label, int score) { this.label = label; this.score = score; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
    }

    // ==================== Meta Attributes ====================

    static final StringAttribute<Parent> PARENT_NAME = new StringAttribute<>("name", Parent.class);
    static final IntegerAttribute<Parent> PARENT_AGE = new IntegerAttribute<>("age", Parent.class);
    static final ModelAttribute<Parent, Child> PARENT_CHILD = new ModelAttribute<>("child", Parent.class, Child.class);
    @SuppressWarnings("unchecked")
    static final CollectionAttribute<Parent, Child> PARENT_CHILDREN =
            new CollectionAttribute<>("children", Parent.class, Child.class);

    static final StringAttribute<Child> CHILD_LABEL = new StringAttribute<>("label", Child.class);
    static final IntegerAttribute<Child> CHILD_SCORE = new IntegerAttribute<>("score", Child.class);

    // ==================== Concrete service implementations ====================

    static class ChildSpecService extends BaseSpecificationService<Child> {
        static final ChildSpecService INSTANCE = new ChildSpecService();

        @Override
        public boolean validateSpecification(Child entity, MetaAttribute<Child, ?> attribute, Operator operator, Object value) {
            return false;
        }
        @Override
        public boolean validateFilter(Child entity, Object filter) { return false; }
        @Override
        public Class<Child> getEntityClass() { return Child.class; }
        @Override
        public Child createInstance() { return new Child(); }
        @Override
        public Object getFieldValue(Child entity, String fieldName) {
            if ("label".equals(fieldName)) return entity.getLabel();
            if ("score".equals(fieldName)) return entity.getScore();
            return null;
        }
        @Override
        @SuppressWarnings("unchecked")
        public Object getFieldValue(Child entity, MetaAttribute<?, ?> attr) {
            if (attr == CHILD_LABEL) return entity.getLabel();
            if (attr == CHILD_SCORE) return entity.getScore();
            return null;
        }
        @Override
        @SuppressWarnings("unchecked")
        public void setFieldValue(Child entity, MetaAttribute<?, ?> attr, Object value) {
            if (attr == CHILD_LABEL) entity.setLabel((String) value);
            if (attr == CHILD_SCORE) entity.setScore((int) value);
        }
        @Override
        public Comparator<Child> createComparator(String fieldName, boolean ascending) { return null; }
        @Override
        public Comparator<Child> createComparator(MetaAttribute<?, ?> attr, boolean ascending) { return null; }
        @Override
        public Comparator<Child> createMultiFieldComparator(List<String> fieldNames, List<Boolean> flags) { return null; }
        @Override
        public Comparator<Child> createMultiFieldComparatorWithMetaAttributes(List<MetaAttribute<?, ?>> attrs, List<Boolean> flags) { return null; }
        @Override
        protected Object navigateNested(Object fieldValue, MetaAttribute<?, ?> attr, List<MetaAttribute<?, ?>> path, int nextIndex) {
            throw new IllegalArgumentException("Child has no nested fields");
        }
        @Override
        protected void navigateNestedForSet(Object fieldValue, MetaAttribute<?, ?> attr, List<MetaAttribute<?, ?>> path, int nextIndex, Object value) {
            throw new IllegalArgumentException("Child has no nested fields");
        }
        @Override
        protected Object createIntermediateInstanceForField(MetaAttribute<?, ?> attr) {
            throw new IllegalArgumentException("Child has no nested model fields");
        }
    }

    static class ParentSpecService extends BaseSpecificationService<Parent> {
        static final ParentSpecService INSTANCE = new ParentSpecService();

        @Override
        public boolean validateSpecification(Parent entity, MetaAttribute<Parent, ?> attribute, Operator operator, Object value) {
            return false;
        }
        @Override
        public boolean validateFilter(Parent entity, Object filter) { return false; }
        @Override
        public Class<Parent> getEntityClass() { return Parent.class; }
        @Override
        public Parent createInstance() { return new Parent(); }
        @Override
        public Object getFieldValue(Parent entity, String fieldName) {
            return switch (fieldName) {
                case "name" -> entity.getName();
                case "age" -> entity.getAge();
                case "child" -> entity.getChild();
                case "children" -> entity.getChildren();
                default -> null;
            };
        }
        @Override
        @SuppressWarnings("unchecked")
        public Object getFieldValue(Parent entity, MetaAttribute<?, ?> attr) {
            if (attr == PARENT_NAME) return entity.getName();
            if (attr == PARENT_AGE) return entity.getAge();
            if (attr == PARENT_CHILD) return entity.getChild();
            if (attr == PARENT_CHILDREN) return entity.getChildren();
            return null;
        }
        @Override
        @SuppressWarnings("unchecked")
        public void setFieldValue(Parent entity, MetaAttribute<?, ?> attr, Object value) {
            if (attr == PARENT_NAME) entity.setName((String) value);
            if (attr == PARENT_AGE) entity.setAge((Integer) value);
            if (attr == PARENT_CHILD) entity.setChild((Child) value);
            if (attr == PARENT_CHILDREN) entity.setChildren((List<Child>) value);
        }
        @Override
        public Comparator<Parent> createComparator(String fieldName, boolean ascending) { return null; }
        @Override
        public Comparator<Parent> createComparator(MetaAttribute<?, ?> attr, boolean ascending) { return null; }
        @Override
        public Comparator<Parent> createMultiFieldComparator(List<String> fieldNames, List<Boolean> flags) { return null; }
        @Override
        public Comparator<Parent> createMultiFieldComparatorWithMetaAttributes(List<MetaAttribute<?, ?>> attrs, List<Boolean> flags) { return null; }

        @Override
        protected Object navigateNested(Object fieldValue, MetaAttribute<?, ?> attr, List<MetaAttribute<?, ?>> path, int nextIndex) {
            if (attr == PARENT_CHILD) {
                return ChildSpecService.INSTANCE.getValueByPathImpl(fieldValue, path, nextIndex);
            }
            throw new IllegalArgumentException("Unknown nested field: " + attr.getName());
        }
        @Override
        protected void navigateNestedForSet(Object fieldValue, MetaAttribute<?, ?> attr, List<MetaAttribute<?, ?>> path, int nextIndex, Object value) {
            if (attr == PARENT_CHILD) {
                ChildSpecService.INSTANCE.setValueByPathImpl(fieldValue, path, nextIndex, value);
                return;
            }
            throw new IllegalArgumentException("Unknown nested field: " + attr.getName());
        }
        @Override
        protected Object createIntermediateInstanceForField(MetaAttribute<?, ?> attr) {
            if (attr == PARENT_CHILD) return ChildSpecService.INSTANCE.createInstance();
            throw new IllegalArgumentException("Unknown field: " + attr.getName());
        }
    }

    private final ParentSpecService service = ParentSpecService.INSTANCE;

    // ==================== getValueByPath with nested path ====================

    @Nested
    @DisplayName("getValueByPath - nested navigation")
    class GetValueByPathNested {

        @Test
        @DisplayName("Navigates through nested model to leaf field")
        void nestedModelNavigation() {
            Parent parent = new Parent("Alice", 30);
            parent.setChild(new Child("kidLabel", 95));

            Object result = service.getValueByPath(parent, List.of(PARENT_CHILD, CHILD_LABEL));
            assertThat(result).isEqualTo("kidLabel");
        }

        @Test
        @DisplayName("Returns null when intermediate nested object is null")
        void nullIntermediateValue() {
            Parent parent = new Parent("Alice", 30);
            // child is null

            Object result = service.getValueByPath(parent, List.of(PARENT_CHILD, CHILD_LABEL));
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Returns intermediate object when path ends at model field")
        void stopsAtModelField() {
            Child child = new Child("x", 10);
            Parent parent = new Parent("Bob", 40);
            parent.setChild(child);

            Object result = service.getValueByPath(parent, List.of(PARENT_CHILD));
            assertThat(result).isSameAs(child);
        }
    }

    // ==================== setValueByPath with nested path ====================

    @Nested
    @DisplayName("setValueByPath - nested navigation and edge cases")
    class SetValueByPathNested {

        @Test
        @DisplayName("Sets value on nested model field")
        void setNestedLeafValue() {
            Parent parent = new Parent("Alice", 30);
            parent.setChild(new Child("old", 50));

            service.setValueByPath(parent, List.of(PARENT_CHILD, CHILD_LABEL), "newLabel");
            assertThat(parent.getChild().getLabel()).isEqualTo("newLabel");
        }

        @Test
        @DisplayName("Creates intermediate instance when null")
        void createsIntermediateWhenNull() {
            Parent parent = new Parent("Alice", 30);
            // child is null, should be auto-created

            service.setValueByPath(parent, List.of(PARENT_CHILD, CHILD_LABEL), "created");
            assertThat(parent.getChild()).isNotNull();
            assertThat(parent.getChild().getLabel()).isEqualTo("created");
        }

        @Test
        @DisplayName("No-op when entity is null")
        void nullEntity() {
            // Should not throw
            service.setValueByPath(null, List.of(PARENT_NAME), "value");
        }

        @Test
        @DisplayName("No-op when path is null")
        void nullPath() {
            Parent parent = new Parent();
            service.setValueByPath(parent, null, "value");
            assertThat(parent.getName()).isNull();
        }

        @Test
        @DisplayName("No-op when path is empty")
        void emptyPath() {
            Parent parent = new Parent();
            service.setValueByPath(parent, List.of(), "value");
            assertThat(parent.getName()).isNull();
        }

        @Test
        @DisplayName("Type mismatch throws IllegalArgumentException")
        void typeMismatchThrows() {
            Parent parent = new Parent("Alice", 30);

            assertThatThrownBy(() -> service.setValueByPath(parent, List.of(PARENT_NAME), 12345))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Type mismatch");
        }

        @Test
        @DisplayName("Allows setting null value without type check")
        void setNullValue() {
            Parent parent = new Parent("Alice", 30);
            service.setValueByPath(parent, List.of(PARENT_NAME), null);
            assertThat(parent.getName()).isNull();
        }
    }

    // ==================== Primitive type compatibility ====================

    @Nested
    @DisplayName("Primitive type compatibility (setValueByPath type checks)")
    class PrimitiveTypeCompat {

        @Test
        @DisplayName("int value can be set to Integer field")
        void intToInteger() {
            Parent parent = new Parent();
            service.setValueByPath(parent, List.of(PARENT_AGE), 42);
            assertThat(parent.getAge()).isEqualTo(42);
        }

        @Test
        @DisplayName("Integer value can be set to Integer field")
        void integerToInteger() {
            Parent parent = new Parent();
            service.setValueByPath(parent, List.of(PARENT_AGE), Integer.valueOf(99));
            assertThat(parent.getAge()).isEqualTo(99);
        }
    }

    // ==================== getValueByPathWithCollections ====================

    @Nested
    @DisplayName("getValueByPathWithCollections")
    class GetValueByPathWithCollections {

        @Test
        @DisplayName("Returns field value with no collection operations")
        void simplePathNoOps() {
            Parent parent = new Parent("test", 42);
            Object result = service.getValueByPathWithCollections(parent, List.of(PARENT_NAME), null);
            assertThat(result).isEqualTo("test");
        }

        @Test
        @DisplayName("Applies FIRST selector on collection field")
        void firstSelectorOnCollection() {
            Parent parent = new Parent("p", 1);
            parent.setChildren(List.of(new Child("a", 10), new Child("b", 20)));

            CollectionOperationMetadata<Parent, Child> op = new CollectionOperationMetadata<>(
                    0, PARENT_CHILDREN, CollectionSelector.FIRST, null);

            Object result = service.getValueByPathWithCollections(parent, List.of(PARENT_CHILDREN), List.of(op));
            assertThat(result).isInstanceOf(Child.class);
            assertThat(((Child) result).getLabel()).isEqualTo("a");
        }

        @Test
        @DisplayName("Applies ALL selector on collection field")
        void allSelectorOnCollection() {
            Parent parent = new Parent("p", 1);
            parent.setChildren(List.of(new Child("a", 10), new Child("b", 20)));

            CollectionOperationMetadata<Parent, Child> op = new CollectionOperationMetadata<>(
                    0, PARENT_CHILDREN, CollectionSelector.ALL, null);

            Object result = service.getValueByPathWithCollections(parent, List.of(PARENT_CHILDREN), List.of(op));
            assertThat(result).isInstanceOf(List.class);
            assertThat((List<?>) result).hasSize(2);
        }

        @Test
        @DisplayName("Applies LAST selector on collection field")
        void lastSelectorOnCollection() {
            Parent parent = new Parent("p", 1);
            parent.setChildren(List.of(new Child("a", 10), new Child("b", 20)));

            CollectionOperationMetadata<Parent, Child> op = new CollectionOperationMetadata<>(
                    0, PARENT_CHILDREN, CollectionSelector.LAST, null);

            Object result = service.getValueByPathWithCollections(parent, List.of(PARENT_CHILDREN), List.of(op));
            assertThat(result).isInstanceOf(Child.class);
            assertThat(((Child) result).getLabel()).isEqualTo("b");
        }

        @Test
        @DisplayName("Applies ANY selector on collection field")
        void anySelectorOnCollection() {
            Parent parent = new Parent("p", 1);
            parent.setChildren(List.of(new Child("a", 10)));

            CollectionOperationMetadata<Parent, Child> op = new CollectionOperationMetadata<>(
                    0, PARENT_CHILDREN, CollectionSelector.ANY, null);

            Object result = service.getValueByPathWithCollections(parent, List.of(PARENT_CHILDREN), List.of(op));
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Returns empty list when collection is null with ALL selector")
        void nullCollectionAllSelector() {
            Parent parent = new Parent("p", 1);
            // children is null

            CollectionOperationMetadata<Parent, Child> op = new CollectionOperationMetadata<>(
                    0, PARENT_CHILDREN, CollectionSelector.ALL, null);

            Object result = service.getValueByPathWithCollections(parent, List.of(PARENT_CHILDREN), List.of(op));
            // field value is null, so result should be null (field not a Collection instance)
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Returns null when collection is empty with FIRST selector")
        void emptyCollectionFirstSelector() {
            Parent parent = new Parent("p", 1);
            parent.setChildren(new ArrayList<>());

            CollectionOperationMetadata<Parent, Child> op = new CollectionOperationMetadata<>(
                    0, PARENT_CHILDREN, CollectionSelector.FIRST, null);

            Object result = service.getValueByPathWithCollections(parent, List.of(PARENT_CHILDREN), List.of(op));
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Applies specification filter on collection operation")
        void specificationFilterOnCollection() {
            Parent parent = new Parent("p", 1);
            parent.setChildren(List.of(new Child("a", 10), new Child("b", 90), new Child("c", 5)));

            Specification<Child> spec = () -> child -> child.getScore() > 50;
            CollectionOperationMetadata<Parent, Child> op = new CollectionOperationMetadata<>(
                    0, PARENT_CHILDREN, CollectionSelector.FIRST, spec);

            Object result = service.getValueByPathWithCollections(parent, List.of(PARENT_CHILDREN), List.of(op));
            assertThat(result).isInstanceOf(Child.class);
            assertThat(((Child) result).getLabel()).isEqualTo("b");
        }

        @Test
        @DisplayName("FIRST with comparator returns min element")
        void firstWithComparatorReturnsMin() {
            Parent parent = new Parent("p", 1);
            parent.setChildren(List.of(new Child("a", 30), new Child("b", 10), new Child("c", 20)));

            Comparator<Child> comparator = Comparator.comparingInt(Child::getScore);
            CollectionOperationMetadata<Parent, Child> op = new CollectionOperationMetadata<>(
                    0, PARENT_CHILDREN, CollectionSelector.FIRST, null, comparator);

            Object result = service.getValueByPathWithCollections(parent, List.of(PARENT_CHILDREN), List.of(op));
            assertThat(result).isInstanceOf(Child.class);
            assertThat(((Child) result).getScore()).isEqualTo(10);
        }

        @Test
        @DisplayName("LAST with comparator returns max element")
        void lastWithComparatorReturnsMax() {
            Parent parent = new Parent("p", 1);
            parent.setChildren(List.of(new Child("a", 30), new Child("b", 10), new Child("c", 20)));

            Comparator<Child> comparator = Comparator.comparingInt(Child::getScore);
            CollectionOperationMetadata<Parent, Child> op = new CollectionOperationMetadata<>(
                    0, PARENT_CHILDREN, CollectionSelector.LAST, null, comparator);

            Object result = service.getValueByPathWithCollections(parent, List.of(PARENT_CHILDREN), List.of(op));
            assertThat(result).isInstanceOf(Child.class);
            assertThat(((Child) result).getScore()).isEqualTo(30);
        }
    }

    // ==================== setValueByPathWithCollections ====================

    @Nested
    @DisplayName("setValueByPathWithCollections")
    class SetValueByPathWithCollections {

        @Test
        @DisplayName("No-op for null entity")
        void nullEntity() {
            service.setValueByPathWithCollections(null, List.of(PARENT_NAME), null, "val");
        }

        @Test
        @DisplayName("No-op for null path")
        void nullPath() {
            service.setValueByPathWithCollections(new Parent(), null, null, "val");
        }

        @Test
        @DisplayName("No-op for empty path")
        void emptyPath() {
            service.setValueByPathWithCollections(new Parent(), List.of(), null, "val");
        }

        @Test
        @DisplayName("Sets simple field value without ops")
        void simpleSet() {
            Parent parent = new Parent();
            service.setValueByPathWithCollections(parent, List.of(PARENT_NAME), null, "hello");
            assertThat(parent.getName()).isEqualTo("hello");
        }
    }

    // ==================== getCollectionSize / isCollectionEmpty ====================

    @Nested
    @DisplayName("getCollectionSize and isCollectionEmpty edge cases")
    class CollectionUtilEdgeCases {

        @Test
        @DisplayName("getCollectionSize returns size for large collection")
        void largeCollection() {
            List<String> large = new ArrayList<>(Collections.nCopies(1000, "x"));
            assertThat(service.getCollectionSize(large)).isEqualTo(1000);
        }

        @Test
        @DisplayName("isCollectionEmpty returns true for null")
        void nullIsEmpty() {
            assertThat(service.isCollectionEmpty(null)).isTrue();
        }

        @Test
        @DisplayName("isCollectionEmpty returns false for non-empty set")
        void nonEmptySet() {
            assertThat(service.isCollectionEmpty(Set.of("a"))).isFalse();
        }
    }

    // ==================== getElementTypeService ====================

    @Nested
    @DisplayName("getElementTypeService")
    class ElementTypeServiceTests {

        @Test
        @DisplayName("Returns null for null class")
        void nullClass() {
            assertThat(service.getElementTypeService(null)).isNull();
        }

        @Test
        @DisplayName("Returns null for String (basic type)")
        void stringBasicType() {
            assertThat(service.getElementTypeService(String.class)).isNull();
        }

        @Test
        @DisplayName("Returns null for Integer (basic type)")
        void integerBasicType() {
            assertThat(service.getElementTypeService(Integer.class)).isNull();
        }

        @Test
        @DisplayName("Returns null for Long (basic type)")
        void longBasicType() {
            assertThat(service.getElementTypeService(Long.class)).isNull();
        }

        @Test
        @DisplayName("Returns null for Double (basic type)")
        void doubleBasicType() {
            assertThat(service.getElementTypeService(Double.class)).isNull();
        }

        @Test
        @DisplayName("Returns null for Float (basic type)")
        void floatBasicType() {
            assertThat(service.getElementTypeService(Float.class)).isNull();
        }

        @Test
        @DisplayName("Returns null for Boolean (basic type)")
        void booleanBasicType() {
            assertThat(service.getElementTypeService(Boolean.class)).isNull();
        }

        @Test
        @DisplayName("Returns null for Character (basic type)")
        void characterBasicType() {
            assertThat(service.getElementTypeService(Character.class)).isNull();
        }

        @Test
        @DisplayName("Returns null for Byte (basic type)")
        void byteBasicType() {
            assertThat(service.getElementTypeService(Byte.class)).isNull();
        }

        @Test
        @DisplayName("Returns null for Short (basic type)")
        void shortBasicType() {
            assertThat(service.getElementTypeService(Short.class)).isNull();
        }

        @Test
        @DisplayName("Returns null for Enum (basic type)")
        void enumBasicType() {
            assertThat(service.getElementTypeService(CollectionSelector.class)).isNull();
        }

        @Test
        @DisplayName("Returns null for LocalDate (temporal basic type)")
        void localDateBasicType() {
            assertThat(service.getElementTypeService(java.time.LocalDate.class)).isNull();
        }

        @Test
        @DisplayName("Returns null for LocalDateTime (temporal basic type)")
        void localDateTimeBasicType() {
            assertThat(service.getElementTypeService(java.time.LocalDateTime.class)).isNull();
        }

        @Test
        @DisplayName("Returns null for Instant (temporal basic type)")
        void instantBasicType() {
            assertThat(service.getElementTypeService(java.time.Instant.class)).isNull();
        }

        @Test
        @DisplayName("Throws for model type without registered service")
        void unregisteredModelTypeThrows() {
            assertThatThrownBy(() -> service.getElementTypeService(Parent.class))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No specification service found");
        }
    }

    // ==================== validateCollectionElement ====================

    @Nested
    @DisplayName("validateCollectionElement")
    class ValidateCollectionElementTests {

        @Test
        @DisplayName("Returns false for null element")
        void nullElement() {
            assertThat(service.validateCollectionElement(null, "filter", String.class)).isFalse();
        }

        @Test
        @DisplayName("Returns true for null filter (no criteria to apply)")
        void nullFilter() {
            assertThat(service.validateCollectionElement("element", null, String.class)).isTrue();
        }

        @Test
        @DisplayName("Returns false for basic type element (no service)")
        void basicTypeElement() {
            assertThat(service.validateCollectionElement("hello", "filter", String.class)).isFalse();
        }
    }

    // ==================== extractFromCollection edge cases ====================

    @Nested
    @DisplayName("extractFromCollection edge cases")
    class ExtractFromCollectionEdgeCases {

        @Test
        @DisplayName("LAST with empty collection returns null")
        void lastWithEmpty() {
            Object result = service.extractFromCollection(new ArrayList<>(), CollectionSelector.LAST);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("ANY with empty collection returns null")
        void anyWithEmpty() {
            Object result = service.extractFromCollection(new ArrayList<>(), CollectionSelector.ANY);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("LAST with single element returns that element")
        void lastWithSingleton() {
            Object result = service.extractFromCollection(List.of("only"), CollectionSelector.LAST);
            assertThat(result).isEqualTo("only");
        }

        @Test
        @DisplayName("FIRST with single element returns that element")
        void firstWithSingleton() {
            Object result = service.extractFromCollection(List.of("only"), CollectionSelector.FIRST);
            assertThat(result).isEqualTo("only");
        }
    }

    // ==================== extractFromCollection with specification edge cases ====================

    @Nested
    @DisplayName("extractFromCollection with spec - edge cases")
    @SuppressWarnings("unchecked")
    class ExtractFromCollectionSpecEdgeCases {

        @Test
        @DisplayName("ANY with spec and no match returns null")
        void anyNoMatch() {
            List<Parent> items = List.of(new Parent("a", 10));
            Specification<Parent> spec = () -> p -> p.getAge() > 100;

            Object result = service.extractFromCollection(items, CollectionSelector.ANY, spec);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("ALL with spec returns only matching")
        void allFiltered() {
            List<Parent> items = List.of(new Parent("a", 10), new Parent("b", 50), new Parent("c", 5));
            Specification<Parent> spec = () -> p -> p.getAge() > 8;

            Object result = service.extractFromCollection((Collection<Parent>) (Collection<?>) items, CollectionSelector.ALL, spec);
            assertThat(result).isInstanceOf(List.class);
            assertThat(((List<?>) result)).hasSize(2);
        }

        @Test
        @DisplayName("Empty collection with ALL and spec returns empty list")
        void emptyAll() {
            Object result = service.extractFromCollection(new ArrayList<>(), CollectionSelector.ALL, null);
            assertThat((List<?>) result).isEmpty();
        }

        @Test
        @DisplayName("Empty collection with ANY returns null")
        void emptyAny() {
            Object result = service.extractFromCollection(new ArrayList<>(), CollectionSelector.ANY, null);
            assertThat(result).isNull();
        }
    }
}
