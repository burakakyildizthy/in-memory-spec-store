package com.thy.fss.common.inmemory.engine.sync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.thy.fss.common.inmemory.engine.mapping.PropertyMapping;

/**
 * Bir batch snapshot işleme sonucunda güncellenmesi gereken
 * store ve dashboard tüketicilerinin kümesi.
 *
 * <p>Bu sınıf immutable'dır — tüm koleksiyonlar constructor'da
 * defensive copy ile kopyalanır ve getter'lar unmodifiable view döndürür.</p>
 */
public final class AffectedConsumerSet {

    private final Set<String> storeIds;
    private final Set<String> dashboardIds;
    private final List<PropertyMapping<?, ?>> affectedMappings;

    /**
     * @param storeIds         etkilenen store ID'leri (null olamaz)
     * @param dashboardIds     etkilenen dashboard ID'leri (null olamaz)
     * @param affectedMappings etkilenen mapping'ler (null olamaz)
     */
    public AffectedConsumerSet(Set<String> storeIds,
                               Set<String> dashboardIds,
                               List<PropertyMapping<?, ?>> affectedMappings) {
        Objects.requireNonNull(storeIds, "storeIds must not be null");
        Objects.requireNonNull(dashboardIds, "dashboardIds must not be null");
        Objects.requireNonNull(affectedMappings, "affectedMappings must not be null");

        this.storeIds = Collections.unmodifiableSet(new LinkedHashSet<>(storeIds));
        this.dashboardIds = Collections.unmodifiableSet(new LinkedHashSet<>(dashboardIds));
        this.affectedMappings = Collections.unmodifiableList(new ArrayList<>(affectedMappings));
    }

    public Set<String> getStoreIds() {
        return storeIds;
    }

    public Set<String> getDashboardIds() {
        return dashboardIds;
    }

    public List<PropertyMapping<?, ?>> getAffectedMappings() {
        return affectedMappings;
    }
}
