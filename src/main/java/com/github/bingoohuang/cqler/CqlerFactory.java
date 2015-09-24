package com.github.bingoohuang.cqler;

import com.github.bingoohuang.cqler.impl.CqlInvocationHandler;

import java.lang.reflect.Proxy;

public class CqlerFactory {
    public static <T> T getCqler(Class<T> cqlerClass) {
        return (T) Proxy.newProxyInstance(cqlerClass.getClassLoader(),
                new Class[]{cqlerClass},
                new CqlInvocationHandler(cqlerClass));
    }
}
