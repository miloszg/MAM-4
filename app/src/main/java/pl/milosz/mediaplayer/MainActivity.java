package pl.milosz.mediaplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    MediaPlayer player;
    MediaRecorder recorder;
    SeekBar musicBar;
    String path="";
    TextView startTime, endTime, musicTextName;
    Button deathGripsButton, recordedSoundButton, startRecordButton, stopRecordButton;
    int musicId=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startTime = findViewById(R.id.startTimeText);
        endTime = findViewById(R.id.endTimeText);
        musicBar = findViewById(R.id.musicBar);
        musicTextName = findViewById(R.id.musicNameText);
        recordedSoundButton = findViewById(R.id.recordedSoundButton);
        startRecordButton = findViewById(R.id.startRecordButton);
        stopRecordButton = findViewById(R.id.stopRecordButton);
        deathGripsButton = findViewById(R.id.deathGripsButton);
        deathGripsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicId=0;
                musicTextName.setText("Wybrano: Death Grips - Guillotine");
            }
        });
        recordedSoundButton = findViewById(R.id.recordedSoundButton);
        recordedSoundButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                musicId=1;
                musicTextName.setText("Wybrano: plik z nagraniem");
            }
        });

        if(checkSelfPermission()){
            startRecordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+ UUID.randomUUID().toString()+"_audio.3gp";
                    setupMediaRecorder();
                    try{
                        recorder.prepare();
                        recorder.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(MainActivity.this, "Rozpoczęcie nagrywania...", Toast.LENGTH_SHORT).show();
                    startRecordButton.setEnabled(false);
                }
            });
            stopRecordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    recorder.stop();
                    startRecordButton.setEnabled(true);
                }
            });
        } else {
            requestPermissions();
        }

    }

    private void setupMediaRecorder() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        recorder.setOutputFile(path);
    }

    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        }, 1000);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1000: {
                if(grantResults.length > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "Nadano uprawnienia do nagrywania dzwięku", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Nie nadano uprawnień aplikacji", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    private boolean checkSelfPermission() {
        int write_result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int record_result = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return write_result == PackageManager.PERMISSION_GRANTED && record_result == PackageManager.PERMISSION_GRANTED;
    }

    public void play(View view){
        if(player == null){
            if(musicId == 0){
                player = MediaPlayer.create(this, R.raw.deathgrips);
            } else {
                if(path.length()>0){
                    try {
                        player = new MediaPlayer();
                        player.setDataSource(path);
                        player.prepare();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, "Nie udało się wgrać pliku", Toast.LENGTH_SHORT).show();
                }
            }

            player.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
                @Override
                public void onPrepared(MediaPlayer mp) {
                    musicBar.setMax(mp.getDuration());
                    endTime.setText(timeLabel(mp.getDuration()));
                }
            });
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlayer();
                }
            });
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (player != null) {
                    try {
                        if (player.isPlaying() && player!=null) {
                            Message msg = new Message();
                            msg.what = player.getCurrentPosition();
                            handler.sendMessage(msg);
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        if(player!=null){
            player.start();
        }
    }

    public void pause(View view){
        if(player != null){
            player.pause();
        }
    }

    public void stop(View view){
        stopPlayer();
    }

    private void stopPlayer(){
        if(player != null){
            player.stop();
            player.release();
            player = null;
            Toast.makeText(this, "Zastopowano player", Toast.LENGTH_SHORT).show();
        }
    }

    public String timeLabel(int duration){
        String timeLabel = "";
        int min = duration / 1000 / 60;
        int sec = duration / 1000 % 60;
        timeLabel+= (min+":");
        if(sec<10) timeLabel +="0";
        timeLabel +=sec;

        return timeLabel;
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int current_position = msg.what;
            musicBar.setProgress(current_position);
            String cTime = timeLabel(current_position);
            startTime.setText(cTime);
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        stopPlayer();
    }
}