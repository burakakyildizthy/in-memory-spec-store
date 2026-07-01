# Uygulama Planı: filter-negated-operators

## Genel Bakış

Bu plan, `NumberFilter` ve `TemporalFilter` filtre ailelerine olumsuz (negated) karşılaştırma operatörlerini ekleyen tasarımı artımlı (incremental) adımlarla uygular. Sıralama, tasarımdaki bağımlılık yönünü izler: önce paylaşılan temel bileşenler (FilterConstants sabitleri, Operator enum değerleri), sonra filtre modeli alan/erişimci değişiklikleri, ardından alt sınıf setter ezmeleri, sonra bu temellere bağlı üreticiler (FilterDeserializerGenerator, StaticSpecificationServiceGenerator) ve en sonda testler ile doğrulama gelir. Her adım bir önceki adımların üzerine kurulur ve ortada bağlanmamış (orphan) kod bırakmaz.

Kaynak kök dizini: `src/main/java/com/thy/fss/common/inmemory/`
Test kök dizini: `src/test/java/com/thy/fss/common/inmemory/`

## Görevler

- [x] 1. Paylaşılan temel bileşenleri ekle (sabitler ve operatörler)
  - [x] 1.1 FilterConstants olumsuz operatör sabitlerini ekle
    - `filter/FilterConstants.java` içine sayısal sabitleri ekle: `FIELD_NGT = "ngt"`, `FIELD_NLT = "nlt"`, `FIELD_NGTE = "ngte"`, `FIELD_NLTE = "nlte"`
    - Aynı dosyaya temporal sabitleri ekle: `FIELD_NOT_BEFORE = "nbe"`, `FIELD_NOT_AFTER = "naf"`, `FIELD_NOT_ON_OR_BEFORE = "nobe"`, `FIELD_NOT_ON_OR_AFTER = "noaf"`
    - Mevcut `FIELD_GT`/`FIELD_BEFORE` desenine ve adlandırma biçimine bire bir uy
    - _Requirements: 10.1, 10.2_

  - [x] 1.2 Operator enum olumsuz değerlerini ekle
    - `specification/Operator.java` içine sayısal değerleri ekle: `NOT_GREATER_THAN`, `NOT_LESS_THAN`, `NOT_GREATER_OR_EQUAL_THAN`, `NOT_LESS_OR_EQUAL_THAN`
    - Aynı enum'a temporal değerleri ekle: `NOT_IS_BEFORE`, `NOT_IS_AFTER`, `NOT_IS_ON_OR_BEFORE`, `NOT_IS_ON_OR_AFTER`
    - `last`/`next` (TemporalPreset) için olumsuz değer ekleme (kapsam dışı)
    - _Requirements: 11.1, 11.2_

  - [x] 1.3 FilterConstants sabit değerleri ve Operator enum varlığı için birim testleri
    - Sekiz yeni sabitin beklenen `String` değerlerine sahip olduğunu doğrula
    - Sekiz yeni enum değerinin tanımlı olduğunu doğrula
    - Testleri mevcut sabit/operatör test altyapısına ekle; yeni operatörler için ayrı filtre test dosyası açma
    - _Requirements: 10.1, 10.2, 11.1, 11.2_

- [x] 2. Filtre modeline olumsuz operatör alanlarını ekle
  - [x] 2.1 NumberFilter olumsuz alanlarını, erişimcilerini ve nesne metotlarını uygula
    - `filter/NumberFilter.java` içine `@JsonProperty` ile dört private `F` alanı ekle: `notGreaterThan` (`ngt`), `notLessThan` (`nlt`), `notGreaterOrEqualThan` (`ngte`), `notLessOrEqualThan` (`nlte`)
    - Her alan için getter ve `this` döndüren zincirlenebilir setter ekle (mevcut `setGreaterThan` deseni)
    - Kopyalama kurucusuna dört alanın kopyalanmasını ekle; `equals`, `hashCode` ve `toString` çıktısına dört alanı dahil et
    - Argümansız kurucunun dört alanı örtük `null` bıraktığını koru
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 7.1, 8.1, 8.2, 8.5, 9.1_

  - [x] 2.2 TemporalFilter olumsuz alanlarını, erişimcilerini ve nesne metotlarını uygula
    - `filter/TemporalFilter.java` içine `@JsonProperty` ile dört private `F` alanı ekle: `notIsBefore` (`nbe`), `notIsAfter` (`naf`), `notIsOnOrBefore` (`nobe`), `notIsOnOrAfter` (`noaf`)
    - Her alan için getter ve `TemporalFilter<F>` döndüren zincirlenebilir setter ekle (mevcut `setIsBefore` deseni)
    - Kopyalama kurucusuna dört alanı ekle; `equals`, `hashCode` ve `toString` çıktısına dört alanı dahil et
    - `last`/`next` alanlarına, bunların erişimcilerine ve `equals`/`hashCode`/`toString`/kopyalama katılımına dokunma
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 6.1, 7.2, 8.3, 8.4, 8.5, 9.2_

