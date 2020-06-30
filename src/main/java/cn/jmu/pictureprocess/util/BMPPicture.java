package cn.jmu.pictureprocess.util;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Getter
@ToString
@EqualsAndHashCode
public class BMPPicture {
    private String bfType;
    private int bfSize;
    private int bfOffBits; //像素数据到文件头的字节偏移
    private int biSize;
    private int biWidth; //宽度(单位:像素)
    private int biHeight; //高度(单位:像素)
    private short biPlanes;
    private short biBitCount; //像素/比特数
    private int biCompression;
    private int biSizeImage; //图像大小(单位:字节)
    private int biXPixelsPerMeter;
    private int biYPixelsPerMeter;
    private int biColorUsed;
    private int biColorImportant;
    @ToString.Exclude
    private final byte[] fileHead = new byte[14];
    @ToString.Exclude
    private final byte[] infoHead = new byte[40];
    @ToString.Exclude
    private byte[][] red;
    @ToString.Exclude
    private byte[][] green;
    @ToString.Exclude
    private byte[][] blue;
    @ToString.Exclude
    private byte[][] alpha;


    public BMPPicture(File file) throws IOException {
        this(new BufferedInputStream(new FileInputStream(file)));
    }

    public BMPPicture(BufferedInputStream is) throws IOException {
        if (is.available() < 54) throw new IllegalArgumentException("this stream is not available");
        if (is.read(fileHead) == -1 || is.read(infoHead) == -1) {
            throw new IOException();
        }
        initHead();
        initData(is);
        is.close();
    }


    public int[] transferPixels() {
        int[] pixels = new int[256];
        for (byte[] bytes : alpha) {
            for (byte aByte : bytes) {
                pixels[aByte & 0xff]++;
            }
        }
        return pixels;
    }

    private void initHead() {
        bfType = String.valueOf(new char[]{(char) fileHead[0], (char) fileHead[1]});
        bfSize = byte2Int(fileHead, 4, 5);
        bfOffBits = byte2Int(fileHead, 4, 13);
        biSize = byte2Int(infoHead, 4, 3);
        biWidth = byte2Int(infoHead, 4, 7);
        biHeight = byte2Int(infoHead, 4, 11);
        biPlanes = (short) byte2Int(infoHead, 2, 13);
        biBitCount = (short) byte2Int(infoHead, 2, 15);
        biCompression = byte2Int(infoHead, 4, 19);
        biSizeImage = byte2Int(infoHead, 4, 23);
        biXPixelsPerMeter = byte2Int(infoHead, 4, 27);
        biYPixelsPerMeter = byte2Int(infoHead, 4, 31);
        biColorUsed = byte2Int(infoHead, 4, 35);
        biColorImportant = byte2Int(infoHead, 4, 39);
    }

    private void initData(BufferedInputStream is) throws IOException {
        int width = biWidth;
        boolean reserve = (biHeight < 0);
        //高度为负数时翻转(自下而上=>自上而下)
        int height = reserve ? -biHeight : biHeight;
        if (biBitCount != 8 && biBitCount != 24 || width * height > 1e9)
            throw new IOException("only resolve 8 or 24 bit bmp and pixels are less than 1e9");
        int skip_width = 0;
        if (biBitCount == 24) {
            red = new byte[height][width];
            green = new byte[height][width];
            blue = new byte[height][width];
            int m = biWidth * 3 % 4;
            if (m > 0) skip_width = 4 - m;
        } else {
            if (is.skip(bfOffBits - 54) == 0) throw new IOException();
            alpha = new byte[height][width];
        }
        for (int i = reserve ? 0 : (height - 1); reserve ? (i < height) : (i >= 0); i = reserve ? i + 1 : i - 1) {
            for (int j = 0; j < width; j++) {
                if (biBitCount == 24) {
                    blue[i][j] = (byte) is.read();
                    green[i][j] = (byte) is.read();
                    red[i][j] = (byte) is.read();
                    if (j == 0) {
                        if (is.skip(skip_width) == 0) throw new IOException();
                    }
                } else {
                    alpha[i][j] = (byte) is.read();
                }
            }
        }
    }

    /**
     * 实现将skip个字节数据逆向转化为int数据的方法
     * 适用小端机器的存储数据的特性
     *
     * @param bytes 待转化的字节数组
     * @param skip  转化的字节数
     * @param start 起始字节
     * @return 对应的int数值
     */
    private int byte2Int(byte[] bytes, int skip, int start) throws IllegalArgumentException {
        if (skip <= 0 || skip > 4 || start < skip - 1 || start > bytes.length - 1) throw new IllegalArgumentException();
        int toInt = 0;
        for (int i = 0; i < skip; i++) {
            toInt = toInt | (bytes[start - i] & 0xff) << (skip - i - 1) * 8;
        }
        return toInt;
    }
}

/*
 * Test on Windows GUI
 */
/*class Test extends JFrame {
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                BufferedInputStream is = new BufferedInputStream(new FileInputStream("/home/xian/大数据技术/大数据课设/课设题目二数据/课设题目二数据/bossbase图片库/1.bmp"));
                BMPPicture picture = new BMPPicture(is);
                System.out.println(picture);
                Test frame = new Test(picture);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    private final BMPPicture picture;

    public Test(BMPPicture pic) {
        this.picture = pic;
        this.setTitle("BMP解析");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(screenSize.width / 5, screenSize.height / 5, 600, 600);
        this.setResizable(true);
        this.setLocationRelativeTo(null);
        MyPanel panel = new MyPanel();
        panel.setPreferredSize(new Dimension(getWidth() / 2, getHeight() / 2));
        this.add(panel);
        this.setVisible(true);
    }

    public class MyPanel extends JPanel {

        public void paint(Graphics g) {
            super.paint(g);
            byte[][] color = picture.getAlpha();
            for (int i = 0; i < Math.abs(picture.getBiHeight()); i++) {
                for (int j = 0; j < picture.getBiWidth(); j++) {
                    g.setColor(new Color(color[i][j], color[i][j], color[i][j]));
                    // g.fillOval(j, i, 1, 1);
                    g.fillRect(j, i, 1, 1);
                }
            }
        }
    }
}*/

