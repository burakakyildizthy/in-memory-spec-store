# Todo — collection-mapping-nesting-fix (Bugfix Spec: Requirements Phase)

## Context
Bug: Collection mapping'de kaynak (from) ve hedef (target) alanların ikisi de koleksiyon
olduğunda, kaynak liste hedef koleksiyonun içine tek eleman olarak ekleniyor
(collection-within-collection / iç içe büyüme). Beklenen: kaynak bütün bir koleksiyon ise
tüm koleksiyon doğrudan set edilmeli; tek eleman seçildiyse koleksiyona eklenmeli.

## Plan
- [x] Codebase araştırması: mapping engine ve collection set mantığını bul
- [x] Kök nedeni doğrula (BaseSpecificationService.handleCollectionField + addValueToCollection her zaman `collection.add(value)` yapıyor; MappingApplicator.applyCollectionMapping'te kısmi workaround var)
- [x] Mevcut spec konvansiyonlarını incele (.kiro/specs/*/bugfix.md, .config.kiro)
- [x] `.kiro/specs/collection-mapping-nesting-fix/.config.kiro` oluştur
- [x] `.kiro/specs/collection-mapping-nesting-fix/bugfix.md` oluştur (Türkçe, bug condition metodolojisi)
- [ ] Kullanıcı review — sonraki faza (design) geçiş kullanıcı onayıyla

## Root Cause (özet)
- `BaseSpecificationService.handleCollectionField(...)` → daima `collection.add(elementToAdd)`
- `BaseSpecificationService.addValueToCollection(...)` (ALL selector yolu) → daima `collection.add(value)`
- `MappingApplicator.applyCollectionMapping(...)` → yalnızca tek seviyeli `MANY_TO_ONE_COLLECTION`
  koleksiyon hedeflerinde `setFieldValue` ile bypass eden kısmi workaround.
- Kod yorumu hatayı doğruluyor: `collection.add(entireList) → [[items]]`.

## Review
- Requirements fazı tamamlandı: bugfix.md + .config.kiro yazıldı. Design/tasks fazları
  kullanıcı onayı sonrası ayrı adımlarda ele alınacak.
