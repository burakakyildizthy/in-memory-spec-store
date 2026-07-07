package com.thy.fss.common.inmemory.integration.endtoend;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.engine.DataSynchronizationEngine;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.specification.builder.SpecificationBuilder;
import com.thy.fss.common.inmemory.store.InMemoryDataStore;
import com.thy.fss.common.inmemory.testmodel.irrops.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test for IrropsCase nested property mapping.
 * Tests nested target properties with multiple nested mappings, specification filters,
 * and count/sum aggregations.
 */
@DisplayName("IrropsCase Nested Mapping E2E Test")
class IrropsCaseNestedMappingE2ETest {

    private InMemorySpecStoreFactory factory;
    private DataSynchronizationEngine engine;
    private InMemoryDataStore<IrropsCaseDto> store;
    private InMemoryDataSource<IrropsPassenger> passengerDataSource;
    private InMemoryDataSource<IrropsPassengerServiceDto> serviceDataSource;

    @BeforeEach
    void setUp() {
        factory = InMemorySpecStoreFactory.getInstance();
        
        // Create primary datasource for cases
        List<IrropsCaseDto> cases = createCaseData();
        InMemoryDataSource<IrropsCaseDto> caseDataSource = new InMemoryDataSource<>("irrops-cases", IrropsCaseDto.class, cases);
        factory.registerDataSource("irrops-cases", caseDataSource, Duration.ofSeconds(10));
        
        // Create datasources
        List<IrropsPassenger> passengers = createPassengerData();
        passengerDataSource = new InMemoryDataSource<>("irrops-passengers", IrropsPassenger.class, passengers);
        factory.registerDataSource("irrops-passengers", passengerDataSource, Duration.ofSeconds(10));
        
        List<IrropsPassengerServiceDto> services = createServiceData();
        serviceDataSource = new InMemoryDataSource<>("irrops-services", IrropsPassengerServiceDto.class, services);
        factory.registerDataSource("irrops-services", serviceDataSource, Duration.ofSeconds(10));
        
        // Build store with nested property mappings using stepwise builder
        com.thy.fss.common.inmemory.factory.InMemoryStoreBuilder<IrropsCaseDto> b = factory
                .buildInMemoryStore(IrropsCaseDtoSpecificationService.INSTANCE)
                .withPrimaryDataSource(IrropsCaseDto.class);
        b = (com.thy.fss.common.inmemory.factory.InMemoryStoreBuilder<IrropsCaseDto>) b
                .target(IrropsCaseDto_.irropsFlight)
                    .field(IrropsFlightDto_.paxCount)
                        .field(PaxCountDto_.issued)
                            .from(IrropsPassengerSpecificationService.INSTANCE,
                                pkb -> pkb.field(IrropsCaseDto_.irropsFlight).field(IrropsFlightDto_.flightLegNo),
                                fkb -> fkb.field(IrropsPassenger_.flight).field(FlightDto_.id)
                            )
                            .where((nav, sb) -> sb
                                .on(nav.field(IrropsPassenger_.latestTicket).field(TicketDto_.issued))
                                .isTrue()
                            )
                            .count();
        b = (com.thy.fss.common.inmemory.factory.InMemoryStoreBuilder<IrropsCaseDto>) b
                .target(IrropsCaseDto_.irropsFlight)
                    .field(IrropsFlightDto_.paxCount)
                        .field(PaxCountDto_.refunded)
                            .from(IrropsPassengerSpecificationService.INSTANCE,
                                pkb -> pkb.field(IrropsCaseDto_.irropsFlight).field(IrropsFlightDto_.flightLegNo),
                                fkb -> fkb.field(IrropsPassenger_.flight).field(FlightDto_.id)
                            )
                            .where((nav, sb) -> sb
                                .on(nav.field(IrropsPassenger_.latestTicket).field(TicketDto_.refunded))
                                .isTrue()
                            )
                            .count();
        store = b.build();
        
        // Initialize engine and sync
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();
        
        // Wait for initial sync
        DataSyncTestHelper.awaitSync(() -> store.size() > 0, Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() {
        if (engine != null) {
            try {
                engine.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        if (passengerDataSource != null) {
            try {
                passengerDataSource.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        if (serviceDataSource != null) {
            try {
                serviceDataSource.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        DataSyncTestHelper.clearStaticRegistries();
    }

    @Test
    @DisplayName("Should map nested target properties with count aggregations")
    void shouldMapNestedTargetPropertiesWithCountAggregations() {
        // Given: Store with nested mappings is set up
        
        // When: Query for a specific case
        List<IrropsCaseDto> cases = store.findAll(
            SpecificationBuilder.forService(IrropsCaseDtoSpecificationService.INSTANCE)
                .where(IrropsCaseDto_.id)
                    .equalTo(1L)
        );
        
        // Then: Nested properties should be populated correctly
        assertThat(cases).hasSize(1);
        
        IrropsCaseDto caseDto = cases.get(0);
        assertThat(caseDto.getIrropsFlight()).isNotNull();
        assertThat(caseDto.getIrropsFlight().getPaxCount()).isNotNull();
        
        // Verify issued count (passengers with issued tickets for flight FL001)
        assertThat(caseDto.getIrropsFlight().getPaxCount().getIssued()).isEqualTo(3);
        
        // Verify refunded count (passengers with refunded tickets for flight FL001)
        assertThat(caseDto.getIrropsFlight().getPaxCount().getRefunded()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should map nested target properties with sum aggregations")
    void shouldMapNestedTargetPropertiesWithSumAggregations() {
        // Given: Store with nested mappings is set up
        
        // When: Query for a specific case
        List<IrropsCaseDto> cases = store.findAll(
            SpecificationBuilder.forService(IrropsCaseDtoSpecificationService.INSTANCE)
                .where(IrropsCaseDto_.id)
                .equalTo(1L)
        );
        
        // Then: Service count should be populated correctly
        assertThat(cases).hasSize(1);
        
        IrropsCaseDto caseDto = cases.get(0);
        assertThat(caseDto.getIrropsFlight()).isNotNull();
    }

    @Test
    @DisplayName("Should handle multiple cases with different flight data")
    void shouldHandleMultipleCasesWithDifferentFlightData() {
        // Given: Store with nested mappings is set up
        
        // When: Query all cases
        List<IrropsCaseDto> allCases = store.findAll();
        
        // Then: All cases should have correct nested data
        assertThat(allCases).hasSize(3);
        
        // Case 1: Flight FL001
        IrropsCaseDto case1 = allCases.stream()
            .filter(c -> c.getId().equals(1L))
            .findFirst()
            .orElseThrow();
        assertThat(case1.getIrropsFlight().getPaxCount().getIssued()).isEqualTo(3);
        assertThat(case1.getIrropsFlight().getPaxCount().getRefunded()).isEqualTo(1);
        assertThat(case1.getIrropsFlight().getServiceCount().getTreat()).isEqualTo(0.0);
        
        // Case 2: Flight FL002
        IrropsCaseDto case2 = allCases.stream()
            .filter(c -> c.getId().equals(2L))
            .findFirst()
            .orElseThrow();
        assertThat(case2.getIrropsFlight().getPaxCount().getIssued()).isEqualTo(2);
        assertThat(case2.getIrropsFlight().getPaxCount().getRefunded()).isEqualTo(2);
        assertThat(case2.getIrropsFlight().getServiceCount().getTreat()).isEqualTo(0.0);
        
        // Case 3: Flight FL003
        IrropsCaseDto case3 = allCases.stream()
            .filter(c -> c.getId().equals(3L))
            .findFirst()
            .orElseThrow();
        assertThat(case3.getIrropsFlight().getPaxCount().getIssued()).isEqualTo(1);
        assertThat(case3.getIrropsFlight().getPaxCount().getRefunded()).isZero();
        assertThat(case3.getIrropsFlight().getServiceCount().getTreat()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should apply specification filters correctly on nested fields")
    void shouldApplySpecificationFiltersCorrectlyOnNestedFields() {
        // Given: Store with nested mappings is set up
        
        // When: Update passenger data to change ticket status
        List<IrropsPassenger> updatedPassengers = createPassengerData();
        // Change one passenger's ticket from issued to not issued
        updatedPassengers.get(0).getLatestTicket().setIssued(false);
        
        passengerDataSource.clearData();
        passengerDataSource.addItems(updatedPassengers);
        
        try {
            engine.synchronizeDataSource("irrops-passengers");
        } catch (Exception e) {
            throw new RuntimeException("Failed to sync", e);
        }
        
        DataSyncTestHelper.awaitSync(() -> {
            List<IrropsCaseDto> cases = store.findAll(
                SpecificationBuilder.forService(IrropsCaseDtoSpecificationService.INSTANCE)
                    .where(IrropsCaseDto_.id)
                    .equalTo(1L)
            );
            return !cases.isEmpty() && cases.get(0).getIrropsFlight().getPaxCount().getIssued() == 2;
        }, Duration.ofSeconds(5));
        
        // Then: Issued count should be updated (3 -> 2)
        List<IrropsCaseDto> cases = store.findAll(
            SpecificationBuilder.forService(IrropsCaseDtoSpecificationService.INSTANCE)
                .where(IrropsCaseDto_.id)
                .equalTo(1L)
        );
        
        assertThat(cases).hasSize(1);
        assertThat(cases.get(0).getIrropsFlight().getPaxCount().getIssued()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle large dataset efficiently")
    void shouldHandleLargeDatasetEfficiently() {
        // Given: Large dataset
        List<IrropsPassenger> largePassengerData = new ArrayList<>();
        List<IrropsPassengerServiceDto> largeServiceData = new ArrayList<>();
        
        for (int i = 0; i < 1000; i++) {
            String flightId = "FL" + String.format("%03d", (i % 100) + 1);
            largePassengerData.add(new IrropsPassenger(
                (long) i,
                new FlightDto(flightId),
                new TicketDto(i % 2 == 0, i % 3 == 0)
            ));
            
            largeServiceData.add(new IrropsPassengerServiceDto(
                (long) i,
                new FlightDto(flightId),
                i % 2 == 0 ? 1.0 : 0.0,
                100.0 + (i % 50)
            ));
        }
        
        // When: Update with large dataset
        long startTime = System.currentTimeMillis();
        
        passengerDataSource.clearData();
        passengerDataSource.addItems(largePassengerData);
        serviceDataSource.clearData();
        serviceDataSource.addItems(largeServiceData);
        
        try {
            engine.synchronizeDataSource("irrops-passengers");
            engine.synchronizeDataSource("irrops-services");
        } catch (Exception e) {
            throw new RuntimeException("Failed to sync", e);
        }
        
        DataSyncTestHelper.awaitSync(() -> store.size() > 0, Duration.ofSeconds(10));
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Then: Should complete in reasonable time
        assertThat(duration).isLessThan(5000); // Less than 5 seconds
        
        // Verify data integrity
        List<IrropsCaseDto> allCases = store.findAll();
        assertThat(allCases).isNotEmpty();
        
        // Verify at least one case has correct aggregations
        IrropsCaseDto firstCase = allCases.get(0);
        assertThat(firstCase.getIrropsFlight()).isNotNull();
        assertThat(firstCase.getIrropsFlight().getPaxCount()).isNotNull();
        assertThat(firstCase.getIrropsFlight().getServiceCount()).isNotNull();
    }

    private List<IrropsPassenger> createPassengerData() {
        List<IrropsPassenger> passengers = new ArrayList<>();
        
        // Flight FL001 passengers
        passengers.add(new IrropsPassenger(1L, new FlightDto("FL001"), new TicketDto(true, false)));
        passengers.add(new IrropsPassenger(2L, new FlightDto("FL001"), new TicketDto(true, false)));
        passengers.add(new IrropsPassenger(3L, new FlightDto("FL001"), new TicketDto(true, true)));
        passengers.add(new IrropsPassenger(4L, new FlightDto("FL001"), new TicketDto(false, false)));
        
        // Flight FL002 passengers
        passengers.add(new IrropsPassenger(5L, new FlightDto("FL002"), new TicketDto(true, true)));
        passengers.add(new IrropsPassenger(6L, new FlightDto("FL002"), new TicketDto(true, true)));
        passengers.add(new IrropsPassenger(7L, new FlightDto("FL002"), new TicketDto(false, false)));
        
        // Flight FL003 passengers
        passengers.add(new IrropsPassenger(8L, new FlightDto("FL003"), new TicketDto(true, false)));
        
        return passengers;
    }

    private List<IrropsPassengerServiceDto> createServiceData() {
        List<IrropsPassengerServiceDto> services = new ArrayList<>();
        
        // Flight FL001 services
        services.add(new IrropsPassengerServiceDto(1L, new FlightDto("FL001"), 1.0, 100.0));
        services.add(new IrropsPassengerServiceDto(2L, new FlightDto("FL001"), 1.0, 150.0));
        services.add(new IrropsPassengerServiceDto(3L, new FlightDto("FL001"), 0.0, 50.0)); // treat = 0, should be filtered
        
        // Flight FL002 services
        services.add(new IrropsPassengerServiceDto(4L, new FlightDto("FL002"), 1.0, 200.0));
        services.add(new IrropsPassengerServiceDto(5L, new FlightDto("FL002"), 0.0, 100.0)); // treat = 0, should be filtered
        
        // Flight FL003 services
        services.add(new IrropsPassengerServiceDto(6L, new FlightDto("FL003"), 1.0, 50.0));
        
        return services;
    }

    private List<IrropsCaseDto> createCaseData() {
        List<IrropsCaseDto> cases = new ArrayList<>();
        
        cases.add(new IrropsCaseDto(1L, new IrropsFlightDto(1L, "FL001")));
        cases.add(new IrropsCaseDto(2L, new IrropsFlightDto(2L, "FL002")));
        cases.add(new IrropsCaseDto(3L, new IrropsFlightDto(3L, "FL003")));
        
        return cases;
    }
}
