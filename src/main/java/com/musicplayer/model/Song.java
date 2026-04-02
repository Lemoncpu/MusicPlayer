package com.musicplayer.model;

public record Song(
    long id,
    String title,
    String artist,
    String album,
    String coverUrl,
    String audioUrl,
    int durationSeconds
) {
}
