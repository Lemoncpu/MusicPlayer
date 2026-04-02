package com.musicplayer.http;

import com.musicplayer.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LocalMediaHandler implements HttpHandler {
    private final Path root;

    public LocalMediaHandler(Path root) {
        this.root = root;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Path file = resolvePath(exchange.getRequestURI().getPath());
        if (file == null || !Files.exists(file) || Files.isDirectory(file)) {
            HttpUtil.send(exchange, 404, "application/json; charset=UTF-8", "{\"success\":false,\"message\":\"文件不存在\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            exchange.close();
            return;
        }
        byte[] bytes = Files.readAllBytes(file);
        HttpUtil.send(exchange, 200, HttpUtil.contentType(file.getFileName().toString()), bytes);
        exchange.close();
    }

    private Path resolvePath(String requestPath) {
        String prefix = "/media/uploads/";
        if (!requestPath.startsWith(prefix)) {
            return null;
        }
        String relative = requestPath.substring(prefix.length());
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            return null;
        }
        return resolved;
    }
}