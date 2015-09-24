package com.github.bingoohuang.cqler;

import com.github.bingoohuang.cqler.domain.Keyspace;
import org.junit.BeforeClass;
import org.junit.Test;

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
}
