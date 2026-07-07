# Gereksinim Dokümanı

## Giriş

Bu özellik, `NumberFilter<F extends Comparable>` ve `TemporalFilter<F extends Comparable>` sınıflarına mevcut pozitif karşılaştırma operatörlerinin mantıksal "değilse" (olumsuz) versiyonlarını ekler.

- NumberFilter için pozitif operatörler `greaterThan`, `lessThan`, `greaterOrEqualThan`, `lessOrEqualThan`; yeni olumsuz operatörler `notGreaterThan`, `notLessThan`, `notGreaterOrEqualThan`, `notLessOrEqualThan`'dir.
- TemporalFilter için pozitif operatörler `isBefore`, `isAfter`, `isOnOrBefore`, `isOnOrAfter`; yeni olumsuz operatörler `notIsBefore`, `notIsAfter`, `notIsOnOrBefore`, `notIsOnOrAfter`'dir.

Her yeni operatör, karşılık geldiği pozitif operatörün mantıksal olumsuzu olarak davranır.

Bu iki filtre ailesi aynı entegrasyon yüzeyini paylaşır. Yeni operatörler; her filtre sınıfının kendi yapısına (alanlar, erişimciler, kopyalama kurucusu, `equals`, `hashCode`, `toString`), paylaşılan sabitlere (`FilterConstants`), operatör kümesine (`Operator` enum), JSON serisizasyon çözümleyici üreticisine (`FilterDeserializerGenerator`) ve karşılaştırma/eşleştirme mantığını üreten servise (`StaticSpecificationServiceGenerator`) entegre edilir. TemporalFilter ayrıca zaman tabanlı alt sınıflara (`InstantFilter`, `LocalDateFilter`, `LocalDateTimeFilter`) entegre edilir. Böylece yeni operatörler hem JSON gövdesi üzerinden hem de sorgu parametresi (query parameter) üzerinden kullanılabilir ve mevcut sistemin geri kalanıyla tutarlı çalışır.

JSON kısaltmaları, ilgili pozitif operatörün kısaltmasının başına yalnızca `n` harfi eklenerek türetilir:
- NumberFilter: `gt` → `ngt`, `lt` → `nlt`, `gte` → `ngte`, `lte` → `nlte`.
- TemporalFilter: `be` → `nbe`, `af` → `naf`, `obe` → `nobe`, `oaf` → `noaf`.

Bu özellik TemporalFilter tarafında yalnızca dört karşılaştırma operatörünü (`isBefore`, `isAfter`, `isOnOrBefore`, `isOnOrAfter`) kapsar. `last` ve `next` göreli zaman ön ayarları (TemporalPreset) bu kapsamın dışında bırakılmıştır; gerekçesi Gereksinim 6'da açıklanmıştır.

## Sözlük (Glossary)

### Ortak Terimler

- **Karsilastirma_Degeri**: Bir operatörün filtre alanına atanan, karşılaştırmada referans olarak kullanılan değer.
- **Alan_Degeri**: Filtrelenen varlık (entity) alanının çalışma zamanındaki değeri.
- **FilterConstants**: Tüm filtre tiplerinde ve çözümleyicilerde kullanılan JSON alan adı sabitlerini içeren yardımcı sınıf.
- **Operator**: Specification ve filtre eşleştirmesinde kullanılan tüm operatörleri tanımlayan enum.
- **FilterDeserializerGenerator**: Filtre tipleri için yüksek performanslı Jackson çözümleyicilerini derleme zamanında üreten sınıf.
- **StaticSpecificationServiceGenerator**: Alan-operatör kombinasyonları için doğrulama (eşleştirme) metotlarını derleme zamanında üreten sınıf.
- **Chained_Setter**: Kendi filtre örneğini geri döndürerek metot zincirlemesine olanak tanıyan setter metodu.
- **Test_Paketi**: Filtre sınıfları için var olan JUnit/jqwik test sınıflarından (`DoubleFilterTest`, `IntegerFilterTest`, `LongFilterTest`, `InstantFilterTest`, `LocalDateFilterTest`, `LocalDateTimeFilterTest`, `TemporalFilterTest` vb.) oluşan mevcut test bütünü.

### NumberFilter Terimleri

- **NumberFilter**: `Comparable` tipindeki alanlar için aralık tabanlı filtreleme sağlayan, JHipster filtre desenini takip eden jenerik filtre sınıfı.
- **greaterThan / gt**: Alan_Degeri, Karsilastirma_Degeri'nden büyük olan kayıtları eşleştiren mevcut pozitif operatör (`compareTo > 0`).
- **lessThan / lt**: Alan_Degeri, Karsilastirma_Degeri'nden küçük olan kayıtları eşleştiren mevcut pozitif operatör (`compareTo < 0`).
- **greaterOrEqualThan / gte**: Alan_Degeri, Karsilastirma_Degeri'nden büyük veya ona eşit olan kayıtları eşleştiren mevcut pozitif operatör (`compareTo >= 0`).
- **lessOrEqualThan / lte**: Alan_Degeri, Karsilastirma_Degeri'nden küçük veya ona eşit olan kayıtları eşleştiren mevcut pozitif operatör (`compareTo <= 0`).
- **notGreaterThan / ngt**: `greaterThan` operatörünün olumsuzu; büyük OLMAYAN, yani küçük veya eşit olan kayıtları eşleştiren yeni operatör (`compareTo <= 0`).
- **notLessThan / nlt**: `lessThan` operatörünün olumsuzu; küçük OLMAYAN, yani büyük veya eşit olan kayıtları eşleştiren yeni operatör (`compareTo >= 0`).
- **notGreaterOrEqualThan / ngte**: `greaterOrEqualThan` operatörünün olumsuzu; büyük veya eşit OLMAYAN, yani küçük olan kayıtları eşleştiren yeni operatör (`compareTo < 0`).
- **notLessOrEqualThan / nlte**: `lessOrEqualThan` operatörünün olumsuzu; küçük veya eşit OLMAYAN, yani büyük olan kayıtları eşleştiren yeni operatör (`compareTo > 0`).

