package com.thy.fss.common.inmemory.processor.generator.importbug.otherpkg;

import com.thy.fss.common.inmemory.processor.MetaModel;

/**
 * A @MetaModel class in a different package, used as a Map key type
 * to test multi-argument generic import resolution.
 */
@MetaModel
public class CustomerId {
    private String value;

    public CustomerId() {}

    public CustomerId(String value) {
        this.value = value;
    }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
