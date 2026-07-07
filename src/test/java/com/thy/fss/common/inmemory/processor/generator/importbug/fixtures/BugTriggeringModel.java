package com.thy.fss.common.inmemory.processor.generator.importbug.fixtures;

import java.util.List;
import java.util.Set;

import com.thy.fss.common.inmemory.processor.MetaModel;
import com.thy.fss.common.inmemory.processor.generator.importbug.otherpkg.Order;
import com.thy.fss.common.inmemory.processor.generator.importbug.otherpkg.Status;

/**
 * A @MetaModel fixture that exercises bug-triggering field shapes.
 * The annotation processor runs on this class during test compilation.
 * If this class compiles (generated sources included), the import bug is fixed.
 *
 * Map-typed fields are intentionally not exercised here: Map fields are not a
 * supported/covered type shape, so they are excluded from this fixture.
 *
 * Field shapes exercised:
 * - List<List<Order>> (doubly-nested collection)
 * - Set<Order> (single-argument collection - preservation)
 * - Order (direct reference - preservation)
 * - Status (direct enum reference - preservation)
 */
@MetaModel
public class BugTriggeringModel {

    private String name;

    // Bug Case 4: Doubly-nested collection
    private List<List<Order>> nestedOrders;

    // Preservation: Single-argument collection
    private Set<Order> orderSet;

    // Preservation: Direct reference
    private Order singleOrder;

    // Preservation: Direct enum reference
    private Status status;

    public BugTriggeringModel() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<List<Order>> getNestedOrders() { return nestedOrders; }
    public void setNestedOrders(List<List<Order>> nestedOrders) { this.nestedOrders = nestedOrders; }

    public Set<Order> getOrderSet() { return orderSet; }
    public void setOrderSet(Set<Order> orderSet) { this.orderSet = orderSet; }

    public Order getSingleOrder() { return singleOrder; }
    public void setSingleOrder(Order singleOrder) { this.singleOrder = singleOrder; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
