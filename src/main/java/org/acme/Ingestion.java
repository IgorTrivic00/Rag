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

    public Ingestion(EmbeddingStore<TextSegment> store, EmbeddingModel embedding) {
        logger.info("
         Započinjem OPTIMIZOVANI ingestion proces...");
        long startTime = System.currentTimeMillis();


        DocumentSplitter semanticSplitter = recursive(CHUNK_SIZE, CHUNK_OVERLAP);

        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(store)
                .embeddingModel(embedding)
                .documentSplitter(semanticSplitter)
                .build();


        Path dir = Path.of("src/main/resources/documents");
        ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();
        List<Document> documents = FileSystemDocumentLoader.loadDocuments(dir, parser);

        logger.infof(" Ucitano %d dokumenata iz %s", documents.size(), dir);
        logger.infof("  Chunk parametri: size=%d chars (~%d tokena), overlap=%d chars (%.1f%%)",
                    CHUNK_SIZE, CHUNK_SIZE/4, CHUNK_OVERLAP, (CHUNK_OVERLAP*100.0/CHUNK_SIZE));


        ingestor.ingest(documents);

        long duration = System.currentTimeMillis() - startTime;
        logger.infof(" Ingestion zavrsen za %.2f sekundi!", duration/1000.0);
        logger.info(" Vektorska baza spremna za semantic search!");
    }
}
