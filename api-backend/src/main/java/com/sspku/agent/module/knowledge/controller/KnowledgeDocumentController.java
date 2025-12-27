package com.sspku.agent.module.knowledge.controller;

import com.sspku.agent.common.api.ApiResponse;
import com.sspku.agent.module.knowledge.entity.KnowledgeDocument;
import com.sspku.agent.module.knowledge.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService documentService;

    @GetMapping("/knowledge-bases/{kbUuid}/documents")
    public ApiResponse<List<KnowledgeDocument>> getDocuments(
            @PathVariable String kbUuid,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(documentService.getDocuments(kbUuid, status));
    }

    @PostMapping("/knowledge-bases/{kbUuid}/documents")
    public ApiResponse<KnowledgeDocument> uploadDocument(
            @PathVariable String kbUuid,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(documentService.uploadDocument(kbUuid, file));
    }

    @GetMapping("/documents/{uuid}")
    public ApiResponse<KnowledgeDocument> getDocument(@PathVariable String uuid) {
        return ApiResponse.ok(documentService.getDocument(uuid));
    }

    @DeleteMapping("/documents/{uuid}")
    public ApiResponse<Void> deleteDocument(@PathVariable String uuid) {
        documentService.deleteDocument(uuid);
        return ApiResponse.ok(null);
    }
}
