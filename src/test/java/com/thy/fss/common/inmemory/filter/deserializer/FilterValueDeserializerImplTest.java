package com.thy.fss.common.inmemory.filter.deserializer;

import com.thy.fss.common.inmemory.filter.deserializer.FieldDeserializationConfig;
import com.thy.fss.common.inmemory.filter.deserializer.EnumDeserializationInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.*;

@DisplayName("FilterValueDeserializerImpl Tests")
class FilterValueDeserializerImplTest {

    private FilterValueDeserializerImpl deserializer;

    @BeforeEach
    void setUp() {
        deserializer = new FilterValueDeserializerImpl();
    }

    // ===== deserializeValue - String =====

    @Test
    void deserializeValue_string_returnsValue() {
        assertThat(deserializer.deserializeValue("hello", String.class, null)).isEqualTo("hello");
    }

    @Test
    void deserializeValue_null_returnsNull() {
        assertThat(deserializer.deserializeValue(null, String.class, null)).isNull();
    }

    // ===== deserializeValue - Boolean =====

    @Test
    void deserializeValue_booleanTrue_returnsTrue() {
        assertThat(deserializer.deserializeValue("true", Boolean.class, null)).isTrue();
    }

    @Test
    void deserializeValue_booleanFalse_returnsFalse() {
        assertThat(deserializer.deserializeValue("false", Boolean.class, null)).isFalse();
    }

    @Test
    void deserializeValue_boolean1_returnsTrue() {
        assertThat(deserializer.deserializeValue("1", Boolean.class, null)).isTrue();
    }

    @Test
    void deserializeValue_boolean0_returnsFalse() {
        assertThat(deserializer.deserializeValue("0", Boolean.class, null)).isFalse();
    }

    @Test
    void deserializeValue_booleanInvalid_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeValue("yes", Boolean.class, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot parse Boolean");
    }

    // ===== deserializeValue - Integer =====

    @Test
    void deserializeValue_integer_returnsInteger() {
        assertThat(deserializer.deserializeValue("42", Integer.class, null)).isEqualTo(42);
    }

