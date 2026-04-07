const { createApp, reactive, computed, onMounted, ref, watch } = Vue;
const { createRouter, createWebHashHistory, useRoute, useRouter } = VueRouter;

const I18N = {
  zh: {
    app: "Music Player",
    home: "首页",
    songs: "歌曲",
    playlists: "歌单",
    now: "正在播放",
    login: "登录",
    loginTitle: "欢迎回来",
    loginText: "登录后即可查看曲库、歌单与播放记录。",
    registerText: "注册新账号，立即开始使用。",
    username: "用户名",
    password: "密码",
    search: "搜索歌曲、歌手或专辑",
    welcome: "欢迎回来",
    featured: "精选歌曲",
    history: "最近播放",
    continueListen: "继续收听",
    library: "歌曲曲库",
    importLocal: "导入本地歌曲",
    title: "歌名",
    artist: "歌手",
    album: "专辑",
    duration: "时长（秒）",
    audio: "音频文件",
    cover: "封面文件",
    createPlaylist: "新建歌单",
    playlistName: "歌单名称",
    addSong: "添加歌曲",
    remove: "移除",
    delete: "删除",
    chooseSong: "选择歌曲",
    play: "播放",
    pause: "暂停",
    prev: "上一首",
    next: "下一首",
    logout: "退出登录",
    volume: "音量",
    noSongs: "当前暂无歌曲。",
    noHistory: "暂无播放记录。",
    noPlaylists: "还没有歌单。",
    noCurrent: "暂无正在播放的歌曲",
    queue: "可立即播放",
    statusReady: "已准备好探索你的音乐。",
    statusLoaded: "数据已加载完成。",
    imported: "歌曲导入成功。",
    playlistCreated: "歌单创建成功。",
    playlistDeleted: "歌单已删除。",
    playlistAdded: "歌曲已添加到歌单。",
    playlistRemoved: "歌曲已从歌单移除。",
    loggedIn: "登录成功。",
    registered: "注册成功。",
    loggedOut: "已退出登录。",
    badFiles: "请同时选择音频和封面文件。",
    fallbackUser: "音乐用户",
    switchLanguage: "Switch Language"
  },
  en: {
    app: "Music Player",
    home: "Home",
    songs: "Songs",
    playlists: "Playlists",
    now: "Now Playing",
    login: "Login",
    loginTitle: "Welcome Back",
    loginText: "Log in to access your music library, playlists and history.",
    registerText: "Register a new account to get started.",
    username: "Username",
    password: "Password",
    search: "Search songs, artists or albums",
    welcome: "Welcome back",
    featured: "Featured Songs",
    history: "Recent Plays",
    continueListen: "Continue Listening",
    library: "Music Library",
    importLocal: "Import Local Songs",
    title: "Title",
    artist: "Artist",
    album: "Album",
    duration: "Duration (sec)",
    audio: "Audio File",
    cover: "Cover Image",
    createPlaylist: "Create Playlist",
    playlistName: "Playlist Name",
    addSong: "Add Song",
    remove: "Remove",
    delete: "Delete",
    chooseSong: "Choose a song",
    play: "Play",
    pause: "Pause",
    prev: "Previous",
    next: "Next",
    logout: "Logout",
    volume: "Volume",
    noSongs: "No songs yet.",
    noHistory: "No play history yet.",
    noPlaylists: "No playlists yet.",
    noCurrent: "No song currently playing",
    queue: "Up Next",
    statusReady: "Ready to explore your music.",
    statusLoaded: "Data loaded successfully.",
    imported: "Song imported successfully.",
    playlistCreated: "Playlist created successfully.",
    playlistDeleted: "Playlist deleted.",
    playlistAdded: "Song added to playlist.",
    playlistRemoved: "Song removed from playlist.",
    loggedIn: "Login successful.",
    registered: "Registration successful.",
    loggedOut: "Logged out successfully.",
    badFiles: "Please select both audio and cover files.",
    fallbackUser: "Music User",
    switchLanguage: "切换语言"
  }
};

const langKey = "musicplayer-language";
const storageKey = "musicplayer-current-user";

const i18n = reactive({
  lang: localStorage.getItem(langKey) || "zh"
});

const T = computed(() => I18N[i18n.lang]);

