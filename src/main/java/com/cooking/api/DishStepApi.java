package com.cooking.api;

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.DishEntity;
import com.cooking.core.entity.DishStepEntity;
import com.cooking.core.service.DishService;
import com.cooking.core.service.DishStepService;
import com.cooking.enums.PathEnum;
import com.cooking.exceptions.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * <p>
 * Dish step controller
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
@RestController
@RequestMapping("/dishStep")
public class DishStepApi extends BaseController {

    private static final int STEP_DESCRIBE_MAX_LENGTH = 255;

    @Autowired
    private DishStepService dishStepService;
    @Autowired
    private DishService dishService;

    @PostMapping("page")
    public BaseResponse page(@RequestBody com.alibaba.fastjson2.JSONObject params) {
        String search = params.getString("search");
        Long dishId = params.getLong("dishId");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("search", search);
        queryParams.put("dishId", dishId);

        IPage<DishStepEntity> entityIPage = dishStepService.findPage(new Page<>(pageNo, pageSize), queryParams);
        return ok(entityIPage);
    }

    @PostMapping("list")
    public BaseResponse list(@RequestBody com.alibaba.fastjson2.JSONObject params) {
        IPage<DishStepEntity> entityIPage = dishStepService.findPage(new Page<>(1, -1), params);
        return ok(entityIPage.getRecords());
    }

    @PostMapping(value = "save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse save(@RequestParam(value = "id", required = false) Long id,
                             @RequestParam(value = "dishId", required = false) Long dishId,
                             @RequestParam(value = "stepDescribe", required = false) String stepDescribe,
                             @RequestParam(value = "sort", required = false) Integer sort,
                             @RequestParam(value = "stepImage", required = false) String stepImage,
                             @RequestPart(value = "files", required = false) MultipartFile[] files) {

        DishStepEntity dishStepEntity;
        if (id == null) {
            dishStepEntity = new DishStepEntity();
        } else {
            dishStepEntity = dishStepService.getById(id);
            if (dishStepEntity == null) {
                throw new ApiException(BaseResponse.Code.fail.code, "步骤不存在");
            }
        }

        validateBasicParams(dishId, stepDescribe, sort);


        dishStepEntity.setDishId(dishId);
        dishStepEntity.setStepDescribe(stepDescribe.trim());
        dishStepEntity.setSort(sort);
        dishStepEntity.setStepImage(stepImage);

        dishStepService.saveOrUpdate(dishStepEntity);
        return ok(dishStepEntity);
    }

    @PostMapping("delete")
    public BaseResponse delete(@RequestBody com.alibaba.fastjson2.JSONObject params) {
        Long id = params.getLong("id");
        if (id == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "id不能为空");
        }

        dishStepService.removeById(id);

