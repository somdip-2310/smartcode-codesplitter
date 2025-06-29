// File: src/main/java/com/somdiproy/smartcode/lambda/models/SplitResult.java
package com.somdiproy.smartcode.lambda.models;

import java.util.List;
import java.util.Map;

/**
 * Result model for code splitting
 */
public class SplitResult {
    private String analysisId;
    private String language;
    private int totalSize;
    private int chunkCount;
    private boolean needsChunking;
    private List<Map<String, Object>> chunks;
    private String originalCode;
    private Map<String, Object> metadata;
    
    // Constructors
    public SplitResult() {}
    
    // Getters and setters
    public String getAnalysisId() {
        return analysisId;
    }
    
    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public int getTotalSize() {
        return totalSize;
    }
    
    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }
    
    public int getChunkCount() {
        return chunkCount;
    }
    
    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }
    
    public boolean isNeedsChunking() {
        return needsChunking;
    }
    
    public void setNeedsChunking(boolean needsChunking) {
        this.needsChunking = needsChunking;
    }
    
    public List<Map<String, Object>> getChunks() {
        return chunks;
    }
    
    public void setChunks(List<Map<String, Object>> chunks) {
        this.chunks = chunks;
    }
    
    public String getOriginalCode() {
        return originalCode;
    }
    
    public void setOriginalCode(String originalCode) {
        this.originalCode = originalCode;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}