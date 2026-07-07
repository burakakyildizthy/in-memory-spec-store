# Uygulama Planı (Implementation Plan)

Bu plan, tasarım belgesindeki **Fix Implementation** ve **Testing Strategy** bölümlerine dayanır.

**Test konvansiyonu:** Property-based testler jqwik ile yazılır (`net.jqwik.api.*`; `@Property`,
`@ForAll`, `@Provide`, `@Example`); doğrulamalar AssertJ (`assertThat`) ile yapılır.
**Fixture'lar:** `CollectionEntity` (`List<String> tags`, `Set<Integer> numbers`),
`MixedCollectionEntity`, `Order` (`List<OrderItem> items`), `OrderItem`
(paket `com.thy.fss.common.inmemory.testmodel`). Bu tipler `@MetaModel` ile işaretli olduğundan
generate edilen `CollectionEntity_`, `Order_` metamodel sınıfları ve
`CollectionEntitySpecificationService.INSTANCE`, `OrderSpecificationService.INSTANCE` servisleri
testlerde kullanılabilir.

**Yeni test sınıfları** (paket `com.thy.fss.common.inmemory.engine.mapping`):
- `CollectionMappingNestingFaultConditionPropertyTest` (exploration)
- `CollectionMappingNestingFixCheckingPropertyTest` (fix checking)
- `CollectionMappingNestingPreservationPropertyTest` (preservation)
- `CollectionMappingNestingExampleTest` (somut birim örnekleri)
- `CollectionMappingNestingIntegrationTest` (tam senkronizasyon akışı)

**Derleme/çalıştırma:** Gradle ile. Windows: `gradlew.bat ...`, diğer kabuklar: `./gradlew ...`.

**Kusurun merkezleri (tasarımdan):**
`BaseSpecificationService.handleCollectionField(...)` (setValueByPath yolu, `collection.add(elementToAdd)`)
ve `BaseSpecificationService.addValueToCollection(...)` (setValueByPathWithCollections/ALL yolu,
`targetCollection.add(value)`). Ayrıca `MappingApplicator.applyCollectionMapping(...)` içindeki tek
seviyeli kısmi workaround.

---

