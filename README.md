# Multi-Source Data Library

A powerful, type-safe Java library for creating in-memory data stores that aggregate data from multiple sources with
automatic synchronization, complex nested hierarchies, and Spring Data integration.

## 🚀 Key Features

- **Multi-Source Aggregation**: Combine data from databases, REST APIs, files, and in-memory sources
- **Type-Safe Configuration**: Compile-time safety with generic types and builder patterns
- **Nested Object Hierarchies**: Support for complex multi-level nested object structures
- **Automatic Synchronization**: Background data refresh with configurable intervals
- **Spring Data Integration**: Built-in pagination, sorting, and specification-based querying
- **Fallback Support**: Automatic failover between data sources for high availability
- **Rich Aggregations**: COUNT, SUM, AVG, MIN, MAX, and custom aggregation functions
- **Performance Optimized**: In-memory storage with reflection caching and optimized queries
- **Production Ready**: Comprehensive error handling, monitoring, and observability

## 📋 Requirements

- Java 21 or higher
- Spring Data Commons 3.2.0+
- Gradle 8.0+ or Maven 3.8+

## 🛠️ Installation

### Gradle

```gradle
dependencies {
    implementation 'com.library:inmemory-data-library:1.0.0'
    implementation 'org.springframework.data:spring-data-commons:3.2.0'
}
```

### Maven

```xml

<dependency>
    <groupId>com.library</groupId>
    <artifactId>inmemory-data-library</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
<groupId>org.springframework.data</groupId>
<artifactId>spring-data-commons</artifactId>
<version>3.2.0</version>
</dependency>
```

## 🏃‍♂️ Quick Start

### 1. Basic Configuration

```java

@Configuration
public class DataStoreConfig {

    @Bean
    public InMemoryDataStore<User> userStore() {
        // Create data sources
        var userDataSource = new DatabaseDataSource<>("users", User.class, jdbcTemplate);
        var orderDataSource = new DatabaseDataSource<>("orders", Order.class, jdbcTemplate);

        // Configure property mappings
        var orderConfig = PropertyDataSourceConfig.<Order>builder()
                .propertyName("orders")
                .dataSource(orderDataSource)
                .dataSourceName("orders")
                .primaryKeyField("id")
                .foreignKeyField("userId")
                .mapper(order -> order)
                .isCollection(true)
                .build();

        var orderCountConfig = PropertyDataSourceConfig.<Order>builder()
                .propertyName("orderCount")
                .dataSource(orderDataSource)
                .dataSourceName("orders")
                .primaryKeyField("id")
                .foreignKeyField("userId")
                .mapper(order -> order)
                .isAggregation(true)
                .aggregationType(AggregationType.COUNT)
                .build();

        // Build configuration
        var config = BuildConfiguration.<User>builder(User.class)
                .withPrimaryDataSource(userDataSource)
                .withPropertyConfig(orderConfig)
                .withPropertyConfig(orderCountConfig)
                .build();

        return new InMemoryDataStore<>(config);
    }
}
```

### 2. Querying Data

```java

@Service
public class UserService {

    @Autowired
    private InMemoryDataStore<User> userStore;

    public Page<User> findActiveUsers(Pageable pageable) {
        // Using MapBasedSpecification for flexible querying
        var spec = MapBasedSpecificationBuilder.forClass(User.class)
                .where("status", "ACTIVE")
                .build();

        return userStore.findAll(spec, pageable);
    }

    public List<User> findHighValueCustomers() {
        // Direct querying without pagination
        return userStore.findAll().stream()
                .filter(user -> user.getOrderCount() > 5)
                .filter(user -> user.getTotalOrderValue() > 1000.0)
                .sorted((u1, u2) -> Double.compare(u2.getTotalOrderValue(), u1.getTotalOrderValue()))
                .collect(Collectors.toList());
    }

    public Optional<User> findUserById(Long id) {
        return userStore.findById(id);
    }
}
```

### 3. REST API Integration