        return ok();
    }

    private void validateBasicParams(Long dishId, String stepDescribe, Integer sort) {
        if (dishId == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "dishId不能为空");
        }
        DishEntity dishEntity = dishService.getById(dishId);
        if (dishEntity == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "菜品不存在");
        }

        if (!StringUtils.hasText(stepDescribe)) {
            throw new ApiException(BaseResponse.Code.fail.code, "步骤描述不能为空");
        }
        if (stepDescribe.trim().length() > STEP_DESCRIBE_MAX_LENGTH) {
            throw new ApiException(BaseResponse.Code.fail.code, "步骤描述长度不能超过" + STEP_DESCRIBE_MAX_LENGTH);
        }

        if (sort == null) {
            throw new ApiException(BaseResponse.Code.fail.code, "步骤序号不能为空");
        }
    }

    private String buildStepImagesValue(Long dishId, MultipartFile[] files, String stepImagesText, String existingStepImagesText) {
        List<String> existingPaths = StringUtils.hasText(existingStepImagesText)
                ? parseAndValidateStepImages(dishId, existingStepImagesText, true)
                : new ArrayList<>();

        boolean hasRetainedParam = stepImagesText != null;
        List<String> retainedPaths;
        if (hasRetainedParam) {
            retainedPaths = parseAndValidateStepImages(dishId, stepImagesText, true);
        } else {
            retainedPaths = new ArrayList<>(existingPaths);
        }

        List<String> newPaths = (files != null && files.length > 0) ? saveUploadedFiles(dishId, files) : new ArrayList<>();

        if (hasRetainedParam) {
            deleteRemovedFiles(existingPaths, retainedPaths);
        }

        Set<String> finalPathSet = new LinkedHashSet<>();
        finalPathSet.addAll(retainedPaths);
        finalPathSet.addAll(newPaths);

        if (finalPathSet.isEmpty()) {
            throw new ApiException(BaseResponse.Code.fail.code, "请上传步骤图片或传入stepImages");
        }

        return JSONArray.from(finalPathSet).toJSONString();
    }

    private List<String> parseAndValidateStepImages(Long dishId, String stepImagesText, boolean allowEmpty) {
        JSONArray stepImagesArray;
        try {
            stepImagesArray = JSONArray.parseArray(stepImagesText);
        } catch (Exception e) {
            throw new ApiException(BaseResponse.Code.fail.code, "stepImages必须是JSON数组");
        }

        if (!allowEmpty && (stepImagesArray == null || stepImagesArray.isEmpty())) {
            throw new ApiException(BaseResponse.Code.fail.code, "stepImages不能为空数组");
        }
        if (stepImagesArray == null) {
            return new ArrayList<>();
        }

        String prefix = PathEnum.dish_img_path.getValue() + "/" + dishId + "/";
        List<String> paths = new ArrayList<>();
        for (Object imageObj : stepImagesArray) {
            if (!(imageObj instanceof String img) || !StringUtils.hasText(img)) {
                throw new ApiException(BaseResponse.Code.fail.code, "stepImages中包含无效图片路径");
            }
            String imagePath = img.trim();
            if (!imagePath.startsWith(prefix)) {
                throw new ApiException(BaseResponse.Code.fail.code, "步骤图片路径必须以" + prefix + "开头");
            }
            paths.add(imagePath);
        }
        return paths;
    }

    private void deleteRemovedFiles(List<String> existingPaths, List<String> retainedPaths) {
        Set<String> retainedSet = new LinkedHashSet<>(retainedPaths);
        for (String existingPath : existingPaths) {
            if (!retainedSet.contains(existingPath)) {
                deleteFileByRelativePath(existingPath);
            }
        }
    }

    private void deleteFileByRelativePath(String relativePath) {
        String normalizedRelativePath = relativePath.replaceFirst("^[/\\\\]+", "");
        Path filePath = Paths.get(uploadPath, normalizedRelativePath);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new ApiException(BaseResponse.Code.fail.code, "删除已移除图片失败");
        }
    }

    private List<String> saveUploadedFiles(Long dishId, MultipartFile[] files) {
        String basePath = PathEnum.dish_img_path.getValue().replaceFirst("^[/\\\\]+", "");
        Path dishDir = Paths.get(uploadPath, basePath, String.valueOf(dishId));
        try {
            Files.createDirectories(dishDir);
        } catch (IOException e) {
            throw new ApiException(BaseResponse.Code.fail.code, "创建图片目录失败");
        }

        List<String> savedPaths = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new ApiException(BaseResponse.Code.fail.code, "上传文件不能为空");
            }
            String ext = "";
            String originFileName = file.getOriginalFilename();
            if (StringUtils.hasText(originFileName) && originFileName.lastIndexOf('.') >= 0) {
                ext = originFileName.substring(originFileName.lastIndexOf('.'));
            }

            String fileName = UUID.randomUUID() + ext;
            Path target = dishDir.resolve(fileName);
            try {
                file.transferTo(target);
            } catch (IOException e) {
                throw new ApiException(BaseResponse.Code.fail.code, "保存图片失败");
            }
            savedPaths.add(PathEnum.dish_img_path.getValue() + "/" + dishId + "/" + fileName);
        }
        return savedPaths;
    }
}
