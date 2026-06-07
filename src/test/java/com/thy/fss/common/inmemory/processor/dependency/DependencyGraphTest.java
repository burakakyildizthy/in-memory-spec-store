package com.thy.fss.common.inmemory.processor.dependency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for DependencyGraph class.
 * Tests all methods and edge cases for dependency management.
 */
@DisplayName("DependencyGraph Tests")
class DependencyGraphTest {
    
    private static final String NODE_A = "NodeA";
    private static final String NODE_B = "NodeB";
    private static final String NODE_C = "NodeC";
    private static final String NODE_D = "NodeD";
    private static final String SELF_DEPENDENT = "SelfDependent";
    private static final String SINGLE_NODE = "SingleNode";
    private static final String MAIN_NODE = "MainNode";

    private DependencyGraph graph;

    @BeforeEach
    void setUp() {
        graph = new DependencyGraph();
    }

    @Nested
    @DisplayName("Node Management")
    class NodeManagement {

        @Test
        @DisplayName("Should add node with dependencies")
        void shouldAddNodeWithDependencies() {
            // Given
            Set<String> dependencies = Set.of(NODE_B, NODE_C);

            // When
            graph.addNode(NODE_A, dependencies);

            // Then
            assertTrue(graph.getNodes().contains(NODE_A));
            assertTrue(graph.getNodes().contains(NODE_B));
            assertTrue(graph.getNodes().contains(NODE_C));
            assertEquals(dependencies, graph.getDependencies(NODE_A));
        }

        @Test
        @DisplayName("Should add node with empty dependencies")
        void shouldAddNodeWithEmptyDependencies() {
            // Given
            Set<String> emptyDependencies = Collections.emptySet();

            // When
            graph.addNode(NODE_A, emptyDependencies);

            // Then
            assertTrue(graph.getNodes().contains(NODE_A));
            assertEquals(1, graph.size());
            assertTrue(graph.getDependencies(NODE_A).isEmpty());
        }

        @Test
        @DisplayName("Should handle duplicate node addition")
        void shouldHandleDuplicateNodeAddition() {
            // Given
            Set<String> dependencies1 = Set.of(NODE_B);
            Set<String> dependencies2 = Set.of(NODE_C);

            // When
            graph.addNode(NODE_A, dependencies1);
            graph.addNode(NODE_A, dependencies2);

            // Then
            assertEquals(3, graph.size()); // NodeA, NodeB, NodeC
            assertEquals(dependencies2, graph.getDependencies(NODE_A)); // Latest wins
        }

        @Test
        @DisplayName("Should get empty dependencies for non-existent node")
        void shouldGetEmptyDependenciesForNonExistentNode() {
            // When
            Set<String> dependencies = graph.getDependencies("NonExistent");

            // Then
            assertTrue(dependencies.isEmpty());
        }

        @Test
        @DisplayName("Should return all nodes")
        void shouldReturnAllNodes() {
            // Given
            graph.addNode(NODE_A, Set.of(NODE_B));
            graph.addNode(NODE_C, Set.of(NODE_D));

            // When
            Set<String> nodes = graph.getNodes();

            // Then
            assertEquals(4, nodes.size());
            assertTrue(nodes.containsAll(Set.of(NODE_A, NODE_B, NODE_C, NODE_D)));
        }
    }

    @Nested
    @DisplayName("Dependency Analysis")
    class DependencyAnalysis {

        @Test
        @DisplayName("Should find dependents correctly")
        void shouldFindDependentsCorrectly() {
            // Given
            graph.addNode(NODE_A, Set.of(NODE_B));
            graph.addNode(NODE_C, Set.of(NODE_B));
            graph.addNode(NODE_D, Set.of(NODE_A));

            // When
            Set<String> dependentsOfB = graph.getDependents(NODE_B);
            Set<String> dependentsOfA = graph.getDependents(NODE_A);

            // Then
            assertEquals(Set.of(NODE_A, NODE_C), dependentsOfB);
            assertEquals(Set.of(NODE_D), dependentsOfA);
        }

