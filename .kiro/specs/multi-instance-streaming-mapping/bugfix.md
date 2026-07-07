# Bugfix Requirements Document

## Introduction

Kütüphane, streaming (canlı) veri kaynağından gelen güncellemeleri artımlı (incremental) olarak
işleyip ilgili store'lardaki hedef nesnelere (root entity / kayıt örneği) eşler (mapler). Aynı
streaming kaynağı, birden çok store tanımı tarafından tüketilebilir: farklı store'lar aynı streaming
kaynak key'i (foreign key / streaming key) üzerinden kendi hedef alanlarına mapleme yapabilir. Ayrıca
tek bir store içinde, gelen bir güncelleme birden çok kök kaydı (instance) aynı anda etkileyebilir.

Kullanıcı, aynı kaynak key üzerinden mapleme yapan **birden çok store tanımı** olduğunda, streaming
eşleştirmesinin yalnızca **ilk (1.) store'a** uygulandığını; **ikinci (2.) ve sonraki store'lara
hiçbir eşleştirmenin uygulanmadığını** bildirdi. Somut tekrarlama (reproduction) senaryosu: List→list
mapleme yapan iki ayrı store tanımı vardır; her ikisi de **aynı kaynak key** üzerinden ve hedefte
**aynı isim ve aynı tipteki** alana mapleme yapar. Sonuçta birinci store tamamen maplenir ve veri
alır; ikinci store'un hiçbir instance'ına veri eklenmez, alan tamamen boş kalır.

