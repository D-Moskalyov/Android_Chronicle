package com.chronicle.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.widget.TextView;
import kankan.wheel.widget.OnWheelChangedListener;
import kankan.wheel.widget.OnWheelScrollListener;
import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.ArrayWheelAdapter;
import kankan.wheel.widget.adapters.NumericWheelAdapter;
import org.apache.http.params.CoreConnectionPNames;

import java.util.Objects;

/**
 * Created by Дмитрий on 06.08.2015.
 */
public class SettingActivity extends Activity {

    private boolean scrolling = false;
    private int indexOfFirstCenturyAC = 20;
    SharedPreferences mySharedPreferences;

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.wheel_view_layout);

        final String centurys[] = new String[] {
                "XX BC", "XIX BC", "XVIII BC", "XVII BC", "XVI BC", "XV BC", "XIV BC", "XIII BC", "XII BC", "XI BC",
                "X BC", "IX BC", "VIII BC", "VII BC", "VI BC", "V BC", "IV BC", "III BC", "II BC", "I BC",
                "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
                "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XIII", "XIX", "XX", "XXI"
        };

        final TextView intervalText = (TextView) findViewById(R.id.intervalText);

        final WheelView centuryStart = (WheelView) findViewById(R.id.century_start);
        centuryStart.setVisibleItems(5);

        final WheelView centuryFinish = (WheelView) findViewById(R.id.century_finish);
        centuryFinish.setVisibleItems(5);

        ArrayWheelAdapter<String> centuryAdapter =
                new ArrayWheelAdapter<String>(this, centurys);
        centuryAdapter.setTextSize(22);

        centuryStart.setViewAdapter(centuryAdapter);
        centuryStart.setCurrentItem(40);
        centuryFinish.setViewAdapter(centuryAdapter);
        centuryFinish.setCurrentItem(40);


        final WheelView yearStart = (WheelView) findViewById(R.id.year_start);
        yearStart.setVisibleItems(5);
        yearStart.setCyclic(true);

        final WheelView yearFinish = (WheelView) findViewById(R.id.year_finish);
        yearFinish.setVisibleItems(5);
        yearFinish.setCyclic(true);

        NumericWheelAdapter yearAdapter =
                new NumericWheelAdapter(this, 0, 99);
        yearAdapter.setTextSize(22);


        yearStart.setViewAdapter(yearAdapter);
        yearStart.setCurrentItem(40);
        yearFinish.setViewAdapter(yearAdapter);
        yearFinish.setCurrentItem(40);

        centuryStart.addScrollingListener(new OnWheelScrollListener() {
            @Override
            public void onScrollingStarted(WheelView wheel) {
                scrolling = true;
            }

            @Override
            public void onScrollingFinished(WheelView wheel) {
                Correction(centuryStart, centuryFinish, yearStart, yearFinish);
                scrolling = false;

                SharedPreferences.Editor editor = mySharedPreferences.edit();
                editor.putInt("centStartIndx", centuryStart.getCurrentItem());
                editor.apply();
                //установит строку
                intervalText.setText(MakeIntervalString(centuryStart, centuryFinish, yearStart, yearFinish));
            }
        });
        centuryFinish.addScrollingListener(new OnWheelScrollListener() {
            @Override
            public void onScrollingStarted(WheelView wheel) {
                scrolling = true;
            }

            @Override
            public void onScrollingFinished(WheelView wheel) {
                Correction(centuryStart, centuryFinish, yearStart, yearFinish);
                scrolling = false;

                SharedPreferences.Editor editor = mySharedPreferences.edit();
                editor.putInt("centFinishIndx", centuryFinish.getCurrentItem());
                editor.apply();

                //установит строку
                intervalText.setText(MakeIntervalString(centuryStart, centuryFinish, yearStart, yearFinish));
            }
        });
        centuryStart.addChangingListener(new OnWheelChangedListener() {
            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                Correction(centuryStart, centuryFinish, yearStart, yearFinish);
                if (!scrolling) {
                    SharedPreferences.Editor editor = mySharedPreferences.edit();
                    editor.putInt("centStartIndx", centuryStart.getCurrentItem());
                    editor.apply();
                    //установить строку
                    intervalText.setText(MakeIntervalString(centuryStart, centuryFinish, yearStart, yearFinish));
                }
            }
        });
        centuryFinish.addChangingListener(new OnWheelChangedListener() {
            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                Correction(centuryStart, centuryFinish, yearStart, yearFinish);
                if (!scrolling) {
                    SharedPreferences.Editor editor = mySharedPreferences.edit();
                    editor.putInt("centFinishIndx", centuryFinish.getCurrentItem());
                    editor.apply();
                    //установить строку
                    intervalText.setText(MakeIntervalString(centuryStart, centuryFinish, yearStart, yearFinish));
                }
            }
        });



        yearStart.addScrollingListener(new OnWheelScrollListener() {
            @Override
            public void onScrollingStarted(WheelView wheel) {
                scrolling = true;
            }

            @Override
            public void onScrollingFinished(WheelView wheel) {
                Correction(centuryStart, centuryFinish, yearStart, yearFinish);
                scrolling = false;

                SharedPreferences.Editor editor = mySharedPreferences.edit();
                editor.putInt("yearStartIndx", yearStart.getCurrentItem());
                editor.apply();
                //установит строку
                intervalText.setText(MakeIntervalString(centuryStart, centuryFinish, yearStart, yearFinish));
            }
        });
        yearFinish.addScrollingListener(new OnWheelScrollListener() {
            @Override
            public void onScrollingStarted(WheelView wheel) {
                scrolling = true;
            }

            @Override
            public void onScrollingFinished(WheelView wheel) {
                Correction(centuryStart, centuryFinish, yearStart, yearFinish);
                scrolling = false;

                SharedPreferences.Editor editor = mySharedPreferences.edit();
                editor.putInt("yearFinishIndx", yearFinish.getCurrentItem());
                editor.apply();
                //установит строку
                intervalText.setText(MakeIntervalString(centuryStart, centuryFinish, yearStart, yearFinish));
            }
        });
        yearStart.addChangingListener(new OnWheelChangedListener() {
            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                Correction(centuryStart, centuryFinish, yearStart, yearFinish);
                if (!scrolling) {
                    SharedPreferences.Editor editor = mySharedPreferences.edit();
                    editor.putInt("yearStartIndx", yearStart.getCurrentItem());
                    editor.apply();

                    //установить строку
                    intervalText.setText(MakeIntervalString(centuryStart, centuryFinish, yearStart, yearFinish));
                }
            }
        });
        yearFinish.addChangingListener(new OnWheelChangedListener() {
            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                Correction(centuryStart, centuryFinish, yearStart, yearFinish);
                if (!scrolling) {
                    SharedPreferences.Editor editor = mySharedPreferences.edit();
                    editor.putInt("yearFinishIndx", yearFinish.getCurrentItem());
                    editor.apply();
                    //установить строку
                    intervalText.setText(MakeIntervalString(centuryStart, centuryFinish, yearStart, yearFinish));
                }
            }
        });

        mySharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Activity.MODE_PRIVATE);

        scrolling = true;
        centuryStart.setCurrentItem(mySharedPreferences.getInt("centStartIndx", 0));
        centuryFinish.setCurrentItem(mySharedPreferences.getInt("centFinishIndx", 0));
        yearStart.setCurrentItem(mySharedPreferences.getInt("yearStartIndx", 0));
        yearFinish.setCurrentItem(mySharedPreferences.getInt("yearFinishIndx", 0));
        scrolling = false;
        intervalText.setText(MakeIntervalString(centuryStart, centuryFinish, yearStart, yearFinish));


    }

    private void Correction(WheelView cS, WheelView cF, WheelView yS, WheelView yF){
        if(cS.getCurrentItem() < indexOfFirstCenturyAC) {
            if(cF.getCurrentItem() < indexOfFirstCenturyAC) {
                if(cS.getCurrentItem() > cF.getCurrentItem()) {
                    cF.stopScrolling();
                    cF.setCurrentItem(cS.getCurrentItem());
                    if(yS.getCurrentItem() < yF.getCurrentItem()){
                        yF.stopScrolling();
                        yF.setCurrentItem(yS.getCurrentItem());
                    }
                }
                else if(cS.getCurrentItem() == cF.getCurrentItem()) {
                    if(yS.getCurrentItem() < yF.getCurrentItem()){
                        yF.stopScrolling();
                        yF.setCurrentItem(yS.getCurrentItem());
                    }
                }
            }
        }
        else if(cF.getCurrentItem() < indexOfFirstCenturyAC) {
            cF.stopScrolling();
            cF.setCurrentItem(cS.getCurrentItem());
            if(yS.getCurrentItem() > yF.getCurrentItem()){
                yF.stopScrolling();
                yF.setCurrentItem(yS.getCurrentItem());
            }
        }
        else if(cS.getCurrentItem() > cF.getCurrentItem()) {
            cF.stopScrolling();
            cF.setCurrentItem(cS.getCurrentItem());
            if(yS.getCurrentItem() > yF.getCurrentItem()){
                yF.stopScrolling();
                yF.setCurrentItem(yS.getCurrentItem());
            }
        }
        else if(cS.getCurrentItem() == cF.getCurrentItem()){
            if(yS.getCurrentItem() > yF.getCurrentItem()){
                yF.stopScrolling();
                yF.setCurrentItem(yS.getCurrentItem());
            }
        }
    }

    private String MakeIntervalString(WheelView cS, WheelView cF, WheelView yS, WheelView yF){

        String interval = "";

        int cs = cS.getCurrentItem();
        int cf = cF.getCurrentItem();
        int ys = yS.getCurrentItem();
        int yf = yF.getCurrentItem();

        if(cs >= indexOfFirstCenturyAC){
            interval += (cs - indexOfFirstCenturyAC) * 100 + ys;
            interval += " - ";
        }
        else {
            interval += (indexOfFirstCenturyAC - cs - 1) * 100 + ys;
            interval += " BC - ";
        }

        if(cf >= indexOfFirstCenturyAC){
            interval += (cf - indexOfFirstCenturyAC) * 100 + yf;
        }
        else {
            interval += (indexOfFirstCenturyAC - cf - 1) * 100 + yf;
            interval += " BC";
        }
        return interval;
    }
}
