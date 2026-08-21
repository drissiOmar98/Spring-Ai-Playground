package com.omar.toolsearch.configs;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Vector store configuration used for two purposes in this demo:
 *
 * <ol>
 *   <li>Backing the Tool Search advisor's semantic {@code ToolIndex}, which embeds
 *       each registered tool's name/description so the model can find relevant
 *       tools by natural-language query instead of loading all of them upfront.</li>
 * </ol>
 *
 * <p>{@link SimpleVectorStore} is an in-memory store — perfect for a demo, but
 * swap it for a persistent store (e.g. pgvector) in production so the tool
 * index survives restarts.
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}