package com.musicplayer.http;

import com.musicplayer.model.PlayHistoryEntry;
import com.musicplayer.model.Playlist;
import com.musicplayer.model.Song;
import com.musicplayer.model.UserSession;
import com.musicplayer.store.DataStore;
import com.musicplayer.util.HttpUtil;
import com.musicplayer.util.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ApiHandler implements HttpHandler {
    private final DataStore store;
    private final Path uploadRoot = Path.of("media", "uploads").toAbsolutePath().normalize();

    public ApiHandler(DataStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            dispatch(exchange);
        } catch (IllegalArgumentException e) {
            writeJson(exchange, 400, error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            writeJson(exchange, 500, error("服务器错误: " + e.getMessage()));
        } finally {
            exchange.close();
        }
    }

    private void dispatch(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String apiPath = path.substring("/api".length());

        if ("OPTIONS".equalsIgnoreCase(method)) {
            HttpUtil.send(exchange, 204, "application/json; charset=UTF-8", new byte[0]);
            return;
        }

        if (apiPath.isBlank() || "/".equals(apiPath)) {
            writeJson(exchange, 200, Map.of("name", "MusicPlayer API", "status", "ok", "mode", store.description()));
            return;
        }

        if ("/health".equals(apiPath) && "GET".equalsIgnoreCase(method)) {
            writeJson(exchange, 200, Map.of("status", "ok", "mode", store.description()));
            return;
        }

        if ("/auth/login".equals(apiPath) && "POST".equalsIgnoreCase(method)) {
            Map<String, String> form = HttpUtil.parseForm(exchange.getRequestBody());
            UserSession session = store.login(required(form, "username"), required(form, "password"));
            writeJson(exchange, 200, success("登录成功", Map.of("user", userJson(session))));
            return;
        }

        if ("/auth/register".equals(apiPath) && "POST".equalsIgnoreCase(method)) {
            Map<String, String> form = HttpUtil.parseForm(exchange.getRequestBody());
            UserSession session = store.register(required(form, "username"), required(form, "password"));
            writeJson(exchange, 201, success("注册成功", Map.of("user", userJson(session))));
            return;
        }

        if ("/songs".equals(apiPath) && "GET".equalsIgnoreCase(method)) {
            writeJson(exchange, 200, success("ok", Map.of("songs", songsJson(store.listSongs()))));
            return;
        }

        if ("/songs/import".equals(apiPath) && "POST".equalsIgnoreCase(method)) {
            Song song = handleSongImport(exchange);
            writeJson(exchange, 201, success("歌曲导入成功", Map.of("song", songJson(song))));
            return;
        }

        if (apiPath.startsWith("/songs/") && "GET".equalsIgnoreCase(method)) {
            long songId = Long.parseLong(apiPath.substring("/songs/".length()));
            Song song = store.getSong(songId).orElseThrow(() -> new IllegalArgumentException("歌曲不存在"));
            writeJson(exchange, 200, success("ok", Map.of("song", songJson(song))));
            return;
        }

        if ("/history".equals(apiPath) && "GET".equalsIgnoreCase(method)) {
            long userId = requiredLong(exchange, "userId");
            writeJson(exchange, 200, success("ok", Map.of("history", historyJson(store.listHistory(userId)))));
            return;
        }

        if ("/history".equals(apiPath) && "POST".equalsIgnoreCase(method)) {
            Map<String, String> form = HttpUtil.parseForm(exchange.getRequestBody());
            long userId = Long.parseLong(required(form, "userId"));
            long songId = Long.parseLong(required(form, "songId"));
            store.recordPlayback(userId, songId);
            writeJson(exchange, 201, success("播放记录已保存", Map.of()));
            return;
        }

        if ("/playlists".equals(apiPath) && "GET".equalsIgnoreCase(method)) {
            long userId = requiredLong(exchange, "userId");
            writeJson(exchange, 200, success("ok", Map.of("playlists", playlistsJson(store.listPlaylists(userId)))));
            return;
        }

        if ("/playlists".equals(apiPath) && "POST".equalsIgnoreCase(method)) {
            Map<String, String> form = HttpUtil.parseForm(exchange.getRequestBody());
            long userId = Long.parseLong(required(form, "userId"));
            String name = required(form, "name");
            Playlist playlist = store.createPlaylist(userId, name);
            writeJson(exchange, 201, success("歌单创建成功", Map.of("playlist", playlistJson(playlist))));
            return;
        }

        if (apiPath.matches("/playlists/\\d+") && "DELETE".equalsIgnoreCase(method)) {
            long playlistId = Long.parseLong(apiPath.substring("/playlists/".length()));
            long userId = requiredLong(exchange, "userId");
            store.deletePlaylist(userId, playlistId);
            writeJson(exchange, 200, success("歌单删除成功", Map.of()));
            return;
        }

        if (apiPath.matches("/playlists/\\d+/songs") && "POST".equalsIgnoreCase(method)) {
            String[] parts = apiPath.split("/");
            long playlistId = Long.parseLong(parts[2]);
            Map<String, String> form = HttpUtil.parseForm(exchange.getRequestBody());
            long songId = Long.parseLong(required(form, "songId"));
            store.addSongToPlaylist(playlistId, songId);
            writeJson(exchange, 201, success("歌曲已加入歌单", Map.of()));
            return;
        }

        if (apiPath.matches("/playlists/\\d+/songs/\\d+") && "DELETE".equalsIgnoreCase(method)) {
            String[] parts = apiPath.split("/");
            long playlistId = Long.parseLong(parts[2]);
            long songId = Long.parseLong(parts[4]);
            store.removeSongFromPlaylist(playlistId, songId);
            writeJson(exchange, 200, success("歌曲已移出歌单", Map.of()));
            return;
        }

        writeJson(exchange, 404, error("接口不存在"));
    }

    private Song handleSongImport(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        Map<String, HttpUtil.MultipartPart> parts = HttpUtil.parseMultipart(exchange.getRequestBody(), contentType);
        String title = requiredText(parts, "title");
        String artist = requiredText(parts, "artist");
        String album = requiredText(parts, "album");
        int durationSeconds = optionalInt(parts.get("durationSeconds"), 0);
        HttpUtil.MultipartPart audioFile = requiredFile(parts, "audioFile", "请上传音频文件");
        HttpUtil.MultipartPart coverFile = requiredFile(parts, "coverFile", "请上传封面文件");

        Files.createDirectories(uploadRoot.resolve("audio"));
        Files.createDirectories(uploadRoot.resolve("covers"));

        SavedFile savedAudio = savePart(audioFile, uploadRoot.resolve("audio"), "/media/uploads/audio/");
        SavedFile savedCover = savePart(coverFile, uploadRoot.resolve("covers"), "/media/uploads/covers/");
        try {
            return store.importSong(title, artist, album, savedCover.url(), savedAudio.url(), durationSeconds);
        } catch (RuntimeException e) {
            Files.deleteIfExists(savedAudio.path());
            Files.deleteIfExists(savedCover.path());
            throw e;
        }
    }

    private SavedFile savePart(HttpUtil.MultipartPart part, Path directory, String urlPrefix) throws IOException {
        String extension = extensionOf(part.filename());
        String fileName = UUID.randomUUID() + extension;
        Path destination = directory.resolve(fileName).normalize();
        Files.write(destination, part.data());
        return new SavedFile(destination, urlPrefix + fileName);
    }

    private static String extensionOf(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return fileName.substring(dot).toLowerCase();
    }

    private static HttpUtil.MultipartPart requiredFile(Map<String, HttpUtil.MultipartPart> parts, String key, String message) {
        HttpUtil.MultipartPart part = parts.get(key);
        if (part == null || !part.hasFile()) {
            throw new IllegalArgumentException(message);
        }
        return part;
    }

    private static String requiredText(Map<String, HttpUtil.MultipartPart> parts, String key) {
        HttpUtil.MultipartPart part = parts.get(key);
        String value = part == null ? null : part.textValue();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("缺少参数: " + key);
        }
        return value;
    }

    private static int optionalInt(HttpUtil.MultipartPart part, int fallback) {
        if (part == null) {
            return fallback;
        }
        String value = part.textValue();
        if (value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("时长必须是数字");
        }
    }

    private static String required(Map<String, String> form, String key) {
        String value = form.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("缺少参数: " + key);
        }
        return value.trim();
    }

    private static long requiredLong(HttpExchange exchange, String key) {
        Map<String, String> query = HttpUtil.parseQuery(exchange.getRequestURI().getRawQuery());
        return Long.parseLong(Optional.ofNullable(query.get(key)).orElseThrow(() -> new IllegalArgumentException("缺少参数: " + key)));
    }

    private static Map<String, Object> success(String message, Map<String, Object> data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("message", message);
        payload.putAll(data);
        return payload;
    }

    private static Map<String, Object> error(String message) {
        return Map.of("success", false, "message", message);
    }

    private static Map<String, Object> userJson(UserSession session) {
        return Map.of(
            "id", session.id(),
            "username", session.username(),
            "displayName", session.displayName()
        );
    }

    private static List<Map<String, Object>> songsJson(List<Song> songs) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Song song : songs) {
            list.add(songJson(song));
        }
        return list;
    }

    private static Map<String, Object> songJson(Song song) {
        return Map.of(
            "id", song.id(),
            "title", song.title(),
            "artist", song.artist(),
            "album", song.album(),
            "coverUrl", song.coverUrl(),
            "audioUrl", song.audioUrl(),
            "durationSeconds", song.durationSeconds()
        );
    }

    private static List<Map<String, Object>> playlistsJson(List<Playlist> playlists) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Playlist playlist : playlists) {
            list.add(playlistJson(playlist));
        }
        return list;
    }

    private static Map<String, Object> playlistJson(Playlist playlist) {
        return Map.of(
            "id", playlist.id(),
            "name", playlist.name(),
            "userId", playlist.userId(),
            "songs", songsJson(playlist.songs())
        );
    }

    private static List<Map<String, Object>> historyJson(List<PlayHistoryEntry> history) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (PlayHistoryEntry item : history) {
            list.add(Map.of(
                "id", item.id(),
                "playedAt", item.playedAt(),
                "song", songJson(item.song())
            ));
        }
        return list;
    }

    private static void writeJson(HttpExchange exchange, int statusCode, Map<String, Object> payload) throws IOException {
        byte[] bytes = JsonUtil.stringify(payload).getBytes(StandardCharsets.UTF_8);
        HttpUtil.send(exchange, statusCode, "application/json; charset=UTF-8", bytes);
    }

    private record SavedFile(Path path, String url) {
    }
}