### TemporalFilter Terimleri

- **TemporalFilter**: `Comparable` tipindeki zaman/tarih alanları için karşılaştırma tabanlı filtreleme sağlayan, JHipster filtre desenini takip eden jenerik filtre sınıfı.
- **Zaman_Tipi**: `isBefore`/`isAfter` metotlarıyla karşılaştırma yapılan `LocalDate`, `LocalDateTime` ve `Instant` tiplerini ifade eden alan tipi kategorisi.
- **isBefore / be**: Alan_Degeri, Karsilastirma_Degeri'nden önce olan kayıtları eşleştiren mevcut pozitif operatör (Zaman_Tipi için `isBefore`; diğerleri için `compareTo < 0`).
- **isAfter / af**: Alan_Degeri, Karsilastirma_Degeri'nden sonra olan kayıtları eşleştiren mevcut pozitif operatör (Zaman_Tipi için `isAfter`; diğerleri için `compareTo > 0`).
- **isOnOrBefore / obe**: Alan_Degeri, Karsilastirma_Degeri'nde veya öncesinde olan kayıtları eşleştiren mevcut pozitif operatör (Zaman_Tipi için `!isAfter`; diğerleri için `compareTo <= 0`).
- **isOnOrAfter / oaf**: Alan_Degeri, Karsilastirma_Degeri'nde veya sonrasında olan kayıtları eşleştiren mevcut pozitif operatör (Zaman_Tipi için `!isBefore`; diğerleri için `compareTo >= 0`).
- **notIsBefore / nbe**: `isBefore` operatörünün olumsuzu; önce OLMAYAN, yani Karsilastirma_Degeri'nde veya sonrasında olan kayıtları eşleştiren yeni operatör (Zaman_Tipi için `!isBefore`; diğerleri için `compareTo >= 0`).
- **notIsAfter / naf**: `isAfter` operatörünün olumsuzu; sonra OLMAYAN, yani Karsilastirma_Degeri'nde veya öncesinde olan kayıtları eşleştiren yeni operatör (Zaman_Tipi için `!isAfter`; diğerleri için `compareTo <= 0`).
- **notIsOnOrBefore / nobe**: `isOnOrBefore` operatörünün olumsuzu; Karsilastirma_Degeri'nde veya öncesinde OLMAYAN, yani Karsilastirma_Degeri'nden sonra olan kayıtları eşleştiren yeni operatör (Zaman_Tipi için `isAfter`; diğerleri için `compareTo > 0`).
- **notIsOnOrAfter / noaf**: `isOnOrAfter` operatörünün olumsuzu; Karsilastirma_Degeri'nde veya sonrasında OLMAYAN, yani Karsilastirma_Degeri'nden önce olan kayıtları eşleştiren yeni operatör (Zaman_Tipi için `isBefore`; diğerleri için `compareTo < 0`).
- **TemporalPreset**: `last` ve `next` operatörlerinde kullanılan göreli zaman ön ayarını (örneğin `24h`, `40m`, `2M`) temsil eden tip.
- **InstantFilter / LocalDateFilter / LocalDateTimeFilter**: TemporalFilter'ı belirli bir zaman tipiyle (`Instant`, `LocalDate`, `LocalDateTime`) somutlaştıran ve setter'ları kendi tipini döndürecek şekilde ezen (override) alt sınıflar.

## Gereksinimler

### Gereksinim 1: NumberFilter olumsuz operatör alanlarının ve erişimcilerin eklenmesi

**Kullanıcı Hikayesi:** Bir geliştirici olarak, NumberFilter üzerinde olumsuz karşılaştırma operatörlerini alan ve erişimciler aracılığıyla ayarlayıp okuyabilmek istiyorum, böylece mevcut pozitif operatörlerle aynı programatik kullanım biçimini elde edebilirim.

#### Kabul Kriterleri

1. THE NumberFilter SHALL `notGreaterThan`, `notLessThan`, `notGreaterOrEqualThan` ve `notLessOrEqualThan` adlı, `F` tipinde dört adet private örnek alanı tanımlar.
2. THE NumberFilter SHALL `notGreaterThan` alanına `ngt`, `notLessThan` alanına `nlt`, `notGreaterOrEqualThan` alanına `ngte`, `notLessOrEqualThan` alanına `nlte` değerini `@JsonProperty` açıklaması olarak atar.
3. WHEN `notGreaterThan`, `notLessThan`, `notGreaterOrEqualThan` veya `notLessOrEqualThan` alanının getter metodu çağrılır, THE NumberFilter SHALL ilgili alana o an atanmış olan değeri (hiç atanmamışsa `null`) döndürür.
4. WHEN `notGreaterThan`, `notLessThan`, `notGreaterOrEqualThan` veya `notLessOrEqualThan` alanının setter metodu bir değer (`null` dahil) ile çağrılır, THE NumberFilter SHALL bu değeri ilgili alana atar ve üzerinde çağrıldığı NumberFilter örneğinin aynısını döndürür.
5. WHEN yeni bir NumberFilter örneği argümansız kurucu ile oluşturulur, THE NumberFilter SHALL `notGreaterThan`, `notLessThan`, `notGreaterOrEqualThan` ve `notLessOrEqualThan` alanlarının değerini `null` olarak başlatır.

