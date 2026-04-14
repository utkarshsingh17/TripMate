package com.utkarsh.tripmate.service;

import com.utkarsh.tripmate.config.properties.RagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final VectorStore vectorStore;
    private final List<ToolCallback> availableToolCallbackList;
    private final RagProperties ragProperties;

    public RagService(
            VectorStore vectorStore,
            List<ToolCallback> availableToolCallbackList,
            RagProperties ragProperties) {
        this.vectorStore = vectorStore;
        this.availableToolCallbackList = availableToolCallbackList == null
                ? Collections.emptyList()
                : availableToolCallbackList;
        this.ragProperties = ragProperties;
    }

    public List<ToolCallback> getRagCandidateToolCallbackList(String query, Integer userSuppliedTopK) {
        Set<String> candidateToolNames = getRagCandidateToolNameSet(query, userSuppliedTopK);
        if (candidateToolNames.isEmpty()) {
            return Collections.emptyList();
        }
        return availableToolCallbackList.stream()
                .filter(tc -> candidateToolNames.contains(tc.getToolDefinition().name()))
                .collect(Collectors.toList());
    }

    public List<ToolCallback> getAvailableToolCallbackList() {
        return availableToolCallbackList;
    }

    private Set<String> getRagCandidateToolNameSet(String query, Integer userSuppliedTopK) {
        Map<String, String> toolMetadata = availableToolCallbackList.stream()
                .collect(Collectors.toMap(
                        tc -> tc.getToolDefinition().name(),
                        tc -> tc.getToolDefinition().description() == null ? "" : tc.getToolDefinition().description(),
                        (first, second) -> first));

        if (toolMetadata.isEmpty()) {
            return Collections.emptySet();
        }

        embedToolMetadata(toolMetadata);
        int topK = Math.min(
                toolMetadata.size(),
                (userSuppliedTopK == null || userSuppliedTopK <= 0) ? ragProperties.getTopK() : userSuppliedTopK);

        return findTopKToolNames(query, topK);
    }

    private void embedToolMetadata(Map<String, String> toolMetadata) {
        FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
        int inserted = 0;

        for (Map.Entry<String, String> entry : toolMetadata.entrySet()) {
            String toolName = entry.getKey();
            String toolDescription = entry.getValue();

            List<Document> existing = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(toolDescription)
                    .filterExpression(filterBuilder.eq("name", toolName).build())
                    .topK(1)
                    .build());

            if (ragProperties.isDeletePreviousRelatedEmbeddings() && existing != null && !existing.isEmpty()) {
                vectorStore.delete(existing.stream().map(Document::getId).toList());
                existing = Collections.emptyList();
            }

            if (existing == null || existing.isEmpty()) {
                vectorStore.add(List.of(new Document(toolDescription, Map.of("name", toolName))));
                inserted++;
            }
        }

        log.info("RAG tool metadata processed: total={}, newlyEmbedded={}", toolMetadata.size(), inserted);
    }

    private Set<String> findTopKToolNames(String query, int topK) {
        if (query == null || query.isBlank()) {
            return Collections.emptySet();
        }

        List<Document> docs = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(ragProperties.getSimilarityThreshold())
                .build());

        if (docs == null || docs.isEmpty()) {
            log.warn("No RAG candidates found for threshold={} and query='{}'",
                    ragProperties.getSimilarityThreshold(), query);
            return Collections.emptySet();
        }

        List<String> toolNames = new ArrayList<>();
        for (Document doc : docs) {
            Object name = doc.getMetadata().get("name");
            if (name instanceof String toolName && !toolName.isBlank()) {
                toolNames.add(toolName);
            }
        }
        return new HashSet<>(toolNames);
    }
}