- [x] 3. TemporalFilter alt sınıflarında setter ezmelerini ve model düzeyi özellik testlerini uygula
  - [x] 3.1 InstantFilter setter ezmelerini ekle
    - `filter/InstantFilter.java` içinde `setNotIsBefore`, `setNotIsAfter`, `setNotIsOnOrBefore`, `setNotIsOnOrAfter` metotlarını ez; her biri `super`'i çağırıp `InstantFilter` döndürsün
    - _Requirements: 5.1, 5.4_

  - [x] 3.2 LocalDateFilter setter ezmelerini ekle
    - `filter/LocalDateFilter.java` içinde dört setter'ı `LocalDate` tipiyle ez; her biri `super`'i çağırıp `LocalDateFilter` döndürsün
    - _Requirements: 5.2, 5.4_

  - [x] 3.3 LocalDateTimeFilter setter ezmelerini ekle
    - `filter/LocalDateTimeFilter.java` içinde dört setter'ı `LocalDateTime` tipiyle ez; her biri `super`'i çağırıp `LocalDateTimeFilter` döndürsün
    - _Requirements: 5.3, 5.4_

  - [x] 3.4 Özellik testi: kopyalama kurucusu eşitliği
    - **Property 5: Kopyalama kurucusu eşitliği** — kopyalama kurucusuyla oluşturulan kopya, olumsuz alan değerleri dahil kaynağa `equals` bakımından eşittir
    - jqwik `@Property`, en az 100 iterasyon; etiket: `Feature: filter-negated-operators, Property 5: Kopyalama kurucusu eşitliği`
    - Testleri mevcut filtre test sınıflarına additive ekle; yeni test dosyası açma
    - **Validates: Requirements 7.1, 7.2**

  - [x] 3.5 Özellik testi: equals/hashCode dahiliyeti ve tutarlılığı
    - **Property 6: equals/hashCode dahiliyeti ve tutarlılığı** — yalnızca bir olumsuz alanda farklılık `equals`'i `false` yapar; eşit örnekler aynı `hashCode`'u üretir
    - jqwik `@Property`, en az 100 iterasyon; etiket: `Feature: filter-negated-operators, Property 6: equals/hashCode dahiliyeti ve tutarlılığı`
    - Testleri mevcut filtre test sınıflarına additive ekle; yeni test dosyası açma
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5**

  - [x] 3.6 Özellik testi: erişimci çevrimi ve zincirlenebilir setter
    - **Property 7: Erişimci çevrimi ve zincirlenebilir setter** — `setNotX(v)` üzerinde çağrıldığı örneği (alt sınıfta kendi somut tipini) döndürür ve `getNotX()` sonrasında `v` (null dahil) değerini döndürür
    - jqwik `@Property`, en az 100 iterasyon; etiket: `Feature: filter-negated-operators, Property 7: Erişimci çevrimi ve zincirlenebilir setter`
    - Alt sınıf setter dönüş tipini ilgili alt sınıf test sınıfında doğrula
    - Testleri mevcut filtre test sınıflarına additive ekle; yeni test dosyası açma
    - **Validates: Requirements 1.3, 1.4, 3.3, 3.4, 5.1, 5.2, 5.3, 5.4**

- [x] 4. Checkpoint - Filtre modeli ve alt sınıflar derlenmeli
  - Tüm testlerin geçtiğinden emin ol, soru çıkarsa kullanıcıya danış.

