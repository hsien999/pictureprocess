package cn.jmu.pictureprocess.hbase.pojo;

import lombok.Data;
import lombok.Setter;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
