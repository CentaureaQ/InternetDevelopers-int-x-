package com.sspku.agent.module.knowledge.component.chunker;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class TextChunker implements Chunker {

    @Override
    public List<String> chunk(String content, ChunkingConfig config) {
        return splitText(content, config.getSeparators(), config.getChunkSize(), config.getChunkOverlap());
    }

    private List<String> splitText(String text, List<String> separators, int chunkSize, int chunkOverlap) {
        List<String> finalChunks = new ArrayList<>();
        
        // Base case: text fits in chunk
        if (text.length() <= chunkSize) {
            finalChunks.add(text);
            return finalChunks;
        }
        
        String separator = "";
        boolean separatorFound = false;
        
        // 1. Find the first separator that exists in the text
        for (String sep : separators) {
            if (text.contains(sep)) {
                separator = sep;
                separatorFound = true;
                break;
            }
        }

        // 2. No separator found, force split
        if (!separatorFound) {
            return forceSplit(text, chunkSize, chunkOverlap);
        }

        // 3. Split
        String[] parts = text.split(Pattern.quote(separator));
        List<String> goodParts = new ArrayList<>();
        for (String p : parts) {
            if (!p.isEmpty()) {
                goodParts.add(p);
            }
        }
        
        // 4. Merge parts into chunks
        List<String> currentChunkParts = new ArrayList<>();
        int currentLength = 0;
        
        for (String part : goodParts) {
            // We assume we'll join with separator, so account for its length
            int partLen = part.length() + (currentChunkParts.isEmpty() ? 0 : separator.length());
            
            if (currentLength + partLen > chunkSize) {
                // Current chunk is full
                if (!currentChunkParts.isEmpty()) {
                    String chunk = String.join(separator, currentChunkParts);
                    
                    // If this single chunk is still too big (because one part was huge), recurse
                    if (chunk.length() > chunkSize) {
                        // Find next separators
                        int sepIndex = separators.indexOf(separator);
                        List<String> nextSeparators = (sepIndex + 1 < separators.size()) ? 
                                                      separators.subList(sepIndex + 1, separators.size()) : 
                                                      new ArrayList<>();
                        finalChunks.addAll(splitText(chunk, nextSeparators, chunkSize, chunkOverlap));
                    } else {
                        finalChunks.add(chunk);
                    }
                    
                    // Handle Overlap
                    int overlapLen = 0;
                    List<String> overlapParts = new ArrayList<>();
                    for (int i = currentChunkParts.size() - 1; i >= 0; i--) {
                        String p = currentChunkParts.get(i);
                        int pLen = p.length() + (overlapParts.isEmpty() ? 0 : separator.length());
                        if (overlapLen + pLen < chunkOverlap) {
                            overlapParts.add(0, p);
                            overlapLen += pLen;
                        } else {
                            break;
                        }
                    }
                    currentChunkParts = new ArrayList<>(overlapParts);
                    currentLength = overlapLen;
                }
            }
            
            currentChunkParts.add(part);
            currentLength += part.length() + (currentChunkParts.size() > 1 ? separator.length() : 0);
        }
        
        // Add remaining
        if (!currentChunkParts.isEmpty()) {
            String chunk = String.join(separator, currentChunkParts);
             if (chunk.length() > chunkSize) {
                 int sepIndex = separators.indexOf(separator);
                 List<String> nextSeparators = (sepIndex + 1 < separators.size()) ? 
                                               separators.subList(sepIndex + 1, separators.size()) : 
                                               new ArrayList<>();
                 finalChunks.addAll(splitText(chunk, nextSeparators, chunkSize, chunkOverlap));
             } else {
                 finalChunks.add(chunk);
             }
        }

        return finalChunks;
    }
    
    private List<String> forceSplit(String text, int chunkSize, int chunkOverlap) {
        List<String> chunks = new ArrayList<>();
        int step = chunkSize - chunkOverlap;
        if (step <= 0) step = 1; // Prevent infinite loop
        
        for (int i = 0; i < text.length(); i += step) {
            int end = Math.min(i + chunkSize, text.length());
            chunks.add(text.substring(i, end));
            if (end == text.length()) break;
        }
        return chunks;
    }
}
