package com.thy.fss.common.inmemory.processor.dependency;

import com.thy.fss.common.inmemory.processor.exception.ProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for TopologicalSorter class.
 * Tests Kahn's algorithm implementation and validation methods.
 */
@DisplayName("TopologicalSorter Tests")
class TopologicalSorterTest {

    private static final String NODE_A = "NodeA";
    private static final String NODE_B = "NodeB";
    private static final String NODE_C = "NodeC";
    private static final String NODE_D = "NodeD";
    private static final String NODE_E = "NodeE";
    private static final String NODE_F = "NodeF";
    private static final String MAIN_NODE = "MainNode";
    private static final String INDEPENDENT_1 = "Independent1";
    private static final String INDEPENDENT_2 = "Independent2";
    private static final String INDEPENDENT_3 = "Independent3";
    private static final String DEPENDENT = "Dependent";
    private static final String CIRCULAR_DEPENDENCY_DETECTED = "Circular dependency detected";

    private TopologicalSorter sorter;
    private DependencyGraph graph;

    @BeforeEach
    void setUp() {
        sorter = new TopologicalSorter();
        graph = new DependencyGraph();
    }

    @Nested
    @DisplayName("Basic Sorting")
    class BasicSorting {

        @Test
        @DisplayName("Should sort empty graph")
        void shouldSortEmptyGraph() throws ProcessingException {
            // When
            List<String> result = sorter.sort(graph);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should sort single node graph")
        void shouldSortSingleNodeGraph() throws ProcessingException {
            // Given
            graph.addNode(NODE_A, Collections.emptySet());

            // When
            List<String> result = sorter.sort(graph);

            // Then
            assertEquals(1, result.size());
            assertEquals(NODE_A, result.get(0));
        }

        @Test
        @DisplayName("Should sort linear dependency chain")
        void shouldSortLinearDependencyChain() throws ProcessingException {
            // Given - A depends on B, B depends on C
            graph.addNode(NODE_A, Set.of(NODE_B));
            graph.addNode(NODE_B, Set.of(NODE_C));
            graph.addNode(NODE_C, Collections.emptySet());

            // When
            List<String> result = sorter.sort(graph);

            // Then
            assertEquals(3, result.size());
            assertEquals(NODE_C, result.get(0)); // No dependencies
            assertEquals(NODE_B, result.get(1)); // Depends on C
            assertEquals(NODE_A, result.get(2)); // Depends on B
        }

        @Test
        @DisplayName("Should sort diamond dependency pattern")
        void shouldSortDiamondDependencyPattern() throws ProcessingException {
            // Given - A depends on B and C, B and C depend on D
            graph.addNode(NODE_A, Set.of(NODE_B, NODE_C));
            graph.addNode(NODE_B, Set.of(NODE_D));
            graph.addNode(NODE_C, Set.of(NODE_D));
            graph.addNode(NODE_D, Collections.emptySet());

            // When
            List<String> result = sorter.sort(graph);

            // Then
            assertEquals(4, result.size());
            assertEquals(NODE_D, result.get(0)); // No dependencies
            assertTrue(result.indexOf(NODE_B) < result.indexOf(NODE_A)); // B before A
            assertTrue(result.indexOf(NODE_C) < result.indexOf(NODE_A)); // C before A
            assertEquals(NODE_A, result.get(3)); // Last (depends on B and C)
        }

        @Test
        @DisplayName("Should sort complex dependency graph")
        void shouldSortComplexDependencyGraph() throws ProcessingException {
            // Given - Complex graph: A->B,C; B->D; C->D,E; E->F
            graph.addNode(NODE_A, Set.of(NODE_B, NODE_C));
            graph.addNode(NODE_B, Set.of(NODE_D));
            graph.addNode(NODE_C, Set.of(NODE_D, NODE_E));
            graph.addNode(NODE_D, Collections.emptySet());
            graph.addNode(NODE_E, Set.of(NODE_F));
            graph.addNode(NODE_F, Collections.emptySet());

            // When
            List<String> result = sorter.sort(graph);

            // Then
            assertEquals(6, result.size());

            // Verify topological order constraints
            assertTrue(result.indexOf(NODE_D) < result.indexOf(NODE_B));
            assertTrue(result.indexOf(NODE_D) < result.indexOf(NODE_C));
            assertTrue(result.indexOf(NODE_F) < result.indexOf(NODE_E));
            assertTrue(result.indexOf(NODE_E) < result.indexOf(NODE_C));
            assertTrue(result.indexOf(NODE_B) < result.indexOf(NODE_A));
            assertTrue(result.indexOf(NODE_C) < result.indexOf(NODE_A));
        }

        @Test
        @DisplayName("Should handle multiple independent components")
        void shouldHandleMultipleIndependentComponents() throws ProcessingException {
            // Given - Two independent chains: A->B and C->D
            graph.addNode(NODE_A, Set.of(NODE_B));
            graph.addNode(NODE_B, Collections.emptySet());
            graph.addNode(NODE_C, Set.of(NODE_D));
            graph.addNode(NODE_D, Collections.emptySet());

            // When
            List<String> result = sorter.sort(graph);

            // Then
            assertEquals(4, result.size());
            assertTrue(result.indexOf(NODE_B) < result.indexOf(NODE_A));
            assertTrue(result.indexOf(NODE_D) < result.indexOf(NODE_C));
        }
    }

