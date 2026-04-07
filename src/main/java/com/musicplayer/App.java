package com.musicplayer;

import com.musicplayer.http.ApiHandler;
import com.musicplayer.http.LocalMediaHandler;
import com.musicplayer.http.StaticFileHandler;
import com.musicplayer.store.DataStore;
import com.musicplayer.store.DataStores;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

public final class App {
    private App() {
    }

    public static void main(String[] args) throws IOException {
        int preferredPort = Integer.parseInt(System.getenv().getOrDefault("MUSICPLAYER_PORT", "8080"));
        Path webRoot = Path.of("web").toAbsolutePath().normalize();
        Path uploadRoot = Path.of("media", "uploads").toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot.resolve("audio"));
        Files.createDirectories(uploadRoot.resolve("covers"));
        DataStore store = DataStores.createDefault();

        HttpServer server = createServer(preferredPort);
        int actualPort = server.getAddress().getPort();
        server.createContext("/api", new ApiHandler(store));
        server.createContext("/media/uploads", new LocalMediaHandler(uploadRoot));
        server.createContext("/", new StaticFileHandler(webRoot));
        server.setExecutor(Executors.newCachedThreadPool());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            store.close();
            server.stop(0);
        }));

        if (actualPort != preferredPort) {
            System.out.println("Port " + preferredPort + " is unavailable, switched to " + actualPort);
        }
        System.out.println("MusicPlayer server started on http://localhost:" + actualPort);
        System.out.println("Static files: " + webRoot);
        System.out.println("Upload root: " + uploadRoot);
        System.out.println("Data mode: " + store.description());
        server.start();
    }

    private static HttpServer createServer(int preferredPort) throws IOException {
        IOException lastError = null;
        for (int port = preferredPort; port < preferredPort + 20; port++) {
            try {
                return HttpServer.create(new InetSocketAddress(port), 0);
            } catch (BindException e) {
                lastError = e;
            }
        }
        throw new IOException("No available port in range: " + preferredPort + "-" + (preferredPort + 19), lastError);
    }
}