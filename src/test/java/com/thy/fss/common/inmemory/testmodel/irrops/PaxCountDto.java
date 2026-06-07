package com.thy.fss.common.inmemory.testmodel.irrops;

import com.thy.fss.common.inmemory.processor.MetaModel;

@MetaModel
public class PaxCountDto {
    private Integer issued;
    private Integer refunded;

    public PaxCountDto() {
        this.issued = 0;
        this.refunded = 0;
    }

    public PaxCountDto(Integer issued, Integer refunded) {
        this.issued = issued;
        this.refunded = refunded;
    }

    public Integer getIssued() {
        return issued;
    }

    public void setIssued(Integer issued) {
        this.issued = issued;
    }

    public Integer getRefunded() {
        return refunded;
    }

    public void setRefunded(Integer refunded) {
        this.refunded = refunded;
    }
}
