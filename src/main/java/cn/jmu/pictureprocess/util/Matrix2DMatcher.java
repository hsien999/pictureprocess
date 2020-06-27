package cn.jmu.pictureprocess.util;

import java.math.BigInteger;

public class Matrix2DMatcher {
    public static void main(String[] args) {
        int[][] text = {
                {'a', 'b', 'a', 'b', 'a'},
                {'a', 'b', 'a', 'b', 'a'},
                {'a', 'b', 'b', 'a', 'a'},
                {'a', 'b', 'a', 'a', 'b'},
                {'b', 'b', 'a', 'b', 'a'}
        };
        int[][] pattern = {
                {'a', 'b'},
                {'b', 'a'}
        };

        matrixPatternMatch(text, pattern);
    }

    private static final BigInteger BASE = BigInteger.valueOf(256);

    private static void matrixPatternMatch(int[][] text, int[][] pattern) {
        BigInteger[] patternStamp = new BigInteger[pattern[0].length];
        BigInteger[] textStamp = new BigInteger[text[0].length];

        calculateStamp(pattern, pattern.length, patternStamp);
        calculateStamp(text, pattern.length, textStamp);

        int[] next = new int[patternStamp.length];
        calculateNext(patternStamp, next);

        for (int i = 0; i < (text.length - pattern.length + 1); i++) {
            int col = isMatch(patternStamp, textStamp, next);
            if (col != -1) {
                System.out.println("found");
                System.out.println(i + ", " + col);
            }
            if (i < text.length - pattern.length)
                calculateNextStamp(text, pattern.length, textStamp, i);
        }

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

    private static void calculateNextStamp(int[][] text, int height,
                                           BigInteger[] textStamp, int row) {
        BigInteger d = BASE.pow(height - 1);
        for (int i = 0; i < textStamp.length; i++) {
            textStamp[i] = BASE.multiply(textStamp[i].
                    subtract(d.multiply(BigInteger.valueOf(text[row][i])))).
                    add(BigInteger.valueOf(text[row + height][i]));
        }
    }

    private static void calculateStamp(int[][] input, int height, BigInteger[] result) {
        for (int i = 0; i < result.length; i++) {
            result[i] = BigInteger.valueOf(0);
            for (int j = 0; j < height; j++) {
                result[i] = BASE.multiply(result[i]).add(BigInteger.valueOf(input[j][i]));
            }
        }
    }
}

