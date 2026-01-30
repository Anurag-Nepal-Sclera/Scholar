package com.scholar.service.cv;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Service for extracting text from various document formats.
 * Supports PDF and DOCX formats.
 */
@Service
@Slf4j
public class DocumentTextExtractor {

    /**
     * Extracts text from a PDF document.
     * 
     * @param fileBytes the PDF file as byte array
     * @return extracted text
     */
    public String extractFromPdf(byte[] fileBytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(fileBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.debug("Extracted {} characters from PDF", text.length());
            return text;
        } catch (Exception e) {
            log.error("Failed to extract text from PDF", e);
            throw new IOException("PDF text extraction failed", e);
        }
    }

    /**
     * Extracts text from a DOCX document.
     * 
     * @param fileBytes the DOCX file as byte array
     * @return extracted text
     */
    public String extractFromDocx(byte[] fileBytes) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes);
             XWPFDocument document = new XWPFDocument(bis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            log.debug("Extracted {} characters from DOCX", text.length());
            return text;
        } catch (Exception e) {
            log.error("Failed to extract text from DOCX", e);
            throw new IOException("DOCX text extraction failed", e);
        }
    }

    /**
     * Extracts text based on MIME type and performs basic cleaning.
     */
    public String extractText(byte[] fileBytes, String mimeType) throws IOException {
        String text = switch (mimeType) {
            case "application/pdf" -> extractFromPdf(fileBytes);
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> extractFromDocx(fileBytes);
            case "application/msword" -> throw new IOException("Legacy DOC format not supported. Please use DOCX.");
            default -> throw new IOException("Unsupported file type: " + mimeType);
        };
        
        return cleanText(text);
    }

    private String cleanText(String text) {
        if (text == null) return "";
        // Remove multiple spaces, tabs, and unnecessary newlines
        // Preserve some structure for section awareness
        return text.replaceAll("[\\t\\r]+", " ")
                  .replaceAll(" {2,}", " ")
                  .replaceAll("\\n{3,}", "\n\n")
                  .trim();
    }
}
