package cn.jmu.pictureprocess.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;

public class Matrix2D {

    private static final BigInteger BASE = BigInteger.valueOf(2);

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

    private static void calculateNextStamp(byte[][] text, int height,
                                           BigInteger[] textStamp, int row) {
        BigInteger d = BASE.pow(height - 1);
        for (int i = 0; i < textStamp.length; i++) {
            textStamp[i] = BASE.multiply(textStamp[i].
                    subtract(d.multiply(BigInteger.valueOf(text[row][i])))).
                    add(BigInteger.valueOf(text[row + height][i]));
        }
    }

    private static void calculateStamp(byte[][] input, int height, BigInteger[] result, int start) {
        for (int i = 0; i < result.length; i++) {
            result[i] = BigInteger.valueOf(0);
            for (int j = start; j < start + height; j++) {
                result[i] = BASE.multiply(result[i]).add(BigInteger.valueOf(input[j][i]));
            }
        }
    }

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

    public static List<Point> splitMatrixPoints(int width, int height, int partition) {
        int split = Math.min(2, partition);
        int stepX = (height - 1) / split;
        int stepY = (width - 1) / split;
        List<Point> points = new ArrayList<>();
        if (stepX < 1 || stepY < 1) return points;
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
        return points;
    }

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


    public static int SplitMatrixMatcher(SplitMatrix splitTextMatrix, final byte[][] patternMatrix) {
        byte[][] textMatrix = splitTextMatrix.getMatrix();
        Point st = splitTextMatrix.getStart();
        Point ed = splitTextMatrix.getEnd();
        int count = 0;
        for (int i = st.getX(); i <= ed.getX(); i++) {
            for (int j = st.getY(); j <= ed.getY(); j++) {
                if (patternMatrix[i][j] == textMatrix[i][j]) count++;
            }
        }
        return count;
    }


    @AllArgsConstructor
    @ToString
    @Getter
    public static class SplitMatrix implements Serializable {
        private final byte[][] matrix;
        private final Point start;
        private final Point end;
    }

    @AllArgsConstructor
    @ToString
    @Getter
    public static class Point implements Serializable {
        private final int x;
        private final int y;
    }
}



