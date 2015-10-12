package com.chronicle.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.maps.model.LatLng;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.WrongCharaterException;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import wikipedia.Wiki;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InitAsync extends AsyncTask<Void, Void, Void> {

    Wiki wikipedia;

    List<String> wordBaseForms;
    char noun = '—';
    SharedPreferences settings;

    ArrayList<Integer> listForFirstInit;
    ArrayList<Integer> listForUpdate;
    ArrayList<Integer> listForNotUpdate;

    List<PageModel> pagesForIsertDB;
    List<PageModel> pagesForUdateDB;
    List<PageModel> pagesForDeleteDB;
    List<EventModel> eventsForDeleteDB;

    SimpleDateFormat dateFormat;

    //DBHelper dbHelper;
    //SQLiteDatabase db;

    MainActivity activity;

    boolean isCrached;
    Logger logger = Logger.getLogger("chron");

    void link(MainActivity act){
        activity = act;
    }
    void unLink(){
        activity = null;
    }

    @Override
    protected Void doInBackground(Void... params) {
        settings = activity.getSharedPreferences(activity.getString(R.string.preference_file_key), 0);
        boolean isOfflineOnly = settings.getBoolean("isOfflineOnly", true);

        if(isOfflineOnly)
            return null;

        //activity.dbHelper = new DBHelper(activity.context);
        wikipedia = new Wiki();

        isCrached = false;

        pagesForIsertDB = new ArrayList<PageModel>();
        pagesForUdateDB = new ArrayList<PageModel>();
        pagesForDeleteDB = new ArrayList<PageModel>();
        eventsForDeleteDB = new ArrayList<EventModel>();

        listForFirstInit = new ArrayList<Integer>();
        listForUpdate = new ArrayList<Integer>();
        listForNotUpdate = new ArrayList<Integer>();


        ArrayMap<Integer, String> allEvntsByYear = new ArrayMap<Integer, String>();
        ArrayList<EventModel> eventsToParseLex = new ArrayList<EventModel>();
        List<EventWithLex> eventWithLexList = new ArrayList<EventWithLex>();
        List<EventWithLex> eventWithLexes = new ArrayList<EventWithLex>();
        SortedSet<EventModel> events = new TreeSet<EventModel>();

        GetPagesForUpdateDeleteCreate();

        if (listForFirstInit.size() != 0 || listForUpdate.size() != 0 & !isCrached)
            allEvntsByYear = GetPageForParse(listForFirstInit, listForUpdate);
        if (allEvntsByYear != null & allEvntsByYear.size() != 0 & !isCrached)
            eventsToParseLex = ParseEvent(allEvntsByYear);
        if (eventsToParseLex != null & eventsToParseLex.size() != 0 & !isCrached)
            eventWithLexList = ParseLexFromEvents(eventsToParseLex);
        if (eventWithLexList != null & eventWithLexList.size() != 0 & !isCrached)
            eventWithLexes = GetRedirectForLexemes(eventWithLexList);
        if (eventWithLexes != null & eventWithLexes.size() != 0 & !isCrached)
            events = GetCoordForEvent(eventWithLexes);
        if (!isCrached & !isCancelled())
            WriteDB(events);

        log(Level.INFO, "InitAsync", "InitAsync -> doInBackground success");
        return null;
    }

    @Override
    protected void onPostExecute(Void o) {
        super.onPostExecute(o);

        if(isCrached)
            Toast.makeText(activity.context, "Data is not updated. Check the connection", Toast.LENGTH_LONG);

        activity.mainButton.setText(activity.textBtn);
        activity.MakeMarkers();
        activity.mClusterManager.cluster();

        log(Level.INFO, "InitAsync", "InitAsync -> onPostExecute success");
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        log(Level.INFO, "onCancelled", "onCancelled success");
        activity.mainButton.setText("STOPPED");
        activity.MakeMarkers();
        activity.mClusterManager.cluster();

        activity.RestartAsync();

        log(Level.INFO, "InitAsync", "InitAsync -> onCancelled success");
    }



    private void GetPagesForUpdateDeleteCreate(){
        if(isCancelled())
            return;

        activity.db = activity.dbHelper.getWritableDatabase();
        String glYearStart = Integer.toString(activity.globalYearStart);
        String glYearFinish = Integer.toString(activity.globalYearFinish);
        Cursor cursor = activity.db.query("Pages", new String[]{"year", "lastUpdate"}, "year >= ? and year <= ?",
                new String[]{glYearStart, glYearFinish}, null, null, null);

        if(cursor == null || cursor.getCount() == 0){
            for(int _year = activity.globalYearStart; _year <= activity.globalYearFinish; _year++) {
                listForFirstInit.add(_year);
            }
        }

        else {
            cursor.moveToFirst();
            do {

                int yearFromDB = cursor.getInt(cursor.getColumnIndex("year"));
                String str = cursor.getString(cursor.getColumnIndex("lastUpdate"));

                //Log.d(LOG_TAG, str);
                try {
                    Calendar thatDay = Calendar.getInstance(TimeZone.getTimeZone("Europe/Kiev"));
                    dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    //dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Kiev"));
                    Date date = dateFormat.parse(str);
                    thatDay.setTime(date);

                    Calendar today = Calendar.getInstance();
                    Date todayD = new Date();
                    today.setTime(todayD);

                    long diff = today.getTimeInMillis() - thatDay.getTimeInMillis(); //result in millis

                    if (diff / (60 * 1000) > 1440) {
                        listForUpdate.add(yearFromDB);
                    } else {
                        listForNotUpdate.add(yearFromDB);
                    }

                } catch (ParseException e) {
                    isCrached = true;
                    e.printStackTrace();
                }

            } while (cursor.moveToNext());

            for (int _year = activity.globalYearStart; _year <= activity.globalYearFinish; _year++) {
                if(!(listForUpdate.contains(_year) || listForNotUpdate.contains(_year))){
                    listForFirstInit.add((_year));
                }
            }
        }

        cursor.close();
        activity.dbHelper.close();
        log(Level.INFO, "GetPagesForUpdateDeleteCreate", "GetPagesForUpdateDeleteCreate success");
    }

    private ArrayMap<Integer, String> GetPageForParse(ArrayList<Integer> listForFirstInit, ArrayList<Integer>listForUpdate){
        if(isCancelled())
            return new ArrayMap<Integer, String>();

        try {
            ArrayMap<Integer, String> allEvntsByYear = new ArrayMap<Integer, String>();

            Integer[] masForFirstInit = GetNewPageForInsertIntoDB(listForFirstInit);

            try {
                allEvntsByYear.putAll((Map<? extends Integer, ? extends String>) GetPage(masForFirstInit));
            } catch (TimeoutException e) {
                isCrached = true;
                e.printStackTrace();
            }

            Integer[] masForUpdate;
            if(listForUpdate != null & listForUpdate.size() != 0)
                masForUpdate = listForUpdate.toArray(new Integer[listForUpdate.size()]);
            else
                masForUpdate = new Integer[]{};

            listForUpdate = new ArrayList<Integer>();
            try {
                listForUpdate.addAll(GetPageRevID(masForUpdate));
            } catch (TimeoutException e) {
                isCrached = true;
                e.printStackTrace();
            }

            if (listForUpdate.size() == 0)
                return allEvntsByYear;

            Integer[] listForUpdateMas = listForUpdate.toArray(new Integer[listForUpdate.size()]);

            try {
                allEvntsByYear.putAll((Map<? extends Integer, ? extends String>) GetPage(listForUpdateMas));
            } catch (TimeoutException e) {
                isCrached = true;
                e.printStackTrace();
            }


            return allEvntsByYear;
        }finally {
            log(Level.INFO, "GetPageForParse", "GetPageForParse success");
        }
    }

    private Integer[] GetNewPageForInsertIntoDB(ArrayList<Integer> listForFirstInit){
        if(isCancelled())
            return new Integer[]{};

        if(listForFirstInit != null & listForFirstInit.size() != 0) {
            Integer[] masForFirstInit = listForFirstInit.toArray(new Integer[listForFirstInit.size()]);

            ArrayMap<Integer, Long> pagesWithID = null;
            try {
                pagesWithID = GetNewPageRevID(masForFirstInit);
            } catch (TimeoutException e) {
                isCrached = true;
                e.printStackTrace();
            }

            dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Kiev"));
            Date date = new Date();

            for(int i = 0; i < pagesWithID.size(); i++){
                pagesWithID.keyAt(i);
                pagesForIsertDB.add(new PageModel(pagesWithID.keyAt(i), pagesWithID.valueAt(i), dateFormat.format(date)));
            }
            log(Level.INFO, "GetNewPageForInsertIntoDB", "GetNewPageForInsertIntoDB success");
            return listForFirstInit.toArray(new Integer[listForFirstInit.size()]);
        }
        log(Level.INFO, "GetNewPageForInsertIntoDB", "GetNewPageForInsertIntoDB success");
        return new Integer[]{};
    }

    private ArrayMap<Integer, String> GetPage (Integer [] params) throws TimeoutException{
        if(isCancelled())
            return new ArrayMap<Integer, String>();

        int count = params.length;
        ArrayMap<Integer, String> pages = new ArrayMap<Integer, String>();
        //long totalSize = 0;
        for (int i = 0; i < count; i++) {
            if(isCancelled())
                return new ArrayMap<Integer, String>();
            String year = String.valueOf(params[i]) + "_год";
            if (params[i] < 0) {
                year += "_до_н._э.";
                year = year.substring(1);
            }
            try {
                String text = wikipedia.getPageText(year);
                pages.put(params[i], text);
            } catch (IOException e) {
                //isCrached = true;
                e.printStackTrace();
                continue;
                //return page;
            }
        }

        if (pages == null)
            return new ArrayMap<Integer, String>();

        ArrayMap<Integer, String> allEvntsByYear = new ArrayMap<Integer, String>();


        for (int i = 0; i < pages.size(); i++) {
            if (!pages.valueAt(i).contains("#перенаправление") &
                    !pages.valueAt(i).contains("#REDIRECT") &
                    !pages.valueAt(i).contains("#redirect"))
                allEvntsByYear.put(pages.keyAt(i), pages.valueAt(i));
        }
        log(Level.INFO, "GetPage", "GetPage success");
        return allEvntsByYear;

    }

    private ArrayList<Integer> GetPageRevID(Integer[] params)  throws TimeoutException{
        if(isCancelled())
            return new ArrayList<Integer>();

        int count = params.length;
        long revId = 0;

        int rowCount = count / 50;
        if (params.length % 50 != 0)
            rowCount++;

        ArrayList<String> pages = new ArrayList<String>();
        ArrayMap<Integer, Long> pagesWithID = new ArrayMap<Integer, Long>();

        for (int i = 0; i < rowCount; i++) {
            if(isCancelled())
                return new ArrayList<Integer>();
            for (int j = 0; j < 50 & count > i * 50 + j; j++) {
                String year = String.valueOf(params[i * 50 + j]) + "_год";
                if (params[i * 50 + j] < 0) {
                    year += "_до_н._э.";
                    year = year.substring(1);
                }

                pages.add(year);
            }
            try {
                pagesWithID.putAll((Map<? extends Integer, ? extends Long>) wikipedia.getPagesRevId(pages));
            } catch (IOException e) {
                isCrached = true;
                e.printStackTrace();
            }
            pages.clear();
        }

        if (pagesWithID == null || pagesWithID.size() == 0)
            return new ArrayList<Integer>();

        //long res = Long.parseLong(result[1]);
        activity.db = activity.dbHelper.getWritableDatabase();

        Cursor cursor = activity.db.query("Pages", new String[]{"year, revisionID"}, "year >= ? and year <= ?",
                new String[]{String.valueOf(activity.globalYearStart), String.valueOf(activity.globalYearFinish)}, null, null, null);

        ArrayList<Integer> listForUpdateInner = new ArrayList<Integer>();

        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //dateFormat.setTimeZone(TimeZone.getTimeZone("Europe/Kiev"));
        Date date = new Date();

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    int year = cursor.getInt(cursor.getColumnIndex("year"));
                    long revID = cursor.getInt(cursor.getColumnIndex("revisionID"));

                    if (pagesWithID.get(new Integer(year)) == null) {
                        //pagesForDeleteDB.add(new PageModel(year, revID, dateFormat.format(date)));
                    } else {
                        long revIDFromWiki = pagesWithID.get(new Integer(year));
                        if (revIDFromWiki != revID) {
                            listForUpdateInner.add(new Integer(year));
                        }
                        pagesForUdateDB.add(new PageModel(year, revIDFromWiki, dateFormat.format(date)));
                    }

                } while (cursor.moveToNext());
                //return listForUpdateInner;
            }
            cursor.close();
            //return listForUpdateInner;
        }
        activity.dbHelper.close();
        log(Level.INFO, "GetPageRevID", "GetPageRevID success");
        return listForUpdateInner;

    }

    private ArrayMap<Integer, Long> GetNewPageRevID(Integer [] params) throws TimeoutException {
        if(isCancelled())
            return new ArrayMap<Integer, Long>();

        int count = listForFirstInit.size();
        int rowCount = count / 50;
        if (listForFirstInit.size() % 50 != 0)
            rowCount++;

        ArrayList<String> pages = new ArrayList<String>();
        ArrayMap<Integer, Long> pagesWithID = new ArrayMap<Integer, Long>();

        for (int i = 0; i < rowCount; i++) {
            if(isCancelled())
                return new ArrayMap<Integer, Long>();
            for (int j = 0; j < 50 & count > i * 50 + j; j++) {
                String year = String.valueOf(listForFirstInit.get(i * 50 + j)) + "_год";
                if (listForFirstInit.get(i * 50 + j) < 0) {
                    year += "_до_н._э.";
                    year = year.substring(1);
                }

                pages.add(year);
            }

            try {
                pagesWithID.putAll((Map<? extends Integer, ? extends Long>) wikipedia.getPagesRevId(pages));
            } catch (IOException e) {
                isCrached = true;
                e.printStackTrace();
            }
            pages.clear();
        }
        log(Level.INFO, "GetNewPageRevID", "GetNewPageRevID success");
        return pagesWithID;
    }

    private ArrayList<EventWithLex> ParseLexFromEvents(ArrayList<EventModel> events){
        if(isCancelled())
            return new ArrayList<EventWithLex>();

        ArrayList<EventWithLex> eventWithLexList = new ArrayList<EventWithLex>();

        LuceneMorphology luceneMorph = null;
        try {
            luceneMorph = new RussianLuceneMorphology();
        } catch (IOException e) {
            isCrached = true;
            e.printStackTrace();
        }

        for(EventModel evModel : events){

            int x = 0;
            int y = 0;
            String word;

            ArrayList<String> lexemes = new ArrayList<String>();
            ArrayList<String> rawWords = new ArrayList<String>();
            String text = evModel.text;

            y = text.indexOf(" ", x);

            while (y != -1){
                word = text.substring(x, y);
                word = PunctuationHook(word);

                if(word != null & word != "" & word != " "){
                    rawWords.add(word);
                }

                x = y + 1;
                y = text.indexOf(" ", x);
            }

            y = text.indexOf(".", x);

            if(y != -1){
                word = text.substring(x, y);
                word = PunctuationHook(word);

                if(word != null & word != "" & word != " "){
                    rawWords.add(word);
                }
            }

            for(String rawWord : rawWords){
                String lex = "";

                rawWord = rawWord.toLowerCase();
                try {//
                    try {
                        wordBaseForms = luceneMorph.getMorphInfo(rawWord);
                    } catch (Exception e){wordBaseForms = null;}

                    if(wordBaseForms != null) {
                        int pos = (wordBaseForms.get(0)).indexOf("|") + 3;
                        String partOfSpeach = wordBaseForms.get(0).substring(pos, pos + 1);
                        char pOS = partOfSpeach.charAt(0);

                        if(pOS == noun){
                            lex = wordBaseForms.get(0).substring(0, pos - 3);
                            if(lex.compareTo("иза") != 0)
                                lexemes.add(lex);
                        }
                    }
                }catch (WrongCharaterException e){
                    isCrached = true;
                    e.printStackTrace();
                }


            }

            eventWithLexList.add(new EventWithLex(evModel, lexemes));
        }
        log(Level.INFO, "ParseLexFromEvents", "ParseLexFromEvents success");
        return eventWithLexList;

    }

    private List<EventWithLex> GetRedirectForLexemes(List<EventWithLex> eventWithLexes){
        if(isCancelled())
            return new ArrayList<EventWithLex>();

        ArrayList<String> rawLex = new ArrayList<String>();

        for(EventWithLex eventWithLex : eventWithLexes){
            rawLex.addAll(eventWithLex.lexemes);
        }

        String[] rawLexMas = rawLex.toArray(new String[rawLex.size()]);

        try {
            ArrayList<String> lexForRedirect = GetPageRedirect(rawLexMas);

            for(String lexForRedir : lexForRedirect){

                String lexToRedir = GetAddressPageForRedirect(new String[]{lexForRedir});//!!!

                lexToRedir = lexToRedir.toLowerCase();
                lexForRedir = lexForRedir.toLowerCase();

                if(lexToRedir.compareTo(lexForRedir) != 0) {
                    for (int i = 0; i < eventWithLexes.size(); i++) {

                        int indx = eventWithLexes.get(i).lexemes.indexOf(lexForRedir);
                        while (indx >= 0) {
                            eventWithLexes.get(i).lexemes.set(indx, lexToRedir);
                            indx = eventWithLexes.get(i).lexemes.indexOf(lexForRedir);
                        }

                    }
                }
            }

        } catch (TimeoutException e) {
            isCrached = true;
            e.printStackTrace();
        } finally {
            log(Level.INFO, "GetRedirectForLexemes", "GetRedirectForLexemes success");
            return eventWithLexes;
        }

    }

    private ArrayList<String> GetPageRedirect(String[] params) throws TimeoutException {
        if(isCancelled())
            return new ArrayList<String>();

        ArrayList<String> paramsList = new ArrayList<String>(Arrays.asList(params));
        HashSet<String> paramsSet = new HashSet<String>();
        ArrayList<String> titleWithRedirect = new ArrayList<String>();

        int sizeParamsList = paramsList.size();
        int rowCount = sizeParamsList / 50;
        if (paramsList.size() % 50 != 0)
            rowCount++;

        for (int i = 0; i < rowCount; i++) {
            if(isCancelled())
                return new ArrayList<String>();
            for (int j = 1; j <= 50 & sizeParamsList > i * 50 + j; j++) {
                paramsSet.add(paramsList.get(i * 50 + j));
            }

            try {
                titleWithRedirect.addAll(wikipedia.getTitlePageWithRedirect(paramsSet));
            } catch (IOException e) {
                isCrached = true;
                e.printStackTrace();
            }
            paramsSet.clear();
        }
        //HashSet<String> paramsSet = new HashSet<String>(paramsList);


        for (int i = 0; i < titleWithRedirect.size(); i++) {
            titleWithRedirect.set(i, StringEscapeUtils.unescapeJava(titleWithRedirect.get(i)));
        }
        log(Level.INFO, "GetPageRedirect", "GetPageRedirect success");
        return titleWithRedirect;

    }

    private String GetAddressPageForRedirect(String params[]) throws TimeoutException {
        if(isCancelled())
            return "";

        String str = null;
        try {
            str = wikipedia.getRedirectForPage(params[0]);
        } catch (IOException e) {
            isCrached = true;
            e.printStackTrace();
        }
        log(Level.INFO, "GetAddressPageForRedirect", "GetAddressPageForRedirect success");
        return str;

    }

    private SortedSet<EventModel> GetCoordForEvent(List<EventWithLex> eventWithLexes){
        if(isCancelled())
            return new TreeSet<EventModel>();

        SortedSet<EventModel> events = new TreeSet<EventModel>();
        ArrayList<String> allLexemes = new ArrayList<String>();

        for(EventWithLex eventWithLex : eventWithLexes){
            allLexemes.addAll(eventWithLex.lexemes);
        }

        HashSet<String> set = new HashSet<String>(allLexemes);

        String[] rawLexMas = set.toArray(new String[set.size()]);

        try {
            ArrayList<String> lexesWithCoord = GetPageTemplates(rawLexMas);

            String[] lexesWithCoordMas = lexesWithCoord.toArray(new String[lexesWithCoord.size()]);

            ArrayMap<String, Coordinate> placesWithCoord = GetCoords(lexesWithCoordMas);



            for(int i = 0; i < eventWithLexes.size(); i++){
                ArrayList<String> lexesFromEvent = eventWithLexes.get(i).lexemes;

                for(String lexFromEvent : lexesFromEvent){

                    int ind = placesWithCoord.indexOfKey(lexFromEvent);
                    if(ind >= 0){
                        eventWithLexes.get(i).evntModel.coord = placesWithCoord.valueAt(ind);
                        //

                        break;
                    }
                }

            }

            for(EventWithLex eventWithLex : eventWithLexes){
                events.add(eventWithLex.evntModel);
                //mClusterManager.setOnClusterItemClickListener(eventWithLex.evntModel.coord);
            }

        } catch (TimeoutException e) {
            isCrached = true;
            e.printStackTrace();
        } finally {
            log(Level.INFO, "GetCoordForEvent", "GetCoordForEvent success");
            return events;
        }

    }

    private ArrayList<String> GetPageTemplates(String[] strs) throws TimeoutException {
        if(isCancelled())
            return new ArrayList<String>();

        ArrayList<String> lexesList = new ArrayList<String>(Arrays.asList(strs));

        ArrayList<String> titleWithCoordTemplate = new ArrayList<String>();
        ArrayList<String> tempList = new ArrayList<String>();

        int sizeParamsList = lexesList.size();
        int rowCount = sizeParamsList / 50;
        if (lexesList.size() % 50 != 0)
            rowCount++;

        for (int i = 0; i < rowCount; i++) {
            if(isCancelled())
                return new ArrayList<String>();
            for (int j = 0; j < 50 & sizeParamsList > i * 50 + j; j++) {
                tempList.add(lexesList.get(i * 50 + j));
            }

            try {
                titleWithCoordTemplate.addAll(wikipedia.getTitlePageWithCoordTemplate(tempList));
            } catch (IOException e) {
                isCrached = true;
                e.printStackTrace();
            }
            tempList.clear();
        }


        for (int i = 0; i < titleWithCoordTemplate.size(); i++) {
            titleWithCoordTemplate.set(i, StringEscapeUtils.unescapeJava(titleWithCoordTemplate.get(i)));
        }

        log(Level.INFO, "GetPageTemplates", "GetPageTemplates success");
        return titleWithCoordTemplate;

    }

    private ArrayMap<String, Coordinate> GetCoords(String[] params) throws TimeoutException {
        if(isCancelled())
            return new ArrayMap<String, Coordinate>();

        ArrayList<String> paramsList = new ArrayList<String>(Arrays.asList(params));
        ArrayMap<String, Coordinate> placesWithCoord = new ArrayMap<String, Coordinate>();
        ArrayMap<String, Coordinate> coord = new ArrayMap<String, Coordinate>();

        //ArrayList<String> titleWithCoordTemplate = new ArrayList<String>();
        ArrayList<String> tempList = new ArrayList<String>();

        int sizeParamsList = paramsList.size();
        int rowCount = sizeParamsList / 50;
        if (paramsList.size() % 50 != 0)
            rowCount++;

        for (int i = 0; i < rowCount; i++) {
            if(isCancelled())
                return new ArrayMap<String, Coordinate>();
            for (int j = 0; j < 50 & sizeParamsList > i * 50 + j; j++) {
                tempList.add(paramsList.get(i * 50 + j));
            }

            ArrayMap<String, LatLng> latLng = null;
            try {
                latLng = wikipedia.getCoordinateForPlaces(tempList);
            } catch (IOException e) {
                isCrached = true;
                e.printStackTrace();
            }

            for (int j = 0; j < latLng.size(); j++) {
                placesWithCoord.put(latLng.keyAt(j), new Coordinate(latLng.valueAt(j)));
            }

            //placesWithCoord.putAll((Map<? extends String, ? extends Coordinate>) coord);
            tempList.clear();
            coord.clear();
        }
        log(Level.INFO, "GetCoords", "GetCoords success");
        return placesWithCoord;
    }



    private ArrayList<EventModel> ParseEvent(ArrayMap<Integer, String> eventsByYear){
        if(isCancelled())
            return new ArrayList<EventModel>();

        List<EventModel> eventList = new ArrayList<EventModel>();

        String eventItem = null;
        int start;
        int finish;
        int startEv;
        int finishEv;
        boolean isLast = false;
        boolean isDate = false;

        for(int i = 0; i < eventsByYear.size(); i++) {
            try {
                isLast = false;
                String events = eventsByYear.valueAt(i);

                start = events.indexOf("== —обыти€ ==");
                if (start < 0) {
                    start = events.indexOf("== ќжидаемые событи€ ==");
                    finish = events.indexOf(" ==\n", start + 24);
                } else
                    finish = events.indexOf(" ==\n", start + 14);
                if (finish != -1)
                    events = events.substring(start + 14, finish);
                else events = events.substring(start + 14);

                startEv = events.indexOf("*");
                finishEv = events.indexOf("*", startEv + 1);

                while (finishEv > 0) {

                    if ((finishEv - startEv) != 1 & (finishEv - startEv) != 0) {
                        if ((finishEv - startEv) > 3) {
                            if (finishEv - startEv > 18) {//
                                isDate = false;
                                eventItem = events.substring(startEv + 2, finishEv - 1);

                                eventItem = ParseEventHelperRef(eventItem);

                                log(Level.INFO, "ParseEvent", "new EventModel " + Integer.toString(eventsByYear.keyAt(i)) +
                                        ": " + eventItem);
                                eventList.add(new EventModel(eventItem, eventsByYear.keyAt(i)));

                            } else { //
                                String str = events.substring(startEv + 2, finishEv - 1).trim();
                                if (str.compareTo("") != 0) {
                                    isDate = true;
                                    eventItem = events.substring(startEv + 4, finishEv - 3) + " Ч ";
                                }
                            }
                        }
                    } else {
                        if (!isLast & isDate) {
                            startEv++;
                            finishEv = events.indexOf("*", startEv + 1);

                            int indxDef = eventItem.indexOf(" Ч ");

                            if (indxDef != -1) {
                                if (finishEv > -1)
                                    eventItem = eventItem.substring(0, indxDef + 3) +
                                            events.substring(startEv + 2, finishEv - 1);

                                else {
                                    eventItem = eventItem.substring(0, indxDef + 3) +
                                            events.substring(startEv + 2);
                                    finishEv = events.length() - 2;
                                }
                            } else {
                                if (finishEv > -1)
                                    eventItem = eventItem +
                                            events.substring(startEv + 2, finishEv - 1);
                                else {
                                    eventItem = eventItem +
                                            events.substring(startEv + 2);
                                    finishEv = events.length() - 2;
                                }
                            }

                            eventItem = ParseEventHelperRef(eventItem);

                            log(Level.INFO, "ParseEvent", "new EventModel " + Integer.toString(eventsByYear.keyAt(i)) +
                                    ": " + eventItem);
                            eventList.add(new EventModel(eventItem, eventsByYear.keyAt(i)));
                        }
                    }

                    startEv = finishEv;
                    finishEv = events.indexOf("*", startEv + 1);
                    if (finishEv < 0 & !isLast) {
                        finishEv = events.indexOf("\n", startEv + 1) + 1;
                        isLast = true;
                    }
                }
            } catch (StringIndexOutOfBoundsException e){
                continue;
            }
        }

        activity.db = activity.dbHelper.getWritableDatabase();

        Cursor cursor = activity.db.query("Events", new String[]{"year", "event"}, "year >= ? and year <= ?",
                new String[]{String.valueOf(activity.globalYearStart), String.valueOf(activity.globalYearFinish)}, null, null, null);

        ArrayList<EventModel> eventsFromDB = new ArrayList<EventModel>();
        //ArrayList<EventModel> eventsToDelFromDB = new ArrayList<EventModel>();
        ArrayList<EventModel> eventsToParseLex = new ArrayList<EventModel>();

        if(cursor == null || cursor.getCount() == 0){
            eventsToParseLex.addAll(eventList);
        }

        else {
            cursor.moveToFirst();
            do {
                int yearFromDB = cursor.getInt(cursor.getColumnIndex("year"));
                String textFromDB = cursor.getString(cursor.getColumnIndex("event"));

                eventsFromDB.add(new EventModel(textFromDB, yearFromDB));
            } while (cursor.moveToNext());

            for(EventModel evntMod : eventList){
                if (!(eventsFromDB.contains(evntMod))){
                    eventsToParseLex.add(evntMod);
                }
            }
        }
        cursor.close();
        activity.dbHelper.close();


        for(EventModel evntMod : eventsFromDB){
            if (!(eventList.contains(evntMod)) & eventsByYear.keySet().contains(evntMod.year)){
                //eventsToDelFromDB.add(evntMod);
                eventsForDeleteDB.add(new EventModel(evntMod.text, evntMod.year));
                //db.delete("Event", "year = ? and text = ?", new String[]{String.valueOf(evntMod.year), evntMod.text});
            }
        }
        log(Level.INFO, "ParseEvent", "ParseEvent success");
        return eventsToParseLex;

    }

    private String ParseEventHelperRef(String eventItem){
        if(isCancelled())
            return "";

        int startRef;
        int finishRef;

        int indxSlash = eventItem.indexOf("==");
        if (indxSlash != -1) {
            eventItem = eventItem.substring(0, indxSlash - 2);
        }


        startRef = eventItem.indexOf("<ref");
        while (startRef != -1) {
            startRef = eventItem.indexOf("<ref>");
            if (startRef != -1) {
                finishRef = eventItem.indexOf("</ref>", startRef);
                if (finishRef < 0)
                    eventItem = eventItem.substring(0, startRef);
                else
                    eventItem = eventItem.substring(0, startRef) + eventItem.substring(finishRef + 6);

                startRef = eventItem.indexOf("<ref");
            } else {
                startRef = eventItem.indexOf("<ref");
                finishRef = eventItem.indexOf("/>", startRef);
                if (finishRef != -1) {
                    eventItem = eventItem.substring(0, startRef) + eventItem.substring(finishRef + 2);
                } else {
                    finishRef = eventItem.indexOf("</ref>", startRef);
                    if (finishRef > -1)
                        eventItem = eventItem.substring(0, startRef) + eventItem.substring(finishRef + 6);
                    else
                        eventItem = eventItem.substring(0, startRef);
                }
                startRef = eventItem.indexOf("<ref");
            }

        }

        while (eventItem.contains("[[")) {
            eventItem = TrimHooks(eventItem);
        }
        return eventItem;
    }

    private String TrimHooks(String evnt) {
        if(isCancelled())
            return "";

        //int startToNext = start;
        try {
            int startEv = evnt.indexOf("[[");
            if (startEv != -1) {
                int finishEv = evnt.indexOf("]]", startEv + 2);
                //startToNext =+ 4;

                if (finishEv != -1) {
                    String inHooks = evnt.substring(startEv + 2, finishEv);
                    int separ = inHooks.indexOf("|");
                    if (separ != -1) {
                        //startToNext += inHooks.length() - separ - 1;
                        inHooks = inHooks.substring(separ + 1, inHooks.length());
                    }

                    evnt = evnt.substring(0, startEv) + inHooks + evnt.substring(finishEv + 2, evnt.length());
                }
                else {
                    //TrimHooks(evnt);
                    evnt =  evnt.substring(0, startEv) + evnt.substring(startEv + 2, evnt.length() - 1);
                }
            }

            return evnt;
        } finally {
            //log(Level.INFO, "TrimHooks", "TrimHooks success");
        }

    }

    private String PunctuationHook(String word){
        if(isCancelled())
            return "";

        word = word.replace(" ", "");
        word = word.replace(",", "");
        word = word.replace(";", "");
        word = word.replace(":", "");
        word = word.replace(".", "");
        word = word.replace("...", "");
        word = word.replace(")", "");
        word = word.replace("(", "");
        word = word.replace("Ч", "");
        word = word.replace("\"", "");
        word = word.replace("'", "");
        //log(Level.INFO, "PunctuationHook", "PunctuationHook success");
        return  word;
    }



    private void WriteDB(SortedSet<EventModel> events) {
        if(isCancelled())
            return;

        activity.db = activity.dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();

        activity.db.beginTransaction();

        try {
            for (EventModel eventModel : events) {

                cv.put("year", eventModel.year);
                cv.put("event", eventModel.text);
                if (eventModel.coord != null) {
                    cv.put("latitude", eventModel.coord.getmPosition().latitude);
                    cv.put("longitude", eventModel.coord.getmPosition().longitude);
                }

                activity.db.insert("Events", null, cv);

                cv.clear();
            }

            for (EventModel eventModel : eventsForDeleteDB) {
                Log.i("eventsForDeleteDB", eventModel.text + " " + String.valueOf(eventModel.year));
                activity.db.delete("Events", "year = ? and event = ?",
                        new String[]{String.valueOf(eventModel.year),  eventModel.text});
                //activity.db.delete("Events", "year = " + String.valueOf(eventModel.year) + " and event = \"" + eventModel.text + "\"", null);
            }

            for (PageModel pageModel : pagesForUdateDB) {

                cv.put("revisionID", pageModel.revID);
                cv.put("lastUpdate", pageModel.lastUpdate);

                activity.db.update("Pages", cv, "year = ?",
                        new String[]{String.valueOf(pageModel.year)});

                cv.clear();
            }

            for (PageModel pageModel : pagesForIsertDB) {

                cv.put("year", pageModel.year);
                cv.put("revisionID", pageModel.revID);
                cv.put("lastUpdate", pageModel.lastUpdate);

                activity.db.insert("Pages", null, cv);

                cv.clear();
            }

            activity.db.setTransactionSuccessful();

        } finally {
            activity.db.endTransaction();
            activity.dbHelper.close();
            log(Level.INFO, "WriteDB", "WriteDB success");
        }

    }

//    private class DBHelper extends SQLiteOpenHelper {
//
//        public DBHelper(Context context){
//            super(context, "chronDB", null, 1);
//            log(Level.INFO, "DBHelper", "DBHelper -> DBHelper success");
//        }
//
//        @Override
//        public void onCreate(SQLiteDatabase db) {
//            db.execSQL("create table Pages " +
//                    "(year integer not null unique, " + "revisionID integer, " + "lastUpdate text)");
//
//            db.execSQL("create table Events " +
//                    "(year integer, " + "event text, " +
//                    "latitude double, " + "longitude double)");
//            log(Level.INFO, "onCreate", "DBHelper -> onCreate success");
//        }
//
//        @Override
//        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//            log(Level.INFO, "onUpgrade", "DBHelper -> onUpgrade success");
//        }
//    }



    protected void log(Level level, String method, String text)
    {
        logger.logp(level, "Chron", method, text);
    }
}