    @Test
    void deserializeValue_integer_emptyString_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeValue("", Integer.class, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deserializeValue_integer_invalid_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeValue("abc", Integer.class, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot parse Integer");
    }

    // ===== deserializeValue - Long =====

    @Test
    void deserializeValue_long_returnsLong() {
        assertThat(deserializer.deserializeValue("100", Long.class, null)).isEqualTo(100L);
    }

    @Test
    void deserializeValue_long_invalid_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeValue("xyz", Long.class, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot parse Long");
    }

    @Test
    void deserializeValue_long_empty_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeValue("", Long.class, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== deserializeValue - Double =====

    @Test
    void deserializeValue_double_returnsDouble() {
        assertThat(deserializer.deserializeValue("3.14", Double.class, null)).isEqualTo(3.14);
    }

    @Test
    void deserializeValue_double_invalid_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeValue("pi", Double.class, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot parse Double");
    }

    @Test
    void deserializeValue_double_empty_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeValue("", Double.class, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== deserializeValue - LocalDateTime =====

    @Test
    void deserializeValue_localDateTime_defaultFormat_returnsValue() {
        LocalDateTime result = deserializer.deserializeValue("2024-01-15 10:30:00", LocalDateTime.class, null);
        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2024);
        assertThat(result.getMonthValue()).isEqualTo(1);
        assertThat(result.getDayOfMonth()).isEqualTo(15);
    }

    @Test
    void deserializeValue_localDateTime_customFormat_returnsValue() {
        FieldDeserializationConfig config = new FieldDeserializationConfig()
                .setDateTimeFormatter(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        LocalDateTime result = deserializer.deserializeValue("15/01/2024 10:30:00", LocalDateTime.class, config);
        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2024);
    }

    @Test
    void deserializeValue_localDateTime_invalidFormat_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeValue("not-a-date", LocalDateTime.class, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot parse LocalDateTime");
    }

    @Test
    void deserializeValue_localDateTime_empty_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeValue("", LocalDateTime.class, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot parse null or empty");
    }

    // ===== deserializeValue - LocalDate =====

    @Test
    void deserializeValue_localDate_defaultFormat_returnsValue() {
        LocalDate result = deserializer.deserializeValue("2024-01-15", LocalDate.class, null);
        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2024);
    }

    @Test
    void deserializeValue_localDate_customFormat_returnsValue() {
        FieldDeserializationConfig config = new FieldDeserializationConfig()
                .setDateTimeFormatter(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        LocalDate result = deserializer.deserializeValue("15/01/2024", LocalDate.class, config);
        assertThat(result).isNotNull();
        assertThat(result.getYear()).isEqualTo(2024);
    }

    @Test
    void deserializeValue_localDate_invalidFormat_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeValue("not-a-date", LocalDate.class, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot parse LocalDate");
    }

    @Test
    void deserializeValue_localDate_empty_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeValue("", LocalDate.class, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== deserializeValue - Instant =====

    @Test
    void deserializeValue_instant_invalidFormat_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeValue("not-an-instant", Instant.class, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot parse Instant");
    }

    @Test
    void deserializeValue_instant_empty_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeValue("", Instant.class, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== deserializeValue - Enum =====

    enum Status { ACTIVE, INACTIVE, PENDING }

    @Test
    void deserializeValue_enum_byName_returnsValue() {
        Status result = deserializer.deserializeValue("ACTIVE", Status.class, null);
        assertThat(result).isEqualTo(Status.ACTIVE);
    }

    @Test
    void deserializeValue_enum_caseInsensitive_returnsValue() {
        Status result = deserializer.deserializeValue("active", Status.class, null);
        assertThat(result).isEqualTo(Status.ACTIVE);
    }

    @Test
    void deserializeValue_enum_invalid_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeValue("UNKNOWN", Status.class, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot parse enum");
    }

    @Test
    void deserializeValue_enum_empty_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeValue("", Status.class, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== deserializeValue - Unsupported type =====

    @Test
    void deserializeValue_unsupportedType_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeValue("value", java.util.Date.class, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported target type");
    }

    // ===== direct methods =====

    @Test
    void deserializeLocalDateTime_nullValue_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeLocalDateTime(null, DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deserializeLocalDate_nullValue_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeLocalDate(null, DateTimeFormatter.ISO_LOCAL_DATE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deserializeInstant_nullValue_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeInstant(null, DateTimeFormatter.ISO_ZONED_DATE_TIME))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deserializeBoolean_nullValue_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeBoolean(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deserializeString_returnsValue() {
        assertThat(deserializer.deserializeString("test")).isEqualTo("test");
    }

    @Test
    void deserializeInteger_valid_returnsValue() {
        assertThat(deserializer.deserializeInteger("123")).isEqualTo(123);
    }

    @Test
    void deserializeInteger_null_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeInteger(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deserializeLong_valid_returnsValue() {
        assertThat(deserializer.deserializeLong("999")).isEqualTo(999L);
    }

    @Test
    void deserializeLong_null_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeLong(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deserializeDouble_valid_returnsValue() {
        assertThat(deserializer.deserializeDouble("2.71")).isEqualTo(2.71);
    }

    @Test
    void deserializeDouble_null_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeDouble(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deserializeEnum_null_throwsException() {
        assertThatThrownBy(() -> deserializer.deserializeEnum(null, Status.class, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deserializeTemporalPreset_valid_returnsPreset() {
        var preset = deserializer.deserializeTemporalPreset("7d");
        assertThat(preset).isNotNull();
        assertThat(preset.getAmount()).isEqualTo(7);
    }

    @Test
    void deserializeEnum_withNullConfig_usesDefault() {
        Status result = deserializer.deserializeEnum("PENDING", Status.class, null);
        assertThat(result).isEqualTo(Status.PENDING);
    }

    @Test
    void deserializeEnum_withEmptyConfig_usesDefault() {
        EnumDeserializationInfo info = new EnumDeserializationInfo();
        info.setType(EnumDeserializationInfo.DeserializationType.DEFAULT_MATCHING);
        Status result = deserializer.deserializeEnum("INACTIVE", Status.class, info);
        assertThat(result).isEqualTo(Status.INACTIVE);
    }
}
