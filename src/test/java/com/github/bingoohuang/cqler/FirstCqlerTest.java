package com.github.bingoohuang.cqler;

import com.github.bingoohuang.cqler.domain.Keyspace;
import com.github.bingoohuang.cqler.domain.TeamMember;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FirstCqlerTest {
    static FirstCqler firstCqler;

    @BeforeClass
    public static void beforeClass() {
        firstCqler = CqlerFactory.getCqler(FirstCqler.class);
        firstCqler.dropKeyspace();
        firstCqler.createKeyspace();
        firstCqler.createTable();
    }

    @Test
    public void test1() {
        Keyspace firstcqler = firstCqler.getKeyspace("firstcqler");
        assertThat(firstcqler.getKeyspaceName()).isEqualTo("firstcqler");
    }

    @Test
    public void createTeam() {
        firstCqler.createTeam("Red Bull", "Christian Horner", "<unknown>");
        firstCqler.addTeamMemeber("Red Bull", "Ricciardo", "Australian", "driver");
        firstCqler.addTeamMemeber("Red Bull", "Kvyat", "Russian", "driver");

        TeamMember kvyat = firstCqler.findTeamMember("Red Bull", "Kvyat");
        TeamMember teamMember = TeamMember
                .newBuilder("Red Bull", "Kvyat")
                .withManager("Christian Horner")
                .withLocation("<unknown>")
                .withNationality("Russian")
                .withPosition("driver")
                .build();
        assertThat(kvyat).isEqualTo(teamMember);

        List<TeamMember> allTeamMembers = firstCqler.findAllTeamMembers("Red Bull");
        assertThat(allTeamMembers).hasSize(2);
    }
}
