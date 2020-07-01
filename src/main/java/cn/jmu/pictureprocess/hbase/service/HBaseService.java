package cn.jmu.pictureprocess.hbase.service;

import cn.jmu.pictureprocess.hbase.pojo.HBaseCell;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.filter.FilterList;

import java.io.IOException;
import java.util.List;

public interface HBaseService {
    void createNameSpace(String namespace) throws IOException;

    void createTable(String tableName, String[] columnFamily) throws IOException;

    void createTable(String tableName, int regionCount, String[] columnFamily) throws IOException;

    void deleteTable(String name) throws IOException;

    void insertRecords(String tableName, String rowKey, String[] fields, List<byte[]> values) throws IOException;

    void delete(String tableName, Delete delete) throws IOException;

    List<HBaseCell> getRow(String tableName, String rowKey) throws IOException;

    List<HBaseCell> getCell(String tableName, String rowKey, String columnFamily, String column) throws IOException;

    List<HBaseCell> scan(Table table, Scan scan) throws IOException;

    List<HBaseCell> scanByColumn(String tableName, String field) throws IOException;

    List<HBaseCell> scanByPage(String tableName, String startRowKey, String endRowKey, int nums) throws IOException;

    List<HBaseCell> scanByFilter(String tableName, FilterList filter) throws IOException;

    boolean tableExists(String tableName) throws IOException;


}
