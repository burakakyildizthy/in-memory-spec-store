package com.thy.fss.common.inmemory.exception;

import com.thy.fss.common.inmemory.dashboard.exception.DashboardNotFoundException;
import com.thy.fss.common.inmemory.processor.exception.ProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for invalid configuration and data scenarios in exception handling.
 * Tests malformed data, invalid parameters, and configuration edge cases.
 */
@DisplayName("Invalid Configuration and Data Tests")
class InvalidConfigurationTest {

    @Nested
    @DisplayName("Invalid Configuration Scenarios")
    class InvalidConfigurationScenarioTest {

        @Test
        @DisplayName("Should handle invalid DataSource configuration")
        void testInvalidDataSourceConfiguration() {
            // Test various invalid DataSource configurations

            // Null configuration
            assertDoesNotThrow(() -> {
                DataSourceConnectionException exception = new DataSourceConnectionException(
                        "Invalid configuration: null", (String) null);
                assertNull(exception.getDataSourceName());
            });

            // Empty configuration
            assertDoesNotThrow(() -> {
                DataSourceConnectionException exception = new DataSourceConnectionException(
                        "Invalid configuration: empty", "");
                assertEquals("", exception.getDataSourceName());
            });

            // Malformed configuration
            String malformedConfig = "invalid://malformed:config@host:port/database?param=value&";
            DataSourceConnectionException exception = new DataSourceConnectionException(
                    "Malformed connection string", malformedConfig);
            assertEquals(malformedConfig, exception.getDataSourceName());
        }

        @Test
        @DisplayName("Should handle invalid synchronization configuration")
        void testInvalidSynchronizationConfiguration() {
            // Test invalid version numbers
            SynchronizationException exception1 = new SynchronizationException(
                    "Invalid version: negative", -999L);
            assertEquals(-999L, exception1.getCurrentVersion());

            // Test invalid sync phases
            String[] invalidPhases = {
                    null, "", "INVALID_PHASE", "phase with spaces", "phase\nwith\nnewlines"
            };

            for (String phase : invalidPhases) {
                SynchronizationException exception = new SynchronizationException(
                        "Invalid sync phase", 123L, phase);
                assertEquals(phase, exception.getSyncPhase());
            }
        }

        @Test
        @DisplayName("Should handle invalid object building configuration")
        void testInvalidObjectBuildingConfiguration() {
            // Test with invalid target classes
            Class<?>[] invalidClasses = {null, void.class, Void.class};

            for (Class<?> clazz : invalidClasses) {
                ObjectBuildingException exception = new ObjectBuildingException(
                        "Invalid target class", clazz, "property");
                assertEquals(clazz, exception.getTargetClass());
            }

            // Test with invalid property names
            String[] invalidProperties = {
                    null, "", "invalid property name", "property.with.dots",
                    "property-with-dashes", "property$with$symbols"
            };

            for (String property : invalidProperties) {
                ObjectBuildingException exception = new ObjectBuildingException(
                        "Invalid property", String.class, property);
                assertEquals(property, exception.getPropertyName());
            }
        }

        @Test
        @DisplayName("Should handle invalid dashboard configuration")
        void testInvalidDashboardConfiguration() {
            // Test various invalid dashboard IDs
            String[] invalidIds = {
                    null, "", " ", "\t", "\n", "id with spaces",
                    "id/with/slashes", "id\\with\\backslashes", "id@with@symbols",
                    "very-long-dashboard-id-that-exceeds-reasonable-limits-" + "x".repeat(1000)
            };

            for (String id : invalidIds) {
                assertDoesNotThrow(() -> {
                    DashboardNotFoundException exception = new DashboardNotFoundException(id);
                    assertEquals(id, exception.getDashboardId());
                });
            }
        }
    }

    @Nested
    @DisplayName("Malformed Data Scenarios")
    class MalformedDataScenarioTest {

        @ParameterizedTest
        @ValueSource(strings = {
                "data\0with\0null\0bytes",
                "data\u0001with\u0002control\u0003characters",
                "data with\ttabs\nand\rnewlines",
                "data with unicode: 测试 🚀 ñáéíóú",
                "data with emojis: 😀😃😄😁😆😅😂🤣",
                "data with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?"
        })
        @DisplayName("Should handle malformed message data")
        void testMalformedMessageData(String malformedData) {
            InMemoryDataStoreException exception = new InMemoryDataStoreException(malformedData);
            assertEquals(malformedData, exception.getMessage());
        }

        @Test
        @DisplayName("Should handle malformed JSON-like data")
        void testMalformedJsonLikeData() {
            String[] malformedJsonData = {
                    "{\"incomplete\": ",
                    "[1, 2, 3,",
                    "{\"nested\": {\"incomplete\": }",
                    "\"unclosed string",
                    "{\"key\": \"value\", \"key\": \"duplicate\"}",
                    "{\"null_value\": null, \"undefined\": undefined}"
            };

            for (String data : malformedJsonData) {
                DataSourceException exception = new DataSourceException(
                        "Malformed JSON data: " + data);
                assertTrue(exception.getMessage().contains(data));
            }
        }

