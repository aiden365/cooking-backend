package com.cooking.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Configuration
public class SpringAiConfig {

    private static final int QWEN_EMBEDDING_DIMENSIONS = 1024;

    @Value("${spring.ai.dashscope.api-key}")
    private String dashscopeApiKey;
    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;
    @Value("${spring.ai.ollama.chat.model}")
    private String ollamaModel;
    @Value("${spring.ai.ollama.embedding.model}")
    private String ollamaEmbeddingModel;
    @Value("${spring.ai.vectorstore.redis.initialize-schema}")
    private Boolean vectorStoreInitializeSchema;
    @Value("${spring.ai.vectorstore.redis.dish.index-name}")
    private String dishVectorStoreIndexName;
    @Value("${spring.ai.vectorstore.redis.dish.prefix}")
    private String dishVectorStorePrefix;
    @Value("${spring.ai.vectorstore.redis.repository.index-name}")
    private String repositoryVectorStoreIndexName;
    @Value("${spring.ai.vectorstore.redis.repository.prefix}")
    private String repositoryVectorStorePrefix;

    @Bean(name = "deepseek")
    public ChatModel deepseek() {
        return DashScopeChatModel.builder().dashScopeApi(DashScopeApi.builder().apiKey(dashscopeApiKey).build())
                .defaultOptions(DashScopeChatOptions.builder().model("deepseek-v3").build()).build();
    }

    @Bean(name = "qwen")
    public ChatModel qwen() {
        return DashScopeChatModel.builder().dashScopeApi(DashScopeApi.builder().apiKey(dashscopeApiKey).build()).defaultOptions(DashScopeChatOptions.builder().model("qwen-plus").build()).build();
    }


    @Bean(name = "qwenEmbedding")
    public EmbeddingModel qwenEmbedding() {
        return DashScopeEmbeddingModel.builder().dashScopeApi(DashScopeApi.builder().apiKey(dashscopeApiKey).build())
                .defaultOptions(DashScopeEmbeddingOptions.builder().model("text-embedding-v3")
                        .dimensions(QWEN_EMBEDDING_DIMENSIONS).build())
                .build();
    }

    @Bean(name = "ollamaQwen")
    public ChatModel ollamaQwen() {
        return OllamaChatModel.builder().ollamaApi(OllamaApi.builder().baseUrl(ollamaBaseUrl).build())
                .defaultOptions(OllamaChatOptions.builder().model(ollamaModel).numCtx(1024).build()).build();

    }

    @Bean(name = "ollamaQwenEmbedding")
    public OllamaEmbeddingModel ollamaQwenEmbedding() {
        return OllamaEmbeddingModel.builder().ollamaApi(OllamaApi.builder().baseUrl(ollamaBaseUrl).build())
                .defaultOptions(OllamaEmbeddingOptions.builder().model(ollamaEmbeddingModel).build()).build();
    }

    @Bean(name = "ollamaQwenClient")
    public ChatClient ollamaQwenClient(@Qualifier("ollamaQwen") ChatModel chatModel) {
        return ChatClient.builder(chatModel).defaultOptions(ChatOptions.builder().model(ollamaModel).build()).build();
    }

    @Bean(name = "dishVectorStore")
    public VectorStore dishVectorStore(JedisConnectionFactory jedisConnectionFactory,
            @Qualifier("qwenEmbedding") EmbeddingModel embeddingModel) {
        JedisPooled jedisPooled = jedisPooled(jedisConnectionFactory);
        boolean recreatedByDimensionMismatch = ensureVectorIndexDimension(jedisPooled, dishVectorStoreIndexName,
                QWEN_EMBEDDING_DIMENSIONS);
        RedisVectorStore.Builder builder = RedisVectorStore.builder(jedisPooled, embeddingModel);
        // 维度不一致时即使全局开关为 false，也要重建索引并基于现有 document 重新索引。
        builder.initializeSchema(Boolean.TRUE.equals(vectorStoreInitializeSchema) || recreatedByDimensionMismatch);
        builder.indexName(dishVectorStoreIndexName);
        builder.prefix(dishVectorStorePrefix);
        builder.metadataFields(
                RedisVectorStore.MetadataField.tag("dish_id"),
                RedisVectorStore.MetadataField.tag("dish_name"));
        builder.batchingStrategy(new TokenCountBatchingStrategy());

        return builder.build();
    }