```java

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private InMemoryDataStore<User> userStore;

    @GetMapping
    public Page<User> getUsers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Double minOrderValue,
            Pageable pageable) {

        // Build dynamic specification
        var specBuilder = MapBasedSpecificationBuilder.forClass(User.class);

        if (status != null) {
            specBuilder.where("status", status);
        }

        if (minOrderValue != null) {
            specBuilder.where("totalOrderValue", ">=", minOrderValue);
        }

        var spec = specBuilder.build();
        return userStore.findAll(spec, pageable);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userStore.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public Map<String, Object> getUserStats() {
        List<User> allUsers = userStore.findAll();

        return Map.of(
                "totalUsers", allUsers.size(),
                "activeUsers", allUsers.stream().filter(u -> "ACTIVE".equals(u.getStatus())).count(),
                "averageOrderCount", allUsers.stream().mapToInt(User::getOrderCount).average().orElse(0.0),
                "totalOrderValue", allUsers.stream().mapToDouble(User::getTotalOrderValue).sum()
        );
    }
}
```

## 🔍 Dynamic Querying with URL Parameters

### MapBasedSpecificationBuilder - REST API Query Parameters

The `MapBasedSpecificationBuilder` allows you to build complex queries directly from URL parameters, making it perfect
for REST APIs where users need flexible filtering capabilities.

#### Basic Usage

```java

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private InMemoryDataStore<User> userStore;

    @GetMapping("/search")
    public Page<User> searchUsers(
            @RequestParam Map<String, String> searchParams,
            Pageable pageable) {

        // Convert URL parameters directly to specification
        var spec = new MapBasedSpecificationBuilder<>(User.class)
                .build(searchParams);

        return userStore.findAll(spec, pageable);
    }
}
```

#### URL Parameter Format

Parameters follow the pattern: `fieldName.operator=value`

**Basic Examples:**

```
GET /api/users/search?name=John&status=ACTIVE
GET /api/users/search?age.gt=25&salary.gte=50000
GET /api/users/search?email.sw=john&department.name.ne=HR
```

#### Supported Operators

##### Equality & Comparison

```
# Equality (default if no operator specified)
?name=John Smith
?status.eq=ACTIVE
?status.ne=INACTIVE

# Numeric comparisons  
?age.gt=30                    # greater than
?salary.gte=50000            # greater than or equal
?orderCount.lt=10            # less than
?totalAmount.lte=1000        # less than or equal

# Range operations
?salary.btw=50000,80000      # between (inclusive)
?age.nbtw=25,35             # not between
```

##### String Operations

```
# String matching
?name.sw=John               # starts with
?email.ew=@company.com      # ends with
?description.contains=java   # contains substring

# Case-insensitive operations
?name.ieq=JOHN              # equals ignore case
?name.isw=john              # starts with ignore case
?name.iew=SMITH             # ends with ignore case
?description.icontains=JAVA  # contains ignore case

# Pattern matching
?name.regex=^J.*son$        # regex pattern
```

##### Collection Operations

```
# Value in collection
?status.in=ACTIVE,PENDING   # status is one of these values
?department.nin=HR,Legal    # department is not one of these

# Collection field operations
?skills.ccontains=Java      # skills collection contains "Java"
?skills.cany=Java,Python    # skills contains any of these values
?skills.call=Java,Spring    # skills contains all of these values
?skills.csize=3             # skills collection has exactly 3 items
?skills.csgt=2              # skills collection size > 2
?skills.cempty=             # skills collection is empty
```

##### Null & Empty Checks

```
?email.null=                # email is null
?email.nnull=               # email is not null
?description.empty=         # description is empty string
?description.nempty=        # description is not empty
?name.blank=                # name is null or whitespace
?name.nblank=               # name is not blank
```

##### Date & Time Operations

```
?hireDate.before=2020-01-01
?hireDate.after=2019-12-31
?hireDate.onorbefore=2020-01-01
?hireDate.onorafter=2020-01-01
?lastLogin.btw=2024-01-01T00:00:00,2024-12-31T23:59:59
```

#### Real-World Examples

##### Employee Search API

```java

@GetMapping("/employees/search")
public Page<Employee> searchEmployees(
        @RequestParam Map<String, String> params,
        Pageable pageable) {

    var spec = new MapBasedSpecificationBuilder<>(Employee.class)
            .build(params);

    return employeeStore.findAll(spec, pageable);
}
```

**Example URLs:**

```
# Find active senior developers
GET /employees/search?active=true&age.gte=35&skills.ccontains=Java&salary.gte=75000

# Find recent hires in specific departments
GET /employees/search?hireDate.after=2023-01-01&department.name.in=Engineering,Marketing

# Find employees with specific skill combinations
GET /employees/search?skills.call=Java,Spring&skills.csize.gte=3

# Complex search with multiple conditions
GET /employees/search?name.sw=J&age.btw=25,45&salary.gte=60000&department.name.ne=HR&active=true
```

