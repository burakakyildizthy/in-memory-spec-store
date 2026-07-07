# Collection Mapping Nesting Fix — Bugfix Tasarımı

## Overview (Genel Bakış)

Eşleştirme (mapping) motoru, bir hedef alan koleksiyon tipinde olduğunda, atanacak değeri her
koşulda "tek bir eleman" olarak koleksiyona `add` ediyor. Kaynak değerin kendisi bir koleksiyon
olduğunda (koleksiyondan koleksiyona atama), bu davranış tüm kaynak listesini hedef koleksiyonun
içine tek bir eleman olarak sarmalıyor ve `[[items]]` şeklinde iç içe (nested) bir yapı
oluşturuyor. Aynı eşleştirme her senkronizasyonda tekrar uygulandığında hedef koleksiyon
sınırsız büyüyor (idempotent değil).

Hatanın kök nedeni paylaşılan koleksiyon-atama mantığındadır:
`BaseSpecificationService.handleCollectionField(...)` her zaman `collection.add(elementToAdd)`
ile bitiyor; `BaseSpecificationService.addValueToCollection(...)` (ALL seçici için) her zaman
`targetCollection.add(value)` yapıyor. Bu mantık "tüm koleksiyonu set et" ile "tek eleman ekle"
durumlarını ayırt etmiyor.

`MappingApplicator.applyCollectionMapping(...)` içinde yalnızca tek seviyeli
(`targetPath.size() == 1` ve `AttributeType.COLLECTION`) MANY_TO_ONE_COLLECTION hedefleri için
kısmi bir geçici çözüm (workaround) var; bu dal `service.setFieldValue(...)` ile sarmalamayı
atlatıyor. Ancak çok seviyeli koleksiyon hedefleri ve koleksiyon-kaynaklı ONE_TO_ONE
eşleştirmeleri hâlâ `setValueByPath -> handleCollectionField` üzerinden hataya düşüyor.

Bu tasarımın stratejisi: düzeltmeyi **paylaşılan koleksiyon-atama mantığında merkezileştirmek**.
Böylece hem tek seviyeli hem çok seviyeli koleksiyon hedefleri, hem `setValueByPath` hem de
`setValueByPathWithCollections` (ALL seçici) yolları tek ve tutarlı bir davranış kazanır. Net bir
ayırt edici (discriminator) ile "tüm koleksiyonu düz olarak set et" ile "tek eleman ekle"
durumları birbirinden ayrılır, idempotentlik sağlanır ve mevcut doğru davranışlar korunur.
`MappingApplicator` içindeki kısmi workaround, paylaşılan mantık düzeltildikten sonra gereksiz
hale geleceği için kaldırılması önerilir (Fix Implementation bölümüne bakınız).

## Glossary (Sözlük)

- **Bug_Condition (C)**: Hatanın tetiklendiği koşul — hedef alan koleksiyon tipindeyken ve
  çözümlenen kaynak değer, hedefin eleman tipiyle uyumlu **bütün bir koleksiyon** iken, motorun
  koleksiyonun tamamını tek bir eleman olarak sarmalaması.
- **Property (P) / expectedBehavior**: C koşulunda beklenen doğru davranış — hedef koleksiyonun,
  kaynak koleksiyonun elemanlarını **düz (flat)** olarak, iç içe olmadan içermesi ve tekrarlanan
  uygulamalarda **idempotent** olması.
- **Preservation (Koruma)**: C sağlanmadığında (¬C) motorun mevcut davranışının değişmeden
  kalması — tek eleman ekleme, skaler atama, agregasyon atamaları, meşru koleksiyon-içinde-
  koleksiyon ekleme ve null koleksiyon başlatma.
- **F (orijinal fonksiyon)**: Düzeltme öncesi atama mantığı (her zaman `add`).
- **F' (düzeltilmiş fonksiyon)**: Düzeltme sonrası, ayırt edici içeren atama mantığı.
- **handleCollectionField(...)**: `BaseSpecificationService` içindeki, `setValueByPath` yolunun
  son eleman koleksiyon olduğunda çağırdığı metot. Koleksiyonu null ise başlatır ve değeri ekler.
  Kusurun birincil merkezi.
- **addValueToCollection(...)**: `BaseSpecificationService` içindeki, `setValueByPathWithCollections`
  yolunda ALL seçici için değeri koleksiyona ekleyen metot. Kusurun ikinci merkezi.
