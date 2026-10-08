package com.hunre.quizservice.controller;

import com.hunre.quizservice.service.QuestionImportService;
import com.hunre.sharedcommon.dto.ApiResponse;
import com.hunre.sharedcommon.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/quizzes/{quizId}/questions/import")
@RequiredArgsConstructor
public class QuestionImportController {
    private final QuestionImportService imports;
    public record Imported(int imported) {}

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Imported>> importFile(@PathVariable Long quizId,
            @RequestPart("file") MultipartFile file, AuthenticatedUser user) {
        return ResponseEntity.status(201).body(ApiResponse.ok(new Imported(imports.importFile(quizId, file, user)),
                "Nhập câu hỏi thành công"));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template(@PathVariable Long quizId, AuthenticatedUser user) throws IOException {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mau-cau-hoi.csv\"")
                .body(imports.template(quizId, user));
    }
}
