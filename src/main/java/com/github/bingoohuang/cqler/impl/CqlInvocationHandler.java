package com.github.bingoohuang.cqler.impl;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

// http://www.planetcassandra.org/getting-started-with-apache-cassandra-and-java/
@Slf4j
public class CqlInvocationHandler implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args
    ) throws Throwable {
        // If the method is a method from Object then defer to normal invocation.
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        return new CqlExecutor(method, args).executeCql();
    }
}
