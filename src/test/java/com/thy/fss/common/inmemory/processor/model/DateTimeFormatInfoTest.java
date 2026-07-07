package com.thy.fss.common.inmemory.processor.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DateTimeFormatInfo model class.
 */
@DisplayName("DateTimeFormatInfo Tests")
class DateTimeFormatInfoTest {

    @Nested
    @DisplayName("Construction and factory methods")
    class Construction {

        @Test
        @DisplayName("No-arg constructor")
        void noArgConstructor() {
            DateTimeFormatInfo info = new DateTimeFormatInfo();
            assertThat(info.getPattern()).isNull();
            assertThat(info.isHasCustomFormat()).isFalse();
            assertThat(info.getFieldType()).isNull();
        }

        @Test
        @DisplayName("Field type constructor sets default pattern for LocalDateTime")
        void fieldTypeConstructorLocalDateTime() {
            DateTimeFormatInfo info = new DateTimeFormatInfo("java.time.LocalDateTime");
            assertThat(info.getFieldType()).isEqualTo("java.time.LocalDateTime");
            assertThat(info.isHasCustomFormat()).isFalse();
            assertThat(info.getPattern()).isNotNull();
        }

        @Test
        @DisplayName("Field type constructor sets default pattern for LocalDate")
        void fieldTypeConstructorLocalDate() {
            DateTimeFormatInfo info = new DateTimeFormatInfo("java.time.LocalDate");
            assertThat(info.getFieldType()).isEqualTo("java.time.LocalDate");
            assertThat(info.getPattern()).isNotNull();
        }

        @Test
        @DisplayName("Field type constructor sets default pattern for Instant")
        void fieldTypeConstructorInstant() {
            DateTimeFormatInfo info = new DateTimeFormatInfo("java.time.Instant");
            assertThat(info.getFieldType()).isEqualTo("java.time.Instant");
            assertThat(info.getPattern()).isNotNull();
        }

        @Test
        @DisplayName("Field type constructor handles unknown type with default")
        void fieldTypeConstructorUnknown() {
            DateTimeFormatInfo info = new DateTimeFormatInfo("some.other.Type");
            assertThat(info.getPattern()).isNotNull(); // defaults to LocalDateTime pattern
        }

        @Test
        @DisplayName("Field type constructor handles null type")
        void fieldTypeConstructorNull() {
            DateTimeFormatInfo info = new DateTimeFormatInfo((String) null);
            // null fieldType means setDefaultPatternForType returns early
            assertThat(info.getFieldType()).isNull();
        }

        @Test
        @DisplayName("Custom pattern constructor")
        void customPatternConstructor() {
            DateTimeFormatInfo info = new DateTimeFormatInfo("dd/MM/yyyy", "java.time.LocalDate");
            assertThat(info.getPattern()).isEqualTo("dd/MM/yyyy");
            assertThat(info.isHasCustomFormat()).isTrue();
            assertThat(info.getFieldType()).isEqualTo("java.time.LocalDate");
        }

        @Test
        @DisplayName("forFieldType factory method")
        void forFieldType() {
            DateTimeFormatInfo info = DateTimeFormatInfo.forFieldType("java.time.LocalDateTime");
            assertThat(info.getFieldType()).isEqualTo("java.time.LocalDateTime");
            assertThat(info.isHasCustomFormat()).isFalse();
        }

        @Test
        @DisplayName("withCustomPattern factory method")
        void withCustomPattern() {
            DateTimeFormatInfo info = DateTimeFormatInfo.withCustomPattern("yyyy-MM-dd'T'HH:mm", "java.time.LocalDateTime");
            assertThat(info.getPattern()).isEqualTo("yyyy-MM-dd'T'HH:mm");
            assertThat(info.isHasCustomFormat()).isTrue();
        }

        @Test
        @DisplayName("withDefaultPattern factory method")
        void withDefaultPattern() {
            DateTimeFormatInfo info = DateTimeFormatInfo.withDefaultPattern("java.time.LocalDate");
            assertThat(info.getFieldType()).isEqualTo("java.time.LocalDate");
            assertThat(info.isHasCustomFormat()).isFalse();
        }
    }

    @Nested
    @DisplayName("Getters and Setters")
    class GettersSetters {

        @Test
        @DisplayName("setFieldType updates default pattern when no custom format")
        void setFieldTypeUpdatesDefault() {
            DateTimeFormatInfo info = new DateTimeFormatInfo();
            info.setFieldType("java.time.LocalDate");
            assertThat(info.getPattern()).isNotNull();
        }

