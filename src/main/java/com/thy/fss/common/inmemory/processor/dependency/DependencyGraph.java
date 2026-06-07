package com.thy.fss.common.inmemory.processor.dependency;

import java.util.*;

/**
 * Represents a dependency graph for @MetaModel annotated classes.
 * This graph is used to determine the correct order for code generation
 * to ensure that dependencies are generated before their dependents.
 */
public class DependencyGraph {

    private final Map<String, Set<String>> dependencies = new HashMap<>();
    private final Set<String> nodes = new HashSet<>();

    /**
     * Adds a node to the dependency graph with its dependencies.
     *
     * @param node             the node to add
     * @param nodeDependencies the set of nodes this node depends on
     */
    public void addNode(String node, Set<String> nodeDependencies) {
        nodes.add(node);
        dependencies.put(node, new HashSet<>(nodeDependencies));

        // Also add dependency nodes to the graph if they're not already present
        for (String dependency : nodeDependencies) {
            if (!nodes.contains(dependency)) {
                nodes.add(dependency);
                dependencies.putIfAbsent(dependency, new HashSet<>());
            }
        }
    }

    /**
     * Gets all nodes in the graph.
     *
     * @return set of all nodes
     */
    public Set<String> getNodes() {
        return new HashSet<>(nodes);
    }

    /**
     * Gets the dependencies of a specific node.
     *
     * @param node the node to get dependencies for
     * @return set of dependencies for the node, or empty set if node doesn't exist
     */
    public Set<String> getDependencies(String node) {
        return dependencies.getOrDefault(node, new HashSet<>());
    }

    /**
     * Gets all nodes that depend on the specified node.
     *
     * @param node the node to find dependents for
     * @return set of nodes that depend on the specified node
     */
    public Set<String> getDependents(String node) {
        Set<String> dependents = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            if (entry.getValue().contains(node)) {
                dependents.add(entry.getKey());
            }
        }
        return dependents;
    }

    /**
     * Calculates the in-degree (number of incoming edges) for each node.
     * In-degree represents how many dependencies this node has.
     *
     * @return map of node to its in-degree
     */
    public Map<String, Integer> calculateInDegrees() {
        Map<String, Integer> inDegrees = new HashMap<>();

        // Initialize all nodes with in-degree 0
        for (String node : nodes) {
            inDegrees.put(node, 0);
        }

        // Count incoming edges - each node's in-degree is the number of its dependencies
        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            String node = entry.getKey();
            Set<String> nodeDependencies = entry.getValue();
            inDegrees.put(node, nodeDependencies.size());
        }

        return inDegrees;
    }

    /**
     * Checks if the graph has any cycles.
     *
     * @return true if the graph contains cycles, false otherwise
     */
    public boolean hasCycles() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String node : nodes) {
            if (!visited.contains(node) && hasCyclesUtil(node, visited, recursionStack)) {
                return true;
            }

        }

        return false;
    }

    /**
     * Utility method for cycle detection using DFS.
     */
    private boolean hasCyclesUtil(String node, Set<String> visited, Set<String> recursionStack) {
        visited.add(node);
        recursionStack.add(node);

        Set<String> nodeDependencies = dependencies.getOrDefault(node, new HashSet<>());
        for (String dependency : nodeDependencies) {
            if (!visited.contains(dependency)) {
                if (hasCyclesUtil(dependency, visited, recursionStack)) {
                    return true;
                }
            } else if (recursionStack.contains(dependency)) {
                return true; // Back edge found, cycle detected
            }
        }

        recursionStack.remove(node);
        return false;
    }

    /**
     * Finds and returns all cycles in the graph.
     *
     * @return list of cycles, where each cycle is represented as a list of nodes
     */
    public List<List<String>> findCycles() {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        List<String> currentPath = new ArrayList<>();

        for (String node : nodes) {
            if (!visited.contains(node)) {
                findCyclesUtil(node, visited, recursionStack, currentPath, cycles);
            }
        }

        return cycles;
    }

    /**
     * Utility method for finding cycles using DFS.
     */
    private void findCyclesUtil(String node, Set<String> visited, Set<String> recursionStack,
                                List<String> currentPath, List<List<String>> cycles) {
        visited.add(node);
        recursionStack.add(node);
        currentPath.add(node);

        Set<String> nodeDependencies = dependencies.getOrDefault(node, new HashSet<>());
        for (String dependency : nodeDependencies) {
            if (!visited.contains(dependency)) {
                findCyclesUtil(dependency, visited, recursionStack, currentPath, cycles);
            } else if (recursionStack.contains(dependency)) {
                // Found a cycle, extract it from current path
                int cycleStart = currentPath.indexOf(dependency);
                List<String> cycle = new ArrayList<>(currentPath.subList(cycleStart, currentPath.size()));
                cycle.add(dependency); // Complete the cycle
                cycles.add(cycle);
            }
        }

        recursionStack.remove(node);
        currentPath.remove(currentPath.size() - 1);
    }

    /**
     * Returns the number of nodes in the graph.
     *
     * @return number of nodes
     */
    public int size() {
        return nodes.size();
    }

    /**
     * Checks if the graph is empty.
     *
     * @return true if the graph has no nodes, false otherwise
     */
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DependencyGraph{\n");
        for (String node : nodes) {
            sb.append("  ").append(node).append(" -> ").append(dependencies.get(node)).append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}