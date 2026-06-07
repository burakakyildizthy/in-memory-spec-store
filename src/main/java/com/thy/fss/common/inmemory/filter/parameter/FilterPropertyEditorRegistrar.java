package com.thy.fss.common.inmemory.filter.parameter;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;

/**
 * Interface for registering custom PropertyEditors for filter field types.
 * This registrar ensures that Spring's parameter binding system can properly
 * convert query parameter strings to filter field types using the same
 * deserialization logic as JSON.
 *
 * <p>Implementations should register PropertyEditors for all supported filter types:
 * <ul>
 *   <li>Temporal types: LocalDateTime, LocalDate, Instant</li>
 *   <li>Numeric types: Integer, Long, Double</li>
 *   <li>String and Boolean types</li>
 *   <li>Enum types</li>
 *   <li>Collection types for 'in' and 'notIn' operations</li>
 * </ul>
 *
 * <p>Example usage in Spring configuration:
 * <pre>
 * {@code
 * @Configuration
 * public class WebConfig implements WebMvcConfigurer {
 *     @Override
 *     public void addFormatters(FormatterRegistry registry) {
 *         registry.addPropertyEditorRegistrar(filterPropertyEditorRegistrar);
 *     }
 * }
 * }
 * </pre>
 */
public interface FilterPropertyEditorRegistrar extends PropertyEditorRegistrar {

    /**
     * Registers custom PropertyEditors for all filter field types with the given registry.
     * This method is called by Spring during initialization to set up custom type conversion
     * for parameter binding.
     *
     * @param registry The PropertyEditorRegistry to register editors with
     */
    @Override
    void registerCustomEditors(PropertyEditorRegistry registry);

    /**
     * Registers PropertyEditors for all fields in a specific filter class.
     * This method can be used to register editors for custom filter types
     * that may have additional field types beyond the standard ones.
     *
     * @param registry    The PropertyEditorRegistry to register editors with
     * @param filterClass The filter class to analyze and register editors for
     */
    void registerFilterFieldEditors(PropertyEditorRegistry registry, Class<?> filterClass);
}
