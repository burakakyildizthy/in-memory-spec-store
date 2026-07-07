package com.thy.fss.common.inmemory.testmodel.irrops;

import com.thy.fss.common.inmemory.processor.MetaModel;

@MetaModel
public class IrropsCaseDto {
    private Long id;
    private IrropsFlightDto irropsFlight;

    public IrropsCaseDto() {
    }

    public IrropsCaseDto(Long id, IrropsFlightDto irropsFlight) {
        this.id = id;
        this.irropsFlight = irropsFlight;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public IrropsFlightDto getIrropsFlight() {
        return irropsFlight;
    }

    public void setIrropsFlight(IrropsFlightDto irropsFlight) {
        this.irropsFlight = irropsFlight;
    }
}
