package com.github.bingoohuang.cqler.annotations;

import java.lang.annotation.*;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cql {
    // cql from https://cassandra.apache.org/doc/cql3/CQL.html
    String value();
}
