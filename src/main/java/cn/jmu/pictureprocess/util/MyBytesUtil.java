package cn.jmu.pictureprocess.util;

import org.apache.kerby.kerberos.kerb.crypto.util.BytesUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 拓展BytesUtil的自定义Bytes工具类
 */
public class MyBytesUtil extends BytesUtil {

    /**
     * 整型数组转字节数组
     *
     * @param ints      整型数组
     * @param bigEndian 存储方式:大端/小端
     * @return 转换后的字节数组
     */
    public static byte[] ints2bytes(int[] ints, boolean bigEndian) {
        byte[] bytes = new byte[ints.length * 4];
        for (int i = 0; i < ints.length; i++) {
            int2bytes(ints[i], bytes, i * 4, bigEndian);
        }
        return bytes;
    }

    /**
     * 字节数组转整型数组
     *
     * @param bytes     字节数组
     * @param bigEndian 存储方式:大端/小端
     * @return 整型数组
     */
    public static int[] bytes2ints(byte[] bytes, boolean bigEndian) {
        if (bytes.length % 4 != 0) throw new IllegalArgumentException();
        int[] ints = new int[bytes.length / 4];
        for (int i = 0; i < bytes.length; i += 4) {
            ints[i / 4] = bytes2int(bytes, i, bigEndian);
        }
        return ints;
    }

    /**
     * 字节数组转整型列表
     *
     * @param bytes     字节数组
     * @param bigEndian 存储方式:大端/小端
     * @return 整型列表
     */
    public static List<Integer> bytes2IntegerList(byte[] bytes, boolean bigEndian) {
        return Arrays.stream(bytes2ints(bytes, bigEndian)).boxed().collect(Collectors.toList());
    }

    /**
     * 字节数组转字节列表
     *
     * @param bytes 字节数组
     * @return 字节列表
     */
    public static List<Byte> bytes2ByteList(byte[] bytes) {
        List<Byte> byteList = new ArrayList<>(bytes.length);
        for (byte b : bytes) {
            byteList.add(b);
        }
        return byteList;
    }
}
