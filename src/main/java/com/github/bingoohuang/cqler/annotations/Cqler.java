package com.github.bingoohuang.cqler.annotations;

import java.lang.annotation.*;

/**
 * Annotation to tag an interface as a cqler.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cqler {
    String keyspace() default "";

    String cluster() default "DEFAULT";
}