function normalizeUser(user) {
  if (!user) return null;
  return { ...user, displayName: (user.displayName || user.username || I18N.zh.fallbackUser).trim() };
}
function loadStoredUser() {
  try { return normalizeUser(JSON.parse(localStorage.getItem(storageKey) || "null")); } catch (e) { return null; }
}
function saveUser(user) { localStorage.setItem(storageKey, JSON.stringify(user)); }
function clearUser() { localStorage.removeItem(storageKey); }
function formBody(payload) { return new URLSearchParams(payload).toString(); }
function formatTime(v) {
  const total = Math.max(0, Math.floor(Number(v || 0)));
  return `${String(Math.floor(total / 60)).padStart(2, "0")}:${String(total % 60).padStart(2, "0")}`;
}
function subtitle(song) { return [song.artist, song.album].filter(Boolean).join(" · "); }
function greeting() { const h = new Date().getHours(); return h < 12 ? "上午好" : h < 18 ? "下午好" : "晚上好"; }

watch(() => i18n.lang, (newLang) => {
  localStorage.setItem(langKey, newLang);
});

const state = reactive({
  currentUser: loadStoredUser(), songs: [], playlists: [], history: [], currentSong: null, currentIndex: -1,
  isPlaying: false, currentTime: 0, duration: 0, volume: 0.72, search: "", loading: false, statusMessage: ""
});

watch(() => T.value.statusReady, (v) => { if (!state.statusMessage) state.statusMessage = v; }, { immediate: true });

function setErrorMessage(error) {
  state.statusMessage = error && error.message ? error.message : "Request failed.";
}

const audio = new Audio();
audio.volume = state.volume;
audio.addEventListener("timeupdate", () => { state.currentTime = audio.currentTime || 0; state.duration = audio.duration || (state.currentSong ? state.currentSong.durationSeconds || 0 : 0); });
audio.addEventListener("play", () => { state.isPlaying = true; });
audio.addEventListener("pause", () => { state.isPlaying = false; });
audio.addEventListener("loadedmetadata", () => { state.duration = audio.duration || (state.currentSong ? state.currentSong.durationSeconds || 0 : 0); });
audio.addEventListener("ended", () => { player.next(); });
watch(() => state.volume, (v) => { audio.volume = Number(v || 0); });

async function apiFetch(path, options = {}) {
  const response = await fetch(path, options);
  const data = await response.json();
  if (!response.ok || !data.success) throw new Error(data.message || "Request failed.");
  return data;
}

