package cn.jmu.pictureprocess;

import cn.jmu.pictureprocess.entity.Pictures;
import cn.jmu.pictureprocess.hbase.service.HBaseService;
import cn.jmu.pictureprocess.service.PicturesService;
import cn.jmu.pictureprocess.spark.service.SearchService;
import cn.jmu.pictureprocess.util.BMPPicture;
import cn.jmu.pictureprocess.util.MyBytesUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

@SpringBootTest
class PictureProcessApplicationTests implements Serializable {


    @Value("spring.profiles.active")
    private String profile;

    @Test
    void testEncode() {
        if (!profile.equals("dev")) return;
        System.out.println("系统默认编码:" + System.getProperty("file.encoding"));
        System.out.println("系统默认字符编码:" + Charset.defaultCharset());
        System.out.println("系统默认语言:" + System.getProperty("user.language"));
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
        File dir = new File("/.../data/bossbase图片库");
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
    @Resource
    private PicturesService picturesService;
    @Resource
    private ObjectMapper objectMapper;

    @Test
    public void test() throws IOException {
        if (!profile.equals("dev")) return;
        for (int i = 1; i <= 20; i++) {
            Pictures pictures = picturesService.getByFileName(i * 25 + ".bmp").get(0);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(com.google.common.primitives.Bytes.toArray(pictures.getNativeData()));
            BufferedImage image = ImageIO.read(inputStream);
            Graphics graphics = image.getGraphics();
            Random random = new Random();
            int rgb = random.nextInt(256);
            graphics.setColor(new Color(rgb, rgb, rgb));
            int time = random.nextInt(5) + 3;
            for (int j = 1; j <= time; j++) {
                int x = random.nextInt(512);
                int y = random.nextInt(512);
                int width = random.nextInt(512 - y);
                int height = random.nextInt(512 - x);
                int type = random.nextInt(4);
                char[] chars = "篡改示例".toCharArray();
                switch (type) {
                    case 0:
                        graphics.fillRect(x, y, width, height);
                        break;
                    case 1:
                        graphics.fillOval(x, y, width, height);
                        break;
                    case 2:
                        graphics.drawLine(x, y, x + height, y + width);
                        break;
                    case 3:
                        graphics.drawChars(chars, 0, chars.length, x, y);
                        break;
                }
            }
            ImageIO.write(image, "bmp",
                    new File("/home/xian/大数据技术/大数据课设/课设题目二数据/课设题目二数据/图像篡改检查样例/sample" + i + ".bmp"));
        }
    }
}
