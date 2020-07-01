package cn.jmu.pictureprocess.hbase.config;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.IOException;

@Configuration
public class HBaseConfig {
    @Value("${hbase.rootdir}")
    private String rootDir;

    @Scope("prototype")
    @Bean(name = "hBaseConfiguration")
    public org.apache.hadoop.conf.Configuration hBaseConfiguration() {
        org.apache.hadoop.conf.Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.rootdir", rootDir);
        /*        conf.set("hbase.zookeeper.quorum", properties.getZkQuorum());
        conf.set("hbase.zookeeper.property.clientPort", properties.getZkPort());
        conf.set("zookeeper.znode.parent", properties.getZkParent());*/
        return conf;
    }

    @Bean(name = "hBaseConnection")
    public Connection hBaseConnection(@Qualifier("hBaseConfiguration") org.apache.hadoop.conf.Configuration conf) throws IOException {
        return ConnectionFactory.createConnection(conf);
    }

    @Bean(name = "hBaseAdmin")
    public Admin hBaseAdmin(@Qualifier("hBaseConnection") Connection connection) throws IOException {
        return connection.getAdmin();
    }


}
