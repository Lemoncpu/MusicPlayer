package com.musicplayer.model;

public record PlayHistoryEntry(long id, Song song, String playedAt) {
}
