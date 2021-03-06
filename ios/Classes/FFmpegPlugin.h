/*
 * Copyright (c) 2019 Taner Sener
 *
 * This file is part of FlutterFFmpeg.
 *
 * FlutterFFmpeg is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlutterFFmpeg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FlutterFFmpeg.  If not, see <http://www.gnu.org/licenses/>.
 */

#define FULL_FLAVOR
#ifdef FULL_FLAVOR

#import <Flutter/Flutter.h>
#import <mobileffmpeg/MobileFFmpegConfig.h>

/**
 * Flutter FFmpeg Plugin
 */
@interface FFmpegPlugin : NSObject<FlutterPlugin,FlutterStreamHandler,LogDelegate,StatisticsDelegate> {
        FFmpegPlugin* FFmpegPlugin; // Singleton
}

@end

extern void FfmpegReg(NSObject<FlutterPluginRegistrar>* registrar);

#endif // FULL_FLAVOR
