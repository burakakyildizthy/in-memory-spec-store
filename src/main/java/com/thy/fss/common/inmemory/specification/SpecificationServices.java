package com.thy.fss.common.inmemory.specification;

/**
 * DEPRECATED: This class provides runtime service lookup as a fallback for cases
 * where compile-time type information is not available.
 * 
 * <p>This class should NOT be used in new code. Use direct INSTANCE references instead:</p>
 * <pre>{@code
 * // OLD (deprecated):
 * SpecificationService<User> service = SpecificationServices.getService(User.class);
 * 
 * // NEW (preferred):
 * SpecificationService<User> service = UserSpecificationService.INSTANCE;
 * }</pre>
 * 
 * <p>This class exists only for backward compatibility and will be removed in a future version.</p>
 * 
 * @deprecated Use direct INSTANCE references to generated services instead
 */
@Deprecated(since = "3.3.4", forRemoval = true)
public class SpecificationServices {
    
    private SpecificationServices() {
        // Utility class
    }
    
    /**
     * Gets a SpecificationService for the given entity class using reflection.
     * 
     * <p><b>DEPRECATED:</b> This method uses reflection and should be avoided.
     * Use direct INSTANCE references instead.</p>
     * 
     * @param entityClass the entity class
     * @param <T> the entity type
     * @return the SpecificationService for the entity
     * @throws IllegalArgumentException if no service is found
     * @deprecated Use direct INSTANCE references like {@code UserSpecificationService.INSTANCE}
     */
    @Deprecated(since = "3.3.4", forRemoval = true)
    @SuppressWarnings("unchecked")
    public static <T> SpecificationService<T> getService(Class<T> entityClass) {
        if (entityClass == null) {
            throw new IllegalArgumentException("Entity class cannot be null");
        }
        
        try {
            // Try to find the generated service class
            String serviceClassName = entityClass.getName() + "SpecificationService";
            Class<?> serviceClass = Class.forName(serviceClassName);
            
            // Get the INSTANCE field
            java.lang.reflect.Field instanceField = serviceClass.getDeclaredField("INSTANCE");
            return (SpecificationService<T>) instanceField.get(null);
            
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(
                "No SpecificationService found for class: " + entityClass.getName() + 
                ". Ensure the class is annotated with @MetaModel and the annotation processor has run.", e);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(
                "SpecificationService for " + entityClass.getName() + " does not have an INSTANCE field.", e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(
                "Cannot access INSTANCE field of SpecificationService for " + entityClass.getName(), e);
        }
    }
    
    /**
     * Checks if a SpecificationService exists for the given entity class.
     * 
     * @param entityClass the entity class
     * @return true if a service exists, false otherwise
     * @deprecated Use direct INSTANCE references instead
     */
    @Deprecated(since = "3.3.4", forRemoval = true)
    public static boolean hasService(Class<?> entityClass) {
        if (entityClass == null) {
            return false;
        }
        
        try {
            String serviceClassName = entityClass.getName() + "SpecificationService";
            Class.forName(serviceClassName);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
