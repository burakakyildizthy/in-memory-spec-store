package com.thy.fss.common.inmemory.filter.parameter;

import com.thy.fss.common.inmemory.testmodel.CollectionTestEntity;
import com.thy.fss.common.inmemory.testmodel.CollectionTestEntityFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Test controller for end-to-end testing of collection filter web binding.
 * This controller accepts CollectionTestEntityFilter as a parameter to verify that
 * collection operators work correctly through HTTP query parameters.
 */
@RestController
@RequestMapping("/api/test/collection-filters")
public class CollectionFilterTestController {
    
    /**
     * Endpoint that accepts a filter with collection fields.
     * Used to test collection filter parameter binding from query strings.
     * 
     * @param filter The filter bound from query parameters
     * @return The filter object for verification
     */
    @GetMapping("/search")
    public CollectionTestEntityFilter searchWithFilter(CollectionTestEntityFilter filter) {
        // In a real application, this would query data using the filter
        // For testing, we just return the filter to verify binding worked
        return filter;
    }
    
    /**
     * Endpoint that returns test entities.
     * Used to verify the complete flow of filtering with collection operators.
     * 
     * @param filter The filter bound from query parameters
     * @return List of entities (mock data for testing)
     */
    @GetMapping("/entities")
    public List<CollectionTestEntity> getEntities(CollectionTestEntityFilter filter) {
        // In a real application, this would apply the filter to a data source
        // For testing, we return empty list and verify filter binding separately
        return List.of();
    }
}