### Gereksinim 2: NumberFilter olumsuz operatörlerin anlambilimi (semantics)

**Kullanıcı Hikayesi:** Bir geliştirici olarak, NumberFilter olumsuz operatörlerinin ilgili pozitif operatörün mantıksal tersini uygulamasını istiyorum, böylece "büyük olmayan", "küçük olmayan" gibi koşulları doğrudan ifade edebilirim.

#### Kabul Kriterleri

1. WHEN `notGreaterThan` operatörü boş olmayan (null olmayan) bir Karsilastirma_Degeri ile uygulanır, THE StaticSpecificationServiceGenerator SHALL Alan_Degeri ile Karsilastirma_Degeri arasında `compareTo` sonucu 0 veya daha küçük olan kayıtları `true`, diğer tüm kayıtları `false` olarak değerlendiren doğrulama mantığı üretir.
2. WHEN `notLessThan` operatörü boş olmayan bir Karsilastirma_Degeri ile uygulanır, THE StaticSpecificationServiceGenerator SHALL Alan_Degeri ile Karsilastirma_Degeri arasında `compareTo` sonucu 0 veya daha büyük olan kayıtları `true`, diğer tüm kayıtları `false` olarak değerlendiren doğrulama mantığı üretir.
3. WHEN `notGreaterOrEqualThan` operatörü boş olmayan bir Karsilastirma_Degeri ile uygulanır, THE StaticSpecificationServiceGenerator SHALL Alan_Degeri ile Karsilastirma_Degeri arasında `compareTo` sonucu 0'dan küçük olan kayıtları `true`, diğer tüm kayıtları `false` olarak değerlendiren doğrulama mantığı üretir.
4. WHEN `notLessOrEqualThan` operatörü boş olmayan bir Karsilastirma_Degeri ile uygulanır, THE StaticSpecificationServiceGenerator SHALL Alan_Degeri ile Karsilastirma_Degeri arasında `compareTo` sonucu 0'dan büyük olan kayıtları `true`, diğer tüm kayıtları `false` olarak değerlendiren doğrulama mantığı üretir.
5. IF bir NumberFilter olumsuz operatörü uygulandığında Alan_Degeri `null` ise, THEN THE StaticSpecificationServiceGenerator SHALL mevcut pozitif karşılaştırma operatörleriyle tutarlı biçimde ilgili kaydı eşleşmemiş (`false`) olarak değerlendirir.
6. IF bir NumberFilter olumsuz operatörünün Karsilastirma_Degeri `null` ise, THEN THE StaticSpecificationServiceGenerator SHALL mevcut pozitif operatörlerle tutarlı biçimde davranır ve karşılaştırma sırasında istisna (exception) fırlatmaz.

### Gereksinim 3: TemporalFilter olumsuz operatör alanlarının ve erişimcilerin eklenmesi

**Kullanıcı Hikayesi:** Bir geliştirici olarak, TemporalFilter üzerinde olumsuz karşılaştırma operatörlerini alan ve erişimciler aracılığıyla ayarlayıp okuyabilmek istiyorum, böylece mevcut pozitif operatörlerle aynı programatik kullanım biçimini elde edebilirim.

#### Kabul Kriterleri

1. THE TemporalFilter SHALL `notIsBefore`, `notIsAfter`, `notIsOnOrBefore` ve `notIsOnOrAfter` adlı, `F` tipinde dört adet private örnek alanı tanımlar.
2. THE TemporalFilter SHALL `notIsBefore` alanına `nbe`, `notIsAfter` alanına `naf`, `notIsOnOrBefore` alanına `nobe`, `notIsOnOrAfter` alanına `noaf` değerini `@JsonProperty` açıklaması olarak atar.
3. WHEN `notIsBefore`, `notIsAfter`, `notIsOnOrBefore` veya `notIsOnOrAfter` alanının getter metodu çağrılır, THE TemporalFilter SHALL ilgili alana o an atanmış olan değeri (hiç atanmamışsa `null`) döndürür.
4. WHEN `notIsBefore`, `notIsAfter`, `notIsOnOrBefore` veya `notIsOnOrAfter` alanının setter metodu bir değer (`null` dahil) ile çağrılır, THE TemporalFilter SHALL bu değeri ilgili alana atar ve üzerinde çağrıldığı TemporalFilter örneğinin aynısını döndürür.
5. WHEN yeni bir TemporalFilter örneği argümansız kurucu ile oluşturulur, THE TemporalFilter SHALL `notIsBefore`, `notIsAfter`, `notIsOnOrBefore` ve `notIsOnOrAfter` alanlarının değerini `null` olarak başlatır.

### Gereksinim 4: TemporalFilter olumsuz operatörlerin anlambilimi (semantics)

