# Implementation Plan

- [x] 1. Write bug condition exploration test (BEFORE implementing fix)
  - **Property 1: Bug Condition** - Complete Import Collection Across All Nested Type Arguments
  - **CRITICAL**: This test MUST FAIL on the unfixed generators - failure confirms the bug exists
  - **DO NOT attempt to fix the test or the code when it fails** - the failure is the goal of this step
  - **NOTE**: This test encodes the expected behavior; it will validate the fix when it passes after implementation
  - **GOAL**: Surface counterexamples that demonstrate missing imports in generated `ClassName_` / `ClassNameFilter` sources
  - **Scoped PBT Approach**: Scope the property to the concrete bug-triggering field shapes below (deterministic reproduction) while asserting the universal property "every user type referenced by simple name has a matching import"
  - Add `@MetaModel` fixture classes to the test sources where the referenced model/enum type `Order` (and `CustomerId`) lives in a **different package** than the generated class
  - Compile the fixtures through the annotation-processing round using the UNFIXED `StaticMetaModelGenerator` and `FilterMetaModelGenerator`, then inspect the generated sources / compilation result
  - Test cases (from Bug Condition + Examples in design):
    - `Map<String, Order>` -> assert import for `Order` (and `OrderFilter` in the filter class). Cause: `Map` declared type `java.util.Map` is excluded by the `startsWith("java.util")` guard before its arguments are read (Requirement 1.1, 1.4)
    - `List<Map<String, Order>>` -> assert import for `Order`. Cause: only the outer first argument is inspected, inner `Order` never reached (Requirement 1.2)
    - `List<List<Order>>` -> assert import for `Order`. Cause: outer first argument `List<Order>` is a `java.util` type, nested `Order` never reached (Requirement 1.2, 1.4)
    - `Map<CustomerId, Order>` -> assert imports for both key and value types. Cause: only `typeArguments.get(0)` considered (Requirement 1.3)
  - The test assertions must match Property 1 (Expected Behavior): for every user-defined model/enum type referenced by the field at any nesting depth (including `Map` keys and values), an import resolved by fully qualified name is present; for the filter generator, referenced `@MetaModel` types also contribute a `...Filter` import
  - Run test on UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS (this is correct - it proves the missing-import bug exists)
  - Document counterexamples found (e.g., "generated `Xyz_` references `Order` by simple name with no import; generated source fails to compile with 'cannot find symbol'")
  - Mark task complete when the test is written, run, and the failure is documented
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Existing Import Sets Unchanged For Already-Correct Fields
  - **IMPORTANT**: Follow the observation-first methodology - record the import sets the UNFIXED generators produce for non-buggy fields, then assert those exact sets
  - **Why property-based**: Preservation is a universal property ("for all non-buggy inputs the fixed and original import sets are equal"); jqwik generates many field-type shapes and catches edge cases (raw types, wildcards, deeply nested standard-library generics)
  - Observe behavior on UNFIXED code for cases where `isBugCondition` returns false and record the emitted imports:
    - Single-argument collection preservation: `List<Order>` / `Set<Order>` (cross-package `Order`) emit the `Order` element import (and `OrderFilter` for the filter class) - Requirement 3.1
    - Direct nested object/enum preservation: a direct cross-package model/enum field emits its import - Requirement 3.2
    - Standard-type omission preservation: primitives, wrappers, `String`, temporals (`java.time.*`), and `java.lang`/`java.util` container/utility types produce no redundant user import - Requirement 3.3, 3.4
    - Base-import and inheritance preservation: the always-added base/standard imports (attribute wildcard for the static model; `java.util`, `java.time`, Jackson, filter imports for the filter class) and inherited `@MetaModel` superclass fields contribute the same imports - Requirement 3.5, 3.6
  - Write property-based tests (jqwik) that generate random non-buggy field shapes and assert `collectImports_original(field) == collectImports_fixed(field)` (byte-for-byte equal import sets), plus scoped unit assertions capturing the observed sets above
  - Run tests on UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (this confirms the baseline behavior to preserve)
  - Mark task complete when the tests are written, run, and passing on unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 3. Fix for missing imports of nested/map/multi-argument generic type parameters in generated classes

  - [x] 3.1 Implement the recursive import collector in `StaticMetaModelGenerator`
    - Add `private void collectTypeImports(TypeMirror type, Set<String> imports)` that separates the import decision from the recursion decision
    - Import decision: if `type` is a `DeclaredType` whose element is a `TypeElement`, add its qualified name to `imports` **only if** it is a user type (not a primitive/wrapper mapping key, not a temporal mapping key, not `java.lang.*`, not `java.util.*`, not `java.lang.Object`) - reuse the exact same predicate used today for the complex-type branch
    - Recursion decision: **regardless of** whether the type itself was imported, iterate `declaredType.getTypeArguments()` and recurse into **every** argument (handles `Map` keys and values and arbitrary nesting)
    - In `addImportsForField`, replace the collection-only, `typeArguments.get(0)`, `java.util`-excluding element block with a single call `collectTypeImports(field.asType(), imports)`
    - Preserve the existing temporal (`LocalDate` / `LocalDateTime` / `Instant`) string checks and the `JAVA_UTIL_COLLECTION` base import added when `isCollectionType(fieldType)` is true
    - De-duplication and cycle safety come naturally from accumulating into the `Set<String>`; recursion terminates because generic nesting depth is finite
    - _Bug_Condition: isBugCondition(field) - a field references a user model/enum type (directly, nested, or as any type argument at any depth including Map keys/values) in a different package that the current logic misses_
    - _Expected_Behavior: Property 1 - emit an import (fully qualified) for every user type referenced by simple name so the generated class compiles_
    - _Preservation: Property 2 - identical import sets for non-buggy fields; unchanged base imports, temporal imports, and single-argument/direct cases_
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [x] 3.2 Implement the recursive import collector in `FilterMetaModelGenerator`
    - Add `private void collectTypeImports(TypeMirror type, Set<String> imports)` mirroring the static generator's collector
    - Import decision: for each `DeclaredType` user type encountered, add the entity's qualified name (skipping `java.lang.*` / `java.util.*` as today); when the type is annotated with `@MetaModel`, also add the derived `...Filter` import (qualified name with the simple name replaced by `SimpleName + "Filter"`, reusing the existing `SimpleName + FILTER4` naming and qualified-name replacement approach)
    - Recursion decision: recurse into **all** `getTypeArguments()` for every type, including `Map` and nested generics, independent of whether the enclosing type was imported
    - In `addFieldImports`, replace the current nested-model + collection (`typeArguments.get(0)`) block with a single call `collectTypeImports(field.asType(), imports)`
    - Leave `analyzeField`, `determineFilterType`, and builder/deserializer generation untouched - the fix is confined to import collection
    - _Bug_Condition: isBugCondition(field) - as above, including derived `...Filter` imports for referenced @MetaModel types_
    - _Expected_Behavior: Property 1 - emit an import for every referenced user type and, for @MetaModel types, the corresponding `...Filter` import_
    - _Preservation: Property 2 - preserve single-argument collection element imports (including `OrderFilter`), direct nested-object/enum imports, and always-added base/standard filter imports_
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [x] 3.3 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Complete Import Collection Across All Nested Type Arguments
    - **IMPORTANT**: Re-run the SAME test from task 1 - do NOT write a new test
    - The test from task 1 encodes the expected behavior; when it passes it confirms every referenced user type (and derived `...Filter`) is imported and the generated sources compile
    - Run the bug condition exploration test from step 1 against the fixed generators
    - **EXPECTED OUTCOME**: Test PASSES (confirms the missing-import bug is fixed)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [x] 3.4 Verify preservation tests still pass
    - **Property 2: Preservation** - Existing Import Sets Unchanged For Already-Correct Fields
    - **IMPORTANT**: Re-run the SAME tests from task 2 - do NOT write new tests
    - Run the preservation property tests from step 2 against the fixed generators
    - **EXPECTED OUTCOME**: Tests PASS (confirms no regressions - import sets for non-buggy fields are byte-for-byte unchanged)
    - Confirm all preservation tests still pass after the fix
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 4. Add supporting unit, property-based, and integration tests
  - Unit tests: import collection for each shape - direct user type, `List<Order>`, `Set<Order>`, `Map<String, Order>`, `Map<CustomerId, Order>`, `List<Map<String, Order>>`, `List<List<Order>>`; cross-package resolution by fully qualified name; standard-type omission (primitives, wrappers, `String`, temporals, `java.lang`/`java.util` containers); filter-specific derived `...Filter` imports for @MetaModel references (direct, collection element, and map value)
  - Property-based tests (jqwik): generate random nested generic field types over a mix of user and standard-library types and assert every reachable user type appears in the collected imports; generate random non-buggy shapes and assert fixed == original import sets; generate random @MetaModel reference placements and assert the filter generator always collects the corresponding `...Filter` imports
  - Integration tests: full annotation-processing round compiling `@MetaModel` fixtures with the buggy field shapes and asserting the whole build (generated sources included) compiles with no "cannot find symbol" errors; cross-package flow; inheritance flow where inherited fields carry nested generics
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.3, 3.4, 3.5, 3.6_

- [x] 5. Checkpoint - Ensure all tests pass
  - Run the full test suite and build; ensure all unit, property-based, and integration tests pass and the project compiles
  - Ask the user if questions arise.