    @Nested
    @DisplayName("Cycle Detection and Error Handling")
    class CycleDetectionAndErrorHandling {

        @Test
        @DisplayName("Should throw exception for simple cycle")
        void shouldThrowExceptionForSimpleCycle() {
            // Given - A -> B -> A
            graph.addNode(NODE_A, Set.of(NODE_B));
            graph.addNode(NODE_B, Set.of(NODE_A));

            // When/Then
            ProcessingException exception = assertThrows(
                    ProcessingException.class,
                    () -> sorter.sort(graph)
            );

            assertTrue(exception.getMessage().contains(CIRCULAR_DEPENDENCY_DETECTED));
            assertTrue(exception.getMessage().contains("@MetaModel classes"));
        }

        @Test
        @DisplayName("Should throw exception for complex cycle")
        void shouldThrowExceptionForComplexCycle() {
            // Given - A -> B -> C -> A
            graph.addNode(NODE_A, Set.of(NODE_B));
            graph.addNode(NODE_B, Set.of(NODE_C));
            graph.addNode(NODE_C, Set.of(NODE_A));

            // When/Then
            ProcessingException exception = assertThrows(
                    ProcessingException.class,
                    () -> sorter.sort(graph)
            );

            assertTrue(exception.getMessage().contains(CIRCULAR_DEPENDENCY_DETECTED));
        }

        @Test
        @DisplayName("Should throw exception for self-cycle")
        void shouldThrowExceptionForSelfCycle() {
            // Given - A -> A
            graph.addNode(NODE_A, Set.of(NODE_A));

            // When/Then
            ProcessingException exception = assertThrows(
                    ProcessingException.class,
                    () -> sorter.sort(graph)
            );

            assertTrue(exception.getMessage().contains(CIRCULAR_DEPENDENCY_DETECTED));
        }

        @Test
        @DisplayName("Should format cycle information in error message")
        void shouldFormatCycleInformationInErrorMessage() {
            // Given - Multiple cycles
            graph.addNode(NODE_A, Set.of(NODE_B));
            graph.addNode(NODE_B, Set.of(NODE_A));
            graph.addNode(NODE_C, Set.of(NODE_D));
            graph.addNode(NODE_D, Set.of(NODE_C));

            // When/Then
            ProcessingException exception = assertThrows(
                    ProcessingException.class,
                    () -> sorter.sort(graph)
            );

            String message = exception.getMessage();
            assertTrue(message.contains("Cycles found:"));
            assertTrue(message.contains("[") && message.contains("]"));
        }
    }

