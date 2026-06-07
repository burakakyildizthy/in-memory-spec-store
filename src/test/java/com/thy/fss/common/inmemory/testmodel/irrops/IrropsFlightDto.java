package com.thy.fss.common.inmemory.testmodel.irrops;

import com.thy.fss.common.inmemory.processor.MetaModel;

@MetaModel
public class IrropsFlightDto {
    private Long id;
    private String flightLegNo;
    private PaxCountDto paxCount;
    private IrropsServiceDto serviceCount;

    public IrropsFlightDto() {
    }

    public IrropsFlightDto(Long id, String flightLegNo) {
        this.id = id;
        this.flightLegNo = flightLegNo;
        this.paxCount = new PaxCountDto();
        this.serviceCount = new IrropsServiceDto();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFlightLegNo() {
        return flightLegNo;
    }

    public void setFlightLegNo(String flightLegNo) {
        this.flightLegNo = flightLegNo;
    }

    public PaxCountDto getPaxCount() {
        return paxCount;
    }

    public void setPaxCount(PaxCountDto paxCount) {
        this.paxCount = paxCount;
    }

    public IrropsServiceDto getServiceCount() {
        return serviceCount;
    }

    public void setServiceCount(IrropsServiceDto serviceCount) {
        this.serviceCount = serviceCount;
    }
}
