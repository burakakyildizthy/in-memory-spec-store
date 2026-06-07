package com.thy.fss.common.inmemory.processor.model;

import com.thy.fss.common.inmemory.processor.model.EnumDeserializationInfo.DeserializationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for FilterFieldConfig model class.
 */
@DisplayName("FilterFieldConfig Tests")
class FilterFieldConfigTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("No-arg constructor")
        void noArgConstructor() {
            FilterFieldConfig config = new FilterFieldConfig();
            assertThat(config.getFieldName()).isNull();
            assertThat(config.getFieldType()).isNull();
            assertThat(config.getFilterType()).isNull();
            assertThat(config.isEnum()).isFalse();
            assertThat(config.isCollection()).isFalse();
            assertThat(config.isTemporal()).isFalse();
            assertThat(config.isNumeric()).isFalse();
            assertThat(config.isString()).isFalse();
            assertThat(config.isModel()).isFalse();
        }

        @Test
        @DisplayName("Three-arg constructor")
        void threeArgConstructor() {
            FilterFieldConfig config = new FilterFieldConfig("name", "java.lang.String", "StringFilter");
            assertThat(config.getFieldName()).isEqualTo("name");
            assertThat(config.getFieldType()).isEqualTo("java.lang.String");
            assertThat(config.getFilterType()).isEqualTo("StringFilter");
        }
    }

    @Nested
    @DisplayName("Getters and Setters")
    class GettersSetters {

        @Test
        @DisplayName("fieldName")
        void fieldName() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setFieldName("age");
            assertThat(config.getFieldName()).isEqualTo("age");
        }

        @Test
        @DisplayName("fieldType")
        void fieldType() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setFieldType("java.lang.Integer");
            assertThat(config.getFieldType()).isEqualTo("java.lang.Integer");
        }

        @Test
        @DisplayName("filterType")
        void filterType() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setFilterType("IntegerFilter");
            assertThat(config.getFilterType()).isEqualTo("IntegerFilter");
        }

        @Test
        @DisplayName("jacksonAnnotations")
        void jacksonAnnotations() {
            FilterFieldConfig config = new FilterFieldConfig();
            List<AnnotationInfo> annotations = List.of(new AnnotationInfo("JsonProperty", Map.of()));
            config.setJacksonAnnotations(annotations);
            assertThat(config.getJacksonAnnotations()).isEqualTo(annotations);
        }

        @Test
        @DisplayName("isEnum")
        void isEnum() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setEnum(true);
            assertThat(config.isEnum()).isTrue();
        }

        @Test
        @DisplayName("isModel")
        void isModel() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setModel(true);
            assertThat(config.isModel()).isTrue();
        }

        @Test
        @DisplayName("isCollection")
        void isCollection() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setCollection(true);
            assertThat(config.isCollection()).isTrue();
        }

        @Test
        @DisplayName("isTemporal")
        void isTemporal() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setTemporal(true);
            assertThat(config.isTemporal()).isTrue();
        }

        @Test
        @DisplayName("isNumeric")
        void isNumeric() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setNumeric(true);
            assertThat(config.isNumeric()).isTrue();
        }

        @Test
        @DisplayName("isString")
        void isString() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setString(true);
            assertThat(config.isString()).isTrue();
        }

        @Test
        @DisplayName("elementType")
        void elementType() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setElementType("Item");
            assertThat(config.getElementType()).isEqualTo("Item");
        }

        @Test
        @DisplayName("isModelElementType")
        void isModelElementType() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setModelElementType(true);
            assertThat(config.isModelElementType()).isTrue();
        }

        @Test
        @DisplayName("elementFilterType")
        void elementFilterType() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setElementFilterType("ItemFilter");
            assertThat(config.getElementFilterType()).isEqualTo("ItemFilter");
        }

        @Test
        @DisplayName("elementFilterPackage")
        void elementFilterPackage() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setElementFilterPackage("com.example");
            assertThat(config.getElementFilterPackage()).isEqualTo("com.example");
        }

        @Test
        @DisplayName("dateTimeFormatInfo")
        void dateTimeFormatInfo() {
            FilterFieldConfig config = new FilterFieldConfig();
            DateTimeFormatInfo info = DateTimeFormatInfo.forFieldType("java.time.LocalDate");
            config.setDateTimeFormatInfo(info);
            assertThat(config.getDateTimeFormatInfo()).isEqualTo(info);
        }

        @Test
        @DisplayName("instantFormatInfo")
        void instantFormatInfo() {
            FilterFieldConfig config = new FilterFieldConfig();
            InstantFormatInfo info = InstantFormatInfo.withDefaultPattern();
            config.setInstantFormatInfo(info);
            assertThat(config.getInstantFormatInfo()).isEqualTo(info);
        }

        @Test
        @DisplayName("enumDeserializationInfo")
        void enumDeserializationInfo() {
            FilterFieldConfig config = new FilterFieldConfig();
            EnumDeserializationInfo info = new EnumDeserializationInfo("com.test.Status");
            config.setEnumDeserializationInfo(info);
            assertThat(config.getEnumDeserializationInfo()).isEqualTo(info);
        }

        @Test
        @DisplayName("packageName")
        void packageName() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setPackageName("com.example.filter");
            assertThat(config.getPackageName()).isEqualTo("com.example.filter");
        }
    }

    @Nested
    @DisplayName("getElementFilterQualifiedName")
    class ElementFilterQualifiedName {

        @Test
        @DisplayName("Returns null when not model element type")
        void notModelElement() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setModelElementType(false);
            config.setElementFilterType("ItemFilter");
            assertThat(config.getElementFilterQualifiedName()).isNull();
        }

        @Test
        @DisplayName("Returns null when element filter type is null")
        void nullFilterType() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setModelElementType(true);
            config.setElementFilterType(null);
            assertThat(config.getElementFilterQualifiedName()).isNull();
        }

        @Test
        @DisplayName("Returns type only when package is null")
        void nullPackage() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setModelElementType(true);
            config.setElementFilterType("ItemFilter");
            config.setElementFilterPackage(null);
            assertThat(config.getElementFilterQualifiedName()).isEqualTo("ItemFilter");
        }

        @Test
        @DisplayName("Returns type only when package is empty")
        void emptyPackage() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setModelElementType(true);
            config.setElementFilterType("ItemFilter");
            config.setElementFilterPackage("");
            assertThat(config.getElementFilterQualifiedName()).isEqualTo("ItemFilter");
        }

        @Test
        @DisplayName("Returns fully qualified name")
        void fullyQualified() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setModelElementType(true);
            config.setElementFilterType("ItemFilter");
            config.setElementFilterPackage("com.example.filter");
            assertThat(config.getElementFilterQualifiedName()).isEqualTo("com.example.filter.ItemFilter");
        }
    }

    @Nested
    @DisplayName("hasJacksonAnnotations")
    class HasJacksonAnnotations {

        @Test
        @DisplayName("Returns false when null")
        void nullAnnotations() {
            FilterFieldConfig config = new FilterFieldConfig();
            assertThat(config.hasJacksonAnnotations()).isFalse();
        }

        @Test
        @DisplayName("Returns false when empty")
        void emptyAnnotations() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setJacksonAnnotations(List.of());
            assertThat(config.hasJacksonAnnotations()).isFalse();
        }

        @Test
        @DisplayName("Returns true when annotations present")
        void hasAnnotations() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setJacksonAnnotations(List.of(new AnnotationInfo("JsonProperty", Map.of())));
            assertThat(config.hasJacksonAnnotations()).isTrue();
        }
    }

    @Nested
    @DisplayName("hasCustomDateTimeFormat")
    class HasCustomDateTimeFormat {

        @Test
        @DisplayName("Returns false when info is null")
        void nullInfo() {
            FilterFieldConfig config = new FilterFieldConfig();
            assertThat(config.hasCustomDateTimeFormat()).isFalse();
        }

        @Test
        @DisplayName("Returns false when no custom format")
        void noCustomFormat() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setDateTimeFormatInfo(DateTimeFormatInfo.forFieldType("java.time.LocalDate"));
            assertThat(config.hasCustomDateTimeFormat()).isFalse();
        }

        @Test
        @DisplayName("Returns true when custom format present")
        void customFormatPresent() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setDateTimeFormatInfo(DateTimeFormatInfo.withCustomPattern("yyyy", "java.time.LocalDate"));
            assertThat(config.hasCustomDateTimeFormat()).isTrue();
        }
    }

    @Nested
    @DisplayName("hasCustomInstantFormat")
    class HasCustomInstantFormat {

        @Test
        @DisplayName("Returns false when info is null")
        void nullInfo() {
            FilterFieldConfig config = new FilterFieldConfig();
            assertThat(config.hasCustomInstantFormat()).isFalse();
        }

        @Test
        @DisplayName("Returns false when no custom format")
        void noCustomFormat() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setInstantFormatInfo(InstantFormatInfo.withDefaultPattern());
            assertThat(config.hasCustomInstantFormat()).isFalse();
        }

        @Test
        @DisplayName("Returns true when custom format present")
        void customPresent() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setInstantFormatInfo(InstantFormatInfo.withCustomPattern("custom"));
            assertThat(config.hasCustomInstantFormat()).isTrue();
        }
    }

    @Nested
    @DisplayName("hasCustomEnumDeserialization")
    class HasCustomEnumDeserialization {

        @Test
        @DisplayName("Returns false when info is null")
        void nullInfo() {
            FilterFieldConfig config = new FilterFieldConfig();
            assertThat(config.hasCustomEnumDeserialization()).isFalse();
        }

        @Test
        @DisplayName("Returns false for DEFAULT_MATCHING")
        void defaultMatching() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setEnumDeserializationInfo(new EnumDeserializationInfo());
            assertThat(config.hasCustomEnumDeserialization()).isFalse();
        }

        @Test
        @DisplayName("Returns true for CREATOR_METHOD")
        void creatorMethod() {
            FilterFieldConfig config = new FilterFieldConfig();
            EnumDeserializationInfo enumInfo = new EnumDeserializationInfo();
            enumInfo.setDeserializationType(DeserializationType.CREATOR_METHOD);
            config.setEnumDeserializationInfo(enumInfo);
            assertThat(config.hasCustomEnumDeserialization()).isTrue();
        }
    }

    @Nested
    @DisplayName("getEffectiveDateTimePattern")
    class EffectiveDateTimePattern {

        @Test
        @DisplayName("Returns null when info is null")
        void nullInfo() {
            FilterFieldConfig config = new FilterFieldConfig();
            assertThat(config.getEffectiveDateTimePattern()).isNull();
        }

        @Test
        @DisplayName("Returns pattern from info")
        void fromInfo() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setDateTimeFormatInfo(DateTimeFormatInfo.withCustomPattern("yyyy-MM-dd", "java.time.LocalDate"));
            assertThat(config.getEffectiveDateTimePattern()).isEqualTo("yyyy-MM-dd");
        }
    }

    @Nested
    @DisplayName("getEffectiveInstantPattern")
    class EffectiveInstantPattern {

        @Test
        @DisplayName("Returns null when info is null")
        void nullInfo() {
            FilterFieldConfig config = new FilterFieldConfig();
            assertThat(config.getEffectiveInstantPattern()).isNull();
        }

        @Test
        @DisplayName("Returns pattern from info")
        void fromInfo() {
            FilterFieldConfig config = new FilterFieldConfig();
            config.setInstantFormatInfo(InstantFormatInfo.withCustomPattern("custom"));
            assertThat(config.getEffectiveInstantPattern()).isEqualTo("custom");
        }
    }

    @Nested
    @DisplayName("getEnumDeserializationType")
    class EnumDeserializationTypeTests {

        @Test
        @DisplayName("Returns null when info is null")
        void nullInfo() {
            FilterFieldConfig config = new FilterFieldConfig();
            assertThat(config.getEnumDeserializationType()).isNull();
        }

        @Test
        @DisplayName("Returns type from info")
        void fromInfo() {
            FilterFieldConfig config = new FilterFieldConfig();
            EnumDeserializationInfo info = new EnumDeserializationInfo();
            info.setDeserializationType(DeserializationType.VALUE_METHOD);
            config.setEnumDeserializationInfo(info);
            assertThat(config.getEnumDeserializationType()).isEqualTo(DeserializationType.VALUE_METHOD);
        }
    }

    @Nested
    @DisplayName("equals, hashCode, toString")
    class EqualsHashCodeToString {

        @Test
        @DisplayName("Equal objects")
        void equalObjects() {
            FilterFieldConfig a = new FilterFieldConfig("name", "String", "StringFilter");
            a.setEnum(false);
            a.setCollection(false);
            a.setString(true);

            FilterFieldConfig b = new FilterFieldConfig("name", "String", "StringFilter");
            b.setEnum(false);
            b.setCollection(false);
            b.setString(true);

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("Not equal with different field name")
        void notEqualDifferentName() {
            FilterFieldConfig a = new FilterFieldConfig("name", "String", "StringFilter");
            FilterFieldConfig b = new FilterFieldConfig("age", "String", "StringFilter");
            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("Not equal to null or different type")
        void notEqualToNullOrDiffType() {
            FilterFieldConfig config = new FilterFieldConfig("x", "y", "z");
            assertThat(config).isNotEqualTo(null);
            assertThat(config).isNotEqualTo("string");
        }

        @Test
        @DisplayName("Equal to itself")
        void equalToSelf() {
            FilterFieldConfig config = new FilterFieldConfig("x", "y", "z");
            assertThat(config).isEqualTo(config);
        }

        @Test
        @DisplayName("toString contains field info")
        void toStringInfo() {
            FilterFieldConfig config = new FilterFieldConfig("name", "String", "StringFilter");
            config.setString(true);
            String str = config.toString();
            assertThat(str).contains("FilterFieldConfig");
            assertThat(str).contains("name");
            assertThat(str).contains("String");
        }
    }
}
