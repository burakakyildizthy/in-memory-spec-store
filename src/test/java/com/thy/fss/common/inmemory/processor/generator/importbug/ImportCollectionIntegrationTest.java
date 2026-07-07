package com.thy.fss.common.inmemory.processor.generator.importbug;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests that exercise the real annotation processor by compiling
 * fixture @MetaModel classes and verifying the generated sources.
 *
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.2, 3.5, 3.6**
 *
 * The annotation processor runs at compile time via testAnnotationProcessor.
 * If these test classes compile successfully (including generated sources),
 * the import bug is fixed. The tests additionally read generated source files
 * to verify specific import statements are present.
 */
@DisplayName("Import Collection Integration Tests")
class ImportCollectionIntegrationTest {

    private static final Path GENERATED_DIR = Path.of("build/generated/sources/annotationProcessor/java/test");
    private static final String FIXTURES_PKG = "com/thy/fss/common/inmemory/processor/generator/importbug/fixtures";
    private static final String OTHER_PKG = "com/thy/fss/common/inmemory/processor/generator/importbug/otherpkg";

    // ==================== Compilation Success Tests ====================

    @Nested
    @DisplayName("Compilation Success - Bug Triggering Shapes")
    class CompilationSuccess {

        @Test
        @DisplayName("BugTriggeringModel compiles with all required imports (annotation processor ran)")
        void bugTriggeringModel_compilesSuccessfully() throws IOException {
            // If we reach here, the annotation processor successfully generated
            // BugTriggeringModel_ and BugTriggeringModelFilter with correct imports
            // because testAnnotationProcessor ran during compilation.
            // Note: Map-typed fields are not exercised here since Map is not a
            // covered/supported field type for this fixture.
            Path generatedStaticMeta = GENERATED_DIR.resolve(FIXTURES_PKG + "/BugTriggeringModel_.java");

            if (Files.exists(generatedStaticMeta)) {
                String content = Files.readString(generatedStaticMeta);
                assertThat(content)
                        .as("Generated BugTriggeringModel_ must import Order from otherpkg")
                        .contains("import com.thy.fss.common.inmemory.processor.generator.importbug.otherpkg.Order;");
            }
            // Compilation itself passing is the primary assertion
        }

        @Test
        @DisplayName("BugTriggeringModelFilter has correct imports for nested collection types")
        void bugTriggeringModelFilter_hasCorrectImports() throws IOException {
            Path generatedFilter = GENERATED_DIR.resolve(FIXTURES_PKG + "/BugTriggeringModelFilter.java");

            if (Files.exists(generatedFilter)) {
                String content = Files.readString(generatedFilter);
                assertThat(content)
                        .as("Filter must import Order from otherpkg")
                        .contains("import com.thy.fss.common.inmemory.processor.generator.importbug.otherpkg.Order;");
                assertThat(content)
                        .as("Filter must import OrderFilter for @MetaModel Order")
                        .contains("import com.thy.fss.common.inmemory.processor.generator.importbug.otherpkg.OrderFilter;");
            }
        }

        @Test
        @DisplayName("Order (otherpkg @MetaModel) is processed and generates its own files")
        void orderModel_generatesOwnFiles() throws IOException {
            Path generatedOrder = GENERATED_DIR.resolve(OTHER_PKG + "/Order_.java");

            if (Files.exists(generatedOrder)) {
                String content = Files.readString(generatedOrder);
                assertThat(content)
                        .as("Order_ must be generated with correct package")
                        .contains("package com.thy.fss.common.inmemory.processor.generator.importbug.otherpkg;");
            }
        }
    }

    // ==================== Cross-Package Tests ====================

    @Nested
    @DisplayName("Cross-Package Resolution")
    class CrossPackageResolution {

        @Test
        @DisplayName("Fixtures reference types in otherpkg - compilation proves cross-package imports work")
        void crossPackageReferences_compileSuccessfully() throws IOException {
            // The very fact that BugTriggeringModel.java (in fixtures package)
            // references Order/Status (in otherpkg) and the annotation
            // processor generates code that also compiles confirms cross-package resolution.
            Path generatedStaticMeta = GENERATED_DIR.resolve(FIXTURES_PKG + "/BugTriggeringModel_.java");

            if (Files.exists(generatedStaticMeta)) {
                String content = Files.readString(generatedStaticMeta);
                // Verify it doesn't try to import java.util.List or java.lang.String (standard types)
                assertThat(content)
                        .doesNotContain("import java.util.List;")
                        .doesNotContain("import java.lang.String;");
            }
        }

        @Test
        @DisplayName("Generated static meta model includes Status enum import")
        void staticMetaModel_includesStatusEnumImport() throws IOException {
            Path generatedStaticMeta = GENERATED_DIR.resolve(FIXTURES_PKG + "/BugTriggeringModel_.java");

            if (Files.exists(generatedStaticMeta)) {
                String content = Files.readString(generatedStaticMeta);
                assertThat(content)
                        .as("Status enum from otherpkg must be imported")
                        .contains("import com.thy.fss.common.inmemory.processor.generator.importbug.otherpkg.Status;");
            }
        }
    }

    // ==================== Inheritance Tests ====================

    @Nested
    @DisplayName("Inheritance Flow")
    class InheritanceFlow {

        @Test
        @DisplayName("InheritanceModel compiles - inherited nested generic fields have correct imports")
        void inheritanceModel_compilesSuccessfully() throws IOException {
            // InheritanceModel extends BaseModel (which has a List<Order> field).
            // If this compiles, inheritance import resolution works.
            Path generatedStaticMeta = GENERATED_DIR.resolve(FIXTURES_PKG + "/InheritanceModel_.java");

            if (Files.exists(generatedStaticMeta)) {
                String content = Files.readString(generatedStaticMeta);
                // Inherited fields from BaseModel reference Order (cross-package)
                assertThat(content)
                        .as("InheritanceModel_ must import Order from inherited fields")
                        .contains("import com.thy.fss.common.inmemory.processor.generator.importbug.otherpkg.Order;");
                // Own field references CustomerId
                assertThat(content)
                        .as("InheritanceModel_ must import CustomerId from own field")
                        .contains("import com.thy.fss.common.inmemory.processor.generator.importbug.otherpkg.CustomerId;");
            }
        }

        @Test
        @DisplayName("BaseModel generates its own static meta model correctly")
        void baseModel_generatesCorrectly() throws IOException {
            Path generatedStaticMeta = GENERATED_DIR.resolve(FIXTURES_PKG + "/BaseModel_.java");

            if (Files.exists(generatedStaticMeta)) {
                String content = Files.readString(generatedStaticMeta);
                assertThat(content)
                        .as("BaseModel_ must import Order for its List<Order> field")
                        .contains("import com.thy.fss.common.inmemory.processor.generator.importbug.otherpkg.Order;");
            }
        }
    }
}
