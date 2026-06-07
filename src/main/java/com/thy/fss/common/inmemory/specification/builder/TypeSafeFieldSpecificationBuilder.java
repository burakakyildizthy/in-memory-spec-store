package com.thy.fss.common.inmemory.specification.builder;

import com.thy.fss.common.inmemory.specification.Operator;
import com.thy.fss.common.inmemory.specification.Specification;
import com.thy.fss.common.inmemory.specification.SpecificationService;
import com.thy.fss.common.inmemory.specification.attribute.*;
import com.thy.fss.common.inmemory.filter.TemporalPreset;
import com.thy.fss.common.inmemory.filter.TemporalPresetParser;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Type-safe field specification builders that provide only appropriate
 * operations for each field type. This eliminates the possibility of using
 * incompatible operations (e.g., string operations on numeric fields).
 */
public class TypeSafeFieldSpecificationBuilder {

    // ==================== STRING FIELD BUILDER ====================
    /**
     * Interface for handling created specifications. Allows different return
     * types for different contexts.
     */
    @FunctionalInterface
    public interface SpecificationHandler<T, R> {

        R handle(Specification<T> specification);
    }

    // ==================== NUMERIC FIELD BUILDER ====================
    /**
     * Builder for String field specifications. Provides only string-appropriate
     * operations.
     */
    public static class StringFieldBuilder<T, R> extends BaseOperations<T, String, R> {

        public StringFieldBuilder(StringAttribute<T> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler) {
            super(attribute, service, handler);
        }

        public StringFieldBuilder(StringAttribute<T> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler, java.util.List<MetaAttribute<?, ?>> fieldPath) {
            super(attribute, service, handler, fieldPath);
        }

        public StringFieldBuilder(StringAttribute<T> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler, java.util.List<MetaAttribute<?, ?>> fieldPath, java.util.List<com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> collectionOperations) {
            super(attribute, service, handler, fieldPath, collectionOperations);
        }

        // String-specific operations
        public R contains(String value) {
            return handleSpecification(createSpecification(Operator.CONTAINS, value));
        }

        public R startsWith(String prefix) {
            return handleSpecification(createSpecification(Operator.STARTS_WITH, prefix));
        }

        public R endsWith(String suffix) {
            return handleSpecification(createSpecification(Operator.ENDS_WITH, suffix));
        }

        public R matches(String regex) {
            return handleSpecification(createSpecification(Operator.MATCHES, regex));
        }

        public R matches(Pattern pattern) {
            return handleSpecification(createSpecification(Operator.MATCHES, pattern.pattern()));
        }

        public R isEmpty() {
            return handleSpecification(createSpecification(Operator.IS_EMPTY, true));
        }

        public R isBlank() {
            return handleSpecification(createSpecification(Operator.IS_BLANK, true));
        }

        public R isNotBlank() {
            return handleSpecification(createSpecification(Operator.IS_NOT_BLANK, false));
        }

        public R isNotEmpty() {
            return handleSpecification(createSpecification(Operator.IS_NOT_EMPTY, false));
        }
    }

    // ==================== TEMPORAL FIELD BUILDER ====================
    /**
     * Builder for numeric field specifications. Provides only
     * numeric-appropriate operations.
     */
    public static class NumericFieldBuilder<T, N extends Number & Comparable<N>, R> extends BaseOperations<T, N, R> {

        public NumericFieldBuilder(MetaAttribute<T, N> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler) {
            super(attribute, service, handler);
        }

        public NumericFieldBuilder(MetaAttribute<T, N> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler, java.util.List<MetaAttribute<?, ?>> fieldPath) {
            super(attribute, service, handler, fieldPath);
        }

        public NumericFieldBuilder(MetaAttribute<T, N> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler, java.util.List<MetaAttribute<?, ?>> fieldPath, java.util.List<com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> collectionOperations) {
            super(attribute, service, handler, fieldPath, collectionOperations);
        }

        // Numeric-specific operations
        public R greaterThan(N value) {
            return handleSpecification(createSpecification(Operator.GREATER_THAN, value));
        }

