package com.thy.fss.common.inmemory.processor.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for InstantFormatInfo model class.
 */
@DisplayName("InstantFormatInfo Tests")
class InstantFormatInfoTest {

    @Nested
    @DisplayName("Construction and factory methods")
    class Construction {

        @Test
        @DisplayName("No-arg constructor defaults to default pattern")
        void noArgConstructor() {
            InstantFormatInfo info = new InstantFormatInfo();
            assertThat(info.isHasCustomFormat()).isFalse();
            assertThat(info.getPattern()).isNotNull();
            assertThat(info.isUseTimestamp()).isFalse();
        }

        @Test
        @DisplayName("Custom pattern constructor")
        void customPatternConstructor() {
            InstantFormatInfo info = new InstantFormatInfo("yyyy-MM-dd'T'HH:mm:ssXXX");
            assertThat(info.getPattern()).isEqualTo("yyyy-MM-dd'T'HH:mm:ssXXX");
            assertThat(info.isHasCustomFormat()).isTrue();
            assertThat(info.isUseTimestamp()).isFalse();
        }

        @Test
        @DisplayName("withDefaultPattern factory method")
        void withDefaultPattern() {
            InstantFormatInfo info = InstantFormatInfo.withDefaultPattern();
            assertThat(info.isHasCustomFormat()).isFalse();
            assertThat(info.getPattern()).isNotNull();
        }

        @Test
        @DisplayName("withCustomPattern factory method")
        void withCustomPattern() {
            InstantFormatInfo info = InstantFormatInfo.withCustomPattern("custom");
            assertThat(info.getPattern()).isEqualTo("custom");
            assertThat(info.isHasCustomFormat()).isTrue();
        }

        @Test
        @DisplayName("withTimestampFormat factory method")
        void withTimestampFormat() {
            InstantFormatInfo info = InstantFormatInfo.withTimestampFormat();
            assertThat(info.isUseTimestamp()).isTrue();
            assertThat(info.getShape()).isEqualTo("NUMBER");
        }

        @Test
        @DisplayName("withTimezone factory method")
        void withTimezone() {
            InstantFormatInfo info = InstantFormatInfo.withTimezone("Europe/Istanbul");
            assertThat(info.getTimezone()).isEqualTo("Europe/Istanbul");
        }
    }

    @Nested
    @DisplayName("Getters and Setters")
    class GettersSetters {

        @Test
        @DisplayName("pattern getter/setter")
        void pattern() {
            InstantFormatInfo info = new InstantFormatInfo();
            info.setPattern("custom");
            assertThat(info.getPattern()).isEqualTo("custom");
        }

        @Test
        @DisplayName("hasCustomFormat getter/setter")
        void hasCustomFormat() {
            InstantFormatInfo info = new InstantFormatInfo();
            info.setHasCustomFormat(true);
            assertThat(info.isHasCustomFormat()).isTrue();
        }

        @Test
        @DisplayName("timezone getter/setter")
        void timezone() {
            InstantFormatInfo info = new InstantFormatInfo();
            info.setTimezone("UTC");
            assertThat(info.getTimezone()).isEqualTo("UTC");
        }

        @Test
        @DisplayName("locale getter/setter")
        void locale() {
            InstantFormatInfo info = new InstantFormatInfo();
            info.setLocale("en_US");
            assertThat(info.getLocale()).isEqualTo("en_US");
        }

        @Test
        @DisplayName("useTimestamp getter/setter")
        void useTimestamp() {
            InstantFormatInfo info = new InstantFormatInfo();
            info.setUseTimestamp(true);
            assertThat(info.isUseTimestamp()).isTrue();
        }

        @Test
        @DisplayName("shape getter/setter")
        void shape() {
            InstantFormatInfo info = new InstantFormatInfo();
            info.setShape("STRING");
            assertThat(info.getShape()).isEqualTo("STRING");
        }
    }

    @Nested
    @DisplayName("Utility methods")
    class UtilityMethods {

        @Test
        @DisplayName("usesCustomFormat delegates to hasCustomFormat")
        void usesCustomFormat() {
            InstantFormatInfo custom = InstantFormatInfo.withCustomPattern("pat");
            assertThat(custom.usesCustomFormat()).isTrue();

            InstantFormatInfo def = InstantFormatInfo.withDefaultPattern();
            assertThat(def.usesCustomFormat()).isFalse();
        }

