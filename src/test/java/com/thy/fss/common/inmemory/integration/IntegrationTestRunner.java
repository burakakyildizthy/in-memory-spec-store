package com.thy.fss.common.inmemory.integration;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/**
 * Simple test runner for integration tests to avoid compilation issues with other tests.
 */
public class IntegrationTestRunner {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(IntegrationTestRunner.class);

    public static void main(String[] args) {
        runIntegrationTests();
    }

    public static boolean runIntegrationTests() {
        try {
            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(
                            DiscoverySelectors.selectClass(CrossComponentIntegrationTest.class),
                            DiscoverySelectors.selectClass(EndToEndWorkflowTest.class)
                    )
                    .build();

            Launcher launcher = LauncherFactory.create();
            SummaryGeneratingListener listener = new SummaryGeneratingListener();

            launcher.registerTestExecutionListeners(listener);
            launcher.execute(request);

            TestExecutionSummary summary = listener.getSummary();

            System.out.println("Integration Tests Summary:");
            System.out.println("Tests found: " + summary.getTestsFoundCount());
            System.out.println("Tests successful: " + summary.getTestsSucceededCount());
            System.out.println("Tests failed: " + summary.getTestsFailedCount());
            System.out.println("Tests skipped: " + summary.getTestsSkippedCount());

            if (summary.getTestsFailedCount() > 0) {
                System.out.println("\nFailures:");
                summary.getFailures().forEach(failure -> {
                    System.out.println("- " + failure.getTestIdentifier().getDisplayName());
                    System.out.println("  " + failure.getException().getMessage());
                });
            }

            return summary.getTestsFailedCount() == 0;

        } catch (Exception e) {
            log.error("Failed to run integration tests", e);

            return false;
        }
    }
}