        public R lessThan(N value) {
            return handleSpecification(createSpecification(Operator.LESS_THAN, value));
        }

        public R greaterThanOrEqual(N value) {
            return handleSpecification(createSpecification(Operator.GREATER_OR_EQUAL_THAN, value));
        }

        public R lessThanOrEqual(N value) {
            return handleSpecification(createSpecification(Operator.LESS_OR_EQUAL_THAN, value));
        }
    }

    // ==================== COLLECTION FIELD BUILDER ====================
    /**
     * Builder for temporal field specifications. Provides only
     * temporal-appropriate operations.
     */
    public static class TemporalFieldBuilder<T, D, R> extends BaseOperations<T, D, R> {

        public TemporalFieldBuilder(MetaAttribute<T, D> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler) {
            super(attribute, service, handler);
        }

        public TemporalFieldBuilder(MetaAttribute<T, D> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler, java.util.List<MetaAttribute<?, ?>> fieldPath) {
            super(attribute, service, handler, fieldPath);
        }

        public TemporalFieldBuilder(MetaAttribute<T, D> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler, java.util.List<MetaAttribute<?, ?>> fieldPath, java.util.List<com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> collectionOperations) {
            super(attribute, service, handler, fieldPath, collectionOperations);
        }

        // Temporal-specific operations
        public R isBefore(D dateTime) {
            return handleSpecification(createSpecification(Operator.IS_BEFORE, dateTime));
        }

        public R isAfter(D dateTime) {
            return handleSpecification(createSpecification(Operator.IS_AFTER, dateTime));
        }

        public R isOnOrBefore(D dateTime) {
            return handleSpecification(createSpecification(Operator.IS_ON_OR_BEFORE, dateTime));
        }

        public R isOnOrAfter(D dateTime) {
            return handleSpecification(createSpecification(Operator.IS_ON_OR_AFTER, dateTime));
        }

        public R last(TemporalPreset preset) {
            return handleSpecification(createSpecification(Operator.LAST, preset));
        }

        public R last(String presetExpression) {
            return last(TemporalPresetParser.parse(presetExpression));
        }

        public R next(TemporalPreset preset) {
            return handleSpecification(createSpecification(Operator.NEXT, preset));
        }

        public R next(String presetExpression) {
            return next(TemporalPresetParser.parse(presetExpression));
        }

        // Temporal fields are also comparable
        public R greaterThan(D value) {
            return handleSpecification(createSpecification(Operator.GREATER_THAN, value));
        }

        public R lessThan(D value) {
            return handleSpecification(createSpecification(Operator.LESS_THAN, value));
        }

        public R greaterThanOrEqual(D value) {
            return handleSpecification(createSpecification(Operator.GREATER_OR_EQUAL_THAN, value));
        }

        public R lessThanOrEqual(D value) {
            return handleSpecification(createSpecification(Operator.LESS_OR_EQUAL_THAN, value));
        }
    }

    // ==================== BOOLEAN FIELD BUILDER ====================
    /**
     * Builder for collection field specifications. Provides only
     * collection-appropriate operations.
     */
    public static class CollectionFieldBuilder<T, E, R> extends BaseOperations<T, Collection<E>, R> {

        private final Class<E> elementType;

        public CollectionFieldBuilder(CollectionAttribute<T, E> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler) {
            super(attribute, service, handler);
            this.elementType = attribute.getElementType();
        }

        public CollectionFieldBuilder(CollectionAttribute<T, E> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler, java.util.List<MetaAttribute<?, ?>> fieldPath) {
            super(attribute, service, handler, fieldPath);
            this.elementType = attribute.getElementType();
        }

        public CollectionFieldBuilder(CollectionAttribute<T, E> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler, java.util.List<MetaAttribute<?, ?>> fieldPath, java.util.List<com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> collectionOperations) {
            super(attribute, service, handler, fieldPath, collectionOperations);
            this.elementType = attribute.getElementType();
        }

