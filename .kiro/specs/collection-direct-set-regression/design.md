# Koleksiyonların Doğrudan Set Edilmesi Regresyonu — Bugfix Tasarımı

## Overview

Bu tasarım, eşleştirme (mapping) motorunda **koleksiyondan koleksiyona doğrudan atama**
(bir grup toplanan değerin koleksiyon tipindeki bir hedef alana düz elemanlar olarak yazılması)
davranışının bozulmasını gideren düzeltmeyi tanımlar.

Regresyon, `f568886` (collection-mapping-nesting-fix) commit'i ile geldi. Bu commit,
`MappingApplicator.applyCollectionMapping` içindeki tek seviyeli koleksiyon hedefleri için
kullanılan özel "doğrudan set" kısayolunu kaldırdı:

```java
// Kaldırılan kısayol (f568886 öncesi):
List<MetaAttribute<?, ?>> targetPath = mapping.getTargetPath();
if (targetPath.size() == 1 && targetPath.get(0).getAttributeType() == AttributeType.COLLECTION) {
    service.setFieldValue(rootEntity, targetPath.get(0), new ArrayList<>(valuesToSet));
} else {
    assignTargetValue(rootEntity, mapping, valuesToSet);
}
```

Kısayolun kaldırılmasıyla artık tüm koleksiyon eşleştirmeleri ortak ayrıştırıcıdan
(`BaseSpecificationService.assignToCollectionField`) geçiyor. Bu ayrıştırıcı, yalnızca
`allElementsAssignable(values, elementType)` koşulu sağlandığında düz atama (clear + addAll)
yapıyor; sağlanmadığında ise tüm listeyi tek eleman olarak ekleyerek (`collection.add(wholeList)`)
**iç içe (nested)** yapı üretiyor. Ayrıca `applyCollectionMapping` çağrısı
`applyMappingsToEntity` içindeki `try/catch` ile sarmalandığı için bu yoldaki istisnalar
sessizce yutuluyor ve hata gözlemlenemiyor.

Düzeltme stratejisi, kaldırılan kısayolu geri eklemek **değildir**. Kısayol yalnızca tek seviyeli
`List` hedeflerini kurtarıyordu; çok seviyeli yolları, `ALL` seçici yolunu ve `Set` gibi düz
liste olmayan hedefleri kapsamıyordu (nitekim `Set`/null durumu kısayol varken de bozuktu).
Bunun yerine kök nedeni merkezi ayrıştırıcıda çözüyoruz: (1) düz atama kararını eleman tipinin
kendisinin bir koleksiyon olup olmamasına dayandırıyoruz (eleman tipi varyansı artık iç içe
sarmalamaya düşmüyor), ve (2) null hedef koleksiyonu, bildirilen somut tiple (List/Set) uyumlu
bir örnekle başlatıyoruz. Böylece tek/çok seviyeli tüm yollar, `List` ve `Set` hedefler tek
noktadan tutarlı biçimde düzeltilir.

## Glossary

- **Bug_Condition (C)**: Hatayı tetikleyen girdi kümesi — bir `Collection` değerinin, eleman tipi
  kendisi bir koleksiyon **olmayan** (skaler/model) bir koleksiyon hedef alanına atandığı durum
  (yani "düz atama beklenen" durum). Mevcut kodun bu kümede yanlış sonuç ürettiği iki tetikleyici:
  eleman tipi varyansı (iç içe sarmalama) ve null + `List` olmayan somut tip (hiç doldurulmama).
- **Property (P)**: C sağlandığında istenen doğru davranış — toplanan değerlerin hedef koleksiyona
  düz (tek seviyeli) elemanlar olarak yazılması, somut koleksiyon tipinin (List/Set) korunması ve
  tekrarlı uygulamalarda idempotent kalınması (sınırsız büyüme olmaması).
- **Preservation (Koruma)**: C dışındaki (¬C) girdilerde davranışın düzeltmeden önceki (orijinal)
  davranışla birebir aynı kalması.
