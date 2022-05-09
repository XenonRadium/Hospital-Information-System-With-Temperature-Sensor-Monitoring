package com.example.prototype;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.provider.MediaStore;
import android.widget.Toast;

public class MyPlayService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener
{
    private MediaPlayer musicPlayer;

    public MyPlayService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        musicPlayer = new MediaPlayer();

        musicPlayer.setOnCompletionListener(this);
        musicPlayer.setOnPreparedListener(this);
        musicPlayer.setOnErrorListener(this);
        musicPlayer.setOnSeekCompleteListener(this);
        musicPlayer.setOnInfoListener(this);
        musicPlayer.setOnBufferingUpdateListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        musicPlayer.reset();

        if (!musicPlayer.isPlaying()) {
            try {
                AssetFileDescriptor afd = this.getResources().openRawResourceFd(R.raw.emergency_alarm);
                musicPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                musicPlayer.prepareAsync();
            }catch (Exception e){
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(musicPlayer!=null){
            if(musicPlayer.isPlaying())
            {
                musicPlayer.stop();
            }

            musicPlayer.release();
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {

    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

        mediaPlayer.seekTo(0);
        mediaPlayer.start();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {

        switch (i)
        {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Toast.makeText(this, "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK", Toast.LENGTH_SHORT).show();
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Toast.makeText(this, "MEDIA_ERROR_SERVER_DIED", Toast.LENGTH_SHORT).show();
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Toast.makeText(this, "MEDIA_ERROR_UNKNOWN", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        if(!mediaPlayer.isPlaying())
        {
            mediaPlayer.start();
        }
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {

    }
}