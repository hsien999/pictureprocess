package cn.jmu.pictureprocess;

import cn.jmu.pictureprocess.hbase.config.HBaseConfig;
import com.sun.jersey.spi.container.servlet.WebConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@SpringBootApplication
public class PictureProcessApplication {


    public static void main(String[] args) {
        SpringApplication.run(PictureProcessApplication.class, args);
    }

}
