package com.bsutton.sounds;
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


import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.arch.core.util.Function;
import androidx.core.app.ActivityCompat;

import android.media.AudioFocusRequest;

import java.io.*;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import java.util.concurrent.Callable;

import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import com.bsutton.sounds.SoundPlayer;
import com.bsutton.sounds.MediaBrowserHelper;
import com.bsutton.sounds.Track;


class TrackPlayerPlugin
	extends SoundPlayerPlugin
	implements MethodCallHandler
{
	public static MethodChannel     channel;
	static        Context           androidContext;
	static        TrackPlayerPlugin trackPlayerPlugin; // singleton


	public static void attachTrackPlayer( Context ctx, BinaryMessenger messenger )
	{
		assert ( flautoPlayerPlugin == null );
		
		trackPlayerPlugin = new TrackPlayerPlugin();
		assert ( slots == null );
		slots   = new ArrayList<SoundPlayer>();
		channel           = new MethodChannel( messenger, "com.bsutton.sounds.sounds_track_player" );
		channel.setMethodCallHandler( trackPlayerPlugin );
		androidContext = ctx;

	}

	void invokeMethod( String methodName, Map dic )
	{
		channel.invokeMethod ( methodName, dic );
	}

	void freeSlot ( int slotNo )
	{
		slots.set ( slotNo, null );
	}


	SoundPlayerPlugin getManager ()
	{
		return flautoPlayerPlugin;
	}



	@Override
	public void onMethodCall( final MethodCall call, final Result result )
	{
		int slotNo = call.argument ( "slotNo" );
		Log.d(TAG, "onMethodCall called: " + call.method + " for slot: " + slotNo);
		
		// The dart code supports lazy initialization of players.
		// This means that players can be registered (and slots allocated)
		// on the client side in a different order to which the players
		// are initialised.
		// As such we need to grow the slot array upto the
		// requested slot no. even if we haven't seen initialisation
		// for the lower numbered slots.
		while (slotNo >= slots.size()) {
			slots.add(null);
		}

		TrackPlayer aPlayer = (TrackPlayer)slots.get ( slotNo );
		switch ( call.method )
		{
			case "initializeMediaPlayer":
			{
				assert ( slots.get ( slotNo ) == null );
				aPlayer = new TrackPlayer ( slotNo );
				slots.set ( slotNo, aPlayer );
				aPlayer.initializeSoundPlayer ( call, result );
				Log.d("TrackPlayer", "************* initialize called");
			}
			break;

			case "releaseMediaPlayer":
			{
				aPlayer.releaseSoundPlayer( call, result );
				Log.d("TrackPlayer", "************* release called");
			}
			break;

			case "startPlayerFromTrack":
				aPlayer.startPlayerFromTrack( call, result );
				break;


			case "stopPlayer":
				aPlayer.stopPlayer(call,  result );
				break;
			case "pausePlayer":
				aPlayer.pausePlayer(call,  result );
				break;
			case "resumePlayer":
				aPlayer.resumePlayer( call, result );
				break;
			case "seekToPlayer":
				aPlayer.seekToPlayer( call, result );
				break;
			case "setVolume":
				aPlayer.setVolume( call, result );
				break;
			case "setSubscriptionDuration":
				if ( call.argument( "sec" ) == null )
				{
					return;
				}
				aPlayer.setSubscriptionDuration( call, result );
				break;

			default:
				super.onMethodCall( call, result );
				break;
			}
	}

}

//-----------------------------------------------------------------------------------------------------------------------------


public class TrackPlayer extends SoundPlayer
{
	private       MediaBrowserHelper mMediaBrowserHelper;
	private       Timer              mTimer      = new Timer();
	final private Handler            mainHandler = new Handler();

	TrackPlayer ( int aSlotNo )
	{
		super(aSlotNo);
		//slotNo = aSlotNo;
	}


	SoundPlayerPlugin getPlugin ()
	{
		return TrackPlayerPlugin.trackPlayerPlugin;
	}