##### Product Catalog API

```java

@GetMapping("/products/search")
public Page<Product> searchProducts(
        @RequestParam Map<String, String> params,
        Pageable pageable) {

    var spec = new MapBasedSpecificationBuilder<>(Product.class)
            .build(params);

    return productStore.findAll(spec, pageable);
}
```

**Example URLs:**

```
# Price range with category filter
GET /products/search?price.btw=100,500&category.name=Electronics&inStock=true

# Text search with rating filter
GET /products/search?name.icontains=laptop&description.icontains=gaming&rating.gte=4.0

# Advanced filtering
GET /products/search?tags.cany=bestseller,featured&reviews.csize.gte=10&discount.gt=0
```

#### Advanced Features

##### Nested Object Queries

```
# Query nested object properties
?profile.address.city=New York
?profile.preferences.theme=dark
?department.manager.name.sw=John
```

##### Collection Element Queries

```
# Query elements within collections
?orders.any.status=PENDING          # any order has PENDING status
?orders.all.amount.gte=100          # all orders have amount >= 100
?orders.none.status=CANCELLED       # no orders are CANCELLED
```

##### Custom Parameter Processing

```java

@GetMapping("/users/advanced-search")
public Page<User> advancedSearch(
        @RequestParam Map<String, String> params,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDir,
        Pageable pageable) {

    // Remove non-filter parameters
    Map<String, String> filterParams = new HashMap<>(params);
    filterParams.remove("sortBy");
    filterParams.remove("sortDir");
    filterParams.remove("page");
    filterParams.remove("size");

    // Build specification
    var spec = new MapBasedSpecificationBuilder<>(User.class)
            .build(filterParams);

    // Apply custom sorting if specified
    if (sortBy != null) {
        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDir) ?
                        Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy
        );
        pageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );
    }

    return userStore.findAll(spec, pageable);
}
```

#### Operator Shortcuts

For convenience, most operators have short aliases:

```
# Full operator names
?age.greaterThan=30
?name.startsWith=John
?skills.collectionContains=Java
?salary.between=50000,80000

# Short aliases (equivalent)
?age.gt=30
?name.sw=John
?skills.ccontains=Java
?salary.btw=50000,80000
```

#### Error Handling

```java

@GetMapping("/users/search")
public ResponseEntity<?> searchUsers(
        @RequestParam Map<String, String> params,
        Pageable pageable) {

    try {
        var spec = new MapBasedSpecificationBuilder<>(User.class)
                .build(params);

        Page<User> results = userStore.findAll(spec, pageable);
        return ResponseEntity.ok(results);

    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "error", "Invalid search parameters",
                        "message", e.getMessage(),
                        "parameters", params
                ));
    }
}
```

#### Performance Tips

1. **Use Indexes**: Ensure frequently queried fields are indexed in your data sources
2. **Limit Parameters**: Consider limiting the number of parameters to prevent complex queries
3. **Validate Input**: Always validate parameter values before processing
4. **Cache Specifications**: Cache compiled specifications for repeated queries

```java

@Service
public class SearchService {

    private final Cache<String, Specification<User>> specCache =
            Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(Duration.ofMinutes(10))
                    .build();

    public Page<User> searchUsers(Map<String, String> params, Pageable pageable) {
        String cacheKey = params.toString();

        Specification<User> spec = specCache.get(cacheKey, key ->
                new MapBasedSpecificationBuilder<>(User.class).build(params)
        );

        return userStore.findAll(spec, pageable);
    }
}
```

## 🏗️ Advanced Features

### Complex Nested Hierarchies

