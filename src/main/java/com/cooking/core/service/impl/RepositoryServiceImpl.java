package com.cooking.core.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cooking.base.BaseServiceImpl;
import com.cooking.core.entity.RepositoryEntity;
import com.cooking.core.mapper.RepositoryMapper;
import com.cooking.core.service.RepositoryService;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Repository service impl
 * </p>
 *
 * @author aiden
 * @since 2026-02-03
 */
@Service
public class RepositoryServiceImpl extends BaseServiceImpl<RepositoryMapper, RepositoryEntity> implements RepositoryService {

    private static final String RECIPE_TITLE_PREFIX = "菜名：";
    private static final Pattern BLANK_LINE_PATTERN = Pattern.compile("\\n\\s*\\n+");
    private static final int SEMANTIC_CHUNK_MAX_LENGTH = 800;

    @Autowired
    private RepositoryMapper repositoryMapper;
    @Resource(name = "repositoryVectorStore")
    private VectorStore repositoryVectorStore;
    @Autowired
    private TokenTextSplitter tokenTextSplitter;

    @Override
    public List<RepositoryEntity> findList(Map<String, Object> params) {
        return repositoryMapper.findPage(new Page<>(0, -1), params).getRecords();
    }

    @Override
    public IPage<RepositoryEntity> findPage(IPage<RepositoryEntity> page, Map<String, Object> params) {
        return repositoryMapper.findPage(page, params);
    }

    @Override
    public void deleteByIds(Set<String> ids) {
        removeByIds(ids);
    }

    @Override
    public void saveToVectorStore(RepositoryEntity repositoryEntity) {
        saveToVectorStoreInternal(repositoryEntity);
    }

    @Override
    public void deleteFromVectorStore(Collection<Long> repositoryIds) {
        if (repositoryIds == null || repositoryIds.isEmpty()) {
            return;
        }

        List<String> chunkIdsToDelete = new ArrayList<>();
        for (Long repositoryId : repositoryIds) {
            if (repositoryId == null) {
                continue;
            }
            repositoryVectorStore.delete(new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder().eq("repository_id", repositoryId.toString()).build());
            /*List<Document> existingDocuments = repositoryVectorStore.similaritySearch(SearchRequest.builder().query("").filterExpression(new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder().eq("repository_id", repositoryId.toString()).build()).build());
            if (existingDocuments != null && !existingDocuments.isEmpty()) {
                chunkIdsToDelete.addAll(existingDocuments.stream().map(Document::getId).filter(StringUtils::hasText).toList());
            }*/
        }

        if (!chunkIdsToDelete.isEmpty()) {
            repositoryVectorStore.delete(chunkIdsToDelete);
        }
    }

