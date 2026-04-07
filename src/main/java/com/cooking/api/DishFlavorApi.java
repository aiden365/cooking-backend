package com.cooking.api;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.DishFlavorEntity;
import com.cooking.core.service.DishFlavorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Dish flavor controller
 * </p>
 */
@RestController
@RequestMapping("dishFlavor")
public class DishFlavorApi extends BaseController {

    @Autowired
    private DishFlavorService dishFlavorService;

    @PostMapping("page")
    public BaseResponse page(@RequestBody JSONObject params) {
        Long dishId = params.getLong("dishId");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("dishId", dishId);
        IPage<DishFlavorEntity> entityIPage = dishFlavorService.findPage(new Page<>(pageNo, pageSize), queryParams);
        return ok(entityIPage);
    }
}