- [x] 1. Fault-condition (exploration) property testini yaz — düzeltme ÖNCESİ
  - **Property 1: Bug Condition** - Koleksiyondan Koleksiyona Düz ve İdempotent Atama
  - **KRİTİK**: Bu test DÜZELTİLMEMİŞ kodda BAŞARISIZ olmalıdır — başarısızlık hatanın var olduğunu kanıtlar
  - **Test veya kod, bu test başarısız olunca DÜZELTİLMEYE ÇALIŞILMAMALIDIR**
  - **NOT**: Bu test beklenen davranışı (düz + idempotent) kodlar; düzeltme sonrası geçtiğinde fix'i doğrular
  - **AMAÇ**: Hatayı gösteren karşı örnekleri (`[[...]]` iç içe sarmalama) yüzeye çıkarmak
  - Sınıf: `CollectionMappingNestingFaultConditionPropertyTest`
  - **Scoped PBT yaklaşımı**: Deterministik hata için özellikleri somut, tekrarlanabilir senaryolara daralt:
    - **Senaryo A (1.1 — `Order.items`, model koleksiyonu):** Rastgele `List<OrderItem>` üret; `OrderSpecificationService.INSTANCE.setValueByPath(order, List.of(Order_.items), sourceItems)` çağır; `order.getItems()` düz `[i1, i2]` assert et (düzeltilmemişte `[[i1, i2]]` — FAIL)
    - **Senaryo B (1.1 — `CollectionEntity.tags`, `List<String>`):** Rastgele `List<String>` üret; `setValueByPath(entity, List.of(CollectionEntity_.tags), sourceTags)`; `tags` düz assert et (düzeltilmemişte `[["a","b"]]` — FAIL)
    - **Senaryo C (1.3 — çok seviyeli hedef):** Son elemanı koleksiyon olan çok seviyeli bir yola koleksiyon atanır (`MappingApplicator.assignTargetValue(...) -> setValueByPath`); düz sonuç assert et (tek seviyeli workaround bu yolu kapsamadığı için FAIL)
    - **Senaryo D (1.2 — idempotentlik):** Aynı koleksiyondan koleksiyona atama aynı hedefe iki kez uygulanır; hedef koleksiyon boyutunun sabit kaldığı assert et (düzeltilmemişte büyür — FAIL)
    - **Senaryo E (ALL seçici yolu — `addValueToCollection`):** `setValueByPathWithCollections(...)` ile son indekste ALL seçicili `CollectionOperationMetadata` kullanılarak bütün bir koleksiyon atanır; düz sonuç assert et (FAIL)
  - Testi DÜZELTİLMEMİŞ kodda çalıştır: `gradlew.bat test --tests "*CollectionMappingNestingFaultConditionPropertyTest"`
  - **BEKLENEN SONUÇ**: Test BAŞARISIZ (bu doğrudur — hatanın var olduğunu kanıtlar)
  - Bulunan karşı örnekleri belgele (ör. `tags == [["a","b"]]`; tekrar uygulamada `[["a","b"], ["a","b"]]`)
  - Görev, test yazılıp çalıştırıldığında ve başarısızlık belgelendiğinde tamamlanır
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 2. Preservation (koruma) property testlerini yaz — düzeltme ÖNCESİ
  - **Property 2: Preservation** - Koleksiyon-Olmayan ve Tek-Eleman Girişlerinin Korunması
  - **ÖNEMLİ**: Observation-first metodolojisini izle — davranışı önce DÜZELTİLMEMİŞ kodda gözlemle, sonra bu davranışı yakalayan property testlerini yaz
  - Sınıf: `CollectionMappingNestingPreservationPropertyTest`
  - Gözlemlenip property testleriyle sabitlenecek ve DÜZELTİLMEMİŞ kodda GEÇTİĞİ doğrulanacak durumlar:
    - **3.1 Tek eleman ekleme:** FIRST/LAST/ANY veya skaler ile seçilmiş tek eleman koleksiyona eklenir (append korunur)
    - **3.2 Skaler hedef:** Koleksiyon olmayan (skaler) alana çözümlenen değer doğrudan atanır
    - **3.3 Agregasyon:** SUM/AVG/COUNT/MIN/MAX skaler hedefe değiştirilmeden yazılır
    - **3.4 Meşru iç içe:** Hedef eleman tipi bir koleksiyon (`List<List<String>>`) iken tek bir iç liste eklenir -> sonuç `[[...]]` (add davranışı korunur)
    - **3.5 Null koleksiyon başlatma:** Null koleksiyon alanı önce initialize edilip sonra doldurulur
  - Rastgele tek elemanlar, skaler değerler ve meşru iç içe girişler üreterek property tabanlı güçlü garanti sağla
  - Testleri DÜZELTİLMEMİŞ kodda çalıştır: `gradlew.bat test --tests "*CollectionMappingNestingPreservationPropertyTest"`
  - **BEKLENEN SONUÇ**: Testler GEÇER (korunacak temel/baseline davranış onaylanır)
  - Görev, testler yazılıp çalıştırıldığında ve düzeltilmemiş kodda geçtiği doğrulandığında tamamlanır
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 3. Koleksiyon-atama mantığını `BaseSpecificationService` içinde merkezî olarak düzelt
  - Dosya: `src/main/java/com/thy/fss/common/inmemory/specification/BaseSpecificationService.java`

  - [x] 3.1 Yardımcı metotları ekle
    - `elementTypeOf(MetaAttribute<?,?> attr)`: attr `CollectionAttribute` ise `getElementType()`, aksi halde `null` döndür
    - `allElementsAssignable(Collection<?> values, Class<?> elementType)`: koleksiyondaki null-olmayan tüm elemanlar için `isAssignableFrom(elementType, el.getClass())` sağlanır; boş koleksiyonda `true`
    - `resolveElementToAdd(Object value, MetaAttribute<?,?> attr)`: mevcut `handleCollectionField` içindeki "eleman tipi model (boxed değil) ve value `null` ise `SpecificationServices.getService(elementType).createInstance()` ile yeni örnek üret" mantığını kapsülle
    - Mevcut `isAssignableFrom(...)` (primitive/wrapper uyumu dahil) yeniden kullanılır
    - _Bug_Condition: isBugCondition(input) — tasarımdaki formal tanım (elementTypeOf/allElementsAssignable ile)_
    - _Preservation: 3.5 (null-model örnek üretimi korunur)_
    - _Requirements: 3.4, 3.5_

  - [x] 3.2 Ortak ayırt edici (discriminator) yardımcı `assignToCollectionField(...)` ekle
    - İmza: `assignToCollectionField(Object container, MetaAttribute<?,?> collectionAttr, Object value, SpecificationService<Object> service)`
    - Koleksiyon `null` ise `new ArrayList<>()` ile initialize edip `setFieldValue(...)` ile yerleştir (3.5)
    - **C koşulu** (`value instanceof Collection` **&&** `elementType != null` **&&** `NOT isAssignable(elementType, value.getClass())` **&&** `allElementsAssignable((Collection) value, elementType)`): `snapshot = new ArrayList<>((Collection) value)`, `collection.clear()`, `collection.addAll(snapshot)` — düz (flat) + idempotent; mevcut örnek/somut tip (`List`/`Set`) korunur, aliasing güvenli
    - **¬C** (tek eleman / skaler / meşru iç içe): `collection.add(resolveElementToAdd(value, collectionAttr))`
    - `elementType == null` ise düz-set YAPMA, mevcut `add` davranışını koru (muhafazakâr, dokümante edilmiş sınırlama)
    - _Bug_Condition: isBugCondition(input) from design_
    - _Expected_Behavior: expectedBehavior(targetCollectionAfter, sourceCollection) — düz + isIdempotent_
    - _Preservation: Preservation Requirements (3.1, 3.4, 3.5)_
    - _Requirements: 2.1, 2.2, 2.3, 3.4, 3.5_

  - [x] 3.3 `handleCollectionField(...)` metodunu `assignToCollectionField(...)`'a yönlendir
    - Sondaki `collection.add(elementToAdd)` çağrısını ve null-init + elementType/null-model üretim mantığını ortak yardımcıya taşı/delege et
    - `setValueByPath` yolu (ONE_TO_ONE koleksiyon-kaynak ve çok seviyeli koleksiyon hedefleri) düzeltmeden yararlanır
    - _Expected_Behavior: expectedBehavior(result) from design_
    - _Preservation: 3.1, 3.4, 3.5_
    - _Requirements: 2.1, 2.3, 3.1, 3.4, 3.5_

  - [x] 3.4 `addValueToCollection(...)` metodunu `assignToCollectionField(...)`'a yönlendir
    - `targetCollection.add(value)` yerine aynı ortak ayırt edici mantığı uygula (ALL seçici yolu — `handleCollectionOperation` üzerinden)
    - `setValueByPathWithCollections` yolu da tek ve tutarlı davranış kazanır
    - _Expected_Behavior: expectedBehavior(result) from design_
    - _Preservation: 3.1_
    - _Requirements: 2.1, 3.1_

  - [x] 3.5 Fault-condition (exploration) testinin artık GEÇTİĞİNİ doğrula
    - **Property 1: Expected Behavior** - Koleksiyondan Koleksiyona Düz ve İdempotent Atama
    - **ÖNEMLİ**: Görev 1'deki AYNI testi yeniden çalıştır — YENİ test yazma. Bu test beklenen davranışı kodlar; geçmesi beklenen davranışın karşılandığını onaylar
    - `gradlew.bat test --tests "*CollectionMappingNestingFaultConditionPropertyTest"`
    - **BEKLENEN SONUÇ**: Test GEÇER (hata düzeldiğini onaylar)
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 3.6 Preservation testlerinin HÂLÂ GEÇTİĞİNİ doğrula
    - **Property 2: Preservation** - Koleksiyon-Olmayan ve Tek-Eleman Girişlerinin Korunması
    - **ÖNEMLİ**: Görev 2'deki AYNI testleri yeniden çalıştır — YENİ test yazma
    - `gradlew.bat test --tests "*CollectionMappingNestingPreservationPropertyTest"`
    - **BEKLENEN SONUÇ**: Testler GEÇER (regresyon yok)
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [x] 4. Fix-checking property testini yaz (düzeltme SONRASI)
  - **Property 1: Expected Behavior** - Fix Checking: Düz (Flat) + İdempotent Atama
  - Sınıf: `CollectionMappingNestingFixCheckingPropertyTest`
  - `isBugCondition(input)` sağlayan tüm girişler için: `applyOnce` ve `applyTwice` (tekrar uygulama) hesapla; `expectedBehavior(applyOnce.targetCollection, source)` ve `isIdempotent(applyOnce, applyTwice)` assert et
  - Rastgele `List<String>` (`CollectionEntity.tags`) ve `Set<Integer>` (`CollectionEntity.numbers`) üret; tek seviyeli ve çok seviyeli hedeflerde düz + idempotent olduğunu doğrula
  - `Order.items` (model koleksiyonu) için de düz + idempotent doğrula
  - **İdempotentlik (2.2):** İki kez uygulamada boyut ve içerik sabit; sınırsız büyüme yok
  - `gradlew.bat test --tests "*CollectionMappingNestingFixCheckingPropertyTest"` → GEÇER
  - _Requirements: 2.1, 2.2, 2.3_

