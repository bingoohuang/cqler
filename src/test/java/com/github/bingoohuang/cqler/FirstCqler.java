package com.github.bingoohuang.cqler;

import com.github.bingoohuang.cqler.annotations.Cql;
import com.github.bingoohuang.cqler.annotations.Cqler;
import com.github.bingoohuang.cqler.domain.Keyspace;
import com.github.bingoohuang.cqler.domain.TeamMember;

import java.util.List;

@Cqler(keyspace = "firstcqler", cluster = "cluster1")
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

    @Cql("INSERT INTO teammember_by_team (teamname, manager, location)" +
            " VALUES (##, ##, ##)")
    void createTeam(String teamName, String manager, String location);

    @Cql("INSERT INTO teammember_by_team (teamname, membername, nationality, position)" +
            " VALUES (#1#, #2#, #3#, #4#);")
    void addTeamMemeber(String teamName, String memberName, String nationality, String position);

    @Cql("SELECT teamname, membername, location, manager, nationality, position" +
            " FROM teammember_by_team" +
            " WHERE teamname = ## and membername = ##")
    TeamMember findTeamMember(String teamName, String memberName);

    @Cql("SELECT teamname, membername, location, manager, nationality, position" +
            " FROM teammember_by_team" +
            " WHERE teamname = ##")
    List<TeamMember> findAllTeamMembers(String teamName);


    @Cql("select * from system.schema_keyspaces")
    List<Keyspace> showKeyspaces();

    @Cql("select * from system.schema_keyspaces where keyspace_name = ##")
    Keyspace getKeyspace(String keyspaceName);
}
