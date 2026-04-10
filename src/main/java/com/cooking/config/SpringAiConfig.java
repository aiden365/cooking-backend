package com.cooking.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
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

@Configuration
public class SpringAiConfig {

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
    public ChatModel deepseek(){
        return DashScopeChatModel.builder().dashScopeApi(DashScopeApi.builder().apiKey(dashscopeApiKey).build()).defaultOptions(DashScopeChatOptions.builder().withModel("deepseek-v3").build()).build();
    }

    @Bean(name = "qwen")
    public ChatModel qwen(){
        return DashScopeChatModel.builder().dashScopeApi(DashScopeApi.builder().apiKey(dashscopeApiKey).build()).defaultOptions(DashScopeChatOptions.builder().withModel("qwen-plus").build()).build();
    }

    @Bean(name = "qwenEmbedding")
    public EmbeddingModel qwenEmbedding(){
        return DashScopeEmbeddingModel.builder().dashScopeApi(DashScopeApi.builder().apiKey(dashscopeApiKey).build()).defaultOptions(DashScopeEmbeddingOptions.builder().withModel("text-embedding-v3").build()).build();
    }

    @Bean(name = "ollamaQwen")
    public ChatModel ollamaQwen(){
        return OllamaChatModel.builder().ollamaApi(OllamaApi.builder().baseUrl(ollamaBaseUrl).build()).defaultOptions(OllamaChatOptions.builder().model(ollamaModel).build()).build();

    }
    @Bean(name = "ollamaQwenEmbedding")
    public OllamaEmbeddingModel ollamaQwenEmbedding(){
        return OllamaEmbeddingModel.builder().ollamaApi(OllamaApi.builder().baseUrl(ollamaBaseUrl).build()).defaultOptions(OllamaEmbeddingOptions.builder().model(ollamaEmbeddingModel).build()).build();
    }

    @Bean(name = "ollamaQwenClient")
    public ChatClient ollamaQwenClient(@Qualifier("ollamaQwen") ChatModel chatModel){
        return ChatClient.builder(chatModel).defaultOptions(ChatOptions.builder().model(ollamaModel).build()).build();
    }

    @Bean(name = "dishVectorStore")
    public VectorStore dishVectorStore(JedisConnectionFactory jedisConnectionFactory, @Qualifier("qwenEmbedding") EmbeddingModel embeddingModel) {
        RedisVectorStore.Builder builder = RedisVectorStore.builder(jedisPooled(jedisConnectionFactory), embeddingModel);
        builder.initializeSchema(vectorStoreInitializeSchema);
        builder.indexName(dishVectorStoreIndexName);
        builder.prefix(dishVectorStorePrefix);
        builder.metadataFields(
                RedisVectorStore.MetadataField.tag("dishId"),
                RedisVectorStore.MetadataField.tag("dishName")
        );
        builder.batchingStrategy(new TokenCountBatchingStrategy());

        return builder.build();
    }

    @Bean(name = "repositoryVectorStore")
    public VectorStore repositoryVectorStore(JedisConnectionFactory jedisConnectionFactory, @Qualifier("qwenEmbedding") EmbeddingModel embeddingModel) {
        RedisVectorStore.Builder builder = RedisVectorStore.builder(jedisPooled(jedisConnectionFactory), embeddingModel);
        builder.initializeSchema(vectorStoreInitializeSchema);
        builder.indexName(repositoryVectorStoreIndexName);
        builder.prefix(repositoryVectorStorePrefix);
        // 可选：定义用于过滤的元数据字段
        builder.metadataFields(
                RedisVectorStore.MetadataField.tag("repository_id"),
                RedisVectorStore.MetadataField.numeric("type")
        );
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

        return new JedisPooled(new HostAndPort(jedisConnectionFactory.getHostName(), jedisConnectionFactory.getPort()),clientConfig);
    }

}