- **Doğrudan koleksiyon set etme / koleksiyondan koleksiyona atama**: Toplanan bir grup değerin
  (`valuesToSet`) koleksiyon tipindeki bir hedef alana, elemanları tek tek yerleştirilecek şekilde
  atanması (MANY_TO_ONE_COLLECTION eşleştirmesi).
- **assignToCollectionField**: `BaseSpecificationService` içindeki, koleksiyon alanına atamada
  "düz atama (C)" ile "tek eleman ekle (¬C)" arasında karar veren merkezi ayrıştırıcı.
- **allElementsAssignable(values, elementType)**: `values` içindeki tüm (null olmayan) elemanların
  `elementType`'a atanabilir olup olmadığını denetleyen yardımcı. Regresyonun tetikleyicisi olan
  aşırı-katı koşul.
- **isAssignableFrom(target, source)**: `source` tipindeki bir değerin `target` tipindeki bir alana
  atanabilirliğini (ilkel/kutulu uyumları dahil) denetleyen yardımcı.
- **elementType**: Koleksiyon hedefin bildirilen eleman tipi (`CollectionAttribute.getElementType()`),
  örn. `List<String>` için `String`, `Set<Integer>` için `Integer`, `List<List<String>>` için `List`.
- **Somut koleksiyon tipi (collectionType)**: Hedef alanın bildirilen koleksiyon arayüzü/tipi
  (`List`, `Set` veya `Collection`). `CollectionAttribute` bugün bu bilgiyi taşımaz.
- **Meşru iç içe (legitimate nesting)**: Eleman tipinin kendisinin bir koleksiyon olduğu durum
  (örn. `List<List<String>>`); tek bir iç koleksiyonun hedefe tek eleman olarak eklenmesi doğru
  davranıştır (Requirement 3.6).
- **Idempotency**: Aynı eşleştirmenin aynı hedefe tekrar uygulanmasının sonucu değiştirmemesi.
- **F / F'**: Orijinal (düzeltilmemiş) fonksiyon / düzeltilmiş fonksiyon.

## Bug Details

### Bug Condition

Hata, bir `Collection` değeri (`valuesToSet`), eleman tipi kendisi bir koleksiyon **olmayan**
bir koleksiyon hedef alanına atandığında ortaya çıkar (bu, "düz atama beklenen" alandır).
`assignToCollectionField` bu alanda iki durumda yanlış davranır:

1. **Eleman tipi varyansı → iç içe sarmalama (1.1, 1.2):** Toplanan değerlerin çalışma-zamanı tipleri
   hedefin bildirilen eleman tipiyle birebir atanabilir değilse, `allElementsAssignable(...)` `false`
   döner ve karar `else` dalına düşer: `collection.add(valuesToSet)`. Böylece tüm grup tek eleman
   olarak sarmalanır (`[[...]]`). Tekrarlı uygulamada `add` çağrısı her seferinde yeni bir iç içe
   eleman ekler → hedef sınırsız büyür.

2. **Null + `List` olmayan somut tip → hiç doldurulmama (1.3):** Hedef koleksiyon null ise
   `assignToCollectionField` onu koşulsuz olarak `new ArrayList<>()` ile başlatıp `setFieldValue`
   ile geri yazar. Üretilen setter, alanın bildirilen tipine cast eder (örn. `Set` alanı için
   `entity.setNumbers((java.util.Set) value)`). Bir `ArrayList`, `Set` olmadığından bu cast
   `ClassCastException` fırlatır; istisna `applyMappingsToEntity` içindeki `try/catch` tarafından
   yutulur ve alan null kalır.

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input = (values, targetField, currentValue)
         // values      : atanacak toplanan grup
         // targetField : hedef alan (MetaAttribute)
         // currentValue : hedef alanın atama öncesi mevcut değeri
  OUTPUT: boolean

  // Yalnızca "düz atama beklenen" koleksiyon hedefleri ilgi alanımızdadır:
  isFlatSetDomain :=
        values IS A Collection
    AND targetField.attributeType == COLLECTION
    AND elementTypeOf(targetField) != null
    AND NOT isCollectionType(elementTypeOf(targetField))   // eleman tipi koleksiyon DEĞİL

  IF NOT isFlatSetDomain THEN
    RETURN false
  END IF

  // Bu alanda mevcut (düzeltilmemiş) kodun yanlış sonuç ürettiği iki tetikleyici:
  triggersNesting  := NOT allElementsAssignable(values, elementTypeOf(targetField))  // 1.1, 1.2
  triggersNullDrop := (currentValue == null)
                      AND NOT isListCompatible(concreteTypeOf(targetField))           // 1.3

  RETURN triggersNesting OR triggersNullDrop
