CREATE DATABASE IF NOT EXISTS `musicplayer` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `musicplayer`;

CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(512) NOT NULL,
    display_name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS song (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    artist VARCHAR(255) NOT NULL,
    album VARCHAR(255) NOT NULL,
    cover_url TEXT NOT NULL,
    audio_url TEXT NOT NULL,
    duration_seconds INT NOT NULL
);

CREATE TABLE IF NOT EXISTS playlist (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT fk_playlist_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS playlist_song (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    playlist_id BIGINT NOT NULL,
    song_id BIGINT NOT NULL,
    UNIQUE KEY uk_playlist_song (playlist_id, song_id),
    CONSTRAINT fk_playlist_song_playlist FOREIGN KEY (playlist_id) REFERENCES playlist(id) ON DELETE CASCADE,
    CONSTRAINT fk_playlist_song_song FOREIGN KEY (song_id) REFERENCES song(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS play_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    song_id BIGINT NOT NULL,
    played_at DATETIME NOT NULL,
    CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_history_song FOREIGN KEY (song_id) REFERENCES song(id) ON DELETE CASCADE
);
