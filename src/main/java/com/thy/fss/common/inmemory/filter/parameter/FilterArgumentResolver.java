package com.thy.fss.common.inmemory.filter.parameter;

import com.thy.fss.common.inmemory.filter.deserializer.DeserializerRegistry;
import com.thy.fss.common.inmemory.filter.deserializer.FilterValueDeserializer;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Custom Spring MVC argument resolver that handles MetaModelFilter parameter binding with @JsonProperty support.
 *
 * <p>This resolver replaces the PropertyEditor-based binding approach by using generated static methods
 * in each filter's Deserializer class. The generated methods provide:
 * <ul>
 *   <li>Zero-reflection field mapping using @JsonProperty annotations</li>
 *   <li>Efficient O(1) field path resolution using switch statements</li>
 *   <li>Support for nested filter paths (e.g., user.addr.city.eq=NYC)</li>
 *   <li>Consistent value deserialization with JSON binding</li>
 * </ul>
 *
 * <p>Example usage in a controller:
 * <pre>
 * {@code
 * @GetMapping("/users")
 * public List<User> searchUsers(UserFilter filter) {
 *     // Query parameters like ?n.eq=john&addr.city.eq=NYC are automatically bound
 *     // where 'n' is the @JsonProperty abbreviation for 'name'
 *     return userService.findUsers(filter);
 * }
 * }
 * </pre>
 *
 * <p>The resolver delegates to generated {@code bindQueryParameters()} methods which:
 * <ol>
 *   <li>Map abbreviated field paths to Java field names using generated mapFieldPath()</li>
 *   <li>Create and populate filter objects without reflection</li>
 *   <li>Deserialize values using the shared FilterValueDeserializer</li>
 *   <li>Handle nested filters and collection operators</li>
 * </ol>
 *
 * @see FilterValueDeserializer
 * @see CollectionParameterHandler
 * @see DeserializerRegistry
 */
public class FilterArgumentResolver implements HandlerMethodArgumentResolver {

    private final FilterValueDeserializer deserializer;
    private final CollectionParameterHandler collectionHandler;
    private final DeserializerRegistry registry;

    /**
     * Creates a new FilterArgumentResolver with the required dependencies.
     *
     * @param deserializer      The value deserializer for converting string values to typed objects
     * @param collectionHandler The handler for parsing comma-separated collection values
     * @param registry          The registry for looking up field deserialization configurations
     */
    public FilterArgumentResolver(
            FilterValueDeserializer deserializer,
            CollectionParameterHandler collectionHandler,
            DeserializerRegistry registry) {
        this.deserializer = deserializer;
        this.collectionHandler = collectionHandler;
        this.registry = registry;
    }

    /**
     * Checks if this resolver supports the given method parameter.
     * Returns true for any parameter whose type name ends with "Filter".
     *
     * <p>This identifies MetaModelFilter parameters that should be bound using
     * the generated bindQueryParameters() methods rather than standard Spring binding.
     *
     * @param parameter The method parameter to check
     * @return true if the parameter type ends with "Filter", false otherwise
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().getSimpleName().endsWith("Filter");
    }

    /**
     * Resolves the filter object from query parameters by delegating to
     * the generated bindQueryParameters() method in the filter's Deserializer class.
     *
     * <p>Resolution process:
     * <ol>
     *   <li>Extract all query parameters from the request</li>
     *   <li>Locate the generated Deserializer class for the filter type</li>
     *   <li>Invoke the static bindQueryParameters() method via reflection</li>
     *   <li>Return the populated filter object</li>
     * </ol>
     *
     * <p>The generated method handles all field mapping, nested path resolution,
     * and value deserialization without any additional reflection overhead.
     *
     * @param parameter     The method parameter to resolve
     * @param mavContainer  The ModelAndViewContainer for the current request
     * @param webRequest    The current web request containing query parameters
     * @param binderFactory The factory for creating WebDataBinder instances (unused)
     * @return The populated filter object
     * @throws Exception if the deserializer class cannot be found, the bindQueryParameters method
     *                   is missing, or parameter binding fails
     */
    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) throws Exception {

        Class<?> filterClass = parameter.getParameterType();
        Map<String, String[]> parameterMap = webRequest.getParameterMap();

        try {
            // Get the deserializer class (e.g., UserFilterDeserializer)
            String deserializerClassName = filterClass.getName() + "Deserializer";
            Class<?> deserializerClass = Class.forName(deserializerClassName);

            // Invoke the generated bindQueryParameters method
            Method bindMethod = deserializerClass.getMethod(
                    "bindQueryParameters",
                    Map.class,
                    FilterValueDeserializer.class,
                    CollectionParameterHandler.class,
                    DeserializerRegistry.class
            );

            return bindMethod.invoke(
                    null,
                    parameterMap,
                    deserializer,
                    collectionHandler,
                    registry
            );

        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(
                    "Cannot find deserializer class for filter type: " + filterClass.getName() +
                            ". Ensure the filter class has been processed by the annotation processor.",
                    e
            );
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    "Deserializer class for " + filterClass.getName() +
                            " does not have a bindQueryParameters() method. " +
                            "Ensure you are using the latest version of the annotation processor.",
                    e
            );
        } catch (Exception e) {
            // Unwrap InvocationTargetException to get the actual cause
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IllegalArgumentException(
                    "Failed to bind query parameters to " + filterClass.getSimpleName() + ": " +
                            cause.getMessage(),
                    cause
            );
        }
    }
}