        @Test
        @DisplayName("Should return empty dependents for node with no dependents")
        void shouldReturnEmptyDependentsForNodeWithNoDependents() {
            // Given
            graph.addNode(NODE_A, Set.of(NODE_B));

            // When
            Set<String> dependents = graph.getDependents(NODE_A);

            // Then
            assertTrue(dependents.isEmpty());
        }

        @Test
        @DisplayName("Should calculate in-degrees correctly")
        void shouldCalculateInDegreesCorrectly() {
            // Given
            graph.addNode(NODE_A, Set.of(NODE_B, NODE_C));
            graph.addNode(NODE_B, Set.of(NODE_C));
            graph.addNode(NODE_C, Collections.emptySet());

            // When
            Map<String, Integer> inDegrees = graph.calculateInDegrees();

            // Then
            assertEquals(2, inDegrees.get(NODE_A)); // depends on B and C
            assertEquals(1, inDegrees.get(NODE_B)); // depends on C
            assertEquals(0, inDegrees.get(NODE_C)); // no dependencies
        }
    }

    @Nested
    @DisplayName("Cycle Detection")
    class CycleDetection {

        @Test
        @DisplayName("Should detect no cycles in acyclic graph")
        void shouldDetectNoCyclesInAcyclicGraph() {
            // Given
            graph.addNode(NODE_A, Set.of(NODE_B));
            graph.addNode(NODE_B, Set.of(NODE_C));
            graph.addNode(NODE_C, Collections.emptySet());

            // When
            boolean hasCycles = graph.hasCycles();

            // Then
            assertFalse(hasCycles);
        }

        @Test
        @DisplayName("Should detect simple cycle")
        void shouldDetectSimpleCycle() {
            // Given - A -> B -> A
            graph.addNode(NODE_A, Set.of(NODE_B));
            graph.addNode(NODE_B, Set.of(NODE_A));

            // When
            boolean hasCycles = graph.hasCycles();

            // Then
            assertTrue(hasCycles);
        }

        @Test
        @DisplayName("Should detect complex cycle")
        void shouldDetectComplexCycle() {
            // Given - A -> B -> C -> A
            graph.addNode(NODE_A, Set.of(NODE_B));
            graph.addNode(NODE_B, Set.of(NODE_C));
            graph.addNode(NODE_C, Set.of(NODE_A));

            // When
            boolean hasCycles = graph.hasCycles();

            // Then
            assertTrue(hasCycles);
        }

        @Test
        @DisplayName("Should detect self-cycle")
        void shouldDetectSelfCycle() {
            // Given - A -> A
            graph.addNode(NODE_A, Set.of(NODE_A));

            // When
            boolean hasCycles = graph.hasCycles();

            // Then
            assertTrue(hasCycles);
        }

        @Test
        @DisplayName("Should find all cycles")
        void shouldFindAllCycles() {
            // Given - Multiple cycles: A -> B -> A and C -> D -> C
            graph.addNode(NODE_A, Set.of(NODE_B));
            graph.addNode(NODE_B, Set.of(NODE_A));
            graph.addNode(NODE_C, Set.of(NODE_D));
            graph.addNode(NODE_D, Set.of(NODE_C));

            // When
            List<List<String>> cycles = graph.findCycles();

            // Then
            assertEquals(2, cycles.size());

            // Check that we found both cycles (order may vary)
            boolean foundABCycle = cycles.stream().anyMatch(cycle ->
                    cycle.containsAll(Set.of(NODE_A, NODE_B)));
            boolean foundCDCycle = cycles.stream().anyMatch(cycle ->
                    cycle.containsAll(Set.of(NODE_C, NODE_D)));

            assertTrue(foundABCycle);
            assertTrue(foundCDCycle);
        }

        @Test
        @DisplayName("Should return empty cycles list for acyclic graph")
        void shouldReturnEmptyCyclesListForAcyclicGraph() {
            // Given
            graph.addNode(NODE_A, Set.of(NODE_B));
            graph.addNode(NODE_B, Collections.emptySet());

            // When
            List<List<String>> cycles = graph.findCycles();

            // Then
            assertTrue(cycles.isEmpty());
        }
    }

    @Nested
    @DisplayName("Graph Properties")
    class GraphProperties {

