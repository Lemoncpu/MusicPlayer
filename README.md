# Music Player

Music Player is a course project music player built with Vue 3 on the frontend and a native Java HTTP server on the backend. The frontend handles routing, page rendering, and HTML5 Audio playback. The backend exposes JSON APIs, serves static files, and reads or writes music data through JDBC.

## Tech Stack

- Frontend: Vue 3, Vue Router, HTML5 Audio
- Backend: Java, com.sun.net.httpserver
- Data access: JDBC
- Default database: MySQL
- Fallback mode: in-memory data store

## Project Structure

- `web/`: frontend HTML, JavaScript, and CSS
- `src/main/java/com/musicplayer/`: backend entry point and Java source
- `src/main/java/com/musicplayer/http/`: API handlers and static/media handlers
- `src/main/java/com/musicplayer/store/`: data abstraction, JDBC store, memory store
- `src/main/java/com/musicplayer/model/`: Song, Playlist, PlayHistoryEntry, UserSession
- `database/`: schema and seed SQL
- `media/uploads/audio/`: built-in audio files
- `media/uploads/covers/`: built-in cover images

## Core Features

- User login and registration
- Song library display and playback
- Playlist create, delete, add, and remove
- Play history recording
- Local song and cover import
- Spotify Web style three-column frontend layout

## Built-in Music Library

The project no longer uses generated demo WAV audio and no longer serves `/media/demo/*.wav`.

The built-in library now comes directly from local project resources:

- Audio files: `media/uploads/audio/song1.mp3` to `song5.mp3`
- Cover files: `media/uploads/covers/song1.jpg` to `song5.jpg`

Both JDBC mode and memory mode load the same five default songs:

- `song1`
- `song2`
- `song3`
- `song4`
- `song5`

The corresponding runtime media URLs are:

- `/media/uploads/audio/song1.mp3` to `/media/uploads/audio/song5.mp3`
- `/media/uploads/covers/song1.jpg` to `/media/uploads/covers/song5.jpg`

## JDBC Initialization

The project uses MySQL JDBC by default.

Default connection values:

- Host: `127.0.0.1`
- Port: `3306`
- User: `root`
- Password: `qx418504`
- Database: `musicplayer`

Startup behavior:

1. Connect to MySQL and create the `musicplayer` database if needed
2. Create `app_user`, `song`, `playlist`, `playlist_song`, and `play_history` tables if needed
3. Check whether the `song` table contains exactly `song1` to `song5`
4. If not, clear built-in song-related data and rebuild the fixed five-song library
5. Recreate the default user, default playlist, and default play history

Default account:

- Username: `student`
- Password: `123456`

## Memory Mode

If JDBC is unavailable, the project can run in memory mode:

```powershell
$env:MUSICPLAYER_DB_MODE="memory"
java -cp out com.musicplayer.App
```

Memory mode uses the same built-in local songs and covers.

## Run the Project

Compile the Java source:

```powershell
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName })
```

Start the server:

```powershell
java -cp out com.musicplayer.App
```

If you prefer to pass the MySQL JDBC jar explicitly:

```powershell
java -cp "out;lib/mysql-connector-j-9.2.0.jar" com.musicplayer.App
```

Open the app in the browser:

- [http://localhost:8080](http://localhost:8080)

## Main API Endpoints

- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/songs`
- `POST /api/songs/import`
- `GET /api/playlists`
- `POST /api/playlists`
- `DELETE /api/playlists/{id}`
- `POST /api/playlists/{id}/songs`
- `DELETE /api/playlists/{id}/songs/{songId}`
- `GET /api/history`
- `POST /api/history`
- `GET /api/health`

## Current Implementation Notes

- `DemoAudioHandler` has been removed
- The server no longer exposes the `/media/demo` route
- Initial playback resources now come from repository media files only
- Frontend structure, API paths, and database schema remain unchanged
