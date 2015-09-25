package com.github.bingoohuang.cqler.impl;

import com.datastax.driver.core.*;
import com.github.bingoohuang.cqler.annotations.Cql;
import com.github.bingoohuang.cqler.annotations.Cqler;
import com.github.bingoohuang.cqler.domain.Keyspace;
import com.google.common.collect.Lists;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

public class CqlInvocationHandler implements InvocationHandler {

    public CqlInvocationHandler() {
    }

    static Cluster cluster = Cluster.builder().addContactPoint("127.0.0.1").build();

    @Override
    public Object invoke(Object proxy, Method method, Object[] args
    ) throws Throwable {
        Cql cqlAnn = method.getAnnotation(Cql.class);
        String cql = cqlAnn.value();

        if (isDdlWoKeyspace(cql)) {
            Session session = cluster.connect();
            session.execute(cql);
            session.close();
            return null;
        }

        Class<?> hostClass = method.getDeclaringClass();
        Cqler cqlerAnn = hostClass.getAnnotation(Cqler.class);
        String keyspace = cqlerAnn.keyspace();

        if (isQuery(cql)) {
            Session session = cluster.connect(keyspace);
            ResultSet resultSet = session.execute(cql);
            List<Keyspace> keyspaces = Lists.newArrayList();
            for (Row row : resultSet) {
                String keyspaceName = row.getString(0);
                Keyspace keyspaceBean = new Keyspace();
                keyspaceBean.setKeyspaceName(keyspaceName);
                keyspaces.add(keyspaceBean);
            }
            session.close();

            if (Collection.class.isAssignableFrom(method.getReturnType())) {
                return keyspaces;
            } else if (keyspaces.isEmpty()) {
                return null;
            } else {
                return keyspaces.get(0);
            }

        }

        Session session = cluster.connect(keyspace);
        session.execute(cql);
        session.close();

        return null;
    }

    private boolean isQuery(String cql) {
        String lowerCaseCql = cql.toLowerCase();
        return lowerCaseCql.startsWith("select");
    }

    private boolean isDdlWoKeyspace(String cql) {
        String lowerCaseCql = cql.toLowerCase();
        return lowerCaseCql.startsWith("create keyspace")
                || lowerCaseCql.startsWith("drop keyspace")
                || lowerCaseCql.startsWith("alter keyspace");
    }
}
