package org.acme;

import dev.langchain4j.data.segment.TextSegment;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class BM25SearchEngine {
    private static final Logger logger = Logger.getLogger(BM25SearchEngine.class);

    private Directory directory;
    private Analyzer analyzer;
    private IndexWriter indexWriter;
    private List<TextSegment> allSegments;

    public BM25SearchEngine() {
        this.analyzer = new StandardAnalyzer();
        this.directory = new ByteBuffersDirectory();
        this.allSegments = new ArrayList<>();

        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            config.setSimilarity(new BM25Similarity()); // BM25 scoring
            this.indexWriter = new IndexWriter(directory, config);
            logger.info("BM25 Search Engine inicijalizovan");
        } catch (IOException e) {
            logger.error("Greška pri inicijalizaciji BM25: " + e.getMessage(), e);
        }
    }


    public void indexSegments(List<TextSegment> segments) {
        try {
            allSegments.clear();
            allSegments.addAll(segments);

            for (int i = 0; i < segments.size(); i++) {
                TextSegment segment = segments.get(i);
                Document doc = new Document();

                doc.add(new TextField("content", segment.text(), Field.Store.YES));
                doc.add(new TextField("id", String.valueOf(i), Field.Store.YES));

                indexWriter.addDocument(doc);
            }

            indexWriter.commit();
            logger.infof("BM25: Indeksirano %d chunks", segments.size());
        } catch (IOException e) {
            logger.error("Greška pri indeksiranju: " + e.getMessage(), e);
        }
    }

    public List<ScoredSegment> search(String queryStr, int topK) {
        List<ScoredSegment> results = new ArrayList<>();

        try (IndexReader reader = DirectoryReader.open(directory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(new BM25Similarity());

            QueryParser parser = new QueryParser("content", analyzer);
            Query query = parser.parse(queryStr);

            TopDocs topDocs = searcher.search(query, topK);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.storedFields().document(scoreDoc.doc);
                int id = Integer.parseInt(doc.get("id"));

                if (id < allSegments.size()) {
                    results.add(new ScoredSegment(allSegments.get(id), scoreDoc.score));
                }
            }

            logger.infof("BM25 pronašao %d chunks za: '%s'", results.size(), queryStr);
        } catch (Exception e) {
            logger.errorf("BM25 search greška: %s", e.getMessage());
        }

        return results;
    }

    public void close() {
        try {
            if (indexWriter != null) {
                indexWriter.close();
            }
        } catch (IOException e) {
            logger.error("Greška pri zatvaranju indexWriter: " + e.getMessage());
        }
    }

    public static class ScoredSegment {
        public final TextSegment segment;
        public final double score;

        public ScoredSegment(TextSegment segment, double score) {
            this.segment = segment;
            this.score = score;
        }
    }
}