**Kullanıcı Hikayesi:** Bir geliştirici olarak, TemporalFilter olumsuz operatörlerinin ilgili pozitif operatörün mantıksal tersini uygulamasını istiyorum, böylece "önce değil", "sonra değil" gibi zaman koşullarını doğrudan ifade edebilirim.

#### Kabul Kriterleri

1. WHEN `notIsBefore` operatörü boş olmayan (null olmayan) bir Karsilastirma_Degeri ile Zaman_Tipi (`LocalDate`, `LocalDateTime`, `Instant`) bir alana uygulanır, THE StaticSpecificationServiceGenerator SHALL Alan_Degeri'nin Karsilastirma_Degeri'nden önce OLMADIĞI (`!isBefore`) kayıtları `true`, diğer tüm kayıtları `false` olarak değerlendiren doğrulama mantığı üretir.
2. WHEN `notIsAfter` operatörü boş olmayan bir Karsilastirma_Degeri ile Zaman_Tipi bir alana uygulanır, THE StaticSpecificationServiceGenerator SHALL Alan_Degeri'nin Karsilastirma_Degeri'nden sonra OLMADIĞI (`!isAfter`) kayıtları `true`, diğer tüm kayıtları `false` olarak değerlendiren doğrulama mantığı üretir.
3. WHEN `notIsOnOrBefore` operatörü boş olmayan bir Karsilastirma_Degeri ile Zaman_Tipi bir alana uygulanır, THE StaticSpecificationServiceGenerator SHALL Alan_Degeri'nin Karsilastirma_Degeri'nden sonra olduğu (`isAfter`) kayıtları `true`, diğer tüm kayıtları `false` olarak değerlendiren doğrulama mantığı üretir.
4. WHEN `notIsOnOrAfter` operatörü boş olmayan bir Karsilastirma_Degeri ile Zaman_Tipi bir alana uygulanır, THE StaticSpecificationServiceGenerator SHALL Alan_Degeri'nin Karsilastirma_Degeri'nden önce olduğu (`isBefore`) kayıtları `true`, diğer tüm kayıtları `false` olarak değerlendiren doğrulama mantığı üretir.
5. WHERE bir TemporalFilter olumsuz operatörü Zaman_Tipi olmayan (`isBefore`/`isAfter` metotları bulunmayan) bir Comparable alana uygulanır, THE StaticSpecificationServiceGenerator SHALL karşılaştırmayı `compareTo` üzerinden şu eşleştirmeyle üretir: `notIsBefore` için `compareTo >= 0`, `notIsAfter` için `compareTo <= 0`, `notIsOnOrBefore` için `compareTo > 0`, `notIsOnOrAfter` için `compareTo < 0`.
6. IF bir TemporalFilter olumsuz operatörü uygulandığında Alan_Degeri `null` ise, THEN THE StaticSpecificationServiceGenerator SHALL mevcut pozitif karşılaştırma operatörleriyle tutarlı biçimde ilgili kaydı eşleşmemiş (`false`) olarak değerlendirir.
7. IF bir TemporalFilter olumsuz operatörünün Karsilastirma_Degeri `null` ise, THEN THE StaticSpecificationServiceGenerator SHALL mevcut pozitif operatörlerle tutarlı biçimde davranır ve karşılaştırma sırasında istisna (exception) fırlatmaz.

### Gereksinim 5: TemporalFilter alt sınıf setter ezmeleri (override) ile bütünleşme

**Kullanıcı Hikayesi:** Bir geliştirici olarak, `InstantFilter`, `LocalDateFilter` ve `LocalDateTimeFilter` üzerinde olumsuz operatör setter'larının kendi somut tipini döndürmesini istiyorum, böylece metot zincirleme (fluent) kullanım alt sınıf tipini korur.

#### Kabul Kriterleri

1. THE InstantFilter SHALL `setNotIsBefore`, `setNotIsAfter`, `setNotIsOnOrBefore` ve `setNotIsOnOrAfter` metotlarını ezer (override) ve her biri `InstantFilter` tipinde bir örnek döndürür.
2. THE LocalDateFilter SHALL `setNotIsBefore`, `setNotIsAfter`, `setNotIsOnOrBefore` ve `setNotIsOnOrAfter` metotlarını ezer (override) ve her biri `LocalDateFilter` tipinde bir örnek döndürür.
3. THE LocalDateTimeFilter SHALL `setNotIsBefore`, `setNotIsAfter`, `setNotIsOnOrBefore` ve `setNotIsOnOrAfter` metotlarını ezer (override) ve her biri `LocalDateTimeFilter` tipinde bir örnek döndürür.
4. WHEN bir alt sınıfın olumsuz operatör setter'ı çağrılır, THE ilgili alt sınıf SHALL üst sınıf (TemporalFilter) üzerindeki karşılık gelen alanı ayarlar ve üzerinde çağrıldığı alt sınıf örneğinin aynısını döndürür.

### Gereksinim 6: Göreli zaman ön ayarlarının (last/next) kapsam dışında bırakılması

**Kullanıcı Hikayesi:** Bir geliştirici olarak, olumsuz operatörlerin yalnızca doğrudan karşılaştırma operatörlerine uygulanmasını istiyorum, böylece anlambilimi belirsiz olan göreli ön ayarlar için yanıltıcı bir olumsuzlama davranışı ortaya çıkmaz.

#### Kabul Kriterleri

