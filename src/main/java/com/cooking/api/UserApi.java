package com.cooking.api;

import cn.hutool.core.lang.UUID;
import cn.hutool.crypto.digest.MD5;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.UserEntity;
import com.cooking.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

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

    @PostMapping("login")
    public BaseResponse login(@RequestBody JSONObject params) {
        String userCode = params.getString("userCode");
        String password = params.getString("password");
        String digestedHex = MD5.create().digestHex(password);
        UserEntity userEntity = userService.lambdaQuery().eq(UserEntity::getUserCode, userCode).eq(UserEntity::getUserPass, digestedHex).list().stream().findAny().orElse(null);
        if(userEntity == null){
            return fail("用户不存在");
        }
        String token = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(token, userEntity.getId().toString());
        JSONObject res = new JSONObject();
        res.put("user", userEntity);
        res.put("token", token);
        return ok(res);
    }

    @PostMapping("register")
    public BaseResponse register(@RequestBody UserEntity userEntity) {
        UserEntity existUserEntity = userService.lambdaQuery().eq(UserEntity::getUserCode, userEntity.getUserCode()).list().stream().findAny().orElse(null);
        if(existUserEntity != null){
            return fail("用户已存在");
        }
        userEntity.setUserPass(MD5.create().digestHex(userEntity.getUserPass()));
        userService.saveOrUpdate(userEntity);
        String token = UUID.randomUUID().toString();
        JSONObject res = new JSONObject();
        res.put("user", userEntity);
        res.put("token", token);
        return ok(res);
    }

    @PostMapping("edit")
    public BaseResponse edit(@RequestBody UserEntity userEntity) {
        UserEntity existUserEntity = userService.lambdaQuery().eq(UserEntity::getId, userEntity.getId()).list().stream().findAny().orElse(null);
        if(existUserEntity == null){
            return fail("用户不存在");
        }

        userService.saveOrUpdate(userEntity);
        return ok(userEntity);
    }

}