- **handleRegularFieldAssignment(...)**: Operasyonsuz son-eleman atamasını yapan, koleksiyon ise
  `handleCollectionField`'a yönlendiren metot.
- **applyCollectionMapping(...) / assignTargetValue(...)**: `MappingApplicator` içindeki
  eşleştirme uygulama metotları; ilki tek seviyeli koleksiyon hedefleri için kısmi workaround
  içerir.
- **CollectionAttribute.getElementType()**: Hedef koleksiyonun eleman tipini veren meta bilgi;
  ayırt edicinin "düz set" mi yoksa "eleman ekle" mi kararı için kullanılır.
- **elementType**: Hedef koleksiyonun bildirilen eleman tipi (örn. `String`, `OrderItem`,
  ya da meşru iç içe durumda `List`).

## Bug Details (Hata Detayları)

### Bug Condition

Hata, hedef alan koleksiyon tipindeyken (`AttributeType.COLLECTION`) ve çözümlenen kaynak değer,
hedef koleksiyonun eleman tipiyle uyumlu **bütün bir koleksiyon** iken ortaya çıkar. Bu durumda
paylaşılan atama mantığı (`handleCollectionField` veya `addValueToCollection`) kaynak koleksiyonu
tek bir eleman gibi ele alıp hedefin içine `add` eder; sonuç `[[...]]` şeklinde iç içe yapı olur.
Meşru koleksiyon-içinde-koleksiyon durumundan (hedefin eleman tipinin kendisinin bir koleksiyon
olduğu 3.4) ayırt edilmesi gerekir.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input = (targetCollectionAttr, resolvedSourceValue, currentTargetCollection)
  OUTPUT: boolean

  RETURN targetCollectionAttr.attributeType == COLLECTION
         AND resolvedSourceValue IS_A Collection
         // Kaynak koleksiyonun elemanları hedefin eleman tipiyle uyumlu:
         AND allElementsAssignable(resolvedSourceValue, elementTypeOf(targetCollectionAttr))
         // Meşru iç içe durumu hariç tut: değerin KENDISI hedefin eleman tipi değilse
         AND NOT isAssignable(elementTypeOf(targetCollectionAttr), resolvedSourceValue.getClass())
