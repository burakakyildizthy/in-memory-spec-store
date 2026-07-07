# Missing Imports In Generated Classes Bugfix Design

## Overview

The annotation processor generates two helper classes per `@MetaModel` type: a static meta model
(`ClassName_`, via `StaticMetaModelGenerator`) and a filter class (`ClassNameFilter`, via
`FilterMetaModelGenerator`). Both classes refer to field-related types (nested model types, enum
types, and collection/map element types) by their **simple name**, so each generator must emit a
matching `import` for every referenced user type. When an import is missing and the type lives in a
different package, the generated source cannot resolve the simple name and the build fails.

Both generators share the same defective import-collection shape. The step that gathers imports for a
field (`StaticMetaModelGenerator.addImportsForField` and `FilterMetaModelGenerator.addFieldImports`):

1. Inspects only the **first** generic type argument (`typeArguments.get(0)`), so `Map<K, V>` value
   types and any second-or-later type parameter are never seen.
2. Does **not recurse** into nested generics, so element types buried inside `List<List<Order>>` or
   `List<Map<String, Order>>` are skipped.
3. Discards any type whose qualified name **starts with `java.util`**, applying that prefix test to
   the container type rather than to the element type. Because a `Map` field's declared type is
   `java.util.Map`, the whole field is dropped before its arguments are ever examined.

The fix replaces the ad-hoc, single-argument inspection with a single recursive walk over a field's
type and **all** of its type arguments, at every nesting depth. The walk decides *whether to import a
given type* (skip primitives, wrappers, temporals, `java.lang`/`java.util` container/utility types)
**independently** from *whether to recurse into that type's arguments* (always recurse). This lets a
`java.util.Map<String, com.acme.Order>` contribute an import for `com.acme.Order` without importing
`java.util.Map` itself.

The strategy is intentionally minimal and surgical: only the import-collection path changes. Field
attribute generation, filter type mapping, builder generation, inheritance traversal, and the base
imports each generator always adds remain untouched.

## Glossary

- **Bug_Condition (C)**: The condition that triggers the bug - a `@MetaModel` field references a
  user-defined model or enum type (directly, as a nested object, or as any generic type argument at
  any depth, including `Map` keys and values) that lives in a different package, and the generator
  fails to emit an import for that type.
- **Property (P)**: The desired behavior - the generated class contains an import for every
  user-defined model/enum type it references by simple name, so the generated source compiles.
- **Preservation**: Existing import behavior that already works - single-argument collection element
  imports, direct nested-object/enum imports, and the omission of redundant imports for primitives,
  wrappers, temporals, and `java.lang`/`java.util` container types - must remain unchanged.
- **addImportsForField**: The method in `StaticMetaModelGenerator` that adds imports for one field to
  the import set used by the `ClassName_` static meta model.
