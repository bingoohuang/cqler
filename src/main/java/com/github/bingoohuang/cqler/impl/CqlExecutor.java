package com.github.bingoohuang.cqler.impl;

import com.datastax.driver.core.*;
import com.github.bingoohuang.cqler.ClusterFactory;
import com.github.bingoohuang.cqler.annotations.Cql;
import com.github.bingoohuang.cqler.annotations.Cqler;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;

import java.beans.Introspector;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
public class CqlExecutor {
    private final Cluster cluster;
    private final String cql;
    private final Method method;
    private final String keyspace;
    private final CqlParser.CqlParserResult parserResult;
    private final String lowerCaseCql;

    public CqlExecutor(Method method, Object[] args) {
        this.method = method;
        Cqler cqlerAnn = method.getDeclaringClass().getAnnotation(Cqler.class);
        this.cluster = ClusterFactory.getCluster(cqlerAnn.cluster());
        this.keyspace = cqlerAnn.keyspace();
        this.cql = method.getAnnotation(Cql.class).value();
        this.lowerCaseCql = StringUtils.trim(cql.toLowerCase());
        this.parserResult = new CqlParser(cql, args).parseCql();
        log.debug("execute cql {} with params: {}",
                parserResult.execSql, parserResult.bindParams);
    }

    public Object executeCql() throws Exception {
        try {
            if (isKeyspaceDdl()) {
                execCqlDirectly();
                return null;
            }

            ResultSet resultSet = execute();
            return isQuery() ? parseResult(resultSet) : resultSet;
        } catch (Exception e) {
            log.error("execute {} failed", cql, e);
            throw e;
        }
    }


    @SneakyThrows
    private ResultSet execute() {
        @Cleanup Session session = cluster.connect(keyspace);
        val ps = session.prepare(parserResult.execSql);
        val boundStatement = new BoundStatement(ps);
        boundStatement.bind(parserResult.bindParams);

        return session.execute(boundStatement);
    }

    private Object parseResult(ResultSet resultSet) throws Exception {
        List<Map<String, Object>> rows = getResultMaps(resultSet);

        if (Collection.class.isAssignableFrom(method.getReturnType()))
            return parseCollectionResult(rows);

        if (rows.isEmpty()) return null;
        if (method.getReturnType() == Map.class)
            return parseMapResult(rows);

        Map row = rows.get(0);

        if (isBasicDataTypes(method.getReturnType()))
            return parseBaseDataTypeResult(row);

        Object obj = method.getReturnType().newInstance();
        map2Bean(row, obj);
        return obj;
    }

    private void execCqlDirectly() {
        @Cleanup Session session = cluster.connect();
        session.execute(cql);
    }

    private Object parseBaseDataTypeResult(Map map) {
        Class<?> returnType = method.getReturnType();
        Object o = null;
        for (Object key : map.keySet()) {
            o = map.get(key);
        }

        if (o == null) return null;

        String s = o.toString();
        if (returnType == int.class) return Integer.parseInt(s);
        if (returnType == byte.class) return Byte.parseByte(s);
        if (returnType == short.class) return Short.parseShort(s);
        if (returnType == long.class) return Long.parseLong(s);
        if (returnType == double.class) return Double.parseDouble(s);
        if (returnType == float.class) return Float.parseFloat(s);
        if (returnType == boolean.class) return Boolean.parseBoolean(s);
        if (returnType == Long.class) return Long.valueOf(s);

        return o;
    }

    private Object parseMapResult(List<Map<String, Object>> result) {
        for (Map map : result) {
            return map;
        }
        return null;
    }

    private Object parseCollectionResult(List<Map<String, Object>> result) throws Exception {
        if (method.getReturnType() == List.class)
            return getListResult(result);

        return result;
    }

    private Object getListResult(List<Map<String, Object>> result) throws Exception {
        List listResult = Lists.newArrayList();
        if (!ParameterizedType.class.
                isAssignableFrom(method.getGenericReturnType().getClass())) {
            parseNoParadigmResult(result, listResult);
            return listResult;
        }

        val pt = (ParameterizedType) method.getGenericReturnType();
        Class<?> actualArgClass = (Class<?>) pt.getActualTypeArguments()[0];

        if (parseBasicDataTypeResult(result, listResult, actualArgClass))
            return listResult;

        return parseCommonBeanResult(result, listResult, actualArgClass);
    }

    private List<Object> parseCommonBeanResult(
            List<Map<String, Object>> result, List<Object> listResult, Class<?> aClass) throws Exception {
        for (Map<String, Object> map : result) {
            Object obj = aClass.newInstance();
            map2Bean(map, obj);
            listResult.add(obj);
        }
        return listResult;
    }

    private boolean parseBasicDataTypeResult(
            List<Map<String, Object>> result, List listResult, Class<?> aClass) {
        if (isBasicDataTypes(aClass)) {
            parseNoParadigmResult(result, listResult);
            return true;
        }
        return false;
    }

    private void parseNoParadigmResult(List<Map<String, Object>> result, List listResult) {
        for (Map<String, Object> map : result) {
            for (Object key : map.keySet()) {
                listResult.add(map.get(key));
            }
        }
    }

    private List<Map<String, Object>> getResultMaps(ResultSet resultSet) {
        List<Map<String, Object>> result = Lists.newArrayList();
        val columnDefinitions = resultSet.getColumnDefinitions();
        for (Row row : resultSet) {
            Map<String, Object> map = Maps.newHashMap();
            result.add(map);

            for (int i = 0, ii = columnDefinitions.size(); i < ii; i++) {
                String key = columnDefinitions.getName(i).toLowerCase()
                        .replaceAll("_", "");
                map.put(key, row.getObject(i));
            }
        }
        return result;
    }

    private void map2Bean(Map map, Object obj) throws Exception {
        val beanInfo = Introspector.getBeanInfo(obj.getClass());
        val propertyDescriptors = beanInfo.getPropertyDescriptors();
        for (val property : propertyDescriptors) {
            val setter = property.getWriteMethod();
            if (setter == null) continue;

            String propertyName = property.getName().toLowerCase();
            Object propertyValue = map.get(propertyName);
            if (propertyValue == null) continue;

            setter.invoke(obj, propertyValue);
        }
    }

    private boolean isQuery() {
        return lowerCaseCql.startsWith("select");
    }

    private boolean isKeyspaceDdl() {
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
