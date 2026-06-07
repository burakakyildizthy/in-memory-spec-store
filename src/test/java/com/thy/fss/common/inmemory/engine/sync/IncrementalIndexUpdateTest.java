package com.thy.fss.common.inmemory.engine.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import com.thy.fss.common.inmemory.engine.index.IndexDefinition;
import com.thy.fss.common.inmemory.specification.attribute.StringAttribute;
import com.thy.fss.common.inmemory.testmodel.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DependencyGraph#updateIndexesForEntities(String, List)}.
 *
 * <p>Covers: empty/null mutated list, single entity mutation, no indexes for datasource,
 * and error fallback signal behaviour.</p>
 *
 * <p><b>Requirements: 2.1, 2.2, 2.5</b></p>
 */
class IncrementalIndexUpdateTest {

    private static final String DS_NAME = "test-ds";
    private static final String ALICE = "Alice";
    private static final String BOB = "Bob";
    private static final String ZARA = "Zara";

    private DependencyGraph graph;
    private IndexDefinition<User> nameIndexDef;

    @BeforeEach
    void setUp() {
        graph = new DependencyGraph();
        StringAttribute<User> nameAttr = new StringAttribute<>("name", User.class);
        nameIndexDef = IndexDefinition.builder(User.class)
                .addKeyFieldWithPath(
                        Collections.singletonList(nameAttr),
                        User::getName)
                .build();
    }

    // ==================== Helpers ====================

    private User createUser(String id, String name) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        return u;
    }

    private List<User> createAndPopulateUsers(int count) {
        String[] names = {ALICE, BOB, "Charlie", "Diana", "Eve",
                "Frank", "Grace", "Hank", "Ivy", "Jack"};
        List<User> users = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            users.add(createUser(String.valueOf(i + 1), names[i % names.length]));
        }
        graph.upsertAll(DS_NAME, users);
        graph.registerIndex(DS_NAME, nameIndexDef);
        return users;
    }

    // ==================== Tests ====================

    /**
     * Empty mutated entity list → returns true, no changes to index.
     * Validates: Requirement 2.1, 2.2
     */
    @Test
    @DisplayName("Empty mutated entity list returns true and leaves index unchanged")
    void emptyMutatedListReturnsTrueAndLeavesIndexUnchanged() {
        createAndPopulateUsers(5);

        // Capture index state before
        List<User> beforeLookup = graph.lookup(DS_NAME, nameIndexDef, ALICE);

        boolean result = graph.updateIndexesForEntities(DS_NAME, Collections.emptyList());

        assertThat(result).isTrue();

        // Index state should be identical
        List<User> afterLookup = graph.lookup(DS_NAME, nameIndexDef, ALICE);
        assertThat(afterLookup).hasSameSizeAs(beforeLookup);
    }

    /**
     * Null mutated entity list → returns true, no changes to index.
     * Validates: Requirement 2.1, 2.2
     */
    @Test
    @DisplayName("Null mutated entity list returns true and leaves index unchanged")
    void nullMutatedListReturnsTrueAndLeavesIndexUnchanged() {
        createAndPopulateUsers(5);

        List<User> beforeLookup = graph.lookup(DS_NAME, nameIndexDef, BOB);

        boolean result = graph.updateIndexesForEntities(DS_NAME, null);

        assertThat(result).isTrue();

        List<User> afterLookup = graph.lookup(DS_NAME, nameIndexDef, BOB);
        assertThat(afterLookup).hasSameSizeAs(beforeLookup);
    }

    /**
     * Single entity mutation → only that entity's index entry is updated, others unchanged.
     * Validates: Requirement 2.1, 2.2
     */
    @Test
    @DisplayName("Single entity mutation updates only that entity's index entry")
    void singleEntityMutationUpdatesOnlyThatEntry() {
        createAndPopulateUsers(5);
        // users: 1=Alice, 2=Bob, 3=Charlie, 4=Diana, 5=Eve

        // Mutate user "1" from ALICE to ZARA
        User mutated = createUser("1", ZARA);
        graph.upsert(DS_NAME, mutated);

        boolean result = graph.updateIndexesForEntities(DS_NAME, List.of(mutated));

        assertThat(result).isTrue();

        // ALICE should no longer be found via index (user 1 was the only Alice)
        List<User> aliceLookup = graph.lookup(DS_NAME, nameIndexDef, ALICE);
        assertThat(aliceLookup).isEmpty();

        // ZARA should now be found
        List<User> zaraLookup = graph.lookup(DS_NAME, nameIndexDef, ZARA);
        assertThat(zaraLookup).hasSize(1);
        assertThat(zaraLookup.get(0).getIdentity()).isEqualTo("1");

        // Other entries remain unchanged
        List<User> bobLookup = graph.lookup(DS_NAME, nameIndexDef, BOB);
        assertThat(bobLookup).hasSize(1);
        assertThat(bobLookup.get(0).getIdentity()).isEqualTo("2");

        List<User> charlieLookup = graph.lookup(DS_NAME, nameIndexDef, "Charlie");
        assertThat(charlieLookup).hasSize(1);
        assertThat(charlieLookup.get(0).getIdentity()).isEqualTo("3");
    }

    /**
     * No indexes registered for datasource → returns true (nothing to update).
     * Validates: Requirement 2.1
     */
    @Test
    @DisplayName("No indexes for datasource returns true")
    void noIndexesForDatasourceReturnsTrue() {
        // Populate entities but do NOT register any index
        DependencyGraph noIndexGraph = new DependencyGraph();
        User user = createUser("1", ALICE);
        noIndexGraph.upsertAll(DS_NAME, List.of(user));

        User mutated = createUser("1", ZARA);
        noIndexGraph.upsert(DS_NAME, mutated);

        boolean result = noIndexGraph.updateIndexesForEntities(DS_NAME, List.of(mutated));

        assertThat(result).isTrue();
    }

    /**
     * Mutation rate exceeding 50% threshold → returns false (full rebuild signal).
     * Validates: Requirement 2.5
     */
    @Test
    @DisplayName("Mutation rate above 50% returns false signalling full rebuild")
    void mutationRateAboveThresholdReturnsFalse() {
        List<User> users = createAndPopulateUsers(4);
        // 4 entities total, mutating 3 → 75% > 50%

        List<User> mutated = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            User m = createUser(users.get(i).getId(), users.get(i).getName() + "_changed");
            graph.upsert(DS_NAME, m);
            mutated.add(m);
        }

        boolean result = graph.updateIndexesForEntities(DS_NAME, mutated);

        assertThat(result)
                .as("Mutation rate 3/4 (75%) exceeds 50% threshold, should return false")
                .isFalse();
    }

    /**
     * Unknown datasource (no entity store entry) with mutations → returns true
     * because there are no indexes to update.
     * Validates: Requirement 2.1
     */
    @Test
    @DisplayName("Unknown datasource with mutations returns true")
    void unknownDatasourceReturnsTrue() {
        User mutated = createUser("1", ALICE);

        boolean result = graph.updateIndexesForEntities("nonexistent-ds", List.of(mutated));

        assertThat(result).isTrue();
    }
}
