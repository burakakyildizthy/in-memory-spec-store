package com.thy.fss.common.inmemory.processor.model;

import com.thy.fss.common.inmemory.processor.model.EnumDeserializationInfo.DeserializationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for EnumDeserializationInfo model class.
 */
@DisplayName("EnumDeserializationInfo Tests")
class EnumDeserializationInfoTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("No-arg constructor defaults to DEFAULT_MATCHING")
        void noArgConstructor() {
            EnumDeserializationInfo info = new EnumDeserializationInfo();
            assertThat(info.getDeserializationType()).isEqualTo(DeserializationType.DEFAULT_MATCHING);
            assertThat(info.getEnumClassName()).isNull();
        }

        @Test
        @DisplayName("Class name constructor sets class and defaults to DEFAULT_MATCHING")
        void classNameConstructor() {
            EnumDeserializationInfo info = new EnumDeserializationInfo("com.example.Status");
            assertThat(info.getEnumClassName()).isEqualTo("com.example.Status");
            assertThat(info.getDeserializationType()).isEqualTo(DeserializationType.DEFAULT_MATCHING);
        }
    }

    @Nested
    @DisplayName("Getters and Setters")
    class GettersSetters {

        @Test
        @DisplayName("deserializationType getter/setter")
        void deserializationType() {
            EnumDeserializationInfo info = new EnumDeserializationInfo();
            info.setDeserializationType(DeserializationType.CREATOR_METHOD);
            assertThat(info.getDeserializationType()).isEqualTo(DeserializationType.CREATOR_METHOD);
        }

        @Test
        @DisplayName("jsonCreatorMethod getter/setter")
        void jsonCreatorMethod() {
            EnumDeserializationInfo info = new EnumDeserializationInfo();
            info.setJsonCreatorMethod("fromValue");
            assertThat(info.getJsonCreatorMethod()).isEqualTo("fromValue");
        }

        @Test
        @DisplayName("jsonValueField getter/setter")
        void jsonValueField() {
            EnumDeserializationInfo info = new EnumDeserializationInfo();
            info.setJsonValueField("code");
            assertThat(info.getJsonValueField()).isEqualTo("code");
        }

        @Test
        @DisplayName("jsonValueMethod getter/setter")
        void jsonValueMethod() {
            EnumDeserializationInfo info = new EnumDeserializationInfo();
            info.setJsonValueMethod("getCode");
            assertThat(info.getJsonValueMethod()).isEqualTo("getCode");
        }

        @Test
        @DisplayName("enumClassName getter/setter")
        void enumClassName() {
            EnumDeserializationInfo info = new EnumDeserializationInfo();
            info.setEnumClassName("com.example.Color");
            assertThat(info.getEnumClassName()).isEqualTo("com.example.Color");
        }
    }

    @Nested
    @DisplayName("hasCustomDeserialization")
    class HasCustomDeserialization {

        @Test
        @DisplayName("Returns false for DEFAULT_MATCHING")
        void defaultMatching() {
            EnumDeserializationInfo info = new EnumDeserializationInfo();
            assertThat(info.hasCustomDeserialization()).isFalse();
        }

        @Test
        @DisplayName("Returns true for CREATOR_METHOD")
        void creatorMethod() {
            EnumDeserializationInfo info = new EnumDeserializationInfo();
            info.setDeserializationType(DeserializationType.CREATOR_METHOD);
            assertThat(info.hasCustomDeserialization()).isTrue();
        }

        @Test
        @DisplayName("Returns true for VALUE_FIELD")
        void valueField() {
            EnumDeserializationInfo info = new EnumDeserializationInfo();
            info.setDeserializationType(DeserializationType.VALUE_FIELD);
            assertThat(info.hasCustomDeserialization()).isTrue();
        }

        @Test
        @DisplayName("Returns true for VALUE_METHOD")
        void valueMethod() {
            EnumDeserializationInfo info = new EnumDeserializationInfo();
            info.setDeserializationType(DeserializationType.VALUE_METHOD);
            assertThat(info.hasCustomDeserialization()).isTrue();
        }
    }

    @Nested
    @DisplayName("getDeserializationTarget")
    class GetDeserializationTarget {

        @Test
        @DisplayName("Returns jsonCreatorMethod for CREATOR_METHOD")
        void creatorMethod() {
            EnumDeserializationInfo info = new EnumDeserializationInfo();
            info.setDeserializationType(DeserializationType.CREATOR_METHOD);
            info.setJsonCreatorMethod("fromVal");
            assertThat(info.getDeserializationTarget()).isEqualTo("fromVal");
        }

        @Test
        @DisplayName("Returns jsonValueField for VALUE_FIELD")
        void valueField() {
            EnumDeserializationInfo info = new EnumDeserializationInfo();
            info.setDeserializationType(DeserializationType.VALUE_FIELD);
            info.setJsonValueField("code");
            assertThat(info.getDeserializationTarget()).isEqualTo("code");
        }

        @Test
        @DisplayName("Returns jsonValueMethod for VALUE_METHOD")
        void valueMethod() {
            EnumDeserializationInfo info = new EnumDeserializationInfo();
            info.setDeserializationType(DeserializationType.VALUE_METHOD);
            info.setJsonValueMethod("getCode");
            assertThat(info.getDeserializationTarget()).isEqualTo("getCode");
        }

        @Test
        @DisplayName("Returns null for DEFAULT_MATCHING")
        void defaultMatching() {
            EnumDeserializationInfo info = new EnumDeserializationInfo();
            assertThat(info.getDeserializationTarget()).isNull();
        }
    }

    @Nested
    @DisplayName("equals, hashCode, toString")
    class EqualsHashCodeToString {

        @Test
        @DisplayName("Equal objects")
        void equalObjects() {
            EnumDeserializationInfo a = new EnumDeserializationInfo("com.example.Status");
            a.setDeserializationType(DeserializationType.CREATOR_METHOD);
            a.setJsonCreatorMethod("fromVal");

            EnumDeserializationInfo b = new EnumDeserializationInfo("com.example.Status");
            b.setDeserializationType(DeserializationType.CREATOR_METHOD);
            b.setJsonCreatorMethod("fromVal");

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
        }

        @Test
        @DisplayName("Not equal with different type")
        void notEqualDifferentType() {
            EnumDeserializationInfo a = new EnumDeserializationInfo("A");
            a.setDeserializationType(DeserializationType.CREATOR_METHOD);
            EnumDeserializationInfo b = new EnumDeserializationInfo("A");
            b.setDeserializationType(DeserializationType.VALUE_FIELD);
            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("Not equal to null or different class")
        void notEqualToNullOrDifferent() {
            EnumDeserializationInfo info = new EnumDeserializationInfo("X");
            assertThat(info).isNotEqualTo(null);
            assertThat(info).isNotEqualTo("string");
        }

        @Test
        @DisplayName("Equal to itself")
        void equalToSelf() {
            EnumDeserializationInfo info = new EnumDeserializationInfo("X");
            assertThat(info).isEqualTo(info);
        }

        @Test
        @DisplayName("toString contains class name and type")
        void toStringInfo() {
            EnumDeserializationInfo info = new EnumDeserializationInfo("com.test.Status");
            info.setDeserializationType(DeserializationType.CREATOR_METHOD);
            String str = info.toString();
            assertThat(str).contains("EnumDeserializationInfo");
            assertThat(str).contains("com.test.Status");
            assertThat(str).contains("CREATOR_METHOD");
        }
    }

    @Nested
    @DisplayName("DeserializationType enum")
    class DeserializationTypeEnum {

        @Test
        @DisplayName("Has all four values")
        void allValues() {
            assertThat(DeserializationType.values()).hasSize(4);
            assertThat(DeserializationType.values()).containsExactly(
                    DeserializationType.CREATOR_METHOD,
                    DeserializationType.VALUE_FIELD,
                    DeserializationType.VALUE_METHOD,
                    DeserializationType.DEFAULT_MATCHING
            );
        }
    }
}
