package com.musicplayer.model;

import java.util.List;

public record Playlist(long id, long userId, String name, List<Song> songs) {
}
