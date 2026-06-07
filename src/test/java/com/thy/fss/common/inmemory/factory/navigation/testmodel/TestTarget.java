package com.thy.fss.common.inmemory.factory.navigation.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Test entity for KeyPairBuilder property tests - target side
 */
@MetaModel
public class TestTarget {
    private Long id;
    private String code;
    private Integer version;
    private String region;
    private Long timestamp;
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
