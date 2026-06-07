package com.thy.fss.common.inmemory.engine.mapping;

import com.thy.fss.common.inmemory.testmodel.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test verifying that {@link MappingApplicator#extractSourceValue(Object, PropertyMapping)}
 * returns the entity itself when called with a mapping that has {@code sourcePath == null}.
 */
@DisplayName("MappingApplicator.extractSourceValue() with null sourcePath")
class MappingApplicatorExtractSourceValueTest {

    @Test
    @DisplayName("extractSourceValue should return the entity itself when sourcePath is null")
    void extractSourceValueShouldReturnEntityItselfWhenSourcePathIsNull() {
        // Arrange: create a ONE_TO_ONE mapping with null sourcePath (direct model reference)
        PropertyMapping<User, Order> mapping = PropertyMapping.<User, Order>builder()
                .consumerId("test-store")
                .isForDashboard(false)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName("orderDataSource")
                // sourcePath intentionally NOT set (null) — direct model reference
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(User_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.status)))
                .mappingType(MappingType.ONE_TO_ONE)
                .build();

        // Verify precondition: sourcePath is indeed null
        assertThat(mapping.getSourcePath()).isNull();

        // Create a source entity
        Order sourceEntity = new Order();
        sourceEntity.setId(42L);
        sourceEntity.setStatus("ACTIVE");

        // Act: call extractSourceValue with the null-sourcePath mapping
        Object result = MappingApplicator.extractSourceValue(sourceEntity, mapping);

        // Assert: the returned value should be the exact same entity object (identity check)
        assertThat(result)
                .as("extractSourceValue should return the entity itself when sourcePath is null")
                .isSameAs(sourceEntity);
    }

    @Test
    @DisplayName("extractSourceValue should return null when source entity is null")
    void extractSourceValueShouldReturnNullWhenSourceEntityIsNull() {
        // Arrange: create a ONE_TO_ONE mapping with null sourcePath
        PropertyMapping<User, Order> mapping = PropertyMapping.<User, Order>builder()
                .consumerId("test-store")
                .isForDashboard(false)
                .sourceService(OrderSpecificationService.INSTANCE)
                .targetService(UserSpecificationService.INSTANCE)
                .targetPath(Collections.singletonList(User_.name))
                .datasourceName("orderDataSource")
                .primaryKeyPaths(Collections.singletonList(Collections.singletonList(User_.id)))
                .foreignKeyPaths(Collections.singletonList(Collections.singletonList(Order_.status)))
                .mappingType(MappingType.ONE_TO_ONE)
                .build();

        // Act
        Object result = MappingApplicator.extractSourceValue(null, mapping);

        // Assert
        assertThat(result)
                .as("extractSourceValue should return null when source entity is null")
                .isNull();
    }
}
