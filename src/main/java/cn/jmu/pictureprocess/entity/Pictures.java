package cn.jmu.pictureprocess.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;
import org.springframework.stereotype.Repository;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Repository
@Getter
@Setter
@NoArgsConstructor
@ToString

public class Pictures implements Serializable {
    public static final String TABLE_NAME = "Pictures";
    public static final String COLUMNFAMILY_FILENAME = "FileName";
    public static final String COLUMNFAMILY_PIXELARRAY = "PixelArray";
    public static final String COLUMNFAMILY_NATIVEDATA = "NativeData";
    public static final String COLUMNFAMILY_INFO = "Info";
    public static final String COLUMNQUALIFIER_INFO_WIDTH = "Width";
    public static final String COLUMNQUALIFIER_INFO_HEIGHT = "Height";
    public static final String COLUMNQUALIFIER_INFO_BITCOUNT = "BitCount";
    public static final String COLUMNQUALIFIER_INFO_OFFBITS = "OffBits";

    public interface BaseInfoView {
    }


    public interface AllInfoView extends BaseInfoView {
    }

    @JsonView(BaseInfoView.class)
    private String rowKey;
    @JsonView(BaseInfoView.class)
    private String fileName;
    @JsonView(BaseInfoView.class)
    private int width;
    @JsonView(BaseInfoView.class)
    private int height;
    @JsonView(BaseInfoView.class)
    private short bitCount;
    @JsonView(AllInfoView.class)
    private int offBits;
    @ToString.Exclude
    @JsonView(BaseInfoView.class)
    private List<Integer> pixelArray;
    @ToString.Exclude
    @JsonView(AllInfoView.class)
    private List<Byte> nativeData;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonView(AllInfoView.class)
    private Date timeStamp;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pictures pictures = (Pictures) o;
        return Objects.equals(rowKey, pictures.rowKey) &&
                Objects.equals(timeStamp, pictures.timeStamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rowKey, timeStamp);
    }
}
