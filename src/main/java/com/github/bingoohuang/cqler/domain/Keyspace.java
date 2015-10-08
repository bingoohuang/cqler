package com.github.bingoohuang.cqler.domain;

import com.google.common.base.Objects;

public class Keyspace {
    String keyspaceName;

    public Keyspace() {
    }

    public Keyspace(String keyspaceName) {
        this.keyspaceName = keyspaceName;
    }

    public String getKeyspaceName() {
        return keyspaceName;
    }

    public void setKeyspaceName(String keyspaceName) {
        this.keyspaceName = keyspaceName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Keyspace keyspace = (Keyspace) o;
        return Objects.equal(keyspaceName, keyspace.keyspaceName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(keyspaceName);
    }
}
