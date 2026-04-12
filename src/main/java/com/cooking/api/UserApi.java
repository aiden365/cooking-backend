package com.cooking.api;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.crypto.digest.MD5;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.*;
import com.cooking.core.service.*;
import com.cooking.dto.UserEmailCodeDTO;
import com.cooking.dto.UserRegisterDTO;
import com.cooking.utils.EmailUtils;
import com.cooking.utils.SystemContextHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
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
 * @since 2026-02-03
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
    private UserNutritionRelService userNutritionRelService;
    @Autowired
    private UserIndividualDishService userIndividualDishService;
    @Autowired
    private UserDietRecordService userDietRecordService;
    @Autowired
    private UserShareCommentService userShareCommentService;
    @Autowired
    private DishAppraisesService dishAppraisesService;
    @Autowired
    private DishCommentService dishCommentService;

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
        IPage<UserEntity> entityIPage = userService.findPage(new Page<>(pageNo, pageSize), params);
        return ok(entityIPage);
    }

    @PostMapping("login")
    public BaseResponse login(@RequestBody JSONObject params) {
        String userCode = params.getString("userCode");
        String password = params.getString("password");
        String digestedHex = MD5.create().digestHex(password);
        UserEntity userEntity = userService.lambdaQuery().eq(UserEntity::getUserCode, userCode).eq(UserEntity::getUserPass, digestedHex).list().stream().findAny().orElse(null);
        if (userEntity == null) {
            return fail("用户不存在");
        }
        String token = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(token, userEntity.getId().toString());
        JSONObject res = new JSONObject();
        res.put("id", userEntity.getId());
        res.put("avatar", null);
        res.put("userName", userEntity.getUserName());
        res.put("username", userEntity.getUserName());
        res.put("nickname", userEntity.getUserName());
        res.put("roles", Collections.singleton("admin"));
        res.put("permissions", Collections.emptyList());
        res.put("accessToken", token);
        res.put("refreshToken", token);
        res.put("expires", "2099/01/01 00:00:00");

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
        res.put("id", userEntity.getId());
        res.put("userName", userEntity.getUserName());
        res.put("expires", "2099/01/01 00:00:00");
        res.put("accessToken", token);
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



        String emailCode = RandomUtil.randomNumbers(6);
        emailUtils.sendVerificationCodeEmail(email, emailCode, EMAIL_CODE_EXPIRE_MINUTES);
        stringRedisTemplate.opsForValue().set(buildEmailCodeKey(email), emailCode, EMAIL_CODE_EXPIRE_MINUTES,
                TimeUnit.MINUTES);
        return ok("验证码已发送");
    }

    @PostMapping("detail")
    public BaseResponse detail(@RequestBody JSONObject params) {
        Long id = params.getLong("id");
        if (!UserEntity.validId(id)) {
            id = SystemContextHelper.getCurrentUser().getId();
        }
        if (!UserEntity.validId(id)) {
            return fail("id不能为空");
        }
        UserEntity userEntity = userService.lambdaQuery().eq(UserEntity::getId, id).list().stream().findAny().orElse(null);
        if (userEntity == null) {
            return fail("用户不存在");
        }
        return ok(userEntity);
    }

    @PostMapping("statistics")
    public BaseResponse statistics() {

        UserEntity currentUser = SystemContextHelper.getCurrentUser();
        Long userId = currentUser.getId();
        Long collectCount = userDishCollectService.lambdaQuery().eq(UserDishCollectEntity::getUserId, userId).count();
        Long shareCount = userShareService.lambdaQuery().eq(UserShareEntity::getUserId, userId).count();
        Long labelCount = userLabelRelService.lambdaQuery().eq(UserLabelRelEntity::getUserId, userId).count();
        Long nutritionCount = userNutritionRelService.lambdaQuery().eq(UserNutritionRelEntity::getUserId, userId).count();
        Long individualDishCount = userIndividualDishService.lambdaQuery().eq(UserIndividualDishEntity::getUserId, userId).count();
        JSONObject res = new JSONObject();
        res.put("collectCount", collectCount);
        res.put("shareCount", shareCount);
        res.put("labelCount", labelCount);
        res.put("nutritionCount", nutritionCount);
        res.put("individualDishCount", individualDishCount);
        return ok(res);
    }


    @PostMapping("labels")
    public BaseResponse labels() {
        UserEntity currentUser = SystemContextHelper.getCurrentUser();
        List<Long> list = userLabelRelService.lambdaQuery().eq(UserLabelRelEntity::getUserId, currentUser.getId()).list().stream().map(UserLabelRelEntity::getLabelId).collect(Collectors.toList());
        return ok(list);
    }

    @PostMapping("update-status")
    public BaseResponse updateStatus(@RequestBody JSONObject params) {
        Long userId = params.getLong("userId");
        Integer status = params.getInteger("status");
        if (!UserEntity.validId(userId)) {
            return fail("id不能为空");
        }
        UserEntity userEntity = userService.lambdaQuery().eq(UserEntity::getId, userId).list().stream().findAny().orElse(null);
        if (userEntity == null) {
            return fail("用户不存在");
        }
        if(status == null){
            return fail("状态不能为空");
        }

        userEntity.setStatus(status);
        userService.saveOrUpdate(userEntity);
        return ok(userEntity);
    }

    @PostMapping("reset-password")
    public BaseResponse resetPassword(@RequestBody JSONObject params) {
        Long userId = params.getLong("userId");
        String password = params.getString("password");
        String emailCode = params.getString("emailCode");
        if (!UserEntity.validId(userId)) {
            return fail("id不能为空");
        }
        UserEntity userEntity = userService.lambdaQuery().eq(UserEntity::getId, userId).list().stream().findAny().orElse(null);
        if (userEntity == null) {
            return fail("用户不存在");
        }
        if(StrUtil.isBlank( password)){
            return fail("密码不能为空");
        }


        userEntity.setUserPass(MD5.create().digestHex(password));
        userService.saveOrUpdate(userEntity);
        return ok(userEntity);
    }

    @PostMapping("forgot-password")
    public BaseResponse forgotPassword(@RequestBody JSONObject params) {
        String email = params.getString("email");
        String verifyCode = params.getString("verifyCode");
        String password = params.getString("password");
        if (StrUtil.isBlank(email)) {
            return fail("邮箱不能为空");
        }
        if (!isValidEmail(email)) {
            return fail("邮箱格式不正确");
        }
        if (StrUtil.isBlank(password)) {
            return fail("密码不能为空");
        }
        if (StrUtil.isBlank(verifyCode)) {
            return fail("验证码不能为空");
        }

        UserEntity userEntity = userService.lambdaQuery().eq(UserEntity::getUserCode, email).list().stream().findAny().orElse(null);
        if (userEntity == null) {
            return fail("用户不存在");
        }

        String redisKey = buildEmailCodeKey(email);
        String cachedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (!StrUtil.equals(cachedCode, verifyCode)) {
            return fail("邮箱验证码错误或已过期");
        }

        userEntity.setUserPass(MD5.create().digestHex(password));
        userService.saveOrUpdate(userEntity);
        return ok(userEntity);
    }



    @PostMapping("save")
    public BaseResponse save(@RequestBody UserEntity userEntity) {
        //根据用户id是否存在 决定是新增用户还是修改用户。
        if (userEntity.getId() == null) {
            userEntity.setUserPass(MD5.create().digestHex(userEntity.getUserPass()));
            userEntity.setType(1);
            userEntity.setStatus(1);
        }else{
            UserEntity exist = userService.getById(userEntity.getId());
            if (exist == null) {
                return fail("用户不存在");
            }
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
        long nutritionCount = userNutritionRelService.lambdaQuery().eq(UserNutritionRelEntity::getUserId, userId).count();
        long individualDishCount = userIndividualDishService.lambdaQuery().eq(UserIndividualDishEntity::getUserId, userId).count();

        JSONObject result = new JSONObject();
        result.put("collectCount", collectCount);
        result.put("shareCount", shareCount);
        result.put("labelCount", labelCount);
        result.put("nutritionCount", nutritionCount);
        result.put("individualDishCount", individualDishCount);

        return ok(result);
    }

    @PostMapping("saveLabels")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse saveLabels(@RequestBody List<Long> labelIds) {
        UserEntity currentUser = SystemContextHelper.getCurrentUser();
        Long userId = currentUser.getId();

        if (userId == null) {
            return fail("userId不能为空");
        }

        long currentLabelCount = userLabelRelService.count(new LambdaQueryWrapper<UserLabelRelEntity>().eq(UserLabelRelEntity::getUserId, userId));
        Set<Long> existingLabelIds = userLabelRelService.list(new LambdaQueryWrapper<UserLabelRelEntity>().eq(UserLabelRelEntity::getUserId, userId)).stream().map(UserLabelRelEntity::getLabelId).collect(Collectors.toSet());

        Set<Long> newLabelIds = (labelIds == null) ? Collections.emptySet() : new HashSet<>(labelIds);

        long toAddCount = newLabelIds.stream().filter(id -> !existingLabelIds.contains(id)).count();
        List<Long> deleteLabelIds = existingLabelIds.stream().filter(id -> !newLabelIds.contains(id)).toList();

        if (currentLabelCount + toAddCount - deleteLabelIds.size() > 5) {
            return fail("用户标签总数不能超过5个");
        }
        if (!deleteLabelIds.isEmpty()) {
            userLabelRelService.lambdaUpdate().eq(UserLabelRelEntity::getUserId, userId).in(UserLabelRelEntity::getLabelId, deleteLabelIds).remove();
        }
        userLabelRelService.saveUserLabels(userId, labelIds);

        return ok("用户标签保存成功");
    }

    @PostMapping("delete")
    @Transactional(rollbackFor = Exception.class)
    public BaseResponse delete(@RequestBody JSONObject params) {
        List<Long> ids = params.getList("ids", Long.class);
        if (ids == null || ids.isEmpty()) {
            return fail("ids不能为空");
        }

        List<UserShareEntity> shareList = userShareService.lambdaQuery().in(UserShareEntity::getUserId, ids).list();
        List<Long> shareIds = shareList.stream().map(UserShareEntity::getId).toList();

        if (!shareIds.isEmpty()) {
            userShareCommentService.lambdaUpdate().in(UserShareCommentEntity::getUserShareId, shareIds).remove();
        }

        userShareCommentService.lambdaUpdate().in(UserShareCommentEntity::getUserId, ids).remove();
        userShareService.lambdaUpdate().in(UserShareEntity::getUserId, ids).remove();
        userDishCollectService.lambdaUpdate().in(UserDishCollectEntity::getUserId, ids).remove();
        userDietRecordService.lambdaUpdate().in(UserDietRecordEntity::getUserId, ids).remove();
        userIndividualDishService.lambdaUpdate().in(UserIndividualDishEntity::getUserId, ids).remove();
        userLabelRelService.lambdaUpdate().in(UserLabelRelEntity::getUserId, ids).remove();
        userNutritionRelService.lambdaUpdate().in(UserNutritionRelEntity::getUserId, ids).remove();
        dishAppraisesService.lambdaUpdate().in(DishAppraisesEntity::getUserId, ids).remove();
        dishCommentService.lambdaUpdate().in(DishCommentEntity::getUserId, ids).remove();
        userService.removeByIds(ids);
        return ok();
    }
}
