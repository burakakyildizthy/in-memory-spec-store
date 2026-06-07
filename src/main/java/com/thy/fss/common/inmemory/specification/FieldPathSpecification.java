package com.thy.fss.common.inmemory.specification;

import com.thy.fss.common.inmemory.engine.mapping.CollectionOperationMetadata;
import com.thy.fss.common.inmemory.specification.attribute.MetaAttribute;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Specification implementation that navigates through a field path before applying validation.
 * This class handles nested property access in specifications, allowing specifications to be
 * applied to fields deep in an object graph, including collection operations.
 * 
 * <p>Example usage:</p>
 * <pre>
 * // Specification for Order.customer.name equals "John"
 * List&lt;MetaAttribute&lt;?, ?&gt;&gt; path = Arrays.asList(Order_.customer, Customer_.name);
 * Specification&lt;Customer&gt; nameSpec = ... // specification for name field
 * Specification&lt;Order&gt; orderSpec = new FieldPathSpecification&lt;&gt;(path, nameSpec, specService);
 * </pre>
 * 
 * @param <T> the root entity type that this specification evaluates
 */
public class FieldPathSpecification<T> implements Specification<T> {
    
    private final List<MetaAttribute<?, ?>> fieldPath;
    private final Specification<?> delegateSpecification;
    private final SpecificationService<T> rootSpecificationService;
    private final List<CollectionOperationMetadata<?, ?>> collectionOperations;
    
    /**
     * Creates a new field path specification without collection operations.
     * 
     * @param fieldPath the path of fields to navigate through
     * @param delegateSpecification the specification to apply to the final field value
     * @param rootSpecificationService the specification service for the root entity type
     * @throws IllegalArgumentException if any parameter is null or fieldPath is empty
     */
    public FieldPathSpecification(
            List<MetaAttribute<?, ?>> fieldPath,
            Specification<?> delegateSpecification,
            SpecificationService<T> rootSpecificationService) {
        this(fieldPath, delegateSpecification, rootSpecificationService, null);
    }
    
    /**
     * Creates a new field path specification with collection operations.
     * 
     * @param fieldPath the path of fields to navigate through
     * @param delegateSpecification the specification to apply to the final field value
     * @param rootSpecificationService the specification service for the root entity type
     * @param collectionOperations collection operations to apply during path navigation
     * @throws IllegalArgumentException if any parameter is null or fieldPath is empty
     */
    public FieldPathSpecification(
            List<MetaAttribute<?, ?>> fieldPath,
            Specification<?> delegateSpecification,
            SpecificationService<T> rootSpecificationService,
            List<CollectionOperationMetadata<?, ?>> collectionOperations) {
        
        if (fieldPath == null || fieldPath.isEmpty()) {
            throw new IllegalArgumentException("Field path cannot be null or empty");
        }
        Objects.requireNonNull(delegateSpecification, "Delegate specification cannot be null");
        Objects.requireNonNull(rootSpecificationService, "Root specification service cannot be null");
        
        this.fieldPath = Collections.unmodifiableList(fieldPath);
        this.delegateSpecification = delegateSpecification;
        this.rootSpecificationService = rootSpecificationService;
        this.collectionOperations = collectionOperations != null 
                ? Collections.unmodifiableList(collectionOperations) 
                : Collections.emptyList();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public boolean test(T entity) {
        if (entity == null) {
            return false;
        }
        
        // Navigate through the field path to get the target value
        // Use getValueByPathWithCollections if collection operations are present
        Object targetValue;
        if (collectionOperations != null && !collectionOperations.isEmpty()) {
            targetValue = rootSpecificationService.getValueByPathWithCollections(entity, fieldPath, collectionOperations);
        } else {
            targetValue = rootSpecificationService.getValueByPath(entity, fieldPath);
        }
        
        // Apply the delegate specification to the target value
        // Cast is safe because the delegate specification was created for the target field type
        return ((Specification<Object>) delegateSpecification).test(targetValue);
    }
    
    @Override
    public java.util.function.Predicate<T> toPredicate() {
        return this::test;
    }
    
    /**
     * Gets the field path for this specification.
     * 
     * @return immutable list of meta attributes representing the field path
     */
    public List<MetaAttribute<?, ?>> getFieldPath() {
        return fieldPath;
    }
    
    /**
     * Gets the delegate specification that is applied to the final field value.
     * 
     * @return the delegate specification
     */
    public Specification<?> getDelegateSpecification() {
        return delegateSpecification;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("FieldPathSpecification{path=");
        for (int i = 0; i < fieldPath.size(); i++) {
            if (i > 0) {
                sb.append(".");
            }
            MetaAttribute<?, ?> attr = fieldPath.get(i);
            sb.append(attr.getOwnerType().getSimpleName())
              .append(".")
              .append(attr.getName());
        }
        sb.append(", spec=").append(delegateSpecification).append("}");
        return sb.toString();
    }
}
