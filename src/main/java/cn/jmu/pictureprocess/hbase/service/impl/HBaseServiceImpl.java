package cn.jmu.pictureprocess.hbase.service.impl;

import cn.jmu.pictureprocess.entity.Pictures;
import cn.jmu.pictureprocess.hbase.pojo.HBaseCell;
import cn.jmu.pictureprocess.hbase.service.HBaseService;
import lombok.Getter;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;

@Service("hBaseService")
@Getter
public class HBaseServiceImpl implements HBaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HBaseServiceImpl.class);

    @Resource(name = "hBaseConnection")
    private Connection connection;
    @Resource(name = "hBaseAdmin")
    private Admin admin;

    /**
     * 初始化命名空间：若命名空间存在则不创建
     *
     * @param namespace 命名空间
     */
    @Override
    public void createNameSpace(String namespace) throws IOException {
        try {
            admin.getNamespaceDescriptor(namespace);
            LOGGER.error("NameSpace {} already exists", namespace);
        } catch (NamespaceNotFoundException e) {
            admin.createNamespace(NamespaceDescriptor.create(namespace).build());
            LOGGER.info("Created namespace: {}", namespace);
        }
    }

    /**
     * 创建表：表存在时，先删除再创建；
     * 分区数默认为0
     *
     * @param tableName    表名
     * @param columnFamily 列族
     * @throws IOException io操作
     */
    @Override
    public void createTable(String tableName, String[] columnFamily) throws IOException {
        createTable(tableName, 0, columnFamily);
    }

    /**
     * 删除表：先禁用再删除
     *
     * @param tableName 表名
     * @throws IOException io操作
     */
    @Override
    public void deleteTable(String tableName) throws IOException {
        TableName name = TableName.valueOf(tableName);
        // 不存在
        if (!admin.tableExists(name)) {
            LOGGER.error("Table named {} doesn‘t exists", tableName);
            throw new TableNotFoundException();
        }
        admin.disableTable(name);
        admin.deleteTable(name);
        LOGGER.info("Deleted table: {}", tableName);
    }

    /**
     * 创建表
     *
     * @param tableName    表名
     * @param regionCount  分区数
     * @param columnFamily [列族]
     * @throws IOException io操作
     */
    @Override
    public void createTable(String tableName, int regionCount, String[] columnFamily) throws IOException {
        TableName name = TableName.valueOf(tableName);
        // 存在
        if (admin.tableExists(name)) {
            LOGGER.error("Table named {} already exists", name);
            throw new TableExistsException();
        }
        createTableTemplate(name, regionCount, columnFamily);
    }

    /**
     * 插入数据：多行、多列族 => 多列多值
     *
     * @param tableName 表名
     * @param rowKey    行
     * @param fields    [列族]/[列族：标识符]
     * @param values    值(与fields一一对应)
     */
    @Override
    public void insertRecords(String tableName, String rowKey, String[] fields, List<byte[]> values) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        Put put = new Put(Bytes.toBytes(rowKey));
        for (int i = 0; i < fields.length; i++) {
            String[] split = fields[i].split("(:)");
            byte[] columnFamily = Bytes.toBytes(split[0]);
            byte[] columnQualifier = split.length > 1 ? Bytes.toBytes(split[1]) : new byte[0];
            byte[] val = values.get(i);
            put.addColumn(columnFamily, columnQualifier, val);
            table.put(put);
        }
        LOGGER.info("Insert Records: success");
        table.close();
    }

    /**
     * 插入数据：单行、单列族 => 单列单值
     *
     * @param tableName 表名
     * @param rowKey    行
     * @param field     列族/列族：标识符
     * @param value     列值
     */
    public void insertRecord(String tableName, String rowKey, String field, byte[] value) throws IOException {
        insertRecords(tableName, rowKey, new String[]{field}, Collections.singletonList(value));
    }


    /**
     * 删除单行数据
     *
     * @param tableName 表名
     * @param rowKey    行名
     */
    public void deleteRow(String tableName, String rowKey) throws IOException {
        Delete delete = new Delete(Bytes.toBytes(rowKey));
        delete(tableName, delete);
    }


    /**
     * 删除单行单列族单列记录
     *
     * @param tableName    表名
     * @param rowKey       行名
     * @param columnFamily 列族
     * @param column       列
     */
    public void deleteColumn(String tableName, String rowKey, String columnFamily, String column) throws IOException {
        Delete delete = new Delete(Bytes.toBytes(rowKey)).addColumn(Bytes.toBytes(columnFamily),
                Bytes.toBytes(column));
        delete(tableName, delete);
    }

    @Override
    public void delete(String tableName, Delete delete) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        table.delete(delete);
        LOGGER.info("Delete: success");
    }

    public void deleteList(String tableName, List<Delete> delete) throws IOException {
        TableName name = TableName.valueOf(tableName);
        Table table = connection.getTable(name);
        table.delete(delete);
    }

    /**
     * 查找一行记录
     *
     * @param tableName 表名
     * @param rowKey    行名
     * @return 结果
     */
    @Override
    public List<HBaseCell> getRow(String tableName, String rowKey) throws IOException {
        Get get = new Get(Bytes.toBytes(rowKey));
        Table table = connection.getTable(TableName.valueOf(tableName));
        List<HBaseCell> list = get(table, get);
        table.close();
        return list;
    }

    /**
     * 查询单行、单列族、单列的值
     *
     * @param tableName    表名
     * @param rowKey       行名
     * @param columnFamily 列族
     * @param column       列名
     * @return 列值
     */
    @Override
    public List<HBaseCell> getCell(String tableName, String rowKey, String columnFamily, String column) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        Get get = new Get(Bytes.toBytes(rowKey));
        get.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(column));
        List<HBaseCell> resultList = get(table, get);
        table.close();
        return resultList;
    }

    public List<HBaseCell> get(Table table, Get get) throws IOException {
        return resultToCellList(table.get(get));
    }

    /**
     * 全表扫描
     *
     * @param tableName 表名
     * @see Scan
     */
    public List<HBaseCell> scanAll(String tableName) throws IOException {
        TableName name = TableName.valueOf(tableName);
        Table table = connection.getTable(name);
        Scan scan = new Scan();
        List<HBaseCell> resultList = scan(table, scan);
        table.close();
        return resultList;
    }

    /**
     * 列扫描
     *
     * @param field 列族/列族：标识符
     */
    @Override
    public List<HBaseCell> scanByColumn(String tableName, String field) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        String[] split = field.split("(:)");
        byte[] columnFamily = Bytes.toBytes(split[0]);
        byte[] columnQualifier = split.length > 1 ? Bytes.toBytes(split[1]) : new byte[0];
        Scan scan = new Scan();
        scan.addColumn(columnFamily, columnQualifier);
        List<HBaseCell> resultList = scan(table, scan);
        table.close();
        return resultList;
    }

    /**
     * 过滤器扫描
     *
     * @param tableName 表名
     * @param filters   过滤器列表
     * @see Filter
     */
    @Override
    public List<HBaseCell> scanByFilter(String tableName, FilterList filters) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        Scan scan = new Scan();
        scan.setFilter(filters);
        List<HBaseCell> resultList = scan(table, scan);
        table.close();
        return resultList;
    }

    /**
     * 分页限制扫描
     *
     * @param tableName   表名
     * @param startRowKey 初始行键
     * @param endRowKey   结束行键
     * @param nums        最大数量
     * @see Filter
     */
    @Override
    public List<HBaseCell> scanByPage(String tableName, String startRowKey, String endRowKey, int nums) throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        Scan scan = new Scan();
        Filter filter = new PageFilter(nums);
        scan.setFilter(filter);
        scan.withStartRow(Bytes.toBytes(startRowKey));
        scan.withStopRow(Bytes.toBytes(endRowKey));
        List<HBaseCell> resultList = scan(table, scan);
        table.close();
        return resultList;
    }

    @Override
    public List<HBaseCell> scan(Table table, Scan scan) throws IOException {
        List<HBaseCell> list = new ArrayList<>();
        try (ResultScanner scanner = table.getScanner(scan)) {
            for (Result result : scanner) {
                list.addAll(resultToCellList(result));
            }
        }
        return list;
    }

    /**
     * 表是否存在
     *
     * @param tableName 表名
     */
    @Override
    public boolean tableExists(String tableName) throws IOException {
        TableName name = TableName.valueOf(tableName);
        return admin.tableExists(name);
    }

    /**
     * 创建表
     *
     * @param tableName    表名
     * @param regionCount  分区数
     * @param columnFamily 列族
     */
    private void createTableTemplate(TableName tableName, int regionCount, String[] columnFamily) {
        try {
            TableDescriptorBuilder tableBuilder = TableDescriptorBuilder.newBuilder(tableName);
            // 增加列族
            tableBuilder.setColumnFamilies(createColumnFamilyList(columnFamily));

            // 无分区（未指定）
            if (regionCount <= 0) {
                admin.createTable(tableBuilder.build());
            } else {
                // 预分区
                byte[][] splitKey = getSplitKeys(regionCount);
                admin.createTable(tableBuilder.build(), splitKey);
            }
            LOGGER.info("Created table named {}", tableName);
        } catch (IOException e) {
            LOGGER.error("Create table error.INFO: " + e.getMessage(), e);
        }
    }

    /**
     * 列族描述器：没有内容时，自定义列族 info
     *
     * @param columnFamily 列族名
     * @return 列族描述器
     */
    private List<ColumnFamilyDescriptor> createColumnFamilyList(String[] columnFamily) {
        List<ColumnFamilyDescriptor> results = new ArrayList<>();
        // 设置默认列族 info
        if (columnFamily == null || columnFamily.length == 0) {
            columnFamily = new String[]{"info"};
        }
        for (String family : columnFamily) {
            ColumnFamilyDescriptorBuilder descriptorBuilder =
                    ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes(family));
            if (family.equals(Pictures.COLUMNFAMILY_NATIVEDATA)) {
                descriptorBuilder.setMobEnabled(true);
                descriptorBuilder.setMobThreshold(102400L);
            }
            results.add(descriptorBuilder.setBlockCacheEnabled(true).build());
        }
        return results;
    }

    /**
     * 生成分区键
     *
     * @param regionCount 分区数
     * @return 多个分区键
     */
    private byte[][] getSplitKeys(int regionCount) throws IllegalArgumentException {
        if (regionCount <= 1) throw new IllegalArgumentException("regionCount should be greater than 1");
        int splitKeyCount = regionCount - 1;
        byte[][] bytes = new byte[splitKeyCount][];

        List<byte[]> byteList = new ArrayList<>();
        for (int i = 0; i < splitKeyCount; i++) {
            String key = i + "|";
            byteList.add(Bytes.toBytes(key));
        }

        byteList.toArray(bytes);
        return bytes;
    }

    /**
     * 处理Result结果
     */
    private List<HBaseCell> resultToCellList(Result result) {
        List<HBaseCell> list = new ArrayList<>(result.size());
        if (result.listCells() == null) return list;
        for (Cell cell : result.listCells()) {
            HBaseCell hBaseCell = new HBaseCell();
            hBaseCell.setRowKey(Bytes.copy(cell.getRowArray(), cell.getRowOffset(), cell.getRowLength()));
            hBaseCell.setCFamily(Bytes.copy(cell.getFamilyArray(), cell.getFamilyOffset(), cell.getFamilyLength()));
            hBaseCell.setCQualifier(Bytes.copy(cell.getQualifierArray(), cell.getQualifierOffset(), cell.getQualifierLength()));
            hBaseCell.setValue(Bytes.copy(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength()));
            hBaseCell.setTime(cell.getTimestamp());
            list.add(hBaseCell);
        }
        return list;
    }

    @PreDestroy
    private void closeCon() {
        try {
            if (admin != null) {
                admin.close();
            }
            if (connection != null) {
                connection.close();
            }
            LOGGER.info("Close hBase's Connection: success");
        } catch (IOException e) {
            LOGGER.error("Error occurred while closing the hBase's Connection.INFO: " + e.getMessage(), e);
        }
    }
}
