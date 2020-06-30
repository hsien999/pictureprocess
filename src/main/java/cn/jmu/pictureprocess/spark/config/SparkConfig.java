package cn.jmu.pictureprocess.spark.config;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkConfig {
    @Value("${spark.appname}")
    private String appName;
    @Value("${spark.master}")
    private String master;
    @Value("${spark.executor-cores}")
    private String executorCores;
    @Value("${spark.executor-memory}")
    private String executorMemory;

    @Bean
    public SparkConf sparkConf() {
        return new SparkConf().
                setAppName(appName).
                setMaster(master).
                set("executor-cores", executorCores).
                set("executor-memory", executorMemory);
    }

    @Bean
    public JavaSparkContext sparkContext(@Autowired SparkConf sparkconf) {
        return new JavaSparkContext(sparkconf);
    }

}
