package com.github.bingoohuang.cqler.impl;

import com.datastax.driver.core.*;
import com.github.bingoohuang.cqler.ClusterFactory;
import com.github.bingoohuang.cqler.annotations.Cql;
import com.github.bingoohuang.cqler.annotations.Cqler;
import com.github.bingoohuang.cqler.impl.CqlParser.CqlParserResult;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

// http://www.planetcassandra.org/getting-started-with-apache-cassandra-and-java/
public class CqlInvocationHandler implements InvocationHandler {
    Logger logger = LoggerFactory.getLogger(CqlInvocationHandler.class);

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


        if (isKeyspaceDdl(cql)) {
            execCqlDirectly(cluster, cql);
            return null;
        }

        if (isQuery(cql)) {
            return executeQuery(method, cluster, keyspace, cql, args);
        }

        return executeOthers(cluster, keyspace, cql, args);
    }

    private Object executeOthers(
            Cluster cluster, String keyspace, String cql, Object[] args
    ) throws Exception {
        try (Session session = cluster.connect(keyspace)) {
            CqlParserResult parserResult = new CqlParser(cql, args).parseCql();
            BoundStatement boundStatement = bindParams(session, parserResult);
            ResultSet resultSet = session.execute(boundStatement);

        } catch (Exception e) {
            logger.error("execute {} failed", cql, e);
            throw Throwables.propagate(e);
        }

        return null;
    }

    private Object executeQuery(
            Method method, Cluster cluster, String keyspace, String cql, Object[] args
    ) throws Exception {
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
            logger.debug("execute cql : {}", cql);
            session.execute(cql);
        }
    }

    private Object parseBaseDataTypeResult(Method method, Map map) throws ParseException {
        Class<?> returnType = method.getReturnType();
        Object o = new Object();
        for (Object key : map.keySet()) {
            o = map.get(key);
        }
        if (returnType == int.class)
            return o == null ? null : Integer.parseInt(o.toString());
        if (returnType == byte.class)
            return o == null ? null : Byte.parseByte(o.toString());
        if (returnType == short.class)
            return o == null ? null : Short.parseShort(o.toString());
        if (returnType == long.class)
            return o == null ? null : Long.parseLong(o.toString());
        if (returnType == double.class)
            return o == null ? null : Double.parseDouble(o.toString());
        if (returnType == float.class)
            return o == null ? null : Float.parseFloat(o.toString());
        if (returnType == boolean.class)
            return o == null ? null : Boolean.parseBoolean(o.toString());
        if (returnType == Long.class) {
            return Long.valueOf(o.toString());
        }
        if (returnType == Date.class) {
//            Calendar c = Calendar.getInstance();
//            c.setTimeInMillis(Long.parseLong(o.toString()));
            Date date = (Date) o;

            return date;
        }

        return o;
    }

    private Object parseMapResult(Method method, List<Map> result) {
        for (Map map : result) {
            return map;
        }
        return null;
    }

    private Object parseCollectionResult(
            Method method, List<Map> result) throws Exception {
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
        Type[] actualTypeArguments = pt.getActualTypeArguments();
        Class<?> actualArgClass = (Class<?>) actualTypeArguments[0];

        if (parseBasicDataTypeResult(result, listResult, actualArgClass))
            return listResult;

        return parseCommonBeanResult(result, listResult, actualArgClass);

    }

    private Object parseCommonBeanResult(
            List<Map> result, List listResult, Class<?> aClass) throws Exception {
        for (Map map : result) {
            Object obj = aClass.newInstance();
            map2Bean(map, obj);
            listResult.add(obj);
        }
        return listResult;
    }

    private boolean parseBasicDataTypeResult(
            List<Map> result, List listResult, Class<?> aClass) {
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
            result.add(map);

            for (int i = 0, ii = resultSet.getColumnDefinitions().size(); i < ii; i++) {
                String key = resultSet.getColumnDefinitions()
                        .getName(i).toLowerCase()
                        .replaceAll("_", "");
                map.put(key, row.getObject(i));
            }
        }
        return result;
    }


    public BoundStatement bindParams(
            Session session, CqlParserResult cqlParserResult) {
        PreparedStatement ps = prepareCql(session, cqlParserResult);
        BoundStatement boundStatement = new BoundStatement(ps);
        logger.debug("execute cql {} with params: {}",
                cqlParserResult.execSql, cqlParserResult.bindParams);
        boundStatement.bind(cqlParserResult.bindParams);
        return boundStatement;
    }

    static Cache<String, PreparedStatement> preparedCache
            = CacheBuilder.newBuilder().build();

    private static PreparedStatement prepareCql(
            final Session session,
            final CqlParserResult cqlParserResult) {
        try {
            return preparedCache.get(cqlParserResult.execSql, new Callable<PreparedStatement>() {
                @Override
                public PreparedStatement call() throws Exception {
                    return session.prepare(cqlParserResult.execSql);
                }
            });
        } catch (ExecutionException e) {
            throw Throwables.propagate(e.getCause());
        }
    }

    private void map2Bean(Map map, Object obj) throws Exception {
        BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        for (PropertyDescriptor property : propertyDescriptors) {
            Method setter = property.getWriteMethod();
            if (setter != null) {
                String propertyName = property.getName().toLowerCase();
                if (map.get(propertyName) == null) continue;
                setter.invoke(obj, map.get(property.getName().toLowerCase()));
            }
        }
    }

    private boolean isQuery(String cql) {
        String lowerCaseCql = cql.toLowerCase();
        return lowerCaseCql.startsWith("select");
    }

    private boolean isKeyspaceDdl(String cql) {
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
                || clazz == String.class || clazz == Date.class
                || clazz == Long.class;
    }

}