END FUNCTION
```

Yani kaynak değer bir koleksiyon **ve** onun elemanları hedefin eleman tipine uyuyor, ancak
değerin kendisi (bir bütün olarak) hedefin eleman tipi değil. Bu ayrım, `List<List<String>>`
gibi meşru iç içe hedeflerde tek bir iç listenin eklenmesini (3.4) hatalı biçimde düzleştirmeyi
önler.

### Examples

- **Order.items (List<OrderItem>) — koleksiyondan koleksiyona (ONE_TO_ONE, kaynak alan bir
  koleksiyon):** Kaynak siparişin `items` listesi `[i1, i2]` iken hedef `Order.items`'e atanıyor.
  - Beklenen: `items == [i1, i2]` (düz)
  - Gerçekleşen (kusur): `items == [[i1, i2]]` (tek elemanlı, iç içe)
- **CollectionEntity.tags (List<String>):** Kaynak `tags` `["a", "b"]` iken hedef `tags`'e
  atanıyor.
  - Beklenen: `tags == ["a", "b"]`
  - Gerçekleşen (kusur): `tags == [["a", "b"]]`
- **Tekrarlanan senkronizasyon (idempotentlik):** Aynı koleksiyondan koleksiyona eşleştirme iki
  kez uygulanıyor.
  - Beklenen: `tags == ["a", "b"]` (her iki uygulamada aynı)
  - Gerçekleşen (kusur): 1. uygulama `[["a","b"]]`, 2. uygulama `[["a","b"], ["a","b"]]` (sınırsız
    büyüme)
- **Çok seviyeli hedef (root.summary.tags gibi bir yol, son eleman koleksiyon):** Kaynak bir
  koleksiyon iken çok seviyeli yol üzerinden hedef koleksiyona atanıyor.
  - Beklenen: hedef koleksiyon kaynak elemanlarını düz içerir
  - Gerçekleşen (kusur): iç içe sarmalanır (tek seviyeli workaround bu yolu kapsamıyor)
- **Meşru iç içe durum (edge / korunmalı):** Hedef `List<List<String>>` ve kaynak tek bir iç
  liste `["a", "b"]` (bir eleman olarak).
  - Beklenen davranış: `add` — sonuç `[["a", "b"]]` (bu durumda iç içe yapı doğrudur, korunmalı)

## Expected Behavior (Beklenen Davranış)

### Preservation Requirements (Korunması Gereken Davranışlar)

**Unchanged Behaviors (Değişmemesi Gereken Davranışlar):**
- Kaynaktan tek bir eleman seçildiğinde (skaler alan veya FIRST/LAST/ANY ile seçilmiş tek eleman)
  ve koleksiyon hedefe atandığında, eleman koleksiyona eklenmeye (append) devam etmelidir. (3.1)
- Hedef alan koleksiyon olmayan (skaler) bir tip olduğunda, çözümlenen kaynak değer doğrudan
  alana atanmaya devam etmelidir. (3.2)
- Agregasyon eşleştirmesi (SUM, AVG, COUNT, MIN, MAX) skaler bir hedefe sayısal sonuç yazdığında,
  agregasyon sonucu değiştirilmeden atanmaya devam etmelidir. (3.3)
- Hedef koleksiyonun eleman tipi kendisi bir koleksiyon olduğunda ve kaynak tek bir iç koleksiyon
  elemanı olarak çözümlendiğinde, bu eleman hedef koleksiyona eklenmeye devam etmelidir (meşru
  koleksiyon-içinde-koleksiyon). (3.4)
- Hedef koleksiyon alanı atamadan önce null olduğunda, koleksiyon doldurulmadan önce başlatılmaya
  (initialize) devam etmelidir. (3.5)

**Scope (Kapsam):**
Bug koşulunu (C) sağlamayan tüm girişler bu düzeltmeden tamamen etkilenmemelidir. Buna dahil olan
durumlar:
- Koleksiyona tek eleman ekleme (skaler veya FIRST/LAST/ANY ile seçilmiş)
- Skaler (koleksiyon olmayan) hedefe atama
- Agregasyon sonuçlarının skaler hedefe atanması
- Meşru koleksiyon-içinde-koleksiyon (hedef eleman tipi bir koleksiyon) ekleme
- Null koleksiyon alanının başlatılması

> Not: C koşulunda beklenen asıl doğru davranış "Correctness Properties" bölümünde Property 1'de
> tanımlanır. Bu bölüm yalnızca **değişmemesi gereken** davranışlara odaklanır.

## Hypothesized Root Cause (Varsayılan Kök Neden)

Kod incelemesiyle doğrulanan kök neden analizi:

1. **Paylaşılan atama mantığı ayırt etmiyor**: `BaseSpecificationService.handleCollectionField(...)`
   metodu, atanacak değerin tek bir eleman mi yoksa bütün bir koleksiyon mu olduğuna bakmadan her
   zaman `collection.add(elementToAdd)` ile bitiyor. Kaynak bir koleksiyon olduğunda tüm liste tek
   eleman olarak sarmalanıyor (`[[...]]`).

2. **ALL seçici yolunda aynı kusur**: `BaseSpecificationService.addValueToCollection(...)`
   (`handleCollectionOperation` üzerinden ALL seçici için çağrılır) her zaman
   `targetCollection.add(value)` yapıyor; aynı sarmalama sorunu bu yolda da mevcut.

3. **İdempotentlik yok**: `add` tabanlı davranış, her senkronizasyonda mevcut koleksiyona yeni bir
   (iç içe) eleman eklediği için hedef koleksiyon tekrarlanan uygulamalarda sınırsız büyüyor
   (1.2 defekti).

4. **Kısmi/dağınık workaround**: `MappingApplicator.applyCollectionMapping(...)` yalnızca tek
   seviyeli MANY_TO_ONE_COLLECTION hedefleri için `setFieldValue(...)` ile sarmalamayı atlatıyor
   (kendi yorumu kusuru adlandırıyor: "collection.add(entireList) -> [[items]]"). Çok seviyeli
   koleksiyon hedefleri ve koleksiyon-kaynaklı ONE_TO_ONE eşleştirmeleri
   `assignTargetValue -> setValueByPath -> handleCollectionField` üzerinden hâlâ hataya düşüyor.
   Ayrıca bu workaround her zaman `new ArrayList<>(...)` ile set ettiği için `Set` tipli hedefler
   (örn. `CollectionEntity.numbers`) için tip uyumsuzluğu riski taşıyor.

**Sonuç**: Düzeltme tek bir noktaya (paylaşılan koleksiyon-atama yardımcı mantığına) konmalı; hem
`handleCollectionField` hem `addValueToCollection` bu ortak mantığı kullanmalı ki tüm yollar
(tek/çok seviyeli, operasyonlu/operasyonsuz) tutarlı davransın.

## Correctness Properties

Property 1: Bug Condition - Koleksiyondan Koleksiyona Düz (Flat) ve İdempotent Atama

_For any_ input where the bug condition holds (isBugCondition returns true) — yani hedef bir
koleksiyon, çözümlenen kaynak değer elemanları hedefin eleman tipiyle uyumlu bütün bir koleksiyon
ve hedefin eleman tipi kendisi bir koleksiyon değil — the fixed function SHALL hedef koleksiyonu
kaynak koleksiyonun elemanlarını **tam olarak tek seviyede (düz, iç içe olmadan)** içerecek
biçimde ayarlamalı ve tekrarlanan uygulamalarda **idempotent** olmalıdır (hedef sınırsız büyümez,
her zaman güncel kaynağı yansıtır). Bu davranış tek seviyeli ve çok seviyeli koleksiyon hedefleri
için tutarlıdır.

**Validates: Requirements 2.1, 2.2, 2.3**

Property 2: Preservation - Koleksiyon-Olmayan ve Tek-Eleman Girişlerinin Korunması

_For any_ input where the bug condition does NOT hold (isBugCondition returns false) — tek eleman
koleksiyona ekleme, skaler hedefe atama, agregasyon sayısal sonucunun skaler hedefe atanması,
meşru koleksiyon-içinde-koleksiyon eleman ekleme ve null koleksiyonun başlatılması dahil — the
fixed function SHALL orijinal fonksiyonla **aynı sonucu** üretmeli; koleksiyona ekleme, skaler
atama, agregasyon ve başlatma davranışlarını korumalıdır.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**

**expectedBehavior (Property 1 için formal doğrulama):**
```
FUNCTION expectedBehavior(targetCollectionAfter, sourceCollection)
  INPUT: targetCollectionAfter — atama sonrası hedef koleksiyon
         sourceCollection      — çözümlenen kaynak koleksiyon
  OUTPUT: boolean

  // 1) Düz (flat): boyut ve elemanlar birebir eşleşir, iç içe eleman yok
  flat := (targetCollectionAfter.size() == sourceCollection.size())
          AND (FOR ALL i: targetCollectionAfter[i] EQUALS sourceCollection[i])
          AND (NOT anyElementIsWrappedCollection(targetCollectionAfter, sourceCollection))

  RETURN flat
