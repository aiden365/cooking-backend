package com.cooking.api;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseEntity;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.RepositoryEntity;
import com.cooking.core.entity.UserEntity;
import com.cooking.core.service.RepositoryService;
import com.cooking.core.service.UserService;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * Repository controller
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
@RestController
@RequestMapping("repository")
public class RepositoryApi extends BaseController {

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private UserService userService;

    @PostMapping("list")
    public BaseResponse list(@RequestBody JSONObject params) {
        String search = params.getString("search");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("search", search);
        List<RepositoryEntity> entityList = repositoryService.findList(params);
        return ok(entityList);
    }

    @PostMapping("page")
    public BaseResponse page(@RequestBody JSONObject params) {
        IPage<RepositoryEntity> entityIPage = repositoryService.findPage(new Page<>(pageNo, pageSize), params);
        Map<Long, UserEntity> userEntityMap = userService.findMapByIds(entityIPage.getRecords().stream().map(BaseEntity::getCreateUser).collect(Collectors.toSet()));
        for (RepositoryEntity record : entityIPage.getRecords()) {
            record.setCreatorName(Optional.ofNullable(userEntityMap.get(record.getCreateUser())).map(UserEntity::getUserName).orElse(""));
        }
        return ok(entityIPage);
    }

    @PostMapping("detail")
    public BaseResponse detail(@RequestBody JSONObject params) {
        Long id = params.getLong("id");
        if (!UserEntity.validId(id)) {
            return fail("id不能为空");
        }
        RepositoryEntity repositoryEntity = repositoryService.lambdaQuery().eq(RepositoryEntity::getId, id).list().stream().findAny().orElse(null);
        if (repositoryEntity == null) {
            return fail("用户不存在");
        }

        return ok(repositoryEntity);
    }

    @PostMapping("save")
    public BaseResponse save(@RequestBody RepositoryEntity repositoryEntity) {
        if (!StringUtils.hasText(repositoryEntity.getName())) {
            return fail("名称不能为空");
        }
        if (repositoryEntity.getType() == null) {
            return fail("知识类型不能为空");
        }
        if (!StringUtils.hasText(repositoryEntity.getDescription())) {
            return fail("描述不能为空");
        }
        if (!StringUtils.hasText(repositoryEntity.getContent())) {
            return fail("内容不能为空");
        }

        repositoryService.saveOrUpdate(repositoryEntity);
        repositoryService.saveToVectorStore(repositoryEntity);

        return ok();
    }

    @PostMapping("delete")
    public BaseResponse delete(@RequestBody JSONObject params) {
        List<Long> ids = params.getList("ids", Long.class);
        if (ids == null || ids.isEmpty()) {
            return fail("id列表不能为空");
        }

        repositoryService.deleteFromVectorStore(ids);
        repositoryService.removeByIds(ids);

        return ok();
    }

    @RequestMapping("rebuildVectorStore")
    public BaseResponse rebuildVectorStore() {
        return ok(repositoryService.rebuildAllVectorStore());
    }
}
