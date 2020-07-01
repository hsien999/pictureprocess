package cn.jmu.pictureprocess.util;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.*;

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
        bfSize = MyBytesUtil.bytes2int(fileHead, 2, false);
        bfOffBits = MyBytesUtil.bytes2int(fileHead, 10, false);
        biSize = MyBytesUtil.bytes2int(infoHead, 0, false);
        biWidth = MyBytesUtil.bytes2int(infoHead, 4, false);
        biHeight = MyBytesUtil.bytes2int(infoHead, 8, false);
        biPlanes = MyBytesUtil.bytes2short(infoHead, 12, false);
        biBitCount = MyBytesUtil.bytes2short(infoHead, 14, false);
        biCompression = MyBytesUtil.bytes2int(infoHead, 16, false);
        biSizeImage = MyBytesUtil.bytes2int(infoHead, 20, false);
        biXPixelsPerMeter = MyBytesUtil.bytes2int(infoHead, 24, false);
        biYPixelsPerMeter = MyBytesUtil.bytes2int(infoHead, 28, false);
        biColorUsed = MyBytesUtil.bytes2int(infoHead, 32, false);
        biColorImportant = MyBytesUtil.bytes2int(infoHead, 36, false);
    }

    private void initData(BufferedInputStream is) throws IOException {
        int width = biWidth;
        boolean reserve = (biHeight < 0);
        //高度为负数时翻转(自下而上=>自上而下)
        int height = reserve ? -biHeight : biHeight;
        if (biBitCount != 8 && biBitCount != 24 && biBitCount != 32 || width * height > 1e9)
            throw new IOException("only resolve 8 or 24 bit or 32 bit bmp and pixels should be  less than 1e9");
        int skip_width = 0;
        if (biBitCount == 24 || biBitCount == 32) {
            red = new byte[height][width];
            green = new byte[height][width];
            blue = new byte[height][width];
            if (biBitCount == 32) alpha = new byte[height][width];
            else {
                int m = width * 3 % 4;
                if (m > 0) skip_width = 4 - m;
            }
        } else {
            if (is.skip(bfOffBits - 54) == 0) throw new IOException();
            alpha = new byte[height][width];
        }
        for (int i = reserve ? 0 : (height - 1); reserve ? (i < height) : (i >= 0); i = reserve ? i + 1 : i - 1) {
            for (int j = 0; j < width; j++) {
                if (biBitCount == 24 || biBitCount == 32) {
                    if (biBitCount == 32) alpha[i][j] = (byte) is.read();
                    blue[i][j] = (byte) is.read();
                    green[i][j] = (byte) is.read();
                    red[i][j] = (byte) is.read();
                    if (j == width - 1) is.skip(skip_width);
                } else {
                    alpha[i][j] = (byte) is.read();
                }
            }
        }
    }

    public static ByteArrayOutputStream transfer8bTo24b(BMPPicture inPic) throws IOException {
        if (inPic.getBiBitCount() != 8) throw new IllegalStateException();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write((byte) inPic.getBfType().charAt(0));
        output.write((byte) inPic.getBfType().charAt(1));
        int size = inPic.getBfSize() - (inPic.getBfOffBits() - 54) + inPic.getBiWidth() * inPic.getBiHeight() * 2;
        output.write(MyBytesUtil.int2bytes(size, false));
        output.write(MyBytesUtil.int2bytes(0, false));
        output.write(MyBytesUtil.int2bytes(54, false));
        output.write(MyBytesUtil.int2bytes(inPic.getBiSize(), false));
        output.write(MyBytesUtil.int2bytes(inPic.getBiWidth(), false));
        output.write(MyBytesUtil.int2bytes(inPic.getBiHeight(), false));
        output.write(MyBytesUtil.short2bytes(inPic.getBiPlanes(), false));
        output.write(MyBytesUtil.short2bytes((short) 24, false));
        output.write(MyBytesUtil.int2bytes(0, false));
        output.write(MyBytesUtil.int2bytes(size - 54, false));
        output.write(MyBytesUtil.int2bytes(0, false));
        output.write(MyBytesUtil.int2bytes(0, false));
        output.write(MyBytesUtil.int2bytes(0, false));
        output.write(MyBytesUtil.int2bytes(0, false));
        int width = inPic.getBiWidth();
        boolean reserve = (inPic.getBiHeight() < 0);
        int height = reserve ? -inPic.getBiHeight() : inPic.getBiHeight();
        int skip_width = 0;
        int m = width * 3 % 4;
        if (m > 0) skip_width = 4 - m;
        byte[][] alpha = inPic.getAlpha();
        byte[] offset = new byte[4];
        for (int i = reserve ? 0 : (height - 1); reserve ? (i < height) : (i >= 0); i = reserve ? i + 1 : i - 1) {
            for (int j = 0; j < width; j++) {
                output.write(alpha[i][j]);
                output.write(alpha[i][j]);
                output.write(alpha[i][j]);
                if (j == width - 1) output.write(offset, 0, skip_width);
            }
        }
        return output;
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

