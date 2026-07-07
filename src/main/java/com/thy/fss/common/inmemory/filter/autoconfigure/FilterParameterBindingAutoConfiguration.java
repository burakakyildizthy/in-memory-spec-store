package com.thy.fss.common.inmemory.filter.autoconfigure;

import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry;
import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistryImpl;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializerImpl;
import com.thy.fss.common.inmemory.filter.parameter.*;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.List;

/**
 * Spring Boot auto-configuration for filter parameter binding.
 *
 * <p>This configuration automatically enables query parameter binding for MetaModelFilter objects
 * using a custom HandlerMethodArgumentResolver that respects @JsonProperty annotations.
 * The resolver uses generated static methods for zero-reflection field mapping and delegates
 * to the same deserialization logic as JSON binding.
 *
 * <p>The auto-configuration is activated when:
 * <ul>
 *   <li>Running in a web application context</li>
 *   <li>Spring MVC classes are present on the classpath</li>
 * </ul>
 *
 * <p>All beans use {@code @ConditionalOnMissingBean} to allow customization by providing
 * custom implementations in the application context.
 *
 * <p>To disable this auto-configuration, exclude it in your application:
 * <pre>
 * {@code
 * @SpringBootApplication(exclude = FilterParameterBindingAutoConfiguration.class)
 * public class Application {
 *     // ...
 * }
 * }
 * </pre>
 *
 * <p>Example usage in a controller:
 * <pre>
 * {@code
 * @GetMapping("/users")
 * public List<User> searchUsers(UserFilter filter) {
 *     // Query parameters like ?n.eq=john&age.gt=25 are automatically bound
 *     // where 'n' is the @JsonProperty abbreviation for 'name'
 *     return userService.findUsers(filter);
 * }
 * }
 * </pre>
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass({RequestMappingHandlerAdapter.class, PropertyEditorRegistrar.class})
public class FilterParameterBindingAutoConfiguration implements WebMvcConfigurer {

    /**
     * Creates the FilterParameterBindingControllerAdvice bean to automatically
     * configure WebDataBinder for all controllers.
     *
     * @param propertyEditorRegistrar The registrar for filter PropertyEditors
     * @return A FilterParameterBindingControllerAdvice instance
     */
    @Bean
    @ConditionalOnMissingBean
    public FilterParameterBindingControllerAdvice filterParameterBindingControllerAdvice(
            FilterPropertyEditorRegistrar propertyEditorRegistrar) {
        return new FilterParameterBindingControllerAdvice(propertyEditorRegistrar);
    }

    /**
     * Configures the RequestMappingHandlerAdapter to use custom PropertyEditors
     * for filter parameter binding.
     *
     * @param adapter                 The RequestMappingHandlerAdapter to configure
     * @param propertyEditorRegistrar The registrar for filter PropertyEditors
     * @return The configured RequestMappingHandlerAdapter
     */
    @Bean
    @ConditionalOnMissingBean(name = "filterParameterBindingConfigurer")
    public RequestMappingHandlerAdapter filterParameterBindingConfigurer(
            RequestMappingHandlerAdapter adapter,
            FilterPropertyEditorRegistrar propertyEditorRegistrar) {

        ConfigurableWebBindingInitializer initializer =
                (ConfigurableWebBindingInitializer) adapter.getWebBindingInitializer();

        if (initializer == null) {
            initializer = new ConfigurableWebBindingInitializer();
            adapter.setWebBindingInitializer(initializer);
        }

        // Enable auto-growing of nested paths to support filter.field.operation binding
        initializer.setAutoGrowNestedPaths(true);
        initializer.setPropertyEditorRegistrar(propertyEditorRegistrar);
        return adapter;
    }

    /**
     * Creates the FilterValueDeserializer bean if not already present.
     * This deserializer provides the core value conversion logic shared between
     * JSON and query parameter deserialization.
     *
     * @return A FilterValueDeserializer implementation
     */
    @Bean
    @ConditionalOnMissingBean
    public FilterValueDeserializer filterValueDeserializer() {
        return new FilterValueDeserializerImpl();
    }

    /**
     * Creates the DeserializerRegistry bean if not already present.
     * This registry caches deserialization configurations and provides them
     * to PropertyEditors during parameter binding.
     *
     * @return A DeserializerRegistry implementation
     */
    @Bean
    @ConditionalOnMissingBean
    public DeserializerRegistry deserializerRegistry() {
        return new DeserializerRegistryImpl();
    }

    /**
     * Creates the CollectionParameterHandler bean if not already present.
     * This handler parses comma-separated values into typed collections for
     * collection operations (in, notIn).
     *
     * @param deserializer The value deserializer to use for element conversion
     * @return A CollectionParameterHandler implementation
     */
    @Bean
    @ConditionalOnMissingBean
    public CollectionParameterHandler collectionParameterHandler(FilterValueDeserializer deserializer) {
        return new CollectionParameterHandlerImpl(deserializer);
    }

    /**
     * Creates the FilterPropertyEditorRegistrar bean if not already present.
     * This registrar registers custom PropertyEditors for all filter field types.
     *
     * <p>Note: This is kept as a fallback mechanism. The primary binding approach
     * uses FilterArgumentResolver which takes precedence.
     *
     * @param deserializer      The value deserializer to use
     * @param collectionHandler The collection parameter handler to use
     * @param registry          The deserializer registry to use
     * @return A FilterPropertyEditorRegistrar implementation
     */
    @Bean
    @ConditionalOnMissingBean
    public FilterPropertyEditorRegistrar filterPropertyEditorRegistrar(
            FilterValueDeserializer deserializer,
            CollectionParameterHandler collectionHandler,
            DeserializerRegistry registry) {
        return new FilterPropertyEditorRegistrarImpl(deserializer, collectionHandler, registry);
    }

    /**
     * Creates the FilterArgumentResolver bean if not already present.
     * This resolver handles MetaModelFilter parameter binding with @JsonProperty support
     * using generated static methods for zero-reflection field mapping.
     *
     * <p>The resolver is registered first in the argument resolver chain to take
     * precedence over PropertyEditor-based binding.
     *
     * @param deserializer      The value deserializer for converting string values to typed objects
     * @param collectionHandler The handler for parsing comma-separated collection values
     * @param registry          The registry for looking up field deserialization configurations
     * @return A FilterArgumentResolver instance
     */
    @Bean
    @ConditionalOnMissingBean
    public FilterArgumentResolver filterArgumentResolver(
            FilterValueDeserializer deserializer,
            CollectionParameterHandler collectionHandler,
            DeserializerRegistry registry) {
        return new FilterArgumentResolver(deserializer, collectionHandler, registry);
    }

    /**
     * Registers the FilterArgumentResolver with Spring MVC.
     * The resolver is added first to ensure it takes precedence over PropertyEditor-based binding.
     *
     * <p>This allows the resolver to handle all MetaModelFilter parameters using the generated
     * bindQueryParameters() methods, which provide:
     * <ul>
     *   <li>Support for @JsonProperty abbreviated field names</li>
     *   <li>Zero-reflection field mapping using generated code</li>
     *   <li>Efficient O(1) field path resolution</li>
     *   <li>Consistent value deserialization with JSON binding</li>
     * </ul>
     *
     * @param resolvers The list of argument resolvers to add to
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(0, filterArgumentResolver(
                filterValueDeserializer(),
                collectionParameterHandler(filterValueDeserializer()),
                deserializerRegistry()
        ));
    }
}