- [x] 5. FilterDeserializerGenerator olumsuz operatör üretimini ekle
  - [x] 5.1 JSON gövdesi switch case'lerini ekle
    - `processor/generator/FilterDeserializerGenerator.java` içinde `generateNumericSwitchCases(...)` ve `generateMixedFieldTypeSwitchCases(...)` sayısal kollarına `FIELD_NGT→setNotGreaterThan`, `FIELD_NGTE→setNotGreaterOrEqualThan`, `FIELD_NLT→setNotLessThan`, `FIELD_NLTE→setNotLessOrEqualThan` case'lerini ekle
    - `generateTemporalSwitchCases(...)` ve `generateMixedFieldTypeSwitchCases(...)` temporal kollarına `FIELD_NOT_BEFORE→setNotIsBefore`, `FIELD_NOT_AFTER→setNotIsAfter`, `FIELD_NOT_ON_OR_BEFORE→setNotIsOnOrBefore`, `FIELD_NOT_ON_OR_AFTER→setNotIsOnOrAfter` case'lerini ekle
    - Mevcut `parseMethod`/`getXxxValue` akışını kullan; bulunmayan/null alanda setter çağrılmaz, dönüştürülemeyen değerde mevcut çözümleme istisnası fırlatılır
    - _Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8, 12.9, 12.10_

  - [x] 5.2 Sorgu parametresi bind case'lerini ekle
    - Aynı dosyada web binding üreten döngünün `isNumeric()` kolunda dört sayısal olumsuz operatör için `generateBindCase(...)` çağrılarını ekle
    - `isTemporal()` kolunda dört temporal olumsuz operatör için `generateBindCase(...)` çağrılarını ekle
    - _Requirements: 13.1, 13.2, 13.3, 13.4, 13.5, 13.6, 13.7, 13.8_

  - [x] 5.3 Element-operatör setter-ad eşleme case'lerini ekle
    - Aynı dosyada `generateElementOperatorBinding(...)` içindeki `setterMethod` switch'ine sekiz olumsuz operatör için `FIELD_* -> "setNot..."` case'lerini ekle
    - Böylece koleksiyon-öğe ve çok seviyeli path filtrelerinde olumsuz operatörler tutarlı çalışır
    - _Requirements: 12.1, 12.5, 13.1, 13.5_

  - [x] 5.4 Özellik testi: JSON çevrimi (round-trip) korunumu
    - **Property 4: JSON çevrimi (round-trip) korunumu** — olumsuz operatör alanları atanmış bir filtrenin JSON'a serileştirilip yeniden çözümlenmesi eşdeğer bir filtre üretir
    - jqwik `@Property`, en az 100 iterasyon; etiket: `Feature: filter-negated-operators, Property 4: JSON çevrimi (round-trip) korunumu`
    - `ObjectMapper` ile serialize/deserialize eşitliğini doğrula; testleri mevcut filtre test sınıflarına additive ekle
    - **Validates: Requirements 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8**

