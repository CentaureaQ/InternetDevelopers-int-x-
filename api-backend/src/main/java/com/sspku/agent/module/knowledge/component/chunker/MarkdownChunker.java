package com.sspku.agent.module.knowledge.component.chunker;

import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class MarkdownChunker implements Chunker {

    private final TextChunker textChunker;

    public MarkdownChunker(TextChunker textChunker) {
        this.textChunker = textChunker;
    }

    @Override
    public List<String> chunk(String content, ChunkingConfig config) {
        // Split by headers (Level 1-3) using lookahead to keep the header
        String[] sections = content.split("(?m)(?=^#{1,3} )");
        
        List<String> finalChunks = new ArrayList<>();
        
        for (String section : sections) {
            if (section.trim().isEmpty()) continue;
            
            if (section.length() > config.getChunkSize()) {
                // If section is too big, use TextChunker on it
                finalChunks.addAll(textChunker.chunk(section, config));
            } else {
                // Merge small sections
                if (!finalChunks.isEmpty()) {
                    String lastChunk = finalChunks.get(finalChunks.size() - 1);
                    if (lastChunk.length() + section.length() < config.getChunkSize()) {
                        finalChunks.set(finalChunks.size() - 1, lastChunk + "\n" + section);
                    } else {
                        finalChunks.add(section);
                    }
                } else {
                    finalChunks.add(section);
                }
            }
        }
        
        return finalChunks;
    }
}
