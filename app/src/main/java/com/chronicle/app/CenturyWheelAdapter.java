package com.chronicle.app;

import android.content.Context;
import kankan.wheel.widget.adapters.ArrayWheelAdapter;

/**
 * Created by ִלטענטי on 06.08.2015.
 */
public class CenturyWheelAdapter extends ArrayWheelAdapter {

    //Object[] centurys;

    protected CenturyWheelAdapter(Context context, Object[] items) {
        //Pass the context and the custom layout for the text to the super method

        super(context, items);
        //this.centurys = items;
    }

    @Override
    public int getItemsCount() {
        return super.getItemsCount();
    }

    @Override
    public CharSequence getItemText(int index) {
        return super.getItemText(index);
    }
}
