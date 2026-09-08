package com.skillsphere.certificate.controller;

import com.skillsphere.certificate.dto.CertificateResponse;
import com.skillsphere.certificate.service.CertificateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateVerificationController {

    private final CertificateService certificateService;

    @GetMapping("/verify/{code}")
    public ResponseEntity<CertificateResponse> verifyCertificate(@PathVariable String code) {
        return ResponseEntity.ok(certificateService.verifyCertificate(code));
    }
}