- [x] 6. StaticSpecificationServiceGenerator olumsuz operatör üretimini ekle
  - [x] 6.1 Desteklenen operatör kümelerine olumsuz operatörleri ekle
    - `processor/generator/StaticSpecificationServiceGenerator.java` statik başlatma bloğunda `numericOps` kümesine `NOT_GREATER_THAN`, `NOT_LESS_THAN`, `NOT_GREATER_OR_EQUAL_THAN`, `NOT_LESS_OR_EQUAL_THAN` ekle
    - `dateTimeOps` kümesine `NOT_IS_BEFORE`, `NOT_IS_AFTER`, `NOT_IS_ON_OR_BEFORE`, `NOT_IS_ON_OR_AFTER` ekle; `LAST`/`NEXT`'e dokunma
    - _Requirements: 11.3, 11.4_

  - [x] 6.2 getOperatorMethodSuffix eşlemelerini ekle
    - Aynı dosyada `getOperatorMethodSuffix` switch'ine sekiz olumsuz operatör için sonek eşlemelerini ekle (`NotGreaterThan`, `NotLessThan`, `NotGreaterOrEqualThan`, `NotLessOrEqualThan`, `NotIsBefore`, `NotIsAfter`, `NotIsOnOrBefore`, `NotIsOnOrAfter`)
    - Bu eşleme, üretilen `validate{Field}{Suffix}` metot adlarını ve `validateFilter` içindeki `get{Suffix}()` çağrılarını otomatik türetir (Gereksinim 14 ve 15.3 kendiliğinden karşılanır)
    - _Requirements: 11.5, 11.6, 14.1, 14.2, 14.3, 14.4, 14.5, 15.3_

  - [x] 6.3 Karşılaştırma mantığı switch case'lerini ekle
    - Aynı dosyada `generateValidationMethod(...)` operatör switch'ine sayısal case'leri ekle: `NOT_GREATER_THAN`→`compareTo <= 0`, `NOT_LESS_THAN`→`>= 0`, `NOT_GREATER_OR_EQUAL_THAN`→`< 0`, `NOT_LESS_OR_EQUAL_THAN`→`> 0` (primitif tipler için doğrudan operatör)
    - Temporal case'leri ekle: `NOT_IS_BEFORE`→`!isBefore`, `NOT_IS_AFTER`→`!isAfter`, `NOT_IS_ON_OR_BEFORE`→`isAfter`, `NOT_IS_ON_OR_AFTER`→`isBefore`; Zaman_Tipi olmayan Comparable için `compareTo` fallback (`>=0`/`<=0`/`>0`/`<0`)
    - Her üretilen kolda `field != null` ön koşulunu koru (null alan → `false`)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7_

  - [x] 6.4 Özellik testi: sayısal olumsuzlama ikiliği
    - **Property 1: Sayısal olumsuzlama ikiliği ve compareTo tutarlılığı** — her NumberFilter olumsuz operatörü, null olmayan alan değeri için karşılık gelen pozitif operatörün tam tersini verir; alan `null` ise sonuç `false`'tur
    - Double/Integer/Long üreteçleri (alan değeri olarak `null` da üret); jqwik `@Property`, en az 100 iterasyon; etiket: `Feature: filter-negated-operators, Property 1: Sayısal olumsuzlama ikiliği ve compareTo tutarlılığı`
    - Testleri mevcut `DoubleFilterTest`, `IntegerFilterTest`, `LongFilterTest` sınıflarına additive ekle
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**

  - [x] 6.5 Özellik testi: zamansal olumsuzlama ikiliği
    - **Property 2: Zamansal olumsuzlama ikiliği** — her TemporalFilter olumsuz operatörü, Zaman_Tipi alan ve null olmayan karşılaştırma değeri için pozitif operatörün tam tersini verir; alan `null` ise `false`'tur
    - Instant/LocalDate/LocalDateTime üreteçleri; jqwik `@Property`, en az 100 iterasyon; etiket: `Feature: filter-negated-operators, Property 2: Zamansal olumsuzlama ikiliği`
    - Testleri mevcut `InstantFilterTest`, `LocalDateFilterTest`, `LocalDateTimeFilterTest` sınıflarına additive ekle
    - **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 4.6**

  - [x] 6.6 Özellik testi: validateFilter mantıksal VE kombinasyonu
    - **Property 3: validateFilter mantıksal VE kombinasyonu** — `validateFilter` yalnızca ayarlanmış (null olmayan) tüm operatörler eşzamanlı sağlandığında `true`'dur; `null` operatörler sonucu etkilemez
    - Rastgele filtre + varlık; model tabanlı beklenen sonuçla karşılaştır; jqwik `@Property`, en az 100 iterasyon; etiket: `Feature: filter-negated-operators, Property 3: validateFilter mantıksal VE kombinasyonu`
    - Testleri mevcut filtre test sınıflarına additive ekle
    - **Validates: Requirements 14.1, 14.2, 14.3, 14.4, 14.5, 15.3**

  - [x] 6.7 Özellik testi: geriye dönük uyumluluk (olumsuz alanlar null)
    - **Property 8: Geriye dönük uyumluluk** — tüm olumsuz alanlar `null` iken `validateFilter` sonucu, yalnızca pozitif operatörler (ve `last`/`next`) uygulanmış gibi elde edilen sonuçla aynıdır
    - jqwik `@Property`, en az 100 iterasyon; etiket: `Feature: filter-negated-operators, Property 8: Geriye dönük uyumluluk`
    - Testleri mevcut filtre test sınıflarına additive ekle
    - **Validates: Requirements 15.1, 15.2, 15.3, 6.2**

- [x] 7. Checkpoint - Üretim zamanı (annotation processing) derlemesi başarılı olmalı
  - Tüm testlerin geçtiğinden emin ol, soru çıkarsa kullanıcıya danış.

