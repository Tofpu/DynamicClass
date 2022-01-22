package io.tofpu.dynamicclass.meta;

import io.tofpu.dynamicclass.DynamicClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Classes that are annotated with {@link AutoRegister} annotation will have an
 * instance of them created when the
 * {@link DynamicClass#scan(String)} is run at the best case
 * scenario. <br> <br>
 * <p>
 * The classes could also have a constructor within it, so long the
 * constructor's parameter(s) were either, annotated with {@link AutoRegister},
 * beforehand or have been manually registered with the
 * {@link DynamicClass#addParameters(Object...)} method before
 * {@link DynamicClass#scan(String)} were initially called.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoRegister {}
