INSERT INTO app_user(username, password, display_name) VALUES ('student', '123456', 'student');

INSERT INTO song(title, artist, album, cover_url, audio_url, duration_seconds) VALUES
('唯一', '宋雨琦', '唯一', '/media/uploads/covers/song1.jpg', '/media/uploads/audio/song1.mp3', 240),
('Giant', '宋雨琦', 'Giant', '/media/uploads/covers/song2.jpg', '/media/uploads/audio/song2.mp3', 240),
('Radio(Dum-Dum)', '宋雨琦', 'Radio(Dum-Dum)', '/media/uploads/covers/song3.jpg', '/media/uploads/audio/song3.mp3', 240),
('FREAK', '宋雨琦', 'FREAK', '/media/uploads/covers/song4.jpg', '/media/uploads/audio/song4.mp3', 240),
('Could It Be', '宋雨琦', 'Could It Be', '/media/uploads/covers/song5.jpg', '/media/uploads/audio/song5.mp3', 240);

INSERT INTO playlist(user_id, name) VALUES (1, 'Favorites');
INSERT INTO playlist_song(playlist_id, song_id) VALUES (1, 1);
INSERT INTO playlist_song(playlist_id, song_id) VALUES (1, 3);
INSERT INTO play_history(user_id, song_id, played_at) VALUES (1, 2, '2026-03-26 09:00:00');
INSERT INTO play_history(user_id, song_id, played_at) VALUES (1, 1, '2026-03-26 09:05:00');
