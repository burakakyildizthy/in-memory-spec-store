package com.thy.fss.common.inmemory.specification;

import com.thy.fss.common.inmemory.filter.CollectionFilter;
import com.thy.fss.common.inmemory.filter.IntegerFilter;
import com.thy.fss.common.inmemory.filter.StringFilter;
import com.thy.fss.common.inmemory.testmodel.Customer;
import com.thy.fss.common.inmemory.testmodel.CustomerFilter;
import com.thy.fss.common.inmemory.testmodel.CustomerSpecificationService;
import com.thy.fss.common.inmemory.testmodel.Order;
import com.thy.fss.common.inmemory.testmodel.OrderFilter;
import com.thy.fss.common.inmemory.testmodel.Profile;
import com.thy.fss.common.inmemory.testmodel.ProfileFilter;
import com.thy.fss.common.inmemory.testmodel.ProfileSpecificationService;
import com.thy.fss.common.inmemory.testmodel.User;
import com.thy.fss.common.inmemory.testmodel.UserFilter;
import com.thy.fss.common.inmemory.testmodel.UserSpecificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for nested null filter behavior.
 * Verifies that when a parent object is null and we filter on child attributes
 * through the null parent, isNull correctly returns true and isNotNull returns false.
 */
@DisplayName("Nested Null Filter Tests")
class NestedNullFilterTest {

    // ==================== User -> Profile (null) -> bio/followers ====================

    @Nested
    @DisplayName("User with null profile - nested field filters")
    class UserWithNullProfileTests {

