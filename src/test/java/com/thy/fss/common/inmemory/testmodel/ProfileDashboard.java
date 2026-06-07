package com.thy.fss.common.inmemory.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

@MetaModel
public class ProfileDashboard {
    private Long totalProfiles;

    public Long getTotalProfiles() {
        return totalProfiles;
    }

    public void setTotalProfiles(Long totalProfiles) {
        this.totalProfiles = totalProfiles;
    }
}