- **addFieldImports**: The method in `FilterMetaModelGenerator` that adds imports for one field to the
  import set used by the `ClassNameFilter` class (including the nested model's own `...Filter` import).
- **DeclaredType / TypeMirror**: `javax.lang.model` representations of a field's type; a `DeclaredType`
  exposes `getTypeArguments()`, the list of generic parameters (e.g. `[String, Order]` for
  `Map<String, Order>`).
- **Type argument**: A generic parameter of a parameterized type; may itself be parameterized
  (`List<Map<String, Order>>` has outer argument `Map<String, Order>`, which in turn has arguments
  `String` and `Order`).
- **User type**: A model or enum type that is not a primitive, wrapper, temporal (`java.time.*`),
  `java.lang.*`, or `java.util.*` type and therefore requires an explicit import.

## Bug Details

### Bug Condition

The bug manifests when the generator collects imports for a field whose referenced model/enum types
are not all reachable through the current single-first-argument, non-recursive, `java.util`-excluding
logic. The import-collection step is either looking at the wrong type argument (only the first),
failing to descend into nested generics, or discarding a type because its enclosing container lives
under `java.util`.

**Formal Specification:**
```
FUNCTION isBugCondition(field)
  INPUT: field of type VariableElement (a @MetaModel field)
  OUTPUT: boolean

  referencedUserTypes := allUserTypesReferencedBySimpleName(field)
      // every model/enum type reachable from field.type and ALL of its
      // type arguments, recursively, including Map keys AND values

  emittedImports := importsProducedByCurrentGenerator(field)

  RETURN EXISTS t IN referencedUserTypes SUCH THAT
             t.package != generatedClass.package
             AND t.qualifiedName NOT IN emittedImports
END FUNCTION
```

Equivalently, the bug is present whenever `referencedUserTypes` contains a type that the current code
misses because it (a) is not the first type argument, (b) is nested inside another generic argument,
or (c) sits inside a `java.util` container (most notably a `Map` value).

### Examples

- `Map<String, Order>` where `Order` is a `@MetaModel` in another package: expected an import for
  `Order` (and, for the filter class, `OrderFilter`); actual - no import emitted, so `Map<String,
  Order>` / `CollectionFilter<Order>` references fail to compile. (Cause: field's declared type is
  `java.util.Map`, excluded by the `startsWith("java.util")` guard before its arguments are read.)
- `List<Map<String, Order>>`: expected imports for `Order`; actual - only the outer first argument
  (`Map<String, Order>`) is looked at, its inner `Order` argument is never inspected.
- `List<List<Order>>`: expected an import for `Order`; actual - the outer first argument is
  `List<Order>` (a `java.util` type, excluded), and the nested `Order` is never reached.
- `Map<CustomerId, Order>` where both `CustomerId` and `Order` are user types in other packages:
  expected imports for both key and value; actual - the generator only ever considers the first
  argument even when it is not excluded.
- `List<Order>` where `Order` is in another package (edge case that already works): expected and
  actual both emit the `Order` import - this case must be preserved.

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Single-parameter collection element imports (for example `List<Order>` / `Set<Order>` where `Order`
  is a user type in a different package) must continue to be emitted, including the `OrderFilter`
  import that the filter generator adds today.
- Direct nested-object and enum imports for a field whose top-level type is a user type in a different
  package must continue to be emitted.
- Base/standard imports that each generator always adds (the attribute wildcard import for the static
  model; the `java.util`, `java.time`, Jackson, and filter imports for the filter class) must remain
  exactly as they are.
- Field attribute generation, filter type determination, builder generation, deserializer generation,
  and inherited-field traversal from `@MetaModel` superclasses must remain unchanged.

**Scope:**
All inputs whose complete set of referenced user types is already imported by the current logic must
produce a byte-for-byte identical import set. This specifically includes:
- Fields whose type is a primitive, wrapper, `String`, temporal (`java.time.*`), or enum handled by
  existing branches.
- Fields whose type or type arguments are `java.lang.*` / `java.util.*` container/utility types that
  require no user-package import.
- Single-argument collections and direct nested objects/enums that already resolve correctly.

The corrected expected behavior for buggy inputs is defined in the Correctness Properties section
(Property 1); this section captures only what must not change.

## Hypothesized Root Cause

Based on the bug description and the current source of `addImportsForField`
(`StaticMetaModelGenerator`) and `addFieldImports` (`FilterMetaModelGenerator`), the defects are:

1. **First-argument-only inspection**: Both methods read `typeArguments.get(0)` and ignore the rest of
   the list. For a `Map<K, V>` this permanently discards `V`; for any multi-argument generic it
   discards every argument after the first.

2. **No recursion into nested generics**: The type-argument inspection is a single, flat step. A type
   argument that is itself parameterized (`Map<String, Order>` inside a `List`, or `List<Order>` inside
   another `List`) is treated as a leaf, so element types nested one or more levels deep are never
   collected.

3. **Container-prefix exclusion applied too early / too broadly**: The guard
   `qualifiedName.startsWith("java.util")` (and `startsWith("java.lang")`) is meant to avoid importing
   utility/container types, but it is evaluated against the outer type and short-circuits the whole
   field. Because a `Map` field's declared type is `java.util.Map`, the field is dropped before its
   arguments are examined, so `Map` key/value model types are never imported. The exclusion should
   govern only *whether a specific type is added as an import*, not *whether traversal continues into
   its arguments*.

4. **Collection-only element handling**: The element-import branch runs only when
   `isCollectionType(fieldType)` is true. `Map` is not a collection type in either generator's
   `COLLECTION_TYPES`/assignability check, so map arguments are never routed through element handling
   at all.

## Correctness Properties

Property 1: Bug Condition - Complete Import Collection Across All Nested Type Arguments

_For any_ input where the bug condition holds (isBugCondition returns true), the fixed generator SHALL
emit an import for every user-defined model/enum type referenced by the field - whether the type
appears directly, as a nested object, or as any generic type argument at any nesting depth (including
both `Map` keys and values) - resolving each type by its fully qualified name across packages, so that
the generated class compiles. For the filter generator, when such a referenced type is itself a
`@MetaModel`, the fixed generator SHALL additionally emit the corresponding `...Filter` import as it
does today for direct and single-argument-collection nested models.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**

Property 2: Preservation - Existing Import Sets Unchanged For Already-Correct Fields

_For any_ input where the bug condition does NOT hold (isBugCondition returns false), the fixed
generator SHALL produce the same import set as the original generator, preserving single-argument
collection element imports, direct nested-object/enum imports, the always-added base/standard imports,
and the omission of redundant imports for primitives, wrappers, temporals, and `java.lang`/`java.util`
container/utility types.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6**

## Fix Implementation

### Changes Required

Assuming our root cause analysis is correct, the fix introduces a single recursive import collector in
each generator and routes every field's type through it. The collector separates the *import decision*
from the *recursion decision*.

**File**: `src/main/java/com/thy/fss/common/inmemory/processor/generator/StaticMetaModelGenerator.java`

**Function**: `addImportsForField` (plus a new private helper, e.g. `collectTypeImports`)

**Specific Changes**:
1. **Add a recursive collector**: Introduce `private void collectTypeImports(TypeMirror type,
   Set<String> imports)` that:
   - If `type` is a `DeclaredType` whose element is a `TypeElement`, compute its qualified name.
   - Add the qualified name to `imports` **only if** it is not a primitive/wrapper mapping key, not a
     temporal mapping key, not `java.lang.*`, not `java.util.*`, and not `java.lang.Object` (the same
     "user type" predicate used today for the complex-type branch).
   - **Regardless of** whether the type itself was imported, iterate over
     `declaredType.getTypeArguments()` and recurse into **every** argument.
2. **Route the whole field through the collector**: Replace the collection-only,
   `typeArguments.get(0)`, `java.util`-excluding element block with a single call
   `collectTypeImports(field.asType(), imports)`. This subsumes the current "collection element" and
   "complex type" branches for user-type imports.
3. **Preserve the temporal and Collection base imports**: Keep the existing `LocalDate` /
   `LocalDateTime` / `Instant` string checks and the `JAVA_UTIL_COLLECTION` import that is added when
   `isCollectionType(fieldType)` is true, so temporal and collection scaffolding imports are unchanged.
4. **Keep exclusion semantics identical for leaf decisions**: The user-type predicate that decides
   whether to add an import is byte-for-byte the same set of rules used today, guaranteeing preservation
   for already-correct fields.

**File**: `src/main/java/com/thy/fss/common/inmemory/processor/generator/FilterMetaModelGenerator.java`

**Function**: `addFieldImports` (plus a new private helper, e.g. `collectTypeImports`)

**Specific Changes**:
1. **Add a recursive collector**: Introduce `private void collectTypeImports(TypeMirror type,
   Set<String> imports)` that:
   - For each `DeclaredType` user type encountered, add the entity's qualified name (skipping
     `java.lang.*` / `java.util.*` as today), and when the type is annotated with `@MetaModel`, also
     add the derived `...Filter` import (qualified name with the simple name replaced by
     `SimpleName + "Filter"`), matching the existing nested-model and collection-element logic.
   - Recurse into **all** `getTypeArguments()` for every type, including `Map` and nested generics,
     independent of whether the enclosing type was imported.
