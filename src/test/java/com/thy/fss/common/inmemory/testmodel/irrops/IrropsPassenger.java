package com.thy.fss.common.inmemory.testmodel.irrops;

import com.thy.fss.common.inmemory.processor.MetaModel;

@MetaModel
public class IrropsPassenger {
    private Long id;
    private FlightDto flight;
    private TicketDto latestTicket;

    public IrropsPassenger() {
    }

    public IrropsPassenger(Long id, FlightDto flight, TicketDto latestTicket) {
        this.id = id;
        this.flight = flight;
        this.latestTicket = latestTicket;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FlightDto getFlight() {
        return flight;
    }

    public void setFlight(FlightDto flight) {
        this.flight = flight;
    }

    public TicketDto getLatestTicket() {
        return latestTicket;
    }

    public void setLatestTicket(TicketDto latestTicket) {
        this.latestTicket = latestTicket;
    }
}