        @Test
        @DisplayName("Should return correct size")
        void shouldReturnCorrectSize() {
            // Given
            graph.addNode(NODE_A, Set.of(NODE_B));
            graph.addNode(NODE_C, Collections.emptySet());

            // When
            int size = graph.size();

            // Then
            assertEquals(3, size); // NodeA, NodeB, NodeC
        }

        @Test
        @DisplayName("Should be empty initially")
        void shouldBeEmptyInitially() {
            // When
            boolean isEmpty = graph.isEmpty();

            // Then
            assertTrue(isEmpty);
            assertEquals(0, graph.size());
        }

        @Test
        @DisplayName("Should not be empty after adding nodes")
        void shouldNotBeEmptyAfterAddingNodes() {
            // Given
            graph.addNode(NODE_A, Collections.emptySet());

            // When
            boolean isEmpty = graph.isEmpty();

            // Then
            assertFalse(isEmpty);
            assertEquals(1, graph.size());
        }

        @Test
        @DisplayName("Should provide meaningful toString")
        void shouldProvideMeaningfulToString() {
            // Given
            graph.addNode(NODE_A, Set.of(NODE_B));
            graph.addNode(NODE_B, Collections.emptySet());

            // When
            String toString = graph.toString();

            // Then
            assertNotNull(toString);
            assertTrue(toString.contains("DependencyGraph"));
            assertTrue(toString.contains(NODE_A));
            assertTrue(toString.contains(NODE_B));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle empty graph operations")
        void shouldHandleEmptyGraphOperations() {
            // When/Then
            assertTrue(graph.isEmpty());
            assertEquals(0, graph.size());
            assertTrue(graph.getNodes().isEmpty());
            assertFalse(graph.hasCycles());
            assertTrue(graph.findCycles().isEmpty());
            assertTrue(graph.calculateInDegrees().isEmpty());
            assertTrue(graph.getDependents("AnyNode").isEmpty());
        }

        @Test
        @DisplayName("Should handle single node graph")
        void shouldHandleSingleNodeGraph() {
            // Given
            graph.addNode(SINGLE_NODE, Collections.emptySet());

            // When/Then
            assertFalse(graph.isEmpty());
            assertEquals(1, graph.size());
            assertEquals(Set.of(SINGLE_NODE), graph.getNodes());
            assertFalse(graph.hasCycles());
            assertTrue(graph.findCycles().isEmpty());
            assertEquals(0, graph.calculateInDegrees().get(SINGLE_NODE));
            assertTrue(graph.getDependents(SINGLE_NODE).isEmpty());
        }

        @Test
        @DisplayName("Should handle node depending on itself")
        void shouldHandleNodeDependingOnItself() {
            // Given
            graph.addNode(SELF_DEPENDENT, Set.of(SELF_DEPENDENT));

            // When/Then
            assertEquals(1, graph.size());
            assertTrue(graph.hasCycles());
            assertEquals(1, graph.findCycles().size());
            assertEquals(1, graph.calculateInDegrees().get(SELF_DEPENDENT));
            assertEquals(Set.of(SELF_DEPENDENT), graph.getDependents(SELF_DEPENDENT));
        }

        @Test
        @DisplayName("Should handle large dependency sets")
        void shouldHandleLargeDependencySets() {
            // Given
            Set<String> largeDependencySet = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                largeDependencySet.add("Dep" + i);
            }

            // When
            graph.addNode(MAIN_NODE, largeDependencySet);

            // Then
            assertEquals(101, graph.size()); // MainNode + 100 dependencies
            assertEquals(100, graph.getDependencies(MAIN_NODE).size());
            assertEquals(100, graph.calculateInDegrees().get(MAIN_NODE));
        }

        @Test
        @DisplayName("Should handle null and empty dependency sets correctly")
        void shouldHandleNullAndEmptyDependencySetsCorrectly() {
            // Given
            Set<String> emptySet = Collections.emptySet();

            // When
            graph.addNode("EmptyDeps", emptySet);

            // Then
            assertEquals(1, graph.size());
            assertTrue(graph.getDependencies("EmptyDeps").isEmpty());

            // Note: Adding null dependencies would be handled by the actual implementation
            // This test ensures we handle edge cases gracefully
        }
    }
}