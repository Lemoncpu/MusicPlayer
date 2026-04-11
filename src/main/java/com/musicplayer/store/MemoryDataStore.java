package com.musicplayer.store;

import com.musicplayer.model.PlayHistoryEntry;
import com.musicplayer.model.Playlist;
import com.musicplayer.model.Song;
import com.musicplayer.model.UserSession;
import com.musicplayer.util.PasswordUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MemoryDataStore implements DataStore {
    private final Map<Long, LocalUser> users = new LinkedHashMap<>();
    private final Map<Long, Song> songs = new LinkedHashMap<>();
    private final Map<Long, LocalPlaylist> playlists = new LinkedHashMap<>();
    private final Map<Long, List<PlayHistoryEntry>> historyByUser = new LinkedHashMap<>();
    private long userSequence = 1;
    private long songSequence = 1;
    private long playlistSequence = 1;
    private long historySequence = 1;

    public MemoryDataStore() {
        seed();
    }

    @Override
    public synchronized UserSession register(String username, String password) {
        PasswordUtil.requireValidPassword(password);
        if (users.values().stream().anyMatch(user -> user.username.equalsIgnoreCase(username))) {
            throw new IllegalArgumentException("Username already exists");
        }
        long id = userSequence++;
        LocalUser user = new LocalUser(id, username, PasswordUtil.hashPassword(password), "User " + username);
        users.put(id, user);
        historyByUser.put(id, new ArrayList<>());
        return user.toSession();
    }

    @Override
    public synchronized UserSession login(String username, String password) {
        LocalUser user = users.values().stream()
            .filter(item -> item.username.equalsIgnoreCase(username))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("User not registered"));
        if (!PasswordUtil.verifyPassword(password, user.passwordHash)) {
            throw new IllegalArgumentException("Wrong password");
        }
        return user.toSession();
    }

    @Override
    public synchronized List<Song> listSongs() {
        return new ArrayList<>(songs.values());
    }

    @Override
    public synchronized Optional<Song> getSong(long songId) {
        return Optional.ofNullable(songs.get(songId));
    }

    @Override
    public synchronized Song importSong(String title, String artist, String album, String coverUrl, String audioUrl, int durationSeconds) {
        long id = songSequence++;
        Song song = new Song(id, title, artist, album, coverUrl, audioUrl, durationSeconds);
        songs.put(id, song);
        return song;
    }

    @Override
    public synchronized List<Playlist> listPlaylists(long userId) {
        return playlists.values().stream()
            .filter(playlist -> playlist.userId == userId)
            .map(this::toPlaylist)
            .sorted(Comparator.comparing(Playlist::id))
            .toList();
    }

    @Override
    public synchronized Playlist createPlaylist(long userId, String name) {
        ensureUser(userId);
        long id = playlistSequence++;
        LocalPlaylist playlist = new LocalPlaylist(id, userId, name, new ArrayList<>());
        playlists.put(id, playlist);
        return toPlaylist(playlist);
    }

    @Override
    public synchronized void deletePlaylist(long userId, long playlistId) {
        LocalPlaylist playlist = requirePlaylist(playlistId);
        if (playlist.userId != userId) {
            throw new IllegalArgumentException("Playlist does not exist or access is denied");
        }
        playlists.remove(playlistId);
    }

    @Override
    public synchronized void addSongToPlaylist(long playlistId, long songId) {
        LocalPlaylist playlist = requirePlaylist(playlistId);
        ensureSong(songId);
        if (!playlist.songIds.contains(songId)) {
            playlist.songIds.add(songId);
        }
    }

    @Override
    public synchronized void removeSongFromPlaylist(long playlistId, long songId) {
        LocalPlaylist playlist = requirePlaylist(playlistId);
        playlist.songIds.remove(songId);
    }

    @Override
    public synchronized void recordPlayback(long userId, long songId) {
        ensureUser(userId);
        Song song = ensureSong(songId);
        String playedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        historyByUser.computeIfAbsent(userId, ignored -> new ArrayList<>())
            .add(0, new PlayHistoryEntry(historySequence++, song, playedAt));
    }

    @Override
    public synchronized List<PlayHistoryEntry> listHistory(long userId) {
        ensureUser(userId);
        return new ArrayList<>(historyByUser.getOrDefault(userId, List.of()));
    }

    @Override
    public String description() {
        return "memory";
    }

    @Override
    public void close() {
    }

    private void seed() {
        UserSession demoUser = insertSeedUser("student", "123456");
        importSong("\u552f\u4e00", "\u5b8b\u96e8\u7426", "\u552f\u4e00", cover(1), audio(1), 240);
        importSong("Giant", "\u5b8b\u96e8\u7426", "Giant", cover(2), audio(2), 240);
        importSong("Radio(Dum-Dum)", "\u5b8b\u96e8\u7426", "Radio(Dum-Dum)", cover(3), audio(3), 240);
        importSong("FREAK", "\u5b8b\u96e8\u7426", "FREAK", cover(4), audio(4), 240);
        importSong("Could It Be", "\u5b8b\u96e8\u7426", "Could It Be", cover(5), audio(5), 240);
        Playlist playlist = createPlaylist(demoUser.id(), "\u6211\u7684\u6536\u85cf");
        addSongToPlaylist(playlist.id(), 1L);
        addSongToPlaylist(playlist.id(), 3L);
        recordPlayback(demoUser.id(), 2L);
        recordPlayback(demoUser.id(), 1L);
    }

    private UserSession insertSeedUser(String username, String password) {
        long id = userSequence++;
        LocalUser user = new LocalUser(id, username, PasswordUtil.hashPassword(password), "User " + username);
        users.put(id, user);
        historyByUser.put(id, new ArrayList<>());
        return user.toSession();
    }

    private static String cover(int seed) {
        return "/media/uploads/covers/song" + seed + ".jpg";
    }

    private static String audio(int seed) {
        return "/media/uploads/audio/song" + seed + ".mp3";
    }

    private void ensureUser(long userId) {
        if (!users.containsKey(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }
    }

    private Song ensureSong(long songId) {
        Song song = songs.get(songId);
        if (song == null) {
            throw new IllegalArgumentException("Song does not exist");
        }
        return song;
    }

    private LocalPlaylist requirePlaylist(long playlistId) {
        LocalPlaylist playlist = playlists.get(playlistId);
        if (playlist == null) {
            throw new IllegalArgumentException("Playlist does not exist");
        }
        return playlist;
    }

    private Playlist toPlaylist(LocalPlaylist playlist) {
        List<Song> playlistSongs = playlist.songIds.stream().map(songs::get).filter(song -> song != null).toList();
        return new Playlist(playlist.id, playlist.userId, playlist.name, playlistSongs);
    }

    private record LocalUser(long id, String username, String passwordHash, String displayName) {
        private UserSession toSession() {
            return new UserSession(id, username, displayName);
        }
    }

    private record LocalPlaylist(long id, long userId, String name, List<Long> songIds) {
    }
}
