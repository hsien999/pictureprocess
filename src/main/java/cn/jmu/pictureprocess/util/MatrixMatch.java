package cn.jmu.pictureprocess.util;

import lombok.*;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;

/**
 * 二维矩阵局部与全局匹配工具类
 */
public class MatrixMatch {

    /*理论上应是像素的个数即256,否则需要在匹配到相同hash值时再比对*/
    private static final BigInteger BASE = BigInteger.valueOf(2);

    /**
     * 二维矩阵模式匹配(基于Rabin-Karp+KMP算法的二维推广)
     *
     * @param splitMatrix 分割完成后的文本矩阵
     * @param pattern     模式矩阵
     * @return 局部特征起点坐标(左上角)
     * @throws IOException IO异常
     */
    public static List<Point> matrixPatternMatch(final SplitMatrix splitMatrix, final byte[][] pattern) throws IOException {
        byte[][] text = splitMatrix.getMatrix();
        int start = splitMatrix.getStart().getX();
        int end = splitMatrix.getEnd().getX();
        if (text == null || pattern == null || pattern.length == 0 || text.length < pattern.length)
            throw new IOException();
        BigInteger[] patternStamp = new BigInteger[pattern[0].length];
        BigInteger[] textStamp = new BigInteger[text[0].length];

        calculateStamp(pattern, pattern.length, patternStamp, 0);
        calculateStamp(text, pattern.length, textStamp, start);

        int[] next = new int[patternStamp.length];
        calculateNext(patternStamp, next);

        List<Point> points = new ArrayList<>();
        for (int i = start; i < (end - pattern.length + 1); i++) {
            int col = isMatch(patternStamp, textStamp, next);
            if (col != -1) {
                points.add(new Point(i, col));
            }
            if (i < end - pattern.length) calculateNextStamp(text, pattern.length, textStamp, i);
        }

        return points;
    }

    /**
     * 一维上的KMP匹配
     *
     * @param patternStamp 模式矩阵对应的一维HASH数组
     * @param textStamp    文本矩阵对应的一维HASH数组
     * @param next         文本矩阵的一维HASH数组对应的next数组
     * @return 匹配到的横轴坐标值
     */
    private static int isMatch(BigInteger[] patternStamp, BigInteger[] textStamp, int[] next) {
        int i = 0, j = 0;
        while (j < patternStamp.length && i < textStamp.length) {
            if (j == -1 || patternStamp[j].equals(textStamp[i])) {
                i++;
                j++;
            } else {
                j = next[j];
            }
        }
        if (j == patternStamp.length) {
            return i - j;
        } else {
            return -1;
        }
    }

    /**
     * 计算模式串next
     *
     * @param pattern 模式数组
     * @param next    next数组
     */
    private static void calculateNext(BigInteger[] pattern, int[] next) {
        next[0] = -1;
        int i = 0, j = -1;
        while (i < pattern.length - 1) {
            if (j == -1 || pattern[i].equals(pattern[j])) {
                i++;
                j++;
                next[i] = j;
            } else {
                j = next[j];
            }
        }

    }

    /**
     * 移动下一行,计算一维hash数组
     *
     * @param text      文本矩阵
     * @param height    模式矩阵的高度
     * @param textStamp 文本矩阵的hash数组
     * @param row       上一行
     */
    private static void calculateNextStamp(byte[][] text, int height,
                                           BigInteger[] textStamp, int row) {
        BigInteger d = BASE.pow(height - 1);
        for (int i = 0; i < textStamp.length; i++) {
            textStamp[i] = BASE.multiply(textStamp[i].
                    subtract(d.multiply(BigInteger.valueOf(text[row][i])))).
                    add(BigInteger.valueOf(text[row + height][i]));
        }
    }

    /**
     * 计算初始一维hash数组
     *
     * @param input  二维矩阵
     * @param height 模式矩阵的高度
     * @param result 一维hash数组
     * @param start  初始行
     */
    private static void calculateStamp(byte[][] input, int height, BigInteger[] result, int start) {
        for (int i = 0; i < result.length; i++) {
            result[i] = BigInteger.valueOf(0);
            for (int j = start; j < start + height; j++) {
                result[i] = BASE.multiply(result[i]).add(BigInteger.valueOf(input[j][i]));
            }
        }
    }