1. THE TemporalFilter SHALL `last` ve `next` (TemporalPreset) alanları için olumsuz operatör alanı, setter'ı veya JSON kısaltması tanımlamaz.
2. WHEN bir TemporalFilter'da `last` veya `next` alanı ayarlanır, THE TemporalFilter SHALL bu alanlar için değişiklik öncesiyle aynı davranışı sürdürür.

### Gereksinim 7: Kopyalama kurucusu ile bütünleşme

**Kullanıcı Hikayesi:** Bir geliştirici olarak, bir filtre örneğini kopyaladığımda olumsuz operatör değerlerinin de kopyalanmasını istiyorum, böylece filtrenin tamamı güvenilir biçimde çoğaltılır.

#### Kabul Kriterleri

1. WHEN bir NumberFilter, kopyalama kurucusu ile başka bir NumberFilter örneğinden oluşturulur, THE NumberFilter SHALL kaynak örneğin `notGreaterThan`, `notLessThan`, `notGreaterOrEqualThan` ve `notLessOrEqualThan` değerlerini yeni örneğe atar.
2. WHEN bir TemporalFilter, kopyalama kurucusu ile başka bir TemporalFilter örneğinden oluşturulur, THE TemporalFilter SHALL kaynak örneğin `notIsBefore`, `notIsAfter`, `notIsOnOrBefore` ve `notIsOnOrAfter` değerlerini yeni örneğe atar.

### Gereksinim 8: equals ve hashCode ile bütünleşme

**Kullanıcı Hikayesi:** Bir geliştirici olarak, iki filtre örneğinin eşitlik karşılaştırmasının olumsuz operatörleri de dikkate almasını istiyorum, böylece eşitlik ve karma (hash) davranışı doğru kalır.

#### Kabul Kriterleri

1. WHEN iki NumberFilter örneği `equals` ile karşılaştırılır, THE NumberFilter SHALL `notGreaterThan`, `notLessThan`, `notGreaterOrEqualThan` ve `notLessOrEqualThan` alanlarının değerlerini eşitlik belirlemesine dahil eder.
2. WHEN bir NumberFilter örneğinin `hashCode` değeri hesaplanır, THE NumberFilter SHALL `notGreaterThan`, `notLessThan`, `notGreaterOrEqualThan` ve `notLessOrEqualThan` alanlarının değerlerini hesaplamaya dahil eder.
3. WHEN iki TemporalFilter örneği `equals` ile karşılaştırılır, THE TemporalFilter SHALL `notIsBefore`, `notIsAfter`, `notIsOnOrBefore` ve `notIsOnOrAfter` alanlarının değerlerini eşitlik belirlemesine dahil eder.
4. WHEN bir TemporalFilter örneğinin `hashCode` değeri hesaplanır, THE TemporalFilter SHALL `notIsBefore`, `notIsAfter`, `notIsOnOrBefore` ve `notIsOnOrAfter` alanlarının değerlerini hesaplamaya dahil eder.
5. WHERE iki filtre örneği `equals` karşılaştırmasında eşit kabul edilir, THE ilgili filtre SHALL bu iki örnek için aynı `hashCode` değerini üretir.

### Gereksinim 9: toString ile bütünleşme

**Kullanıcı Hikayesi:** Bir geliştirici olarak, bir filtrenin metin gösteriminde olumsuz operatör değerlerini görmek istiyorum, böylece hata ayıklama ve günlükleme sırasında filtrenin tam durumunu inceleyebilirim.

#### Kabul Kriterleri

1. WHEN bir NumberFilter örneğinin `toString` metodu çağrılır, THE NumberFilter SHALL çıktı metninde `notGreaterThan`, `notLessThan`, `notGreaterOrEqualThan` ve `notLessOrEqualThan` alanlarının adlarını ve değerlerini içerir.
2. WHEN bir TemporalFilter örneğinin `toString` metodu çağrılır, THE TemporalFilter SHALL çıktı metninde `notIsBefore`, `notIsAfter`, `notIsOnOrBefore` ve `notIsOnOrAfter` alanlarının adlarını ve değerlerini içerir.

### Gereksinim 10: FilterConstants alan adı sabitleri

**Kullanıcı Hikayesi:** Bir geliştirici olarak, olumsuz operatörlerin JSON alan adlarının paylaşılan sabitler olarak tanımlanmasını istiyorum, böylece çözümleyici ve üreticiler bu adları tek bir kaynaktan tutarlı biçimde kullanır.

#### Kabul Kriterleri

1. THE FilterConstants SHALL `ngt` değeri için `FIELD_NGT`, `nlt` değeri için `FIELD_NLT`, `ngte` değeri için `FIELD_NGTE`, `nlte` değeri için `FIELD_NLTE` adlı sabitleri tanımlar.
2. THE FilterConstants SHALL `nbe` değeri için `FIELD_NOT_BEFORE`, `naf` değeri için `FIELD_NOT_AFTER`, `nobe` değeri için `FIELD_NOT_ON_OR_BEFORE`, `noaf` değeri için `FIELD_NOT_ON_OR_AFTER` adlı sabitleri tanımlar.

### Gereksinim 11: Operator enum değerleri

**Kullanıcı Hikayesi:** Bir geliştirici olarak, olumsuz operatörlerin sistemin operatör kümesinde ilk sınıf değerler olarak yer almasını istiyorum, böylece doğrulama ve eşleştirme mantığı bu operatörleri tanır.

#### Kabul Kriterleri

