package cn.jmu.pictureprocess.service;

import cn.jmu.pictureprocess.hbase.pojo.HBaseCell;
import cn.jmu.pictureprocess.entity.Pictures;
import cn.jmu.pictureprocess.hbase.service.impl.HBaseServiceImpl;
import cn.jmu.pictureprocess.util.MyBytesUtil;
import org.apache.hadoop.hbase.CompareOperator;
import org.apache.hadoop.hbase.filter.BinaryComparator;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service("picturesService")
public class PicturesService extends HBaseServiceImpl {

    public List<Pictures> getByFileName(String name) throws IOException {
        Filter filter1 = new SingleColumnValueFilter(Bytes.toBytes(Pictures.COLUMNFAMILY_FILENAME), Bytes.toBytes(""),
                CompareOperator.EQUAL, new BinaryComparator(Bytes.toBytes(name)));
        FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL, filter1);
        List<HBaseCell> cells = scanByFilter(Pictures.TABLE_NAME, filterList);
        return cells2Pictures(cells);
    }

    public List<Pictures> getByRowKey(String rowKey) throws IOException {
        List<HBaseCell> cells = getRow(Pictures.TABLE_NAME, rowKey);
        return cells2Pictures(cells);
    }

    /**
     * 单元格数据转成适合业务的行数据
     *
     * @param cells HBaseCell列表
     * @return Pictures列表
     */
    private List<Pictures> cells2Pictures(List<HBaseCell> cells) {
        List<Pictures> pictures = new ArrayList<>();
        if (cells == null || cells.isEmpty()) return pictures;
        //只获取最新的行
        final Map<String, Long> rowMapTime = new HashMap<>(cells.size() / 6);
        cells.forEach(cell -> {
            String row = cell.formatRowKey();
            Long time = cell.getTime();
            Long timeOld = rowMapTime.get(row);
            if (timeOld == null || time > timeOld) {
                rowMapTime.put(row, time);
            }
        });
        final Map<Pictures, Integer> pictureMapIdx = new HashMap<>(rowMapTime.size());
        //合并cell为行
        cells.forEach(cell -> {
            String row = cell.formatRowKey();
            Long time = rowMapTime.get(row);
            if (time != null && time == cell.getTime()) {
                Pictures p = new Pictures();
                p.setRowKey(cell.formatRowKey());
                p.setTimeStamp(cell.formatTime());
                String columnFamily = cell.formatCFamily();
                String columnQualifier = cell.formatCQualifier();
                Integer idx = pictureMapIdx.get(p);
                if (idx != null) {
                    p = pictures.get(idx);
                } else {
                    pictures.add(p);
                    pictureMapIdx.put(p, pictures.size() - 1);
                }
                switch (columnFamily) {
                    case Pictures.COLUMNFAMILY_FILENAME:
                        p.setFileName(Bytes.toString(cell.getValue()));
                        break;
                    case Pictures.COLUMNFAMILY_PIXELARRAY:
                        p.setPixelArray(MyBytesUtil.bytes2IntegerList(cell.getValue(), true));
                        break;
                    case Pictures.COLUMNFAMILY_NATIVEDATA:
                        p.setNativeData(MyBytesUtil.bytes2ByteList(cell.getValue()));
                        break;
                    case Pictures.COLUMNFAMILY_INFO:
                        switch (columnQualifier) {
                            case Pictures.COLUMNQUALIFIER_INFO_WIDTH:
                                p.setWidth(Bytes.toInt(cell.getValue()));
                                break;
                            case Pictures.COLUMNQUALIFIER_INFO_HEIGHT:
                                p.setHeight(Bytes.toInt(cell.getValue()));
                                break;
                            case Pictures.COLUMNQUALIFIER_INFO_BITCOUNT:
                                p.setBitCount(Bytes.toShort(cell.getValue()));
                                break;
                            case Pictures.COLUMNQUALIFIER_INFO_OFFBITS:
                                p.setOffBits(Bytes.toInt(cell.getValue()));
                                break;
                            default:
                                throw new IllegalArgumentException();
                        }
                        break;
                    default:
                        throw new IllegalArgumentException();
                }
            }
        });
        return pictures;
    }


}
