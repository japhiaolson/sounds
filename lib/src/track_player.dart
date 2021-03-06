import 'dart:async';

import 'audio_focus.dart';

import 'codec.dart';
import 'playback_disposition.dart';
import 'sound_player.dart';
import 'track.dart';

/// @deprecated use [SoundPlayer.withUI()]
class TrackPlayer {
  SoundPlayer _player;

  ///
  TrackPlayer(
      {bool canPause = true,
      bool canSkipBackward = false,
      bool canSkipForward = false,
      bool playInBackground = false}) {
    _player = SoundPlayer.withUI(
        canPause: canPause,
        canSkipBackward: canSkipBackward,
        canSkipForward: canSkipForward,
        playInBackground: playInBackground);
  }

  /// initialize the TrackPlayer.
  /// You do not need to call this as the player auto initializes itself
  /// and in fact has to re-initialize its self after an app pause.
  void initialize() {
    // NOOP - as its not required but apparently wanted.
  }

  /// call this method once you are done with the player
  /// so that it can release all of the attached resources.
  ///
  Future<void> release() async {
    return _player.release();
  }

  ///
  void showPauseButton(PauseButtonMode mode) {
    /// TODO: I don't know how to control the pause button.
  }

  /// Starts playback.
  /// The [track] to play.
  Future<void> play(Track track) async {
    return _player.play(track);
  }

  /// Stops playback.
  Future<void> stop() async {
    return _player.stop();
  }

  /// Pauses playback.
  /// If you call this and the audio is not playing
  /// a [PlayerInvalidStateException] will be thrown.
  Future<void> pause() async {
    return _player.pause();
  }

  /// Resumes playback.
  /// If you call this when audio is not paused
  /// then a [PlayerInvalidStateException] will be thrown.
  Future<void> resume() async {
    return _player.resume();
  }

  ///
  Future<void> seekTo(Duration position) async {
    return _player.seekTo(position);
  }

  /// Rewinds the current track by the given interval
  Future<void> rewind(Duration interval) {
    return _player.rewind(interval);
  }

  /// Sets the playback volume
  /// The [volume] must be in the range 0.0 to 1.0.
  Future<void> setVolume(double volume) async {
    return _player.setVolume(volume);
  }

  /// Returns true if the specified decoder is supported
  ///  by sounds on this platform
  Future<bool> isSupported(Codec codec) async {
    return _player.isSupported(codec);
  }

  ///  The caller can manage his audio focus with this function
  /// Depending on your configuration this will either make
  /// this player the loudest stream or it will silence all other stream.
  Future<void> audioFocus(AudioFocus mode) async {
    return _player.audioFocus(mode);
  }

  /// [true] if the player is currently playing audio
  bool get isPlaying => _player.isPlaying;

  /// [true] if the player is playing but the audio is paused
  bool get isPaused => _player.isPaused;

  /// [true] if the player is stopped.
  bool get isStopped => _player.isStopped;

  ///
  Stream<PlaybackDisposition> dispositionStream(
      {Duration interval = const Duration(milliseconds: 100)}) {
    return _player.dispositionStream(interval: interval);
  }

  ///
  // ignore: avoid_setters_without_getters
  set onSkipBackward(PlayerEvent onSkipBackward) {
    _player.onSkipBackward = onSkipBackward;
  }

  ///
  // ignore: avoid_setters_without_getters
  set onSkipForward(PlayerEvent onSkipForward) {
    _player.onSkipForward = onSkipForward;
  }

  /// Pass a callback if you want to be notified
  /// when the OS Media Player changs state.
  // ignore: avoid_setters_without_getters
  set onUpdatePlaybackState(OSPlayerStateEvent onUpdatePlaybackState) {
    _player.onUpdatePlaybackState = onUpdatePlaybackState;
  }

  ///
  /// Pass a callback if you want to be notified when
  /// playback is paused.
  /// The [wasUser] argument in the callback will
  /// be true if the user clicked the pause button
  /// on the OS UI.  To show the OS UI you must have called
  /// [SoundPlayer.withUI].
  ///
  /// [wasUser] will be false if you paused the audio
  /// via a call to [pause].
  // ignore: avoid_setters_without_getters
  set onPaused(PlayerEventWithCause onPaused) {
    _player.onPaused = onPaused;
  }

  ///
  /// Pass a callback if you want to be notified when
  /// playback is resumed.
  /// The [wasUser] argument in the callback will
  /// be true if the user clicked the resume button
  /// on the OS UI.  To show the OS UI you must have called
  /// [SoundPlayer.withUI].
  ///
  /// [wasUser] will be false if you resumed the audio
  /// via a call to [resume].
  // ignore: avoid_setters_without_getters
  set onResumed(PlayerEventWithCause onResumed) {
    _player.onResumed = onResumed;
  }

  /// Pass a callback if you want to be notified
  /// that audio has started playing.
  ///
  /// If the player has to download or transcribe
  /// the audio then this method won't return
  /// util the audio actually starts to play.
  ///
  /// This can occur if you called [play]
  /// or the user click the start button on the
  /// OS UI. To show the OS UI you must have called
  /// [SoundPlayer.withUI].
  // ignore: avoid_setters_without_getters
  set onStarted(PlayerEventWithCause onStarted) {
    _player.onStarted = onStarted;
  }

  /// Pass a callback if you want to be notified
  /// that audio has stopped playing.
  /// [onStoppped]  can occur if you called [stop]
  /// or the user click the stop button (widget or OS UI).
  ///
  // ignore: avoid_setters_without_getters
  set onStopped(PlayerEventWithCause onStopped) {
    _player.onStopped = onStopped;
  }
}

///
enum PauseButtonMode {
  ///
  hidden,

  ///
  disabled,

  ///
  paused,

  ///
  playing,

  ///
  auto,
}
