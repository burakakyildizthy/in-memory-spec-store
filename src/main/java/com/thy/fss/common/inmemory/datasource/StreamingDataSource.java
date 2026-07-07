package com.thy.fss.common.inmemory.datasource;

import com.thy.fss.common.inmemory.entity.Identifiable;

/**
 * Streaming veri kaynağı arayüzü.
 * {@link DataSource} arayüzünü extend ederek fetchAll() dahil tüm batch yeteneklerini sağlar.
 * Ek olarak subscribe/unsubscribe ile batch snapshot stream yeteneği sunar.
 *
 * <p>DataSource'tan miras alınan metotlar: {@code fetchAll()}, {@code fetchAllById()},
 * {@code getName()}, {@code getEntityType()}, {@code isHealthy()}, {@code close()},
 * {@code getFallbackDataSource()}, {@code setFallbackDataSource()}</p>
 *
 * @param <T> Identifiable uygulayan entity tipi
 */
public interface StreamingDataSource<T extends Identifiable<?>> extends DataSource<T> {

    /**
     * Bu streaming datasource'un mevcut yaşam döngüsü durumunu döndürür.
     *
     * @return mevcut durum (INITIALIZING, READY veya ERROR)
     */
    StreamingDataSourceState getState();

    /**
     * Verilen listener'ı bu streaming datasource'a abone eder.
     * Abone edilen listener, {@link BatchSnapshotEvent} nesneleri alır.
     *
     * @param listener abone edilecek listener
     */
    void subscribe(BatchSnapshotEventListener<T> listener);

    /**
     * Verilen listener'ın aboneliğini iptal eder.
     *
     * @param listener aboneliği iptal edilecek listener
     */
    void unsubscribe(BatchSnapshotEventListener<T> listener);
}
