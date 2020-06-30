package cn.jmu.pictureprocess.controller;

import cn.jmu.pictureprocess.entity.Pictures;
import cn.jmu.pictureprocess.service.PicturesService;
import cn.jmu.pictureprocess.spark.service.SearchService;
import cn.jmu.pictureprocess.util.BMPPicture;
import cn.jmu.pictureprocess.util.Matrix2D;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Bytes;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.util.*;

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
            System.out.println(pictures.get(0));
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
            Map<String, List<Matrix2D.Point>> partMap = searchService.searchPart(bmpPicture);
            List<Map<String, Object>> result = new ArrayList<>(partMap.size());
            partMap.forEach((k, v) -> {
                Map<String, Object> map = new HashMap<>(2);
                map.put("fileName", k);
                map.put("points", v);
                result.add(map);
            });
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
            Map<String, String> partMap = searchService.checkTampered(bmpPicture);
            List<Map<String, String>> result = new ArrayList<>(partMap.size());
            partMap.forEach((k, v) -> {
                Map<String, String> map = new HashMap<>(2);
                map.put("fileName", k);
                map.put("similarity", v);
                result.add(map);
            });
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
