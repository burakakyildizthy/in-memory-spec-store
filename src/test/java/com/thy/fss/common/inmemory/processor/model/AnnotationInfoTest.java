package com.thy.fss.common.inmemory.processor.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AnnotationInfo model class.
 */
@DisplayName("AnnotationInfo Tests")
class AnnotationInfoTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("No-arg constructor creates empty instance")
        void noArgConstructor() {
            AnnotationInfo info = new AnnotationInfo();
            assertThat(info.getAnnotationType()).isNull();
            assertThat(info.getParameters()).isNull();
            assertThat(info.getAnnotationCode()).isNull();
            assertThat(info.isAppliesToField()).isFalse();
            assertThat(info.isAppliesToGetter()).isFalse();
            assertThat(info.isAppliesToSetter()).isFalse();
        }

        @Test
        @DisplayName("Two-arg constructor sets type and parameters")
        void twoArgConstructor() {
            Map<String, Object> params = Map.of("value", "test");
            AnnotationInfo info = new AnnotationInfo("JsonProperty", params);
            assertThat(info.getAnnotationType()).isEqualTo("JsonProperty");
            assertThat(info.getParameters()).isEqualTo(params);
        }
    }

    @Nested
    @DisplayName("Getters and Setters")
    class GettersSetters {

        @Test
        @DisplayName("annotationType getter/setter")
        void annotationType() {
            AnnotationInfo info = new AnnotationInfo();
            info.setAnnotationType("JsonFormat");
            assertThat(info.getAnnotationType()).isEqualTo("JsonFormat");
        }

        @Test
        @DisplayName("parameters getter/setter")
        void parameters() {
            AnnotationInfo info = new AnnotationInfo();
            Map<String, Object> params = Map.of("pattern", "yyyy-MM-dd");
            info.setParameters(params);
            assertThat(info.getParameters()).isEqualTo(params);
        }

        @Test
        @DisplayName("annotationCode getter/setter")
        void annotationCode() {
            AnnotationInfo info = new AnnotationInfo();
            info.setAnnotationCode("@JsonProperty(\"name\")");
            assertThat(info.getAnnotationCode()).isEqualTo("@JsonProperty(\"name\")");
        }

        @Test
        @DisplayName("appliesToField getter/setter")
        void appliesToField() {
            AnnotationInfo info = new AnnotationInfo();
            info.setAppliesToField(true);
            assertThat(info.isAppliesToField()).isTrue();
        }

        @Test
        @DisplayName("appliesToGetter getter/setter")
        void appliesToGetter() {
            AnnotationInfo info = new AnnotationInfo();
            info.setAppliesToGetter(true);
            assertThat(info.isAppliesToGetter()).isTrue();
        }

        @Test
        @DisplayName("appliesToSetter getter/setter")
        void appliesToSetter() {
            AnnotationInfo info = new AnnotationInfo();
            info.setAppliesToSetter(true);
            assertThat(info.isAppliesToSetter()).isTrue();
        }
    }

    @Nested
    @DisplayName("equals and hashCode")
    class EqualsHashCode {

        @Test
        @DisplayName("Same values are equal")
        void sameValuesEqual() {
            Map<String, Object> params = Map.of("value", "test");
            AnnotationInfo a = new AnnotationInfo("JsonProperty", params);
            a.setAnnotationCode("@JsonProperty(\"test\")");
            a.setAppliesToField(true);

            AnnotationInfo b = new AnnotationInfo("JsonProperty", params);
            b.setAnnotationCode("@JsonProperty(\"test\")");
            b.setAppliesToField(true);

            assertThat(a).isEqualTo(b);
            assertThat(a).hasSameHashCodeAs(b);
        }

        @Test
        @DisplayName("Different values are not equal")
        void differentValuesNotEqual() {
            AnnotationInfo a = new AnnotationInfo("JsonProperty", Map.of());
            AnnotationInfo b = new AnnotationInfo("JsonFormat", Map.of());
            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("Not equal to null")
        void notEqualToNull() {
            AnnotationInfo info = new AnnotationInfo("JsonProperty", Map.of());
            assertThat(info).isNotEqualTo(null);
        }

        @Test
        @DisplayName("Not equal to different type")
        void notEqualToDifferentType() {
            AnnotationInfo info = new AnnotationInfo("JsonProperty", Map.of());
            assertThat(info).isNotEqualTo("string");
        }

        @Test
        @DisplayName("Equal to itself")
        void equalToSelf() {
            AnnotationInfo info = new AnnotationInfo("JsonProperty", Map.of());
            assertThat(info).isEqualTo(info);
        }

        @Test
        @DisplayName("Different appliesToGetter causes inequality")
        void differentAppliesTo() {
            AnnotationInfo a = new AnnotationInfo("X", Map.of());
            a.setAppliesToGetter(true);
            AnnotationInfo b = new AnnotationInfo("X", Map.of());
            b.setAppliesToGetter(false);
            assertThat(a).isNotEqualTo(b);
        }

        @Test
        @DisplayName("Different appliesToSetter causes inequality")
        void differentAppliesToSetter() {
            AnnotationInfo a = new AnnotationInfo("X", Map.of());
            a.setAppliesToSetter(true);
            AnnotationInfo b = new AnnotationInfo("X", Map.of());
            b.setAppliesToSetter(false);
            assertThat(a).isNotEqualTo(b);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("Contains annotation type")
        void containsType() {
            AnnotationInfo info = new AnnotationInfo("JsonFormat", Map.of("pattern", "yyyy"));
            String str = info.toString();
            assertThat(str).contains("JsonFormat").contains("AnnotationInfo");
        }
    }
}
