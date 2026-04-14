package com.utkarsh.tripmate.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private int topK = 5;
    private double similarityThreshold = 0.1;
    private boolean deletePreviousRelatedEmbeddings = false;

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public boolean isDeletePreviousRelatedEmbeddings() {
        return deletePreviousRelatedEmbeddings;
    }

    public void setDeletePreviousRelatedEmbeddings(boolean deletePreviousRelatedEmbeddings) {
        this.deletePreviousRelatedEmbeddings = deletePreviousRelatedEmbeddings;
    }
}
