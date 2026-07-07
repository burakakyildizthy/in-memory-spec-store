# Bugfix Requirements Document

## Introduction

The annotation processor generates helper classes for every `@MetaModel` annotated type
(`ClassName_` static meta models via `StaticMetaModelGenerator`, and `ClassNameFilter`
classes via `FilterMetaModelGenerator`). These generated classes refer to field-related
types (nested model types, enum types, and collection element types) by their simple names
and therefore rely on the generator emitting a matching `import` statement for each type.

Several fixes have already been applied to the import-collection logic, but gaps remain.
The step that collects imports for a field is incomplete: it inspects only the first generic
type argument of a field, does not recurse into nested generics, and drops type arguments that
live under `java.util` (which discards `Map` value types entirely). As a result, generated
classes are missing import statements for some referenced types, and when those types live in
a different package than the generated class the emitted code cannot resolve the simple name,
so the project fails to compile.

The fix must ensure that the import-collection step covers every model/enum type referenced by
a field, including nested object types and ALL generic type parameters of collections and maps
(for example the element type of `List`/`Set` and both the key and value types of `Map`),
resolving them across packages, and assuming that any referenced meta model must be imported.

## Bug Analysis

### Current Behavior (Defect)

When collecting imports for a field, the generators (`StaticMetaModelGenerator.addImportsForField`
and `FilterMetaModelGenerator.addFieldImports`) miss certain referenced types, producing generated
classes that do not compile.

1.1 WHEN a field is a `Map<K, V>` whose value type `V` is a model or enum in a different package THEN the generator omits the import for `V`, and the generated class fails to compile because the type cannot be resolved.

1.2 WHEN a field is a parameterized collection whose element type is itself parameterized (for example `List<List<Order>>` or `List<Map<String, Order>>`) THEN the generator inspects only the outer first type argument, skips the nested element types, and omits their imports, causing a compilation failure.

1.3 WHEN a field is a generic type with more than one type argument (for example `Map<String, Order>`) THEN the generator only considers the first type argument and omits imports for the remaining type arguments.

1.4 WHEN a referenced model/enum element type resides under a package that the import filter excludes by prefix (for example any type nested inside a `java.util.Map` argument) THEN the generator discards it and never emits the required import.

1.5 WHEN a referenced nested-object or collection-element type is a `@MetaModel` in a different package than the generated class THEN the generated class references it by simple name without a corresponding import, so the build cannot be produced.

### Expected Behavior (Correct)

For every `@MetaModel` class processed, the generated classes SHALL include an import for each
model/enum type they reference by simple name, so the generated sources compile.

2.1 WHEN a field is a `Map<K, V>` whose value type `V` is a model or enum in a different package THEN the system SHALL emit an import for `V` so the generated class compiles.

2.2 WHEN a field is a parameterized collection whose element type is itself parameterized (for example `List<List<Order>>` or `List<Map<String, Order>>`) THEN the system SHALL recurse through the nested generic type arguments and emit imports for every referenced model/enum type.

2.3 WHEN a field is a generic type with more than one type argument (for example `Map<String, Order>`) THEN the system SHALL consider all type arguments and emit imports for every referenced model/enum type among them.

2.4 WHEN a referenced model/enum type lives in a different package than the generated class THEN the system SHALL resolve and emit its fully qualified import, assuming that models may live in different packages.

2.5 WHEN a field references a `@MetaModel` type (directly, as a nested object, or as any generic type argument) THEN the system SHALL emit an import for that meta model, assuming that ALL referenced meta models must be imported.

### Unchanged Behavior (Regression Prevention)

The fix must not change import handling for cases that already work correctly.

3.1 WHEN a field is a single-parameter collection (for example `List<Order>` or `Set<Order>`) whose element type is a model/enum in a different package THEN the system SHALL CONTINUE TO emit the import for that element type.

3.2 WHEN a field is a direct nested-object or enum type in a different package THEN the system SHALL CONTINUE TO emit the import for that type.

3.3 WHEN a field type or generic type argument is a primitive, wrapper, temporal (`java.time.*`), or standard `java.lang`/`java.util` type that requires no user-package import THEN the system SHALL CONTINUE TO omit redundant imports for it (except where the type itself must be referenced, such as `java.util.Collection` and `java.time.*` types that are already imported today).

3.4 WHEN a field type is a primitive, wrapper, temporal, string, or enum handled by the existing branches THEN the system SHALL CONTINUE TO generate the same attribute/filter declarations and the same imports it produces today.

3.5 WHEN inherited fields are contributed by a `@MetaModel` annotated superclass THEN the system SHALL CONTINUE TO include those fields when collecting imports.

3.6 WHEN a class is generated whose referenced types were already fully imported before this fix THEN the system SHALL CONTINUE TO produce byte-for-byte equivalent import sets (aside from newly added, previously missing imports).
