package com.thy.fss.common.inmemory.engine.sync;

import java.time.Instant;

/**
 * Batch ve streaming versiyonlarını birleştiren bilgi amaçlı yapı.
 * İzleme ve raporlama için birleşik versiyon bilgisi sağlar.
 *
 * @param batchVersion        son batch full sync versiyonu
 * @param streamingVersion    streaming güncelleme sayacı
 * @param lastStreamingUpdate son streaming güncelleme zamanı
 */
public record CompositeVersion(
        long batchVersion,
        long streamingVersion,
        Instant lastStreamingUpdate
) {
}
