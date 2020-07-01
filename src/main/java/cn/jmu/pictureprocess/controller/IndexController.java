package cn.jmu.pictureprocess.controller;

import cn.jmu.pictureprocess.entity.Pictures;
import cn.jmu.pictureprocess.service.PicturesService;
import cn.jmu.pictureprocess.spark.pojo.Similar;
import cn.jmu.pictureprocess.spark.service.SearchService;
import cn.jmu.pictureprocess.util.BMPPicture;
import cn.jmu.pictureprocess.util.MatrixBFS;
import cn.jmu.pictureprocess.util.MatrixMatch;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Bytes;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sun.misc.BASE64Encoder;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

@Controller
public class IndexController {
    @Resource
    private PicturesService picturesService;
    @Resource
    private SearchService searchService;
    @Resource
    private ObjectMapper objectMapper;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping(value = "/getPictureInfoByName", produces = "application/json")
    @ResponseBody
    @JsonView(Pictures.BaseInfoView.class)
    public Pictures getPictureInfoByName(@RequestParam(defaultValue = "1.bmp") String name) {
        try {
            List<Pictures> pictures = picturesService.getByFileName(name);
            if (pictures.size() == 0) return null;
            return pictures.get(0);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @GetMapping(value = "/getPicture", produces = "image/bmp")
    @ResponseBody
    public BufferedImage getPicture(@RequestParam String rowKey) {
        try {
            List<Pictures> pictures = picturesService.getByRowKey(rowKey);
            if (pictures.size() == 0) return null;
            Pictures pictures1 = pictures.get(0);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Bytes.toArray(pictures1.getNativeData()));
            return ImageIO.read(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @PostMapping(value = "/searchPictureForAll", produces = "application/json")
    @ResponseBody
    public String searchPictureForAll(@RequestParam("file") MultipartFile file) {
        try {
            InputStream stream = file.getInputStream();
            BufferedInputStream bufferedInputStream = new BufferedInputStream(stream);
            BMPPicture bmpPicture = new BMPPicture(bufferedInputStream);
            String fileName = searchService.searchAll(bmpPicture);
            Map<String, String> map = new HashMap<>(1);
            map.put("fileName", fileName);
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @PostMapping(value = "/searchPictureForPart", produces = "application/json")
    @ResponseBody
    public String searchPictureForPart(@RequestParam("file") MultipartFile file) {
        try {
            InputStream stream = file.getInputStream();
            BufferedInputStream bufferedInputStream = new BufferedInputStream(stream);
            BMPPicture bmpPicture = new BMPPicture(bufferedInputStream);
            Map<String, List<MatrixMatch.Point>> partMap = searchService.searchPart(bmpPicture);
            List<Map<String, Object>> result = new ArrayList<>(partMap.size());
            //第一个数据保存提交的图片的宽高
            Map<String, Object> head = new HashMap<>(2);
            head.put("width", bmpPicture.getBiWidth());
            head.put("height", Math.abs(bmpPicture.getBiHeight()));
            result.add(head);
            partMap.forEach((k, v) -> {
                Map<String, Object> map = new HashMap<>(2);
                map.put("fileName", k);
                map.put("points", v);
                result.add(map);
            });
            if (result.size() < 1) return "[]";
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @PostMapping(value = "/searchPictureForTampered", produces = "application/json")
    @ResponseBody
    public String searchPictureForTampered(@RequestParam("file") MultipartFile file) {
        try {
            InputStream stream = file.getInputStream();
            BufferedInputStream bufferedInputStream = new BufferedInputStream(stream);
            BMPPicture bmpPicture = new BMPPicture(bufferedInputStream);
            Map<String, Similar> partMap = searchService.checkTampered(bmpPicture);
            List<Map<String, Object>> result = new ArrayList<>(partMap.size());
            partMap.forEach((k, v) -> {
                Map<String, Object> map = new HashMap<>();
                map.put("fileName", k);
                map.put("similarity", v.getSimilarity());
                map.put("matrix", v.getMatrix());
                result.add(map);
            });
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @PostMapping(value = "/getResolvePictureForPart", produces = "image/bmp")
    @ResponseBody
    public String getResolvePictureForPart(@RequestParam("fileName") String fileName,
                                           @RequestParam("width") int width,
                                           @RequestParam("height") int height,
                                           @RequestParam("points") String pointsStr) {
        try {
            //获取匹配到的原图片
            Pictures pictures = picturesService.getByFileName(fileName).get(0);
            ByteArrayInputStream inputStream1 = new ByteArrayInputStream(Bytes.toArray(pictures.getNativeData()));
            BMPPicture bmpPicture = new BMPPicture(new BufferedInputStream(inputStream1));
            inputStream1.close();
            //原图片8bit转24bit(才能绘制彩色线条)
            BufferedImage image = transferPic(bmpPicture);
            //绘制局部匹配所在的矩形范围
            Graphics graphics = image.getGraphics();
            graphics.setColor(Color.YELLOW);
            JavaType type = objectMapper.getTypeFactory().constructParametricType(List.class, MatrixMatch.Point.class);
            List<MatrixMatch.Point> pointList = objectMapper.readValue(pointsStr, type);
            for (MatrixMatch.Point point : pointList) {
                graphics.drawRect(point.getX(), point.getY(), width, height);
            }
            return image2Base64(image);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @PostMapping(value = "/getResolvePictureForTampered", produces = "image/bmp")
    @ResponseBody
    public String getResolvePictureForPart(@RequestParam("file") MultipartFile file, @RequestParam("matrix") String matrixStr) {
        try {
            InputStream stream = file.getInputStream();
            BMPPicture patternPic = new BMPPicture(new BufferedInputStream(stream));
            String[] matrixStrLine = objectMapper.readValue(matrixStr, String[].class);
            byte[][] matrix = new byte[matrixStrLine.length][];
            Base64.Decoder decoder = Base64.getDecoder();
            for (int i = 0; i < matrix.length; i++) {
                matrix[i] = decoder.decode(matrixStrLine[i]);
            }
            MatrixBFS bfs = new MatrixBFS(matrix, patternPic.getBiHeight() / matrix.length,
                    patternPic.getBiWidth() / matrix[0].length);
            List<MatrixBFS.Point> pointList = bfs.process();
            //原图片8bit转24bit(才能绘制彩色线条)
            BufferedImage image = transferPic(patternPic);
            //绘制篡改部分所在的矩形范围
            Graphics graphics = image.getGraphics();
            graphics.setColor(Color.YELLOW);
            for (int i = 0; i < pointList.size(); i += 2) {
                MatrixBFS.Point start = pointList.get(i), end = pointList.get(i + 1);
                int x = start.getY(), y = start.getX();
                int width = end.getY() - x, height = end.getX() - y;
                graphics.drawRect(x, y, width, height);
            }
            return image2Base64(image);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 原图片8bit转24bit(才能绘制彩色线条)
     *
     * @param picture BMPPicture图片
     * @return BufferedImage
     * @throws IOException IO异常
     */
    private BufferedImage transferPic(BMPPicture picture) throws IOException {
        ByteArrayOutputStream outputStream1 = BMPPicture.transfer8bTo24b(picture);
        ByteArrayInputStream inputStream2 = new ByteArrayInputStream(outputStream1.toByteArray());
        BufferedImage image = ImageIO.read(inputStream2);
        outputStream1.close();
        inputStream2.close();
        return image;
    }

    /**
     * BufferedImage转base64字符串
     *
     * @param image BufferedImage
     * @return ase64字符串
     * @throws IOException IO异常
     */
    private String image2Base64(BufferedImage image) throws IOException {
        //将绘制后的BufferedImage通过字节数组流输出
        ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
        ImageIO.write(image, "bmp", outputStream2);
        byte[] bytes = outputStream2.toByteArray();
        outputStream2.close();
        //字节数组转base64字符串,提交前端
        BASE64Encoder encoder = new BASE64Encoder();
        String bmp_base64 = encoder.encodeBuffer(bytes).trim();
        bmp_base64 = bmp_base64.replaceAll("\n", "").replaceAll("\r", "");
        return "data:image/bmp;base64," + bmp_base64;
    }
}
