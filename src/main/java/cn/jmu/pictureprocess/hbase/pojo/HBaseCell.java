package cn.jmu.pictureprocess.hbase.pojo;

import lombok.Data;
import org.apache.hadoop.hbase.util.Bytes;

import java.util.Date;

@Data
public class HBaseCell {

    private byte[] rowKey;
    private byte[] cFamily;
    private byte[] cQualifier;
    private byte[] value;
    private long time;

    public String formatRowKey() {
        return Bytes.toString(rowKey);
    }


    public String formatCFamily() {
        return Bytes.toString(cFamily);
    }

    public String formatCQualifier() {
        return Bytes.toString(cQualifier);
    }


    public Date formatTime() {
        return new Date(time);
    }
}