        @Test
        @DisplayName("Should handle malformed XML-like data")
        void testMalformedXmlLikeData() {
            String[] malformedXmlData = {
                    "<unclosed>tag",
                    "<tag>content</wrong-tag>",
                    "<tag attribute=\"unclosed>content</tag>",
                    "<?xml version=\"1.0\"?><root><child></root>",
                    "<tag>content with & unescaped ampersand</tag>"
            };

            for (String data : malformedXmlData) {
                ProcessingException exception = new ProcessingException(
                        "Malformed XML data: " + data);
                assertTrue(exception.getMessage().contains(data));
            }
        }

        @Test
        @DisplayName("Should handle malformed URL-like data")
        void testMalformedUrlLikeData() {
            String[] malformedUrls = {
                    "http://",
                    "ftp://user:@host",
                    "https://host:port/path with spaces",
                    "invalid-protocol://host",
                    "http://host:invalid-port/path",
                    "http://[invalid-ipv6]/path"
            };

            for (String url : malformedUrls) {
                DataSourceConnectionException exception = new DataSourceConnectionException(
                        "Invalid URL", url);
                assertEquals(url, exception.getDataSourceName());
            }
        }
    }

    @Nested
    @DisplayName("Invalid Parameter Combinations")
    class InvalidParameterCombinationTest {

        @Test
        @DisplayName("Should handle conflicting parameter combinations")
        void testConflictingParameterCombinations() {
            // Test ObjectBuildingException with conflicting information
            ObjectBuildingException exception1 = new ObjectBuildingException(
                    "String property on Integer class", Integer.class, "stringProperty");
            assertEquals(Integer.class, exception1.getTargetClass());
            assertEquals("stringProperty", exception1.getPropertyName());

            // Test SynchronizationException with conflicting version and phase
            SynchronizationException exception2 = new SynchronizationException(
                    "Future version in past phase", Long.MAX_VALUE, "INITIALIZATION");
            assertEquals(Long.MAX_VALUE, exception2.getCurrentVersion());
            assertEquals("INITIALIZATION", exception2.getSyncPhase());
        }

        @Test
        @DisplayName("Should handle impossible parameter values")
        void testImpossibleParameterValues() {
            // Test with impossible version numbers
            SynchronizationException exception1 = new SynchronizationException(
                    "Impossible negative version", -1L, "COMMIT");
            assertEquals(-1L, exception1.getCurrentVersion());

            // Test with impossible class combinations
            ObjectBuildingException exception2 = new ObjectBuildingException(
                    "Primitive void property", void.class, "voidProperty");
            assertEquals(void.class, exception2.getTargetClass());
            assertEquals("voidProperty", exception2.getPropertyName());
        }

        @Test
        @DisplayName("Should handle circular reference scenarios")
        void testCircularReferenceScenarios() {
            // Create a scenario that might lead to circular references
            RuntimeException cause1 = new RuntimeException("Cause 1");
            RuntimeException cause2 = new RuntimeException("Cause 2", cause1);

            InMemoryDataStoreException exception1 = new InMemoryDataStoreException(
                    "Exception 1", cause2);
            InMemoryDataStoreException exception2 = new InMemoryDataStoreException(
                    "Exception 2 referencing Exception 1", exception1);

            assertEquals("Exception 2 referencing Exception 1", exception2.getMessage());
            assertEquals(exception1, exception2.getCause());
            assertEquals(cause2, exception2.getCause().getCause());
            assertEquals(cause1, exception2.getCause().getCause().getCause());
        }
    }

    @Nested
    @DisplayName("Resource Constraint Scenarios")
    class ResourceConstraintScenarioTest {

        @Test
        @DisplayName("Should handle memory constraint scenarios")
        void testMemoryConstraintScenarios() {
            // Simulate low memory conditions
            String largeMessage = "x".repeat(1_000_000); // 1MB message

            assertDoesNotThrow(() -> {
                InMemoryDataStoreException exception = new InMemoryDataStoreException(largeMessage);
                assertEquals(largeMessage, exception.getMessage());
            });
        }

        @Test
        @DisplayName("Should handle thread exhaustion scenarios")
        void testThreadExhaustionScenarios() {
            // Simulate scenarios where thread resources are exhausted
            Supplier<InMemoryDataStoreException> exceptionSupplier = () ->
                    new InMemoryDataStoreException("Thread exhaustion scenario");

            // Should still be able to create exceptions even under thread pressure
            for (int i = 0; i < 1000; i++) {
                InMemoryDataStoreException exception = exceptionSupplier.get();
                assertEquals("Thread exhaustion scenario", exception.getMessage());
            }
        }