    @Nested
    @DisplayName("Validation Methods")
    class ValidationMethods {

        @Test
        @DisplayName("Should validate correct topological order")
        void shouldValidateCorrectTopologicalOrder() {
            // Given
            graph.addNode(NODE_A, Set.of(NODE_B));
            graph.addNode(NODE_B, Set.of(NODE_C));
            graph.addNode(NODE_C, Collections.emptySet());

            List<String> validOrder = List.of(NODE_C, NODE_B, NODE_A);

            // When
            boolean isValid = sorter.isValidTopologicalOrder(graph, validOrder);

            // Then
            assertTrue(isValid);
        }

        @Test
        @DisplayName("Should reject incorrect topological order")
        void shouldRejectIncorrectTopologicalOrder() {
            // Given
            graph.addNode(NODE_A, Set.of(NODE_B));
            graph.addNode(NODE_B, Set.of(NODE_C));
            graph.addNode(NODE_C, Collections.emptySet());

            List<String> invalidOrder = List.of(NODE_A, NODE_B, NODE_C); // Wrong order

            // When
            boolean isValid = sorter.isValidTopologicalOrder(graph, invalidOrder);

            // Then
            assertFalse(isValid);
        }

        @Test
        @DisplayName("Should reject order with wrong size")
        void shouldRejectOrderWithWrongSize() {
            // Given
            graph.addNode(NODE_A, Set.of(NODE_B));
            graph.addNode(NODE_B, Collections.emptySet());

            List<String> wrongSizeOrder = List.of(NODE_A); // Missing NodeB

            // When
            boolean isValid = sorter.isValidTopologicalOrder(graph, wrongSizeOrder);

            // Then
            assertFalse(isValid);
        }

        @Test
        @DisplayName("Should reject order with missing nodes")
        void shouldRejectOrderWithMissingNodes() {
            // Given
            graph.addNode(NODE_A, Set.of(NODE_B));
            graph.addNode(NODE_B, Collections.emptySet());

            List<String> missingNodeOrder = List.of(NODE_A, NODE_C); // NodeC not in graph

            // When
            boolean isValid = sorter.isValidTopologicalOrder(graph, missingNodeOrder);

            // Then
            assertFalse(isValid);
        }

        @Test
        @DisplayName("Should validate empty order for empty graph")
        void shouldValidateEmptyOrderForEmptyGraph() {
            // Given
            List<String> emptyOrder = Collections.emptyList();

            // When
            boolean isValid = sorter.isValidTopologicalOrder(graph, emptyOrder);

            // Then
            assertTrue(isValid);
        }

        @Test
        @DisplayName("Should validate single node order")
        void shouldValidateSingleNodeOrder() {
            // Given
            graph.addNode(NODE_A, Collections.emptySet());
            List<String> singleNodeOrder = List.of(NODE_A);

            // When
            boolean isValid = sorter.isValidTopologicalOrder(graph, singleNodeOrder);

            // Then
            assertTrue(isValid);
        }