- [x] 5. Somut birim testlerini yaz (Example)
  - Sınıf: `CollectionMappingNestingExampleTest`
  - `Order.items`: `[i1, i2]` -> düz `[i1, i2]` (`setValueByPath` yolu)
  - `CollectionEntity.tags` (`List<String>`): `["a","b"]` -> `["a","b"]`
  - `CollectionEntity.numbers` (`Set<Integer>`): `{1,2}` -> `{1,2}`; hedef somut tipinin `Set` olarak korunduğunu doğrula (clear()+addAll)
  - Boş kaynak koleksiyon: hedef boşalır (idempotent, kenar durum)
  - Null hedef alan: önce initialize edilir, sonra doldurulur (3.5)
  - Meşru iç içe (`List<List<String>>`): tek iç liste eklenir -> `[[...]]` (3.4 korunur)
  - ALL seçici yolu (`setValueByPathWithCollections`) için düz sonuç
  - `gradlew.bat test --tests "*CollectionMappingNestingExampleTest"` → GEÇER
  - _Requirements: 2.1, 2.2, 3.4, 3.5_

- [x] 6. Entegrasyon testlerini yaz
  - Sınıf: `CollectionMappingNestingIntegrationTest`
  - Tam senkronizasyon akışında (`MappingApplicator.applyMappingsToEntity` / gerçek eşleştirme) koleksiyondan koleksiyona eşleştirmenin düz sonuç ürettiğini doğrula
  - Tek seviyeli VE çok seviyeli hedeflerde tutarlı davranış (2.3)
  - Tekrarlanan senkronizasyon (birden çok sync döngüsü) sonrası hedef koleksiyonun büyümediğini (idempotent, 2.2) doğrula
  - Tek seviyeli MANY_TO_ONE_COLLECTION hedefleri için `List` (`tags`) VE `Set` (`numbers`) ayrı ayrı doğrulanır (Görev 7 geçidi için gerekli kanıt)
  - `gradlew.bat test --tests "*CollectionMappingNestingIntegrationTest"` → GEÇER
  - _Requirements: 2.1, 2.2, 2.3_

