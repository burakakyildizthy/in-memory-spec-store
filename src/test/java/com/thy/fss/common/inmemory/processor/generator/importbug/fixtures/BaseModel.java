package com.thy.fss.common.inmemory.processor.generator.importbug.fixtures;

import java.util.List;

import com.thy.fss.common.inmemory.processor.MetaModel;
import com.thy.fss.common.inmemory.processor.generator.importbug.otherpkg.Order;

/**
 * Base @MetaModel class that holds nested generic fields with cross-package types.
 * Used to test that inherited fields' imports are correctly collected.
 *
 * Map-typed fields are intentionally not exercised here: Map fields are not a
 * supported/covered type shape, so they are excluded from this fixture.
 */
@MetaModel
public class BaseModel {

    private String baseName;

    // Inherited field with bug-triggering shape: nested generic
    private List<Order> baseOrders;

    public BaseModel() {}

    public String getBaseName() { return baseName; }
    public void setBaseName(String baseName) { this.baseName = baseName; }

    public List<Order> getBaseOrders() { return baseOrders; }
    public void setBaseOrders(List<Order> baseOrders) { this.baseOrders = baseOrders; }
}