        @Test
        @DisplayName("Filter profile.bio.isNull=true should match user with null profile")
        void profileBioIsNullTrueShouldMatchNullProfile() {
            User user = new User();
            user.setId("1");
            user.setName("Test");
            user.setProfile(null);

            ProfileFilter profileFilter = new ProfileFilter();
            StringFilter bioFilter = new StringFilter();
            bioFilter.setIsNull(true);
            profileFilter.setBio(bioFilter);

            UserFilter userFilter = new UserFilter();
            userFilter.setProfile(profileFilter);

            boolean result = UserSpecificationService.INSTANCE.validateFilter(user, userFilter);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Filter profile.bio.isNotNull=true should NOT match user with null profile")
        void profileBioIsNotNullTrueShouldNotMatchNullProfile() {
            User user = new User();
            user.setId("1");
            user.setName("Test");
            user.setProfile(null);

            ProfileFilter profileFilter = new ProfileFilter();
            StringFilter bioFilter = new StringFilter();
            bioFilter.setIsNotNull(true);
            profileFilter.setBio(bioFilter);

            UserFilter userFilter = new UserFilter();
            userFilter.setProfile(profileFilter);

            boolean result = UserSpecificationService.INSTANCE.validateFilter(user, userFilter);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Filter profile.bio.equals('value') should NOT match user with null profile")
        void profileBioEqualsShouldNotMatchNullProfile() {
            User user = new User();
            user.setId("1");
            user.setName("Test");
            user.setProfile(null);

            ProfileFilter profileFilter = new ProfileFilter();
            StringFilter bioFilter = new StringFilter();
            bioFilter.setEquals("some bio");
            profileFilter.setBio(bioFilter);

            UserFilter userFilter = new UserFilter();
            userFilter.setProfile(profileFilter);

            boolean result = UserSpecificationService.INSTANCE.validateFilter(user, userFilter);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Filter profile.followers.isNull=true should match user with null profile")
        void profileFollowersIsNullTrueShouldMatchNullProfile() {
            User user = new User();
            user.setId("1");
            user.setName("Test");
            user.setProfile(null);

            ProfileFilter profileFilter = new ProfileFilter();
            IntegerFilter followersFilter = new IntegerFilter();
            followersFilter.setIsNull(true);
            profileFilter.setFollowers(followersFilter);

            UserFilter userFilter = new UserFilter();
            userFilter.setProfile(profileFilter);

            boolean result = UserSpecificationService.INSTANCE.validateFilter(user, userFilter);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Filter profile.followers.isNotNull=true should NOT match user with null profile")
        void profileFollowersIsNotNullTrueShouldNotMatchNullProfile() {
            User user = new User();
            user.setId("1");
            user.setName("Test");
            user.setProfile(null);

            ProfileFilter profileFilter = new ProfileFilter();
            IntegerFilter followersFilter = new IntegerFilter();
            followersFilter.setIsNotNull(true);
            profileFilter.setFollowers(followersFilter);

            UserFilter userFilter = new UserFilter();
            userFilter.setProfile(profileFilter);

            boolean result = UserSpecificationService.INSTANCE.validateFilter(user, userFilter);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Filter profile.followers.greaterThan should NOT match user with null profile")
        void profileFollowersGreaterThanShouldNotMatchNullProfile() {
            User user = new User();
            user.setId("1");
            user.setName("Test");
            user.setProfile(null);

            ProfileFilter profileFilter = new ProfileFilter();
            IntegerFilter followersFilter = new IntegerFilter();
            followersFilter.setGreaterThan(100);
            profileFilter.setFollowers(followersFilter);

            UserFilter userFilter = new UserFilter();
            userFilter.setProfile(profileFilter);

            boolean result = UserSpecificationService.INSTANCE.validateFilter(user, userFilter);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Filter profile.bio.isNull=false should NOT match user with null profile (bio IS null)")
        void profileBioIsNullFalseShouldNotMatchNullProfile() {
            User user = new User();
            user.setId("1");
            user.setName("Test");
            user.setProfile(null);

            ProfileFilter profileFilter = new ProfileFilter();
            StringFilter bioFilter = new StringFilter();
            bioFilter.setIsNull(false); // Asking "is bio NOT null?" -> no, it is null
            profileFilter.setBio(bioFilter);

            UserFilter userFilter = new UserFilter();
            userFilter.setProfile(profileFilter);

            boolean result = UserSpecificationService.INSTANCE.validateFilter(user, userFilter);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Filter profile.bio.isNotNull=false should match user with null profile")
        void profileBioIsNotNullFalseShouldMatchNullProfile() {
            User user = new User();
            user.setId("1");
            user.setName("Test");
            user.setProfile(null);

            ProfileFilter profileFilter = new ProfileFilter();
            StringFilter bioFilter = new StringFilter();
            bioFilter.setIsNotNull(false); // Asking "is bio null?" (negated isNotNull) -> yes
            profileFilter.setBio(bioFilter);

            UserFilter userFilter = new UserFilter();
            userFilter.setProfile(profileFilter);

            boolean result = UserSpecificationService.INSTANCE.validateFilter(user, userFilter);
            assertThat(result).isTrue();
        }
    }

    // ==================== User with non-null profile - normal behavior preserved ====================

    @Nested
    @DisplayName("User with non-null profile - normal filter behavior preserved")
    class UserWithNonNullProfileTests {

        @Test
        @DisplayName("Filter profile.bio.isNull=true should match when profile exists but bio is null")
        void profileBioIsNullTrueWithNullBio() {
            User user = new User();
            user.setId("1");
            user.setName("Test");
            Profile profile = new Profile();
            profile.setBio(null);
            user.setProfile(profile);

            ProfileFilter profileFilter = new ProfileFilter();
            StringFilter bioFilter = new StringFilter();
            bioFilter.setIsNull(true);
            profileFilter.setBio(bioFilter);

            UserFilter userFilter = new UserFilter();
            userFilter.setProfile(profileFilter);

            boolean result = UserSpecificationService.INSTANCE.validateFilter(user, userFilter);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Filter profile.bio.isNull=true should NOT match when bio has value")
        void profileBioIsNullTrueWithNonNullBio() {
            User user = new User();
            user.setId("1");
            user.setName("Test");
            Profile profile = new Profile();
            profile.setBio("Hello");
            user.setProfile(profile);

            ProfileFilter profileFilter = new ProfileFilter();
            StringFilter bioFilter = new StringFilter();
            bioFilter.setIsNull(true);
            profileFilter.setBio(bioFilter);

            UserFilter userFilter = new UserFilter();
            userFilter.setProfile(profileFilter);

            boolean result = UserSpecificationService.INSTANCE.validateFilter(user, userFilter);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Filter profile.bio.isNotNull=true should match when bio has value")
        void profileBioIsNotNullTrueWithNonNullBio() {
            User user = new User();
            user.setId("1");
            user.setName("Test");
            Profile profile = new Profile();
            profile.setBio("Hello");
            user.setProfile(profile);

            ProfileFilter profileFilter = new ProfileFilter();
            StringFilter bioFilter = new StringFilter();
            bioFilter.setIsNotNull(true);
            profileFilter.setBio(bioFilter);

            UserFilter userFilter = new UserFilter();
            userFilter.setProfile(profileFilter);

            boolean result = UserSpecificationService.INSTANCE.validateFilter(user, userFilter);
            assertThat(result).isTrue();
        }
    }

    // ==================== Mixed scenarios ====================

    @Nested
    @DisplayName("Mixed scenarios - filtering lists with some null profiles")
    class MixedScenarios {

        @Test
        @DisplayName("Filter correctly separates users with null and non-null profiles")
        void mixedNullAndNonNullProfiles() {
            User userWithNullProfile = new User();
            userWithNullProfile.setId("1");
            userWithNullProfile.setProfile(null);

            User userWithProfile = new User();
            userWithProfile.setId("2");
            Profile profile = new Profile();
            profile.setBio("Hello");
            profile.setFollowers(100);
            userWithProfile.setProfile(profile);

            User userWithNullBio = new User();
            userWithNullBio.setId("3");
            Profile profileNullBio = new Profile();
            profileNullBio.setBio(null);
            profileNullBio.setFollowers(50);
            userWithNullBio.setProfile(profileNullBio);

            // Filter: profile.bio.isNull = true
            ProfileFilter profileFilter = new ProfileFilter();
            StringFilter bioFilter = new StringFilter();
            bioFilter.setIsNull(true);
            profileFilter.setBio(bioFilter);

            UserFilter userFilter = new UserFilter();
            userFilter.setProfile(profileFilter);

            List<User> users = List.of(userWithNullProfile, userWithProfile, userWithNullBio);
            List<User> matched = users.stream()
                    .filter(u -> UserSpecificationService.INSTANCE.validateFilter(u, userFilter))
                    .toList();

            // userWithNullProfile: profile is null -> bio is null -> matches isNull=true
            // userWithProfile: profile.bio = "Hello" -> does not match isNull=true
            // userWithNullBio: profile.bio = null -> matches isNull=true
            assertThat(matched).hasSize(2);
            assertThat(matched).extracting(User::getId).containsExactlyInAnyOrder("1", "3");
        }

        @Test
        @DisplayName("Filter profile.bio.isNotNull=true correctly excludes users with null profiles")
        void mixedNullAndNonNullProfilesIsNotNull() {
            User userWithNullProfile = new User();
            userWithNullProfile.setId("1");
            userWithNullProfile.setProfile(null);

            User userWithProfile = new User();
            userWithProfile.setId("2");
            Profile profile = new Profile();
            profile.setBio("Hello");
            userWithProfile.setProfile(profile);

            // Filter: profile.bio.isNotNull = true
            ProfileFilter profileFilter = new ProfileFilter();
            StringFilter bioFilter = new StringFilter();
            bioFilter.setIsNotNull(true);
            profileFilter.setBio(bioFilter);

            UserFilter userFilter = new UserFilter();
            userFilter.setProfile(profileFilter);

            List<User> users = List.of(userWithNullProfile, userWithProfile);
            List<User> matched = users.stream()
                    .filter(u -> UserSpecificationService.INSTANCE.validateFilter(u, userFilter))
                    .toList();

            // Only user with non-null bio should match
            assertThat(matched).hasSize(1);
            assertThat(matched.get(0).getId()).isEqualTo("2");
        }

        @Test
        @DisplayName("Combined filter: name filter + null profile filter")
        void combinedNameAndNullProfileFilter() {
            User user = new User();
            user.setId("1");
            user.setName("John");
            user.setProfile(null);

            // Filter: name.equals("John") AND profile.bio.isNull=true
            UserFilter userFilter = new UserFilter();
            StringFilter nameFilter = new StringFilter();
            nameFilter.setEquals("John");
            userFilter.setName(nameFilter);

            ProfileFilter profileFilter = new ProfileFilter();
            StringFilter bioFilter = new StringFilter();
            bioFilter.setIsNull(true);
            profileFilter.setBio(bioFilter);
            userFilter.setProfile(profileFilter);

            boolean result = UserSpecificationService.INSTANCE.validateFilter(user, userFilter);
            assertThat(result).isTrue();
        }
    }

    // ==================== Direct null entity tests ====================

    @Nested
    @DisplayName("Direct null entity validateFilter tests")
    class DirectNullEntityTests {

        @Test
        @DisplayName("validateFilter with null entity and isNull=true filter returns true")
        void nullEntityWithIsNullFilter() {
            ProfileFilter profileFilter = new ProfileFilter();
            StringFilter bioFilter = new StringFilter();
            bioFilter.setIsNull(true);
            profileFilter.setBio(bioFilter);

            boolean result = ProfileSpecificationService.INSTANCE.validateFilter(null, profileFilter);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("validateFilter with null entity and isNotNull=true filter returns false")
        void nullEntityWithIsNotNullFilter() {
            ProfileFilter profileFilter = new ProfileFilter();
            StringFilter bioFilter = new StringFilter();
            bioFilter.setIsNotNull(true);
            profileFilter.setBio(bioFilter);

            boolean result = ProfileSpecificationService.INSTANCE.validateFilter(null, profileFilter);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("validateFilter with null entity and equals filter returns false")
        void nullEntityWithEqualsFilter() {
            ProfileFilter profileFilter = new ProfileFilter();
            StringFilter bioFilter = new StringFilter();
            bioFilter.setEquals("test");
            profileFilter.setBio(bioFilter);

            boolean result = ProfileSpecificationService.INSTANCE.validateFilter(null, profileFilter);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("validateFilter with null entity and multiple isNull=true filters returns true")
        void nullEntityWithMultipleIsNullFilters() {
            ProfileFilter profileFilter = new ProfileFilter();
            StringFilter bioFilter = new StringFilter();
            bioFilter.setIsNull(true);
            profileFilter.setBio(bioFilter);

            IntegerFilter followersFilter = new IntegerFilter();
            followersFilter.setIsNull(true);
            profileFilter.setFollowers(followersFilter);

            boolean result = ProfileSpecificationService.INSTANCE.validateFilter(null, profileFilter);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("validateFilter with null entity - one isNull=true and one equals fails")
        void nullEntityWithMixedFilters() {
            ProfileFilter profileFilter = new ProfileFilter();
            StringFilter bioFilter = new StringFilter();
            bioFilter.setIsNull(true);
            profileFilter.setBio(bioFilter);

            IntegerFilter followersFilter = new IntegerFilter();
            followersFilter.setEquals(100);
            profileFilter.setFollowers(followersFilter);

            boolean result = ProfileSpecificationService.INSTANCE.validateFilter(null, profileFilter);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("validateFilter with null entity and empty filter returns true")
        void nullEntityWithEmptyFilter() {
            ProfileFilter profileFilter = new ProfileFilter();
            // No filter criteria set

            boolean result = ProfileSpecificationService.INSTANCE.validateFilter(null, profileFilter);
            assertThat(result).isTrue();
        }
    }

    // ==================== Collection filter with null entity ====================

    @Nested
    @DisplayName("Collection filter scenarios with null entity/parent")
    class CollectionNullEntityTests {

        @Test
        @DisplayName("Customer with null orders - orders.isNull=true should match")
        void nullOrdersIsNullTrue() {
            Customer customer = new Customer();
            customer.setId(1L);
            customer.setOrders(null);

            CustomerFilter filter = new CustomerFilter();
            CollectionFilter<Order> ordersFilter = new CollectionFilter<>();
            ordersFilter.setIsNull(true);
            filter.setOrders(ordersFilter);

            boolean result = CustomerSpecificationService.INSTANCE.validateFilter(customer, filter);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Customer with null orders - orders.isNotNull=true should NOT match")
        void nullOrdersIsNotNullTrue() {
            Customer customer = new Customer();
            customer.setId(1L);
            customer.setOrders(null);

            CustomerFilter filter = new CustomerFilter();
            CollectionFilter<Order> ordersFilter = new CollectionFilter<>();
            ordersFilter.setIsNotNull(true);
            filter.setOrders(ordersFilter);

            boolean result = CustomerSpecificationService.INSTANCE.validateFilter(customer, filter);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Customer with empty orders - orders.isNull=true should NOT match")
        void emptyOrdersIsNullTrue() {
            Customer customer = new Customer();
            customer.setId(1L);
            customer.setOrders(new ArrayList<>());

            CustomerFilter filter = new CustomerFilter();
            CollectionFilter<Order> ordersFilter = new CollectionFilter<>();
            ordersFilter.setIsNull(true);
            filter.setOrders(ordersFilter);

            boolean result = CustomerSpecificationService.INSTANCE.validateFilter(customer, filter);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Customer with orders - orders.isNull=true should NOT match")
        void nonNullOrdersIsNullTrue() {
            Customer customer = new Customer();
            customer.setId(1L);
            Order order = new Order();
            order.setId(1L);
            customer.setOrders(List.of(order));

            CustomerFilter filter = new CustomerFilter();
            CollectionFilter<Order> ordersFilter = new CollectionFilter<>();
            ordersFilter.setIsNull(true);
            filter.setOrders(ordersFilter);

            boolean result = CustomerSpecificationService.INSTANCE.validateFilter(customer, filter);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Null customer entity - orders.isNull=true should match (all fields null)")
        void nullCustomerEntityOrdersIsNull() {
            CustomerFilter filter = new CustomerFilter();
            CollectionFilter<Order> ordersFilter = new CollectionFilter<>();
            ordersFilter.setIsNull(true);
            filter.setOrders(ordersFilter);

            boolean result = CustomerSpecificationService.INSTANCE.validateFilter(null, filter);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Null customer entity - orders.isNotNull=true should NOT match")
        void nullCustomerEntityOrdersIsNotNull() {
            CustomerFilter filter = new CustomerFilter();
            CollectionFilter<Order> ordersFilter = new CollectionFilter<>();
            ordersFilter.setIsNotNull(true);
            filter.setOrders(ordersFilter);

            boolean result = CustomerSpecificationService.INSTANCE.validateFilter(null, filter);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Collection element with null field - element filter isNull=true matches")
        void collectionElementWithNullFieldIsNull() {
            Customer customer = new Customer();
            customer.setId(1L);
            Order order = new Order();
            order.setId(1L);
            order.setStatus(null); // status is null
            customer.setOrders(List.of(order));

            // Filter: any order where status.isNull = true
            CustomerFilter filter = new CustomerFilter();
            CollectionFilter<Order> ordersFilter = new CollectionFilter<>();
            OrderFilter orderFilter = new OrderFilter();
            StringFilter statusFilter = new StringFilter();
            statusFilter.setIsNull(true);
            orderFilter.setStatus(statusFilter);
            ordersFilter.setCollectionAny(orderFilter);
            filter.setOrders(ordersFilter);

            boolean result = CustomerSpecificationService.INSTANCE.validateFilter(customer, filter);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Collection element with non-null field - element filter isNull=true does NOT match")
        void collectionElementWithNonNullFieldIsNull() {
            Customer customer = new Customer();
            customer.setId(1L);
            Order order = new Order();
            order.setId(1L);
            order.setStatus("ACTIVE");
            customer.setOrders(List.of(order));

            // Filter: any order where status.isNull = true
            CustomerFilter filter = new CustomerFilter();
            CollectionFilter<Order> ordersFilter = new CollectionFilter<>();
            OrderFilter orderFilter = new OrderFilter();
            StringFilter statusFilter = new StringFilter();
            statusFilter.setIsNull(true);
            orderFilter.setStatus(statusFilter);
            ordersFilter.setCollectionAny(orderFilter);
            filter.setOrders(ordersFilter);

            boolean result = CustomerSpecificationService.INSTANCE.validateFilter(customer, filter);
            assertThat(result).isFalse();
        }
    }

    // ==================== Deep nesting through null objects ====================

    @Nested
    @DisplayName("Deep nesting - nested null object chain")
    class DeepNestingTests {

        @Test
        @DisplayName("User with null profile - recursive null propagation works correctly")
        void deepNullPropagationIsNull() {
            User user = new User();
            user.setId("1");
            user.setProfile(null); // Profile is null, so profile.bio and profile.followers are null

            // Filter: profile.bio.isNull=true AND profile.followers.isNull=true
            ProfileFilter profileFilter = new ProfileFilter();
            StringFilter bioFilter = new StringFilter();
            bioFilter.setIsNull(true);
            profileFilter.setBio(bioFilter);
            IntegerFilter followersFilter = new IntegerFilter();
            followersFilter.setIsNull(true);
            profileFilter.setFollowers(followersFilter);

            UserFilter userFilter = new UserFilter();
            userFilter.setProfile(profileFilter);

            boolean result = UserSpecificationService.INSTANCE.validateFilter(user, userFilter);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("User with null profile - any value filter on nested field fails")
        void deepNullPropagationValueFilter() {
            User user = new User();
            user.setId("1");
            user.setProfile(null);

            // Filter: profile.bio.contains("hello") - should fail since profile is null
            ProfileFilter profileFilter = new ProfileFilter();
            StringFilter bioFilter = new StringFilter();
            bioFilter.setContains("hello");
            profileFilter.setBio(bioFilter);

            UserFilter userFilter = new UserFilter();
            userFilter.setProfile(profileFilter);

            boolean result = UserSpecificationService.INSTANCE.validateFilter(user, userFilter);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("User with null profile - combined isNull on multiple nested fields")
        void deepNullPropagationMultipleFields() {
            User user = new User();
            user.setId("1");
            user.setProfile(null);

            // Filter: profile.bio.isNull=true AND profile.followers.isNotNull=true
            // bio isNull = true (passes), followers isNotNull = true (fails since followers is null)
            ProfileFilter profileFilter = new ProfileFilter();
            StringFilter bioFilter = new StringFilter();
            bioFilter.setIsNull(true);
            profileFilter.setBio(bioFilter);
            IntegerFilter followersFilter = new IntegerFilter();
            followersFilter.setIsNotNull(true);
            profileFilter.setFollowers(followersFilter);

            UserFilter userFilter = new UserFilter();
            userFilter.setProfile(profileFilter);

            boolean result = UserSpecificationService.INSTANCE.validateFilter(user, userFilter);
            assertThat(result).isFalse();
        }
    }
}
