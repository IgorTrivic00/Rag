package org.acme;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;


@Singleton
public class HybridContentRetriever implements ContentRetriever {
    private static final Logger logger = Logger.getLogger(HybridContentRetriever.class);

    private final EmbeddingStoreContentRetriever embeddingRetriever;
    private final BM25SearchEngine bm25Engine;

    private static final double BM25_WEIGHT = 0.4;      
    private static final double SEMANTIC_WEIGHT = 0.6;  
    private final int maxResults;

    @Inject
    public HybridContentRetriever(EmbeddingStore<TextSegment> store,
                                   EmbeddingModel model,
                                   BM25SearchEngine bm25Engine) {
        this.maxResults = 10;
        this.bm25Engine = bm25Engine;

      
        this.embeddingRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(model)
                .maxResults(maxResults * 2) 
                .minScore(0.3) 
                .build();

        logger.info("Hybrid Retriever inicijalizovan (BM25 + Semantic)");
    }

    @Override
    public List<Content> retrieve(Query query) {
        String queryText = query.text();

        logger.infof(" HYBRID SEARCH za: '%s'", queryText);

       
        List<BM25SearchEngine.ScoredSegment> bm25Results = bm25Engine.search(queryText, maxResults * 2);

        
        List<Content> semanticResults = embeddingRetriever.retrieve(query);

       
        List<Content> mergedResults = mergeResults(bm25Results, semanticResults);

        logger.infof(" HYBRID: BM25=%d, Semantic=%d → Merged=%d chunks",
                bm25Results.size(), semanticResults.size(), mergedResults.size());

        return mergedResults.stream().limit(maxResults).collect(Collectors.toList());
    }


    private List<Content> mergeResults(List<BM25SearchEngine.ScoredSegment> bm25Results,
                                        List<Content> semanticResults) {
        Map<String, Double> scoreMap = new HashMap<>();
        Map<String, TextSegment> segmentMap = new HashMap<>();

        double maxBM25 = bm25Results.stream().mapToDouble(s -> s.score).max().orElse(1.0);

        for (BM25SearchEngine.ScoredSegment scored : bm25Results) {
            String text = scored.segment.text();
            double normalizedScore = scored.score / maxBM25;
            scoreMap.put(text, normalizedScore * BM25_WEIGHT);
            segmentMap.put(text, scored.segment);
        }

        for (Content content : semanticResults) {
            String text = content.textSegment().text();
            double existingScore = scoreMap.getOrDefault(text, 0.0);

            scoreMap.put(text, existingScore + (SEMANTIC_WEIGHT * 0.7)); 
            segmentMap.putIfAbsent(text, content.textSegment());
        }

        return scoreMap.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> Content.from(segmentMap.get(entry.getKey())))
                .collect(Collectors.toList());
    }
}
