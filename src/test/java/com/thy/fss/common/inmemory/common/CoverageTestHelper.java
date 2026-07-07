package com.thy.fss.common.inmemory.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Utility for testing edge cases and boundary conditions to improve coverage.
 * Provides reusable patterns for null safety, empty collections, boundary values, and exceptions.
 */
public class CoverageTestHelper {

    private CoverageTestHelper() {
        // Utility class
    }

    /**
     * Tests null safety for a method that accepts a nullable parameter.
     * 
     * @param <T> parameter type
     * @param <R> return type
     * @param method method to test
     * @param expectedResult expected result when null is passed
     */
    public static <T, R> void testNullSafety(Function<T, R> method, R expectedResult) {
        R result = method.apply(null);
        assertThat(result).isEqualTo(expectedResult);
    }

    /**
     * Tests null safety for a method that should throw an exception.
     * 
     * @param <T> parameter type
     * @param method method to test
     * @param expectedExceptionType expected exception type
     */
    public static <T> void testNullSafetyThrows(
            Consumer<T> method,
            Class<? extends Exception> expectedExceptionType) {
        assertThatThrownBy(() -> method.accept(null))
            .isInstanceOf(expectedExceptionType);
    }

    /**
     * Tests null safety for a supplier method that should throw an exception.
     * 
     * @param method method to test
     * @param expectedExceptionType expected exception type
     */
    public static void testNullSafetyThrows(
            Runnable method,
            Class<? extends Exception> expectedExceptionType) {
        assertThatThrownBy(method::run)
            .isInstanceOf(expectedExceptionType);
    }

    /**
     * Tests empty collection handling.
     * 
     * @param <T> element type
     * @param <R> return type
     * @param method method to test
     * @param expectedResult expected result for empty collection
     */
    public static <T, R> void testEmptyCollection(
            Function<List<T>, R> method,
            R expectedResult) {
        R result = method.apply(Collections.emptyList());
        assertThat(result).isEqualTo(expectedResult);
    }

    /**
     * Tests empty collection handling with custom assertion.
     * 
     * @param <T> element type
     * @param <R> return type
     * @param method method to test
     * @param assertion custom assertion for result
     */
    public static <T, R> void testEmptyCollection(
            Function<List<T>, R> method,
            Consumer<R> assertion) {
        R result = method.apply(Collections.emptyList());
        assertion.accept(result);
    }

    /**
     * Tests boundary values for integer parameters.
     * 
     * @param method method to test
     * @param assertion assertion for each boundary value
     */
    public static void testBoundaryValues(
            Function<Integer, ?> method,
            Consumer<Object> assertion) {
        List<Integer> boundaryValues = List.of(
            Integer.MIN_VALUE,
            -1,
            0,
            1,
            Integer.MAX_VALUE
        );
        
        for (Integer value : boundaryValues) {
            Object result = method.apply(value);
            assertion.accept(result);
        }
    }

    /**
     * Tests boundary values for long parameters.
     * 
     * @param method method to test
     * @param assertion assertion for each boundary value
     */
    public static void testBoundaryValuesLong(
            Function<Long, ?> method,
            Consumer<Object> assertion) {
        List<Long> boundaryValues = List.of(
            Long.MIN_VALUE,
            -1L,
            0L,
            1L,
            Long.MAX_VALUE
        );
        
        for (Long value : boundaryValues) {
            Object result = method.apply(value);
            assertion.accept(result);
        }
    }

    /**
     * Tests boundary values for double parameters.
     * 
     * @param method method to test
     * @param assertion assertion for each boundary value
     */
    public static void testBoundaryValuesDouble(
            Function<Double, ?> method,
            Consumer<Object> assertion) {
        List<Double> boundaryValues = List.of(
            Double.MIN_VALUE,
            -1.0,
            0.0,
            1.0,
            Double.MAX_VALUE,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
        );
        
        for (Double value : boundaryValues) {
            Object result = method.apply(value);
            assertion.accept(result);
        }
    }

    /**
     * Tests exception scenarios with expected exception type.
     * 
     * @param method method to test
     * @param expectedExceptionType expected exception type
     */
    public static void testExceptionScenarios(
            Runnable method,
            Class<? extends Exception> expectedExceptionType) {
        assertThatThrownBy(method::run)
            .isInstanceOf(expectedExceptionType);
    }

