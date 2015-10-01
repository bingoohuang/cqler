package com.github.bingoohuang.cqler.domain;

import com.google.common.base.Objects;

public class TeamMember {
    private String teamName, memberName, location, manager, nationality, position;

    private TeamMember(Builder builder) {
        setTeamName(builder.teamName);
        setMemberName(builder.memberName);
        setLocation(builder.location);
        setManager(builder.manager);
        setNationality(builder.nationality);
        setPosition(builder.position);
    }

    public TeamMember() {
    }

    public static Builder newBuilder(String teamName, String memberName) {
        return new Builder(teamName, memberName);
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TeamMember that = (TeamMember) o;
        return Objects.equal(teamName, that.teamName) &&
                Objects.equal(memberName, that.memberName) &&
                Objects.equal(location, that.location) &&
                Objects.equal(manager, that.manager) &&
                Objects.equal(nationality, that.nationality) &&
                Objects.equal(position, that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(teamName, memberName, location, manager, nationality, position);
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public static final class Builder {
        private String teamName;
        private String memberName;
        private String location;
        private String manager;
        private String nationality;
        private String position;

        private Builder(String teamName, String memberName) {
            this.teamName = teamName;
            this.memberName = memberName;
        }

        public Builder withLocation(String val) {
            location = val;
            return this;
        }

        public Builder withManager(String val) {
            manager = val;
            return this;
        }

        public Builder withNationality(String val) {
            nationality = val;
            return this;
        }

        public Builder withPosition(String val) {
            position = val;
            return this;
        }

        public TeamMember build() {
            return new TeamMember(this);
        }
    }
}
