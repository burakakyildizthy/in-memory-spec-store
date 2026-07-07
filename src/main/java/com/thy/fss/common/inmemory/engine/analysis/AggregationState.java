package com.thy.fss.common.inmemory.engine.analysis;

/**
 * Caches the current aggregation values for a single dashboard+task combination.
 * Used by {@code IncrementalSyncProcessor} to enable incremental aggregation
 * computation instead of full scans on every batch snapshot event.
 *
 * <p>On the first computation (when no cached state exists), a full scan is performed
 * and the results are stored here. Subsequent computations use the cached values
 * and apply deltas from changed/removed entities.</p>
 *
 * <p>For MIN/MAX, the entity IDs holding the current min/max are tracked so that
 * we can detect when a full scan is needed (i.e., when the min/max entity is
 * among the changed or removed entities).</p>
 */
public class AggregationState {

    private long count;
    private double sum;
    private Double min;
    private Double max;
    private Object minEntityId;
    private Object maxEntityId;

    public AggregationState() {
        this.count = 0;
        this.sum = 0.0;
        this.min = null;
        this.max = null;
        this.minEntityId = null;
        this.maxEntityId = null;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public double getSum() {
        return sum;
    }

    public void setSum(double sum) {
        this.sum = sum;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public Object getMinEntityId() {
        return minEntityId;
    }

    public void setMinEntityId(Object minEntityId) {
        this.minEntityId = minEntityId;
    }

    public Object getMaxEntityId() {
        return maxEntityId;
    }

    public void setMaxEntityId(Object maxEntityId) {
        this.maxEntityId = maxEntityId;
    }

    @Override
    public String toString() {
        return String.format("AggregationState[count=%d, sum=%.2f, min=%s, max=%s]",
                count, sum, min, max);
    }
}
