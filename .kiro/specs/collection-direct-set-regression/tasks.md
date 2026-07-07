# Uygulama Planı (Implementation Plan)

Bu plan, `design.md` içindeki düzeltme stratejisini bug koşulu metodolojisiyle (C(X) / P(result) /
¬C(X)) hayata geçirir. Sıralama **test-önce (test-first)** yaklaşımını izler: önce düzeltilmemiş
kod (F) üzerinde regresyonu sabitleyen keşif (exploration) ve koruma (preservation) testleri
yazılır, ardından düzeltme (F') uygulanır ve doğrulanır. Test paketi mevcut
`CollectionMappingNesting*` (jqwik + AssertJ) suite'iyle tutarlıdır.

> **Kritik gözlem (design.md — Testing Strategy):** Mevcut 42 `CollectionMappingNesting*` testi
> geçiyor; çünkü hedef koleksiyonu **doğru somut tiple önceden başlatıyorlar** (örn. `numbers` için
> `HashSet`) ve eleman tipiyle **birebir uyumlu** (varyanssız) değerler kullanıyorlar. Bu iki durum
> hem null yolunu hem de eleman-tipi varyansı yolunu maskeliyor. Bu nedenle yeni keşif testleri
> **null hedefleri** ve **eleman-tipi varyansını** açıkça kapsamalıdır; aksi halde regresyon
> yüzeye çıkmaz.

- [x] 1. Bug koşulu keşif (exploration) özellik testini yaz
  - **Property 1: Bug Condition** - Koleksiyondan Koleksiyona Düz, İdempotent ve Tip-Koruyucu Atama
  - **KRİTİK**: Bu test DÜZELTİLMEMİŞ kodda BAŞARISIZ olmalıdır — başarısızlık hatanın var olduğunu
    kanıtlar. **Test başarısız olduğunda testi ya da kodu DÜZELTMEYE ÇALIŞMAYIN.**
  - **NOT**: Bu test beklenen davranışı (expectedBehavior) kodlar; düzeltme sonrası GEÇTİĞİNDE
    (Task 3.4) fix'i doğrulayacaktır.
  - **AMAÇ**: Hatayı gösteren karşı örnekleri (`[[...]]` iç içe sarmalama, sınırsız büyüme, null
    kalma) yüzeye çıkarmak; kök neden hipotezini (aşırı-katı `allElementsAssignable` + tip-duyarsız
    `new ArrayList<>()` null init) doğrulamak.
  - **Kapsanan tetikleyiciler (design.md — Bug Condition / isBugCondition):** `value` bir
    `Collection` VE hedef `AttributeType.COLLECTION` VE `elementType != null` VE eleman tipi
    kendisi koleksiyon DEĞİL iken; `triggersNesting` (eleman-tipi varyansı) VEYA `triggersNullDrop`
    (null hedef + `List` olmayan somut tip) sağlandığında.
  - Test dosyası: `src/test/java/com/thy/fss/common/inmemory/engine/mapping/CollectionMappingNestingFaultConditionPropertyTest.java`
    (mevcut suite'e null-hedef + varyans senaryoları eklenerek genişletilir).
  - **Senaryo (a) — `tags` (`List<String>`) null hedef + eleman-tipi varyansı → iç içe (1.1):**
    Bildirilen eleman tipiyle (`String`) birebir atanamayan değerlerden oluşan bir grup üretilir
    (örn. `List.of(7, 8)` — `Integer` değerler) ve önceden başlatılmamış (null) `tags` alanına
    `setValueByPath(entity, [CollectionEntity_.tags], source)` ile atanır. Düz `[7, 8]` beklenir.
    **DÜZELTİLMEMİŞ karşı örnek:** `allElementsAssignable` `false` döner → `else` dalı →
    `collection.add(source)` → iç içe `[[7, 8]]` (tek elemanlı, ilk eleman bir `List`).
  - **Senaryo (b) — Tekrarlı uygulama → sınırsız büyüme (1.2):** (a)'daki atama aynı hedefe 2-3 kez
    uygulanır; hedef boyutunun kaynak boyutuyla sabit kalması beklenir (idempotent).
    **DÜZELTİLMEMİŞ karşı örnek:** her uygulamada `add` yeni bir iç içe eleman ekler →
    `[[7,8]]` → `[[7,8],[7,8]]` → … (büyüme).
  - **Senaryo (c) — `numbers` (`Set<Integer>`) null hedef → hiç doldurulmama (1.3, 2.3):** Bir grup
    `Integer` değer, önceden başlatılmamış (null) `numbers` (`Set<Integer>`) alanına
    `setValueByPath(entity, [CollectionEntity_.numbers], source)` ile atanır. Alanın düz doldurulmuş
    ve `Set` tipinde olması beklenir. **DÜZELTİLMEMİŞ karşı örnek:** null init koşulsuz
    `new ArrayList<>()` üretir; üretilen setter `(java.util.Set) value` cast'i ile
    `ClassCastException` fırlatır → alan atanmamış/`null` kalır (ya da CCE yayılır).
  - **Yaklaşım (Scoped PBT):** Deterministik yeniden üretim için özellik, somut kırılgan durumlara
    daraltılır (örn. sabit varyanslı değer kümesi + null hedef); ayrıca jqwik `@Property` ile
    rastgele varyanslı gruplar üretilir. Örneğin var olan `scenarioB`/`scenarioD`'nin alpha-string
    (varyanssız) sürümlerinin bu regresyonu YAKALAMADIĞINA dikkat edilir.
  - Testi DÜZELTİLMEMİŞ kodda çalıştır. **BEKLENEN SONUÇ**: Test BAŞARISIZ (bu doğrudur — hatanın
    varlığını kanıtlar).
  - Bulunan karşı örnekleri belgele (örn. `tags == [[7, 8]]`; ikinci uygulamada `size == 2`;
    `numbers == null`).
  - Test yazıldığında, çalıştırıldığında ve başarısızlık belgelendiğinde görevi tamamlanmış işaretle.
  - _Requirements: 1.1, 1.2, 1.3_

- [x] 2. Koruma (preservation) özellik testlerini yaz (düzeltmeden ÖNCE)
  - **Property 2: Preservation** - Doğrudan Atama Kapsamı Dışındaki Davranışların Korunması
  - **ÖNEMLİ**: Gözlem-önce (observation-first) metodolojisini izle — önce DÜZELTİLMEMİŞ kod (F)
    üzerinde ¬C girdilerinin (isBugCondition == false) gerçek çıktısını gözlemle, sonra bu davranışı
    yakalayan özellik-tabanlı testleri yaz.
  - **Neden PBT?** Koruma doğası gereği evrensel bir özelliktir ("tüm ¬C girdileri için F' = F");
    jqwik çok sayıda durumu otomatik üretir ve elle yazılan testlerin kaçırabileceği kenar durumları
    yakalar (design.md — Preservation Checking).
  - Test dosyası: `src/test/java/com/thy/fss/common/inmemory/engine/mapping/CollectionMappingNestingPreservationPropertyTest.java`
    (mevcut suite; nihai gereksinim numaralandırmasına — 3.1–3.7 — göre kapsam tamamlanır).
  - **¬C senaryoları (design.md — Preservation Requirements / Scope):**
    - **3.1 — Uyumlu düz atama (tek seviyeli):** Eleman tipleri hedefin bildirilen eleman tipiyle
      **zaten uyumlu** olan bir grup (örn. `List<String>` → `List<String>` alanı) düz atanır; sonuç
      düz kalır. (Bugün doğru çalışan durum — değişmemeli.)
    - **3.2 — Uyumlu düz atama (çok seviyeli yol):** İç içe nesne üzerinden erişilen koleksiyon
      hedefe (örn. `[ComplexNestedEntity_.level1, Level1_.items]`) uyumlu grup atanır; tek seviyeli
      durumla tutarlı biçimde düz kalır.
    - **3.3 — Tek eleman ekleme (append):** Tek bir skaler değer (ya da FIRST/LAST/ANY ile seçilmiş
      tek eleman) koleksiyon hedefe eklenir; `add` (append) davranışı korunur.
    - **3.4 — Skaler hedef:** Koleksiyon olmayan (skaler) alana çözümlenen değer doğrudan atanır.
    - **3.5 — Agregasyon:** SUM/AVG/COUNT/MIN/MAX sonuçları skaler hedefe değiştirilmeden yazılır.
    - **3.6 — Meşru iç içe:** Eleman tipi kendisi koleksiyon olan hedefte (`List<List<String>>`)
      tek iç liste tek eleman olarak eklenir → `[[...]]` korunur.
    - **3.7 — Null-`List` başlatma:** Düz listeyle uyumlu (`List`/`Collection`) null hedef,
      doldurulmadan önce başlatılır (initialize).
  - Testleri DÜZELTİLMEMİŞ kodda çalıştır. **BEKLENEN SONUÇ**: Testler GEÇER (korunacak baseline
    davranışı doğrular).
  - Testler yazıldığında, çalıştırıldığında ve DÜZELTİLMEMİŞ kodda geçtiğinde görevi tamamlanmış
    işaretle.
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

- [x] 3. Koleksiyonların doğrudan set edilmesi regresyonu için düzeltme

  - [x] 3.1 Somut koleksiyon tipini metadataya taşı (`CollectionAttribute`)
    - Dosya: `src/main/java/com/thy/fss/common/inmemory/specification/attribute/CollectionAttribute.java`
    - `Class<?> collectionType` alanı ekle ve `getCollectionType()` erişimcisini sağla.
    - Yeni yapıcı ekle: `CollectionAttribute(name, ownerType, elementType, collectionType)`.
    - **Geriye dönük uyum:** mevcut üç argümanlı yapıcıyı koru ve `collectionType`'ı
      `Collection.class` olarak varsay (elle yazılmış mevcut kullanımlar derlenmeye devam etsin).
    - `equals`/`hashCode`/`toString`'i `collectionType`'ı içerecek biçimde tutarlı güncelle. Not:
      eşleştirme sıcak yolu MetaAttribute'ları kimlik (`==`) ile karşılaştırdığından bu değişiklik
      sıcak yol davranışını etkilemez.
    - _Design: Fix Implementation §2 (Somut koleksiyon tipinin metadataya taşınması)_
    - _Requirements: 2.3_

  - [x] 3.2 Üreticinin bildirilen koleksiyon tipini yaymasını sağla (`StaticMetaModelGenerator`)
    - Dosya: `src/main/java/com/thy/fss/common/inmemory/processor/generator/StaticMetaModelGenerator.java`
    - Fonksiyon: `generateCollectionAttribute(...)`
    - Alanın ham (erased) bildirilmiş tipini (`java.util.List` / `java.util.Set` /
      `java.util.Collection`) `field.asType()`'tan elde et ve üretilen `CollectionAttribute<>`
      bildirimine **dördüncü argüman** olarak `.class` biçiminde yaz. Örnek çıktı:
      `new CollectionAttribute<>("numbers", CollectionEntity.class, Integer.class, java.util.Set.class);`
    - Yalnızca `List`, `Set`, `Collection` desteklendiğinden (bkz. `MetaModelValidator`) eşleme
      sınırlı ve güvenlidir. Mevcut metamodeller derleme sırasında yeniden üretilir.
    - _Design: Fix Implementation §2 (StaticMetaModelGenerator)_
    - _Requirements: 2.3_

  - [x] 3.3 Merkezi ayrıştırıcıyı düzelt (`BaseSpecificationService.assignToCollectionField`)
    - Dosya: `src/main/java/com/thy/fss/common/inmemory/specification/BaseSpecificationService.java`
    - Fonksiyon: `assignToCollectionField(container, collectionAttr, value, service)`
    - **(a) Düz atama kararını sağlamlaştır (1.1, 1.2):** Düz atama koşulundaki
      `&& allElementsAssignable((Collection<?>) value, elementType)` konjunktını **kaldır**. Böylece
      eleman tipi kendisi koleksiyon **olmayan** bir hedefe bir `Collection` değeri her zaman düz
      atanır (snapshot + `clear()` + `addAll()`; aliasing-güvenli ve idempotent). Meşru iç içe için
      gerekli koruma olan `!isAssignableFrom(elementType, value.getClass())` (eleman tipi koleksiyon
      DEĞİL) koşulu korunur; eleman tipi koleksiyon olan hedeflerde (3.6) `add` davranışı sürer.
    - **(b) Null başlatmayı tip-duyarlı yap (1.3, 2.3):** `collection == null` durumunda koşulsuz
      `new ArrayList<>()` yerine `createConcreteCollectionFor(collectionAttr)` kullan:
      `Set`'e uyumlu somut tip → `new LinkedHashSet<>()` (ekleme sırasını koruyarak deterministik ve
      idempotent), `List`/`Collection` → `new ArrayList<>()`, tip bilinmiyorsa güvenli varsayılan
      `new ArrayList<>()`. Somut tip bilgisi `CollectionAttribute.getCollectionType()`'tan (Task 3.1)
      okunur. Oluşturulan koleksiyon `service.setFieldValue(...)` ile geri yazılır.
    - _Design: Fix Implementation §1 (Merkezi ayrıştırıcının düzeltilmesi); Hypothesized Root Cause 1 & 2_
    - _Bug_Condition: isBugCondition(input) — value bir Collection, hedef COLLECTION, elementType != null,
      eleman tipi koleksiyon DEĞİL; triggersNesting (varyans) VEYA triggersNullDrop (null + List-olmayan somut tip)_
    - _Expected_Behavior: expectedBehavior(result) — düz (tek seviyeli) + sameElementsFlat + isIdempotent
      (clear+addAll) + concreteTypePreserved (List→List, Set→Set) + targetCollectionAfter != null_
    - _Preservation: design.md Preservation Requirements — skaler atama, tek eleman ekleme, meşru iç içe,
      agregasyon ve uyumlu düz atamalar (¬C) değişmez_
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 3.4 Bug koşulu keşif testinin artık GEÇTİĞİNİ doğrula + fix-checking özellik testlerini tamamla
    - **Property 1: Expected Behavior** - Düz + İdempotent + Tip-Koruyucu Atama (Fix Checking)
    - **ÖNEMLİ**: Task 1'deki AYNI keşif testini yeniden çalıştır — yeni bir test YAZMA amacı değil;
      test beklenen davranışı kodlar ve geçtiğinde fix'i doğrular.
    - Task 1'deki keşif senaryolarını (varyans+null → düz; tekrarlı → idempotent; `Set` null → düz +
      `Set`) DÜZELTİLMİŞ kodda çalıştır. **BEKLENEN SONUÇ**: Testler GEÇER (hata giderildi).
    - Fix-checking özellik testlerini, bug koşulu (C) sağlanan tüm girdileri kapsayacak biçimde
      tamamla/genişlet: rastgele koleksiyon içerikleri ve **eleman-tipi varyansı** üretilerek düz
      (iç içe değil) + kaynakla aynı elemanlar + idempotent (N kez uygulama boyut/​içeriği değiştirmez)
      + somut tip korunmuş (`List`→`List`, `Set`→`Set`; `numbers` için `isInstanceOf(Set.class)`)
      doğrulanır. Tek/çok seviyeli yol ve `ALL` seçici (`setValueByPathWithCollections`) yolları da
      kapsanır.
    - Test dosyası: `src/test/java/com/thy/fss/common/inmemory/engine/mapping/CollectionMappingNestingFixCheckingPropertyTest.java`
    - _Design: Correctness Properties — Property 1; Testing Strategy — Fix Checking_
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 3.5 Koruma testlerinin hâlâ GEÇTİĞİNİ doğrula (regresyon yok)
    - **Property 2: Preservation** - Doğrudan Atama Kapsamı Dışındaki Davranışların Korunması
    - **ÖNEMLİ**: Task 2'deki AYNI testleri yeniden çalıştır — yeni test YAZMA.
    - Task 2'deki koruma özellik testlerini (3.1–3.7) DÜZELTİLMİŞ kodda çalıştır.
    - **BEKLENEN SONUÇ**: Testler GEÇER (F' = F; ¬C davranışları değişmemiş, hiçbir regresyon yok).
    - Ayrıca mevcut 42 `CollectionMappingNesting*` testinin (Example, Preservation, Integration,
      FaultCondition→artık Expected Behavior, FixChecking) tümünün geçtiğini teyit et.
    - _Design: Correctness Properties — Property 2; Testing Strategy — Preservation Checking_
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_

- [x] 4. Checkpoint - Tüm testlerin ve derlemenin geçtiğinden emin ol
  - Projenin Gradle derlemesini çalıştır: `./gradlew build` (annotation processor metamodelleri
    yeniden üretir; Task 3.2 çıktısının — dördüncü argüman — üretilen `_` sınıflarına yansıdığını
    doğrular). Gerekirse `./gradlew clean build` ile temiz derleme yap.
  - Test odaklı doğrulama için: `./gradlew test` (ilgili `CollectionMappingNesting*` suite'i dahil)
    ve gerekiyorsa `./gradlew checkAnnotationProcessor` ile üretici çıktısını denetle.
  - Tüm testlerin geçtiğini, keşif testinin artık GEÇTİĞİNİ (Property 1: Expected Behavior), koruma
    testlerinin hâlâ GEÇTİĞİNİ (Property 2: Preservation) ve derlemenin başarılı olduğunu teyit et.
  - Sorular veya beklenmeyen başarısızlıklar ortaya çıkarsa devam etmeden önce kullanıcıya danış.
