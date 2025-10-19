package org.acme;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import io.quarkus.runtime.Startup;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.nio.file.Path;
import java.util.List;

import static dev.langchain4j.data.document.splitter.DocumentSplitters.recursive;


@Singleton
@Startup
public class Ingestion {
    private static final Logger logger = Logger.getLogger(Ingestion.class);


    private static final int CHUNK_SIZE = 1200;     
    private static final int CHUNK_OVERLAP = 180;   

    public Ingestion(EmbeddingStore<TextSegment> store, EmbeddingModel embedding, BM25SearchEngine bm25Engine) {
        logger.info("Započinjem HYBRID ingestion proces (Embeddings + BM25)...");
        long startTime = System.currentTimeMillis();

        DocumentSplitter semanticSplitter = recursive(CHUNK_SIZE, CHUNK_OVERLAP);

        Path dir = Path.of("src/main/resources/documents");
        ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(dir, parser);

        logger.infof("Učitano %d dokumenata iz %s", documents.size(), dir);
        logger.infof("Chunk parametri: size=%d chars (~%d tokena), overlap=%d chars (%.1f%%)",
                    CHUNK_SIZE, CHUNK_SIZE/4, CHUNK_OVERLAP, (CHUNK_OVERLAP*100.0/CHUNK_SIZE));

        List<TextSegment> allSegments = new java.util.ArrayList<>();
        for (Document doc : documents) {
            allSegments.addAll(semanticSplitter.split(doc));
        }

        logger.infof("Napravljeno %d chunks", allSegments.size());

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(store)
                .embeddingModel(embedding)
                .documentSplitter(semanticSplitter)
                .build();

        ingestor.ingest(documents);
        logger.info("Semantic embeddings kreirani");

        bm25Engine.indexSegments(allSegments);
        logger.info("BM25 index kreiran");

        long duration = System.currentTimeMillis() - startTime;
        logger.infof("HYBRID ingestion završen za %.2f sekundi!", duration/1000.0);
        logger.info("Sistem spreman: BM25 (keyword) + Embeddings (semantic)");
    }
}
