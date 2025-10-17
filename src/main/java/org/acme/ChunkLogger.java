package org.acme;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Singleton
@Alternative
public class ChunkLogger implements ContentRetriever {
    private static final Logger logger = Logger.getLogger(ChunkLogger.class);
    private static final String LOG_FILE = "chunks-log.txt";

    private final EmbeddingStoreContentRetriever delegate;
    private final int maxResults;
    private final double minScore;

    public ChunkLogger(EmbeddingStore<TextSegment> store,
                      EmbeddingModel model,
                      int maxResults,
                      double minScore) {
        this.maxResults = maxResults;
        this.minScore = minScore;

        this.delegate = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(model)
                .embeddingStore(store)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();

        logger.infof(" ChunkLogger enabled - logging to %s", LOG_FILE);
    }

    @Override
    public List<Content> retrieve(Query query) {
        List<Content> contents = delegate.retrieve(query);

        // Logiraj u fajl
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            writer.println("═══════════════════════════════════════════════════════════════");
            writer.println("TIMESTAMP: " + timestamp);
            writer.println("PITANJE: " + query.text());
            writer.println("PRONADJENO: " + contents.size() + " chunks (minScore=" + minScore + ")");
            writer.println("═══════════════════════════════════════════════════════════════");
            writer.println();

            int chunkNum = 1;
            for (Content content : contents) {
                String text = content.textSegment().text();

                writer.println("─────────────────────────────────────────────────────────────");
                writer.println("CHUNK #" + chunkNum);
                writer.println("─────────────────────────────────────────────────────────────");
                writer.println(text);
                writer.println();
                writer.println("Duzina: " + text.length() + " karaktera");
                writer.println();

                chunkNum++;
            }

            writer.println("\n\n");
            writer.flush();

        } catch (IOException e) {
            logger.errorf("Greska pri pisanju u %s: %s", LOG_FILE, e.getMessage());
        }

        // Logiraj i u konzolu (kratko)
        logger.infof(" Izvuceno %d chunks za pitanje: '%s'", contents.size(), query.text());
        logger.infof("   ↳ Logovi zapisani u: %s", LOG_FILE);

        return contents;
    }
}
