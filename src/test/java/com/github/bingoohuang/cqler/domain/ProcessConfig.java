package com.github.bingoohuang.cqler.domain;

import com.google.common.base.Objects;

import java.util.List;

public class ProcessConfig {
    String processName;
    List<String> args;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProcessConfig that = (ProcessConfig) o;
        return Objects.equal(processName, that.processName) &&
                Objects.equal(args, that.args);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(processName, args);
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }
}
