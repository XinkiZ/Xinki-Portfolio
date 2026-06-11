package com.xinki.portfolio.service;

import com.xinki.portfolio.config.RagConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentChunkService {

    private final RagConfig ragConfig;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".md", ".txt", ".markdown");

    /**
     * Parse uploaded file into text chunks.
     * Returns list of chunk strings, empty if file is invalid or unreadable.
     */
    public List<String> chunkFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || !isAllowed(filename)) {
            log.warn("Unsupported file type: {}", filename);
            return Collections.emptyList();
        }

        String ext = filename.substring(filename.lastIndexOf('.')).toLowerCase();
        String rawText;

        try {
            rawText = switch (ext) {
                case ".pdf" -> extractPdfText(file);
                case ".md", ".markdown" -> extractTextFile(file);
                case ".txt" -> extractTextFile(file);
                default -> null;
            };
        } catch (Exception e) {
            log.error("Failed to extract text from {}: {}", filename, e.getMessage());
            return Collections.emptyList();
        }

        if (rawText == null || rawText.trim().isEmpty()) {
            log.warn("No extractable text from {} (possibly scanned PDF)", filename);
            return Collections.emptyList();
        }

        List<String> chunks = switch (ext) {
            case ".md", ".markdown" -> splitMarkdown(rawText);
            default -> splitPlainText(rawText);
        };

        // Filter out too-short chunks
        chunks = chunks.stream()
                .filter(c -> c.trim().length() >= ragConfig.getMinChunkLength())
                .toList();

        // Cap at max chunks
        if (chunks.size() > ragConfig.getMaxChunksPerFile()) {
            chunks = chunks.subList(0, ragConfig.getMaxChunksPerFile());
        }

        return chunks;
    }

    /** Compute SHA-256 of file bytes for dedup. */
    public String computeHash(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(file.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            log.error("Failed to compute hash", e);
            return "";
        }
    }

    private boolean isAllowed(String filename) {
        String lower = filename.toLowerCase();
        return ALLOWED_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }

    private String extractPdfText(MultipartFile file) throws Exception {
        try (PDDocument doc = PDDocument.load(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }

    private String extractTextFile(MultipartFile file) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Split plain text by double-newline (paragraphs), with overlap.
     */
    private List<String> splitPlainText(String text) {
        List<String> paragraphs = new ArrayList<>();
        String[] parts = text.split("\\n\\s*\\n");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                // If a paragraph is too long, further split by sentences
                if (trimmed.length() > ragConfig.getMaxChunkLength()) {
                    paragraphs.addAll(splitLongText(trimmed));
                } else {
                    paragraphs.add(trimmed);
                }
            }
        }
        // Merge small adjacent chunks if possible
        return mergeSmallChunks(paragraphs);
    }

    /**
     * Split Markdown by headings (## / ###), protect code fences.
     */
    private List<String> splitMarkdown(String text) {
        List<String> chunks = new ArrayList<>();
        boolean inCodeBlock = false;
        StringBuilder current = new StringBuilder();

        for (String line : text.split("\\n")) {
            String trimmed = line.trim();
            // Toggle code fence
            if (trimmed.startsWith("```")) {
                inCodeBlock = !inCodeBlock;
                current.append(line).append("\n");
                continue;
            }
            // Heading boundary (outside code blocks)
            if (!inCodeBlock && (trimmed.startsWith("## ") || trimmed.startsWith("### "))) {
                if (current.length() > 0) {
                    String chunk = current.toString().trim();
                    if (chunk.length() >= ragConfig.getMinChunkLength()) {
                        chunks.add(chunk);
                    }
                    current = new StringBuilder();
                }
                current.append(line).append("\n");
                continue;
            }
            current.append(line).append("\n");
        }
        // Last chunk
        if (current.length() > 0) {
            String chunk = current.toString().trim();
            if (chunk.length() >= ragConfig.getMinChunkLength()) {
                chunks.add(chunk);
            }
        }
        // If any chunk is still too long, split further
        List<String> result = new ArrayList<>();
        for (String chunk : chunks) {
            if (chunk.length() > ragConfig.getMaxChunkLength()) {
                result.addAll(splitLongText(chunk));
            } else {
                result.add(chunk);
            }
        }
        return result;
    }

    /** Split overly long text by sentence boundaries. */
    private List<String> splitLongText(String text) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String[] sentences = text.split("(?<=[。！？.!?])\\s*");
        for (String sentence : sentences) {
            if (current.length() + sentence.length() > ragConfig.getMaxChunkLength() && current.length() > 0) {
                chunks.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(sentence);
        }
        if (current.length() > 0) chunks.add(current.toString().trim());
        return chunks;
    }

    /** Merge consecutive short chunks to reduce total chunk count. */
    private List<String> mergeSmallChunks(List<String> chunks) {
        List<String> result = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (String chunk : chunks) {
            if (buffer.length() + chunk.length() <= ragConfig.getMaxChunkLength()) {
                if (buffer.length() > 0) buffer.append("\n\n");
                buffer.append(chunk);
            } else {
                if (buffer.length() >= ragConfig.getMinChunkLength()) {
                    result.add(buffer.toString());
                }
                buffer = new StringBuilder(chunk);
            }
        }
        if (buffer.length() >= ragConfig.getMinChunkLength()) {
            result.add(buffer.toString());
        }
        return result.isEmpty() ? chunks : result;
    }
}