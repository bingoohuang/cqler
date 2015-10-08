package com.github.bingoohuang.cqler;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class ClusterFactory {

    static LoadingCache<String, Cluster> cache = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, Cluster>() {
                @Override
                public Cluster load(String clusterName) throws Exception {
                    return ClusterFactory.load(clusterName);
                }
            });

    public static Cluster getCluster(String clusterName) {
        return cache.getUnchecked(clusterName);
    }


    private static Cluster load(String clusterName) throws Exception {
        PoolingOptions poolingOptions = new PoolingOptions();
        poolingOptions
                .setCoreConnectionsPerHost(HostDistance.LOCAL, 4)
                .setMaxConnectionsPerHost(HostDistance.LOCAL, 10)
                .setCoreConnectionsPerHost(HostDistance.REMOTE, 2)
                .setMaxConnectionsPerHost(HostDistance.REMOTE, 4);

        final Properties properties = loadClasspathPropertiesFile(
                "cql/" + clusterName + ".properties");

        String contactPoints = properties.getProperty("contactPoints", "127.0.0.1");
        Splitter splitter = Splitter.onPattern("\\s+").omitEmptyStrings().trimResults();
        List<String> contactPointList = splitter.splitToList(contactPoints);

        String port = properties.getProperty("port", "9042");


        return Cluster.builder()
                .addContactPoints(contactPointList.toArray(new String[0]))
                .withPort(Integer.parseInt(port))
                .withPoolingOptions(poolingOptions)
                .build();
    }


    public static Properties loadClasspathPropertiesFile(String propertiesFile) {
        final Properties properties = new Properties();
        try {
            ClassLoader classLoader = ClusterFactory.class.getClassLoader();
            InputStream is = classLoader.getResourceAsStream(propertiesFile);
            properties.load(is);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        return properties;
    }
}