1. THE Operator SHALL `notGreaterThan` için `NOT_GREATER_THAN`, `notLessThan` için `NOT_LESS_THAN`, `notGreaterOrEqualThan` için `NOT_GREATER_OR_EQUAL_THAN`, `notLessOrEqualThan` için `NOT_LESS_OR_EQUAL_THAN` adlı enum değerlerini tanımlar.
2. THE Operator SHALL `notIsBefore` için `NOT_IS_BEFORE`, `notIsAfter` için `NOT_IS_AFTER`, `notIsOnOrBefore` için `NOT_IS_ON_OR_BEFORE`, `notIsOnOrAfter` için `NOT_IS_ON_OR_AFTER` adlı enum değerlerini tanımlar.
3. THE StaticSpecificationServiceGenerator SHALL sayısal (numeric) alan tiplerinin desteklediği operatör kümesine `NOT_GREATER_THAN`, `NOT_LESS_THAN`, `NOT_GREATER_OR_EQUAL_THAN` ve `NOT_LESS_OR_EQUAL_THAN` operatörlerini dahil eder.
4. THE StaticSpecificationServiceGenerator SHALL zaman/tarih (date/time) alan tiplerinin desteklediği operatör kümesine `NOT_IS_BEFORE`, `NOT_IS_AFTER`, `NOT_IS_ON_OR_BEFORE` ve `NOT_IS_ON_OR_AFTER` operatörlerini dahil eder.
5. THE StaticSpecificationServiceGenerator SHALL her NumberFilter olumsuz operatörü için, ilgili doğrulama metodu adında kullanılan bir metot son eki (`NotGreaterThan`, `NotLessThan`, `NotGreaterOrEqualThan`, `NotLessOrEqualThan`) üretir.
6. THE StaticSpecificationServiceGenerator SHALL her TemporalFilter olumsuz operatörü için, ilgili doğrulama metodu adında kullanılan bir metot son eki (`NotIsBefore`, `NotIsAfter`, `NotIsOnOrBefore`, `NotIsOnOrAfter`) üretir.

### Gereksinim 12: JSON çözümleme (deserialization) desteği

**Kullanıcı Hikayesi:** Bir geliştirici olarak, JSON gövdesinde olumsuz operatörlerin kısaltmalarını kullanarak filtreleri belirtebilmek istiyorum, böylece filtreleri istemci taraflı istekler üzerinden gönderebilirim.

#### Kabul Kriterleri

1. WHEN JSON gövdesinde `ngt` alanı NumberFilter'ın alan tipine (`F extends Comparable`) dönüştürülebilen bir sayısal değer ile bulunur, THE FilterDeserializerGenerator SHALL bu değeri alan tipine dönüştürüp NumberFilter üzerindeki `notGreaterThan` setter metoduna atayan çözümleme kodu üretir.
2. WHEN JSON gövdesinde `nlt` alanı NumberFilter'ın alan tipine dönüştürülebilen bir sayısal değer ile bulunur, THE FilterDeserializerGenerator SHALL bu değeri alan tipine dönüştürüp NumberFilter üzerindeki `notLessThan` setter metoduna atayan çözümleme kodu üretir.
3. WHEN JSON gövdesinde `ngte` alanı NumberFilter'ın alan tipine dönüştürülebilen bir sayısal değer ile bulunur, THE FilterDeserializerGenerator SHALL bu değeri alan tipine dönüştürüp NumberFilter üzerindeki `notGreaterOrEqualThan` setter metoduna atayan çözümleme kodu üretir.
4. WHEN JSON gövdesinde `nlte` alanı NumberFilter'ın alan tipine dönüştürülebilen bir sayısal değer ile bulunur, THE FilterDeserializerGenerator SHALL bu değeri alan tipine dönüştürüp NumberFilter üzerindeki `notLessOrEqualThan` setter metoduna atayan çözümleme kodu üretir.
5. WHEN JSON gövdesinde `nbe` alanı TemporalFilter'ın alan tipine (`F extends Comparable`) dönüştürülebilen bir zaman/tarih değeri ile bulunur, THE FilterDeserializerGenerator SHALL bu değeri alan tipine dönüştürüp TemporalFilter üzerindeki `notIsBefore` setter metoduna atayan çözümleme kodu üretir.
6. WHEN JSON gövdesinde `naf` alanı TemporalFilter'ın alan tipine dönüştürülebilen bir zaman/tarih değeri ile bulunur, THE FilterDeserializerGenerator SHALL bu değeri alan tipine dönüştürüp TemporalFilter üzerindeki `notIsAfter` setter metoduna atayan çözümleme kodu üretir.
7. WHEN JSON gövdesinde `nobe` alanı TemporalFilter'ın alan tipine dönüştürülebilen bir zaman/tarih değeri ile bulunur, THE FilterDeserializerGenerator SHALL bu değeri alan tipine dönüştürüp TemporalFilter üzerindeki `notIsOnOrBefore` setter metoduna atayan çözümleme kodu üretir.
8. WHEN JSON gövdesinde `noaf` alanı TemporalFilter'ın alan tipine dönüştürülebilen bir zaman/tarih değeri ile bulunur, THE FilterDeserializerGenerator SHALL bu değeri alan tipine dönüştürüp TemporalFilter üzerindeki `notIsOnOrAfter` setter metoduna atayan çözümleme kodu üretir.
9. IF JSON gövdesindeki `ngt`, `nlt`, `ngte`, `nlte`, `nbe`, `naf`, `nobe` veya `noaf` alanlarından herhangi biri ilgili filtrenin alan tipine dönüştürülemeyen bir değer (örneğin sayısal olmayan metin, boolean veya geçersiz tarih biçimi) içerir, THEN THE FilterDeserializerGenerator SHALL çalışma anında hatalı alanı belirten bir çözümleme hatası (deserialization exception) fırlatan ve ilgili setter'ı çağırmayan çözümleme kodu üretir.
10. WHEN JSON gövdesinde `ngt`, `nlt`, `ngte`, `nlte`, `nbe`, `naf`, `nobe` veya `noaf` alanlarından herhangi biri bulunmaz veya `null` değeri ile bulunur, THE FilterDeserializerGenerator SHALL o alana karşılık gelen setter metodunu çağırmayan ve ilgili filtre alanını atanmamış (null) bırakan çözümleme kodu üretir.

