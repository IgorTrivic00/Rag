package org.acme;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.util.function.Supplier;


@Singleton
public class Retriever implements Supplier<RetrievalAugmentor> {
    private static final Logger logger = Logger.getLogger(Retriever.class);

 
    private static final int MAX_RESULTS = 10;        
    private static final double MIN_SCORE = 0.50;      

    private final DefaultRetrievalAugmentor augmentor;

    public Retriever(EmbeddingStore<TextSegment> store, EmbeddingModel embeddingModel) {
        logger.info(" Inicijalizujem ULTIMATE RAG Pipeline...");

        ChunkLogger chunkLogger = new ChunkLogger(store, embeddingModel, MAX_RESULTS, MIN_SCORE);

        augmentor = DefaultRetrievalAugmentor
                .builder()
                .contentRetriever(chunkLogger)
                .build();

        logger.infof(" ULTIMATE RAG Pipeline konfigurisan:");
        logger.infof("    Embedding: Snowflake Arctic 768-dim");
        logger.infof("   Retrieval: top %d, minScore %.2f (maksimalna preciznost)",
                    MAX_RESULTS, MIN_SCORE);
        logger.infof("    LLM: Granite 3.3:8b (RAG-optimized)");
        logger.infof("    Chunks: 1200 chars (~300 tokena)");
    }

    @Override
    public RetrievalAugmentor get() {
        return augmentor;
    }
}
