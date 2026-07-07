package com.thy.fss.common.inmemory.engine.index;

/**
 * Lightweight statistics for index performance tracking.
 * Immutable class with zero runtime overhead - only captures creation-time metrics.
 */
public final class IndexStatistics {
    
    private final String datasourceName;
    private final long creationTimeMs;
    private final int totalEntries;
    private final int depth;
    private final long creationTimestamp;
    
    /**
     * Creates a new IndexStatistics instance.
     *
     * @param datasourceName    The name of the datasource
     * @param creationTimeMs    The time taken to create the index in milliseconds
     * @param totalEntries      The total number of entries in the index
     * @param depth             The depth of the index (number of levels)
     */
    public IndexStatistics(String datasourceName, long creationTimeMs, int totalEntries, int depth) {
        this.datasourceName = datasourceName;
        this.creationTimeMs = creationTimeMs;
        this.totalEntries = totalEntries;
        this.depth = depth;
        this.creationTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Gets the datasource name.
     *
     * @return The datasource name
     */
    public String getDatasourceName() {
        return datasourceName;
    }
    
    /**
     * Gets the index creation time in milliseconds.
     *
     * @return The creation time in milliseconds
     */
    public long getCreationTimeMs() {
        return creationTimeMs;
    }
    
    /**
     * Gets the total number of entries in the index.
     *
     * @return The total number of entries
     */
    public int getTotalEntries() {
        return totalEntries;
    }
    
    /**
     * Gets the depth of the index.
     *
     * @return The number of levels in the index
     */
    public int getDepth() {
        return depth;
    }
    
    /**
     * Gets the timestamp when the index was created.
     *
     * @return The creation timestamp in milliseconds since epoch
     */
    public long getCreationTimestamp() {
        return creationTimestamp;
    }
    
    /**
     * Generates a formatted report of the index statistics.
     *
     * @return A formatted string containing all statistics
     */
    public String getFormattedReport() {
        return String.format(
            "Index Statistics for '%s':%n" +
            "  Creation Time: %d ms%n" +
            "  Total Entries: %d%n" +
            "  Depth: %d level(s)%n" +
            "  Created At: %s",
            datasourceName,
            creationTimeMs,
            totalEntries,
            depth,
            new java.util.Date(creationTimestamp)
        );
    }
    
    @Override
    public String toString() {
        return String.format(
            "IndexStatistics{datasource='%s', creationTimeMs=%d, totalEntries=%d, depth=%d}",
            datasourceName, creationTimeMs, totalEntries, depth
        );
    }
}
