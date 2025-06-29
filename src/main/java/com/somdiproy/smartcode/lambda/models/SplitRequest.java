// File: src/main/java/com/somdiproy/smartcode/lambda/models/SplitRequest.java
package com.somdiproy.smartcode.lambda.models;

import java.util.Map;

/**
 * Request model for code splitting
 */
public class SplitRequest {
    private String analysisId;
    private String code;
    private String language;
    private String s3Key;
    private String codeLocation;
    private Map<String, Object> metadata;
    
    // Constructors
    public SplitRequest() {}
    
    public SplitRequest(String analysisId, String code, String language) {
        this.analysisId = analysisId;
        this.code = code;
        this.language = language;
    }
    
    // Getters and setters
    public String getAnalysisId() {
        return analysisId;
    }
    
    public void setAnalysisId(String analysisId) {
        this.analysisId = analysisId;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getS3Key() {
        return s3Key;
    }
    
    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }
    
    public String getCodeLocation() {
        return codeLocation;
    }
    
    public void setCodeLocation(String codeLocation) {
        this.codeLocation = codeLocation;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}

