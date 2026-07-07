package com.thy.fss.common.inmemory.processor.dependency;

import com.thy.fss.common.inmemory.processor.exception.ProcessingException;

import java.util.*;

/**
 * Performs topological sorting on a dependency graph using Kahn's algorithm.
 * This ensures that dependencies are processed before their dependents,
 * which is crucial for correct code generation order.
 */
public class TopologicalSorter {

    /**
     * Sorts the nodes in the dependency graph using topological sort.
     * Uses Kahn's algorithm which works by:
     * 1. Finding nodes with no incoming edges (in-degree 0)
     * 2. Removing these nodes and their outgoing edges
     * 3. Repeating until all nodes are processed or a cycle is detected
     *
     * @param graph the dependency graph to sort
     * @return list of nodes in topological order
     * @throws ProcessingException if the graph contains cycles
     */
    public List<String> sort(DependencyGraph graph) throws ProcessingException {
        if (graph.isEmpty()) {
            return new ArrayList<>();
        }

        // First check for cycles
        if (graph.hasCycles()) {
            List<List<String>> cycles = graph.findCycles();
            throw new ProcessingException(
                    "Circular dependency detected in @MetaModel classes. Cycles found: " +
                            formatCycles(cycles)
            );
        }

        // Perform Kahn's algorithm
        return performKahnsAlgorithm(graph);
    }

    /**
     * Implements Kahn's algorithm for topological sorting.
     */
    private List<String> performKahnsAlgorithm(DependencyGraph graph) throws ProcessingException {
        // Calculate in-degrees for all nodes
        Map<String, Integer> inDegrees = graph.calculateInDegrees();

        // Queue for nodes with no incoming edges
        Queue<String> queue = new LinkedList<>();
        List<String> result = new ArrayList<>();

        // Find all nodes with in-degree 0
        for (Map.Entry<String, Integer> entry : inDegrees.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        // Process nodes in topological order
        while (!queue.isEmpty()) {
            String current = queue.poll();
            result.add(current);

            // For each node that depends on current node, decrease its in-degree
            Set<String> dependents = graph.getDependents(current);
            for (String dependent : dependents) {
                // Decrease in-degree (remove the dependency on current)
                int newInDegree = inDegrees.get(dependent) - 1;
                inDegrees.put(dependent, newInDegree);

                // If in-degree becomes 0, add to queue
                if (newInDegree == 0) {
                    queue.offer(dependent);
                }
            }
        }

        // Verify that all nodes were processed
        if (result.size() != graph.size()) {
            // This should not happen if cycle detection worked correctly
            throw new ProcessingException(
                    "Topological sort failed: processed " + result.size() +
                            " nodes but graph has " + graph.size() + " nodes. " +
                            "This indicates a cycle that was not detected."
            );
        }

        return result;
    }

    /**
     * Formats cycle information for error messages.
     */
    private String formatCycles(List<List<String>> cycles) {
        if (cycles.isEmpty()) {
            return "No cycles found";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cycles.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("[");
            List<String> cycle = cycles.get(i);
            for (int j = 0; j < cycle.size(); j++) {
                if (j > 0) {
                    sb.append(" -> ");
                }
                sb.append(cycle.get(j));
            }
            sb.append("]");
        }
        return sb.toString();
    }

    /**
     * Validates that the given order is a valid topological sort for the graph.
     * This is useful for testing and verification.
     *
     * @param graph the dependency graph
     * @param order the proposed topological order
     * @return true if the order is valid, false otherwise
     */
    public boolean isValidTopologicalOrder(DependencyGraph graph, List<String> order) {
        if (order.size() != graph.size()) {
            return false;
        }

        // Create position map for efficient lookup
        Map<String, Integer> positions = new HashMap<>();
        for (int i = 0; i < order.size(); i++) {
            positions.put(order.get(i), i);
        }

        // Check that all dependencies come before their dependents
        for (String node : graph.getNodes()) {
            Integer nodePosition = positions.get(node);
            if (nodePosition == null) {
                return false; // Node not in order
            }

            Set<String> dependencies = graph.getDependencies(node);
            for (String dependency : dependencies) {
                Integer depPosition = positions.get(dependency);
                if (depPosition == null || depPosition >= nodePosition) {
                    return false; // Dependency comes after dependent
                }
            }
        }

        return true;
    }
}