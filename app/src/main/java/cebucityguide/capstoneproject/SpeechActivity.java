package cebucityguide.capstoneproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import cebucityguide.capstoneproject.classes.SpeechService;
import cebucityguide.capstoneproject.classes.SpeechTranslatorProcess;
import cebucityguide.capstoneproject.classes.VoiceRecorder;
import cebucityguide.capstoneproject.fragments.MessageDialogFragment;

import static cebucityguide.capstoneproject.classes.SpeechHelper.FRAGMENT_MESSAGE_DIALOG;
import static cebucityguide.capstoneproject.classes.SpeechHelper.REQUEST_RECORD_AUDIO_PERMISSION;
import static cebucityguide.capstoneproject.classes.SpeechHelper.mSpeechService;
import static cebucityguide.capstoneproject.classes.SpeechHelper.mVoiceRecorder;

public class SpeechActivity extends HomeActivity
        implements TextToSpeech.OnInitListener, AdapterView.OnItemSelectedListener {

    public static TextToSpeech textToSpeech;
    @SuppressLint("StaticFieldLeak")
    public static String selectedLanguage = "";
    @SuppressLint("StaticFieldLeak")
    public static TextView SpeechOutput;
    @SuppressLint("StaticFieldLeak")
    public static TextView mText;
    private final SpeechService.Listener mSpeechServiceListener =
            new SpeechService.Listener() {
                @Override
                public void onSpeechRecognized(final String text, final boolean isFinal) {
                    if (isFinal) {
                        mVoiceRecorder.dismiss();
                    }
                    if (mText != null && !TextUtils.isEmpty(text)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (isFinal) {
                                    mText.setText(text);
                                } else {
                                    mText.setText(text);
                                    SpeechOutput.setText(mText.getText().toString());
                                }
                            }
                        });
                    }
                }
            };
    private ImageButton speakButton;
    private ImageButton translateButton;
    private ImageButton textTranslatorSwitcher;
    // Resource caches
    private int mColorHearing;
    private int mColorNotHearing;

    /*


    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /////TRANSLATOR, TEXT TO SPEECH BOUNDARY AND SPEECH TO TEXT MODULES////////
    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////


    */
    // View references
    private TextView mStatus;
    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            showStatus(true);
            if (mSpeechService != null) {
                mSpeechService.startRecognizing(mVoiceRecorder.getSampleRate());
            }
        }

        @Override
        public void onVoice(byte[] data, int size) {
            if (mSpeechService != null) {
                mSpeechService.recognize(data, size);
            }
        }

        @Override
        public void onVoiceEnd() {
            showStatus(false);
            if (mSpeechService != null) {
                mSpeechService.finishRecognizing();
            }
        }

    };
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mSpeechService = SpeechService.from(binder);
            mSpeechService.addListener(mSpeechServiceListener);
            mStatus.setVisibility(View.VISIBLE);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSpeechService = null;
        }

    };
    private Spinner spinner;

    @Override
    protected void onDestroy() {
        if(textToSpeech != null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onInit(final int status) {
        if(status == TextToSpeech.SUCCESS){
            int result = textToSpeech.setLanguage(Locale.getDefault());
            if(result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED){
                Log.e("TextToSpeech", "This Language is not Supported");
                speakButton.setEnabled(false);
            } else {
                speakButton.setEnabled(true);
            }
        }
    }

    private void speakOut(){
        String text = SpeechOutput.getText().toString();
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLayoutInflater().inflate(R.layout.capstone_speech_activity, frameLayout);
        setTitle("Speech Translator");

        textToSpeech = new TextToSpeech(this, this);
        SpeechOutput = findViewById(R.id.STTOutput);

        speakButton = findViewById(R.id.speakButton);
        speakButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speakOut();
            }
        });

        translateButton = findViewById(R.id.translateButton);
        translateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SpeechTranslatorProcess().execute();
            }
        });

        textTranslatorSwitcher = findViewById(R.id.translateTextSwitcher);
        textTranslatorSwitcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SpeechActivity_Text.class);
                startActivity(intent);
            }
        });

        spinner = findViewById(R.id.selectLanguage);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.languages, android.R.layout.simple_spinner_dropdown_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
        /*


        ////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////
        /////TEXT TO SPEECH BOUNDARY AND SPEECH TO TEXT MODULES/////
        ////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////


        */

        final Resources resources = getResources();
        final Resources.Theme theme = getTheme();
        mColorHearing = ResourcesCompat.getColor(resources, R.color.status_hearing, theme);
        mColorNotHearing = ResourcesCompat.getColor(resources, R.color.status_not_hearing, theme);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mStatus = findViewById(R.id.statusView);
        mText = findViewById(R.id.speechText);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Prepare Cloud Speech API
        bindService(new Intent(this, SpeechService.class), mServiceConnection, BIND_AUTO_CREATE);

        // Start listening to voices
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecorder();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)) {
            showPermissionMessageDialog();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    @Override
    protected void onStop() {
        // Stop listening to voice
        stopVoiceRecorder();

        // Stop Cloud Speech API
        mSpeechService.removeListener(mSpeechServiceListener);
        unbindService(mServiceConnection);
        mSpeechService = null;

        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (permissions.length == 1 && grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecorder();
            } else {
                showPermissionMessageDialog();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.speech_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_file:
                mSpeechService.recognizeInputStream(getResources().openRawResource(R.raw.audio));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
        }
        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
        mVoiceRecorder.start();
    }

    private void stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
        }
    }

    private void showPermissionMessageDialog() {
        MessageDialogFragment
                .newInstance(getString(R.string.permission_message))
                .show(getSupportFragmentManager(), FRAGMENT_MESSAGE_DIALOG);
    }

    private void showStatus(final boolean hearingVoice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatus.setTextColor(hearingVoice ? mColorHearing : mColorNotHearing);
            }
        });
    }

    /*


    ////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////
    /////SPEECH TO TEXT MODULES to TRANSLATOR MODULE////////////
    ////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////


    */

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position == 0) {
            selectedLanguage = "en";
            Toast.makeText(this,"The selected Language is english", Toast.LENGTH_SHORT).show();
        } else if (position == 1) {
            selectedLanguage = "ceb";
            Toast.makeText(this,"The selected Language is cebuano", Toast.LENGTH_SHORT).show();
        } else if (position == 2) {
            selectedLanguage = "tl";
            Toast.makeText(this,"The selected Language is tagalog", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
