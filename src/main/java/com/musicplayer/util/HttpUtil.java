package com.musicplayer.util;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HttpUtil {
    private static final Pattern NAME_PATTERN = Pattern.compile("name=\"([^\"]+)\"");
    private static final Pattern FILENAME_PATTERN = Pattern.compile("filename=\"([^\"]*)\"");

    private HttpUtil() {
    }

    public static Map<String, String> parseForm(InputStream inputStream) throws IOException {
        String body = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        return parseQuery(body);
    }

    public static Map<String, String> parseQuery(String query) {
        Map<String, String> values = new LinkedHashMap<>();
        if (query == null || query.isBlank()) {
            return values;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            if (pair.isBlank()) {
                continue;
            }
            int separator = pair.indexOf('=');
            String key = separator >= 0 ? pair.substring(0, separator) : pair;
            String value = separator >= 0 ? pair.substring(separator + 1) : "";
            values.put(
                URLDecoder.decode(key, StandardCharsets.UTF_8),
                URLDecoder.decode(value, StandardCharsets.UTF_8)
            );
        }
        return values;
    }

    public static Map<String, MultipartPart> parseMultipart(InputStream inputStream, String contentType) throws IOException {
        String boundary = extractBoundary(contentType);
        if (boundary == null || boundary.isBlank()) {
            throw new IllegalArgumentException("无效的 multipart 请求");
        }

        byte[] bodyBytes = inputStream.readAllBytes();
        String body = new String(bodyBytes, StandardCharsets.ISO_8859_1);
        String delimiter = "--" + boundary;
        String[] rawParts = body.split(Pattern.quote(delimiter));
        Map<String, MultipartPart> parts = new LinkedHashMap<>();

        for (String rawPart : rawParts) {
            if (rawPart == null || rawPart.isBlank() || "--".equals(rawPart.trim())) {
                continue;
            }
            String normalized = rawPart.startsWith("\r\n") ? rawPart.substring(2) : rawPart;
            int headerEnd = normalized.indexOf("\r\n\r\n");
            if (headerEnd < 0) {
                continue;
            }

            String headerText = normalized.substring(0, headerEnd);
            String contentText = normalized.substring(headerEnd + 4);
            if (contentText.endsWith("\r\n")) {
                contentText = contentText.substring(0, contentText.length() - 2);
            }

            String disposition = null;
            String partContentType = "application/octet-stream";
            for (String line : headerText.split("\r\n")) {
                String lower = line.toLowerCase();
                if (lower.startsWith("content-disposition:")) {
                    disposition = line.substring(line.indexOf(':') + 1).trim();
                } else if (lower.startsWith("content-type:")) {
                    partContentType = line.substring(line.indexOf(':') + 1).trim();
                }
            }

            if (disposition == null) {
                continue;
            }

            String name = matchGroup(NAME_PATTERN, disposition);
            if (name == null || name.isBlank()) {
                continue;
            }
            String filename = matchGroup(FILENAME_PATTERN, disposition);
            byte[] data = contentText.getBytes(StandardCharsets.ISO_8859_1);
            parts.put(name, new MultipartPart(name, filename, partContentType, data));
        }

        return parts;
    }

    private static String extractBoundary(String contentType) {
        if (contentType == null) {
            return null;
        }
        for (String part : contentType.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("boundary=")) {
                return trimmed.substring("boundary=".length()).replace("\"", "");
            }
        }
        return null;
    }

    private static String matchGroup(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    public static void send(HttpExchange exchange, int statusCode, String contentType, byte[] body) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", contentType);
        headers.set("Cache-Control", "no-store");
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type");
        exchange.sendResponseHeaders(statusCode, body.length);
        exchange.getResponseBody().write(body);
    }

    public static String contentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }
        if (lower.endsWith(".js")) {
            return "text/javascript; charset=UTF-8";
        }
        if (lower.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }
        if (lower.endsWith(".json")) {
            return "application/json; charset=UTF-8";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".mp3")) {
            return "audio/mpeg";
        }
        if (lower.endsWith(".wav")) {
            return "audio/wav";
        }
        if (lower.endsWith(".ogg")) {
            return "audio/ogg";
        }
        if (lower.endsWith(".m4a")) {
            return "audio/mp4";
        }
        return "application/octet-stream";
    }

    public record MultipartPart(String name, String filename, String contentType, byte[] data) {
        public boolean hasFile() {
            return filename != null && !filename.isBlank() && data != null && data.length > 0;
        }

        public String textValue() {
            return new String(data, StandardCharsets.UTF_8).trim();
        }
    }
}