- [x] 7. [GEÇİTLİ] `MappingApplicator.applyCollectionMapping` kısmi workaround'unu kaldır / genelleştir
  - Dosya: `src/main/java/com/thy/fss/common/inmemory/engine/mapping/MappingApplicator.java`
  - **ÖN KOŞUL (GEÇİT — bu görev başlamadan sağlanmalı):** Tek seviyeli MANY_TO_ONE_COLLECTION hedefleri için fix-checking (Görev 4) ve preservation (Görev 3.6) testlerinin **hem `List` (`tags`) hem `Set` (`numbers`)** hedeflerinde GEÇTİĞİ, ayrıca entegrasyon testlerinin (Görev 6) GEÇTİĞİ doğrulanmış olmalıdır
  - `targetPath.size() == 1 && targetPath.get(0).getAttributeType() == AttributeType.COLLECTION` özel dalını (ve `new ArrayList<>(valuesToSet)` ile `setFieldValue` sarmalama atlatmasını) kaldır; tüm koleksiyon hedeflerini (tek/çok seviyeli) `assignTargetValue(rootEntity, mapping, valuesToSet)` üzerinden tek koda yönlendir
  - Fayda: tek/çok seviyeli tutarlılık (2.3); `clear()+addAll()` mevcut örneği koruduğu için `Set` hedeflerinde (`numbers`) tip uyumsuzluğu riski ortadan kalkar
  - Değişiklik sonrası tüm koleksiyon-eşleştirme ve MappingApplicator testlerini yeniden çalıştır
  - **FALLBACK PLANI:** Eğer testler tek seviyeli yolda herhangi bir davranış farkı ortaya çıkarırsa, workaround'u tamamen kaldırmak yerine ortak mantığa (`assignToCollectionField`) delege edecek şekilde **genelleştir** (kaldırma geri alınır, delege yaklaşımı uygulanır)
  - `gradlew.bat test --tests "*CollectionMapping*" --tests "*MappingApplicator*"` → GEÇER
  - _Requirements: 2.1, 2.3_

- [x] 8. Checkpoint — tüm testlerin geçtiğinden emin ol
  - Tam derleme ve test: `gradlew.bat build`
  - Tüm fault-condition / fix-checking / preservation / example / integration testleri GEÇER; mevcut mapping/koleksiyon testlerinde regresyon yok
  - Sorular ortaya çıkarsa kullanıcıya danış
  - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3, 3.1, 3.2, 3.3, 3.4, 3.5_