    @Override
    public Map<String, Object> rebuildAllVectorStore() {
        List<RepositoryEntity> repositoryEntities = list();
        int chunkCount = 0;
        for (RepositoryEntity repositoryEntity : repositoryEntities) {
            chunkCount += saveToVectorStoreInternal(repositoryEntity);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("repositoryCount", repositoryEntities.size());
        result.put("chunkCount", chunkCount);
        return result;
    }

    private int saveToVectorStoreInternal(RepositoryEntity repositoryEntity) {
        if (repositoryEntity == null || repositoryEntity.getId() == null) {
            return 0;
        }

        deleteFromVectorStore(List.of(repositoryEntity.getId()));

        List<Document> documents = buildRepositoryDocuments(repositoryEntity);
        if (documents.isEmpty()) {
            return 0;
        }

        repositoryVectorStore.add(documents);
        return documents.size();
    }

    private List<Document> buildRepositoryDocuments(RepositoryEntity repositoryEntity) {
        String normalizedContent = normalizeContent(repositoryEntity.getContent());
        if (!StringUtils.hasText(normalizedContent)) {
            return Collections.emptyList();
        }

        List<String> semanticChunks = splitSemanticChunks(repositoryEntity.getType(), normalizedContent);
        if (semanticChunks.isEmpty()) {
            return Collections.emptyList();
        }

        List<Document> documents = new ArrayList<>();
        for (String semanticChunk : semanticChunks) {
            if (!StringUtils.hasText(semanticChunk)) {
                continue;
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("type", repositoryEntity.getType());
            metadata.put("repository_id", repositoryEntity.getId().toString());

            String documentText = buildVectorText(repositoryEntity, semanticChunk);
            Document document = new Document(documentText, metadata);
            if (documentText.length() > SEMANTIC_CHUNK_MAX_LENGTH) {
                documents.addAll(tokenTextSplitter.apply(List.of(document)));
            } else {
                documents.add(document);
            }
        }

        return documents;
    }

    private String buildVectorText(RepositoryEntity repositoryEntity, String semanticChunk) {
        if (!Objects.equals(repositoryEntity.getType(), 2)) {
            return semanticChunk;
        }

        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(repositoryEntity.getName())) {
            parts.add("标题: " + repositoryEntity.getName().trim());
        }
        if (StringUtils.hasText(repositoryEntity.getDescription())) {
            parts.add("描述: " + repositoryEntity.getDescription().trim());
        }
        parts.add("正文: " + semanticChunk.trim());
        return String.join("\n", parts);
    }

    private List<String> splitSemanticChunks(Integer type, String content) {
        if (Objects.equals(type, 1)) {
            return splitRecipeKnowledge(content);
        }
        if (Objects.equals(type, 2)) {
            return splitNutritionKnowledge(content);
        }
        return splitByBlankLine(content);
    }

    private List<String> splitRecipeKnowledge(String content) {
        List<String> chunksByPrefix = splitByPrefix(content, RECIPE_TITLE_PREFIX);
        if (!chunksByPrefix.isEmpty()) {
            return chunksByPrefix;
        }
        return splitByBlankLine(content);
    }

    private List<String> splitNutritionKnowledge(String content) {
        List<String> chunks = splitByBlankLine(content);
        if (!chunks.isEmpty()) {
            return chunks;
        }
        return List.of(content);
    }

    private List<String> splitByPrefix(String content, String prefix) {
        List<Integer> startIndexes = new ArrayList<>();
        int searchIndex = 0;
        while (searchIndex < content.length()) {
            int foundIndex = content.indexOf(prefix, searchIndex);
            if (foundIndex < 0) {
                break;
            }
            startIndexes.add(foundIndex);
            searchIndex = foundIndex + prefix.length();
        }

        if (startIndexes.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> chunks = new ArrayList<>();
        if (startIndexes.get(0) > 0) {
            String leading = content.substring(0, startIndexes.get(0)).trim();
            if (StringUtils.hasText(leading)) {
                chunks.add(leading);
            }
        }

        for (int i = 0; i < startIndexes.size(); i++) {
            int start = startIndexes.get(i);
            int end = i + 1 < startIndexes.size() ? startIndexes.get(i + 1) : content.length();
            String chunk = content.substring(start, end).trim();
            if (StringUtils.hasText(chunk)) {
                chunks.add(chunk);
            }
        }

        return chunks;
    }

    private List<String> splitByBlankLine(String content) {
        Matcher matcher = BLANK_LINE_PATTERN.matcher(content);
        if (!matcher.find()) {
            return StringUtils.hasText(content) ? List.of(content.trim()) : Collections.emptyList();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        matcher.reset();
        while (matcher.find()) {
            String chunk = content.substring(start, matcher.start()).trim();
            if (StringUtils.hasText(chunk)) {
                chunks.add(chunk);
            }
            start = matcher.end();
        }

        String tail = content.substring(start).trim();
        if (StringUtils.hasText(tail)) {
            chunks.add(tail);
        }
        return chunks;
    }

    private String normalizeContent(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        return content.replace("\r\n", "\n").replace('\r', '\n').trim();
    }
}
