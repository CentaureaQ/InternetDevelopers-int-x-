package com.sspku.agent.module.knowledge.component.chunker;

import org.springframework.stereotype.Component;

@Component
public class ChunkerFactory {
    
    private final TextChunker textChunker;
    private final MarkdownChunker markdownChunker;

    public ChunkerFactory(TextChunker textChunker, MarkdownChunker markdownChunker) {
        this.textChunker = textChunker;
        this.markdownChunker = markdownChunker;
    }

    public Chunker getChunker(String strategy) {
        if ("markdown_aware".equalsIgnoreCase(strategy)) {
            return markdownChunker;
        }
        return textChunker;
    }
}
