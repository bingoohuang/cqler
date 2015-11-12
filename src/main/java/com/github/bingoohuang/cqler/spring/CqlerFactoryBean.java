package com.github.bingoohuang.cqler.spring;

import com.github.bingoohuang.cqler.CqlerFactory;
import org.springframework.beans.factory.FactoryBean;

public class CqlerFactoryBean<T> implements FactoryBean<T> {
    private Class<T> cqlerInterface;

    public void setCqlerInterface(Class<T> cqlerInterface) {
        this.cqlerInterface = cqlerInterface;
    }

    @Override
    public T getObject() throws Exception {
        return CqlerFactory.getCqler(cqlerInterface);
    }

    @Override
    public Class<?> getObjectType() {
        return this.cqlerInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
