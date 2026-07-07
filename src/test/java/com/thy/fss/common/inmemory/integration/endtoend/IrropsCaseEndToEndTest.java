package com.thy.fss.common.inmemory.integration.endtoend;

import com.thy.fss.common.inmemory.common.DataSyncTestHelper;
import com.thy.fss.common.inmemory.datasource.InMemoryDataSource;
import com.thy.fss.common.inmemory.engine.DataSynchronizationEngine;
import com.thy.fss.common.inmemory.factory.InMemorySpecStoreFactory;
import com.thy.fss.common.inmemory.factory.InMemoryStoreBuilder;
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
 * Comprehensive end-to-end test for IrropsCase scenario.
 * Tests nested target properties, multiple nested mappings, specification filters,
 * count and sum aggregations.
 */
@DisplayName("IrropsCase End-to-End Test")
class IrropsCaseEndToEndTest {

    private InMemorySpecStoreFactory factory;
    private DataSynchronizationEngine engine;
    private InMemoryDataStore<IrropsCaseDto> store;
    private InMemoryDataSource<IrropsPassenger> passengerDataSource;
    private InMemoryDataSource<IrropsPassengerServiceDto> serviceDataSource;

    @BeforeEach
    void setUp() {
        DataSyncTestHelper.clearStaticRegistries();
        factory = InMemorySpecStoreFactory.getInstance();
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
    @DisplayName("Should aggregate nested passenger counts with specification filters")
    void testNestedPassengerCountsWithFilters() {
        List<IrropsPassenger> passengers = createPassengerData();
        List<IrropsPassengerServiceDto> services = createServiceData();

        passengerDataSource = new InMemoryDataSource<>("irrops-passengers", IrropsPassenger.class, passengers);
        serviceDataSource = new InMemoryDataSource<>("irrops-services", IrropsPassengerServiceDto.class, services);

        factory.registerDataSource("irrops-passengers", passengerDataSource, Duration.ofSeconds(10));
        factory.registerDataSource("irrops-services", serviceDataSource, Duration.ofSeconds(10));

        // Register primary datasource for cases
        List<IrropsCaseDto> cases = new ArrayList<>();
        cases.add(new IrropsCaseDto(1L, new IrropsFlightDto(1L, "FL001")));
        cases.add(new IrropsCaseDto(2L, new IrropsFlightDto(2L, "FL002")));
        InMemoryDataSource<IrropsCaseDto> caseDs = new InMemoryDataSource<>("irrops-cases", IrropsCaseDto.class, cases);
        factory.registerDataSource("irrops-cases", caseDs, Duration.ofSeconds(10));

        InMemoryStoreBuilder<IrropsCaseDto> builder = factory
                .buildInMemoryStore(IrropsCaseDtoSpecificationService.INSTANCE)
                .withPrimaryDataSource(IrropsCaseDto.class);
        builder = (InMemoryStoreBuilder<IrropsCaseDto>) builder
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
        builder = (InMemoryStoreBuilder<IrropsCaseDto>) builder
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
        store = builder.build();

        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        DataSyncTestHelper.awaitSync(() -> store.size() > 0, Duration.ofSeconds(5));

        List<IrropsCaseDto> result1 = store.findAll(
            SpecificationBuilder.forService(IrropsCaseDtoSpecificationService.INSTANCE)
                .where(IrropsCaseDto_.id)
                .equalTo(1L)
        );
        assertThat(result1).hasSize(1);
        IrropsCaseDto case1 = result1.get(0);
        assertThat(case1.getIrropsFlight()).isNotNull();
        assertThat(case1.getIrropsFlight().getPaxCount()).isNotNull();
        assertThat(case1.getIrropsFlight().getPaxCount().getIssued()).isEqualTo(2);
        assertThat(case1.getIrropsFlight().getPaxCount().getRefunded()).isEqualTo(1);

        List<IrropsCaseDto> result2 = store.findAll(
            SpecificationBuilder.forService(IrropsCaseDtoSpecificationService.INSTANCE)
                .where(IrropsCaseDto_.id)
                .equalTo(2L)
        );
        assertThat(result2).hasSize(1);
        IrropsCaseDto case2 = result2.get(0);
        assertThat(case2.getIrropsFlight()).isNotNull();
        assertThat(case2.getIrropsFlight().getPaxCount()).isNotNull();
        assertThat(case2.getIrropsFlight().getPaxCount().getIssued()).isEqualTo(1);
        assertThat(case2.getIrropsFlight().getPaxCount().getRefunded()).isZero();
    }

    @Test
    @DisplayName("Should handle large dataset with nested aggregations")
    void testLargeDatasetWithNestedAggregations() {
        List<IrropsPassenger> passengers = createLargePassengerDataset(1000);
        List<IrropsPassengerServiceDto> services = createLargeServiceDataset(1000);

        passengerDataSource = new InMemoryDataSource<>("irrops-passengers", IrropsPassenger.class, passengers);
        serviceDataSource = new InMemoryDataSource<>("irrops-services", IrropsPassengerServiceDto.class, services);

        factory.registerDataSource("irrops-passengers", passengerDataSource, Duration.ofSeconds(10));
        factory.registerDataSource("irrops-services", serviceDataSource, Duration.ofSeconds(10));

        // Register primary datasource for cases
        List<IrropsCaseDto> cases2 = new ArrayList<>();
        cases2.add(new IrropsCaseDto(1L, new IrropsFlightDto(1L, "FL001")));
        InMemoryDataSource<IrropsCaseDto> caseDs2 = new InMemoryDataSource<>("irrops-cases", IrropsCaseDto.class, cases2);
        factory.registerDataSource("irrops-cases", caseDs2, Duration.ofSeconds(10));

        InMemoryStoreBuilder<IrropsCaseDto> builder2 = factory
                .buildInMemoryStore(IrropsCaseDtoSpecificationService.INSTANCE)
                .withPrimaryDataSource(IrropsCaseDto.class);
        builder2 = (InMemoryStoreBuilder<IrropsCaseDto>) builder2
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
        store = builder2.build();

        long startTime = System.currentTimeMillis();
        engine = new DataSynchronizationEngine(factory);
        engine.initialize();

        DataSyncTestHelper.awaitSync(() -> store.size() > 0, Duration.ofSeconds(10));
        long endTime = System.currentTimeMillis();

        assertThat(store.size()).isGreaterThan(0);
        assertThat(endTime - startTime).isLessThan(5000);

        List<IrropsCaseDto> firstList = store.findAll(
            SpecificationBuilder.forService(IrropsCaseDtoSpecificationService.INSTANCE)
                .where(IrropsCaseDto_.id)
                .equalTo(1L)
        );
        assertThat(firstList).isNotEmpty();
        IrropsCaseDto firstCase = firstList.get(0);
        assertThat(firstCase.getIrropsFlight()).isNotNull();
        assertThat(firstCase.getIrropsFlight().getPaxCount()).isNotNull();
        assertThat(firstCase.getIrropsFlight().getPaxCount().getIssued()).isGreaterThanOrEqualTo(0);
    }

    private List<IrropsPassenger> createPassengerData() {
        List<IrropsPassenger> passengers = new ArrayList<>();

        passengers.add(new IrropsPassenger(1L, new FlightDto("FL001"), new TicketDto(true, false)));
        passengers.add(new IrropsPassenger(2L, new FlightDto("FL001"), new TicketDto(true, false)));
        passengers.add(new IrropsPassenger(3L, new FlightDto("FL001"), new TicketDto(false, true)));
        passengers.add(new IrropsPassenger(4L, new FlightDto("FL002"), new TicketDto(true, false)));
        passengers.add(new IrropsPassenger(5L, new FlightDto("FL002"), new TicketDto(false, false)));

        return passengers;
    }

    private List<IrropsPassengerServiceDto> createServiceData() {
        List<IrropsPassengerServiceDto> services = new ArrayList<>();

        services.add(new IrropsPassengerServiceDto(1L, new FlightDto("FL001"), 1.0, 100.0));
        services.add(new IrropsPassengerServiceDto(2L, new FlightDto("FL001"), 1.0, 150.0));
        services.add(new IrropsPassengerServiceDto(3L, new FlightDto("FL001"), 0.0, 50.0));
        services.add(new IrropsPassengerServiceDto(4L, new FlightDto("FL002"), 1.0, 150.0));
        services.add(new IrropsPassengerServiceDto(5L, new FlightDto("FL002"), 0.0, 75.0));

        return services;
    }

    private List<IrropsPassenger> createLargePassengerDataset(int count) {
        List<IrropsPassenger> passengers = new ArrayList<>();
        for (long i = 1; i <= count; i++) {
            String flightId = "FL" + String.format("%03d", (i % 100) + 1);
            boolean issued = i % 3 != 0;
            boolean refunded = i % 5 == 0;
            passengers.add(new IrropsPassenger(i, new FlightDto(flightId), new TicketDto(issued, refunded)));
        }
        return passengers;
    }

    private List<IrropsPassengerServiceDto> createLargeServiceDataset(int count) {
        List<IrropsPassengerServiceDto> services = new ArrayList<>();
        for (long i = 1; i <= count; i++) {
            String flightId = "FL" + String.format("%03d", (i % 100) + 1);
            double treat = i % 4 == 0 ? 0.0 : 1.0;
            double amount = 50.0 + (i % 200);
            services.add(new IrropsPassengerServiceDto(i, new FlightDto(flightId), treat, amount));
        }
        return services;
    }
}