END FUNCTION
```

> Not: `isFlatSetDomain` doğru olup her iki tetikleyici de yanlış olduğunda (eleman tipleri uyumlu
> ve hedef `List`/null-List) mevcut kod zaten doğru çalışır; bu girdiler ¬C kapsamındadır ve
> korunması gereken davranışlardır (bkz. Requirements 3.1, 3.2, 3.7).

### Examples

- **`tags` (`List<String>`), hedef null, eleman tipi varyansı:** Toplanan grup, bildirilen eleman
  tipiyle birebir atanamayan değerler içerdiğinde (örn. `[7, 8]`) → beklenen düz `[7, 8]` yerine
  iç içe `[[7, 8]]` (tek elemanlı) üretiliyor. (İnceleme sırasında yeniden üretildi.)
- **Aynı eşleştirmenin tekrarı (`tags`):** İkinci senkronizasyonda `[[7, 8], [7, 8]]`, üçüncüde üç
  elemanlı… hedef koleksiyon her uygulamada büyüyor (idempotent değil).
- **`numbers` (`Set<Integer>`), hedef null:** Atama sonrası alan `null` kalıyor (hiç doldurulmuyor);
  çünkü null init `new ArrayList<>()` üretip `Set` alanına yazmaya çalışıyor ve `ClassCastException`
  sessizce yutuluyor. (İnceleme sırasında yeniden üretildi.)
- **Kontrollü deney:** Eski kısayol (`new ArrayList<>(valuesToSet)`) geri konduğunda `tags`
  senaryosu düz sonuç üretti (regresyon doğrulandı); `numbers` (`Set`) senaryosu yine başarısız
  oldu (önceden var olan, ayrı bir kusur — `ArrayList`'in `Set` alanına yazılması).
- **Meşru iç içe (`List<List<String>>`), beklenen davranış:** Tek bir iç liste `["a","b","c"]`
  eklendiğinde sonuç `[["a","b","c"]]` olmalı (eleman tipi koleksiyon olduğundan add davranışı
  doğrudur — bu bir hata değildir, korunmalıdır).

## Expected Behavior

### Preservation Requirements

**Değişmemesi gereken davranışlar (Unchanged Behaviors):**
- Skaler (koleksiyon olmayan) hedef alana atama, çözümlenen değeri doğrudan alana yazmaya devam
  etmeli (3.4).
- Kaynaktan tek eleman seçildiğinde (skaler ya da FIRST/LAST/ANY ile) koleksiyon hedefe **ekleme
  (append)** davranışı korunmalı (3.3).
- Eleman tipi kendisi bir koleksiyon olan hedeflerde (örn. `List<List<String>>`) tek iç
  koleksiyonun tek eleman olarak eklenmesi (meşru iç içe) korunmalı (3.6).
- Agregasyon (SUM, AVG, COUNT, MIN, MAX) sonuçlarının skaler hedefe değiştirilmeden yazılması
  korunmalı (3.5).
- Eleman tipleri uyumlu olan koleksiyondan koleksiyona atamaların (bugün doğru çalışan durum) düz
  kalması korunmalı (3.1); çok seviyeli yol üzerinden erişilen koleksiyon hedeflerinin de tek
  seviyeli durumla tutarlı biçimde düz kalması korunmalı (3.2).
- Düz listeyle uyumlu (`List`/`Collection`) null hedef alanların atamadan önce başlatılması
  korunmalı (3.7).

**Kapsam (Scope):**
Aşağıdaki girdiler bu düzeltmeden **etkilenmemelidir** (F' = F):
- Skaler hedef alanlara yapılan tüm atamalar.
- Koleksiyon hedefe tek eleman (skaler) atamaları.
- Eleman tipi koleksiyon olan hedeflere meşru iç içe atamalar.
- Skaler hedefe agregasyon sonucu atamaları.
- Eleman tipleri hedefin bildirilen eleman tipiyle **zaten uyumlu** olan düz atamalar
  (tek veya çok seviyeli, hedef null-`List` dahil).

> Doğru davranışın kendisi (düz + idempotent + tip-koruyucu) Correctness Properties bölümündeki
> Property 1'de tanımlıdır. Bu bölüm yalnızca **değişmemesi gerekenleri** vurgular.

## Hypothesized Root Cause

Bug analizine dayanarak en olası nedenler:

1. **Aşırı-katı düz atama koşulu (birincil, regresyon):** `assignToCollectionField` içindeki
   ayrıştırıcı, düz atamayı yalnızca `allElementsAssignable(values, elementType)` doğruyken yapıyor.
   Eleman tipi varyansında bu koşul `false` döner ve karar `else` dalına (`collection.add(value)`)
   düşerek tüm grubu tek eleman olarak sarmalar (iç içe). Oysa eleman tipinin kendisi bir koleksiyon
   **değilse**, bir `Collection` değeri her zaman düz atanmalıdır — atanabilirlik koşulu iç içe
   sarmalamaya geri düşüş için bir gerekçe olmamalıdır. Bu, 1.1 ve 1.2'nin (nesting + sınırsız
   büyüme) kök nedenidir.

2. **Tip-duyarsız null başlatma (birincil, önceden var olan kusur):** Null hedef koleksiyon
   koşulsuz `new ArrayList<>()` ile başlatılıyor. Alan `Set` (veya `List` olmayan başka bir tip)
   ise üretilen setter'ın cast'i (`(java.util.Set) value`) `ClassCastException` fırlatır. Bu,
   1.3/2.3'ün (Set/null hedefin hiç doldurulmaması) kök nedenidir. Somut koleksiyon tipini korumak
   için hedefin bildirilen tipini (List/Set/Collection) bilmek gerekir; ancak `CollectionAttribute`
   bugün yalnızca eleman tipini taşır, somut koleksiyon tipini taşımaz.

3. **Sessiz istisna yutulması (katkıda bulunan etken):** `applyMappingsToEntity`, her COLLECTION
   eşleştirmesini `try/catch` ile sarıp yalnızca WARN loglar. Bu nedenle 1.3'teki
   `ClassCastException` gözlemlenmeden yutulur ve hata "sessiz" olur. Düzeltme, istisnanın kaynağını
   ortadan kaldırarak bu belirtiyi giderir (yutma mantığı işlevsel olarak değiştirilmez).

4. **Yerelleştirilmemiş kısayolun kaldırılması (tetikleyici olay):** `f568886`, tek seviyeli
   `List` hedefleri kurtaran özel kısayolu kaldırıp her şeyi merkezi ayrıştırıcıya yönlendirdi.
   Kısayol zaten kısmi bir çözümdü (çok seviyeli yolları, `ALL` seçici yolunu ve `Set` hedefleri
   kapsamıyordu). Doğru düzeltme, kısayolu geri koymak değil, merkezi ayrıştırıcıyı sağlam hale
   getirmektir.

## Correctness Properties

Property 1: Bug Condition - Doğrudan koleksiyon atama düz, idempotent ve tip-koruyucu olmalı

_For any_ input where the bug condition holds (isBugCondition returns true) — yani bir `Collection`
değeri, eleman tipi kendisi bir koleksiyon olmayan bir koleksiyon hedef alanına atandığında — the
fixed function SHALL toplanan değerleri hedef koleksiyonun düz (tek seviyeli) elemanları olarak
yerleştirmeli (tüm grubu tek eleman olarak sarmalamamalı), hedef alanın somut koleksiyon tipini
(List/Set) korumalı ve tekrarlı uygulamalarda idempotent kalmalı (hedef koleksiyon sınırsız
büyümeden güncel kaynağı yansıtmalı).

**Validates: Requirements 2.1, 2.2, 2.3**

Property 2: Preservation - Doğrudan atama kapsamı dışındaki davranışlar değişmemeli

_For any_ input where the bug condition does NOT hold (isBugCondition returns false) — skaler
hedefe atama; koleksiyon hedefe tek eleman ekleme; eleman tipi koleksiyon olan hedeflere meşru iç
içe atama; skaler hedefe agregasyon sonucu; ve eleman tipleri zaten uyumlu olan (tek/çok seviyeli,
null-`List` dahil) düz atamalar — the fixed function SHALL produce the same result as the original
function, preserving mevcut ekleme (append), skaler atama, agregasyon, meşru iç içe ve uyumlu düz
atama davranışlarını.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7**

**expectedBehavior (P) — düz atama alanı (C) için beklenen sonuç:**
```
FUNCTION expectedBehavior(result)
  INPUT: result = (targetCollectionAfter, targetConcreteTypeAfter)
  OUTPUT: boolean

  RETURN
        // Düz (tek seviyeli): hiçbir eleman, kaynak grubun tamamını sarmalayan bir koleksiyon değil
        (FOR EACH e IN targetCollectionAfter: NOT wrapsEntireSource(e, values))
    AND sameElementsFlat(targetCollectionAfter, values)      // 2.1
    AND isIdempotent(targetCollectionAfter, values)          // 2.2 (clear+addAll → tekrar = aynı)
    AND concreteTypePreserved(targetConcreteTypeAfter, targetField)  // 2.3 (List→List, Set→Set)
    AND targetCollectionAfter != null                        // 2.3 (Set/null artık doldurulur)
