package com.github.bingoohuang.cqler.impl;

import com.datastax.driver.core.*;
import com.github.bingoohuang.cqler.annotations.Cql;
import com.github.bingoohuang.cqler.annotations.Cqler;
import com.github.bingoohuang.cqler.impl.CqlParser.CqlParserResult;
import com.github.bingoohuang.cqler.util.ReflectionUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;

// http://www.planetcassandra.org/getting-started-with-apache-cassandra-and-java/
public class CqlInvocationHandler implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args
    ) throws Throwable {
        // If the method is a method from Object then defer to normal invocation.
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }

        return executeCql(method, args);

    }

    private Object executeCql(Method method, Object[] args) throws Exception {
        Class<?> hostClass = method.getDeclaringClass();
        Cqler cqlerAnn = hostClass.getAnnotation(Cqler.class);
        String keyspace = cqlerAnn.keyspace();
        String clusterName = cqlerAnn.cluster();
        Cluster cluster = ClusterFactory.getCluster(clusterName);

        Cql cqlAnn = method.getAnnotation(Cql.class);
        String cql = cqlAnn.value();

        if (isDdlWithKeyspace(cql)) {
            execCqlDirectly(cluster, cql);
            return null;
        }

        if (isDml(cql)) {
            return executeQuery(method, args, keyspace, cluster, cql);
        }

        return executeDdl(args, keyspace, cluster, cql);
    }

    private Object executeDdl(Object[] args, String keyspace, Cluster cluster, String cql) {
        try (Session session = cluster.connect(keyspace)) {
            CqlParserResult parserResult = new CqlParser(cql, args).parseCql();
            BoundStatement boundStatement = bindParams(session, parserResult);
            session.execute(boundStatement);
        }
        return null;
    }

    private Object executeQuery(Method method, Object[] args, String keyspace, Cluster cluster, String cql) throws Exception {
        List<Map> result;

        try (Session session = cluster.connect(keyspace)) {
            CqlParserResult parserResult = new CqlParser(cql, args).parseCql();
            BoundStatement boundStatement = bindParams(session, parserResult);
            ResultSet resultSet = session.execute(boundStatement);
            result = getResultMaps(resultSet);
        }

        if (Collection.class.isAssignableFrom(method.getReturnType()))
            return parseCollectionResult(method, result);
        if (result.isEmpty()) return null;
        if (method.getReturnType() == Map.class)
            return parseMapResult(method, result);

        Map map = result.get(0);
        if (map == null) return null;
        if (isBasicDataTypes(method.getReturnType()))
            return parseBaseDataTypeResult(method, map);
        Object obj = method.getReturnType().newInstance();
        map2Bean(map, obj);
        return obj;
    }

    private void execCqlDirectly(Cluster cluster, String cql) {
        try (Session session = cluster.connect()) {
            session.execute(cql);
        }
    }

    private Object parseBaseDataTypeResult(Method method, Map map) {
        Class<?> returnType = method.getReturnType();
        Object o = new Object();
        for (Object key : map.keySet()) {
            o = map.get(key);
        }
        if (returnType == int.class) return Integer.parseInt(o.toString());
        if (returnType == byte.class) return Byte.parseByte(o.toString());
        if (returnType == short.class) return Short.parseShort(o.toString());
        if (returnType == long.class) return Long.parseLong(o.toString());
        if (returnType == double.class) return Double.parseDouble(o.toString());
        if (returnType == float.class) return Float.parseFloat(o.toString());
        if (returnType == boolean.class)
            return Boolean.parseBoolean(o.toString());
        return o;
    }

    private Object parseMapResult(Method method, List<Map> result) {
        for (Map map : result) {
            return map;
        }
        return null;
    }

    private Object parseCollectionResult(Method method, List<Map> result) throws Exception {
        if (method.getReturnType() == List.class)
            return getListResult(method, result);
        return result;
    }

    private Object getListResult(Method method, List<Map> result)
            throws Exception {
        List listResult = Lists.newArrayList();
        if (!ParameterizedType.class.
                isAssignableFrom(method.getGenericReturnType().getClass())) {
            parseNoParadigmResult(result, listResult);
            return listResult;
        }

        ParameterizedType pt = (ParameterizedType) method.getGenericReturnType();
        Type[] ownerType = pt.getActualTypeArguments();
        Class<?> aClass = ReflectionUtils.getClass(ownerType[0]);

        if (parseBasicDataTypeResult(result, listResult, aClass))
            return listResult;

        return parseCommonBeanResult(result, listResult, aClass);

    }

    private Object parseCommonBeanResult(List<Map> result, List listResult, Class<?> aClass) throws Exception {
        for (Map map : result) {
            Object obj = aClass.newInstance();
            map2Bean(map, obj);
            listResult.add(obj);
        }
        return listResult;
    }

    private boolean parseBasicDataTypeResult(List<Map> result, List listResult, Class<?> aClass) {
        if (isBasicDataTypes(aClass)) {
            parseNoParadigmResult(result, listResult);
            return true;
        }
        return false;
    }

    private void parseNoParadigmResult(List<Map> result, List listResult) {
        for (Map map : result) {
            for (Object key : map.keySet()) {
                listResult.add(map.get(key));
            }
        }
    }

    private List<Map> getResultMaps(ResultSet resultSet) {
        List<Map> result = Lists.newArrayList();
        for (Row row : resultSet) {
            Map map = Maps.newHashMap();
            for (int i = 0; i < resultSet.getColumnDefinitions().size(); i++) {
                map.put(resultSet.getColumnDefinitions()
                        .getName(i).toLowerCase()
                        .replaceAll("_", ""), row.getObject(i));
            }
            result.add(map);
        }
        return result;
    }


    public static BoundStatement bindParams(Session session, CqlParserResult cqlParserResult) {
        PreparedStatement ps = session.prepare(cqlParserResult.execSql);
        BoundStatement boundStatement = new BoundStatement(ps);
        boundStatement.bind(cqlParserResult.bindParams);
        return boundStatement;
    }

    private void map2Bean(Map map, Object obj) throws Exception {
        BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        for (PropertyDescriptor property : propertyDescriptors) {
            Method setter = property.getWriteMethod();
            if (setter != null) {
                setter.invoke(obj, map.get(property.getName().toLowerCase()));
            }
        }
    }

    private boolean isDml(String cql) {
        String lowerCaseCql = cql.toLowerCase();
        return lowerCaseCql.startsWith("select");
    }

    private boolean isDdlWithKeyspace(String cql) {
        String lowerCaseCql = cql.toLowerCase();
        return lowerCaseCql.startsWith("create keyspace")
                || lowerCaseCql.startsWith("drop keyspace")
                || lowerCaseCql.startsWith("alter keyspace");
    }

    private boolean isBasicDataTypes(Class<?> clazz) {
        return clazz == int.class || clazz == char.class ||
                clazz == short.class || clazz == byte.class ||
                clazz == long.class || clazz == double.class ||
                clazz == float.class || clazz == boolean.class
                || clazz == String.class;
    }

}
