package com.github.bingoohuang.cqler.impl;

import lombok.SneakyThrows;
import org.apache.commons.beanutils.PropertyUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CqlParser {
    static Pattern seqParamPlaceholder = Pattern.compile("#(\\d+?)#");
    static Pattern attrParamPlaceholder = Pattern.compile("#(\\S+?)#");

    static class CqlParserResult {
        public final Object[] bindParams;
        public final String execSql;

        public CqlParserResult(String execSql, Object[] bindParams) {
            this.execSql = execSql;
            this.bindParams = bindParams;
        }
    }

    final String cql;
    final Object[] args;

    public CqlParser(String cql, Object[] args) {
        this.cql = cql;
        this.args = args;
    }

    public CqlParserResult parseCql()  {
        if (cql.contains("##")) return parseAutoParams();
        if (seqParamPlaceholder.matcher(cql).find()) return parseSeqParams();
        return parseAttrParams();
    }

    private CqlParserResult parseSeqParams() {
        Matcher matcher = seqParamPlaceholder.matcher(cql);
        if (!matcher.find()) return new CqlParserResult(cql, new Object[0]);

        List<Integer> seqs = new ArrayList<Integer>();

        for (int pos = 0; matcher.find(pos); pos = matcher.end()) {
            String seqNum = matcher.group(1);
            seqs.add(Integer.parseInt(seqNum));
        }

        Object[] bindParams = new Object[seqs.size()];
        for (int i = 0; i < seqs.size(); i++) {
            bindParams[i] = args[seqs.get(i) - 1];
        }

        String execSql = matcher.replaceAll("?");
        return new CqlParserResult(execSql, bindParams);

    }

    @SneakyThrows
    private CqlParserResult parseAttrParams()  {
        Matcher matcher = attrParamPlaceholder.matcher(cql);
        List<Object> attrSeqs = new ArrayList<Object>();

        for (int pos = 0; matcher.find(pos); pos = matcher.end()) {
            String seqNum = matcher.group(1);
            attrSeqs.add(PropertyUtils.getProperty(args[0], seqNum));
        }
        String execSql = matcher.replaceAll("?");
        Object[] objects = new Object[attrSeqs.size()];

        for (int i = 0; i < attrSeqs.size(); i++) {
            objects[i] = attrSeqs.get(i);
        }
        return new CqlParserResult(execSql, objects);

    }

    private CqlParserResult parseAutoParams() {
        String execSql = cql.replaceAll("##", "?");
        return new CqlParserResult(execSql, args);
    }
}