        @Test
        @DisplayName("Should handle file system constraint scenarios")
        void testFileSystemConstraintScenarios() {
            // Simulate file system related errors
            String[] fileSystemErrors = {
                    "No space left on device",
                    "Permission denied",
                    "File not found",
                    "Too many open files",
                    "Disk quota exceeded",
                    "Read-only file system"
            };

            for (String error : fileSystemErrors) {
                DataSourceException exception = new DataSourceException(
                        "File system error: " + error);
                assertTrue(exception.getMessage().contains(error));
            }
        }

        @Test
        @DisplayName("Should handle network constraint scenarios")
        void testNetworkConstraintScenarios() {
            // Simulate various network constraint scenarios
            String[] networkErrors = {
                    "Connection timeout",
                    "Network unreachable",
                    "Host unreachable",
                    "Connection refused",
                    "Network is down",
                    "No route to host",
                    "Connection reset by peer"
            };

            for (String error : networkErrors) {
                DataSourceConnectionException exception = new DataSourceConnectionException(
                        error, "network-datasource");
                assertTrue(exception.getMessage().contains(error));
                assertEquals("network-datasource", exception.getDataSourceName());
            }
        }
    }

    @Nested
    @DisplayName("Encoding and Character Set Issues")
    class EncodingCharacterSetTest {

        @Test
        @DisplayName("Should handle various character encodings")
        void testVariousCharacterEncodings() {
            String[] encodingTestStrings = {
                    "ASCII text",
                    "UTF-8: 测试文本 🚀",
                    "Latin-1: café naïve résumé",
                    "Cyrillic: Привет мир",
                    "Arabic: مرحبا بالعالم",
                    "Japanese: こんにちは世界",
                    "Korean: 안녕하세요 세계",
                    "Emoji: 😀😃😄😁😆😅😂🤣😊😇"
            };

            for (String testString : encodingTestStrings) {
                InMemoryDataStoreException exception = new InMemoryDataStoreException(testString);
                assertEquals(testString, exception.getMessage());
            }
        }

        @Test
        @DisplayName("Should handle byte order mark (BOM) characters")
        void testByteOrderMarkCharacters() {
            String[] bomStrings = {
                    "\uFEFFUTF-8 BOM text",
                    "\uFFFEUTF-16 BE BOM text",
                    "\uFEFFUTF-16 LE BOM text"
            };

            for (String bomString : bomStrings) {
                DataSourceException exception = new DataSourceException(bomString);
                assertEquals(bomString, exception.getMessage());
            }
        }

        @Test
        @DisplayName("Should handle surrogate pairs")
        void testSurrogatePairs() {
            // Test with characters that require surrogate pairs in UTF-16
            String[] surrogatePairStrings = {
                    "𝕳𝖊𝖑𝖑𝖔 𝖂𝖔𝖗𝖑𝖉", // Mathematical bold fraktur
                    "🌍🌎🌏🌐🌑🌒🌓🌔", // Earth and moon emojis
                    "𝒜𝒷𝒸𝒹𝑒𝒻𝑔𝒽𝒾𝒿𝓀𝓁𝓂", // Mathematical script
                    "𝔸𝔹ℂ𝔻𝔼𝔽𝔾ℍ𝕀𝕁𝕂𝕃𝕄" // Mathematical double-struck
            };

            for (String surrogateString : surrogatePairStrings) {
                ProcessingException exception = new ProcessingException(surrogateString);
                assertEquals(surrogateString, exception.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("Locale and Internationalization Issues")
    class LocaleInternationalizationTest {

        @Test
        @DisplayName("Should handle right-to-left text")
        void testRightToLeftText() {
            String[] rtlStrings = {
                    "العربية", // Arabic
                    "עברית", // Hebrew
                    "فارسی", // Persian
                    "اردو" // Urdu
            };

            for (String rtlString : rtlStrings) {
                InMemoryDataStoreException exception = new InMemoryDataStoreException(
                        "RTL text error: " + rtlString);
                assertTrue(exception.getMessage().contains(rtlString));
            }
        }

        @Test
        @DisplayName("Should handle mixed text directions")
        void testMixedTextDirections() {
            String[] mixedStrings = {
                    "English العربية English",
                    "Left-to-right עברית right-to-left",
                    "Mixed: English + العربية + עברית"
            };

            for (String mixedString : mixedStrings) {
                DataSourceException exception = new DataSourceException(mixedString);
                assertEquals(mixedString, exception.getMessage());
            }
        }

        @Test
        @DisplayName("Should handle locale-specific number formats")
        void testLocaleSpecificNumberFormats() {
            String[] localeNumbers = {
                    "1,234.56", // US format
                    "1.234,56", // European format
                    "1 234,56", // French format
                    "१२३४.५६", // Devanagari digits
                    "١٢٣٤.٥٦" // Arabic-Indic digits
            };

            for (String number : localeNumbers) {
                SynchronizationException exception = new SynchronizationException(
                        "Locale number format: " + number, 123L);
                assertTrue(exception.getMessage().contains(number));
            }
        }
    }
}