package com.sspku.agent.module.knowledge.controller;

import com.sspku.agent.common.api.ApiResponse;
import com.sspku.agent.module.knowledge.entity.KnowledgeBase;
import com.sspku.agent.module.knowledge.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    @PostMapping
    public ApiResponse<KnowledgeBase> createKnowledgeBase(@RequestBody KnowledgeBase knowledgeBase) {
        // TODO: Get current user ID from security context and set ownerId
        // knowledgeBase.setOwnerId(currentUserId);
        return ApiResponse.ok(knowledgeBaseService.createKnowledgeBase(knowledgeBase));
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeBase> updateKnowledgeBase(@PathVariable Long id, @RequestBody KnowledgeBase knowledgeBase) {
        knowledgeBase.setId(id);
        return ApiResponse.ok(knowledgeBaseService.updateKnowledgeBase(knowledgeBase));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteKnowledgeBase(@PathVariable Long id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBase> getKnowledgeBase(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeBaseService.getKnowledgeBaseById(id));
    }

    @GetMapping
    public ApiResponse<List<KnowledgeBase>> getAllKnowledgeBases() {
        return ApiResponse.ok(knowledgeBaseService.getAllKnowledgeBases());
    }
    
    @GetMapping("/my")
    public ApiResponse<List<KnowledgeBase>> getMyKnowledgeBases() {
        // TODO: Get current user ID from security context
        Long currentUserId = 1L; // Temporary mock
        return ApiResponse.ok(knowledgeBaseService.getKnowledgeBasesByOwner(currentUserId));
    }
}