2. **Route the whole field through the collector**: Replace the current nested-model + collection
   (`typeArguments.get(0)`) block with a single call `collectTypeImports(field.asType(), imports)` so
   direct types, collection elements, map keys/values, and nested generics are all handled uniformly.
3. **Preserve filter-class import derivation**: Reuse the existing `SimpleName + FILTER4` naming and
   the qualified-name replacement approach so the emitted `...Filter` imports are identical to today's
   for the cases that already worked.
4. **Leave `analyzeField`, `determineFilterType`, and builder/deserializer generation untouched**: The
   fix is confined to import collection; field mapping and code emission are unchanged.
5. **Guard against cycles/duplicates via the Set**: Since imports accumulate into a `Set<String>`,
   repeated types are naturally de-duplicated; recursion terminates because generic nesting depth is
   finite.

## Testing Strategy

### Validation Approach

The testing strategy follows a two-phase approach: first, surface counterexamples that demonstrate the
bug on the UNFIXED generators, then verify the fix collects the missing imports and preserves existing
import behavior. Because the generators run inside an annotation-processing round, tests exercise them
by compiling small `@MetaModel` fixture classes (with fields such as `Map<String, Order>`,
`List<Map<String, Order>>`, and `List<List<Order>>`, where `Order` lives in a different package) and
inspecting the generated sources / compilation result.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix. Confirm or
refute the root-cause analysis (first-argument-only, no recursion, `java.util` exclusion). If refuted,
re-hypothesize.