```java
// Multi-level hierarchy: User -> Profile -> Address
public InMemoryDataStore<User> createComplexUserStore() {
    // Data sources
    var userDataSource = new DatabaseDataSource<>("users", User.class, jdbcTemplate);
    var profileDataSource = new DatabaseDataSource<>("profiles", Profile.class, jdbcTemplate);
    var addressDataSource = new DatabaseDataSource<>("addresses", Address.class, jdbcTemplate);
    var orderDataSource = new DatabaseDataSource<>("orders", Order.class, jdbcTemplate);

    // Profile configuration (nested object)
    var profileConfig = SubObjectConfig.builder()
            .propertyName("profile")
            .objectType(Profile.class)
            .addPropertyConfig(PropertyDataSourceConfig.<Profile>builder()
                    .propertyName("profile")
                    .dataSource(profileDataSource)
                    .dataSourceName("profiles")
                    .primaryKeyField("id")
                    .foreignKeyField("userId")
                    .mapper(profile -> profile)
                    .build())
            .build();

    // Orders configuration (collection)
    var ordersConfig = PropertyDataSourceConfig.<Order>builder()
            .propertyName("orders")
            .dataSource(orderDataSource)
            .dataSourceName("orders")
            .primaryKeyField("id")
            .foreignKeyField("userId")
            .mapper(order -> order)
            .isCollection(true)
            .build();

    // Order count aggregation
    var orderCountConfig = PropertyDataSourceConfig.<Order>builder()
            .propertyName("orderCount")
            .dataSource(orderDataSource)
            .dataSourceName("orders")
            .primaryKeyField("id")
            .foreignKeyField("userId")
            .mapper(order -> order)
            .isAggregation(true)
            .aggregationType(AggregationType.COUNT)
            .build();

    // Build configuration
    var config = BuildConfiguration.<User>builder(User.class)
            .withPrimaryDataSource(userDataSource)
            .withPropertyConfig(ordersConfig)
            .withPropertyConfig(orderCountConfig)
            .withSubObjectConfig(profileConfig)
            .build();

    return new InMemoryDataStore<>(config);
}
```

### Custom Aggregations

```java
// Average rating calculation
var averageRatingConfig = PropertyDataSourceConfig.<Review>builder()
                .propertyName("averageRating")
                .dataSource(reviewDataSource)
                .dataSourceName("reviews")
                .primaryKeyField("id")
                .foreignKeyField("productId")
                .mapper(review -> review)
                .isAggregation(true)
                .aggregationType(AggregationType.CUSTOM)
                .customAggregationFunction(reviews ->
                        reviews.stream()
                                .mapToDouble(Review::getRating)
                                .average()
                                .orElse(0.0))
                .build();

// Top reviews with filtering
var topReviewsConfig = PropertyDataSourceConfig.<Review>builder()
        .propertyName("topReviews")
        .dataSource(reviewDataSource)
        .dataSourceName("reviews")
        .primaryKeyField("id")
        .foreignKeyField("productId")
        .mapper(review -> review)
        .isAggregation(true)
        .aggregationType(AggregationType.CUSTOM)
        .aggregationFilter(review -> review.getRating() >= 4.0)
        .customAggregationFunction(reviews ->
                reviews.stream()
                        .sorted((r1, r2) -> r2.getRating().compareTo(r1.getRating()))
                        .limit(5)
                        .collect(Collectors.toList()))
        .build();

// Numeric aggregations
var totalAmountConfig = PropertyDataSourceConfig.<Order>builder()
        .propertyName("totalAmount")
        .dataSource(orderDataSource)
        .dataSourceName("orders")
        .primaryKeyField("id")
        .foreignKeyField("userId")
        .mapper(order -> order)
        .isAggregation(true)
        .aggregationType(AggregationType.SUM)
        .aggregationField("amount")
        .build();
```

### Fallback Data Sources

```java
// Create fallback chain: Database -> Cache -> File
var primaryDataSource = new DatabaseDataSource<>("users", User.class, jdbcTemplate);
var cacheDataSource = new CacheDataSource<>("user-cache", User.class, cacheManager);
var fileDataSource = new FileDataSource<>("users-backup.json", User.class);

// Configure fallback chain
primaryDataSource.

setFallbackDataSource(cacheDataSource);
cacheDataSource.

setFallbackDataSource(fileDataSource);

// Build configuration with fallback support
var config = BuildConfiguration.<User>builder(User.class)
        .withPrimaryDataSource(primaryDataSource) // Fallback chain included automatically
        .build();

var userStore = new InMemoryDataStore<>(config);

// DataSource automatically handles fallback:
// 1. Try database first
// 2. If database fails, try cache
// 3. If cache fails, try file
// 4. If all fail, return empty list
```

## 📊 Monitoring and Health Checks

### Store Status Monitoring

