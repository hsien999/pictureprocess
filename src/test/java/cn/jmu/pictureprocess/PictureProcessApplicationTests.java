package cn.jmu.pictureprocess;

import cn.jmu.pictureprocess.entity.Pictures;
import cn.jmu.pictureprocess.hbase.service.HBaseService;
import cn.jmu.pictureprocess.service.PicturesService;
import cn.jmu.pictureprocess.util.BMPPicture;
import cn.jmu.pictureprocess.util.MyBytesUtil;
import org.apache.commons.collections.ListUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.protobuf.ProtobufUtil;
import org.apache.hadoop.hbase.protobuf.generated.ClientProtos;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.mllib.linalg.distributed.MatrixEntry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;
import scala.Tuple2;

import javax.annotation.Resource;
import javax.imageio.stream.FileImageInputStream;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.util.*;

@SpringBootTest
class PictureProcessApplicationTests {

    @Test
    void testEncode() {
        /*
        //获取系统默认编码
        System.out.println("系统默认编码:" + System.getProperty("file.encoding"));//查询结果GBK
        //系统默认字符编码
        System.out.println("系统默认字符编码:" + Charset.defaultCharset()); //查询结果GBK
        //操作系统用户使用的语言
        System.out.println("系统默认语言:" + System.getProperty("user.language")); //查询结果zh
        */
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
        hBaseService.deleteTable(Pictures.TABLE_NAME);
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

    @Resource(name = "hBaseConfiguration")
    private Configuration hBaseConfig;
    @Resource
    private PicturesService picturesService;

    @Test
    public void testSpark() throws IOException {
        List<Integer> byteList = picturesService.getByFileName("522.bmp").get(0).getPixelArray();
        SparkConf sparkconf = new SparkConf().
                setAppName(this.getClass().getName()).
                setMaster("local[*]");
        JavaSparkContext sparkContext = new JavaSparkContext(sparkconf);
        final Scan scan = new Scan();
        scan.addFamily(Bytes.toBytes(Pictures.COLUMNFAMILY_FILENAME));
        scan.addFamily(Bytes.toBytes(Pictures.COLUMNFAMILY_PIXELARRAY));
        try {
            ClientProtos.Scan proto = ProtobufUtil.toScan(scan);
            hBaseConfig.set(TableInputFormat.INPUT_TABLE, Pictures.TABLE_NAME);
            String scanToString = DatatypeConverter.printBase64Binary(proto.toByteArray());
            hBaseConfig.set(TableInputFormat.SCAN, scanToString);
        } catch (IOException e) {
            e.printStackTrace();
        }

        JavaPairRDD<ImmutableBytesWritable, Result> rdd = sparkContext.newAPIHadoopRDD(
                hBaseConfig, TableInputFormat.class,
                ImmutableBytesWritable.class, Result.class);
        JavaPairRDD<String, List<Integer>> mapFilterRdd = rdd.mapToPair((PairFunction<Tuple2<ImmutableBytesWritable, Result>, String, List<Integer>>) resultTuple2 -> {
            byte[] o1 = resultTuple2._2.getValue(Bytes.toBytes(Pictures.COLUMNFAMILY_FILENAME), Bytes.toBytes(""));
            byte[] o2 = resultTuple2._2.getValue(Bytes.toBytes(Pictures.COLUMNFAMILY_PIXELARRAY), Bytes.toBytes(""));
            return new Tuple2<>(new String(o1), MyBytesUtil.bytes2IntegerList(o2, true));
        }).filter((Function<Tuple2<String, List<Integer>>, Boolean>) resultTuple2 -> (ListUtils.isEqualList(resultTuple2._2, byteList)));
        List<Tuple2<String, List<Integer>>> collect = mapFilterRdd.collect();
        System.out.println(collect.get(0)._2);
    }


    @Test
    public void testSpark2() throws IOException {

    }
}
