package com.github.bingoohuang.cqler;

import com.github.bingoohuang.cqler.annotations.Cql;
import com.github.bingoohuang.cqler.annotations.Cqler;
import com.github.bingoohuang.cqler.domain.Keyspace;
import com.github.bingoohuang.cqler.domain.ProcessConfig;
import com.github.bingoohuang.cqler.domain.TeamMember;

import java.util.List;
import java.util.Map;

/*
http://docs.datastax.com/en/developer/java-driver/2.0/java-driver/reference/javaClass2Cql3Datatypes_r.html

Java classes to CQL3 data types
CQL3 data type	Java type
ascii	java.lang.String
bigint	long
blob	java.nio.ByteBuffer
boolean	boolean
counter	long
decimal	java.math.BigDecimal
double	double
float	float
inet	java.net.InetAddress
int	int
list	java.util.List<T>
map	java.util.Map<K, V>
set	java.util.Set<T>
text	java.lang.String
timestamp	java.util.Date
timeuuid	java.util.UUID
uuid	java.util.UUID
varchar	java.lang.String
varint	java.math.BigInteger
 */

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
    void createTableTeammemberByTeam();


    @Cql("INSERT INTO teammember_by_team (teamname, manager, location)" +
            " VALUES (##, ##, ##)")
    void createTeam(String teamName, String manager, String location);

    @Cql("INSERT INTO teammember_by_team (membername, teamname, nationality, position)" +
            " VALUES (#2#, #1#, #3#, #4#)")
    void addTeamMember(String teamName, String memberName, String nationality, String position);

    @Cql("SELECT teamname, location, membername, manager, nationality, position" +
            " FROM teammember_by_team" +
            " WHERE teamname = ## and membername = ##")
    TeamMember findTeamMember(String teamName, String memberName);

    @Cql("SELECT teamname, membername, location, manager, nationality, position" +
            " FROM teammember_by_team" +
            " WHERE teamname = ##")
    List<TeamMember> findAllTeamMembers(String teamName);


    @Cql("SELECT * FROM system.schema_keyspaces")
    List<Keyspace> showKeyspaces();

    @Cql("SELECT * FROM system.schema_keyspaces WHERE keyspace_name = ##")
    Keyspace getKeyspace(String keyspaceName);


    @Cql("SELECT teamname FROM teammember_by_team")
    List<String> findTeamName();

    @Cql("SELECT membername FROM teammember_by_team")
    List findMemberName();

    @Cql("SELECT teamname, location, membername, manager, nationality, position" +
            " FROM teammember_by_team" +
            " WHERE teamname = ## and membername = ##")
    Map<String, String> findTeamMemberMap(String s, String kvyat);

    @Cql("SELECT count(*) FROM teammember_by_team")
    int findMemCount();

    @Cql("SELECT membername FROM teammember_by_team WHERE teamname = ## AND membername = ##")
    String findMemberNameStr(String teamName, String kvyat);

    @Cql("INSERT INTO teammember_by_team(membername, teamname, location, manager, nationality, position) " +
            "VALUES(#memberName#, #teamName#, #location#, #manager#, #nationality#, #position#)")
    void addTeamMemberByObject(TeamMember teamMember);

}