	@Override
	void initializeSoundPlayer( final MethodCall call, final Result result )
	{
		//super.initializeSoundPlayer( call, result );
		audioManager = ( AudioManager ) SoundPlayerPlugin.androidContext.getSystemService ( Context.AUDIO_SERVICE );
		assert(Sounds.androidActivity != null);

		// Initialize the media browser if it hasn't already been initialized
		if ( mMediaBrowserHelper == null )
		{
			// If the initialization will be successful, result.success will
			// be called, otherwise result.error will be called.
			mMediaBrowserHelper = new MediaBrowserHelper( new MediaPlayerConnectionListener( result, true ), new MediaPlayerConnectionListener( result, false ) );
			// Pass the playback state updater to the media browser
			mMediaBrowserHelper.setPlaybackStateUpdater(new PlaybackStateUpdater());

		}
		result.success( "The player had already been initialized." );
	}

	@Override
	void releaseSoundPlayer( final MethodCall call, final Result result )
	{
		// Throw an error if the media player is not initialized
		if ( mMediaBrowserHelper == null )
		{
			result.error( TAG, "The player cannot be released because it is not initialized.", null );
			return;
		}

		// Release the media browser
		mMediaBrowserHelper.releaseMediaBrowser();
		mMediaBrowserHelper = null;
		result.success( "The player has been successfully released" );
	}

	void invokeMethodWithInteger ( String methodName, double arg )
	{
		Map<String, Object> dic = new HashMap<String, Object> ();
		dic.put ( "slotNo", slotNo );
		dic.put ( "arg", arg );
		getPlugin ().invokeMethod ( methodName, dic );
	}

	void invokeMethodWithBoolean ( String methodName, Boolean arg )
	{
		Map<String, Object> dic = new HashMap<String, Object> ();
		dic.put ( "slotNo", slotNo );
		dic.put ( "arg", arg );
		getPlugin ().invokeMethod ( methodName, dic );
	}



	public void startPlayerFromTrack( final MethodCall call, final Result result )
	{
		final HashMap<String, Object> trackMap = call.argument( "track" );
		final Track track = new Track( trackMap );

		boolean canSkipForward = call.argument( "canSkipForward" );
		boolean canSkipBackward = call.argument( "canSkipBackward" );
		boolean canPause = call.argument( "canPause" );

		// Exit the method if a media browser helper was not initialized
		if ( !wasMediaPlayerInitialized( result ) )
		{
			return;
		}

		// Check whether the audio file is stored by a string or a buffer
		String path;
		if ( track.isUsingPath() )
		{
			// The audio file is stored by a String, then get the path to the file audio to
			// play
			path = track.getPath();
		} else
		{
			// The audio file is stored by a buffer, then save it as a file and get the path
			// to that file.
			//
			// NOTE: this code is no longer used. The Dart code will ALWAYS pass a file
			try
			{
				File             f   = File.createTempFile( "sounds", extentionArray[ track.getBufferCodecIndex() ] );
				FileOutputStream fos = new FileOutputStream( f );
				fos.write( track.getDataBuffer() );
				path = f.getAbsolutePath();
			}
			catch ( Exception e )
			{
				Log.e(TAG, e.getMessage(), e);
				result.error( ERR_UNKNOWN, ERR_UNKNOWN, e.getMessage() );
				return;
			}
		}

		mTimer = new Timer();

		if ( canPause )
		{
			mMediaBrowserHelper.setPauseHandler( new PauseHandler(  ) );
		} else
		{
			mMediaBrowserHelper.removePauseHandler();
		}


		// Add or remove the handlers for when the user tries to skip the current track
		if ( canSkipForward )
		{
			mMediaBrowserHelper.setSkipTrackForwardHandler( new SkipTrackHandler( true ) );
		} else
		{
			mMediaBrowserHelper.removeSkipTrackForwardHandler();
		}
		if ( canSkipBackward )
		{
			mMediaBrowserHelper.setSkipTrackBackwardHandler( new SkipTrackHandler( false ) );
		} else
		{
			mMediaBrowserHelper.removeSkipTrackBackwardHandler();
		}

		if ( canPause )
		{
			mMediaBrowserHelper.setPauseHandler( new PauseHandler(  ) );
		} else
		{
			mMediaBrowserHelper.removePauseHandler();
		}


		if ( setActiveDone == t_SET_CATEGORY_DONE.NOT_SET )
		{
			requestFocus();
			setActiveDone = t_SET_CATEGORY_DONE.FOR_PLAYING;
		}

		// Pass to the media browser the metadata to use in the notification
		mMediaBrowserHelper.setNotificationMetadata( track );

		// Add the listeners for the onPrepared and onCompletion events
		mMediaBrowserHelper.setMediaPlayerOnPreparedListener( new MediaPlayerOnPreparedListener( result, path ) );
		mMediaBrowserHelper.setMediaPlayerOnCompletionListener( new MediaPlayerOnCompletionListener() );

		// Check whether a path to an audio file was given
		if ( path == null )
		{
			// No paths were given, then use the default file
			mMediaBrowserHelper.mediaControllerCompat.getTransportControls().playFromMediaId( PlayerAudioModel.DEFAULT_FILE_LOCATION, null );
		} else
		{
			// A path was given, then send it to the media player
			mMediaBrowserHelper.mediaControllerCompat.getTransportControls().playFromMediaId( path, null );
		}

		// The media player is started in the on prepared callback
	}


