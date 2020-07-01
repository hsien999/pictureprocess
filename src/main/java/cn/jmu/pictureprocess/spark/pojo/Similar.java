package cn.jmu.pictureprocess.spark.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Similar {
    private String similarity;
    private byte[][] matrix;
}
