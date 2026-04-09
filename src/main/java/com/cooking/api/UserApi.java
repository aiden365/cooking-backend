package com.cooking.api;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.crypto.digest.MD5;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.*;
import com.cooking.core.service.UserService;
import com.cooking.core.service.UserDishCollectService;
import com.cooking.core.service.UserLabelRelService;
import com.cooking.core.service.UserNutritionService;
import com.cooking.core.service.UserShareService;
import com.cooking.dto.UserEmailCodeDTO;
import com.cooking.dto.UserRegisterDTO;
import com.cooking.utils.EmailUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cooking.base.BaseController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

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

    private static final String EMAIL_CODE_KEY_PREFIX = "user:register:emailCode:";
    private static final int EMAIL_CODE_EXPIRE_MINUTES = 5;

    @Autowired
    private UserService userService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private EmailUtils emailUtils;
    @Autowired
    private UserDishCollectService userDishCollectService;
    @Autowired
    private UserShareService userShareService;
    @Autowired
    private UserLabelRelService userLabelRelService;
    @Autowired
    private UserNutritionService userNutritionService;

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
        UserEntity userEntity = userService.lambdaQuery().eq(UserEntity::getUserCode, userCode)
                .eq(UserEntity::getUserPass, digestedHex).list().stream().findAny().orElse(null);
        if (userEntity == null) {
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
    public BaseResponse register(@RequestBody UserRegisterDTO params) {
        String username = StrUtil.trim(params.getUsername());
        String password = params.getPassword();
        String email = StrUtil.trim(params.getEmail());
        String emailCode = StrUtil.trim(params.getEmailCode());

        if (StrUtil.hasBlank(username, password, email, emailCode)) {
            return fail("注册参数不完整");
        }
        if (!isValidEmail(email)) {
            return fail("邮箱格式不正确");
        }

        String redisKey = buildEmailCodeKey(email);
        String cachedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (!StrUtil.equals(cachedCode, emailCode)) {
            return fail("邮箱验证码错误或已过期");
        }

        UserEntity existUserEntity = userService.lambdaQuery()
                .and(wrapper -> wrapper.eq(UserEntity::getUserCode, email).or().eq(UserEntity::getEmail, email)).list()
                .stream().findAny().orElse(null);
        if (existUserEntity != null) {
            return fail("该邮箱已注册");
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setUserName(username);
        userEntity.setUserCode(email);
        userEntity.setEmail(email);
        userEntity.setUserPass(MD5.create().digestHex(password));
        userEntity.setType(1);
        userEntity.setStatus(1);
        userService.save(userEntity);
        stringRedisTemplate.delete(redisKey);

        String token = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(token, userEntity.getId().toString(), 1, TimeUnit.HOURS);
        JSONObject res = new JSONObject();
        res.put("user", userEntity);
        res.put("token", token);
        return ok(res);
    }

    @PostMapping("sendEmailCode")
    public BaseResponse sendEmailCode(@RequestBody UserEmailCodeDTO params) {
        String email = StrUtil.trim(params.getEmail());
        if (StrUtil.isBlank(email)) {
            return fail("邮箱不能为空");
        }
        if (!isValidEmail(email)) {
            return fail("邮箱格式不正确");
        }

        UserEntity existUserEntity = userService.lambdaQuery()
                .and(wrapper -> wrapper.eq(UserEntity::getUserCode, email).or().eq(UserEntity::getEmail, email)).list()
                .stream().findAny().orElse(null);
        if (existUserEntity != null) {
            return fail("该邮箱已注册");
        }

        String emailCode = RandomUtil.randomNumbers(6);
        emailUtils.sendVerificationCodeEmail(email, emailCode, EMAIL_CODE_EXPIRE_MINUTES);
        stringRedisTemplate.opsForValue().set(buildEmailCodeKey(email), emailCode, EMAIL_CODE_EXPIRE_MINUTES,
                TimeUnit.MINUTES);
        return ok("验证码已发送");
    }

    @PostMapping("edit")
    public BaseResponse edit(@RequestBody UserEntity userEntity) {
        UserEntity existUserEntity = userService.lambdaQuery().eq(UserEntity::getId, userEntity.getId()).list().stream()
                .findAny().orElse(null);
        if (existUserEntity == null) {
            return fail("用户不存在");
        }

        userService.saveOrUpdate(userEntity);
        return ok(userEntity);
    }

    private boolean isValidEmail(String email) {
        return ReUtil.isMatch("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", email);
    }

    private String buildEmailCodeKey(String email) {
        return EMAIL_CODE_KEY_PREFIX + email.toLowerCase();
    }

    @PostMapping("count")
    public BaseResponse count(@RequestBody JSONObject params) {
        Long userId = params.getLong("userId");
        if (userId == null) {
            return fail("userId不能为空");
        }

        long collectCount = userDishCollectService.lambdaQuery().eq(UserDishCollectEntity::getUserId, userId).count();
        long shareCount = userShareService.lambdaQuery().eq(UserShareEntity::getUserId, userId).count();
        long labelCount = userLabelRelService.lambdaQuery().eq(UserLabelRelEntity::getUserId, userId).count();
        long nutritionCount = userNutritionService.lambdaQuery().eq(UserNutritionEntity::getUserId, userId).count();

        JSONObject result = new JSONObject();
        result.put("collectCount", collectCount);
        result.put("shareCount", shareCount);
        result.put("labelCount", labelCount);
        result.put("nutritionCount", nutritionCount);

        return ok(result);
    }

    @PostMapping("saveLabels")
    public BaseResponse saveLabels(@RequestBody JSONObject params) {
        Long userId = params.getLong("userId");
        List<Long> labelIds = params.getList("labels", Long.class);

        if (userId == null) {
            return fail("userId不能为空");
        }

        long currentLabelCount = userLabelRelService.count(new LambdaQueryWrapper<UserLabelRelEntity>().eq(UserLabelRelEntity::getUserId, userId));
        Set<Long> existingLabelIds = userLabelRelService.list(new LambdaQueryWrapper<UserLabelRelEntity>().eq(UserLabelRelEntity::getUserId, userId)).stream().map(UserLabelRelEntity::getLabelId).collect(Collectors.toSet());

        Set<Long> newLabelIds = (labelIds == null) ? Collections.emptySet() : new HashSet<>(labelIds);

        long toAddCount = newLabelIds.stream().filter(id -> !existingLabelIds.contains(id)).count();
        long toDeleteCount = existingLabelIds.stream().filter(id -> !newLabelIds.contains(id)).count();

        if (currentLabelCount + toAddCount - toDeleteCount > 5) {
            return fail("用户标签总数不能超过5个");
        }

        userLabelRelService.saveUserLabels(userId, labelIds);

        return ok("用户标签保存成功");
    }
}
