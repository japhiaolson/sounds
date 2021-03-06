/*
 * This file is part of Sounds .
 *
 *   Sounds  is free software: you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License
 *   version 3 (LGPL3) as published by the Free Software Foundation.
 *
 *   Sounds  is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the Lesser GNU General Public License
 *   along with Sounds .  If not, see <https://www.gnu.org/licenses/>.
 */
 
#ifndef TrackPlayer_h
#define TrackPlayer_h


#import <Flutter/Flutter.h>
#import "SoundPlayer.h"

extern void TrackPlayerReg(NSObject<FlutterPluginRegistrar>* registrar);



@interface TrackPlayerManager : SoundPlayerManager
{
}

+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar;
@end


@interface TrackPlayer : SoundPlayer
{

}
- (TrackPlayer*)init: (int)aSlotNo;
- (void)startPlayerFromTrack:(FlutterMethodCall*)call result: (FlutterResult)result;
- (void)initializeTrackPlayer: (FlutterMethodCall*)call result: (FlutterResult)result;
- (void)releaseTrackPlayer:(FlutterMethodCall *)call result:(FlutterResult)result;
- (void)freeSlot: (int)slotNo;

@end

#endif // TrackPlayer_h