        @Test
        @DisplayName("setFieldType does not change pattern when using custom format")
        void setFieldTypeKeepsCustom() {
            DateTimeFormatInfo info = new DateTimeFormatInfo("custom-pattern", "java.time.LocalDateTime");
            info.setFieldType("java.time.LocalDate");
            assertThat(info.getPattern()).isEqualTo("custom-pattern");
        }

        @Test
        @DisplayName("timezone getter/setter")
        void timezone() {
            DateTimeFormatInfo info = new DateTimeFormatInfo();
            info.setTimezone("UTC");
            assertThat(info.getTimezone()).isEqualTo("UTC");
        }

        @Test
        @DisplayName("locale getter/setter")
        void locale() {
            DateTimeFormatInfo info = new DateTimeFormatInfo();
            info.setLocale("en_US");
            assertThat(info.getLocale()).isEqualTo("en_US");
        }

        @Test
        @DisplayName("hasCustomFormat getter/setter")
        void hasCustomFormat() {
            DateTimeFormatInfo info = new DateTimeFormatInfo();
            info.setHasCustomFormat(true);
            assertThat(info.isHasCustomFormat()).isTrue();
        }

        @Test
        @DisplayName("pattern getter/setter")
        void pattern() {
            DateTimeFormatInfo info = new DateTimeFormatInfo();
            info.setPattern("yyyy");
            assertThat(info.getPattern()).isEqualTo("yyyy");
        }
    }

    @Nested
    @DisplayName("Utility methods")
    class UtilityMethods {

        @Test
        @DisplayName("usesCustomFormat returns hasCustomFormat")
        void usesCustomFormat() {
            DateTimeFormatInfo info = DateTimeFormatInfo.withCustomPattern("x", "y");
            assertThat(info.usesCustomFormat()).isTrue();

            DateTimeFormatInfo info2 = DateTimeFormatInfo.forFieldType("java.time.LocalDate");
            assertThat(info2.usesCustomFormat()).isFalse();
        }

        @Test
        @DisplayName("getEffectivePattern returns pattern")
        void effectivePattern() {
            DateTimeFormatInfo info = new DateTimeFormatInfo("custom", "java.time.LocalDate");
            assertThat(info.getEffectivePattern()).isEqualTo("custom");
        }
    }

    @Nested
    @DisplayName("equals, hashCode, toString")
    class EqualsHashCodeToString {

        @Test
        @DisplayName("Equal objects")
        void equalObjects() {
            DateTimeFormatInfo a = DateTimeFormatInfo.withCustomPattern("yyyy", "java.time.LocalDate");
            a.setTimezone("UTC");
            a.setLocale("en");
            DateTimeFormatInfo b = DateTimeFormatInfo.withCustomPattern("yyyy", "java.time.LocalDate");
            b.setTimezone("UTC");
            b.setLocale("en");
            assertThat(a).isEqualTo(b);
            assertThat(a).hasSameHashCodeAs(b);
        }

        @Test
        @DisplayName("Not equal with different pattern")
        void notEqualDifferentPattern() {
            DateTimeFormatInfo a = DateTimeFormatInfo.withCustomPattern("yyyy", "java.time.LocalDate");
            DateTimeFormatInfo b = DateTimeFormatInfo.withCustomPattern("MM-dd", "java.time.LocalDate");
            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("Not equal to null")
        void notEqualToNull() {
            assertThat(DateTimeFormatInfo.forFieldType("java.time.LocalDate")).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Not equal to different type")
        void notEqualToDiffType() {
            assertThat(DateTimeFormatInfo.forFieldType("java.time.LocalDate")).isNotEqualTo("string");
        }

        @Test
        @DisplayName("Equal to itself")
        void equalToSelf() {
            DateTimeFormatInfo info = DateTimeFormatInfo.forFieldType("java.time.LocalDate");
            assertThat(info).isEqualTo(info);
        }

        @Test
        @DisplayName("toString contains field type")
        void toStringContainsInfo() {
            DateTimeFormatInfo info = DateTimeFormatInfo.forFieldType("java.time.LocalDateTime");
            assertThat(info.toString()).contains("DateTimeFormatInfo");
            assertThat(info.toString()).contains("java.time.LocalDateTime");
        }
    }
}
