package com.thy.fss.common.inmemory.common;

import com.thy.fss.common.inmemory.testmodel.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility for generating large datasets for performance and scalability testing.
 * Uses fixed seed for reproducibility.
 * 
 * <p>The library is designed for large-scale data aggregation, so tests must use
 * realistic data volumes (10K-1M entities) to verify performance characteristics.</p>
 */
public class LargeDatasetGenerator {
    private static final String CUSTOMER_PREFIX = "Customer";
    private static final String EXAMPLE_COM = "@example.com";
    private static final String TEST_USER_PREFIX = "TestUser";

    private static final long DEFAULT_SEED = 42L;
    private static final int DEFAULT_AVG_ORDERS_PER_USER = 5;

    private final Random random;

    /**
     * Creates generator with default seed for reproducibility.
     */
    public LargeDatasetGenerator() {
        this(DEFAULT_SEED);
    }

    /**
     * Creates generator with custom seed.
     * 
     * @param seed random seed for reproducibility
     */
    public LargeDatasetGenerator(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Generates users with realistic distribution.
     * 
     * @param count number of users to generate (10K-1M recommended)
     * @return list of generated users
     */
    public List<User> generateUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User u = new User();
            u.setName("User_" + i); // name alanı boş/whitespace/null OLMAMALI
            u.setId(String.valueOf(i));
            users.add(u);
            Profile profile = new Profile();
            profile.setBio("Bio_" + i);
            profile.setFollowers(5000 + random.nextInt(900));
            u.setProfile(profile);
        }
        return users;
    }
    /**
     * Generates orders with realistic distribution.
     * 
     * @param userCount number of users
     * @param avgOrdersPerUser average orders per user
     * @return list of generated orders
     */
    public List<Order> generateOrders(int userCount, int avgOrdersPerUser) {
        List<Order> orders = new ArrayList<>(userCount * avgOrdersPerUser);
        
        for (long userId = 0; userId < userCount; userId++) {
            int orderCount = avgOrdersPerUser + random.nextInt(avgOrdersPerUser) - avgOrdersPerUser / 2;
            orderCount = Math.max(0, orderCount);
            
            for (int j = 0; j < orderCount; j++) {
                Order order = new Order();
                order.setId((long) orders.size());
                order.setCustomerId(userId);
                order.setOrderDate(LocalDateTime.now().minusDays(random.nextInt(365)));
                order.setTotalAmount(10.0 + random.nextDouble() * 990.0);
                order.setStatus(random.nextBoolean() ? "COMPLETED" : "PENDING");
                order.setLastUpdated(LocalDateTime.now());
                
                orders.add(order);
            }
        }
        
        return orders;
    }

    /**
     * Generates orders with default average (5 per user).
     * 
     * @param userCount number of users
     * @return list of generated orders
     */
    public List<Order> generateOrders(int userCount) {
        return generateOrders(userCount, DEFAULT_AVG_ORDERS_PER_USER);
    }

    /**
     * Generates complete dataset with users and orders.
     * 
     * @param userCount number of users
     * @param avgOrdersPerUser average orders per user
     * @return complete dataset
     */
    public CompleteDataset generateCompleteDataset(int userCount, int avgOrdersPerUser) {
        List<User> users = generateUsers(userCount);
        List<Order> orders = generateOrders(userCount, avgOrdersPerUser);
        return new CompleteDataset(users, orders);
    }

    /**
     * Generates complete dataset with default average (5 orders per user).
     * 
     * @param userCount number of users
     * @return complete dataset
     */
    public CompleteDataset generateCompleteDataset(int userCount) {
        return generateCompleteDataset(userCount, DEFAULT_AVG_ORDERS_PER_USER);
    }

    /**
     * Generates customers with realistic distribution.
     * 
     * @param count number of customers to generate
     * @return list of generated customers
     */
    public List<Customer> generateCustomers(int count) {
        List<Customer> customers = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            Customer customer = new Customer();
            customer.setId((long) i);
            customer.setName(CUSTOMER_PREFIX + i);
            customer.setEmail(CUSTOMER_PREFIX + i + EXAMPLE_COM);
            customer.setActive(random.nextBoolean());
            
            customers.add(customer);
        }
        
        return customers;
    }

    /**
     * Generates products with realistic distribution.
     * 
     * @param count number of products to generate
     * @return list of generated products
     */
    public List<Product> generateProducts(int count) {
        List<Product> products = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            Product product = new Product();
            product.setId((long) i);
            product.setName("Product" + i);
            product.setPrice(10.0 + random.nextDouble() * 990.0);
            product.setStock(random.nextInt(1000));
            product.setCategory("Category" + (i % 10));
            
            products.add(product);
        }
        
        return products;
    }

    /**
     * Generates test users with realistic distribution.
     * 
     * @param count number of test users to generate
     * @return list of generated test users
     */
    public List<TestUser> generateTestUsers(int count) {
        List<TestUser> users = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            TestUser user = new TestUser();
            user.setId((long) i);
            user.setName(TEST_USER_PREFIX + i);
            user.setEmail(TEST_USER_PREFIX + i + EXAMPLE_COM);
            user.setActive(random.nextBoolean());
            
            users.add(user);
        }
        
        return users;
    }

    /**
     * Generates simple users with realistic distribution.
     * 
     * @param count number of simple users to generate
     * @return list of generated simple users
     */
    public List<SimpleUser> generateSimpleUsers(int count) {
        List<SimpleUser> users = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            SimpleUser user = new SimpleUser();
            user.setId((long) i);
            user.setName("SimpleUser" + i);
            user.setAge(20 + random.nextInt(60));
            user.setActive(random.nextBoolean());
            
            users.add(user);
        }
        
        return users;
    }

    /**
     * Generates temporal entities with realistic distribution.
     * 
     * @param count number of temporal entities to generate
     * @return list of generated temporal entities
     */
    public List<TemporalEntity> generateTemporalEntities(int count) {
        List<TemporalEntity> entities = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            TemporalEntity entity = new TemporalEntity();
            entity.setId((long) i);
            entity.setBirthDate(LocalDate.now().minusDays(random.nextInt(365)));
            entity.setCreatedAt(LocalDateTime.now().minusHours(random.nextInt(8760)));
            entity.setLastModified(java.time.Instant.now().minusSeconds(random.nextInt(31536000)));
            
            entities.add(entity);
        }
        
        return entities;
    }

    /**
     * Generates temporal entities with dates within specified range.
     * 
     * @param count number of temporal entities to generate
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of generated temporal entities
     */
    public static List<TemporalEntity> generateTemporalEntities(int count, LocalDate startDate, LocalDate endDate) {
        List<TemporalEntity> entities = new ArrayList<>(count);
        Random random = new Random(DEFAULT_SEED);
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        
        for (int i = 0; i < count; i++) {
            TemporalEntity entity = new TemporalEntity();
            entity.setId((long) i);
            
            long randomDays = random.nextLong(daysBetween + 1);
            entity.setBirthDate(startDate.plusDays(randomDays));
            
            entity.setCreatedAt(LocalDateTime.now().minusHours(random.nextInt(8760)));
            entity.setLastModified(java.time.Instant.now().minusSeconds(random.nextInt(31536000)));
            
            entities.add(entity);
        }
        
        return entities;
    }

    /**
     * Generates temporal entities with LocalDateTime within specified range.
     * 
     * @param count number of temporal entities to generate
     * @param startDateTime start of datetime range
     * @param endDateTime end of datetime range
     * @return list of generated temporal entities
     */
    public static List<TemporalEntity> generateTemporalEntitiesWithDateTime(int count, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        List<TemporalEntity> entities = new ArrayList<>(count);
        Random random = new Random(DEFAULT_SEED);
        long secondsBetween = java.time.temporal.ChronoUnit.SECONDS.between(startDateTime, endDateTime);
        
        for (int i = 0; i < count; i++) {
            TemporalEntity entity = new TemporalEntity();
            entity.setId((long) i);
            
            long randomSeconds = random.nextLong(secondsBetween + 1);
            entity.setCreatedAt(startDateTime.plusSeconds(randomSeconds));
            
            entity.setBirthDate(LocalDate.now().minusDays(random.nextInt(365)));
            entity.setLastModified(java.time.Instant.now().minusSeconds(random.nextInt(31536000)));
            
            entities.add(entity);
        }
        
        return entities;
    }

    /**
     * Generates temporal entities with Instant within specified range.
     * 
     * @param count number of temporal entities to generate
     * @param startInstant start of instant range
     * @param endInstant end of instant range
     * @return list of generated temporal entities
     */
    public static List<TemporalEntity> generateTemporalEntitiesWithInstant(int count, java.time.Instant startInstant, java.time.Instant endInstant) {
        List<TemporalEntity> entities = new ArrayList<>(count);
        Random random = new Random(DEFAULT_SEED);
        long secondsBetween = java.time.temporal.ChronoUnit.SECONDS.between(startInstant, endInstant);
        
        for (int i = 0; i < count; i++) {
            TemporalEntity entity = new TemporalEntity();
            entity.setId((long) i);
            
            long randomSeconds = random.nextLong(secondsBetween + 1);
            entity.setLastModified(startInstant.plusSeconds(randomSeconds));
            
            entity.setBirthDate(LocalDate.now().minusDays(random.nextInt(365)));
            entity.setCreatedAt(LocalDateTime.now().minusHours(random.nextInt(8760)));
            
            entities.add(entity);
        }
        
        return entities;
    }

    /**
     * Generates temporal entities with dates within specified range (overloaded for Instant).
     * 
     * @param count number of temporal entities to generate
     * @param startInstant start of instant range
     * @param endInstant end of instant range
     * @return list of generated temporal entities
     */
    public static List<TemporalEntity> generateTemporalEntities(int count, java.time.Instant startInstant, java.time.Instant endInstant) {
        return generateTemporalEntitiesWithInstant(count, startInstant, endInstant);
    }

    /**
     * Generates temporal entities with dates within specified range (overloaded for LocalDateTime).
     * 
     * @param count number of temporal entities to generate
     * @param startDateTime start of datetime range
     * @param endDateTime end of datetime range
     * @return list of generated temporal entities
     */
    public static List<TemporalEntity> generateTemporalEntities(int count, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return generateTemporalEntitiesWithDateTime(count, startDateTime, endDateTime);
    }

    /**
     * Generates collection entities with realistic distribution.
     * 
     * @param count number of collection entities to generate
     * @return list of generated collection entities
     */
    public List<CollectionEntity> generateCollectionEntities(int count) {
        List<CollectionEntity> entities = new ArrayList<>(count);
        
        for (int i = 0; i < count; i++) {
            CollectionEntity entity = new CollectionEntity();
            entity.setId((long) i);
            
            List<String> tags = new ArrayList<>();
            int tagCount = random.nextInt(10);
            for (int j = 0; j < tagCount; j++) {
                tags.add("Tag" + j);
            }
            entity.setTags(tags);
            
            entities.add(entity);
        }
        
        return entities;
    }

    /**
     * Generates entities with enum with realistic distribution.
     * 
     * @param count number of entities to generate
     * @return list of generated entities
     */
    public List<EntityWithEnum> generateEntitiesWithEnum(int count) {
        List<EntityWithEnum> entities = new ArrayList<>(count);
        Status[] statuses = Status.values();
        
        for (int i = 0; i < count; i++) {
            EntityWithEnum entity = new EntityWithEnum();
            entity.setId((long) i);
            entity.setName("EntityWithEnum" + i);
            entity.setStatus(statuses[random.nextInt(statuses.length)]);
            
            entities.add(entity);
        }
        
        return entities;
    }

    /**
     * Holder for complete dataset with users and orders.
     */
    public static class CompleteDataset {
        private final List<User> users;
        private final List<Order> orders;

        public CompleteDataset(List<User> users, List<Order> orders) {
            this.users = users;
            this.orders = orders;
        }

        public List<User> getUsers() {
            return users;
        }

        public List<Order> getOrders() {
            return orders;
        }
    }

    /**
     * Creates a generator with default seed.
     * 
     * @return new generator instance
     */
    public static LargeDatasetGenerator create() {
        return new LargeDatasetGenerator();
    }

    /**
     * Creates a generator with custom seed.
     * 
     * @param seed random seed
     * @return new generator instance
     */
    public static LargeDatasetGenerator create(long seed) {
        return new LargeDatasetGenerator(seed);
    }
}