### Gereksinim 13: Sorgu parametresi (query parameter) bağlama desteği

**Kullanıcı Hikayesi:** Bir geliştirici olarak, sorgu parametreleri üzerinden olumsuz operatörleri kullanabilmek istiyorum, böylece olumsuz filtreleri URL üzerinden de uygulayabilirim.

#### Kabul Kriterleri

1. WHEN `alan.ngt` biçiminde bir sayısal sorgu parametresi alınır, THE FilterDeserializerGenerator SHALL değeri ilgili NumberFilter'ın `notGreaterThan` setter metoduna bağlayan kod üretir.
2. WHEN `alan.nlt` biçiminde bir sayısal sorgu parametresi alınır, THE FilterDeserializerGenerator SHALL değeri ilgili NumberFilter'ın `notLessThan` setter metoduna bağlayan kod üretir.
3. WHEN `alan.ngte` biçiminde bir sayısal sorgu parametresi alınır, THE FilterDeserializerGenerator SHALL değeri ilgili NumberFilter'ın `notGreaterOrEqualThan` setter metoduna bağlayan kod üretir.
4. WHEN `alan.nlte` biçiminde bir sayısal sorgu parametresi alınır, THE FilterDeserializerGenerator SHALL değeri ilgili NumberFilter'ın `notLessOrEqualThan` setter metoduna bağlayan kod üretir.
5. WHEN `alan.nbe` biçiminde bir zaman/tarih sorgu parametresi alınır, THE FilterDeserializerGenerator SHALL değeri ilgili TemporalFilter'ın `notIsBefore` setter metoduna bağlayan kod üretir.
6. WHEN `alan.naf` biçiminde bir zaman/tarih sorgu parametresi alınır, THE FilterDeserializerGenerator SHALL değeri ilgili TemporalFilter'ın `notIsAfter` setter metoduna bağlayan kod üretir.
7. WHEN `alan.nobe` biçiminde bir zaman/tarih sorgu parametresi alınır, THE FilterDeserializerGenerator SHALL değeri ilgili TemporalFilter'ın `notIsOnOrBefore` setter metoduna bağlayan kod üretir.
8. WHEN `alan.noaf` biçiminde bir zaman/tarih sorgu parametresi alınır, THE FilterDeserializerGenerator SHALL değeri ilgili TemporalFilter'ın `notIsOnOrAfter` setter metoduna bağlayan kod üretir.

### Gereksinim 14: Filtre doğrulaması ile bütünleşme

**Kullanıcı Hikayesi:** Bir geliştirici olarak, bir filtrede olumsuz operatörler ayarlandığında bu operatörlerin varlık (entity) doğrulaması sırasında değerlendirilmesini istiyorum, böylece filtre bir bütün olarak doğru sonuç verir.

#### Kabul Kriterleri

1. WHILE bir NumberFilter içindeki bir olumsuz operatör alanı `null` olmayan bir değere sahiptir, THE StaticSpecificationServiceGenerator SHALL üretilen `validateFilter` mantığında bu alan için karşılık gelen doğrulama metodunu çağırır.
2. WHILE bir TemporalFilter içindeki bir olumsuz operatör alanı `null` olmayan bir değere sahiptir, THE StaticSpecificationServiceGenerator SHALL üretilen `validateFilter` mantığında bu alan için karşılık gelen doğrulama metodunu çağırır.
3. IF bir olumsuz operatör alanının doğrulaması başarısız olur, THEN THE StaticSpecificationServiceGenerator SHALL üretilen `validateFilter` mantığında ilgili varlık için `false` döndürür.
4. WHERE bir NumberFilter içinde hem pozitif hem olumsuz operatör alanları eşzamanlı olarak `null` olmayan değerlere sahiptir, THE StaticSpecificationServiceGenerator SHALL üretilen doğrulama mantığında tüm bu operatörlerin birlikte (mantıksal VE) sağlanmasını gerektirir.
5. WHERE bir TemporalFilter içinde hem pozitif hem olumsuz operatör alanları eşzamanlı olarak `null` olmayan değerlere sahiptir, THE StaticSpecificationServiceGenerator SHALL üretilen doğrulama mantığında tüm bu operatörlerin birlikte (mantıksal VE) sağlanmasını gerektirir.

### Gereksinim 15: Mevcut davranışın korunması (geriye dönük uyumluluk)

**Kullanıcı Hikayesi:** Bir geliştirici olarak, yeni olumsuz operatörlerin mevcut pozitif operatörlerin ve göreli ön ayarların davranışını değiştirmemesini istiyorum, böylece mevcut filtreler ve istekler etkilenmeden çalışmaya devam eder.