	private boolean _stopPlayer()
	{
		// This remove all pending runnables
		mTimer.cancel();
		if ( mMediaBrowserHelper == null )
			return false;
		if ( ( setActiveDone != t_SET_CATEGORY_DONE.BY_USER ) && ( setActiveDone != t_SET_CATEGORY_DONE.NOT_SET ) )
		{
			abandonFocus();
			setActiveDone = t_SET_CATEGORY_DONE.NOT_SET;
		}
		try
		{
			// Stop the playback
			mMediaBrowserHelper.stop();
		}
		catch ( Exception e )
		{
			return false;
		}
		return true;
	}

	@Override
	public void stopPlayer(final MethodCall call, final Result result )
	{
		_stopPlayer();
		result.success( "Unknown result" );
	}

	@Override
	public void pausePlayer(final MethodCall call, final Result result )
	{
		// Exit the method if a media browser helper was not initialized
		if ( !wasMediaPlayerInitialized( result ) )
		{
			return;
		}

		if ( ( setActiveDone != t_SET_CATEGORY_DONE.BY_USER ) && ( setActiveDone != t_SET_CATEGORY_DONE.NOT_SET ) )
		{
			abandonFocus();
			setActiveDone = t_SET_CATEGORY_DONE.NOT_SET;
		}

		try
		{
			// Pause the media player
			mMediaBrowserHelper.pausePlayback();
			result.success( "paused player." );
		}
		catch ( Exception e )
		{
			Log.e( TAG, "pausePlay exception: " + e.getMessage() );
			result.error( ERR_UNKNOWN, ERR_UNKNOWN, e.getMessage() );
		}
	}

	@Override
	public void resumePlayer( final MethodCall call,final Result result )
	{
		// Exit the method if a media browser helper was not initialized
		if ( !wasMediaPlayerInitialized( result ) )
		{
			return;
		}

		// Throw an error if we can't resume the media player because it is already
		// playing
		PlaybackStateCompat playbackState = mMediaBrowserHelper.mediaControllerCompat.getPlaybackState();
		if ( playbackState != null && playbackState.getState() == PlaybackStateCompat.STATE_PLAYING )
		{
			result.error( ERR_PLAYER_IS_PLAYING, ERR_PLAYER_IS_PLAYING, ERR_PLAYER_IS_PLAYING );
			return;
		}
		if ( setActiveDone == t_SET_CATEGORY_DONE.NOT_SET )
		{
			requestFocus();
			setActiveDone = t_SET_CATEGORY_DONE.FOR_PLAYING;
		}

		try
		{
			// Resume the player
			mMediaBrowserHelper.resumePlayback();

			// Seek the player to the last position and resume it
			result.success( "resumed player." );
		}
		catch ( Exception e )
		{
			Log.e( TAG, "mediaPlayer resume: " + e.getMessage() );
			result.error( ERR_UNKNOWN, ERR_UNKNOWN, e.getMessage() );
		}
	}

