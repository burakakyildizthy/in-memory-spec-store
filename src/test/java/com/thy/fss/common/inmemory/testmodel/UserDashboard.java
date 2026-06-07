package com.thy.fss.common.inmemory.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

@MetaModel
public class UserDashboard {
    private Long totalUsers;

    public Long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }
}
