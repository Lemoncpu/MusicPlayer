package com.musicplayer.http;

import com.musicplayer.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class StaticFileHandler implements HttpHandler {
    private final Path webRoot;

    public StaticFileHandler(Path webRoot) {
        this.webRoot = webRoot;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String rawPath = exchange.getRequestURI().getPath();
        Path requested = resolvePath(rawPath);
        Path fileToServe = Files.exists(requested) && !Files.isDirectory(requested)
            ? requested
            : webRoot.resolve("index.html");

        byte[] bytes = Files.readAllBytes(fileToServe);
        HttpUtil.send(exchange, 200, HttpUtil.contentType(fileToServe.getFileName().toString()), bytes);
        exchange.close();
    }

    private Path resolvePath(String rawPath) {
        String normalized = rawPath.equals("/") ? "/index.html" : rawPath;
        Path relative = Path.of(normalized.substring(1)).normalize();
        Path resolved = webRoot.resolve(relative).normalize();
        if (!resolved.startsWith(webRoot)) {
            return webRoot.resolve("index.html");
        }
        return resolved;
    }
}