	@Override
	public void seekToPlayer(final MethodCall call, Result result )
	{
		int millis = call.argument ( "sec" ) ;

		// Exit the method if a media browser helper was not initialized
		if ( !wasMediaPlayerInitialized( result ) )
		{
			Log.d(TAG, "seekToPlayer ended with no initialization");
			return;
		}

		mMediaBrowserHelper.seekTo(millis);
		// Should declaratively change state: https://stackoverflow.com/questions/39719320/seekto-does-not-trigger-onplaybackstatechanged-in-mediacontrollercompat
		mMediaBrowserHelper.playPlayback();

		result.success( String.valueOf( millis ) );
	}

	@Override
	public void setVolume(final MethodCall call,final Result result )
	{
		// Exit the method if a media browser helper was not initialized
		if ( !wasMediaPlayerInitialized( result ) )
		{
			return;
		}
		double volume = call.argument("volume");
		float mVolume = (float) volume;

		// Get the maximum value for the volume
		int maxVolume = mMediaBrowserHelper.mediaControllerCompat.getPlaybackInfo().getMaxVolume();
		// Get the value of the new volume level
		int newVolume = ( int ) Math.floor( mVolume * maxVolume );

		// Adjust the media player volume to the given level
		mMediaBrowserHelper.mediaControllerCompat.setVolumeTo( newVolume, 0 );
		result.success( "Set volume" );
	}


	public void setSubscriptionDuration( final MethodCall call, Result result )
	{
		if (call.argument("sec") == null)
			return;
		double duration = call.argument("sec");

		this.model.subsDurationMillis = ( int ) ( duration * 1000 );
		result.success( "setSubscriptionDuration: " + this.model.subsDurationMillis );
	}

	private boolean wasMediaPlayerInitialized(  final Result result )
	{
		if ( mMediaBrowserHelper == null )
		{
			Log.e( TAG, "initializePlayer() must be called before this method." );
			result.error( TAG, "initializePlayer() must be called before this method.", null );
			return false;
		}

		return true;
	}


//-------------------------------------------------------------------------------------------------------------------------------

	/**
	 * The callable instance to call when the media player has been connected.
	 */
	private class MediaPlayerConnectionListener
		implements Callable<Void>
	{
		private Result  mResult;
		// Whether this callback is called when the connection is successful
		private boolean mIsSuccessfulCallback;

		MediaPlayerConnectionListener( Result result, boolean isSuccessfulCallback )
		{
			mResult               = result;
			mIsSuccessfulCallback = isSuccessfulCallback;
		}

		@Override
		public Void call()
			throws
			Exception
		{
			if ( mIsSuccessfulCallback )
			{
				//mResult.success( "The media player has been successfully initialized" );
				invokeMethodWithBoolean("onPlayerReady", true);
			} else
			{
				invokeMethodWithBoolean("onPlayerReady", false);
				//mResult.error( TAG, "An error occurred while initializing the media player", null );
			}
			return null;
		}
	}

	/**
	 * A listener that is triggered when the pause buttons in the notification are
	 * clicked.
	 */
	private class PauseHandler
		implements Callable<Void>
	{
		private boolean mIsSkippingForward;

		PauseHandler(  )
		{
		}

		@Override
		public Void call()
			throws
			Exception
		{
			PlaybackStateCompat playbackState = mMediaBrowserHelper.mediaControllerCompat.getPlaybackState();
			if (playbackState.getState() == PlaybackStateCompat.STATE_PLAYING)
				invokeMethodWithBoolean("pause", true);
			else
				invokeMethodWithBoolean("resume", true);

			return null;
		}
	}




	/**
	 * A listener that is triggered when the skip buttons in the notification are
	 * clicked.
	 */
	private class SkipTrackHandler
		implements Callable<Void>
	{
		private boolean mIsSkippingForward;

		SkipTrackHandler( boolean isSkippingForward )
		{
			mIsSkippingForward = isSkippingForward;
		}

		@Override
		public Void call()
			throws
			Exception
		{
			if ( mIsSkippingForward )
			{
				invokeMethodWithString( "skipForward", null );
			} else
			{
				invokeMethodWithString( "skipBackward", null );
			}

			return null;
		}
	}

	


