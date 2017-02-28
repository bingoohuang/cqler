package com.github.bingoohuang.cqler;

import com.github.bingoohuang.cqler.domain.Keyspace;
import com.github.bingoohuang.cqler.domain.TeamMember;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class FirstCqlerTest {
    static FirstCqler firstCqler;

    @BeforeClass
    public static void beforeClass() {
        firstCqler = CqlerFactory.getCqler(FirstCqler.class);
        firstCqler.dropKeyspace();
        firstCqler.createKeyspace();
        firstCqler.createTableTeammemberByTeam();
    }

    @Test
    public void test1() {
        Keyspace firstcqler = firstCqler.getKeyspace("firstcqler");
        assertThat(firstcqler.getKeyspaceName()).isEqualTo("firstcqler");
    }

    @Test
    public void createTeam() {
        firstCqler.createTeam("Red Bull", "Christian Horner", "<unknown>");
        firstCqler.addTeamMember("Red Bull", "Ricciardo", "Australian", "driver");
        firstCqler.addTeamMember("Red Bull", "Kvyat", "Russian", "driver");

        TeamMember kvyat = firstCqler.findTeamMember("Red Bull", "Kvyat");
        System.out.println(kvyat.getMemberName());
        TeamMember teamMember = TeamMember
                .newBuilder("Red Bull", "Kvyat")
                .withManager("Christian Horner")
                .withLocation("<unknown>")
                .withNationality("Russian")
                .withPosition("driver")
                .build();
        assertThat(kvyat).isEqualTo(teamMember);

        List<TeamMember> allTeamMembers = firstCqler.findAllTeamMembers("Red Bull");
        for (TeamMember teamMem : allTeamMembers) {
            assertThat(teamMem.getTeamName()).isEqualTo("Red Bull");
        }

        assertThat(allTeamMembers).hasSize(2);
    }


    @Test
    public void testList() {
        firstCqler.createTeam("Red Bull", "Christian Horner", "<unknown>");
        firstCqler.addTeamMember("Red Bull", "Ricciardo", "Australian", "driver");
        firstCqler.addTeamMember("Red Bull", "Kvyat", "Russian", "driver");
        List<String> teamNames = firstCqler.findTeamName();

        assertThat(teamNames.get(0)).isEqualTo("Red Bull");
    }

    @Test
    public void testList1() {
        firstCqler.createTeam("Red Bull", "Christian Horner", "<unknown>");
        firstCqler.addTeamMember("Red Bull", "Ricciardo", "Australian", "driver");
        firstCqler.addTeamMember("Red Bull", "Kvyat", "Russian", "driver");
        List MemberNames = firstCqler.findMemberName();
        assertThat(MemberNames.get(0)).isEqualTo("Kvyat");
    }

    @Test
    public void testMap() {
        firstCqler.createTeam("Red Bull", "Christian Horner", "<unknown>");
        firstCqler.addTeamMember("Red Bull", "Ricciardo", "Australian", "driver");
        firstCqler.addTeamMember("Red Bull", "Kvyat", "Russian", "driver");

        Map<String, String> teamMember = firstCqler.findTeamMemberMap("Red Bull", "Kvyat");

        assertThat(teamMember.size()).isEqualTo(6);
        assertThat(teamMember)
                .contains(entry("position", "driver"))
                .contains(entry("nationality", "Russian"))
                .contains(entry("manager", "Christian Horner"))
                .contains(entry("location", "<unknown>"))
                .contains(entry("teamname", "Red Bull"))
                .contains(entry("membername", "Kvyat"))
        ;
    }

    @Test
    public void showKeySpace() {
        firstCqler.createTeam("Red Bull", "Christian Horner", "<unknown>");
        firstCqler.addTeamMember("Red Bull", "Ricciardo", "Australian", "driver");
        firstCqler.addTeamMember("Red Bull", "Kvyat", "Russian", "driver");

        List<Keyspace> keyspaces = firstCqler.showKeyspaces();
        assertThat(keyspaces.size()).isGreaterThanOrEqualTo(4);
        assertThat(keyspaces).contains(new Keyspace("firstcqler"));
    }

    @Test
    public void getCountMem(){
        firstCqler.createTeam("Red Bull", "Christian Horner", "<unknown>");
        firstCqler.addTeamMember("Red Bull", "Ricciardo", "Australian", "driver");
        firstCqler.addTeamMember("Red Bull", "Kvyat", "Russian", "driver");

        int memCount = firstCqler.findMemCount();

        assertThat(memCount).isEqualTo(3);
    }

    @Test
    public void testString(){
        firstCqler.createTeam("Red Bull", "Christian Horner", "<unknown>");
        firstCqler.addTeamMember("Red Bull", "Ricciardo", "Australian", "driver");
        firstCqler.addTeamMember("Red Bull", "Kvyat", "Russian", "driver");

        String str = firstCqler.findMemberNameStr("Red Bull","Kvyat");

        assertThat(str).isEqualTo("Kvyat");
    }


    @Test
    public void testInsertObject(){
        firstCqler.createTeam("Red Bull", "Christian Horner", "<unknown>");
        firstCqler.addTeamMember("Red Bull", "Ricciardo", "Australian", "driver");
        firstCqler.addTeamMember("Red Bull", "Kvyat", "Russian", "driver");
        TeamMember teamMember = TeamMember.newBuilder("test","123")
                .withLocation("南京")
                .withManager("不知道")
                .withNationality("..")
                .withPosition("1232323")
                .build();
        firstCqler.addTeamMemberByObject(teamMember);

        TeamMember test = firstCqler.findTeamMember("test", "123");

        assertThat("不知道").isEqualTo(test.getManager());
    }

}
