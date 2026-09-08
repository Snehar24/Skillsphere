package com.skillsphere.common.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Service
public class DocumentTextExtractionService {

    public String extractText(String filename, byte[] fileBytes) {
        if (filename == null || filename.trim().isEmpty()) {
            return new String(fileBytes, StandardCharsets.UTF_8);
        }
        String ext = "";
        int dotIdx = filename.lastIndexOf(".");
        if (dotIdx >= 0) {
            ext = filename.substring(dotIdx + 1).toLowerCase();
        }
        
        try {
            switch (ext) {
                case "pdf":
                    try (PDDocument document = PDDocument.load(new ByteArrayInputStream(fileBytes))) {
                        PDFTextStripper stripper = new PDFTextStripper();
                        return stripper.getText(document);
                    }
                case "docx":
                    try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(fileBytes));
                         XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                        return extractor.getText();
                    }
                case "txt":
                default:
                    return new String(fileBytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to process the uploaded document: " + e.getMessage(), e);
        }
    }
}
