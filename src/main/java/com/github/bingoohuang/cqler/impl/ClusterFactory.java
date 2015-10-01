package com.github.bingoohuang.cqler.impl;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ClusterFactory {

    static LoadingCache<String, Cluster> cache = CacheBuilder.newBuilder()
            .build(new CacheLoader<String, Cluster>() {
                final Properties properties = new Properties();
                @Override
                public Cluster load(String clusterName) throws Exception {
                    PoolingOptions poolingOptions = new PoolingOptions();
                    poolingOptions
                            .setCoreConnectionsPerHost(HostDistance.LOCAL, 4)
                            .setMaxConnectionsPerHost(HostDistance.LOCAL, 10)
                            .setCoreConnectionsPerHost(HostDistance.REMOTE, 2)
                            .setMaxConnectionsPerHost(HostDistance.REMOTE, 4);
                    try {
                        InputStream is = getClass().getClassLoader().getResourceAsStream("cql/"+clusterName+".properties");
                        properties.load(is);
                    } catch (IOException e) {
                    }

                    return Cluster.builder()
                            .addContactPoint(properties.getProperty("contractPoint"))
                            .withPort(Integer.parseInt(properties.getProperty("port")))
                            .withPoolingOptions(poolingOptions)
                            .build();
                }
            });

    public static Cluster getCluster(String clusterName) {
        return cache.getUnchecked(clusterName);
    }
}
