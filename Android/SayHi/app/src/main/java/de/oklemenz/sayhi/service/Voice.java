package de.oklemenz.sayhi.service;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Toast;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.oklemenz.sayhi.AppDelegate;
import de.oklemenz.sayhi.R;
import de.oklemenz.sayhi.base.BaseActivity;
import de.oklemenz.sayhi.model.Enum;

import static android.media.MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED;
import static android.speech.tts.TextToSpeech.Engine.KEY_PARAM_VOLUME;

/**
 * Created by Oliver Klemenz on 22.02.17.
 */

public class Voice implements TextToSpeech.OnInitListener {

    public interface Callback {
        void completion(byte[] voiceData);
    }

    private static class VoiceSettings {
        float rate = 1.0f;
        float pitch = 1.0f;
        float volume = 1.0f;

        VoiceSettings(float rate, float pitch, float volume) {
            this.rate = rate;
            this.pitch = pitch;
            this.volume = volume;
        }
    }

    private static Voice instance = new Voice();

    public static Voice getInstance() {
        return instance;
    }

    private static Map<Enum.Gender, VoiceSettings> voices = new HashMap<>();

    static {
        voices.put(Enum.Gender.None, new VoiceSettings(1.5f, 1.0f, 2.0f));
        voices.put(Enum.Gender.Male, new VoiceSettings(1.5f, 0.5f, 2.0f));
        voices.put(Enum.Gender.Female, new VoiceSettings(1.5f, 1.5f, 2.0f));
    }

    private String text;
    private Enum.Gender gender;
    private TextToSpeech textToSpeech;

    private String message;
    private Callback recordCallback;

    private MediaRecorder audioRecorder;
    private MediaPlayer audioPlayer;

    private AlertDialog recordAlert;

    private String voiceFilename;

    public void speak(Context context, String text, Enum.Gender gender) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audio.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            return;
        }
        clearSpeak();
        this.text = text;
        this.gender = gender;
        this.textToSpeech = new TextToSpeech(context, this);
    }

    public void sayHi(Context context, String name, Enum.Gender gender, boolean toast) {
        String text = context.getString(R.string.HiPlain);
        if (!TextUtils.isEmpty(name)) {
            text = String.format(context.getString(R.string.HiIm), name);
        }
        Voice.getInstance().speak(context, text, gender);
        if (toast) {
            text = text + "\n\n" + context.getString(R.string.DeviceNotMuted);
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        }
    }

    public void clearSpeak() {
        this.text = null;
        this.gender = null;
        if (this.textToSpeech != null) {
            this.textToSpeech.shutdown();
            this.textToSpeech = null;
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            Locale locale = Locale.getDefault();
            if (!AppDelegate.BundleLangCodes.contains(Locale.getDefault().getLanguage())) {
                locale = Locale.US;
            }
            int result = textToSpeech.setLanguage(locale);
            VoiceSettings voice = voices.get(gender);
            if (voice != null) {
                textToSpeech.setSpeechRate(voice.rate);
                textToSpeech.setPitch(voice.pitch);
            }
            if (!(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Bundle params = new Bundle();
                    params.putFloat(KEY_PARAM_VOLUME, voice.volume);
                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params, null);
                } else {
                    HashMap<String, String> params = new HashMap<>();
                    params.put(KEY_PARAM_VOLUME, "" + voice.volume);
                    textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params);
                }
            }
        }
    }

    public boolean replay(Context context, byte[] voiceData) {
        AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audio.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
            return false;
        }
        String url = "data:audio/m4a;base64," + Base64.encodeToString(voiceData, Base64.NO_WRAP);
        try {
            if (audioPlayer == null) {
                audioPlayer = new MediaPlayer();
            }
            audioPlayer.reset();
            audioPlayer.setDataSource(url);
            audioPlayer.prepare();
            audioPlayer.setVolume(1.0f, 1.0f);
            audioPlayer.start();
            Toast.makeText(context, context.getString(R.string.PlayingRecording) + "\n\n" +
                    context.getString(R.string.DeviceNotMuted), Toast.LENGTH_SHORT).show();
            return true;
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void record(final BaseActivity context, String message, Callback callback) {
        this.message = message;
        this.recordCallback = callback;
        context.askForPermission(Manifest.permission.RECORD_AUDIO, BaseActivity.PERMISSION_RECORD_AUDIO, new BaseActivity.PermissionDelegate() {
            @Override
            public void permissionResult(String permission, int requestCode, boolean granted) {
                if (requestCode == BaseActivity.PERMISSION_RECORD_AUDIO) {
                    if (granted) {
                        alertRecord(context);
                    } else {
                        alertMicrophoneError(context);
                    }
                }
            }
        });
    }

    public void alertRecord(final BaseActivity context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.Say)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.Done, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finishRecording(context, true);
                    }
                });
        recordAlert = builder.create();
        recordAlert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                if (recordAlert != null) {
                    recordAlert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                }
            }
        });
        if (startRecording(context)) {
            recordAlert.show();
        }
    }

    public boolean startRecording(final BaseActivity context) {
        try {
            voiceFilename = context.getCacheDir().getAbsolutePath() + "/voice.m4a";

            if (audioRecorder == null) {
                audioRecorder = new MediaRecorder();
            }
            audioRecorder.reset();
            audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            audioRecorder.setOutputFile(voiceFilename);
            audioRecorder.setMaxDuration(3000); // 3 Seconds
            audioRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                @Override
                public void onInfo(MediaRecorder mediaRecorder, int what, int extra) {
                    if (what == MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        finishRecording(context, true);
                    }
                }
            });
            audioRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
                @Override
                public void onError(MediaRecorder mediaRecorder, int what, int extra) {
                    finishRecording(context, false);
                }
            });
            audioRecorder.prepare();
            audioRecorder.start();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            finishRecording(context, false);
            alertMicrophoneError(context);
        }
        return false;
    }

    private void finishRecording(Context context, boolean success) {
        if (audioRecorder != null) {
            audioRecorder.reset();
        }

        if (recordAlert != null) {
            recordAlert.dismiss();
        }
        recordAlert = null;

        byte[] voiceData = null;
        if (success) {
            voiceData = Utilities.readFile(context, voiceFilename);
        }
        Utilities.deleteFile(context, voiceFilename);

        if (recordCallback != null) {
            recordCallback.completion(voiceData);
            if (voiceData != null && voiceData.length > 0) {
                replay(context, voiceData);
            }
        }
        recordCallback = null;
    }

    private void alertMicrophoneError(final BaseActivity context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.VoiceRecording)
                .setMessage(context.getString(R.string.MicrophoneError) + "\n\n" + context.getString(R.string.MicrophoneUsageDescription))
                .setCancelable(false)
                .setPositiveButton(context.getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNeutralButton(context.getString(R.string.Settings), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Utilities.openAppSettings(context);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                alert.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(AppDelegate.AccentColor);
                alert.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(AppDelegate.AccentColor);
            }
        });
        alert.show();
    }
}
