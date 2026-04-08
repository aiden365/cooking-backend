package com.cooking.api;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseController;
import com.cooking.base.BaseResponse;
import com.cooking.core.entity.RepositoryEntity;
import com.cooking.core.service.RepositoryService;
import jakarta.annotation.Resource;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * Repository controller
 * </p>
 *
 * @author aiden
 * @since 2026-03-04
 */
@RestController
@RequestMapping("repository")
public class RepositoryApi extends BaseController {

    @Autowired
    private RepositoryService repositoryService;
    @Resource(name = "redisVectorStore")
    private VectorStore redisVectorStore;
    @Autowired
    private TokenTextSplitter tokenTextSplitter;

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
        int pageNo = params.getIntValue("pageNo");
        int pageSize = params.getIntValue("pageSize");
        IPage<RepositoryEntity> page = new Page<>(pageNo, pageSize);

        String search = params.getString("search");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("search", search);
        IPage<RepositoryEntity> entityIPage = repositoryService.findPage(page, params);
        return ok(entityIPage);
    }

    @PostMapping("save")
    public BaseResponse save(@RequestBody RepositoryEntity repositoryEntity) {
        if (!StringUtils.hasText(repositoryEntity.getName())) {
            return fail("名称不能为空");
        }
        if (repositoryEntity.getType() == null) {
            return fail("知识类型不能为空");
        }
        repositoryService.saveOrUpdate(repositoryEntity);

        // 1. First, delete any existing chunks for this repository entry to handle updates cleanly.
        List<Document> existingDocuments = redisVectorStore.similaritySearch(SearchRequest.builder().query("").filterExpression(new FilterExpressionBuilder().eq("repository_id", repositoryEntity.getId().toString()).build()).build());
        if (existingDocuments != null && !existingDocuments.isEmpty()){
            List<String> idsToDelete = existingDocuments.stream().map(Document::getId).collect(Collectors.toList());
            redisVectorStore.delete(idsToDelete);
        }

        // 2. Now, create and split the new document.
        String content = repositoryEntity.getName() + "。 " + (repositoryEntity.getDescription() == null ? "" : repositoryEntity.getDescription());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", repositoryEntity.getType());
        metadata.put("repository_id", repositoryEntity.getId().toString()); // Add original ID to metadata
        Document document = new Document(content, metadata);

        // Use a TokenTextSplitter to chunk the document
        List<Document> chunks = tokenTextSplitter.apply(List.of(document));
        // 3. Add the new chunks to the vector store
        redisVectorStore.add(chunks);

        return ok();
    }

    @PostMapping("delete")
    public BaseResponse delete(@RequestBody JSONObject params) {
        List<Long> ids = params.getList("ids", Long.class);
        if (ids == null || ids.isEmpty()) {
            return fail("id列表不能为空");
        }

        // Find all chunk IDs associated with the repository IDs and delete them from the vector store.
        List<String> chunkIdsToDelete = new ArrayList<>();
        for (Long repoId : ids) {
            List<Document> documentsToDelete = redisVectorStore.similaritySearch(SearchRequest.builder().filterExpression(new FilterExpressionBuilder().eq("repository_id", repoId.toString()).build()).build());
            if (documentsToDelete != null && !documentsToDelete.isEmpty()) {
                chunkIdsToDelete.addAll(documentsToDelete.stream().map(Document::getId).toList());
            }
        }

        if (!chunkIdsToDelete.isEmpty()) {
            redisVectorStore.delete(chunkIdsToDelete);
        }

        // Finally, delete the entries from the main database.
        repositoryService.removeByIds(ids);

        return ok();
    }
}