#### Kabul Kriterleri

1. WHEN bir NumberFilter'da yalnızca mevcut pozitif operatörler (`greaterThan`, `lessThan`, `greaterOrEqualThan`, `lessOrEqualThan`) kullanılır, THE NumberFilter SHALL bu operatörler için değişiklik öncesiyle aynı eşleştirme sonuçlarını üretir.
2. WHEN bir TemporalFilter'da yalnızca mevcut pozitif operatörler (`isBefore`, `isAfter`, `isOnOrBefore`, `isOnOrAfter`) veya göreli ön ayarlar (`last`, `next`) kullanılır, THE TemporalFilter SHALL bu operatörler için değişiklik öncesiyle aynı eşleştirme sonuçlarını üretir.
3. WHERE bir olumsuz operatör alanı ayarlanmamış (`null`) durumdadır, THE StaticSpecificationServiceGenerator SHALL üretilen doğrulama mantığında o operatörü değerlendirmeye almaz.

### Gereksinim 16: Kapsam (coverage) testlerinin mevcut filtre test sınıflarına eklenmesi

**Kullanıcı Hikayesi:** Bir geliştirici olarak, yeni olumsuz operatörlere ait kapsam (coverage) testlerinin ayrı/yeni test dosyaları oluşturmak yerine mevcut filtre test sınıflarına eklenmesini istiyorum, böylece test paketi tek ve tutarlı bir yapıda kalır ve yeni operatörler mevcut test kurgusuyla aynı yerde doğrulanır.

#### Kabul Kriterleri

1. THE Test_Paketi SHALL NumberFilter olumsuz operatörlerine (`notGreaterThan`/`ngt`, `notLessThan`/`nlt`, `notGreaterOrEqualThan`/`ngte`, `notLessOrEqualThan`/`nlte`) ait kapsam testlerini mevcut `DoubleFilterTest`, `IntegerFilterTest` ve `LongFilterTest` sınıflarına ekler; bu operatörler için ayrı veya yeni bir test dosyası oluşturmaz.
2. THE Test_Paketi SHALL TemporalFilter olumsuz operatörlerine (`notIsBefore`/`nbe`, `notIsAfter`/`naf`, `notIsOnOrBefore`/`nobe`, `notIsOnOrAfter`/`noaf`) ait kapsam testlerini mevcut `InstantFilterTest`, `LocalDateFilterTest`, `LocalDateTimeFilterTest` ve/veya `TemporalFilterTest` sınıflarına ekler; bu operatörler için ayrı veya yeni bir test dosyası oluşturmaz.
3. WHEN bir filtre tipi için eklenen kapsam testleri çalıştırılır, THE Test_Paketi SHALL o filtre tipinin dört yeni olumsuz alanının getter davranışını ve zincirlenebilir setter (Chained_Setter) davranışını doğrular.
4. WHEN bir filtre tipi için eklenen kapsam testleri çalıştırılır, THE Test_Paketi SHALL argümansız kurucu ile oluşturulan örnekte dört yeni olumsuz alanın `null` olarak başlatıldığını doğrular.
5. WHEN bir filtre tipi için eklenen kapsam testleri çalıştırılır, THE Test_Paketi SHALL kopyalama kurucusunun dört yeni olumsuz alanın değerlerini kaynak örnekten yeni örneğe taşıdığını doğrular.
6. WHEN bir filtre tipi için eklenen kapsam testleri çalıştırılır, THE Test_Paketi SHALL dört yeni olumsuz alanın `equals` ve `hashCode` sonuçlarına dahil edildiğini doğrular.
7. WHEN bir filtre tipi için eklenen kapsam testleri çalıştırılır, THE Test_Paketi SHALL dört yeni olumsuz alanın adlarının ve değerlerinin `toString` çıktısında yer aldığını doğrular.
8. WHEN bir filtre tipi için eklenen kapsam testleri çalıştırılır, THE Test_Paketi SHALL her olumsuz operatörün, Gereksinim 2 ve Gereksinim 4'te tanımlanan olumsuzlama anlambilimiyle tutarlı eşleştirme davranışını doğrular.
9. IF eklenen kapsam testlerinde bir olumsuz operatör uygulandığında Alan_Degeri `null` ise, THEN THE Test_Paketi SHALL ilgili kaydın eşleşmemiş (`false`) olarak değerlendirildiğini doğrular.
10. WHERE bir TemporalFilter alt sınıfı (`InstantFilter`, `LocalDateFilter`, `LocalDateTimeFilter`) Zaman_Tipi tabanlı `isBefore`/`isAfter` değerlendirmesi kullanır, THE Test_Paketi SHALL eklenen kapsam testlerinde bu değerlendirmeyi ilgili alt sınıfın test sınıfında doğrular.
11. WHERE test paketi özellik tabanlı test (jqwik) kurallarını kullanır, THE Test_Paketi SHOULD eklenen kapsam testlerinde filtre test paketinde halihazırda kullanılan özellik tabanlı test kurallarını uygun olan yerlerde takip eder.
12. WHEN yeni olumsuz operatör kapsam testleri mevcut test sınıflarına eklenir, THE Test_Paketi SHALL bu sınıflarda halihazırda bulunan mevcut doğrulamaları (assertion) korur ve yalnızca yeni testler ekleyerek (additive) değişiklik yapar.
