package com.thy.fss.common.inmemory.datasource;

/**
 * Bir StreamingDataSource'un yaşam döngüsü durumunu temsil eder.
 */
public enum StreamingDataSourceState {
    /** Initial data load devam ediyor, datasource hazır değil */
    INITIALIZING,
    /** Initial load tamamlandı, artımlı güncelleme modu */
    READY,
    /** Bağlantı hatası veya kritik hata */
    ERROR
}
