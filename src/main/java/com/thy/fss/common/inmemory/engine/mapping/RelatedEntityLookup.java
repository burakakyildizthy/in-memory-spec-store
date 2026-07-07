package com.thy.fss.common.inmemory.engine.mapping;

import java.util.List;

/**
 * PK/FK ilişkisine göre foreign datasource'dan ilgili entity'leri bulan fonksiyonel arayüz.
 *
 * <p>Bu arayüz, mapping uygulama mantığını veri erişim katmanından soyutlar.
 * Full sync pipeline'da {@code DataVersion} üzerinden indexed lookup,
 * streaming pipeline'da {@code DependencyGraph} üzerinden in-memory FK eşleşmesi
 * ile implement edilir.</p>
 *
 * <p>Kullanım örneği:</p>
 * <pre>{@code
 * RelatedEntityLookup lookup = (mapping, pkValues) -> {
 *     // FK-bazlı entity lookup implementasyonu
 *     return matchingEntities;
 * };
 * }</pre>
 *
 * @see PropertyMapping
 * @see com.thy.fss.common.inmemory.engine.mapping.MappingApplicator
 */
@FunctionalInterface
public interface RelatedEntityLookup {

    /**
     * Verilen property mapping'in FK path'leri ve primary key değerleri kullanılarak
     * foreign datasource'dan eşleşen entity'leri bulur.
     *
     * @param mapping FK path'leri, datasource adı ve mapping tipini içeren property mapping tanımı
     * @param primaryKeyValues eşleştirilecek primary key değerleri (composite key durumunda birden fazla)
     * @return eşleşen foreign entity listesi; eşleşme bulunamazsa boş liste
     */
    List<?> lookupRelatedEntities(PropertyMapping<?, ?> mapping, List<Object> primaryKeyValues);
}
