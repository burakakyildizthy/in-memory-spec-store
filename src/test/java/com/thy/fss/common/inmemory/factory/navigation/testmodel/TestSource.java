package com.thy.fss.common.inmemory.factory.navigation.testmodel;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * Test entity for KeyPairBuilder property tests - source side
 */
@MetaModel
public class TestSource {
    private Long targetId;
    private String targetCode;
    private Integer targetVersion;
    private String targetRegion;
    private Long targetTimestamp;
    
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getTargetCode() { return targetCode; }
    public void setTargetCode(String targetCode) { this.targetCode = targetCode; }
    public Integer getTargetVersion() { return targetVersion; }
    public void setTargetVersion(Integer targetVersion) { this.targetVersion = targetVersion; }
    public String getTargetRegion() { return targetRegion; }
    public void setTargetRegion(String targetRegion) { this.targetRegion = targetRegion; }
    public Long getTargetTimestamp() { return targetTimestamp; }
    public void setTargetTimestamp(Long targetTimestamp) { this.targetTimestamp = targetTimestamp; }
}
