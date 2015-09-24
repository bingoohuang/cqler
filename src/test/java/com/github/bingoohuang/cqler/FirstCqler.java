package com.github.bingoohuang.cqler;

import com.github.bingoohuang.cqler.annotations.Cql;
import com.github.bingoohuang.cqler.annotations.Cqler;
import com.github.bingoohuang.cqler.domain.Keyspace;

import java.util.List;

@Cqler
public interface FirstCqler {
    // http://wiki.apache.org/cassandra/GettingStarted
    @Cql("drop keyspace if exists firstcqler")
    void dropKeyspace();

    @Cql("create keyspace firstcqler with replication = " +
            "{'class': 'SimpleStrategy', 'replication_factor': 1 }")
    void createKeyspace();

    // https://blogs.infosupport.com/static-columns-in-cassandra-and-their-benefits/
    // http://www.ipponusa.com/blog/modeling-data-with-cassandra-what-cql-hides-away-from-you/
    @Cql("CREATE TABLE teammember_by_team (" +
            " teamname text," +
            " manager text static," +
            " location text static," +
            " membername text," +
            " nationality text," +
            " position text," +
            " PRIMARY KEY ((teamname), membername)" +
            ")")
    void createTable();


    @Cql("select * from system.schema_keyspaces")
    List<Keyspace> showKeyspaces();

    @Cql("select * from system.schema_keyspaces where keyspace_name = 'firstcqler'")
    Keyspace getKeyspace(String keyspaceName);

}
