package com.thy.fss.common.inmemory.testmodel.irrops;

import com.thy.fss.common.inmemory.processor.MetaModel;

@MetaModel
public class IrropsPassengerServiceDto {
    private Long id;
    private FlightDto flight;
    private Double treat;
    private Double amount;

    public IrropsPassengerServiceDto() {
    }

    public IrropsPassengerServiceDto(Long id, FlightDto flight, Double treat, Double amount) {
        this.id = id;
        this.flight = flight;
        this.treat = treat;
        this.amount = amount;
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

    public Double getTreat() {
        return treat;
    }

    public void setTreat(Double treat) {
        this.treat = treat;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }
}
