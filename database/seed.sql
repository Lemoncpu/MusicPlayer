INSERT INTO app_user(username, password, display_name) VALUES ('student', '123456', '用户student');

INSERT INTO song(title, artist, album, cover_url, audio_url, duration_seconds) VALUES
('Campus Sunrise', 'Course Band', 'Semester Beats', 'https://picsum.photos/seed/musicplayer-1/480/480', '/media/demo/1.wav', 8),
('Library Lo-Fi', 'Study Crew', 'Focus Session', 'https://picsum.photos/seed/musicplayer-2/480/480', '/media/demo/2.wav', 8),
('Route To Finals', 'Night Coders', 'Deadline Mix', 'https://picsum.photos/seed/musicplayer-3/480/480', '/media/demo/3.wav', 8),
('Weekend Refactor', 'Merge Conflict', 'Build Success', 'https://picsum.photos/seed/musicplayer-4/480/480', '/media/demo/4.wav', 8),
('Fresh Deploy', 'Blue Screeners', 'Hotfix Dreams', 'https://picsum.photos/seed/musicplayer-5/480/480', '/media/demo/5.wav', 8);

INSERT INTO playlist(user_id, name) VALUES (1, '我的收藏');
INSERT INTO playlist_song(playlist_id, song_id) VALUES (1, 1);
INSERT INTO playlist_song(playlist_id, song_id) VALUES (1, 3);
INSERT INTO play_history(user_id, song_id, played_at) VALUES (1, 2, '2026-03-26 09:00:00');
INSERT INTO play_history(user_id, song_id, played_at) VALUES (1, 1, '2026-03-26 09:05:00');
