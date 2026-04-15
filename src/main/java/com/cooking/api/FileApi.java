package com.cooking.api;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.cooking.base.BaseController;
import com.cooking.base.BaseResponse;
import com.cooking.enums.PathEnum;
import com.cooking.exceptions.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * 文件上传
 */
@RestController
@RequestMapping("/file")
public class FileApi extends BaseController {

    @Value("${upload.path}")
    private String uploadPath;

    @Value("${upload.url}")
    private String uploadUrl;

    /**
     * 文件上传
     *
     * @param file
     * @param pathType
     * @return
     */
    @PostMapping("/upload")
    public BaseResponse upload(@RequestParam("file") MultipartFile file,@RequestParam(value = "pathType", required = false) String pathType) {

        if (StrUtil.isBlank(pathType)) {
            pathType = PathEnum.dish_img_path.toString();
        }

        PathEnum pathEnum = null;
        for (PathEnum value : PathEnum.values()) {
            if (value.toString().equals(pathType)) {
                pathEnum = value;
                break;
            }
        }

        if (pathEnum == null) {
            return fail("pathType 值不正确");
        }

        

        // 获取文件的原始名称
        String originalFilename = file.getOriginalFilename();
        // 获取文件的扩展名
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        // 生成新的文件名
        String newFileName = IdUtil.simpleUUID() + fileExtension;
        // 构建文件的存储路径
        String filePath = pathEnum.getValue() + "/" + newFileName;
        String destPath = uploadPath + filePath;

        try {
            // 保存文件
            FileUtil.writeBytes(file.getBytes(), destPath);
        } catch (IOException e) {
            return fail("文件上传失败");
        }

        return ok("文件上传成功", filePath);
    }
}