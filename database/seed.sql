INSERT INTO app_user(username, password, display_name) VALUES ('student', 'pbkdf2$120000$sBw1c83IF9pedgc+F8ONIg==$+DVfSrUfRguHNPZMcHabbDnUAQcfz4LeR6uCxf/vORo=', 'student');

INSERT INTO song(title, artist, album, cover_url, audio_url, duration_seconds) VALUES
('song1', 'artist1', 'album1', '/media/uploads/covers/song1.jpg', '/media/uploads/audio/song1.mp3', 240),
('song2', 'artist2', 'album2', '/media/uploads/covers/song2.jpg', '/media/uploads/audio/song2.mp3', 240),
('song3', 'artist3', 'album3', '/media/uploads/covers/song3.jpg', '/media/uploads/audio/song3.mp3', 240),
('song4', 'artist4', 'album4', '/media/uploads/covers/song4.jpg', '/media/uploads/audio/song4.mp3', 240),
('song5', 'artist5', 'album5', '/media/uploads/covers/song5.jpg', '/media/uploads/audio/song5.mp3', 240);

INSERT INTO playlist(user_id, name) VALUES (1, 'Favorites');
INSERT INTO playlist_song(playlist_id, song_id) VALUES (1, 1);
INSERT INTO playlist_song(playlist_id, song_id) VALUES (1, 3);
INSERT INTO play_history(user_id, song_id, played_at) VALUES (1, 2, '2026-03-26 09:00:00');
INSERT INTO play_history(user_id, song_id, played_at) VALUES (1, 1, '2026-03-26 09:05:00');