    /**
     * Tests exception scenarios with expected exception type and message.
     * 
     * @param method method to test
     * @param expectedExceptionType expected exception type
     * @param expectedMessagePart expected message part
     */
    public static void testExceptionScenarios(
            Runnable method,
            Class<? extends Exception> expectedExceptionType,
            String expectedMessagePart) {
        assertThatThrownBy(method::run)
            .isInstanceOf(expectedExceptionType)
            .hasMessageContaining(expectedMessagePart);
    }

    /**
     * Tests exception scenarios with supplier.
     * 
     * @param <T> return type
     * @param method method to test
     * @param expectedExceptionType expected exception type
     */
    public static <T> void testExceptionScenariosSupplier(
            Supplier<T> method,
            Class<? extends Exception> expectedExceptionType) {
        assertThatThrownBy(method::get)
            .isInstanceOf(expectedExceptionType);
    }

    /**
     * Tests exception scenarios with supplier and message.
     * 
     * @param <T> return type
     * @param method method to test
     * @param expectedExceptionType expected exception type
     * @param expectedMessagePart expected message part
     */
    public static <T> void testExceptionScenariosSupplier(
            Supplier<T> method,
            Class<? extends Exception> expectedExceptionType,
            String expectedMessagePart) {
        assertThatThrownBy(method::get)
            .isInstanceOf(expectedExceptionType)
            .hasMessageContaining(expectedMessagePart);
    }

    /**
     * Tests all common edge cases for a collection-based method.
     * 
     * @param <T> element type
     * @param <R> return type
     * @param method method to test
     * @param sampleElement sample element for single-item test
     * @param nullAssertion assertion for null input
     * @param emptyAssertion assertion for empty collection
     * @param singleAssertion assertion for single-item collection
     */
    public static <T, R> void testAllCollectionEdgeCases(
            Function<List<T>, R> method,
            T sampleElement,
            Consumer<R> nullAssertion,
            Consumer<R> emptyAssertion,
            Consumer<R> singleAssertion) {
        
        // Test null
        R nullResult = method.apply(null);
        nullAssertion.accept(nullResult);
        
        // Test empty
        R emptyResult = method.apply(Collections.emptyList());
        emptyAssertion.accept(emptyResult);
        
        // Test single item
        List<T> singleItem = new ArrayList<>();
        singleItem.add(sampleElement);
        R singleResult = method.apply(singleItem);
        singleAssertion.accept(singleResult);
    }

    /**
     * Tests string edge cases (null, empty, blank, whitespace).
     * 
     * @param <R> return type
     * @param method method to test
     * @param nullAssertion assertion for null
     * @param emptyAssertion assertion for empty string
     * @param blankAssertion assertion for blank string
     * @param whitespaceAssertion assertion for whitespace string
     */
    public static <R> void testStringEdgeCases(
            Function<String, R> method,
            Consumer<R> nullAssertion,
            Consumer<R> emptyAssertion,
            Consumer<R> blankAssertion,
            Consumer<R> whitespaceAssertion) {
        
        // Test null
        R nullResult = method.apply(null);
        nullAssertion.accept(nullResult);
        
        // Test empty
        R emptyResult = method.apply("");
        emptyAssertion.accept(emptyResult);
        
        // Test blank
        R blankResult = method.apply("   ");
        blankAssertion.accept(blankResult);
        
        // Test whitespace variations
        R whitespaceResult = method.apply("\t\n\r");
        whitespaceAssertion.accept(whitespaceResult);
    }

    /**
     * Tests concurrent access scenarios.
     * 
     * @param method method to test concurrently
     * @param threadCount number of concurrent threads
     * @param iterationsPerThread iterations per thread
     */
    public static void testConcurrentAccess(
            Runnable method,
            int threadCount,
            int iterationsPerThread) {
        
        List<Thread> threads = new ArrayList<>();
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());
        
        for (int i = 0; i < threadCount; i++) {
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        method.run();
                    }
                } catch (Throwable t) {
                    exceptions.add(t);
                }
            });
            threads.add(thread);
            thread.start();
        }
        
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted", e);
            }
        }
        
        assertThat(exceptions).isEmpty();
    }

    /**
     * Tests defensive copying by verifying modifications don't affect original.
     * 
     * @param <T> element type
     * @param originalList original list
     * @param copySupplier supplier that returns a copy
     * @param modifier modifier to apply to copy
     */
    public static <T> void testDefensiveCopy(
            List<T> originalList,
            Supplier<List<T>> copySupplier,
            Consumer<List<T>> modifier) {
        
        int originalSize = originalList.size();
        List<T> copy = copySupplier.get();
        
        modifier.accept(copy);
        
        assertThat(originalList).hasSize(originalSize);
    }
}