Kullanıcı ayrıca bu eksikliğin koleksiyona özgü olmayabileceğini; aynı kaynak key'i paylaşan birden
çok store söz konusu olduğunda diğer alan tiplerinde de (tekil/skaler alan, ONE_TO_ONE eşleştirme ya
da agregasyon sonucu) aynı sorunun görülebileceğini belirtti. Buna ek olarak, tek bir store içinde bir
streaming güncellemenin birden çok kök kaydı (instance) etkilemesi gerektiği durumlarda da benzer bir
eksik güncelleme (yalnızca ilk etkilenen instance'ın güncellenmesi) yaşanabileceği şüphesi vardır.

Kök neden hipotezi (davranışsal düzeyde): Hata, artımlı streaming eşleştirme yolunun **aynı kaynak
key'e bağlı birden çok hedefi** ele alış mantığında görünmektedir. Bir streaming güncelleme için o
key'e bağlı tüm store tanımlarının (ve her store içinde etkilenen tüm kök kayıtların) tek tek
işlenmesi gerekirken, güncelleme yalnızca ilk eşleşen hedefe uygulanıp diğerleri atlanmaktadır. Bu
davranış, alanın Collection olup olmamasından bağımsız görünmektedir ve yalnızca artımlı streaming
yolunu ilgilendirmektedir; tam (full/batch) senkronizasyon yolu bu senaryoda tüm store'ları/kayıtları
işlemeye devam etmektedir.

Bu hatanın etkisi: streaming güncelleme sonrası aynı kaynak key'e bağlı store'lardan okunan veride
yalnızca ilk store güncel değerlerle dolu; ikinci ve sonraki store'lar boş/eksik kalır. Tek store
içindeki çoklu instance durumunda ise bazı kayıtlar güncel, bazıları eski (stale) döner; yani çoklu
store ve çoklu instance senaryolarında veri tutarsız ve eksik kalır. Bu düzeltme, tek bir streaming
güncellemenin, aynı kaynak key'e bağlı tüm store'lara ve her store içinde etkilenen tüm instance'lara
güvenilir biçimde uygulanmasını sağlamalı ve halihazırda doğru çalışan davranışları (tek store / tek
instance mapleme, tam senkronizasyon, etkilenmeyen kayıtların korunması) bozmamalıdır.

## Bug Analysis

### Current Behavior (Defect)

Aynı streaming kaynak key'i üzerinden mapleme yapan birden çok store tanımı olduğunda, sistem
streaming eşleştirmesini yalnızca ilk store'a uygular; ikinci ve sonraki store'lar hiç maplenmez. Tek
bir store içinde birden çok kök kayıt etkilendiğinde de benzer bir eksik güncelleme görülebilir.

1.1 WHEN aynı streaming kaynak key'i üzerinden mapleme yapan birden çok store tanımı kayıtlıyken bir
streaming güncelleme geldiğinde THEN the system streaming eşleştirmesini yalnızca ilk (1.) store'a
uygular; aynı key'e bağlı ikinci (2.) ve sonraki store'lara hiçbir eşleştirme uygulamaz.

1.2 WHEN iki ayrı store da aynı kaynak key üzerinden ve hedefte aynı isim/aynı tipteki bir alana (ör.
List→list) mapleme yaptığında THEN the system yalnızca birinci store'un hedef alanına veri ekler;
ikinci store'un hiçbir instance'ına veri eklenmez ve alan tamamen boş kalır.

1.3 WHEN aynı çoklu-store senaryosu Collection dışı bir alan tipinde (tekil/skaler alan, ONE_TO_ONE
eşleştirme veya agregasyon sonucu) gerçekleştiğinde THEN the system yine yalnızca ilk store'u
günceller; hata koleksiyona özgü değildir ve aynı key'i paylaşan diğer store'ları ve alan tiplerini de
etkiler.

1.4 WHEN ikinci (veya sonraki) store üzerinden ilgili kayıtlar okunduğunda THEN the system hiç
maplenmediği için boş/eksik sonuç döndürür; yalnızca ilk store güncel değerleri içerir (çoklu store'ta
tutarsız ve eksik veri).

1.5 WHEN tek bir store içinde bir streaming güncelleme birden çok kök kaydı (instance) etkilemesi
gerektiğinde THEN the system eşleştirmeyi yalnızca etkilenen ilk instance'a uygular ve aynı store'daki
diğer etkilenen instance'ları güncellemeden (eski/eksik) bırakabilir.

### Expected Behavior (Correct)

Bir streaming güncelleme, aynı kaynak key'e bağlı TÜM store'lara ve her store içinde etkilenen TÜM
kök kayıtlara uygulanmalıdır.

2.1 WHEN aynı streaming kaynak key'i üzerinden mapleme yapan birden çok store tanımı kayıtlıyken bir
streaming güncelleme geldiğinde THEN the system SHALL streaming eşleştirmesini aynı key'e bağlı TÜM
store'lara (ilk, ikinci ve sonraki tümü) uygulamalıdır.

2.2 WHEN iki ayrı store da aynı kaynak key üzerinden ve hedefte aynı isim/aynı tipteki bir alana (ör.
List→list) mapleme yaptığında THEN the system SHALL her iki store'un da hedef alanını doğru şekilde ve
veriyle doldurmalıdır.

2.3 WHEN aynı çoklu-store senaryosu Collection dışı bir alan tipinde (tekil/skaler alan, ONE_TO_ONE
eşleştirme veya agregasyon sonucu) gerçekleştiğinde THEN the system SHALL aynı key'e bağlı tüm
store'ları alan tipinden bağımsız olarak güncellemelidir.

2.4 WHEN streaming güncelleme işlendikten sonra herhangi bir store (ilk, ikinci veya sonraki)
üzerinden ilgili kayıtlar okunduğunda THEN the system SHALL tüm store'ları güncel ve tutarlı
değerlerle döndürmelidir.

2.5 WHEN tek bir store içinde bir streaming güncelleme birden çok kök kaydı (instance) etkilemesi
gerektiğinde THEN the system SHALL aynı store içinde etkilenen tüm instance'lara eşleştirmeyi
uygulamalıdır.

### Unchanged Behavior (Regression Prevention)

Halihazırda doğru çalışan mapleme ve yayma (propagation) davranışları korunmalıdır.

3.1 WHEN bir streaming güncelleme yalnızca tek bir store'u (tek consumer) ve tek bir instance'ı
etkilediğinde THEN the system SHALL CONTINUE TO o tek store/instance eşleştirmesini bugünkü gibi doğru
şekilde yapmaya devam etmelidir.

3.2 WHEN tam (full/batch) senkronizasyon birden çok store'u veya instance'ı eşlediğinde THEN the
system SHALL CONTINUE TO tüm store'ları ve instance'ları doğru şekilde maplemeye devam etmelidir
(streaming olmayan yol bu düzeltmeden etkilenmemelidir).

3.3 WHEN bir streaming güncelleme geldiğinde ve mevcut bazı store'lar veya instance'lar bu
güncellemeden etkilenmiyorsa THEN the system SHALL CONTINUE TO etkilenmeyen store ve instance'ları
değiştirmeden korumaya devam etmelidir.

3.4 WHEN tek bir instance için maplenecek içerik hesaplandığında (koleksiyon elemanlarının tamamı,
FIRST/LAST/ANY ile seçilen eleman veya SUM/AVG/COUNT/MIN/MAX agregasyon değerleri) THEN the system
SHALL CONTINUE TO bu içeriği bugünkü gibi doğru üretmeye devam etmelidir.

3.5 WHEN streaming güncelleme sonrası sürümleme (version) ve consumer'a yayma (store güncelleme)
gerçekleştiğinde THEN the system SHALL CONTINUE TO mevcut sürümleme ve store güncelleme davranışını
korumaya devam etmelidir.
