package com.example.acme.codegen.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Asks AcmeCodegen to generate {@code <Type>Description} beside the annotated type. The
 * generated code is compiled into the user's own binary, which is why a user of AcmeCodegen
 * needs a rebuild, not just a new dependency spec, when AcmeCodegen changes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Describe {

    String value();
}
