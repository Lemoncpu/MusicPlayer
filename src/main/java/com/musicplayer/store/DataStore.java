package com.musicplayer.store;

import com.musicplayer.model.PlayHistoryEntry;
import com.musicplayer.model.Playlist;
import com.musicplayer.model.Song;
import com.musicplayer.model.UserSession;
import java.util.List;
import java.util.Optional;

public interface DataStore extends AutoCloseable {
    UserSession register(String username, String password);

    UserSession login(String username, String password);

    List<Song> listSongs();

    Optional<Song> getSong(long songId);

    Song importSong(String title, String artist, String album, String coverUrl, String audioUrl, int durationSeconds);

    List<Playlist> listPlaylists(long userId);

    Playlist createPlaylist(long userId, String name);

    void deletePlaylist(long userId, long playlistId);

    void addSongToPlaylist(long playlistId, long songId);

    void removeSongFromPlaylist(long playlistId, long songId);

    void recordPlayback(long userId, long songId);

    List<PlayHistoryEntry> listHistory(long userId);

    String description();

    @Override
    void close();
}