const api = {
  login: (username, password) => apiFetch("/api/auth/login", { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" }, body: formBody({ username, password }) }),
  register: (username, password) => apiFetch("/api/auth/register", { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" }, body: formBody({ username, password }) }),
  listSongs: () => apiFetch("/api/songs"),
  importSong: (formData) => apiFetch("/api/songs/import", { method: "POST", body: formData }),
  listPlaylists: (userId) => apiFetch(`/api/playlists?userId=${encodeURIComponent(userId)}`),
  createPlaylist: (userId, name) => apiFetch("/api/playlists", { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" }, body: formBody({ userId, name }) }),
  deletePlaylist: (userId, playlistId) => apiFetch(`/api/playlists/${playlistId}?userId=${encodeURIComponent(userId)}`, { method: "DELETE" }),
  addSongToPlaylist: (playlistId, songId) => apiFetch(`/api/playlists/${playlistId}/songs`, { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" }, body: formBody({ songId }) }),
  removeSongFromPlaylist: (playlistId, songId) => apiFetch(`/api/playlists/${playlistId}/songs/${songId}`, { method: "DELETE" }),
  listHistory: (userId) => apiFetch(`/api/history?userId=${encodeURIComponent(userId)}`),
  recordHistory: (userId, songId) => apiFetch("/api/history", { method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8" }, body: formBody({ userId, songId }) })
};

const player = {
  async bootstrap() {
    state.loading = true;
    try {
      const songsData = await api.listSongs();
      state.songs = songsData.songs || [];
      if (state.currentUser) await this.refreshCollections();
      state.statusMessage = T.value.statusLoaded;
    } catch (e) { setErrorMessage(e); } finally { state.loading = false; }
  },
  async refreshCollections() {
    if (!state.currentUser) { state.playlists = []; state.history = []; return; }
    const [p, h] = await Promise.all([api.listPlaylists(state.currentUser.id), api.listHistory(state.currentUser.id)]);
    state.playlists = p.playlists || [];
    state.history = h.history || [];
  },
  async playSong(song) {
    if (!song) return;
    state.currentSong = song;
    state.currentIndex = state.songs.findIndex((s) => s.id === song.id);
    state.currentTime = 0;
    state.duration = song.durationSeconds || 0;
    audio.src = song.audioUrl;
    await audio.play();
    state.statusMessage = `${T.value.now}: ${song.title}`;
    if (state.currentUser) {
      try { await api.recordHistory(state.currentUser.id, song.id); await this.refreshCollections(); } catch (e) { setErrorMessage(e); }
    }
  },
  async toggle() { if (!state.currentSong) return state.songs.length ? this.playSong(state.songs[0]) : null; if (audio.paused) return audio.play(); audio.pause(); },
  async next() { if (!state.songs.length) return; const i = state.currentIndex >= 0 ? (state.currentIndex + 1) % state.songs.length : 0; await this.playSong(state.songs[i]); },
  async prev() { if (!state.songs.length) return; const i = state.currentIndex > 0 ? state.currentIndex - 1 : state.songs.length - 1; await this.playSong(state.songs[i]); },
  seek(v) { audio.currentTime = Number(v || 0); state.currentTime = audio.currentTime; },
  setVolume(v) { state.volume = Number(v || 0); },
  async auth(username, password, mode) {
    const data = await (mode === "register" ? api.register(username, password) : api.login(username, password));
    state.currentUser = normalizeUser(data.user); saveUser(state.currentUser); await this.refreshCollections();
    state.statusMessage = mode === "register" ? T.value.registered : T.value.loggedIn;
  },
  logout() {
    audio.pause(); audio.src = ""; clearUser();
    state.currentUser = null; state.playlists = []; state.history = []; state.currentSong = null; state.currentIndex = -1; state.currentTime = 0; state.duration = 0; state.isPlaying = false; state.statusMessage = T.value.loggedOut;
  },
  async createPlaylist(name) { const data = await api.createPlaylist(state.currentUser.id, name); await this.refreshCollections(); state.statusMessage = T.value.playlistCreated; return data.playlist; },
  async deletePlaylist(id) { await api.deletePlaylist(state.currentUser.id, id); await this.refreshCollections(); state.statusMessage = T.value.playlistDeleted; },
  async addToPlaylist(playlistId, songId) { await api.addSongToPlaylist(playlistId, songId); await this.refreshCollections(); state.statusMessage = T.value.playlistAdded; },
  async removeFromPlaylist(playlistId, songId) { await api.removeSongFromPlaylist(playlistId, songId); await this.refreshCollections(); state.statusMessage = T.value.playlistRemoved; },
  async importSong(formData) { const data = await api.importSong(formData); await this.bootstrap(); state.statusMessage = T.value.imported; return data.song; }
};

const SongCard = {
  props: { song: { type: Object, required: true } }, emits: ["play"],
  template: `
    <article class="song-card"><div class="song-card-cover-wrap"><img class="song-card-cover" :src="song.coverUrl" :alt="song.title"><button class="card-play-button small-pill" type="button" @click="$emit('play', song)">{{ state.currentSong && state.currentSong.id === song.id && state.isPlaying ? $root.T.pause : $root.T.play }}</button></div><div class="song-card-copy"><strong>{{ song.title }}</strong><p>{{ subtitle(song) }}</p></div></article>
  `,
  setup() { return { state, subtitle }; }
};

const TrackRow = {
  props: { song: { type: Object, required: true }, index: { type: Number, default: 0 }, actionLabel: { type: String, default: "" }, actionKind: { type: String, default: "ghost-button" } },
  emits: ["play", "action"],
  template: `
    <div class="track-row"><div class="track-row-main"><span class="track-index">{{ index }}</span><img class="track-cover" :src="song.coverUrl" :alt="song.title"><div class="track-copy"><strong>{{ song.title }}</strong><p>{{ subtitle(song) }}</p></div></div><div class="row-action-group"><span class="track-meta">{{ formatTime(song.durationSeconds) }}</span><button class="ghost-button text-only" type="button" @click="$emit('play', song)">{{ $root.T.play }}</button><button v-if="actionLabel" :class="actionKind" class="text-only" type="button" @click="$emit('action', song)">{{ actionLabel }}</button></div></div>
  `,
  setup() { return { subtitle, formatTime }; }
};

const HomePage = {
  components: { SongCard, TrackRow },
  setup() {
    const featured = computed(() => state.songs.slice(0, 4));
    const continueSongs = computed(() => {
      const seen = new Set(); const list = [];
      state.history.forEach((item) => { if (item.song && !seen.has(item.song.id)) { seen.add(item.song.id); list.push(item.song); } });
      return list.slice(0, 4);
    });
    return { state, featured, continueSongs, greeting, player };
  },
  template: `
    <section class="page-stack">
      <section class="hero-banner"><div><span class="eyebrow">{{ greeting() }}</span><h2>{{ $root.T.featured }}</h2><p>{{ $root.T.featured }}</p><div class="hero-actions"><button class="primary-button pill-cta" type="button" @click="player.toggle()">{{ state.isPlaying ? $root.T.pause : $root.T.play }}</button><button class="ghost-button pill-cta" type="button" @click="$router.push('/songs')">{{ $root.T.songs }}</button></div></div><div class="auth-badges"><span>{{ $root.T.library }}</span><span>{{ $root.T.importLocal }}</span><span>{{ $root.T.playlists }}</span></div></section>
      <section class="content-section"><div class="section-head"><div><span>{{ $root.T.featured }}</span><h3>{{ $root.T.featured }}</h3></div></div><div class="card-grid four-up"><SongCard v-for="song in featured" :key="song.id" :song="song" @play="player.playSong" /></div><p v-if="!featured.length" class="empty-text">{{ $root.T.noSongs }}</p></section>
      <section class="split-section"><div class="content-section"><div class="section-head"><div><span>{{ $root.T.continueListen }}</span><h3>{{ $root.T.continueListen }}</h3></div></div><div class="card-grid four-up"><SongCard v-for="song in continueSongs" :key="'c'+song.id" :song="song" @play="player.playSong" /></div><p v-if="!continueSongs.length" class="empty-text">{{ $root.T.noHistory }}</p></div><div class="content-section"><div class="section-head compact-head"><div><span>{{ $root.T.history }}</span><h3>{{ $root.T.history }}</h3></div></div><div class="track-list"><TrackRow v-for="(item,index) in state.history.slice(0,5)" :key="item.id" :song="item.song" :index="index+1" @play="player.playSong" /></div><p v-if="!state.history.length" class="empty-text">{{ $root.T.noHistory }}</p></div></section>
    </section>
  `
};

const SongsPage = {
  components: { TrackRow },
  setup() {
    const form = reactive({ title: "", artist: "", album: "", durationSeconds: "", audioFile: null, coverFile: null });
    const submitting = ref(false);
    const setAudio = (e) => { form.audioFile = e.target.files && e.target.files[0] ? e.target.files[0] : null; };
    const setCover = (e) => { form.coverFile = e.target.files && e.target.files[0] ? e.target.files[0] : null; };
    const reset = () => { form.title = ""; form.artist = ""; form.album = ""; form.durationSeconds = ""; form.audioFile = null; form.coverFile = null; };
    const submit = async () => {
      if (!form.audioFile || !form.coverFile) { state.statusMessage = T.value.badFiles; return; }
      submitting.value = true;
      try {
        const fd = new FormData();
        fd.append("title", form.title); fd.append("artist", form.artist); fd.append("album", form.album); fd.append("durationSeconds", String(form.durationSeconds || 0));
        fd.append("audioFile", form.audioFile); fd.append("coverFile", form.coverFile);
        await player.importSong(fd); reset();
      } catch (e) { setErrorMessage(e); } finally { submitting.value = false; }
    };
    return { state, form, submitting, setAudio, setCover, submit, player };
  },
  template: `
    <section class="page-stack">
      <section class="hero-banner"><div><span class="eyebrow">{{ $root.T.songs }}</span><h2>{{ $root.T.importLocal }}</h2><p>{{ $root.T.importLocal }}</p></div><div class="hero-stat-row"><div class="history-chip"><img :src="state.songs[0] ? state.songs[0].coverUrl : 'data:,'" alt=""><span>{{ state.songs.length }} {{ $root.T.songs }}</span></div></div></section>
      <section class="toolbar-card"><div class="section-head"><div><span>{{ $root.T.importLocal }}</span><h3>{{ $root.T.importLocal }}</h3></div><p class="section-note">{{ $root.T.audio }} & {{ $root.T.cover }}</p></div><div class="import-grid"><input v-model.trim="form.title" :placeholder="$root.T.title"><input v-model.trim="form.artist" :placeholder="$root.T.artist"><input v-model.trim="form.album" :placeholder="$root.T.album"><input v-model.trim="form.durationSeconds" type="number" min="0" :placeholder="$root.T.duration"><label class="file-field"><span>{{ $root.T.audio }}</span><input type="file" accept="audio/*" @change="setAudio"></label><label class="file-field"><span>{{ $root.T.cover }}</span><input type="file" accept="image/*" @change="setCover"></label><button class="primary-button wide" type="button" :disabled="submitting" @click="submit">{{ submitting ? $root.T.importLocal : $root.T.importLocal }}</button></div></section>
      <section class="content-section"><div class="section-head"><div><span>{{ $root.T.library }}</span><h3>{{ $root.T.library }}</h3></div><p class="section-note">{{ state.songs.length }} {{ $root.T.songs }}</p></div><div class="table-head"><span>{{ $root.T.title }}</span><span>{{ $root.T.duration }}</span></div><div class="track-list"><TrackRow v-for="(song,index) in state.songs" :key="song.id" :song="song" :index="index+1" @play="player.playSong" /></div><p v-if="!state.songs.length" class="empty-text">{{ $root.T.noSongs }}</p></section>
    </section>
  `
};

const PlaylistsPage = {
  components: { TrackRow },
  setup() {
    const form = reactive({ name: "", songId: "" });
    const selectedId = ref(null);
    const selected = computed(() => state.playlists.find((p) => p.id === selectedId.value) || null);
    watch(() => state.playlists, () => { if (!state.playlists.length) selectedId.value = null; else if (!state.playlists.some((p) => p.id === selectedId.value)) selectedId.value = state.playlists[0].id; }, { immediate: true, deep: true });
    const create = async () => { if (!form.name.trim()) return; try { const p = await player.createPlaylist(form.name.trim()); form.name = ""; if (p) selectedId.value = p.id; } catch (e) { setErrorMessage(e); } };
    const del = async (id) => { try { await player.deletePlaylist(id); } catch (e) { setErrorMessage(e); } };
    const add = async () => { if (!selected.value || !form.songId) return; try { await player.addToPlaylist(selected.value.id, Number(form.songId)); form.songId = ""; } catch (e) { setErrorMessage(e); } };
    const remove = async (song) => { if (!selected.value) return; try { await player.removeFromPlaylist(selected.value.id, song.id); } catch (e) { setErrorMessage(e); } };
    return { state, form, selectedId, selected, create, del, add, remove, player };
  },
  template: `
    <section class="page-stack">
      <section class="playlist-hero"><div class="playlist-cover-block">P</div><div><span class="eyebrow">{{ $root.T.playlists }}</span><h2>{{ $root.T.playlists }}</h2><p>{{ $root.T.playlists }}</p></div></section>
      <section class="playlist-toolbar-grid"><div class="toolbar-card"><div class="section-head compact-head"><div><span>{{ $root.T.createPlaylist }}</span><h3>{{ $root.T.createPlaylist }}</h3></div></div><div class="double-row"><input v-model.trim="form.name" :placeholder="$root.T.playlistName"><button class="primary-button" type="button" @click="create">{{ $root.T.createPlaylist }}</button></div></div><div class="toolbar-card"><div class="section-head compact-head"><div><span>{{ $root.T.addSong }}</span><h3>{{ $root.T.addSong }}</h3></div></div><div class="double-row"><select v-model="form.songId"><option value="">{{ $root.T.chooseSong }}</option><option v-for="song in state.songs" :key="song.id" :value="song.id">{{ song.title }}</option></select><button class="ghost-button" type="button" @click="add">{{ $root.T.addSong }}</button></div></div></section>
      <section class="playlist-grid"><div class="content-section"><div class="section-head"><div><span>{{ $root.T.playlists }}</span><h3>{{ $root.T.playlists }}</h3></div></div><div class="track-list"><div v-for="playlist in state.playlists" :key="playlist.id" class="playlist-summary-card" :class="{ active: selected && selected.id === playlist.id }"><button class="playlist-summary-main" type="button" @click="selectedId = playlist.id"><strong>{{ playlist.name }}</strong><p>{{ playlist.songs.length }} {{ $root.T.songs }}</p></button><button class="danger-button text-only" type="button" @click="del(playlist.id)">{{ $root.T.delete }}</button></div></div><p v-if="!state.playlists.length" class="empty-text">{{ $root.T.noPlaylists }}</p></div><div class="content-section"><div class="section-head"><div><span>{{ selected ? selected.name : $root.T.playlists }}</span><h3>{{ selected ? selected.name : $root.T.playlists }}</h3></div></div><div class="track-list"><TrackRow v-for="(song,index) in (selected ? selected.songs : [])" :key="(selected ? selected.id : 'x') + '-' + song.id" :song="song" :index="index+1" :action-label="$root.T.remove" action-kind="danger-button" @play="player.playSong" @action="remove" /></div><p v-if="selected && !selected.songs.length" class="empty-text">{{ $root.T.noSongs }}</p><p v-if="!selected" class="empty-text">{{ $root.T.noPlaylists }}</p></div></section>
    </section>
  `
};

const NowPlayingPage = {
  setup() {
    const progress = computed(() => state.duration ? Math.min(100, Math.round((state.currentTime / state.duration) * 100)) : 0);
    const artStyle = computed(() => state.currentSong ? { backgroundImage: `linear-gradient(135deg, rgba(255,255,255,0.18), rgba(29,185,84,0.08)), url(${state.currentSong.coverUrl})` } : {});
    return { state, player, progress, artStyle, formatTime, subtitle };
  },
  template: `
    <section class="page-stack"><section class="content-section now-stage"><div class="now-stage-art" :style="artStyle"><img v-if="state.currentSong" class="now-cover" :src="state.currentSong.coverUrl" :alt="state.currentSong.title"><div v-else class="placeholder-cover">&#9835;</div></div><div class="now-stage-copy"><span class="eyebrow">{{ $root.T.now }}</span><h2>{{ state.currentSong ? state.currentSong.title : $root.T.noCurrent }}</h2><p>{{ state.currentSong ? subtitle(state.currentSong) : $root.T.noSongs }}</p><div class="progress-shell"><div class="progress-bar"><div class="progress-fill" :style="{ width: progress + '%' }"></div></div><div class="time-row"><span>{{ formatTime(state.currentTime) }}</span><span>{{ formatTime(state.duration) }}</span></div></div><div class="hero-controls"><button class="ghost-button pill-cta" type="button" @click="player.prev()">{{ $root.T.prev }}</button><button class="primary-button pill-cta" type="button" @click="player.toggle()">{{ state.isPlaying ? $root.T.pause : $root.T.play }}</button><button class="ghost-button pill-cta" type="button" @click="player.next()">{{ $root.T.next }}</button></div></div></section></section>
  `
};

const LoginPage = {
  setup() {
    const router = useRouter();
    const mode = ref("login");
    const form = reactive({ username: "", password: "" });
    const busy = ref(false);
    const submit = async () => {
      busy.value = true;
      try { await player.auth(form.username.trim(), form.password, mode.value); router.push("/"); } catch (e) { state.statusMessage = e.message; } finally { busy.value = false; }
    };
    const toggleLanguage = () => { i18n.lang = i18n.lang === "zh" ? "en" : "zh"; };
    return { state, mode, form, busy, submit, toggleLanguage, T };
  },
  template: `
    <div class="auth-layout"><div class="auth-panel"><button class="ghost-button text-only lang-switch-btn auth-lang-switch" type="button" @click="toggleLanguage">{{ T.switchLanguage }}</button><section class="auth-hero"><div><span class="eyebrow">{{ $root.T.app }}</span><h1>{{ $root.T.loginTitle }}</h1><p>{{ mode === 'login' ? $root.T.loginText : $root.T.registerText }}</p></div><div class="auth-badges"><span>{{ $root.T.library }}</span><span>{{ $root.T.importLocal }}</span><span>{{ $root.T.playlists }}</span></div></section><section class="auth-card"><div class="auth-tabs"><button type="button" :class="{ active: mode === 'login' }" @click="mode = 'login'">{{ $root.T.login }}</button><button type="button" :class="{ active: mode === 'register' }" @click="mode = 'register'">{{ $root.T.login === '登录' ? '注册' : 'Register' }}</button></div><input v-model.trim="form.username" :placeholder="$root.T.username"><input v-model="form.password" type="password" :placeholder="$root.T.password"><button class="primary-button wide" type="button" :disabled="busy" @click="submit">{{ mode === 'login' ? $root.T.login : ($root.T.login === '登录' ? '注册' : 'Register') }}</button><p class="status-text">{{ state.statusMessage }}</p></section></div></div>
  `
};

const routes = [
  { path: "/", component: HomePage },
  { path: "/songs", component: SongsPage },
  { path: "/playlists", component: PlaylistsPage },
  { path: "/now-playing", component: NowPlayingPage },
  { path: "/login", component: LoginPage }
];

const router = createRouter({ history: createWebHashHistory(), routes });
router.beforeEach((to) => {
  if (!state.currentUser && to.path !== "/login") return "/login";
  if (state.currentUser && to.path === "/login") return "/";
  return true;
});

const routeTitles = reactive({
  "/": T.value.home,
  "/songs": T.value.songs,
  "/playlists": T.value.playlists,
  "/now-playing": T.value.now,
  "/login": T.value.login
});

watch(T, () => {
  routeTitles["/"] = T.value.home;
  routeTitles["/songs"] = T.value.songs;
  routeTitles["/playlists"] = T.value.playlists;
  routeTitles["/now-playing"] = T.value.now;
  routeTitles["/login"] = T.value.login;
});

const RootApp = {
  setup() {
    const route = useRoute();
    const navTitle = computed(() => routeTitles[route.path] || T.value.app);
    const searchResults = computed(() => {
      const q = state.search.trim().toLowerCase();
      if (!q) return [];
      return state.songs.filter((song) => [song.title, song.artist, song.album].some((v) => String(v || "").toLowerCase().includes(q))).slice(0, 6);
    });
    const recent = computed(() => state.history.slice(0, 4).map((item) => item.song));
    const queue = computed(() => state.songs.slice(0, 3));
    const router = useRouter();
    const openSong = async (song) => { await player.playSong(song); router.push("/now-playing"); state.search = ""; };
    const logout = () => { player.logout(); router.push("/login"); };
    const toggleLanguage = () => { i18n.lang = i18n.lang === "zh" ? "en" : "zh"; };
    onMounted(() => { player.bootstrap(); });
    return { T, state, navTitle, searchResults, recent, queue, openSong, logout, player, formatTime, subtitle, toggleLanguage, i18n };
  },
  template: `
    <div :class="state.currentUser ? 'app-shell' : 'logged-out-shell'"><RouterView v-if="!state.currentUser" /><template v-else><div class="spotify-layout"><aside class="sidebar-shell"><section class="brand-card panel-card"><div class="brand-icon">M</div><div><strong>{{ T.app }}</strong><p>{{ T.app }}</p></div></section><nav class="sidebar-nav panel-card"><RouterLink class="sidebar-link" to="/"><span class="nav-icon">H</span><span>{{ T.home }}</span></RouterLink><RouterLink class="sidebar-link" to="/songs"><span class="nav-icon">S</span><span>{{ T.songs }}</span></RouterLink><RouterLink class="sidebar-link" to="/playlists"><span class="nav-icon">P</span><span>{{ T.playlists }}</span></RouterLink><RouterLink class="sidebar-link" to="/now-playing"><span class="nav-icon">N</span><span>{{ T.now }}</span></RouterLink></nav><section class="sidebar-library panel-card"><span class="eyebrow">{{ T.welcome }}</span><strong>{{ state.currentUser.displayName }}</strong><p>{{ state.statusMessage }}</p></section></aside><main class="main-shell"><header class="topbar-shell panel-card"><div><span class="eyebrow">{{ T.app }}</span><h1>{{ navTitle }}</h1><p class="status-text">{{ state.statusMessage }}</p></div><div class="topbar-right"><div class="search-shell"><input v-model.trim="state.search" class="search-input" :placeholder="T.search"><div v-if="state.search" class="search-dropdown"><button v-for="song in searchResults" :key="'s'+song.id" class="search-result" type="button" @click="openSong(song)"><img :src="song.coverUrl" :alt="song.title"><div><strong>{{ song.title }}</strong><p>{{ subtitle(song) }}</p></div></button><p v-if="!searchResults.length" class="empty-text">{{ T.noSongs }}</p></div></div><button class="ghost-button text-only lang-switch-btn" type="button" @click="toggleLanguage">{{ T.switchLanguage }}</button><div class="user-chip panel-soft"><div><strong>{{ state.currentUser.displayName }}</strong><p>{{ state.currentUser.username }}</p></div><button class="ghost-button text-only" type="button" @click="logout">{{ T.logout }}</button></div></div></header><RouterView /></main><aside class="insight-shell panel-card"><div class="section-head compact-head"><div><span>{{ T.history }}</span><h3>{{ T.history }}</h3></div></div><p class="section-note"></p><div class="insight-current panel-soft"><img v-if="state.currentSong" :src="state.currentSong.coverUrl" :alt="state.currentSong.title"><div v-else class="placeholder-dock">M</div><div><strong>{{ state.currentSong ? state.currentSong.title : T.noCurrent }}</strong><p>{{ state.currentSong ? subtitle(state.currentSong) : T.noSongs }}</p></div></div><div class="insight-block"><div class="section-head compact-head"><div><span>{{ T.history }}</span><h3>{{ T.history }}</h3></div></div><div class="mini-card-list"><button v-for="song in recent" :key="'r'+song.id" class="mini-song-card" type="button" @click="openSong(song)"><img :src="song.coverUrl" :alt="song.title"><div><strong>{{ song.title }}</strong><p>{{ subtitle(song) }}</p></div></button></div><p v-if="!recent.length" class="empty-text">{{ T.noHistory }}</p></div><div class="insight-block"><div class="section-head compact-head"><div><span>{{ T.queue }}</span><h3>{{ T.queue }}</h3></div></div><div class="mini-card-list"><button v-for="song in queue" :key="'q'+song.id" class="mini-song-card" type="button" @click="openSong(song)"><img :src="song.coverUrl" :alt="song.title"><div><strong>{{ song.title }}</strong><p>{{ formatTime(song.durationSeconds) }}</p></div></button></div><p v-if="!queue.length" class="empty-text">{{ T.noSongs }}</p></div></aside></div><footer class="player-dock"><div class="dock-song"><img v-if="state.currentSong" class="dock-cover" :src="state.currentSong.coverUrl" :alt="state.currentSong.title"><div v-else class="placeholder-dock">M</div><div class="dock-copy"><strong>{{ state.currentSong ? state.currentSong.title : T.noCurrent }}</strong><p>{{ state.currentSong ? subtitle(state.currentSong) : T.noSongs }}</p></div></div><div class="dock-center"><div class="dock-controls"><button class="ghost-button pill-cta" type="button" @click="player.prev()">{{ T.prev }}</button><button class="primary-button pill-cta" type="button" @click="player.toggle()">{{ state.isPlaying ? T.pause : T.play }}</button><button class="ghost-button pill-cta" type="button" @click="player.next()">{{ T.next }}</button></div><div class="dock-progress"><span>{{ formatTime(state.currentTime) }}</span><input type="range" min="0" :max="Math.max(state.duration, 0)" :value="state.currentTime" @input="player.seek($event.target.value)"><span>{{ formatTime(state.duration) }}</span></div></div><div class="dock-volume"><span>{{ T.volume }}</span><input type="range" min="0" max="1" step="0.01" :value="state.volume" @input="player.setVolume($event.target.value)"></div></footer></template></div>
  `
};

const app = createApp(RootApp).use(router);
app.config.globalProperties.T = T;
app.mount("#app");
