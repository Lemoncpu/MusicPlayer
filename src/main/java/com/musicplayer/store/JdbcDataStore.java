package com.musicplayer.store;

import com.musicplayer.model.PlayHistoryEntry;
import com.musicplayer.model.Playlist;
import com.musicplayer.model.Song;
import com.musicplayer.model.UserSession;
import com.musicplayer.util.PasswordUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JdbcDataStore implements DataStore {
    private final String url;
    private final String user;
    private final String password;

    public JdbcDataStore(String url, String user, String password) throws SQLException {
        this.url = url;
        this.user = user;
        this.password = password;
        initializeSchema();
        resetLegacyUsersIfNeeded();
        initializeFixedLibrary();
    }

    @Override
    public UserSession register(String username, String passwordValue) {
        PasswordUtil.requireValidPassword(passwordValue);
        try (Connection connection = open()) {
            try (PreparedStatement check = connection.prepareStatement("SELECT id FROM app_user WHERE username = ?")) {
                check.setString(1, username);
                try (ResultSet resultSet = check.executeQuery()) {
                    if (resultSet.next()) {
                        throw new IllegalArgumentException("Username already exists");
                    }
                }
            }

            String displayName = "User " + username;
            String passwordHash = PasswordUtil.hashPassword(passwordValue);
            try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO app_user(username, password, display_name) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            )) {
                insert.setString(1, username);
                insert.setString(2, passwordHash);
                insert.setString(3, displayName);
                insert.executeUpdate();
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (keys.next()) {
                        return new UserSession(keys.getLong(1), username, displayName);
                    }
                }
            }
            throw new IllegalStateException("Register failed");
        } catch (SQLException e) {
            throw new IllegalStateException("Database operation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public UserSession login(String username, String passwordValue) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, username, password, display_name FROM app_user WHERE username = ?"
             )) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("User not registered");
                }
                String storedPassword = resultSet.getString("password");
                if (!PasswordUtil.verifyPassword(passwordValue, storedPassword)) {
                    throw new IllegalArgumentException("Wrong password");
                }
                return new UserSession(
                    resultSet.getLong("id"),
                    resultSet.getString("username"),
                    resultSet.getString("display_name")
                );
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database query failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Song> listSongs() {
        List<Song> songs = new ArrayList<>();
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, title, artist, album, cover_url, audio_url, duration_seconds FROM song ORDER BY id"
             );
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                songs.add(mapSong(resultSet));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Song query failed: " + e.getMessage(), e);
        }
        return songs;
    }

    @Override
    public Optional<Song> getSong(long songId) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, title, artist, album, cover_url, audio_url, duration_seconds FROM song WHERE id = ?"
             )) {
            statement.setLong(1, songId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapSong(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Song query failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Song importSong(String title, String artist, String album, String coverUrl, String audioUrl, int durationSeconds) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO song(title, artist, album, cover_url, audio_url, duration_seconds) VALUES (?, ?, ?, ?, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS
             )) {
            statement.setString(1, title);
            statement.setString(2, artist);
            statement.setString(3, album);
            statement.setString(4, coverUrl);
            statement.setString(5, audioUrl);
            statement.setInt(6, Math.max(0, durationSeconds));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Song(keys.getLong(1), title, artist, album, coverUrl, audioUrl, Math.max(0, durationSeconds));
                }
            }
            throw new IllegalStateException("Import song failed");
        } catch (SQLException e) {
            throw new IllegalStateException("Database operation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Playlist> listPlaylists(long userId) {
        Map<Long, PlaylistBuilder> playlistMap = new LinkedHashMap<>();
        String sql = """
            SELECT p.id AS playlist_id, p.user_id, p.name,
                   s.id AS song_id, s.title, s.artist, s.album, s.cover_url, s.audio_url, s.duration_seconds
            FROM playlist p
            LEFT JOIN playlist_song ps ON p.id = ps.playlist_id
            LEFT JOIN song s ON s.id = ps.song_id
            WHERE p.user_id = ?
            ORDER BY p.id, ps.id
            """;
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long playlistId = resultSet.getLong("playlist_id");
                    PlaylistBuilder builder = playlistMap.computeIfAbsent(
                        playlistId,
                        id -> new PlaylistBuilder(id, safeLong(resultSet, "user_id"), safeString(resultSet, "name"))
                    );
                    long songId = resultSet.getLong("song_id");
                    if (!resultSet.wasNull()) {
                        builder.songs.add(new Song(
                            songId,
                            resultSet.getString("title"),
                            resultSet.getString("artist"),
                            resultSet.getString("album"),
                            resultSet.getString("cover_url"),
                            resultSet.getString("audio_url"),
                            resultSet.getInt("duration_seconds")
                        ));
                    }
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Playlist query failed: " + e.getMessage(), e);
        }
        return playlistMap.values().stream().map(PlaylistBuilder::build).toList();
    }

    @Override
    public Playlist createPlaylist(long userId, String name) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO playlist(user_id, name) VALUES (?, ?)",
                 Statement.RETURN_GENERATED_KEYS
             )) {
            statement.setLong(1, userId);
            statement.setString(2, name);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Playlist(keys.getLong(1), userId, name, List.of());
                }
            }
            throw new IllegalStateException("Create playlist failed");
        } catch (SQLException e) {
            throw new IllegalStateException("Database operation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void deletePlaylist(long userId, long playlistId) {
        try (Connection connection = open()) {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM playlist WHERE id = ? AND user_id = ?")) {
                statement.setLong(1, playlistId);
                statement.setLong(2, userId);
                if (statement.executeUpdate() == 0) {
                    throw new IllegalArgumentException("Playlist does not exist or access is denied");
                }
            }
            try (PreparedStatement cleanup = connection.prepareStatement("DELETE FROM playlist_song WHERE playlist_id = ?")) {
                cleanup.setLong(1, playlistId);
                cleanup.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database operation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void addSongToPlaylist(long playlistId, long songId) {
        try (Connection connection = open()) {
            try (PreparedStatement check = connection.prepareStatement("SELECT id FROM playlist_song WHERE playlist_id = ? AND song_id = ?")) {
                check.setLong(1, playlistId);
                check.setLong(2, songId);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        return;
                    }
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("INSERT INTO playlist_song(playlist_id, song_id) VALUES (?, ?)")) {
                insert.setLong(1, playlistId);
                insert.setLong(2, songId);
                insert.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Database operation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeSongFromPlaylist(long playlistId, long songId) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM playlist_song WHERE playlist_id = ? AND song_id = ?")) {
            statement.setLong(1, playlistId);
            statement.setLong(2, songId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Database operation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void recordPlayback(long userId, long songId) {
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO play_history(user_id, song_id, played_at) VALUES (?, ?, ?)")) {
            statement.setLong(1, userId);
            statement.setLong(2, songId);
            statement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Database operation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<PlayHistoryEntry> listHistory(long userId) {
        List<PlayHistoryEntry> history = new ArrayList<>();
        String sql = """
            SELECT h.id, h.played_at, s.id AS song_id, s.title, s.artist, s.album, s.cover_url, s.audio_url, s.duration_seconds
            FROM play_history h
            JOIN song s ON s.id = h.song_id
            WHERE h.user_id = ?
            ORDER BY h.id DESC
            LIMIT 20
            """;
        try (Connection connection = open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Song song = new Song(resultSet.getLong("song_id"), resultSet.getString("title"), resultSet.getString("artist"), resultSet.getString("album"), resultSet.getString("cover_url"), resultSet.getString("audio_url"), resultSet.getInt("duration_seconds"));
                    history.add(new PlayHistoryEntry(resultSet.getLong("id"), song, resultSet.getTimestamp("played_at").toLocalDateTime().toString()));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("History query failed: " + e.getMessage(), e);
        }
        return history;
    }

    @Override
    public String description() {
        return "jdbc(" + url + ")";
    }

    @Override
    public void close() {
    }

    private Connection open() throws SQLException {
        return DataStores.openConnection(url, user, password);
    }

    private void initializeSchema() throws SQLException {
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS app_user (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    username VARCHAR(100) NOT NULL UNIQUE,
                    password VARCHAR(512) NOT NULL,
                    display_name VARCHAR(100) NOT NULL
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS song (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    title VARCHAR(255) NOT NULL,
                    artist VARCHAR(255) NOT NULL,
                    album VARCHAR(255) NOT NULL,
                    cover_url TEXT NOT NULL,
                    audio_url TEXT NOT NULL,
                    duration_seconds INT NOT NULL
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS playlist (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL,
                    name VARCHAR(255) NOT NULL,
                    CONSTRAINT fk_playlist_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS playlist_song (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    playlist_id BIGINT NOT NULL,
                    song_id BIGINT NOT NULL,
                    UNIQUE KEY uk_playlist_song (playlist_id, song_id),
                    CONSTRAINT fk_playlist_song_playlist FOREIGN KEY (playlist_id) REFERENCES playlist(id) ON DELETE CASCADE,
                    CONSTRAINT fk_playlist_song_song FOREIGN KEY (song_id) REFERENCES song(id) ON DELETE CASCADE
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS play_history (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    user_id BIGINT NOT NULL,
                    song_id BIGINT NOT NULL,
                    played_at DATETIME NOT NULL,
                    CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
                    CONSTRAINT fk_history_song FOREIGN KEY (song_id) REFERENCES song(id) ON DELETE CASCADE
                )
                """);
            statement.execute("ALTER TABLE app_user MODIFY COLUMN password VARCHAR(512) NOT NULL");
        }
    }

    private void resetLegacyUsersIfNeeded() throws SQLException {
        boolean resetNeeded = false;
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement("SELECT password FROM app_user"); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                if (!PasswordUtil.isHashed(resultSet.getString("password"))) {
                    resetNeeded = true;
                    break;
                }
            }
        }
        if (!resetNeeded) return;
        try (Connection connection = open(); Statement delete = connection.createStatement()) {
            delete.executeUpdate("DELETE FROM app_user");
        }
    }

    private void initializeFixedLibrary() throws SQLException {
        ensureCanonicalSongs();
        seedDemoData();
    }

    private void seedDemoData() throws SQLException {
        long studentUserId = ensureStudentUser();
        ensureDefaultPlaylist(studentUserId);
        ensureDemoHistory(studentUserId);
    }

    private long ensureStudentUser() throws SQLException {
        Optional<UserSession> existing = findUserByUsername("student");
        if (existing.isPresent()) {
            return existing.get().id();
        }
        return insertSeedUser("student", "123456");
    }

    private long insertSeedUser(String username, String passwordValue) throws SQLException {
        try (Connection connection = open();
             PreparedStatement insert = connection.prepareStatement("INSERT INTO app_user(username, password, display_name) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, username);
            insert.setString(2, PasswordUtil.hashPassword(passwordValue));
            insert.setString(3, "User " + username);
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new IllegalStateException("Seed user insert failed");
    }

    private void ensureCanonicalSongs() throws SQLException {
        if (hasCanonicalSongs()) return;
        resetLibraryData();
        importSong("\u552f\u4e00", "\u5b8b\u96e8\u7426", "\u552f\u4e00", cover(1), audio(1), 240);
        importSong("Giant", "\u5b8b\u96e8\u7426", "Giant", cover(2), audio(2), 240);
        importSong("Radio(Dum-Dum)", "\u5b8b\u96e8\u7426", "Radio(Dum-Dum)", cover(3), audio(3), 240);
        importSong("FREAK", "\u5b8b\u96e8\u7426", "FREAK", cover(4), audio(4), 240);
        importSong("Could It Be", "\u5b8b\u96e8\u7426", "Could It Be", cover(5), audio(5), 240);
    }

    private void ensureDefaultPlaylist(long userId) throws SQLException {
        if (playlistExists(userId, "\u6211\u7684\u6536\u85cf")) return;
        Playlist playlist = createPlaylist(userId, "\u6211\u7684\u6536\u85cf");
        if (songExists(1L)) addSongToPlaylist(playlist.id(), 1L);
        if (songExists(3L)) addSongToPlaylist(playlist.id(), 3L);
    }

    private void ensureDemoHistory(long userId) throws SQLException {
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM play_history WHERE user_id = ?")) {
            statement.setLong(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next() && resultSet.getInt(1) > 0) return;
            }
        }
        if (songExists(2L)) recordPlayback(userId, 2L);
        if (songExists(1L)) recordPlayback(userId, 1L);
    }

    private int countRows(String tableName) throws SQLException {
        try (Connection connection = open(); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private boolean hasCanonicalSongs() throws SQLException {
        if (countRows("song") != 5) return false;
        List<String> titles = new ArrayList<>();
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement("SELECT title FROM song ORDER BY id"); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                titles.add(resultSet.getString("title"));
            }
        }
        return titles.equals(List.of("\u552f\u4e00", "Giant", "Radio(Dum-Dum)", "FREAK", "Could It Be"));
    }

    private void resetLibraryData() throws SQLException {
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM play_history");
            statement.executeUpdate("DELETE FROM playlist_song");
            statement.executeUpdate("DELETE FROM playlist");
            statement.executeUpdate("DELETE FROM song");
        }
    }

    private Optional<UserSession> findUserByUsername(String username) throws SQLException {
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement("SELECT id, username, display_name FROM app_user WHERE username = ?")) {
            statement.setString(1, username);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(new UserSession(resultSet.getLong("id"), resultSet.getString("username"), resultSet.getString("display_name")));
                }
            }
        }
        return Optional.empty();
    }

    private boolean playlistExists(long userId, String name) throws SQLException {
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement("SELECT id FROM playlist WHERE user_id = ? AND name = ?")) {
            statement.setLong(1, userId);
            statement.setString(2, name);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean songExists(long songId) throws SQLException {
        try (Connection connection = open(); PreparedStatement statement = connection.prepareStatement("SELECT id FROM song WHERE id = ?")) {
            statement.setLong(1, songId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static Song mapSong(ResultSet resultSet) throws SQLException {
        return new Song(resultSet.getLong("id"), resultSet.getString("title"), resultSet.getString("artist"), resultSet.getString("album"), resultSet.getString("cover_url"), resultSet.getString("audio_url"), resultSet.getInt("duration_seconds"));
    }

    private static long safeLong(ResultSet resultSet, String column) {
        try { return resultSet.getLong(column); } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    private static String safeString(ResultSet resultSet, String column) {
        try { return resultSet.getString(column); } catch (SQLException e) { throw new IllegalStateException(e); }
    }

    private static String cover(int seed) {
        return "/media/uploads/covers/song" + seed + ".jpg";
    }

    private static String audio(int seed) {
        return "/media/uploads/audio/song" + seed + ".mp3";
    }

    private static final class PlaylistBuilder {
        private final long id;
        private final long userId;
        private final String name;
        private final List<Song> songs = new ArrayList<>();

        private PlaylistBuilder(long id, long userId, String name) {
            this.id = id;
            this.userId = userId;
            this.name = name;
        }

        private Playlist build() {
            return new Playlist(id, userId, name, List.copyOf(songs));
        }
    }
}