        @Test
        @DisplayName("getEffectivePattern returns custom pattern when set")
        void effectivePatternCustom() {
            InstantFormatInfo info = InstantFormatInfo.withCustomPattern("custom");
            assertThat(info.getEffectivePattern()).isEqualTo("custom");
        }

        @Test
        @DisplayName("getEffectivePattern returns default when pattern is null")
        void effectivePatternNull() {
            InstantFormatInfo info = new InstantFormatInfo();
            info.setPattern(null);
            assertThat(info.getEffectivePattern()).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("getEffectivePattern returns default when pattern is blank")
        void effectivePatternBlank() {
            InstantFormatInfo info = new InstantFormatInfo();
            info.setPattern("   ");
            assertThat(info.getEffectivePattern()).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("shouldUseTimestamp returns true when useTimestamp is true")
        void shouldUseTimestampTrue() {
            InstantFormatInfo info = new InstantFormatInfo();
            info.setUseTimestamp(true);
            assertThat(info.shouldUseTimestamp()).isTrue();
        }

        @Test
        @DisplayName("shouldUseTimestamp returns true when shape is NUMBER")
        void shouldUseTimestampNumber() {
            InstantFormatInfo info = new InstantFormatInfo();
            info.setShape("NUMBER");
            assertThat(info.shouldUseTimestamp()).isTrue();
        }

        @Test
        @DisplayName("shouldUseTimestamp returns false normally")
        void shouldUseTimestampFalse() {
            InstantFormatInfo info = new InstantFormatInfo();
            assertThat(info.shouldUseTimestamp()).isFalse();
        }

        @Test
        @DisplayName("getEffectiveTimezone returns UTC by default")
        void effectiveTimezoneDefault() {
            InstantFormatInfo info = new InstantFormatInfo();
            assertThat(info.getEffectiveTimezone()).isEqualTo("UTC");
        }

        @Test
        @DisplayName("getEffectiveTimezone returns custom timezone when set")
        void effectiveTimezoneCustom() {
            InstantFormatInfo info = new InstantFormatInfo();
            info.setTimezone("Europe/Istanbul");
            assertThat(info.getEffectiveTimezone()).isEqualTo("Europe/Istanbul");
        }

        @Test
        @DisplayName("getEffectiveTimezone returns UTC for empty timezone")
        void effectiveTimezoneEmpty() {
            InstantFormatInfo info = new InstantFormatInfo();
            info.setTimezone("");
            assertThat(info.getEffectiveTimezone()).isEqualTo("UTC");
        }
    }

    @Nested
    @DisplayName("equals, hashCode, toString")
    class EqualsHashCodeToString {

        @Test
        @DisplayName("Equal objects")
        void equalObjects() {
            InstantFormatInfo a = InstantFormatInfo.withCustomPattern("pat");
            a.setTimezone("UTC");
            a.setLocale("en");
            a.setShape("STRING");

            InstantFormatInfo b = InstantFormatInfo.withCustomPattern("pat");
            b.setTimezone("UTC");
            b.setLocale("en");
            b.setShape("STRING");

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("Not equal with different pattern")
        void notEqualDifferentPattern() {
            InstantFormatInfo a = InstantFormatInfo.withCustomPattern("a");
            InstantFormatInfo b = InstantFormatInfo.withCustomPattern("b");
            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("Not equal to null or string")
        void notEqualToNullOrDiffType() {
            InstantFormatInfo info = new InstantFormatInfo();
            assertThat(info).isNotEqualTo(null);
            assertThat(info).isNotEqualTo("string");
        }

        @Test
        @DisplayName("Equal to itself")
        void equalToSelf() {
            InstantFormatInfo info = new InstantFormatInfo();
            assertThat(info).isEqualTo(info);
        }

        @Test
        @DisplayName("toString contains pattern and info")
        void toStringInfo() {
            InstantFormatInfo info = InstantFormatInfo.withCustomPattern("custom");
            assertThat(info.toString()).contains("InstantFormatInfo");
            assertThat(info.toString()).contains("custom");
        }
    }
}
