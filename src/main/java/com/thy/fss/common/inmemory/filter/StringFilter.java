package com.thy.fss.common.inmemory.filter;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * String filter class for String field types.
 * Extends the base Filter class with string-specific filtering operations.
 * This class follows the JHipster filter pattern for consistency and compatibility.
 */
public class StringFilter extends Filter<String> {

    @JsonProperty("cont")
    private String contains;
    @JsonProperty("start")
    private String startsWith;
    @JsonProperty("end")
    private String endsWith;
    @JsonProperty("match")
    private String matches;
    @JsonProperty("empty")
    private Boolean isEmpty;
    @JsonProperty("nempty")
    private Boolean isNotEmpty;
    @JsonProperty("blank")
    private Boolean isBlank;
    @JsonProperty("nblank")
    private Boolean isNotBlank;

    /**
     * Default constructor.
     */
    public StringFilter() {
        super();
    }

    /**
     * Copy constructor.
     *
     * @param filter The string filter to copy from
     */
    public StringFilter(StringFilter filter) {
        super(filter);
        this.contains = filter.contains;
        this.startsWith = filter.startsWith;
        this.endsWith = filter.endsWith;
        this.matches = filter.matches;
        this.isEmpty = filter.isEmpty;
        this.isNotEmpty = filter.isNotEmpty;
        this.isBlank = filter.isBlank;
        this.isNotBlank = filter.isNotBlank;
    }

    /**
     * Gets the contains filter value.
     *
     * @return The substring that the field must contain
     */
    public String getContains() {
        return contains;
    }

    /**
     * Sets the contains filter value.
     *
     * @param contains The substring that the field must contain
     * @return This filter instance for method chaining
     */
    public StringFilter setContains(String contains) {
        this.contains = contains;
        return this;
    }

    /**
     * Gets the startsWith filter value.
     *
     * @return The prefix that the field must start with
     */
    public String getStartsWith() {
        return startsWith;
    }

    /**
     * Sets the startsWith filter value.
     *
     * @param startsWith The prefix that the field must start with
     * @return This filter instance for method chaining
     */
    public StringFilter setStartsWith(String startsWith) {
        this.startsWith = startsWith;
        return this;
    }

    /**
     * Gets the endsWith filter value.
     *
     * @return The suffix that the field must end with
     */
    public String getEndsWith() {
        return endsWith;
    }

    /**
     * Sets the endsWith filter value.
     *
     * @param endsWith The suffix that the field must end with
     * @return This filter instance for method chaining
     */
    public StringFilter setEndsWith(String endsWith) {
        this.endsWith = endsWith;
        return this;
    }

    /**
     * Gets the matches filter value.
     *
     * @return The regular expression pattern that the field must match
     */
    public String getMatches() {
        return matches;
    }

    /**
     * Sets the matches filter value.
     *
     * @param matches The regular expression pattern that the field must match
     * @return This filter instance for method chaining
     */
    public StringFilter setMatches(String matches) {
        this.matches = matches;
        return this;
    }

    /**
     * Gets the isEmpty filter value.
     *
     * @return True to match empty strings, false to match non-empty strings, null to ignore
     */
    public Boolean getIsEmpty() {
        return isEmpty;
    }

    /**
     * Sets the isEmpty filter value.
     *
     * @param isEmpty True to match empty strings, false to match non-empty strings, null to ignore
     * @return This filter instance for method chaining
     */
    public StringFilter setIsEmpty(Boolean isEmpty) {
        this.isEmpty = isEmpty;
        return this;
    }

    /**
     * Gets the isBlank filter value.
     *
     * @return True to match blank strings, false to match non-blank strings, null to ignore
     */
    public Boolean getIsBlank() {
        return isBlank;
    }

    /**
     * Sets the isBlank filter value.
     *
     * @param isBlank True to match blank strings, false to match non-blank strings, null to ignore
     * @return This filter instance for method chaining
     */
    public StringFilter setIsBlank(Boolean isBlank) {
        this.isBlank = isBlank;
        return this;
    }

    /**
     * Gets the isNotEmpty filter value.
     *
     * @return True to match non-empty strings, false to match empty strings, null to ignore
     */
    public Boolean getIsNotEmpty() {
        return isNotEmpty;
    }

    /**
     * Sets the isNotEmpty filter value.
     *
     * @param isNotEmpty True to match non-empty strings, false to match empty strings, null to ignore
     * @return This filter instance for method chaining
     */
    public StringFilter setIsNotEmpty(Boolean isNotEmpty) {
        this.isNotEmpty = isNotEmpty;
        return this;
    }

    /**
     * Gets the isNotBlank filter value.
     *
     * @return True to match non-blank strings, false to match blank strings, null to ignore
     */
    public Boolean getIsNotBlank() {
        return isNotBlank;
    }

    /**
     * Sets the isNotBlank filter value.
     *
     * @param isNotBlank True to match non-blank strings, false to match blank strings, null to ignore
     * @return This filter instance for method chaining
     */
    public StringFilter setIsNotBlank(Boolean isNotBlank) {
        this.isNotBlank = isNotBlank;
        return this;
    }

    @Override
    public StringFilter setEquals(String equals) {
        super.setEquals(equals);
        return this;
    }

    @Override
    public StringFilter setNotEquals(String notEquals) {
        super.setNotEquals(notEquals);
        return this;
    }

    @Override
    public StringFilter setIsNull(Boolean isNull) {
        super.setIsNull(isNull);
        return this;
    }

    @Override
    public StringFilter setIsNotNull(Boolean isNotNull) {
        super.setIsNotNull(isNotNull);
        return this;
    }

    @Override
    public StringFilter setIn(java.util.Collection<String> in) {
        super.setIn(in);
        return this;
    }

    @Override
    public StringFilter setNotIn(java.util.Collection<String> notIn) {
        super.setNotIn(notIn);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        StringFilter that = (StringFilter) o;
        return Objects.equals(contains, that.contains) &&
                Objects.equals(startsWith, that.startsWith) &&
                Objects.equals(endsWith, that.endsWith) &&
                Objects.equals(matches, that.matches) &&
                Objects.equals(isEmpty, that.isEmpty) &&
                Objects.equals(isNotEmpty, that.isNotEmpty) &&
                Objects.equals(isBlank, that.isBlank) &&
                Objects.equals(isNotBlank, that.isNotBlank);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), contains, startsWith, endsWith, matches, isEmpty, isNotEmpty, isBlank, isNotBlank);
    }

    @Override
    public String toString() {
        return "StringFilter{" +
                "equals=" + getEquals() +
                ", specified=" + getIsNull() +
                ", in=" + getIn() +
                ", notIn=" + getNotIn() +
                ", contains='" + contains + '\'' +
                ", startsWith='" + startsWith + '\'' +
                ", endsWith='" + endsWith + '\'' +
                ", matches='" + matches + '\'' +
                ", isEmpty=" + isEmpty +
                ", isNotEmpty=" + isNotEmpty +
                ", isBlank=" + isBlank +
                ", isNotBlank=" + isNotBlank +
                '}';
    }
}