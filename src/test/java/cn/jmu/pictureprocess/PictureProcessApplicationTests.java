package cn.jmu.pictureprocess;

import cn.jmu.pictureprocess.entity.Pictures;
import cn.jmu.pictureprocess.hbase.service.HBaseService;
import cn.jmu.pictureprocess.spark.service.SearchService;
import cn.jmu.pictureprocess.util.BMPPicture;
import cn.jmu.pictureprocess.util.MyBytesUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.imageio.stream.FileImageInputStream;
import java.io.*;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.*;

@SpringBootTest
class PictureProcessApplicationTests implements Serializable {


    @Value("spring.profiles.active")
    private String profile;

    @Test
    void testEncode() {
        if (!profile.equals("dev")) return;
        //获取系统默认编码
        System.out.println("系统默认编码:" + System.getProperty("file.encoding"));//查询结果GBK
        //系统默认字符编码
        System.out.println("系统默认字符编码:" + Charset.defaultCharset()); //查询结果GBK
        //操作系统用户使用的语言
        System.out.println("系统默认语言:" + System.getProperty("user.language")); //查询结果zh
        //*
    }

    @Resource
    private HBaseService hBaseService;

    /**
     * 本地图片上传到HBase数据库
     * <p>
     * Pictures
     * FileName,PixelArray,NativeData
     */
    @Test
    void loadPictures() throws IOException, InterruptedException {
        if (!profile.equals("dev")) return;
        hBaseService.createTable(Pictures.TABLE_NAME,
                new String[]{Pictures.COLUMNFAMILY_FILENAME,
                        Pictures.COLUMNFAMILY_PIXELARRAY,
                        Pictures.COLUMNFAMILY_NATIVEDATA,
                        Pictures.COLUMNFAMILY_INFO,});
        File dir = new File("/home/xian/大数据技术/大数据课设/课设题目二数据/课设题目二数据/bossbase图片库");
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    boolean flag = true;
                    int tries = 0;
                    while (flag) {
                        try {
                            BMPPicture picture = new BMPPicture(file);
                            String col1 = file.getName();//图片名称
                            int[] col2 = picture.transferPixels(); //直方图数据
                            FileImageInputStream input = new FileImageInputStream(file);
                            ByteArrayOutputStream output = new ByteArrayOutputStream();
                            byte[] buf = new byte[1024];
                            int read;
                            while ((read = input.read(buf)) != -1) {
                                output.write(buf, 0, read);
                            }
                            byte[] col3 = output.toByteArray();//图片原始数据
                            //MD5生成rowKey
                            String key = DigestUtils.md5DigestAsHex((col1 + System.currentTimeMillis()).getBytes());
                            hBaseService.insertRecords(Pictures.TABLE_NAME, key,
                                    new String[]{Pictures.COLUMNFAMILY_FILENAME,
                                            Pictures.COLUMNFAMILY_PIXELARRAY,
                                            Pictures.COLUMNFAMILY_NATIVEDATA,
                                            Pictures.COLUMNFAMILY_INFO + ":" + Pictures.COLUMNQUALIFIER_INFO_WIDTH,
                                            Pictures.COLUMNFAMILY_INFO + ":" + Pictures.COLUMNQUALIFIER_INFO_HEIGHT,
                                            Pictures.COLUMNFAMILY_INFO + ":" + Pictures.COLUMNQUALIFIER_INFO_BITCOUNT,
                                            Pictures.COLUMNFAMILY_INFO + ":" + Pictures.COLUMNQUALIFIER_INFO_OFFBITS,},
                                    Arrays.asList(Bytes.toBytes(col1),
                                            MyBytesUtil.ints2bytes(col2, true),
                                            col3,
                                            Bytes.toBytes(picture.getBiWidth()),
                                            Bytes.toBytes(picture.getBiHeight()),
                                            Bytes.toBytes(picture.getBiBitCount()),
                                            Bytes.toBytes(picture.getBfOffBits())));
                            flag = false;
                            input.close();
                        } catch (Exception e) {
                            //解决集群transition的问题
                            System.out.println(String.format("Sleeping... tries = %d", tries));
                            if (tries < 10) {
                                Thread.sleep(2000);
                                tries++;
                                flag = true;
                            } else {
                                flag = false;
                            }
                        }
                    }
                }
            }
        }
    }

    @Resource
    private SearchService searchService;

    @Test
    public void test() throws IOException {
//        if (!profile.equals("dev")) return;
//        BMPPicture picture = new BMPPicture(
//                new File("/home/xian/大数据技术/大数据课设/课设题目二数据/课设题目二数据/图像篡改检查样例/样例二.bmp"));
//        System.out.println(searchService.checkTampered(picture));
        String format = new DecimalFormat("#.##%").
                format(25536.0 / 512 / 512);
        System.out.println(format);
    }
}