- [x] 8. Kapsam testlerini mevcut filtre test sınıflarına ekle (Gereksinim 16, additive)
  - [x] 8.1 NumberFilter olumsuz operatör kapsam testlerini ekle
    - Mevcut `DoubleFilterTest`, `IntegerFilterTest`, `LongFilterTest` sınıflarına additive ekle; bu operatörler için ayrı/yeni test dosyası açma
    - Dört yeni alanın getter davranışını ve zincirlenebilir setter davranışını doğrula (16.3); argümansız kurucuda dört alanın `null` başladığını doğrula (16.4)
    - Kopyalama kurucusunun dört alanı taşıdığını (16.5); dört alanın `equals`/`hashCode`'a dahil olduğunu (16.6); ad ve değerlerinin `toString`'de yer aldığını (16.7) doğrula
    - Her olumsuz operatörün Gereksinim 2 anlambilimiyle tutarlı eşleştirme davranışını (16.8) ve alan değeri `null` iken `false` sonucunu (16.9) doğrula
    - Mevcut assertion'ları koru; yalnızca yeni testler ekle (16.12); uygun yerlerde mevcut jqwik kurallarını izle (16.11)
    - _Requirements: 16.1, 16.3, 16.4, 16.5, 16.6, 16.7, 16.8, 16.9, 16.11, 16.12_

  - [x] 8.2 TemporalFilter olumsuz operatör kapsam testlerini ekle
    - Mevcut `InstantFilterTest`, `LocalDateFilterTest`, `LocalDateTimeFilterTest` ve/veya `TemporalFilterTest` sınıflarına additive ekle; bu operatörler için ayrı/yeni test dosyası açma
    - Dört yeni alanın getter/zincirlenebilir setter davranışını (16.3), argümansız kurucuda `null` başlangıcını (16.4), kopyalama kurucusu taşımasını (16.5), `equals`/`hashCode` dahiliyetini (16.6) ve `toString` içeriğini (16.7) doğrula
    - Her olumsuz operatörün Gereksinim 4 anlambilimiyle tutarlı eşleştirme davranışını (16.8) ve alan değeri `null` iken `false` sonucunu (16.9) doğrula
    - Zaman_Tipi tabanlı `isBefore`/`isAfter` değerlendirmesini ilgili alt sınıfın test sınıfında doğrula (16.10)
    - Mevcut assertion'ları koru; yalnızca yeni testler ekle (16.12); uygun yerlerde mevcut jqwik kurallarını izle (16.11)
    - _Requirements: 16.2, 16.3, 16.4, 16.5, 16.6, 16.7, 16.8, 16.9, 16.10, 16.11, 16.12_

- [x] 9. Son doğrulama - Gradle derleme ve testleri çalıştır
  - [x] 9.1 Gradle ile derleme ve tüm testleri çalıştır
    - `./gradlew build` (veya Windows'ta `gradlew.bat build`) komutunu çalıştırarak annotation processing üretiminin derlendiğini ve tüm birim/özellik testlerinin geçtiğini doğrula
    - Başarısız derleme veya test varsa kök nedeni bulup düzelt, ardından yeniden çalıştır
    - _Requirements: 2.1, 4.1, 12.1, 13.1, 14.1, 16.1, 16.2_

## Notlar

- `*` ile işaretli alt görevler isteğe bağlıdır (özellik/birim testleri) ve daha hızlı MVP için atlanabilir; üst düzey görevler asla `*` ile işaretlenmez.
- Her görev, izlenebilirlik için tasarım ve gereksinim maddelerine referans verir.
- Checkpoint'ler artımlı doğrulama sağlar.
- Özellik testleri, tasarımdaki 8 doğruluk özelliğini jqwik ile evrensel niceleyicili olarak doğrular; her biri en az 100 iterasyon çalışır ve `Feature: filter-negated-operators, Property {number}: {property_text}` etiketini taşır.
- Gereksinim 16 gereği yeni operatörlerin kapsam testleri ayrı/yeni dosyalarda değil, mevcut filtre test sınıflarına additive olarak eklenir.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1", "2.2"] },
    { "id": 2, "tasks": ["3.1", "3.2", "3.3", "5.1", "6.1"] },
    { "id": 3, "tasks": ["5.2", "6.2"] },
    { "id": 4, "tasks": ["5.3", "6.3"] },
    { "id": 5, "tasks": ["1.3", "8.1", "8.2"] },
    { "id": 6, "tasks": ["3.4"] },
    { "id": 7, "tasks": ["3.5"] },
    { "id": 8, "tasks": ["3.6"] },
    { "id": 9, "tasks": ["6.4", "6.5"] },
    { "id": 10, "tasks": ["6.6"] },
    { "id": 11, "tasks": ["5.4"] },
    { "id": 12, "tasks": ["6.7"] },
    { "id": 13, "tasks": ["9.1"] }
  ]
}
```
