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

    public Retriever(HybridContentRetriever hybridRetriever) {
        logger.info("Inicijalizujem HYBRID RAG Pipeline (BM25 + Semantic)...");

        augmentor = DefaultRetrievalAugmentor
                .builder()
                .contentRetriever(hybridRetriever)
                .build();

        logger.infof("HYBRID RAG Pipeline konfigurisan:");
        logger.infof(" BM25: Keyword search (40%% weight)");
        logger.infof("Semantic: Embedding search (60%% weight)");
        logger.infof("Model: AllMiniLM-L6-v2 (384-dim)");
        logger.infof("LLM: claude-sonnet-4-20250514");
        logger.infof("Chunks: 1200 chars (~300 tokena)");
    }

    @Override
    public RetrievalAugmentor get() {
        return augmentor;
    }
}