    @Bean(name = "repositoryVectorStore")
    public VectorStore repositoryVectorStore(JedisConnectionFactory jedisConnectionFactory,
            @Qualifier("qwenEmbedding") EmbeddingModel embeddingModel) {
        JedisPooled jedisPooled = jedisPooled(jedisConnectionFactory);
        boolean recreatedByDimensionMismatch = ensureVectorIndexDimension(jedisPooled, repositoryVectorStoreIndexName, QWEN_EMBEDDING_DIMENSIONS);
        RedisVectorStore.Builder builder = RedisVectorStore.builder(jedisPooled, embeddingModel);
        // 维度不一致时即使全局开关为 false，也要重建索引并基于现有 document 重新索引。
        builder.initializeSchema(Boolean.TRUE.equals(vectorStoreInitializeSchema) || recreatedByDimensionMismatch);
        builder.indexName(repositoryVectorStoreIndexName);
        builder.prefix(repositoryVectorStorePrefix);
        // 可选：定义用于过滤的元数据字段
        builder.metadataFields(
                RedisVectorStore.MetadataField.tag("repository_id"),
                RedisVectorStore.MetadataField.numeric("type"));
        // 可选：默认为 TokenCountBatchingStrategy
        builder.batchingStrategy(new TokenCountBatchingStrategy());

        return builder.build();
    }

    private JedisPooled jedisPooled(JedisConnectionFactory jedisConnectionFactory) {
        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder();
        builder.ssl(jedisConnectionFactory.isUseSsl());
        builder.clientName(jedisConnectionFactory.getClientName());
        builder.timeoutMillis(jedisConnectionFactory.getTimeout());
        builder.password(jedisConnectionFactory.getPassword());
        DefaultJedisClientConfig clientConfig = builder.build();

        return new JedisPooled(new HostAndPort(jedisConnectionFactory.getHostName(), jedisConnectionFactory.getPort()),
                clientConfig);
    }

    private boolean ensureVectorIndexDimension(JedisPooled jedisPooled, String indexName, int expectedDimension) {
        try {
            Map<String, Object> indexInfo = jedisPooled.ftInfo(indexName);
            Integer actualDimension = extractVectorDimension(indexInfo);
            if (actualDimension != null && !Objects.equals(actualDimension, expectedDimension)) {
                log.warn(
                        "Redis vector index dimension mismatch, dropping index. indexName={}, actualDimension={}, expectedDimension={}",
                        indexName, actualDimension, expectedDimension);
                jedisPooled.ftDropIndex(indexName);
                return true;
            }
        } catch (Exception e) {
            String message = e.getMessage();
            if (message != null && message.toLowerCase().contains("unknown index name")) {
                return false;
            }
            log.warn("Failed to inspect Redis vector index, continue with existing schema. indexName={}", indexName, e);
        }
        return false;
    }

    private Integer extractVectorDimension(Map<String, Object> indexInfo) {
        if (indexInfo == null || indexInfo.isEmpty()) {
            return null;
        }

        Object attributes = indexInfo.get("attributes");
        if (!(attributes instanceof List<?> attributeList)) {
            return null;
        }

        for (Object attribute : attributeList) {
            Map<String, Object> attributeMap = toFlatMap(attribute);
            if ("VECTOR".equalsIgnoreCase(String.valueOf(attributeMap.get("type")))) {
                Object dimension = attributeMap.get("dim");
                if (dimension instanceof Number number) {
                    return number.intValue();
                }
                if (dimension != null) {
                    try {
                        return Integer.parseInt(dimension.toString());
                    } catch (NumberFormatException ignored) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    private Map<String, Object> toFlatMap(Object value) {
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Object> result = new LinkedHashMap<>();
            rawMap.forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue));
            return result;
        }
        if (!(value instanceof List<?> listValue)) {
            return Map.of();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i + 1 < listValue.size(); i += 2) {
            result.put(String.valueOf(listValue.get(i)), listValue.get(i + 1));
        }
        return result;
    }

}
