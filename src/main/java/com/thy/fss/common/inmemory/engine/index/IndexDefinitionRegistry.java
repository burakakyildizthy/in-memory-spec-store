package com.thy.fss.common.inmemory.engine.index;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Registry for managing index definitions per datasource.
 * Provides default index strategy based on first key field from provided definition.
 * This class is not thread-safe as it's designed for single-threaded use in DataSyncEngine.
 */
public final class IndexDefinitionRegistry {
    
    private static final String DATASOURCE_NAME_CANNOT_BE_NULL = "datasourceName cannot be null";
    private final Map<String, IndexDefinition<?>> definitions;
    
    /**
     * Creates a new empty registry.
     */
    public IndexDefinitionRegistry() {
        this.definitions = new HashMap<>();
    }
    
    /**
     * Registers a custom index definition for a datasource.
     * Once registered, the definition is immutable and cannot be changed.
     *
     * @param <T>            The entity type
     * @param datasourceName The name of the datasource
     * @param definition     The index definition
     * @throws NullPointerException     if datasourceName or definition is null
     * @throws IllegalArgumentException if a definition is already registered for this datasource
     */
    public <T> void register(String datasourceName, IndexDefinition<T> definition) {
        Objects.requireNonNull(datasourceName, DATASOURCE_NAME_CANNOT_BE_NULL);
        Objects.requireNonNull(definition, "definition cannot be null");
        
        if (definitions.containsKey(datasourceName)) {
            throw new IllegalArgumentException(
                "Index definition already registered for datasource: " + datasourceName
            );
        }
        
        definitions.put(datasourceName, definition);
    }
    
    /**
     * Gets the index definition for a datasource, or creates a default one if not registered.
     * The default strategy uses the first key field from the provided definition.
     * This method is typically used when an index definition is provided inline.
     *
     * @param <T>            The entity type
     * @param datasourceName The name of the datasource
     * @param definition     The default definition to use if not already registered
     * @return The index definition (custom or provided default)
     * @throws NullPointerException if datasourceName or definition is null
     */
    @SuppressWarnings("unchecked")
    public <T> IndexDefinition<T> getOrCreateDefault(String datasourceName, IndexDefinition<T> definition) {
        Objects.requireNonNull(datasourceName, DATASOURCE_NAME_CANNOT_BE_NULL);
        Objects.requireNonNull(definition, "definition cannot be null");
        
        IndexDefinition<?> existing = definitions.get(datasourceName);
        if (existing != null) {
            return (IndexDefinition<T>) existing;
        }
        
        // Register the provided definition as default
        definitions.put(datasourceName, definition);
        return definition;
    }
    
    /**
     * Gets the index definition for a datasource.
     *
     * @param <T>            The entity type
     * @param datasourceName The name of the datasource
     * @return The index definition, or null if not registered
     * @throws NullPointerException if datasourceName is null
     */
    @SuppressWarnings("unchecked")
    public <T> IndexDefinition<T> get(String datasourceName) {
        Objects.requireNonNull(datasourceName, DATASOURCE_NAME_CANNOT_BE_NULL);
        return (IndexDefinition<T>) definitions.get(datasourceName);
    }
    
    /**
     * Checks if a custom definition exists for a datasource.
     *
     * @param datasourceName The name of the datasource
     * @return true if a custom definition is registered, false otherwise
     */
    public boolean hasCustomDefinition(String datasourceName) {
        return definitions.containsKey(datasourceName);
    }
    
    /**
     * Clears all registered definitions.
     * This is typically used in test cleanup.
     */
    public void clear() {
        definitions.clear();
    }
}
