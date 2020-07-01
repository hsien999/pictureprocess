package cn.jmu.pictureprocess.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.hadoop.hbase.exceptions.IllegalArgumentIOException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * 基于BFS算法的二维矩阵篡改匹配
 */
public class MatrixBFS {

    private static final int[] nextX = {0, 0, 1, -1, -1, -1, 1, 1};
    private static final int[] nextY = {1, -1, 0, 0, -1, 1, -1, 1};

    private final boolean[][] visit;
    private final Point nowUL, nowLR;

    private final byte[][] matrix;
    private final int line, col;
    private final int stepX, stepY;

    public MatrixBFS(byte[][] matrix, int stepX, int stepY) throws IOException {
        if (matrix.length < 1)
            throw new IllegalArgumentIOException();
        this.matrix = matrix;
        this.line = matrix.length;
        this.col = matrix[0].length;
        this.stepX = stepX;
        this.stepY = stepY;
        this.visit = new boolean[col][line];
        this.nowUL = new Point();
        this.nowLR = new Point();
    }

    public List<Point> process() {
        List<Point> points = new ArrayList<>();
        for (int i = 0; i < line; i++) {
            for (int j = 0; j < col; j++) {
                if (matrix[i][j] == 0 && !visit[i][j]) {
                    nowUL.setX(line - 1);
                    nowUL.setY(col - 1);
                    nowLR.setX(0);
                    nowLR.setY(0);
                    bfs(i, j);
                    points.add(new Point(nowUL.getX() * stepX, nowUL.getY() * stepY));
                    int lastX = (nowLR.getX() + 1) * stepX, lastY = (nowLR.getY() + 1) * stepY;
                    if (lastX + stepX > (line * stepX)) lastX += stepX;
                    if (lastY + stepY > (col * stepY)) lastY += stepY;
                    points.add(new Point(lastX, lastY));
                }
            }
        }
        return points;
    }

    private void bfs(int stX, int stY) {
        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(stX, stY));
        visit[stX][stY] = true;
        while (!queue.isEmpty()) {
            Point now = queue.poll();
            update(now);
            for (int i = 0; i < nextX.length; i++) {
                int newX = now.getX() + nextX[i];
                int newY = now.getY() + nextY[i];
                if (judge(newX, newY)) {
                    queue.add(new Point(newX, newY));
                    visit[newX][newY] = true;
                }
            }
        }
    }


    private void update(Point p) {
        int x = p.getX();
        int y = p.getY();
        if (x < nowUL.getX()) nowUL.setX(x);
        if (x > nowLR.getX()) nowLR.setX(x);
        if (y < nowUL.getY()) nowUL.setY(y);
        if (y > nowLR.getY()) nowLR.setY(y);
    }

    private boolean judge(int x, int y) {
        if (x >= line || x < 0 || y >= col || y < 0) return false;
        return !visit[x][y] && matrix[x][y] == 0;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Point {
        int x;
        int y;
    }
}