        // Collection-specific operations
        public R contains(E element) {
            return handleSpecification(createSpecification(Operator.COLLECTION_CONTAINS, element));
        }

        public R collectionAny(Specification<?> elementSpec) {
            return handleSpecification(createSpecification(Operator.COLLECTION_ANY, elementSpec));
        }

        public R collectionAll(Specification<?> elementSpec) {
            return handleSpecification(createSpecification(Operator.COLLECTION_ALL, elementSpec));
        }

        public R collectionNone(Specification<?> elementSpec) {
            return handleSpecification(createSpecification(Operator.COLLECTION_NONE, elementSpec));
        }

        public Class<E> getElementType() {
            return elementType;
        }
    }

    // ==================== MODEL FIELD BUILDER ====================
    /**
     * Builder for model field specifications. Provides operations for
     * model-typed fields (complex objects).
     * 
     * <p>Model fields support basic operations like null checks and equality,
     * but not type-specific operations like string contains or numeric comparisons.</p>
     */
    public static class ModelFieldBuilder<T, M, R> extends BaseOperations<T, M, R> {

        public ModelFieldBuilder(com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<T, M> attribute, 
                                SpecificationService<T> service, 
                                SpecificationHandler<T, R> handler) {
            super(attribute, service, handler);
        }

        public ModelFieldBuilder(com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<T, M> attribute, 
                                SpecificationService<T> service, 
                                SpecificationHandler<T, R> handler, 
                                java.util.List<MetaAttribute<?, ?>> fieldPath) {
            super(attribute, service, handler, fieldPath);
        }

        public ModelFieldBuilder(com.thy.fss.common.inmemory.specification.attribute.ModelAttribute<T, M> attribute, 
                                SpecificationService<T> service, 
                                SpecificationHandler<T, R> handler, 
                                java.util.List<MetaAttribute<?, ?>> fieldPath, 
                                java.util.List<com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> collectionOperations) {
            super(attribute, service, handler, fieldPath, collectionOperations);
        }

        // Model fields support basic operations (equalTo, notEqualTo, isNull, isNotNull)
        // All basic operations are inherited from BaseOperations
    }

    // ==================== ENUM FIELD BUILDER ====================
    /**
     * Builder for boolean field specifications. Provides only
     * boolean-appropriate operations.
     */
    public static class BooleanFieldBuilder<T, R> extends BaseOperations<T, Boolean, R> {

        public BooleanFieldBuilder(BooleanAttribute<T> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler) {
            super(attribute, service, handler);
        }

        public BooleanFieldBuilder(BooleanAttribute<T> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler, java.util.List<MetaAttribute<?, ?>> fieldPath) {
            super(attribute, service, handler, fieldPath);
        }

        public BooleanFieldBuilder(BooleanAttribute<T> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler, java.util.List<MetaAttribute<?, ?>> fieldPath, java.util.List<com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> collectionOperations) {
            super(attribute, service, handler, fieldPath, collectionOperations);
        }

        // Boolean fields typically only need equality operations
        // All basic operations (equalTo, notEqualTo, isNull, isNotNull) are inherited
        public R isTrue() {
            return equalTo(Boolean.TRUE);
        }

        public R isFalse() {
            return equalTo(Boolean.FALSE);
        }
    }

    // ==================== BASE OPERATIONS ====================
    /**
     * Builder for enum field specifications. Provides only enum-appropriate
     * operations.
     */
    public static class EnumFieldBuilder<T, E extends Enum<E>, R> extends BaseOperations<T, E, R> {

        public EnumFieldBuilder(EnumAttribute<T, E> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler) {
            super(attribute, service, handler);
        }

        public EnumFieldBuilder(EnumAttribute<T, E> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler, java.util.List<MetaAttribute<?, ?>> fieldPath) {
            super(attribute, service, handler, fieldPath);
        }

        public EnumFieldBuilder(EnumAttribute<T, E> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler, java.util.List<MetaAttribute<?, ?>> fieldPath, java.util.List<com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> collectionOperations) {
            super(attribute, service, handler, fieldPath, collectionOperations);
        }

