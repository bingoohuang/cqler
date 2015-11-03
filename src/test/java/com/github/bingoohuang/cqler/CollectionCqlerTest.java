package com.github.bingoohuang.cqler;

import com.github.bingoohuang.cqler.domain.ProcessConfig;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;

public class CollectionCqlerTest {
    static FirstCqler firstCqler;
    static CollectionCqler collectionCqler;

    @BeforeClass
    public static void beforeClass() {
        firstCqler = CqlerFactory.getCqler(FirstCqler.class);
        firstCqler.dropKeyspace();
        firstCqler.createKeyspace();

        collectionCqler = CqlerFactory.getCqler(CollectionCqler.class);
        collectionCqler.createTableProcessConfig();
    }

    @Test
    public void test() {
        ProcessConfig processConfig = new ProcessConfig();
        processConfig.setProcessName("Yoga-Mobile");
        processConfig.setArgs(of("java", "javascript", "guava"));
        collectionCqler.insertProcessConfig(processConfig);
        List<ProcessConfig> processConfigs = collectionCqler.queryProcessConfig();
        assertThat(processConfigs).hasSize(1).contains(processConfig);

        List<String> processes = collectionCqler.queryProcesses();
        assertThat(processes).hasSize(1).contains("Yoga-Mobile");
    }
}