**Test Plan**: Compile fixture `@MetaModel` classes with cross-package generic fields and assert that
the generated `ClassName_` / `ClassNameFilter` sources contain the required imports (or that
compilation of the generated sources succeeds). Run against the UNFIXED generators to observe the
missing-import failures.

**Test Cases**:
1. **Map value in another package**: A field `Map<String, Order>` where `Order` is a `@MetaModel` in a
   different package - assert an import for `Order` (and `OrderFilter` in the filter class) (will fail
   on unfixed code).
2. **Nested generic in collection**: A field `List<Map<String, Order>>` - assert an import for `Order`
   (will fail on unfixed code).
3. **Doubly-nested collection**: A field `List<List<Order>>` - assert an import for `Order` (will fail
   on unfixed code).
4. **Multi-argument user key and value**: A field `Map<CustomerId, Order>` with both types in other
   packages - assert imports for both (will fail on unfixed code, only first argument considered).

**Expected Counterexamples**:
- Generated sources reference `Order` / `CustomerId` by simple name with no matching import, so
  compilation of the generated code fails.
- Confirms causes: first-argument-only inspection, missing recursion, and premature `java.util`
  exclusion of the enclosing `Map`/`List`.

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed generator produces the
expected behavior (every referenced user type imported).

**Pseudocode:**
```
FOR ALL field WHERE isBugCondition(field) DO
  imports := collectImports_fixed(field)
  FOR ALL t IN allUserTypesReferencedBySimpleName(field) DO
    ASSERT t.qualifiedName IN imports
    IF generator == FILTER AND isMetaModel(t) THEN
      ASSERT deriveFilterImport(t) IN imports
  END FOR
END FOR
```

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed generator
produces the same import set as the original generator.

**Pseudocode:**
```
FOR ALL field WHERE NOT isBugCondition(field) DO
  ASSERT collectImports_original(field) = collectImports_fixed(field)
END FOR
```

**Testing Approach**: Property-based testing is recommended for preservation checking because:
- It generates many field-type shapes automatically across the input domain (primitives, wrappers,
  temporals, enums, single-argument collections, direct nested objects, `java.util`/`java.lang` types).
- It catches edge cases that hand-written unit tests might miss (raw types, wildcards, deeply nested
  standard-library generics).
- It provides strong assurance that already-correct import sets are byte-for-byte unchanged.

**Test Plan**: Observe the import sets produced by the UNFIXED generators for non-buggy fields, then
write property-based tests asserting the fixed generators produce identical sets for those same inputs.

**Test Cases**:
1. **Single-argument collection preservation**: Observe that `List<Order>` / `Set<Order>` element
   imports (and `OrderFilter`) are emitted on unfixed code, then verify this continues after the fix.
2. **Direct nested object/enum preservation**: Observe that a direct cross-package model/enum field
   import is emitted on unfixed code, then verify it continues after the fix.
3. **Standard-type omission preservation**: Observe that primitives, wrappers, temporals, and
   `java.lang`/`java.util` container types produce no redundant user import on unfixed code, then
   verify the same after the fix.
4. **Base-import and inheritance preservation**: Verify the always-added base imports and inherited
   `@MetaModel` superclass fields still contribute exactly the same imports after the fix.

### Unit Tests

- Import collection for each shape: direct user type, `List<Order>`, `Set<Order>`, `Map<String,
  Order>`, `Map<CustomerId, Order>`, `List<Map<String, Order>>`, `List<List<Order>>`.
- Cross-package resolution: referenced types in a different package are imported by fully qualified
  name; same-package types are handled consistently with today's behavior.
- Standard-type omission: primitives, wrappers, `String`, temporals, `java.lang`/`java.util` containers
  produce no spurious user imports.
- Filter-specific: for `@MetaModel` referenced types (direct, collection element, and map value), the
  derived `...Filter` import is present.

### Property-Based Tests

- Generate random nested generic field types over a mix of user types and standard-library types, and
  assert every user type reachable through any type argument (including all `Map` keys/values and
  arbitrary nesting) appears in the collected imports.
- Generate random non-buggy field shapes and assert the fixed and original import sets are equal
  (preservation).
- Generate random `@MetaModel` reference placements and assert the corresponding `...Filter` imports
  are always collected by the filter generator.

### Integration Tests

- Full annotation-processing round: compile a set of `@MetaModel` fixtures containing the buggy field
  shapes and assert the whole build (generated sources included) compiles successfully.
- Cross-package flow: fixtures where referenced models/enums live in different packages than the
  generated classes, asserting no "cannot find symbol" errors.
- Inheritance flow: a `@MetaModel` subclass whose inherited fields carry nested generics, asserting
  inherited-field imports are collected and the build compiles.
