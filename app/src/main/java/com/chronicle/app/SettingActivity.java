package com.chronicle.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.widget.TextView;
import kankan.wheel.widget.OnWheelChangedListener;
import kankan.wheel.widget.OnWheelScrollListener;
import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.adapters.ArrayWheelAdapter;
import kankan.wheel.widget.adapters.NumericWheelAdapter;

import java.util.Objects;

/**
 * Created by Дмитрий on 06.08.2015.
 */
public class SettingActivity extends Activity {

    private boolean scrolling = false;

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
                "XI", "XII", "XII", "XIV", "XV", "XVI", "XVII", "XIII", "XIX", "XX", "XXI"
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

        final WheelView yearFinish = (WheelView) findViewById(R.id.year_finish);
        yearFinish.setVisibleItems(5);

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
                scrolling = false;
                //установит строку
            }
        });
        centuryFinish.addScrollingListener(new OnWheelScrollListener() {
            @Override
            public void onScrollingStarted(WheelView wheel) {
                scrolling = true;
            }

            @Override
            public void onScrollingFinished(WheelView wheel) {
                scrolling = false;
                //установит строку
            }
        });
        centuryStart.addChangingListener(new OnWheelChangedListener() {
            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                if (centuryStart.getCurrentItem() > centuryFinish.getCurrentItem()) {
                    centuryFinish.setCurrentItem(centuryStart.getCurrentItem());
                }
                if (!scrolling) {
                    //установить строку
                }
            }
        });
        centuryFinish.addChangingListener(new OnWheelChangedListener() {
            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                if (centuryStart.getCurrentItem() > centuryFinish.getCurrentItem()) {
                    centuryStart.setCurrentItem(centuryFinish.getCurrentItem());
                }
                if (!scrolling) {
                    //установить строку
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
                scrolling = false;
                //установит строку
            }
        });
        yearFinish.addScrollingListener(new OnWheelScrollListener() {
            @Override
            public void onScrollingStarted(WheelView wheel) {
                scrolling = true;
            }

            @Override
            public void onScrollingFinished(WheelView wheel) {
                scrolling = false;
                //установит строку
            }
        });
        yearStart.addChangingListener(new OnWheelChangedListener() {
            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                if (!scrolling) {
                    //установить строку
                }
            }
        });
        yearFinish.addChangingListener(new OnWheelChangedListener() {
            @Override
            public void onChanged(WheelView wheel, int oldValue, int newValue) {
                if (!scrolling) {
                    //установить строку
                }
            }
        });




    }
}
