import 'package:sounds/sounds.dart';

import 'demo_common.dart';

/// Paths for example media files.
class MediaPath {
  static final MediaPath _self = MediaPath._internal();

  /// list of sample paths for each codec
  static const List<String> paths = [
    'sounds_example.aac', // DEFAULT
    'sounds_example.aac', // CODEC_AAC
    'sounds_example.opus', // CODEC_OPUS
    'sounds_example.caf', // CODEC_CAF_OPUS
    'sounds_example.mp3', // CODEC_MP3
    'sounds_example.ogg', // CODEC_VORBIS
    'sounds_example.wav', // CODEC_PCM
  ];

  final List<String> _path = [null, null, null, null, null, null, null];

  /// The media we are storing
  MediaStorage media = MediaStorage.file;

  /// ctor
  factory MediaPath() {
    return _self;
  }
  MediaPath._internal();

  /// true if the media is an asset
  bool get isAsset => media == MediaStorage.asset;

  /// true if the media is an file
  bool get isFile => media == MediaStorage.file;

  /// true if the media is an buffer
  bool get isBuffer => media == MediaStorage.buffer;

  /// true if the media is the example file.
  bool get isExampleFile => media == MediaStorage.remoteExampleFile;

  /// Sets the location of the file for the given codec.
  void setCodecPath(Codec codec, String path) {
    _path[codec.index] = path;
  }

  /// returns the path to the file for the given codec.
  String pathForCodec(Codec codec) {
    return _path[codec.index];
  }

  /// [true] if a path for the give codec exists.
  bool exists(Codec codec) {
    return _path[codec.index] != null;
  }
}