    /**
     * 水平划分文本矩阵
     * 用于局部搜索
     *
     * @param text      文本矩阵
     * @param height    模式矩阵的高度
     * @param partition 分区数
     * @return 划分生成的点集
     */
    public static List<Point> spitTextMatrix(byte[][] text, int height, int partition) {
        List<Point> spitResult = new ArrayList<>();
        int width = text[0].length;
        int maxHeight = text.length;
        if (height * 3 > maxHeight) {
            spitResult.add(new Point(0, 0));
        } else {
            int split = Math.min(maxHeight / (6 * height), partition);
            if (split <= 1) split = 2;
            int step = maxHeight / split;
            int st, ed;
            for (int i = 0; i < split; i++) {
                st = i * step;
                ed = Math.min(st + step, maxHeight);
                st -= height - 1;
                if (st <= 0) st = 0;
                spitResult.add(new Point(st, 0));
                spitResult.add(new Point(ed, 0));
            }
        }
        return spitResult;
    }

    /**
     * 水平垂直划分文本矩阵
     * 用于全局搜索
     *
     * @param width     文本矩阵宽度
     * @param height    文本矩阵高度
     * @param partition 分区数
     * @return 划分生成的点集
     */
    public static SplitResult splitMatrixPoints(int width, int height, int partition) {
        int split = Math.min((int) Math.sqrt(width), partition * 3);
        int stepX = (height - 1) / split;
        int stepY = (width - 1) / split;
        List<Point> points = new ArrayList<>();
        SplitResult result = new SplitResult();
        result.setPointList(points);
        result.setWidth(split);
        result.setHeight(split);
        if (stepX < 1 || stepY < 1) return result;
        for (int i = 0; i < split; i++) {
            for (int j = 0; j < split; j++) {
                int stX = i * stepX, stY = j * stepY;
                int edX = stX + stepX - 1, edY = stY + stepY - 1;
                if (edX + stepX >= height) edX = height - 1;
                if (edY + stepY >= width) edY = width - 1;
                points.add(new Point(stX, stY));
                points.add(new Point(edX, edY));
            }
        }
        return result;
    }

    /**
     * 提取一维矩阵中要匹配的文本矩阵
     *
     * @param bytes  一维byte数组
     * @param offset 偏移量
     * @param width  矩阵宽度
     * @param height 矩阵高度
     * @return 文本矩阵
     */
    public static byte[][] transfer(final byte[] bytes, int offset, int width, int height) {
        boolean reserve = (height < 0);
        //高度为负数时翻转(自下而上=>自上而下)
        int _height = reserve ? -height : height;
        byte[][] alpha = new byte[width][_height];
        int cnt = offset;
        for (int i = reserve ? 0 : (_height - 1); reserve ? (i < _height) : (i >= 0); i = reserve ? i + 1 : i - 1) {
            for (int j = 0; j < width; j++) {
                alpha[i][j] = bytes[cnt++];
            }
        }
        return alpha;
    }


    /**
     * 分区匹配
     *
     * @param splitTextMatrix 划分后的文本矩阵
     * @param patternMatrix   模式矩阵
     * @return 匹配结果
     */
    private static final double THREAD = 99.5;

    public static SimilarResult SplitMatrixMatcher(SplitMatrix splitTextMatrix, final byte[][] patternMatrix) {
        byte[][] textMatrix = splitTextMatrix.getMatrix();
        Point st = splitTextMatrix.getStart();
        Point ed = splitTextMatrix.getEnd();
        int count = 0;
        for (int i = st.getX(); i <= ed.getX(); i++) {
            for (int j = st.getY(); j <= ed.getY(); j++) {
                if (patternMatrix[i][j] == textMatrix[i][j]) count++;
            }
        }
        boolean isSimilar = Double.compare(count * 100.0 / ((ed.getX() - st.getX() + 1) * (ed.getY() - st.getY() + 1)), THREAD) > 0;
//        boolean isSimilar = (count==((ed.getX() - st.getX()) * (ed.getY() - st.getY())));
        TreeMap<Point, Boolean> map = new TreeMap<>();
        map.put(st, isSimilar);
        return new SimilarResult(map, count);
    }

    /**
     * 篡改匹配的中间结果
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SimilarResult implements Serializable, Comparable<SimilarResult> {
        private TreeMap<Point, Boolean> isSimilar;
        private int similarity;

        @Override
        public int compareTo(SimilarResult o) {
            if (this == o) return 0;
            if (o == null) return -1;
            return Integer.compare(this.similarity, o.getSimilarity());
        }
    }

    /**
     * 划分结果
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SplitResult implements Serializable {
        private int width;
        private int height;
        private List<Point> pointList;
    }

    /**
     * 文本矩阵划分的中间结果
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SplitMatrix implements Serializable {
        private byte[][] matrix;
        private Point start;
        private Point end;
    }

    /**
     * 二维坐标上的点
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Point implements Serializable, Comparable<Point> {
        private int x;
        private int y;

        @Override
        public int compareTo(Point o) {
            if (this == o) return 0;
            if (o == null) return -1;
            if (x != o.getX()) return Integer.compare(x, o.getX());
            return Integer.compare(y, o.getY());
        }
    }
}