	/**
	 * A function that triggers a function in the Dart code to update the playback
	 * state.
	 */
	private class PlaybackStateUpdater implements Function<BackgroundAudioService.SystemPlaybackState, Void> {
		@Override
		public Void apply(BackgroundAudioService.SystemPlaybackState newState) {
			invokeMethodWithInteger("updatePlaybackState", newState.stateNo);
			return null;
		}
	}
	/**
	 * The callable instance to call when the media player is prepared.
	 */
	private class MediaPlayerOnPreparedListener
		implements Callable<Void>
	{
		private Result mResult;
		private String mPath;

		private MediaPlayerOnPreparedListener(
			Result result, String path
		                                     )
		{
			mResult = result;
			mPath   = path;
		}

		@Override
		public Void call()
			throws
			Exception
		{
			// The content is ready to be played, then play it
			mMediaBrowserHelper.playPlayback();

			// Set timer task to send event to RN
			long trackDuration = mMediaBrowserHelper.mediaControllerCompat.getMetadata().getLong( MediaMetadataCompat.METADATA_KEY_DURATION );

			TimerTask mTask = new TimerTask()
			{
				@Override
				public void run()
				{
					// long time = mp.getCurrentPosition();
					// DateFormat format = new SimpleDateFormat("mm:ss:SS", Locale.US);
					// final String displayTime = format.format(time);

					try
					{
						if ((mMediaBrowserHelper == null) || (mMediaBrowserHelper.mediaControllerCompat == null))
						{
							Log.e( TAG, "MediaPlayerOnPreparedListener timer: mMediaBrowserHelper.mediaControllerCompat is NULL. This is BAD !!!"  );

							_stopPlayer( );
							if (mMediaBrowserHelper != null)
								mMediaBrowserHelper.releaseMediaBrowser();
							mMediaBrowserHelper = null;
							return;
						}
						JSONObject          json          = new JSONObject();
						PlaybackStateCompat playbackState = mMediaBrowserHelper.mediaControllerCompat.getPlaybackState();

						if ( playbackState == null )
						{
							return;
						}

						long currentPosition = playbackState.getPosition();

						json.put( "duration", String.valueOf( trackDuration ) );
						json.put( "current_position", String.valueOf( currentPosition ) );
						mainHandler.post( new Runnable()
						{
							@Override
							public void run()
							{
								invokeMethodWithString( "updateProgress", json.toString() );
							}
						} );

					}
					catch ( JSONException je )
					{
						Log.d( TAG, "Json Exception: " + je.toString() );
					}
				}
			};

			mTimer.schedule( mTask, 0, model.subsDurationMillis );
			String resolvedPath = mPath == null ? PlayerAudioModel.DEFAULT_FILE_LOCATION : mPath;
			mResult.success( ( resolvedPath ) );

			return null;
		}
	}


	/**
	 * The callable instance to call when the media player calls the onCompletion
	 * event.
	 */
	private class MediaPlayerOnCompletionListener
		implements Callable<Void>
	{
		MediaPlayerOnCompletionListener()
		{
		}

		@Override
		public Void call()
			throws
			Exception
		{
			// Reset the timer
			mTimer.cancel();
			long trackDuration = mMediaBrowserHelper.mediaControllerCompat.getMetadata().getLong( MediaMetadataCompat.METADATA_KEY_DURATION );

			Log.d( TAG, "Plays completed." );
			try
			{
				JSONObject json            = new JSONObject();
				long       currentPosition = mMediaBrowserHelper.mediaControllerCompat.getPlaybackState().getPosition();

				json.put( "duration", String.valueOf( trackDuration ) );
				json.put( "current_position", String.valueOf( currentPosition ) );
				invokeMethodWithString( "audioPlayerFinishedPlaying", json.toString() );
				if ( ( setActiveDone != t_SET_CATEGORY_DONE.BY_USER ) && ( setActiveDone != t_SET_CATEGORY_DONE.NOT_SET ) )
				{
					abandonFocus();
					setActiveDone = t_SET_CATEGORY_DONE.NOT_SET;
				}
			}
			catch ( JSONException je )
			{
				Log.d( TAG, "Json Exception: " + je.toString() );
			}

			return null;
		}
	}

}
//---------------------------------------------------------------------------------------------------------------------------------
