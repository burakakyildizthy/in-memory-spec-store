package com.thy.fss.common.inmemory.testmodel.irrops;

import com.thy.fss.common.inmemory.processor.MetaModel;

@MetaModel
public class TicketDto {
    private Boolean issued;
    private Boolean refunded;

    public TicketDto() {
    }

    public TicketDto(Boolean issued, Boolean refunded) {
        this.issued = issued;
        this.refunded = refunded;
    }

    public Boolean getIssued() {
        return issued;
    }

    public void setIssued(Boolean issued) {
        this.issued = issued;
    }

    public Boolean getRefunded() {
        return refunded;
    }

    public void setRefunded(Boolean refunded) {
        this.refunded = refunded;
    }
}
