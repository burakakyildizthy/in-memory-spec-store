# Lessons

## collection-mapping-nesting-fix (Requirements)
- Koleksiyon hedeflere atamada "set (tüm koleksiyon)" ile "add (tek eleman)" davranışları
  ayrıştırılmalı. Ayırt edici sinyal: kaynağın bütün bir koleksiyon olarak mı yoksa tek bir
  eleman olarak mı çözümlendiği (ör. ALL / doğrudan koleksiyon alanı vs FIRST/LAST/ANY veya
  skaler yaprak).
- Kısmi workaround'lara dikkat: `MappingApplicator.applyCollectionMapping` yalnızca tek
  seviyeli `MANY_TO_ONE_COLLECTION` koleksiyon hedeflerini düzeltiyordu; asıl kök neden paylaşımlı
  atama mantığında (`BaseSpecificationService.handleCollectionField` / `addValueToCollection`).
  Requirements'ı belirli bir mapping tipine değil, gözlemlenen davranışa göre yaz.
- Bu repo'da bugfix spec konvansiyonu: `.kiro/specs/{feature}/bugfix.md` + `.config.kiro`,
  bölüm başlıkları İngilizce, maddeler X.Y numaralı.
