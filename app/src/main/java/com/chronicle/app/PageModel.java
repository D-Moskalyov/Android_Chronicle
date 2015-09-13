package com.chronicle.app;

import android.graphics.pdf.PdfDocument;

import java.util.Date;

/**
 * Created by ִלטענטי on 19.08.2015.
 */
public class PageModel {
    int year;
    long revID;
    String lastUpdate;

    public PageModel(int y, long r, String u){
        year = y;
        revID = r;
        lastUpdate = u;
    }

}
