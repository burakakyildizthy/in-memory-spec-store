package com.thy.fss.common.inmemory.processor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark classes for meta model generation.
 * When a class is annotated with @MetaModel, the annotation processor will generate:
 * 1. StaticMetaModel (ClassName_) - Type-safe field references
 * 2. FilterMetaModel (ClassNameFilter) -  filter definitions
 * 3. StaticSpecificationService (ClassNameSpecificationService) - Validation methods
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface MetaModel {
}