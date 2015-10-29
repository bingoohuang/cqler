package com.github.bingoohuang.cqler;

import com.github.bingoohuang.cqler.annotations.Cql;
import com.github.bingoohuang.cqler.annotations.Cqler;
import com.github.bingoohuang.cqler.domain.ProcessConfig;

import java.util.List;

@Cqler(keyspace = "firstcqler", cluster = "cluster1")
public interface CollectionCqler {
    @Cql("CREATE TABLE process_config(" +
            "processName text," +
            "args list<text>," +
            "PRIMARY KEY((processName)))")
    void createTableProcessConfig();

    @Cql("INSERT INTO process_config(processName, args) " +
            "VALUES (#processName#, #args#)")
    void insertProcessConfig(ProcessConfig processConfig);

    @Cql("SELECT processName, args FROM process_config")
    List<ProcessConfig> queryProcessConfig();
}
