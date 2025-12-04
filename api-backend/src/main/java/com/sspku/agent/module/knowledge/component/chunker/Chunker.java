package com.sspku.agent.module.knowledge.component.chunker;

import java.util.List;

public interface Chunker {
    List<String> chunk(String content, ChunkingConfig config);
}
