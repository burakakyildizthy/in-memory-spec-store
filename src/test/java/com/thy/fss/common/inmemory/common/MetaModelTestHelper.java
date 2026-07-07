package com.thy.fss.common.inmemory.common;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility for working with generated metamodel classes in tests.
 * Provides methods to access and verify generated classes (User_, UserFilter, UserSpecificationService).
 * 
 * <p>Tests MUST use generated metamodel classes instead of string-based field names.</p>
 */
public class MetaModelTestHelper {
    private static final String UNDERSCORE = "_";
    private static final String FILTER_SUFFIX = "Filter";
    private static final String SPECIFICATION_SERVICE_SUFFIX = "SpecificationService";
    private static final String MAKE_SURE_ANNOTATION_PROCESSOR_HAS_RUN = ". Make sure annotation processor has run.";

    private MetaModelTestHelper() {
        // Utility class
    }

    /**
     * Gets the metamodel class for an entity.
     * 
     * @param entityClass entity class
     * @return metamodel class (e.g., User_ for User)
     */
    public static Class<?> getMetaModelClass(Class<?> entityClass) {
        String metaModelClassName = entityClass.getName() + UNDERSCORE;
        
        try {
            return Class.forName(metaModelClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                "MetaModel class not found: " + metaModelClassName + 
                MAKE_SURE_ANNOTATION_PROCESSOR_HAS_RUN, e);
        }
    }

    /**
     * Gets the filter class for an entity.
     * 
     * @param entityClass entity class
     * @return filter class (e.g., UserFilter for User)
     */
    public static Class<?> getFilterClass(Class<?> entityClass) {
        String filterClassName = entityClass.getName() + FILTER_SUFFIX;
        
        try {
            return Class.forName(filterClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                "Filter class not found: " + filterClassName + 
                MAKE_SURE_ANNOTATION_PROCESSOR_HAS_RUN, e);
        }
    }

    /**
     * Gets the specification service class for an entity.
     * 
     * @param entityClass entity class
     * @return specification service class (e.g., UserSpecificationService for User)
     */
    public static Class<?> getSpecificationServiceClass(Class<?> entityClass) {
        String serviceClassName = entityClass.getName() + SPECIFICATION_SERVICE_SUFFIX;
        
        try {
            return Class.forName(serviceClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                "SpecificationService class not found: " + serviceClassName + 
                MAKE_SURE_ANNOTATION_PROCESSOR_HAS_RUN, e);
        }
    }

    /**
     * Verifies that all generated classes exist for an entity.
     * 
     * @param entityClass entity class
     */
    public static void verifyGeneratedClassesExist(Class<?> entityClass) {
        Class<?> metaModelClass = getMetaModelClass(entityClass);
        assertThat(metaModelClass).isNotNull();
        
        Class<?> filterClass = getFilterClass(entityClass);
        assertThat(filterClass).isNotNull();
        
        Class<?> serviceClass = getSpecificationServiceClass(entityClass);
        assertThat(serviceClass).isNotNull();
    }

    /**
     * Checks if metamodel class exists for an entity.
     * 
     * @param entityClass entity class
     * @return true if metamodel class exists
     */
    public static boolean hasMetaModelClass(Class<?> entityClass) {
        try {
            getMetaModelClass(entityClass);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * Checks if filter class exists for an entity.
     * 
     * @param entityClass entity class
     * @return true if filter class exists
     */
    public static boolean hasFilterClass(Class<?> entityClass) {
        try {
            getFilterClass(entityClass);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * Checks if specification service class exists for an entity.
     * 
     * @param entityClass entity class
     * @return true if specification service class exists
     */
    public static boolean hasSpecificationServiceClass(Class<?> entityClass) {
        try {
            getSpecificationServiceClass(entityClass);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * Gets the simple name of the metamodel class.
     * 
     * @param entityClass entity class
     * @return simple name (e.g., "User_" for User)
     */
    public static String getMetaModelSimpleName(Class<?> entityClass) {
        return entityClass.getSimpleName() + UNDERSCORE;
    }

    /**
     * Gets the simple name of the filter class.
     * 
     * @param entityClass entity class
     * @return simple name (e.g., "UserFilter" for User)
     */
    public static String getFilterSimpleName(Class<?> entityClass) {
        return entityClass.getSimpleName() + FILTER_SUFFIX;
    }

    /**
     * Gets the simple name of the specification service class.
     * 
     * @param entityClass entity class
     * @return simple name (e.g., "UserSpecificationService" for User)
     */
    public static String getSpecificationServiceSimpleName(Class<?> entityClass) {
        return entityClass.getSimpleName() + SPECIFICATION_SERVICE_SUFFIX;
    }

    /**
     * Verifies that a metamodel class has expected static fields.
     * 
     * @param metaModelClass metamodel class
     * @param expectedFieldNames expected field names
     */
    public static void verifyMetaModelFields(Class<?> metaModelClass, String... expectedFieldNames) {
        for (String fieldName : expectedFieldNames) {
            try {
                java.lang.reflect.Field field = metaModelClass.getDeclaredField(fieldName);
                assertThat(field).isNotNull();
                assertThat(java.lang.reflect.Modifier.isStatic(field.getModifiers())).isTrue();
                assertThat(java.lang.reflect.Modifier.isFinal(field.getModifiers())).isTrue();
            } catch (NoSuchFieldException e) {
                throw new AssertionError("Expected field not found: " + fieldName, e);
            }
        }
    }

    /**
     * Gets a static field value from metamodel class.
     * 
     * @param <T> field type
     * @param metaModelClass metamodel class
     * @param fieldName field name
     * @return field value
     */
    @SuppressWarnings("unchecked")
    public static <T> T getMetaModelField(Class<?> metaModelClass, String fieldName) {
        try {
            java.lang.reflect.Field field = metaModelClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(null);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to get field: " + fieldName, e);
        }
    }

    /**
     * Creates a new instance of a filter class.
     * 
     * @param <T> filter type
     * @param filterClass filter class
     * @return new filter instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T createFilterInstance(Class<?> filterClass) {
        try {
            return (T) filterClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create filter instance: " + filterClass.getName(), e);
        }
    }

    /**
     * Verifies that generated classes follow naming conventions.
     * 
     * @param entityClass entity class
     */
    public static void verifyNamingConventions(Class<?> entityClass) {
        String entityName = entityClass.getSimpleName();
        
        Class<?> metaModelClass = getMetaModelClass(entityClass);
        assertThat(metaModelClass.getSimpleName()).isEqualTo(entityName + UNDERSCORE);
        
        Class<?> filterClass = getFilterClass(entityClass);
        assertThat(filterClass.getSimpleName()).isEqualTo(entityName + FILTER_SUFFIX);
        
        Class<?> serviceClass = getSpecificationServiceClass(entityClass);
        assertThat(serviceClass.getSimpleName()).isEqualTo(entityName + SPECIFICATION_SERVICE_SUFFIX);
    }

    /**
     * Note: Registry clearing is no longer needed as the new architecture uses
     * direct INSTANCE references instead of a registry pattern.
     * This method is kept for backward compatibility but does nothing.
     * 
     * @deprecated Registry pattern has been eliminated. This method is a no-op.
     */
    @Deprecated
    public static void clearSpecificationServicesRegistry() {
        // No-op: Registry pattern has been eliminated
        // Services are now accessed via direct INSTANCE references
    }
}