END FUNCTION
```

## Fix Implementation

### Değişiklikler (root neden analizinin doğru olduğu varsayımıyla)

#### 1) Merkezi ayrıştırıcının düzeltilmesi (birincil)

**Dosya:** `src/main/java/com/thy/fss/common/inmemory/specification/BaseSpecificationService.java`
**Fonksiyon:** `assignToCollectionField(container, collectionAttr, value, service)`

1. **Düz atama kararını sağlamlaştır (1.1, 1.2):** Ayrıştırıcıdaki
   `&& allElementsAssignable((Collection<?>) value, elementType)` koşulunu kaldır. Eleman tipinin
   kendisinin koleksiyon olup olmadığı zaten `!isAssignableFrom(elementType, value.getClass())`
   ile ayırt edildiğinden, bu koşul yalnızca eleman tipi varyansında hatalı biçimde iç içe
   sarmalamaya düşüşe yol açıyordu. Düzeltilmiş koşul:
   ```
   IF value instanceof Collection
      AND elementType != null
      AND NOT isAssignableFrom(elementType, value.getClass())   // eleman tipi koleksiyon DEĞİL → düz
   THEN
       // C dalı: düz + idempotent
       snapshot := new ArrayList<>((Collection) value)   // aliasing-güvenli kopya
       collection.clear()                                // idempotency: ekleme değil, yerine koyma
       collection.addAll(snapshot)
   ELSE
       // ¬C dalı: tek eleman / skaler / meşru iç içe / null-model
       collection.add(resolveElementToAdd(value, collectionAttr))
   END IF
   ```
   Böylece eleman tipi skaler/model olan tüm koleksiyon hedeflerinde bir `Collection` değeri her
   zaman düz atanır; eleman tipi koleksiyon olan hedeflerde (meşru iç içe, 3.6) `add` davranışı
   korunur.

2. **Null başlatmayı tip-duyarlı yap (1.3, 2.3):** Null hedef koleksiyonu koşulsuz `new ArrayList<>()`
   yerine, bildirilen somut tiple uyumlu bir örnekle başlat:
   ```
   IF collection == null THEN
       collection := createConcreteCollectionFor(collectionAttr)
       service.setFieldValue(container, collectionAttr, collection)
   END IF
   ```
   `createConcreteCollectionFor(attr)`:
   - somut tip `Set`'e uyumluysa → `new LinkedHashSet<>()` (ekleme sırasını koruyarak deterministik
     ve idempotent sonuç),
   - `List`/`Collection` ise → `new ArrayList<>()`,
   - tip bilinmiyorsa → güvenli varsayılan `new ArrayList<>()`.

#### 2) Somut koleksiyon tipinin metadataya taşınması (2.3'ün ön koşulu)

**Dosya:** `src/main/java/com/thy/fss/common/inmemory/specification/attribute/CollectionAttribute.java`
- `Class<?> collectionType` alanı eklenir ve `getCollectionType()` erişimcisi sağlanır.
- Yeni yapıcı: `CollectionAttribute(name, ownerType, elementType, collectionType)`.
- Geriye dönük uyum: mevcut üç argümanlı yapıcı korunur ve `collectionType`'ı `Collection.class`
  olarak varsayar (elle yazılmış mevcut kullanımlar derlenmeye devam eder).
- `equals`/`hashCode`/`toString` tutarlı biçimde güncellenir. Not: eşleştirme sıcak yolu
  MetaAttribute'ları kimlik (`==`) ile karşılaştırdığından bu değişiklik sıcak yol davranışını
  etkilemez.

**Dosya:** `src/main/java/com/thy/fss/common/inmemory/processor/generator/StaticMetaModelGenerator.java`
**Fonksiyon:** `generateCollectionAttribute(...)`
- Üretilen `CollectionAttribute` bildirimi, alanın bildirilen koleksiyon tipini de içerir. Alanın
  ham (erased) bildirilmiş tipi (`java.util.List` / `java.util.Set` / `java.util.Collection`)
  `field.asType()`'tan elde edilir ve dördüncü argüman olarak `.class` biçiminde yazılır. Örnek:
  ```java
  public static final CollectionAttribute<CollectionEntity, Integer> numbers =
      new CollectionAttribute<>("numbers", CollectionEntity.class, Integer.class, java.util.Set.class);
  ```
- Yalnızca `List`, `Set`, `Collection` desteklendiğinden (bkz. `MetaModelValidator`) eşleme
  sınırlıdır ve güvenlidir. Mevcut metamodeller derleme sırasında yeniden üretilir.

> **Alternatif tasarım (kayıt için):** `CollectionAttribute`'u değiştirmek yerine üretilen servise
> alan başına bir `newCollectionInstanceFor(MetaAttribute)` fabrika metodu eklenebilir (üretici
> bildirilen tipi zaten bilir). Ancak somut koleksiyon tipi, doğal olarak eleman tipiyle birlikte
> `CollectionAttribute`'ta yer aldığından birincil yaklaşım tercih edilmiştir.

#### 3) MappingApplicator (davranış değişikliği yok)

**Dosya:** `src/main/java/com/thy/fss/common/inmemory/engine/mapping/MappingApplicator.java`
- `applyCollectionMapping`, atamayı merkezi ayrıştırıcıya `assignTargetValue` üzerinden
  yönlendirmeye devam eder (kaldırılan kısayol geri **eklenmez**). Kök neden ayrıştırıcıda
  çözüldüğü için tek/çok seviyeli ve `ALL` seçici yolları tutarlı biçimde düzelir.
- `applyMappingsToEntity` içindeki `try/catch` yapısı korunur; 1.3'teki istisnanın kaynağı
  ortadan kalktığı için sessiz yutma artık bu senaryoyu gizlemez. (İsteğe bağlı: WARN logu daha
  fazla bağlam içerecek şekilde zenginleştirilebilir; işlevsel davranış değişmez.)

## Testing Strategy

### Validation Approach

Test stratejisi iki aşamalıdır: önce düzeltilmemiş kod üzerinde hatayı gösteren karşı örnekleri
(counterexample) yüzeye çıkarmak, ardından düzeltmenin doğru çalıştığını ve mevcut davranışları
koruduğunu doğrulamak. Mevcut `CollectionMappingNesting*` test paketiyle (Example, FixChecking,
Preservation, FaultCondition, Integration) tutarlı jqwik tabanlı yaklaşım kullanılır.

> Kritik gözlem: Mevcut 42 `CollectionMappingNesting*` testi geçiyor; çünkü hedef koleksiyonu
> **doğru somut tiple önceden başlatıyorlar** (örn. `numbers` için `HashSet`, `tags` için
> `ArrayList`). Bu ön-başlatma, hem null yolunu hem de eleman-tipi varyansı yolunu maskeliyor.
> Yeni testler bu maskeyi kaldırmak için null hedefleri ve eleman-tipi varyansını açıkça kapsamalıdır.

### Exploratory Bug Condition Checking

**Amaç:** Düzeltmeden ÖNCE, düzeltilmemiş kod üzerinde hatayı gösteren karşı örnekleri yüzeye
çıkarmak ve kök neden hipotezini doğrulamak/çürütmek. Çürütülürse yeniden hipotez kurulur.

**Test Planı:** Klavye/UI değil; eşleştirme motoru senaryoları simüle edilir. Hedef koleksiyonu
**önceden başlatmadan** (null) veya eleman-tipi varyansı ile atama yapan testler yazılır ve
DÜZELTİLMEMİŞ kodda çalıştırılıp başarısızlıklar gözlemlenir.

**Test Durumları:**
1. **`tags` varyans + null hedef:** Bildirilen eleman tipiyle birebir atanamayan değerler içeren
   bir grup `List<String>` null hedefe atanır (düzeltilmemiş kodda iç içe `[[...]]` üretir).
2. **Tekrarlı uygulama (büyüme):** Aynı atama iki-üç kez uygulanır (düzeltilmemiş kodda hedef büyür).
3. **`numbers` (`Set<Integer>`) null hedef:** Grup null `Set` hedefe atanır (düzeltilmemiş kodda
   alan null kalır; `ClassCastException` yutulur).
4. **Çok seviyeli yol + varyans (kenar durum):** İç içe nesne üzerinden erişilen koleksiyon hedefe
   varyanslı atama (düzeltilmemiş kodda iç içe üretebilir).

**Beklenen Karşı Örnekler:**
- Düz beklenirken iç içe (`[[...]]`) yapı; tekrarlı uygulamada sınırsız büyüme.
- `Set`/null hedefin atanmamış (null) kalması.
- Olası nedenler: aşırı-katı `allElementsAssignable` koşulu; tip-duyarsız `new ArrayList<>()` null
  başlatması.

### Fix Checking

**Amaç:** Bug koşulu sağlanan tüm girdiler için düzeltilmiş fonksiyonun beklenen davranışı
ürettiğini doğrulamak.

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := assignToCollectionField_fixed(input)
  ASSERT expectedBehavior(result)
    // düz (tek seviyeli) + kaynakla aynı elemanlar
    // + somut tip korunmuş (List/Set)
    // + idempotent (tekrar uygulama sonucu değiştirmez)
END FOR
```