END FUNCTION

// İdempotentlik: F'(F'(state)) EQUALS F'(state)
FUNCTION isIdempotent(applyOnce, applyTwice)
  RETURN applyOnce EQUALS applyTwice
END FUNCTION
```

## Fix Implementation (Düzeltme Uygulaması)

Düzeltme, root-cause analizi doğru kabul edilerek paylaşılan koleksiyon-atama mantığında
merkezileştirilir.

### Changes Required

**File**: `src/main/java/com/thy/fss/common/inmemory/specification/BaseSpecificationService.java`

**Function**: yeni ortak yardımcı + `handleCollectionField(...)` ve `addValueToCollection(...)`
metotlarının bu yardımcıya yönlendirilmesi.

**Specific Changes**:

1. **Ortak ayırt edici yardımcı ekle**: Yeni bir özel metot, örn.
   `assignToCollectionField(container, collectionAttr, value, service)`. Bu metot "tüm koleksiyonu
   düz set et" ile "tek eleman ekle" kararını verir:
   ```
   FUNCTION assignToCollectionField(container, collectionAttr, value, service)
     collection := service.getFieldValue(container, collectionAttr)

     // 3.5: null ise başlat (mevcut davranış korunur)
     IF collection == null THEN
        collection := new ArrayList()
        service.setFieldValue(container, collectionAttr, collection)
     END IF

     elementType := elementTypeOf(collectionAttr)   // CollectionAttribute.getElementType()

     IF value IS_A Collection
        AND elementType != null
        // Meşru iç içe (3.4) önce kontrol edilir: değerin KENDISI geçerli bir eleman mı?
        AND NOT isAssignable(elementType, value.getClass())
        // Kaynak koleksiyonun elemanları hedefin eleman tipine uyuyor mu?
        AND allElementsAssignable((Collection) value, elementType)
     THEN
        // C koşulu: düz (flat) + idempotent atama
        snapshot := new ArrayList((Collection) value)   // aliasing'e karşı güvenli
        collection.clear()                              // idempotentlik: içeriği değiştir, ekleme
        collection.addAll(snapshot)
     ELSE
        // ¬C: tek eleman / skaler / meşru iç içe -> mevcut ekleme davranışı
        elementToAdd := resolveElementToAdd(value, collectionAttr) // null-model örneği üretimi korunur
        collection.add(elementToAdd)
     END IF
   END FUNCTION
   ```
   - `collection.clear()` + `addAll(...)` mevcut koleksiyon örneğini yerinde günceller; bu sayede
     hedefin somut tipi (`List`/`Set`) korunur ve **idempotentlik** sağlanır (2.2).
   - `snapshot` ile önce kaynak elemanları kopyalanır; kaynak ve hedef aynı örnek olsa bile (aliasing)
     `clear()` veri kaybına yol açmaz.

2. **`handleCollectionField(...)` metodunu yönlendir**: Metodun sonundaki
   `collection.add(elementToAdd)` çağrısını kaldırıp, mevcut null-başlatma ve `elementType`/
   null-model örneği üretim mantığını `assignToCollectionField(...)` içine taşıyarak bu ortak
   yardımcıya delege et. Böylece `setValueByPath` yolu (ONE_TO_ONE koleksiyon-kaynak ve çok
   seviyeli koleksiyon hedefleri) düzeltmeden yararlanır.

3. **`addValueToCollection(...)` metodunu yönlendir**: `targetCollection.add(value)` yerine aynı
   ortak ayırt edici mantığı uygula (ALL seçici yolu — `handleCollectionOperation` üzerinden).
   Böylece `setValueByPathWithCollections` yolu da tutarlı olur.

4. **Yardımcı metotlar ekle**:
   - `elementTypeOf(collectionAttr)`: `collectionAttr instanceof CollectionAttribute` ise
     `getElementType()` döndürür, aksi halde `null`.
   - `allElementsAssignable(collection, elementType)`: koleksiyondaki null-olmayan tüm elemanların
     `isAssignableFrom(elementType, el.getClass())` koşulunu sağladığını doğrular (boş koleksiyonda
     `true`).
   - Mevcut `isAssignableFrom(...)` yeniden kullanılır (primitive/wrapper uyumu dahil).
   - `resolveElementToAdd(value, collectionAttr)`: mevcut `handleCollectionField` içindeki
     "eleman tipi model ve değer null ise yeni örnek üret" mantığını kapsüller (3.5 ve mevcut
     davranış korunur).

5. **Eleman tipi bilinmiyorsa muhafazakâr davran**: `elementType == null` (attr bir
   `CollectionAttribute` değilse) durumunda düz-set yapılmaz; mevcut `add` davranışı korunur. Üretilen
   MetaModel koleksiyon attribute'ları `CollectionAttribute` olduğundan birincil hata senaryoları
   (Order.items, CollectionEntity.tags/numbers) kapsanır. Bu, bilinmeyen tip durumunda regresyon
   riskini ortadan kaldırır (dokümante edilmiş bilinçli sınırlama).

**File**: `src/main/java/com/thy/fss/common/inmemory/engine/mapping/MappingApplicator.java`

**Function**: `applyCollectionMapping(...)`

6. **Kısmi workaround'u kaldır (önerilen)**: Paylaşılan mantık düzeltildikten sonra
   `targetPath.size() == 1 && AttributeType.COLLECTION` özel dalı gereksizleşir. Bu dal kaldırılıp
   tüm koleksiyon hedefleri (tek/çok seviyeli) `assignTargetValue(rootEntity, mapping, valuesToSet)`
   üzerinden tek koda yönlendirilmelidir. Faydalar:
   - Tek seviyeli ve çok seviyeli koleksiyon hedefleri **tutarlı** davranır (2.3).
   - `clear()+addAll()` mevcut örneği koruduğu için `Set` tipli hedefler (`CollectionEntity.numbers`)
     de doğru çalışır (workaround'un `new ArrayList<>(...)` set etmesindeki tip uyumsuzluğu riski
     ortadan kalkar).
   - Tek bir kod yolu, bakım ve okunabilirlik açısından daha temiz.

   > Karar: Bu kaldırma işlemi, testlerle (özellikle tek seviyeli MANY_TO_ONE_COLLECTION için
   > fix-checking ve preservation) doğrulanmadan yapılmamalıdır. Eğer testler tek seviyeli yolda
   > herhangi bir davranış farkı ortaya çıkarırsa, workaround geçici olarak korunup ortak mantığa
   > delege edecek şekilde genelleştirilir (fallback planı).

### Not Etkilenmeyen Alanlar

- `MappingApplicator.convertValueToTargetType(...)`: Koleksiyon değerinde hedef tip `List`/`Set`
  olduğundan `targetType.isAssignableFrom(value.getClass())` doğru döner ve değer olduğu gibi
  geçer; sayısal dönüşümler koleksiyonları etkilemez. Değişiklik gerekmez.
- Skaler atama (`setFieldValue`), agregasyon yolu ve tip-uyumsuzluğu doğrulaması (mevcut
  `IllegalArgumentException`) değişmez.

## Testing Strategy (Test Stratejisi)

### Validation Approach

İki aşamalı yaklaşım izlenir: önce hatayı düzeltilmemiş kod üzerinde gösteren karşı örnekler
(counterexample) yüzeye çıkarılır (exploration), ardından düzeltmenin doğru çalıştığı (fix
checking) ve mevcut davranışın korunduğu (preservation) doğrulanır. Testler jqwik ile
property-based olarak yazılır ve proje konvansiyonuna uyar (`net.jqwik.api.*`, `@Property`,
`@ForAll`, `@Provide`, `@Example`; AssertJ `assertThat`). Test fixture'ları:
`CollectionEntity` (`List<String> tags`, `Set<Integer> numbers`), `MixedCollectionEntity`,
`Order` (`List<OrderItem> items`).

Önerilen test sınıfı adları (proje konvansiyonu ile uyumlu):
- `CollectionMappingNestingFaultConditionPropertyTest` (exploration)
- `CollectionMappingNestingFixCheckingPropertyTest` (fix checking)
- `CollectionMappingNestingPreservationPropertyTest` (preservation)
- `CollectionMappingNestingExampleTest` (somut birim örnekleri)

### Exploratory Bug Condition Checking

**Goal**: Düzeltmeyi uygulamadan ÖNCE hatayı gösteren karşı örnekleri yüzeye çıkarmak; kök neden
analizini doğrulamak. Testler DÜZELTİLMEMİŞ kod üzerinde çalıştırılır ve başarısız olması
(iç içe yapı gözlemlenmesi) beklenir.

**Test Plan**: `handleCollectionField` ve `addValueToCollection` yollarını tetikleyen atamalar
kurgulanır; hedef koleksiyona bütün bir kaynak koleksiyon atanır ve sonucun düz olması
"assert" edilir (düzeltilmemiş kodda başarısız olur).

**Test Cases**:
1. **ONE_TO_ONE koleksiyon-kaynak (Order.items)**: Kaynak `List<OrderItem>` -> `Order.items`;
   `items` düz `[i1, i2]` beklenir (düzeltilmemişte `[[i1,i2]]` — başarısız).
2. **Basit tip koleksiyonu (CollectionEntity.tags)**: `List<String>` atanır; düz `["a","b"]`
   beklenir (düzeltilmemişte `[["a","b"]]` — başarısız).
3. **Çok seviyeli hedef**: Son elemanı koleksiyon olan çok seviyeli bir yola koleksiyon atanır;
   düz sonuç beklenir (workaround kapsamadığı için düzeltilmemişte başarısız).
4. **İdempotentlik (1.2)**: Aynı koleksiyondan koleksiyona eşleştirme iki kez uygulanır; boyutun
   sabit kaldığı "assert" edilir (düzeltilmemişte büyür — başarısız).

**Expected Counterexamples**:
- Hedef koleksiyonun tek bir elemanı, kaynağın tamamını içeren iç içe bir koleksiyondur (`[[...]]`).
- Tekrarlı uygulamada boyut her seferinde artar (sınırsız büyüme).

### Fix Checking

**Goal**: Bug koşulunu sağlayan tüm girişler için düzeltilmiş fonksiyonun beklenen davranışı
(düz + idempotent) ürettiğini doğrulamak.

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  applyOnce  := applyMapping_fixed(input)
  applyTwice := applyMapping_fixed(applyOnce)   // tekrar uygula
  ASSERT expectedBehavior(applyOnce.targetCollection, input.sourceCollection)
  ASSERT isIdempotent(applyOnce.targetCollection, applyTwice.targetCollection)
END FOR
```

