package com.thy.fss.common.inmemory.exception;

/**
 * Exception thrown when there are issues during the object building process.
 * This includes reflection errors, type conversion failures, mapping issues,
 * and other problems that occur while constructing objects from DataSource data.
 */
public class ObjectBuildingException extends InMemoryDataStoreException {

    private final Class<?> targetClass;
    private final String propertyName;

    /**
     * Constructs a new ObjectBuildingException with the specified detail message.
     *
     * @param message the detail message explaining the object building issue
     */
    public ObjectBuildingException(String message) {
        super(message);
        this.targetClass = null;
        this.propertyName = null;
    }

    /**
     * Constructs a new ObjectBuildingException with the specified detail message
     * and cause.
     *
     * @param message the detail message explaining the object building issue
     * @param cause   the underlying cause of the building failure
     */
    public ObjectBuildingException(String message, Throwable cause) {
        super(message, cause);
        this.targetClass = null;
        this.propertyName = null;
    }

    /**
     * Constructs a new ObjectBuildingException with the specified detail message
     * and target class for better error context.
     *
     * @param message     the detail message explaining the object building issue
     * @param targetClass the class that was being built when the error occurred
     */
    public ObjectBuildingException(String message, Class<?> targetClass) {
        super(message);
        this.targetClass = targetClass;
        this.propertyName = null;
    }

    /**
     * Constructs a new ObjectBuildingException with the specified detail message,
     * cause, and target class for complete error context.
     *
     * @param message     the detail message explaining the object building issue
     * @param cause       the underlying cause of the building failure
     * @param targetClass the class that was being built when the error occurred
     */
    public ObjectBuildingException(String message, Throwable cause, Class<?> targetClass) {
        super(message, cause);
        this.targetClass = targetClass;
        this.propertyName = null;
    }

    /**
     * Constructs a new ObjectBuildingException with the specified detail message,
     * target class, and property name for detailed error context.
     *
     * @param message      the detail message explaining the object building issue
     * @param targetClass  the class that was being built when the error occurred
     * @param propertyName the property that was being populated when the error occurred
     */
    public ObjectBuildingException(String message, Class<?> targetClass, String propertyName) {
        super(message);
        this.targetClass = targetClass;
        this.propertyName = propertyName;
    }

    /**
     * Constructs a new ObjectBuildingException with complete error context including
     * message, cause, target class, and property name.
     *
     * @param message      the detail message explaining the object building issue
     * @param cause        the underlying cause of the building failure
     * @param targetClass  the class that was being built when the error occurred
     * @param propertyName the property that was being populated when the error occurred
     */
    public ObjectBuildingException(String message, Throwable cause, Class<?> targetClass, String propertyName) {
        super(message, cause);
        this.targetClass = targetClass;
        this.propertyName = propertyName;
    }

    /**
     * Returns the target class that was being built when the exception occurred.
     *
     * @return the target class, or null if not specified
     */
    public Class<?> getTargetClass() {
        return targetClass;
    }

    /**
     * Returns the property name that was being populated when the exception occurred.
     *
     * @return the property name, or null if not specified
     */
    public String getPropertyName() {
        return propertyName;
    }
}