        // Enum fields typically only need equality and membership operations
        // All basic operations (equalTo, notEqualTo, in, notIn, isNull, isNotNull) are inherited
    }

    // ==================== SPECIFICATION HANDLER INTERFACE ====================
    /**
     * Base class containing operations available to all field types.
     */
    public abstract static class BaseOperations<T, F, R> {

        protected final MetaAttribute<T, F> attribute;
        protected final SpecificationService<T> specificationService;
        protected final SpecificationHandler<T, R> handler;
        protected final java.util.List<MetaAttribute<?, ?>> fieldPath;
        protected final java.util.List<com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> collectionOperations;

        protected BaseOperations(MetaAttribute<T, F> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler) {
            this(attribute, service, handler, null, null);
        }

        protected BaseOperations(MetaAttribute<T, F> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler, java.util.List<MetaAttribute<?, ?>> fieldPath) {
            this(attribute, service, handler, fieldPath, null);
        }

        protected BaseOperations(MetaAttribute<T, F> attribute, SpecificationService<T> service, SpecificationHandler<T, R> handler, java.util.List<MetaAttribute<?, ?>> fieldPath, java.util.List<com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata<?, ?>> collectionOperations) {
            this.attribute = attribute;
            this.specificationService = service;
            this.handler = handler;
            this.fieldPath = fieldPath;
            this.collectionOperations = collectionOperations;
        }

        // Basic operations available to all types
        public R equalTo(F value) {
            return handleSpecification(createSpecification(Operator.EQUALS, value));
        }

        public R notEqualTo(F value) {
            return handleSpecification(createSpecification(Operator.NOT_EQUALS, value));
        }

        public R in(Collection<F> values) {
            return handleSpecification(createSpecification(Operator.IN, values));
        }

        @SafeVarargs
        public final R in(F... values) {
            return handleSpecification(createSpecification(Operator.IN, values));
        }

        public R notIn(Collection<F> values) {
            return handleSpecification(createSpecification(Operator.NOT_IN, values));
        }

        @SafeVarargs
        public final R notIn(F... values) {
            return handleSpecification(createSpecification(Operator.NOT_IN, values));
        }

        public R isNull() {
            return handleSpecification(createSpecification(Operator.IS_NULL, null));
        }

        public R isNotNull() {
            return handleSpecification(createSpecification(Operator.IS_NOT_NULL, null));
        }

        protected R handleSpecification(Specification<T> specification) {
            return handler.handle(specification);
        }