```java

@Component
public class DataStoreMonitor {

    @Autowired
    private InMemoryDataStore<User> userStore;

    @Scheduled(fixedRate = 60000) // Every minute
    public void monitorHealth() {
        StoreStatus status = userStore.getStatus();

        logger.info("Store Status - Size: {}, Last Sync: {}, Status: {}",
                status.getDataSize(),
                status.getLastSyncTime(),
                status.getStatus());

        // Check if store is healthy
        if (status.getStatus() != StoreStatus.Status.ACTIVE) {
            alertService.sendAlert("DataStore is not active: " + status.getStatus());
        }

        // Check data freshness
        if (status.getLastSyncTime() != null &&
                status.getLastSyncTime().isBefore(LocalDateTime.now().minusMinutes(10))) {
            alertService.sendAlert("Data appears stale - last sync: " + status.getLastSyncTime());
        }

        // Log current data size
        logger.debug("Current data size: {} users", status.getDataSize());
    }

    @GetMapping("/health/datastore")
    public ResponseEntity<Map<String, Object>> getDataStoreHealth() {
        StoreStatus status = userStore.getStatus();

        Map<String, Object> health = Map.of(
                "status", status.getStatus().toString(),
                "dataSize", status.getDataSize(),
                "lastSyncTime", status.getLastSyncTime(),
                "isHealthy", status.getStatus() == StoreStatus.Status.ACTIVE
        );

        return ResponseEntity.ok(health);
    }
}
```

### Performance Metrics

```java

@Component
public class DataStoreMetrics {

    private final MeterRegistry meterRegistry;

    public DataStoreMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void recordMetrics() {
        // Record data store size
        meterRegistry.gauge("datastore.size",
                Tags.of("store", "users"),
                userStore.findAll().size());

        // Record query performance
        long startTime = System.currentTimeMillis();
        userStore.findAll();
        long duration = System.currentTimeMillis() - startTime;

        meterRegistry.timer("datastore.query.duration",
                        Tags.of("store", "users", "operation", "findAll"))
                .record(duration, TimeUnit.MILLISECONDS);
    }

    public void recordCustomQuery(String queryType, long duration) {
        meterRegistry.timer("datastore.query.duration",
                        Tags.of("store", "users", "operation", queryType))
                .record(duration, TimeUnit.MILLISECONDS);
    }
}
```

## 🧪 Testing

### Unit Testing

```java

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private InMemoryDataStore<User> userStore;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldFindActiveUsers() {
        // Given
        List<User> activeUsers = Arrays.asList(
                new User(1L, "John", "Doe", "ACTIVE"),
                new User(2L, "Jane", "Smith", "ACTIVE")
        );

        when(userStore.findAll()).thenReturn(activeUsers);

        // When
        List<User> result = userService.findActiveUsers();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(user -> "ACTIVE".equals(user.getStatus()));
    }

    @Test
    void shouldFindUserById() {
        // Given
        User expectedUser = new User(1L, "John", "Doe", "ACTIVE");
        when(userStore.findById(1L)).thenReturn(Optional.of(expectedUser));

        // When
        Optional<User> result = userService.findUserById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("John");
    }
}
```

### Integration Testing

```java

@SpringBootTest
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:testdb")
class UserStoreIntegrationTest {

    @Autowired
    private InMemoryDataStore<User> userStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldLoadDataFromDatabase() {
        // Given - Insert test data
        jdbcTemplate.update("INSERT INTO users (id, name, email, status) VALUES (?, ?, ?, ?)",
                1L, "John Doe", "john@example.com", "ACTIVE");
        jdbcTemplate.update("INSERT INTO users (id, name, email, status) VALUES (?, ?, ?, ?)",
                2L, "Jane Smith", "jane@example.com", "INACTIVE");

        // Trigger manual sync
        userStore.synchronize();

        // When
        List<User> users = userStore.findAll();

        // Then
        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getName)
                .containsExactlyInAnyOrder("John Doe", "Jane Smith");
    }

    @Test
    void shouldHandleSpecificationQueries() {
        // Given
        setupTestData();

        // When
        var spec = MapBasedSpecificationBuilder.forClass(User.class)
                .where("status", "ACTIVE")
                .build();

        Page<User> result = userStore.findAll(spec, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).allMatch(user -> "ACTIVE".equals(user.getStatus()));
    }
}
```

## 📚 Documentation

### Core Documentation

- [API Documentation](docs/api-documentation.md) - Comprehensive API reference
- [Troubleshooting Guide](docs/troubleshooting-guide.md) - Common issues and solutions
- [Performance Tuning](docs/performance-tuning-guide.md) - Optimization techniques
- [Best Practices](docs/best-practices.md) - Production-ready patterns
- [Migration Guide](docs/migration-guide.md) - Migrating from existing solutions

