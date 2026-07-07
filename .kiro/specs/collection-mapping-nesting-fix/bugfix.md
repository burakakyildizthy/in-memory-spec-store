# Bugfix Requirements Document

## Introduction

Eşleştirme (mapping) motoru, bir kaynak (from) alandaki veriyi hedef (target) alana kopyalar.
Kaynak ve hedef alanların her ikisi de koleksiyon (collection) tipinde olduğunda iki farklı
davranış beklenir:

- **Durum 1 — Koleksiyondan koleksiyona:** Kaynak, hedef koleksiyonla aynı eleman tipine
  sahip bir koleksiyon olarak seçildiğinde, kaynak koleksiyonun tamamı doğrudan hedef
  koleksiyon olarak atanmalıdır (koleksiyonun kendisi set edilmeli).
- **Durum 2 — Elemandan koleksiyona:** Kaynak koleksiyonun tamamı değil de içinden tek bir
  eleman seçilip hedefe verildiğinde, bu eleman hedef koleksiyona eklenmelidir (append).

Mevcut durumda motor bu iki durumu ayırt etmiyor; koleksiyon tipindeki bir hedefe atama
yaparken her koşulda değeri "eleman olarak ekliyor". Bunun sonucunda kaynak bir koleksiyon
olduğunda tüm liste hedef koleksiyonun içine tek bir eleman olarak konuluyor ve koleksiyon
içinde koleksiyon (iç içe / nested) yapısı oluşuyor. Aynı eşleştirme her senkronizasyonda
tekrar uygulandığında bu iç içe yapı büyümeye devam ediyor.

Bu hata; hedef verinin yanlış yapıda oluşmasına, koleksiyon erişimlerinin/sorgularının hatalı
sonuç vermesine ve tekrarlanan senkronizasyonlarda verinin sürekli (sınırsız) büyümesine yol
açıyor. Bu düzeltme, koleksiyondan koleksiyona atamada tüm koleksiyonun doğrudan set
edilmesini; tek eleman seçildiğinde ise elemanın koleksiyona eklenmesini sağlamalı ve
halihazırda doğru çalışan davranışları bozmamalıdır.

## Bug Analysis

### Current Behavior (Defect)

Kaynak bütün bir koleksiyon olarak çözümlendiğinde ve koleksiyon tipindeki bir hedef alana
atandığında, sistem koleksiyonun tamamını tek bir eleman gibi ele alır.

1.1 Bir eşleştirme kaynağı bütün bir koleksiyon olarak çözümleyip aynı eleman tipindeki
koleksiyon hedef alana atadığında, sistem kaynak koleksiyonun tamamını hedef koleksiyonun
içine tek bir eleman olarak ekler ve koleksiyon içinde koleksiyon (iç içe / nested) yapısı
oluşturur.

1.2 Aynı koleksiyondan koleksiyona eşleştirme (bkz. 1.1) aynı hedef için birden fazla kez
(örneğin her senkronizasyonda) uygulandığında, sistem kaynak koleksiyonu her seferinde yeni
bir iç içe eleman olarak yeniden ekler; böylece hedef koleksiyon her senkronizasyonda sınırsız
şekilde büyür.

1.3 Koleksiyon tipindeki hedef alana çok seviyeli (iç içe nesne üzerinden) bir yol ile
ulaşıldığında ve kaynak bir koleksiyon olduğunda, sistem yine kaynak koleksiyonu tek bir iç
içe eleman olarak sarmalar; koleksiyonu hedef koleksiyon olarak set etmez.

### Expected Behavior (Correct)

Kaynak bütün bir koleksiyon olduğunda, hedef koleksiyon kaynak koleksiyonun elemanlarını düz
(flat) olarak içermelidir.

2.1 Bir eşleştirme kaynağı bütün bir koleksiyon olarak çözümleyip aynı eleman tipindeki
koleksiyon hedef alana atadığında, sistem hedef koleksiyonu doğrudan kaynak koleksiyonun
elemanlarına (düz olarak) set etmelidir ve koleksiyonun içine koleksiyon koymamalıdır.

2.2 Aynı koleksiyondan koleksiyona eşleştirme (bkz. 2.1) aynı hedef için birden fazla kez
uygulandığında, sistem her seferinde aynı sonucu üretmelidir (idempotent); böylece hedef
koleksiyon sınırsız büyümeden güncel kaynağı yansıtmalıdır.

2.3 Koleksiyon tipindeki hedef alana çok seviyeli (iç içe nesne üzerinden) bir yol ile
ulaşıldığında ve kaynak bir koleksiyon olduğunda, sistem kaynak koleksiyonu tek seviyeli
durumla tutarlı biçimde doğrudan hedef koleksiyon olarak set etmelidir.

### Unchanged Behavior (Regression Prevention)

Halihazırda doğru çalışan atama davranışları korunmalıdır.

3.1 Kaynaktan tek bir eleman seçildiğinde (bir skaler alan ya da FIRST/LAST/ANY ile seçilmiş
tek eleman) ve koleksiyon tipindeki bir hedefe atandığında, sistem bu elemanı hedef
koleksiyona eklemeye devam etmelidir.

3.2 Hedef alan koleksiyon olmayan (skaler) bir tip olduğunda, sistem çözümlenen kaynak değeri
doğrudan bu alana atamaya devam etmelidir.

3.3 Bir agregasyon eşleştirmesi (SUM, AVG, COUNT, MIN, MAX) skaler bir hedefe sayısal sonuç
yazdığında, sistem agregasyon sonucunu değiştirmeden atamaya devam etmelidir.

3.4 Hedef koleksiyonun eleman tipi kendisi bir koleksiyon olduğunda ve kaynak tek bir iç
koleksiyon elemanı olarak çözümlendiğinde, sistem bu elemanı hedef koleksiyona eklemeye devam
etmelidir (meşru koleksiyon-içinde-koleksiyon durumu).

3.5 Hedef koleksiyon alanı atamadan önce null olduğunda, sistem koleksiyonu doldurmadan önce
başlatmaya (initialize) devam etmelidir.