        @SuppressWarnings("unchecked")
        protected Specification<T> createSpecification(Operator operator, Object value) {
            // Check if this is a nested path by comparing attribute owner with specification service's entity class
            Class<?> attributeOwner = attribute.getOwnerType();
            Class<?> serviceEntityClass = specificationService.getEntityClass();
            boolean isNestedPath = !attributeOwner.equals(serviceEntityClass);

            // Case 1: We have a proper field path - use FieldPathSpecification
            if (fieldPath != null && fieldPath.size() > 1) {
                // This is the correct way to handle nested paths

                // Get the last attribute - this is the field we want to validate
                MetaAttribute<?, ?> lastAttribute = fieldPath.get(fieldPath.size() - 1);
                Class<?> ownerType = lastAttribute.getOwnerType();

                // Get specification service for the owner type
                com.thy.fss.common.inmemory.specification.SpecificationService<?> targetService
                        = com.thy.fss.common.inmemory.specification.SpecificationServices.getService(ownerType);

                // Create navigation path (all elements EXCEPT the last one)
                // The last element is the field to validate, not part of navigation
                // Create a new ArrayList to avoid subList issues
                java.util.List<MetaAttribute<?, ?>> navigationPath = new java.util.ArrayList<>(
                        fieldPath.subList(0, fieldPath.size() - 1)
                );
                
                // Check if any element in the full path (except last) is a collection
                // If so, we need to handle it differently
                boolean hasCollectionInPath = false;
                int collectionIndex = -1;
                for (int i = 0; i < fieldPath.size() - 1; i++) {
                    if (fieldPath.get(i) instanceof com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute) {
                        hasCollectionInPath = true;
                        collectionIndex = i;
                        break;
                    }
                }

                // If navigation path is empty, this means we only have one field in the path
                // In this case, we should not use FieldPathSpecification
                if (navigationPath.isEmpty()) {
                    // This should not happen because we check fieldPath.size() > 1
                    // But if it does, fall back to simple case
                    com.thy.fss.common.inmemory.specification.SpecificationService correctService
                            = com.thy.fss.common.inmemory.specification.SpecificationServices.getService(ownerType);
                    return () -> obj -> correctService.validateSpecification(obj, lastAttribute, operator, value);
                }
                
                // Handle collection in path: if path contains collection field, 
                // we need to use COLLECTION_ANY to check elements
                if (hasCollectionInPath) {
                    // Split path at collection: before collection and after collection
                    java.util.List<MetaAttribute<?, ?>> beforeCollection = new java.util.ArrayList<>(
                            fieldPath.subList(0, collectionIndex + 1)
                    );
                    java.util.List<MetaAttribute<?, ?>> afterCollection = new java.util.ArrayList<>(
                            fieldPath.subList(collectionIndex + 1, fieldPath.size())
                    );
                    
                    // Get collection attribute and element type
                    com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute<?, ?> collectionAttr = 
                            (com.thy.fss.common.inmemory.specification.attribute.CollectionAttribute<?, ?>) 
                            fieldPath.get(collectionIndex);
                    Class<?> elementType = collectionAttr.getElementType();
                    
                    // Create specification for element (afterCollection path)
                    com.thy.fss.common.inmemory.specification.SpecificationService<?> elementService = 
                            com.thy.fss.common.inmemory.specification.SpecificationServices.getService(elementType);
                    
                    // Build element specification
                    com.thy.fss.common.inmemory.specification.Specification<?> elementSpec;
                    if (afterCollection.size() == 1) {
                        // Direct field on element
                        MetaAttribute<?, ?> elementField = afterCollection.get(0);
                        elementSpec = new com.thy.fss.common.inmemory.specification.Specification<Object>() {
                            @Override
                            @SuppressWarnings({"unchecked", "rawtypes"})
                            public boolean test(Object obj) {
                                if (obj == null) {
                                    // When the element itself is null, only IS_NULL should return true
                                    return operator == Operator.IS_NULL;
                                }
                                return ((com.thy.fss.common.inmemory.specification.SpecificationService) elementService)
                                        .validateSpecification(obj, elementField, operator, value);
                            }
                            
                            @Override
                            public java.util.function.Predicate<Object> toPredicate() {
                                return this::test;
                            }
                        };
                    } else {
                        // Nested path on element - use FieldPathSpecification recursively
                        MetaAttribute<?, ?> finalField = afterCollection.get(afterCollection.size() - 1);
                        java.util.List<MetaAttribute<?, ?>> elementNavPath = new java.util.ArrayList<>(
                                afterCollection.subList(0, afterCollection.size() - 1)
                        );
                        
                        com.thy.fss.common.inmemory.specification.SpecificationService<?> finalService = 
                                com.thy.fss.common.inmemory.specification.SpecificationServices.getService(
                                        finalField.getOwnerType()
                                );
                        
                        com.thy.fss.common.inmemory.specification.Specification<?> finalSpec = 
                                new com.thy.fss.common.inmemory.specification.Specification<Object>() {
                            @Override
                            @SuppressWarnings({"unchecked", "rawtypes"})
                            public boolean test(Object obj) {
                                if (obj == null) {
                                    // When the nested object is null, only IS_NULL should return true
                                    return operator == Operator.IS_NULL;
                                }
                                return ((com.thy.fss.common.inmemory.specification.SpecificationService) finalService)
                                        .validateSpecification(obj, finalField, operator, value);
                            }
                            
                            @Override
                            public java.util.function.Predicate<Object> toPredicate() {
                                return this::test;
                            }
                        };
                        
                        elementSpec = new com.thy.fss.common.inmemory.specification.FieldPathSpecification<>(
                                elementNavPath,
                                finalSpec,
                                (com.thy.fss.common.inmemory.specification.SpecificationService) elementService
                        );
                    }
                    
                    // Create COLLECTION_ANY specification for the collection field
                    com.thy.fss.common.inmemory.specification.Specification<?> collectionSpec = 
                            new com.thy.fss.common.inmemory.specification.Specification<Object>() {
                        @Override
                        @SuppressWarnings({"unchecked", "rawtypes"})
                        public boolean test(Object obj) {
                            if (obj == null) {
                                return false;
                            }
                            if (!(obj instanceof Collection)) {
                                return false;
                            }
                            Collection<?> coll = (Collection<?>) obj;
                            // COLLECTION_ANY: at least one element matches
                            for (Object element : coll) {
                                if (((com.thy.fss.common.inmemory.specification.Specification) elementSpec).test(element)) {
                                    return true;
                                }
                            }
                            return false;
                        }
                        
                        @Override
                        public java.util.function.Predicate<Object> toPredicate() {
                            return this::test;
                        }
                    };
                    
                    // Navigate to collection field (beforeCollection without last element)
                    if (collectionIndex == 0) {
                        // Collection is at root level
                        return (com.thy.fss.common.inmemory.specification.Specification<T>) collectionSpec;
                    } else {
                        // Navigate to collection
                        java.util.List<MetaAttribute<?, ?>> navToCollection = new java.util.ArrayList<>(
                                beforeCollection.subList(0, beforeCollection.size() - 1)
                        );
                        return new com.thy.fss.common.inmemory.specification.FieldPathSpecification<>(
                                navToCollection,
                                collectionSpec,
                                specificationService
                        );
                    }
                }

                // Create a specification for the target field
                // This will be applied to the owner object (not the field value!)
                com.thy.fss.common.inmemory.specification.Specification<?> targetSpec
                        = new com.thy.fss.common.inmemory.specification.Specification<Object>() {
                    @Override
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    public boolean test(Object obj) {
                        if (obj == null) {
                            // Handle null check based on operator
                            // When the navigated object itself is null, only IS_NULL should return true
                            return operator == Operator.IS_NULL;
                        }
                        // Validate that obj is of the expected owner type
                        if (!ownerType.isInstance(obj)) {
                            throw new IllegalStateException(
                                    String.format("Type mismatch in nested specification: expected %s but got %s for field %s",
                                            ownerType.getName(), obj.getClass().getName(), lastAttribute.getName())
                            );
                        }
                        // Use raw types to avoid generic issues
                        return ((com.thy.fss.common.inmemory.specification.SpecificationService) targetService)
                                .validateSpecification(obj, lastAttribute, operator, value);
                    }

                    @Override
                    public java.util.function.Predicate<Object> toPredicate() {
                        return this::test;
                    }
                };

                // Wrap it in a FieldPathSpecification that navigates through the path
                // Use navigationPath (without last element) for navigation
                // Pass collection operations for proper collection field handling
                return new com.thy.fss.common.inmemory.specification.FieldPathSpecification<>(
                        navigationPath,
                        targetSpec,
                        specificationService,
                        collectionOperations
                );
            }

            // Case 2: Attribute owner doesn't match service entity (type safety violation)
            // User is directly using an attribute from a different entity without proper navigation
            if (isNestedPath) {
                // This is incorrect usage but we handle it gracefully
                // The specification will be applied to objects of the attribute's owner type, not root type
                com.thy.fss.common.inmemory.specification.SpecificationService correctService
                        = com.thy.fss.common.inmemory.specification.SpecificationServices.getService(attributeOwner);
                return () -> obj -> correctService.validateSpecification(obj, attribute, operator, value);
            }

            // Case 3: Simple case - direct field access on root entity
            return () -> obj -> specificationService.validateSpecification(obj, attribute, operator, value);
        }
    }
}
