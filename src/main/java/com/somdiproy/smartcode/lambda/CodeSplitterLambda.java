package com.somdiproy.smartcode.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

/**
 * CodeSplitterLambda - Splits code into manageable chunks for analysis
 * 
 * This Lambda function is responsible for:
 * 1. Validating input code
 * 2. Determining if chunking is needed
 * 3. Splitting code into appropriate chunks
 * 4. Preparing data for parallel processing
 * 
 * @author Somdip Roy
 */
public class CodeSplitterLambda implements RequestHandler<Map<String, Object>, Map<String, Object>> {
    
	// Smaller chunks to reduce Bedrock processing time
	private static final int MAX_CHUNK_SIZE = Integer.parseInt(System.getenv().getOrDefault("MAX_CHUNK_SIZE", "20000"));
    private static final String S3_BUCKET_NAME = System.getenv().getOrDefault("S3_BUCKET_NAME", "smartcode-uploads");
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AmazonS3 s3Client;
    
    public CodeSplitterLambda() {
        this.s3Client = AmazonS3ClientBuilder.standard().build();
    }
    
    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        context.getLogger().log("CodeSplitter invoked with event: " + event);
        
        try {
            // Extract input parameters
            String analysisId = (String) event.get("analysisId");
            String language = (String) event.get("language");
            
            // Validate required parameters
            if (analysisId == null || analysisId.isEmpty()) {
                throw new IllegalArgumentException("analysisId is required");
            }
            
            // Get code content
            String code = getCodeContent(event, context);
            
            if (code == null || code.isEmpty()) {
                throw new IllegalArgumentException("Code content is empty");
            }
            
            // Prepare result
            Map<String, Object> result = new HashMap<>();
            result.put("analysisId", analysisId);
            result.put("language", language != null ? language : detectLanguage(code));
            result.put("totalSize", code.length());
            
            // Determine if chunking is needed
            if (shouldChunk(code)) {
                List<Map<String, Object>> chunks = splitIntoChunks(code, analysisId);
                result.put("chunkCount", chunks.size());
                result.put("chunks", chunks);
                result.put("needsChunking", true);
                context.getLogger().log("Code split into " + chunks.size() + " chunks");
            } else {
                result.put("chunkCount", 0);
                result.put("chunks", new ArrayList<>());
                result.put("originalCode", code);
                result.put("needsChunking", false);
                context.getLogger().log("Code does not need chunking, size: " + code.length());
            }
            
            // Add metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("processedAt", new Date().toString());
            metadata.put("maxChunkSize", MAX_CHUNK_SIZE);
            metadata.put("lambdaRequestId", context.getAwsRequestId());
            result.put("metadata", metadata);
            
            return result;
            
        } catch (Exception e) {
            context.getLogger().log("Error in CodeSplitter: " + e.getMessage());
            e.printStackTrace();
            
            // Return error result
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", true);
            errorResult.put("errorMessage", e.getMessage());
            errorResult.put("errorType", e.getClass().getSimpleName());
            return errorResult;
        }
    }
    
    /**
     * Get code content from event (direct or from S3)
     */
    private String getCodeContent(Map<String, Object> event, Context context) {
        // Check if code is provided directly
        if (event.containsKey("code")) {
            return (String) event.get("code");
        }
        
        // Check if code is in S3
        if (event.containsKey("s3Key")) {
            String s3Key = (String) event.get("s3Key");
            context.getLogger().log("Fetching code from S3: " + s3Key);
            return s3Client.getObjectAsString(S3_BUCKET_NAME, s3Key);
        }
        
        // Check if code location is specified
        if (event.containsKey("codeLocation") && "s3".equals(event.get("codeLocation"))) {
            String s3Key = (String) event.get("s3Key");
            if (s3Key != null) {
                return s3Client.getObjectAsString(S3_BUCKET_NAME, s3Key);
            }
        }
        
        return null;
    }
    
    /**
     * Determine if code needs to be chunked
     */
    private boolean shouldChunk(String code) {
        return code.length() > MAX_CHUNK_SIZE;
    }
    
    /**
     * Detect programming language from code content
     */
    private String detectLanguage(String code) {
        // Simple language detection based on common patterns
        if (code.contains("public class") || code.contains("import java.")) {
            return "java";
        } else if (code.contains("def ") || code.contains("import ") && code.contains("from ")) {
            return "python";
        } else if (code.contains("function ") || code.contains("const ") || code.contains("let ")) {
            return "javascript";
        } else if (code.contains("#include") || code.contains("int main()")) {
            return "c";
        } else if (code.contains("using namespace") || code.contains("std::")) {
            return "cpp";
        } else if (code.contains("package main") || code.contains("func ")) {
            return "go";
        }
        return "unknown";
    }
    
    /**
     * Split code into chunks maintaining code structure integrity
     */
    private List<Map<String, Object>> splitIntoChunks(String code, String analysisId) {
        List<Map<String, Object>> chunks = new ArrayList<>();
        String[] lines = code.split("\n", -1); // -1 to preserve empty lines
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 0;
        int lineNumber = 1;
        int chunkStartLine = 1;
        
        // Track code structure depth (braces, parentheses)
        int braceDepth = 0;
        int parenDepth = 0;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            // Update structure depth
            braceDepth += countChar(line, '{') - countChar(line, '}');
            parenDepth += countChar(line, '(') - countChar(line, ')');
            
            // Check if adding this line would exceed chunk size
            boolean wouldExceedSize = currentChunk.length() + line.length() + 1 > MAX_CHUNK_SIZE;
            boolean hasContent = currentChunk.length() > 0;
            boolean atStructuralBoundary = braceDepth == 0 && parenDepth == 0;
            
            if (wouldExceedSize && hasContent && atStructuralBoundary) {
                // Create chunk
                Map<String, Object> chunk = createChunk(
                    currentChunk.toString(),
                    analysisId,
                    chunkIndex,
                    chunkStartLine,
                    lineNumber - 1
                );
                chunks.add(chunk);
                
                // Reset for next chunk
                currentChunk = new StringBuilder();
                chunkIndex++;
                chunkStartLine = lineNumber;
            }
            
            // Add line to current chunk
            currentChunk.append(line);
            if (i < lines.length - 1) {
                currentChunk.append("\n");
            }
            lineNumber++;
        }
        
        // Add remaining content as final chunk
        if (currentChunk.length() > 0) {
            Map<String, Object> chunk = createChunk(
                currentChunk.toString(),
                analysisId,
                chunkIndex,
                chunkStartLine,
                lineNumber - 1
            );
            chunks.add(chunk);
        }
        
        return chunks;
    }
    
    /**
     * Create a chunk object with metadata
     */
    private Map<String, Object> createChunk(String code, String analysisId, int index, int startLine, int endLine) {
        Map<String, Object> chunk = new HashMap<>();
        chunk.put("chunkId", analysisId + "-chunk-" + index);
        chunk.put("chunkIndex", index);
        chunk.put("code", code);
        chunk.put("analysisId", analysisId);
        chunk.put("startLine", startLine);
        chunk.put("endLine", endLine);
        chunk.put("size", code.length());
        chunk.put("lineCount", endLine - startLine + 1);
        return chunk;
    }
    
    /**
     * Count occurrences of a character in a string
     */
    private int countChar(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }
}