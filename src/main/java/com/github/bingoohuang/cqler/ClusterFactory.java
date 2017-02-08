package com.github.bingoohuang.cqler;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.google.common.base.Splitter;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.SneakyThrows;
import lombok.val;

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
        val options = new PoolingOptions();
        options.setCoreConnectionsPerHost(HostDistance.LOCAL, 4);
        options.setMaxConnectionsPerHost(HostDistance.LOCAL, 10);
        options.setCoreConnectionsPerHost(HostDistance.REMOTE, 2);
        options.setMaxConnectionsPerHost(HostDistance.REMOTE, 4);

        val props = loadClasspathProperties(
                "cqler/" + clusterName + ".properties");
        val contactPoints = props.getProperty("contactPoints", "127.0.0.1");
        val splitter = Splitter.onPattern("\\s+").omitEmptyStrings().trimResults();
        val contactPointList = splitter.splitToList(contactPoints);
        val port = props.getProperty("port", "9042");

        return Cluster.builder()
                .addContactPoints(contactPointList.toArray(new String[0]))
                .withPort(Integer.parseInt(port))
                .withPoolingOptions(options)
                .build();
    }

    @SneakyThrows
    public static Properties loadClasspathProperties(String propertiesFile) {
        val classLoader = ClusterFactory.class.getClassLoader();
        val is = classLoader.getResourceAsStream(propertiesFile);
        if (is == null) {
            throw new RuntimeException("property file "
                    + propertiesFile + " does not exit");
        }

        val properties = new Properties();
        properties.load(is);
        return properties;
    }
}
