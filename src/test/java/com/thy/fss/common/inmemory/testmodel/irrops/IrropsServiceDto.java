package com.thy.fss.common.inmemory.testmodel.irrops;

import com.thy.fss.common.inmemory.processor.MetaModel;

@MetaModel
public class IrropsServiceDto {
    private Double treat;
    private Double groundService;

    public IrropsServiceDto() {
        this.treat = 0.0;
        this.groundService = 0.0;
    }

    public IrropsServiceDto(Double treat, Double groundService) {
        this.treat = treat;
        this.groundService = groundService;
    }

    public Double getTreat() {
        return treat;
    }

    public void setTreat(Double treat) {
        this.treat = treat;
    }

    public Double getGroundService() {
        return groundService;
    }

    public void setGroundService(Double groundService) {
        this.groundService = groundService;
    }
}