### Preservation Checking

**Goal**: Bug koşulunu sağlamayan tüm girişler için düzeltilmiş fonksiyonun orijinal fonksiyonla
aynı sonucu ürettiğini doğrulamak.

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT applyMapping_original(input) EQUALS applyMapping_fixed(input)
END FOR
```

**Testing Approach**: Preservation için property-based test önerilir çünkü:
- Giriş uzayında çok sayıda durumu otomatik üretir.
- Manuel birim testlerin kaçırabileceği kenar durumları yakalar.
- Bug-dışı tüm girişler için davranışın değişmediğine dair güçlü garanti sağlar.

**Test Plan**: Bug-dışı girişlerin davranışı önce DÜZELTİLMEMİŞ kodda gözlemlenir, ardından bu
davranışı yakalayan property-based testler yazılır ve düzeltme sonrası da geçtiği doğrulanır.

**Test Cases**:
1. **Tek eleman ekleme (3.1)**: FIRST/LAST/ANY veya skaler ile seçilmiş tek eleman koleksiyona
   ekleniyor; ekleme (append) davranışının korunduğu doğrulanır.
2. **Skaler hedef (3.2)**: Koleksiyon olmayan alana çözümlenen değer doğrudan atanıyor; korunur.
3. **Agregasyon (3.3)**: SUM/AVG/COUNT/MIN/MAX skaler hedefe yazılıyor; sonucun değişmeden
   atandığı doğrulanır.
4. **Meşru iç içe (3.4)**: Hedef eleman tipi bir koleksiyon (`List<List<String>>`) iken tek bir
   iç liste ekleniyor; `add` davranışının korunduğu (sonuç `[[...]]`) doğrulanır.
5. **Null koleksiyon başlatma (3.5)**: Null koleksiyon alanı önce başlatılıp sonra dolduruluyor;
   başlatma davranışının korunduğu doğrulanır.

### Unit Tests

- Her yol için (setValueByPath ve setValueByPathWithCollections/ALL) koleksiyondan koleksiyona
  atamanın düz sonuç ürettiğini doğrulayan somut örnekler.
- `List<String>` (tags), `Set<Integer>` (numbers) ve model koleksiyonu (`Order.items`) için ayrı
  birim testleri.
- Kenar durumlar: boş kaynak koleksiyon (hedef boşalır, idempotent), null hedef (başlatılır),
  meşru iç içe (`List<List<...>>`).

### Property-Based Tests

- Rastgele `List<String>` / `Set<Integer>` üretip düz atama ve idempotentlik (iki kez uygulama)
  özelliklerini doğrula.
- Rastgele tek elemanlar üretip koleksiyona ekleme davranışının korunduğunu doğrula.
- Rastgele skaler değerler üretip doğrudan atamanın korunduğunu doğrula.
- Rastgele meşru iç içe girişler (`List<List<...>>`) üretip `add` davranışının korunduğunu doğrula.

### Integration Tests

- Tam senkronizasyon akışında koleksiyondan koleksiyona eşleştirmenin düz sonuç ürettiğini,
  tek seviyeli ve çok seviyeli hedeflerde tutarlı davrandığını doğrula.
- Tekrarlanan senkronizasyon (birden çok sync döngüsü) sonrası hedef koleksiyonun büyümediğini
  (idempotent) doğrula.
- `MappingApplicator` workaround'u kaldırıldıktan sonra tek seviyeli MANY_TO_ONE_COLLECTION
  hedeflerinin (List ve Set) regresyonsuz çalıştığını doğrula.