### Working Examples

#### Complete User-Order Example

```java
// Entity classes
public class User {
    private Long id;
    private String name;
    private String email;
    private String status;
    private List<Order> orders = new ArrayList<>();
    private int orderCount;
    private double totalAmount;

    // constructors, getters, setters...
}

public class Order {
    private Long id;
    private Long userId;
    private String product;
    private double amount;
    private String status;

    // constructors, getters, setters...
}

// Configuration
@Configuration
public class DataStoreConfig {

    @Bean
    public InMemoryDataStore<User> userStore(JdbcTemplate jdbcTemplate) {
        // Create data sources
        var userDataSource = new DatabaseDataSource<>("users", User.class, jdbcTemplate);
        var orderDataSource = new DatabaseDataSource<>("orders", Order.class, jdbcTemplate);

        // Configure orders collection
        var ordersConfig = PropertyDataSourceConfig.<Order>builder()
                .propertyName("orders")
                .dataSource(orderDataSource)
                .dataSourceName("orders")
                .primaryKeyField("id")
                .foreignKeyField("userId")
                .mapper(order -> order)
                .isCollection(true)
                .build();

        // Configure order count aggregation
        var orderCountConfig = PropertyDataSourceConfig.<Order>builder()
                .propertyName("orderCount")
                .dataSource(orderDataSource)
                .dataSourceName("orders")
                .primaryKeyField("id")
                .foreignKeyField("userId")
                .mapper(order -> order)
                .isAggregation(true)
                .aggregationType(AggregationType.COUNT)
                .build();

        // Configure total amount aggregation
        var totalAmountConfig = PropertyDataSourceConfig.<Order>builder()
                .propertyName("totalAmount")
                .dataSource(orderDataSource)
                .dataSourceName("orders")
                .primaryKeyField("id")
                .foreignKeyField("userId")
                .mapper(order -> order)
                .isAggregation(true)
                .aggregationType(AggregationType.SUM)
                .aggregationField("amount")
                .build();

        // Build configuration
        var config = BuildConfiguration.<User>builder(User.class)
                .withPrimaryDataSource(userDataSource)
                .withPropertyConfig(ordersConfig)
                .withPropertyConfig(orderCountConfig)
                .withPropertyConfig(totalAmountConfig)
                .withSyncInterval(Duration.ofMinutes(5))
                .build();

        return new InMemoryDataStore<>(config);
    }
}

// Usage in service
@Service
public class UserService {

    @Autowired
    private InMemoryDataStore<User> userStore;

    public List<User> findActiveUsers() {
        return userStore.findAll().stream()
                .filter(user -> "ACTIVE".equals(user.getStatus()))
                .collect(Collectors.toList());
    }

    public Page<User> findHighValueCustomers(Pageable pageable) {
        var spec = MapBasedSpecificationBuilder.forClass(User.class)
                .where("totalAmount", ">=", 1000.0)
                .where("orderCount", ">=", 5)
                .build();

        return userStore.findAll(spec, pageable);
    }
}
```

### Coverage and Quality

- **Code Coverage**: 70%+ line coverage, 60%+ branch coverage
- **Testing**: Comprehensive unit and integration tests
- **Documentation**: Complete JavaDoc for all public APIs
- **Examples**: Working examples for all major features

## 🔧 Configuration Options

### Data Source Types

```java
// Database DataSource
var dbDataSource = new DatabaseDataSource<>("users", User.class, jdbcTemplate);

// REST API DataSource  
var apiDataSource = new RestApiDataSource<>("users", User.class, restTemplate, "/api/users");

// File DataSource
var fileDataSource = new FileDataSource<>("users.json", User.class);

// In-Memory DataSource (for testing)
var memoryDataSource = new InMemoryDataSource<>("users", User.class);

// Cache DataSource
var cacheDataSource = new CacheDataSource<>("users", User.class, cacheManager);
```

### Synchronization Settings

```java
// Configure automatic synchronization
var config = BuildConfiguration.<User>builder(User.class)
                .withPrimaryDataSource(dataSource)
                .withSyncInterval(Duration.ofMinutes(5)) // Sync every 5 minutes
                .build();

// Manual synchronization
userStore.

synchronize(); // Trigger immediate sync

// Check sync status
StoreStatus status = userStore.getStatus();
LocalDateTime lastSync = status.getLastSyncTime();
```