### Preservation Checking

**Amaç:** Bug koşulu sağlanmayan tüm girdiler için düzeltilmiş fonksiyonun orijinal fonksiyonla
aynı sonucu ürettiğini doğrulamak.

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT assignToCollectionField_original(input) = assignToCollectionField_fixed(input)
END FOR
```

**Test Yaklaşımı:** Koruma denetimi için özellik-tabanlı test (PBT) önerilir; çünkü:
- Girdi uzayında çok sayıda durumu otomatik üretir.
- Elle yazılan testlerin kaçırabileceği kenar durumları yakalar.
- Uyumlu (¬C) girdiler için davranışın değişmediğine dair güçlü güvence sağlar.

**Test Planı:** Önce DÜZELTİLMEMİŞ kodda ¬C girdileri (skaler hedef, tek eleman ekleme, meşru iç
içe, agregasyon, uyumlu düz atama) için davranış gözlemlenir; ardından bu davranışı yakalayan
özellik-tabanlı testler yazılır.

**Test Durumları:**
1. **Skaler hedef koruması (3.4):** Düzeltilmemiş kodda skaler alana atamanın doğru çalıştığı
   gözlemlenir; düzeltme sonrası aynı sonuç doğrulanır.
2. **Tek eleman ekleme koruması (3.3):** FIRST/LAST/ANY veya skaler tek değerin koleksiyona
   eklenmesi (append) korunur.
3. **Meşru iç içe koruması (3.6):** `List<List<String>>` hedefe tek iç listenin `[[...]]` olarak
   eklenmesi korunur.
4. **Agregasyon koruması (3.5):** SUM/AVG/COUNT/MIN/MAX sonuçlarının skaler hedefe değişmeden
   yazılması korunur.
5. **Uyumlu düz atama koruması (3.1, 3.2):** Eleman tipleri zaten uyumlu olan tek/çok seviyeli düz
   atamaların düz kalması korunur.
6. **Null-`List` başlatma koruması (3.7):** Null `List`/`Collection` hedefin başlatılıp düz
   doldurulması korunur.

### Unit Tests

- Merkezi ayrıştırıcının düz atama (C) dalı: `tags` (`List<String>`) ve `numbers` (`Set<Integer>`)
  için null ve dolu hedefte düz, tip-koruyucu sonuç (deterministik `@Example`).
- Kenar durumlar: boş kaynak koleksiyon (hedef boşalır, idempotent); tek elemanlı kaynak.
- Meşru iç içe (`List<List<String>>`) tek eleman ekleme.
- `Set` somut tipinin korunması (`isInstanceOf(Set.class)`) ve idempotency (tekrar atamada boyut
  sabit).
- `StaticMetaModelGenerator` çıktısı: üretilen `CollectionAttribute` bildiriminin dördüncü argüman
  olarak doğru somut tipi (`java.util.Set.class` / `java.util.List.class`) içermesi.

### Property-Based Tests

- Rastgele koleksiyon içerikleri ve eleman-tipi varyansı üretilerek C alanında düz + idempotent +
  tip-koruyucu sonuç doğrulanır (Fix Checking).
- Rastgele ¬C girdileri (skaler hedef, tek eleman, meşru iç içe, agregasyon, uyumlu düz atama)
  üretilerek F' = F korunması doğrulanır (Preservation Checking).
- Idempotency: aynı atamanın N kez uygulanmasının sonucu değiştirmediği çok sayıda senaryoda
  doğrulanır (sınırsız büyüme olmaması).

### Integration Tests

- Tam senkronizasyon akışında (MANY_TO_ONE_COLLECTION eşleştirmesi) `List` ve `Set` hedeflere düz
  atama; tekrarlı senkronizasyonda idempotency.
- Çok seviyeli hedef yolu (iç içe nesne üzerinden koleksiyon alanı) ile tek seviyeli davranışın
  tutarlılığı.
- `ALL` seçici yolu (`setValueByPathWithCollections`) üzerinden düz sonuç.
- Meşru iç içe ve skaler/agregasyon eşleştirmelerinin tam akışta korunması.
