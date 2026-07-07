# Bugfix Requirements Document

## Introduction

Eşleştirme (mapping) motorunda, bir grup değerin toplanıp doğrudan koleksiyon tipindeki bir
hedef alana yazıldığı senaryo (koleksiyondan koleksiyona / "many-to-one" koleksiyon eşleştirmesi —
kısaca **koleksiyonların doğrudan set edilmesi**) bulunuyor. Bu senaryoda toplanan değerlerin,
hedef koleksiyonun düz (flat) elemanları olarak yazılması beklenir.

"collection-mapping-nesting-fix" düzeltme çalışması ve onu takip eden düzeltme oturumundan sonra
bu **doğrudan set** yolu bozuldu. Daha önce toplanan değerleri hedef alana düz olarak yerleştiren
eşleştirmeler, artık etkilenen durumlarda toplanan grubun tamamını hedef koleksiyonun içine **tek
bir eleman** olarak sarmalıyor (iç içe / nested yapı) ya da bazı hedef koleksiyon tiplerinde hedefi
**hiç doldurmuyor**. Kullanıcı bunu "koleksiyonların doğrudan set edilmesi hiç çalışmamaya başladı;
önceden (hatalı da olsa) çalışan eşleştirme artık hiç çalışmıyor" şeklinde bildirdi.

Bu hatanın etkileri: hedef koleksiyonun yanlış yapıda (tek elemanlı, iç içe) oluşması; aynı
eşleştirme her senkronizasyonda tekrar uygulandığında hedef koleksiyonun sınırsız büyümesi; ve
belirli hedef koleksiyon tiplerinde koleksiyonun hiç set edilmemesi (boş/atanmamış kalması). Bu
düzeltme, koleksiyonların doğrudan set edilmesini yeniden güvenilir biçimde çalışır hale getirmeli
ve halihazırda doğru çalışan davranışları bozmamalıdır.

## Bug Analysis

### Current Behavior (Defect)

Bir grup toplanan değer koleksiyon tipindeki bir hedef alana doğrudan atandığında, sistem değerleri
düz elemanlar olarak yerleştirmek yerine bazı durumlarda tüm grubu tek bir eleman gibi ele alıyor
veya hedefi hiç doldurmuyor.

1.1 WHEN bir eşleştirme bir grup değeri toplayıp koleksiyon tipindeki bir hedef alana doğrudan
atadığında ve toplanan değerlerin tipleri hedef koleksiyonun bildirilen eleman tipiyle birebir aynı
değilse (örneğin farklı bir sayısal tipteki değerler ya da dönüştürülmesi gereken değerler) THEN the
system toplanan grubun tamamını hedef koleksiyonun içine tek bir eleman olarak sarmalar (iç içe yapı:
hedef koleksiyon, içinde tüm listeyi barındıran tek bir elemana sahip olur) ve değerleri düz elemanlar
olarak yerleştirmez.

1.2 WHEN 1.1'de tanımlanan eşleştirme aynı hedef için birden fazla kez (örneğin her
senkronizasyonda) uygulandığında THEN the system toplanan grubu her seferinde yeni bir iç içe eleman
olarak yeniden ekler; böylece hedef koleksiyon her uygulamada sınırsız şekilde büyür.

1.3 WHEN bir eşleştirme bir grup toplanan değeri, somut koleksiyon tipi düz bir liste olmayan
(örneğin bir Set) ve henüz başlatılmamış (null) koleksiyon tipindeki bir hedef alana doğrudan
atadığında THEN the system hedef koleksiyonu doldurmaz; alan eşleştirmeden sonra atanmamış/boş kalır.

### Expected Behavior (Correct)

Bir grup toplanan değer koleksiyon tipindeki bir hedef alana doğrudan atandığında, sistem değerleri
her zaman hedef koleksiyonun düz (flat) elemanları olarak yerleştirmelidir.

2.1 WHEN bir eşleştirme bir grup değeri toplayıp koleksiyon tipindeki bir hedef alana doğrudan
atadığında THEN the system SHALL toplanan değerleri hedef koleksiyonun tek tek (düz, tek seviyeli)
elemanları olarak yerleştirmeli; grubun tamamını tek bir eleman olarak sarmalamamalıdır.

2.2 WHEN 2.1'deki eşleştirme aynı hedef için birden fazla kez uygulandığında THEN the system SHALL
her seferinde aynı sonucu üretmeli (idempotent); böylece hedef koleksiyon sınırsız büyümeden güncel
kaynağı yansıtmalıdır.

2.3 WHEN bir eşleştirme bir grup toplanan değeri, somut tipi Set (veya düz liste olmayan başka bir
koleksiyon) olan ve başlangıçta null olabilen koleksiyon tipindeki bir hedef alana doğrudan atadığında
THEN the system SHALL hedefi toplanan değerlerle düz olarak doldurmalı ve alanın somut koleksiyon
tipini korumalıdır.

### Unchanged Behavior (Regression Prevention)

Halihazırda doğru çalışan atama davranışları korunmalıdır.

3.1 WHEN bir eşleştirme bir grup toplanan değeri, bildirilen eleman tipi toplanan değerlerle uyumlu
olan koleksiyon tipindeki bir hedef alana doğrudan atadığında (bugün doğru çalışan durum) THEN the
system SHALL CONTINUE TO değerleri düz olarak yerleştirmeye devam etmelidir.

3.2 WHEN koleksiyon tipindeki hedef alana çok seviyeli (iç içe nesne üzerinden) bir yol ile
ulaşıldığında THEN the system SHALL CONTINUE TO toplanan değerleri düz olarak ve tek seviyeli durumla
tutarlı biçimde yerleştirmeye devam etmelidir.

3.3 WHEN kaynaktan tek bir eleman seçildiğinde (bir skaler değer ya da FIRST/LAST/ANY ile seçilmiş tek
eleman) ve koleksiyon tipindeki bir hedefe atandığında THEN the system SHALL CONTINUE TO bu elemanı
hedef koleksiyona eklemeye (append) devam etmelidir.

3.4 WHEN hedef alan koleksiyon olmayan (skaler) bir tip olduğunda THEN the system SHALL CONTINUE TO
çözümlenen değeri doğrudan bu alana atamaya devam etmelidir.

3.5 WHEN bir agregasyon eşleştirmesi (SUM, AVG, COUNT, MIN, MAX) skaler bir hedefe sayısal sonuç
yazdığında THEN the system SHALL CONTINUE TO agregasyon sonucunu değiştirmeden atamaya devam
etmelidir.

3.6 WHEN hedef koleksiyonun eleman tipi kendisi bir koleksiyon olduğunda ve tek bir iç koleksiyon,
hedefe tek eleman olarak atandığında (meşru iç içe durum) THEN the system SHALL CONTINUE TO bu iç
koleksiyonu hedef koleksiyona tek eleman olarak eklemeye devam etmelidir.

3.7 WHEN düz listeyle uyumlu bir koleksiyon hedef alanı atamadan önce null olduğunda THEN the system
SHALL CONTINUE TO koleksiyonu doldurmadan önce başlatmaya (initialize) devam etmelidir.