### Memory Optimization

```java
// Use aggregations instead of loading all related data
var orderCountConfig = PropertyDataSourceConfig.<Order>builder()
                .propertyName("orderCount")
                .dataSource(orderDataSource)
                .primaryKeyField("id")
                .foreignKeyField("userId")
                .isAggregation(true)
                .aggregationType(AggregationType.COUNT)
                .build();

// Filter data at source to reduce memory usage
var recentOrdersConfig = PropertyDataSourceConfig.<Order>builder()
        .propertyName("recentOrders")
        .dataSource(orderDataSource)
        .primaryKeyField("id")
        .foreignKeyField("userId")
        .isAggregation(true)
        .aggregationType(AggregationType.CUSTOM)
        .aggregationFilter(order ->
                order.getOrderDate().isAfter(LocalDateTime.now().minusDays(30)))
        .customAggregationFunction(orders ->
                orders.stream().limit(10).collect(Collectors.toList()))
        .build();
```

## 🚀 Performance

### Current Implementation Status

- **In-Memory Storage**: Fast read operations with O(1) lookups by ID
- **Reflection Caching**: Optimized field access with ReflectionUtils
- **Type Safety**: Compile-time type checking with generics
- **Concurrent Access**: Thread-safe operations for read queries
- **Aggregation Support**: Built-in COUNT, SUM, AVG, MIN, MAX, and custom functions

### Optimization Tips

- Use aggregations instead of loading full collections when possible
- Configure appropriate sync intervals based on data change frequency
- Implement proper pagination for large result sets
- Monitor memory usage and data store size in production
- Use specifications for efficient filtering instead of post-processing

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

### Development Setup

```bash
# Clone the repository
git clone https://github.com/your-org/multi-source-data-library.git

# Build the project
./gradlew build

# Run tests
./gradlew test

# Run coverage analysis
./gradlew jacocoTestReport
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Documentation**: Check the [docs/](docs/) directory
- **Examples**: See [examples/](examples/) for working code
- **Issues**: Report bugs on [GitHub Issues](https://github.com/your-org/multi-source-data-library/issues)
- **Discussions**: Join our [GitHub Discussions](https://github.com/your-org/multi-source-data-library/discussions)

## 🗺️ Current Status & Roadmap

### Version 1.0 (Current Implementation)

- ✅ Core ObjectBuildingEngine with aggregation support
- ✅ Multiple DataSource implementations (Database, REST API, File, Cache, InMemory)
- ✅ Type-safe PropertyDataSourceConfig with builder pattern
- ✅ Nested object hierarchies with SubObjectConfig
- ✅ Fallback chain support for high availability
- ✅ Spring Data integration with Specification queries
- ✅ Comprehensive error handling and monitoring
- ✅ 70%+ code coverage with extensive test suite

### Version 1.1 (In Progress)

- 🔄 Enhanced Factory API (InMemoryDataFactory) - Partially implemented
- 🔄 Fluent builder patterns (PropertyBuilder, SubObjectBuilder)
- 🔄 Advanced integration tests
- 🔄 Performance benchmarking and optimization

### Version 2.0 (Planned)

- 📋 Reactive streams support
- 📋 GraphQL integration
- 📋 Enhanced caching strategies
- 📋 Distributed data store support
- 📋 Advanced query optimization

## 📝 Implementation Notes

### Current Status

This library is actively developed with a focus on production readiness. The core functionality is implemented and
tested:

- **ObjectBuildingEngine**: Fully implemented with aggregation support
- **DataSource Implementations**: Database, REST API, File, Cache, and InMemory sources available
- **Configuration System**: Type-safe PropertyDataSourceConfig and BuildConfiguration
- **Spring Integration**: MapBasedSpecification for flexible querying
- **Error Handling**: Comprehensive exception handling and fallback support
- **Testing**: 70%+ code coverage with unit and integration tests

### Factory API Status

The fluent Factory API (InMemoryDataFactory, PropertyBuilder, SubObjectBuilder) is partially implemented and currently
disabled in tests. The core functionality works through direct configuration objects as shown in the examples above.

### Getting Started

1. Use the direct configuration approach shown in the examples
2. Start with simple User-Order relationships
3. Add aggregations as needed
4. Implement proper error handling and monitoring
5. Test thoroughly before production deployment

---

**Built with ❤️ for the Java community**