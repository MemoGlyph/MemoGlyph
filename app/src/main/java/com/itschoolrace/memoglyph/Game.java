package com.itschoolrace.memoglyph;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by StephaneTruong on 26/11/2014.
 */
public class Game extends Activity implements Level, Stage {

    private Animation animScale;
    private Long duration;

    private TypedArray images;
    private TypedArray buttons;
    private TypedArray texts;

    private Button currentBtn;
    private Button[] allBtns;
    private int rightButton;

    private TimerTask task;
    private Timer timer;

    private List<Integer> buttonPressed;

    private int currentStage;
    private int[] currentStageMap;
    private int currentStageIndex;
    private int currentLevel;
    private String difficulty;

    private int period;

    //0 = player, 1 = IA
    private int turn;

    private int count;

    private Button mainButton;
    private TextView levelAndStage;
    private Button tryAgainButton;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);

        //scale animation and update buttons text
        animScale = AnimationUtils.loadAnimation(this, R.anim.anim_scale);
        duration = animScale.getDuration() * 3;

        //set the handler
        handler = new Handler();

        //loading buttons resources
        images = getResources().obtainTypedArray(R.array.loading_images);
        buttons = getResources().obtainTypedArray(R.array.loading_buttons);

        //button array that contains all the buttons
        allBtns = new Button[buttons.length()];

        //list of buttons pressed by the user
        buttonPressed = new ArrayList<Integer>();

        //set the current stage, level and the number of play per stage
        currentStage = STAGE1;
        currentLevel = EASY;

        currentStageIndex = 0;
        currentStageMap = STAGE1_MAP;

        texts = null;

        //count how many times the user has succeeded to touch the right button at the right order.
        rightButton = 0;

        //count how many times the IA simulate a touch event.
        count = 1;

        //IA turn
        turn = 1;

        //timer for simulating touch events
        initTimer();

        //start the game when the button is pressed
        mainButton = (Button) findViewById(R.id.mainButton);
        mainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mainButton.isEnabled()) {
                    tryAgainButton.setEnabled(true);
                    mainButton.setVisibility(View.INVISIBLE);
                    startSimulating(500);
                }
            }
        });

        levelAndStage = (TextView) findViewById(R.id.levelAndStage);

        //restart the IA;
        tryAgainButton = (Button) findViewById(R.id.tryAgainButton);
        tryAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSimulating(500);
            }
        });
        tryAgainButton.setEnabled(false);

        //loading level, stage and resources.
        Load();
        updateUI();
    }

    private void clickable(boolean clickable) {
        for (int i = 0; i < buttons.length(); i++) {
            allBtns[i].setEnabled(clickable);
        }
    }

    private void initTimer() {
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                if (count > currentStageMap[currentStageIndex]) {
                    turn = 0;
                    count = 0;
                    timer.cancel();
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //dispatch a motionEvent on a random created button

                            final int choice = (int) (Math.random() * buttons.length());
                            allBtns[choice].setEnabled(true);
                            try {
                                allBtns[choice].dispatchTouchEvent(mEvent());
                                buttonPressed.add(allBtns[choice].getId());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
                count++;
            }
        };
    }

    private View.OnTouchListener listener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            view.startAnimation(animScale);
            currentBtn = (Button) findViewById(view.getId());
            currentBtn.setTextColor(Color.WHITE);

            if (motionEvent.getAction() == MotionEvent.ACTION_UP && turn == 0) {
                if (buttonPressed.size() != 0){
                    check();
                }
            }
            return true;
        }
    };

    private void check() {
        if (currentBtn.getId() == buttonPressed.get(rightButton)) {
            rightButton++;
            if (rightButton == currentStageMap[currentStageIndex] && currentStageIndex == NBPLAY) {
                message(3);
                currentStage += 1;
                if (currentStage > NBSTAGE) {
                    message(0);
                } else {
                    currentLevel = EASY;
                    currentStageIndex = 0;
                    Load();
                    startSimulating(2000);
                }
            } else if (rightButton == currentStageMap[currentStageIndex]) {
                message(1);
                currentStageIndex += 1;
                currentLevel += 1;
                config();
                startSimulating(2000);
            }
        } else {
            if (turn == 0) {
                rightButton = 0;
                message(2);
            }
        }
    }

    private void startSimulating(int duration) {
        turn = 1;
        rightButton = 0;
        updateUI();
        clickable(false);
        buttonPressed.removeAll(buttonPressed);
        tryAgainButton.setEnabled(false);
        initTimer();
        timer.scheduleAtFixedRate(task, duration, period);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                clickable(true);
                tryAgainButton.setEnabled(true);
            }
        }, duration*3 + period);
    }

    private void updateUI() {
        levelAndStage.setText("Niveau " + currentStage + " : " + difficulty);
    }

    private void message(int i) {
        mainButton.setEnabled(false);
        if (i == 1) {
            mainButton.setText("Bien joué !");
        } else if (i == 2) {
            mainButton.setText("Raté !");
        } else if (i == 3) {
            mainButton.setText("Vous passez au niveau suivant !");
        }
        if (currentStage > NBSTAGE) {
            mainButton.setText("Félicitations, tu as terminé le jeu !");
            duration = 200000L;
        }
        mainButton.setEnabled(false);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentStage > NBSTAGE) {
                    Intent intent = new Intent(Game.this, Main.class);
                    startActivity(intent);
                }
                mainButton.setVisibility(View.INVISIBLE);
            }
        }, duration);
        mainButton.setVisibility(View.VISIBLE);
    }

    private MotionEvent mEvent() throws InterruptedException {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 100;
        float x = 0.0f;
        float y = 0.0f;
        int metaState = 0;

        return MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_UP,
                x,
                y,
                metaState
        );
    }

    private void Load() {
        config();
        for (int i = 0; i < buttons.length(); i++) {
            allBtns[i] = (Button) findViewById(buttons.getResourceId(i, R.id.button));
            int choice = (int) (Math.random() * images.length());
            allBtns[i].setBackgroundResource(images.getResourceId(choice, R.drawable.btn_aubay_orange));
            allBtns[i].setText(texts.getText(i));
            allBtns[i].setTextColor(Color.TRANSPARENT);
            allBtns[i].setOnTouchListener(listener);
        }
    }

    private void config() {
        switch (currentLevel) {
            case EASY:
                difficulty = "Facile";
                period = 1600;
                break;
            case MEDIUM:
                difficulty = "Intermédiaire";
                period = 1300;
                break;
            case HARD:
                difficulty = "Difficile";
                period = 1000;
                break;
        }
        switch (currentStage) {
            case STAGE1:
                currentStageMap = STAGE1_MAP;
                texts = getResources().obtainTypedArray(R.array.loading_stage1);
                break;
            case STAGE2:
                currentStageMap = STAGE2_MAP;
                texts = getResources().obtainTypedArray(R.array.loading_stage2);
                break;
            case STAGE3:
                currentStageMap = STAGE3_MAP;
                texts = getResources().obtainTypedArray(R.array.loading_stage3);
                break;
        }
    }
}

