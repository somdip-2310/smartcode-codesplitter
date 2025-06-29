package com.somdiproy.smartcode.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

public class CodeSplitterLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    
    private static final int MAX_CHUNK_SIZE = 30000; // Same as your existing
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("CodeSplitter invoked with: " + event);
        
        try {
            String analysisId = (String) event.get("analysisId");
            String code = (String) event.get("code");
            String language = (String) event.get("language");
            
            Map<String, Object> result = new HashMap<>();
            result.put("analysisId", analysisId);
            result.put("language", language);
            
            if (code.length() <= MAX_CHUNK_SIZE) {
                result.put("chunkCount", 0);
                result.put("chunks", new ArrayList<>());
                result.put("originalCode", code);
            } else {
                List<Map<String, Object>> chunks = splitIntoChunks(code, analysisId);
                result.put("chunkCount", chunks.size());
                result.put("chunks", chunks);
            }
            
            return result;
            
        } catch (Exception e) {
            context.getLogger().log("Error: " + e.getMessage());
            throw new RuntimeException("Failed to split code", e);
        }
    }
    
    private List<String> splitIntoChunks(String code, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        String[] lines = code.split("\n");
        StringBuilder currentChunk = new StringBuilder();
        
        for (String line : lines) {
            if (currentChunk.length() + line.length() + 1 > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(line).append("\n");
        }
        
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }
}