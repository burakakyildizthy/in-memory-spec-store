package com.thy.fss.common.inmemory.processor.generator.importbug.fixtures;

import java.util.List;

import com.thy.fss.common.inmemory.processor.MetaModel;
import com.thy.fss.common.inmemory.processor.generator.importbug.otherpkg.CustomerId;

/**
 * A @MetaModel class that extends BaseModel.
 * Tests that inherited fields with nested generics have their imports correctly resolved.
 * The annotation processor must collect imports for types referenced by the parent's fields.
 *
 * Map-typed fields are intentionally not exercised here: Map fields are not a
 * supported/covered type shape, so they are excluded from this fixture.
 */
@MetaModel
public class InheritanceModel extends BaseModel {

    private String ownField;

    // Own field that also uses a cross-package type
    private List<CustomerId> customerIds;

    public InheritanceModel() {}

    public String getOwnField() { return ownField; }
    public void setOwnField(String ownField) { this.ownField = ownField; }

    public List<CustomerId> getCustomerIds() { return customerIds; }
    public void setCustomerIds(List<CustomerId> customerIds) { this.customerIds = customerIds; }
}
