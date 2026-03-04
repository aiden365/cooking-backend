package com.cooking.api;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.UserEntity;
import com.cooking.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cooking.base.BaseController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@RestController
@RequestMapping("user")
public class UserApi extends BaseController {

    @Autowired
    private UserService userService;

    @PostMapping("list")
    public BaseResponse list(@RequestBody JSONObject params) {
        String search = params.getString("search");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("search", search);
        List<UserEntity> userEntityList = userService.findList(params);
        return ok(userEntityList);
    }

    @PostMapping("page")
    public BaseResponse page(@RequestBody JSONObject params) {
        // 分页页号
        int pageNo = params.getIntValue("pageNo");
        // 一页显示多少数据
        int pageSize = params.getIntValue("pageSize");
        IPage<UserEntity> page = new Page<>(pageNo, pageSize);

        String search = params.getString("search");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("search", search);
        IPage<UserEntity> entityIPage = userService.findPage(page, params);
        return ok(entityIPage);
    }

}