        @Test
        @DisplayName("Should validate multiple valid orders")
        void shouldValidateMultipleValidOrders() {
            // Given - Diamond pattern allows multiple valid orders
            graph.addNode(NODE_A, Set.of(NODE_B, NODE_C));
            graph.addNode(NODE_B, Set.of(NODE_D));
            graph.addNode(NODE_C, Set.of(NODE_D));
            graph.addNode(NODE_D, Collections.emptySet());

            List<String> validOrder1 = List.of(NODE_D, NODE_B, NODE_C, NODE_A);
            List<String> validOrder2 = List.of(NODE_D, NODE_C, NODE_B, NODE_A);

            // When/Then
            assertTrue(sorter.isValidTopologicalOrder(graph, validOrder1));
            assertTrue(sorter.isValidTopologicalOrder(graph, validOrder2));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Performance")
    class EdgeCasesAndPerformance {

        @Test
        @DisplayName("Should handle large graphs efficiently")
        void shouldHandleLargeGraphsEfficiently() throws ProcessingException {
            // Given - Linear chain of 1000 nodes
            for (int i = 0; i < 1000; i++) {
                String currentNode = "Node" + i;
                if (i == 0) {
                    graph.addNode(currentNode, Collections.emptySet());
                } else {
                    graph.addNode(currentNode, Set.of("Node" + (i - 1)));
                }
            }

            // When
            long startTime = System.currentTimeMillis();
            List<String> result = sorter.sort(graph);
            long endTime = System.currentTimeMillis();

            // Then
            assertEquals(1000, result.size());
            assertEquals("Node0", result.get(0)); // First node has no dependencies
            assertEquals("Node999", result.get(999)); // Last node depends on all others

            // Performance check - should complete in reasonable time
            assertTrue(endTime - startTime < 1000, "Sorting should complete within 1 second");
        }

        @Test
        @DisplayName("Should handle wide dependency graphs")
        void shouldHandleWideDependencyGraphs() throws ProcessingException {
            // Given - One node depends on 100 others
            Set<String> manyDependencies = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                String depNode = "Dep" + i;
                manyDependencies.add(depNode);
                graph.addNode(depNode, Collections.emptySet());
            }
            graph.addNode(MAIN_NODE, manyDependencies);

            // When
            List<String> result = sorter.sort(graph);

            // Then
            assertEquals(101, result.size());
            assertEquals(MAIN_NODE, result.get(100)); // Should be last

            // All dependencies should come before MainNode
            int mainNodeIndex = result.indexOf(MAIN_NODE);
            for (String dep : manyDependencies) {
                assertTrue(result.indexOf(dep) < mainNodeIndex);
            }
        }

        @Test
        @DisplayName("Should handle nodes with no dependencies correctly")
        void shouldHandleNodesWithNoDependenciesCorrectly() throws ProcessingException {
            // Given - Multiple nodes with no dependencies
            graph.addNode(INDEPENDENT_1, Collections.emptySet());
            graph.addNode(INDEPENDENT_2, Collections.emptySet());
            graph.addNode(INDEPENDENT_3, Collections.emptySet());
            graph.addNode(DEPENDENT, Set.of(INDEPENDENT_1));

            // When
            List<String> result = sorter.sort(graph);

            // Then
            assertEquals(4, result.size());
            assertEquals(DEPENDENT, result.get(3)); // Should be last

            // Independent nodes should come first (in any order)
            List<String> firstThree = result.subList(0, 3);
            assertTrue(firstThree.contains(INDEPENDENT_1));
            assertTrue(firstThree.contains(INDEPENDENT_2));
            assertTrue(firstThree.contains(INDEPENDENT_3));
        }

        @Test
        @DisplayName("Should maintain consistency across multiple sorts")
        void shouldMaintainConsistencyAcrossMultipleSorts() throws ProcessingException {
            // Given
            graph.addNode(NODE_A, Set.of(NODE_B));
            graph.addNode(NODE_B, Set.of(NODE_C));
            graph.addNode(NODE_C, Collections.emptySet());

            // When - Sort multiple times
            List<String> result1 = sorter.sort(graph);
            List<String> result2 = sorter.sort(graph);
            List<String> result3 = sorter.sort(graph);

            // Then - Results should be consistent
            assertEquals(result1, result2);
            assertEquals(result2, result3);

            // And all should be valid topological orders
            assertTrue(sorter.isValidTopologicalOrder(graph, result1));
            assertTrue(sorter.isValidTopologicalOrder(graph, result2));
            assertTrue(sorter.isValidTopologicalOrder(graph, result3));
        }
    }
}