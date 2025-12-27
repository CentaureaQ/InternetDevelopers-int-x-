package com.sspku.agent.module.knowledge.component.chunker;

import lombok.Data;
import java.util.Arrays;
import java.util.List;

@Data
public class ChunkingConfig {
    private int chunkSize = 800;
    private int chunkOverlap = 50;
    private String chunkStrategy = "sliding_window";
    private List<String> separators = Arrays.asList("\n\n", "\n", "。", ".", " ");
}
