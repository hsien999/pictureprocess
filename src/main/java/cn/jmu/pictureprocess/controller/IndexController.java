package cn.jmu.pictureprocess.controller;

import cn.jmu.pictureprocess.entity.Pictures;
import cn.jmu.pictureprocess.service.PicturesService;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Bytes;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

@Controller
public class IndexController {
    @Resource
    private PicturesService picturesService;
    @Resource
    private ObjectMapper objectMapper;

    @GetMapping("/")
    public String index() {
        return "index";
    }


/*
    @GetMapping("get")
    @ResponseBody
    public void get(HttpServletResponse response) {
        try {
            List<HBaseCell> list = hBaseService.selectValue("Pictures", "101", "NativeData", "");
            if (list.size() < 1) return;
            ByteArrayInputStream inputStream = new ByteArrayInputStream(list.get(0).getValue());
            BufferedImage image = ImageIO.read(inputStream);
            ImageIO.write(image, "bmp", response.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
*/


    @GetMapping("/getPictureInfoByName")
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
}
