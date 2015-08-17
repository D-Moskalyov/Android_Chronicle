package wikipedia;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * Created by ִלטענטי on 17.08.2015.
 */
public class Wiki implements Serializable{

    public static final int ASSERT_NONE = 0;
    public static final int ASSERT_USER = 1;
    public static final int ASSERT_BOT = 2;

    private String domain;
    protected String query, base, apiUrl;
    protected String scriptPath = "/w";
    private String protocol = "https://";

//    private Map<String, String> cookies = new HashMap<>(12);
//    private String useragent = "Wiki.java/" + " (https://github.com/MER-C/wiki-java/)";
    private String useragent = "com.chronicle.app/" + "viva-barca@i.ua";
    private int maxlag = 5;
    private boolean zipped = true;
    private Level loglevel = Level.ALL;
    private static final Logger logger = Logger.getLogger("wiki");
    private int assertion = ASSERT_NONE; // assertion mode

    private static final int CONNECTION_CONNECT_TIMEOUT_MSEC = 30000; // 30 seconds
    private static final int CONNECTION_READ_TIMEOUT_MSEC = 180000; // 180 seconds

    public Wiki()
    {
        this("en.wikipedia.org", "/w");
    }

    public Wiki(String domain)
    {
        this(domain, "/w");
    }

    public Wiki(String domain, String scriptPath)
    {
        this(domain, scriptPath, "https://");
    }

    public Wiki(String domain, String scriptPath, String protocol)
    {
        if (domain == null || domain.isEmpty())
            domain = "en.wikipedia.org";
        this.domain = domain;
        this.scriptPath = scriptPath;
        this.protocol = protocol;


        logger.setLevel(loglevel);
        log(Level.CONFIG, "<init>", "Using Wiki.java");
        initVars();
    }

    protected void initVars()
    {
        StringBuilder basegen = new StringBuilder(protocol);
        basegen.append(domain);
        basegen.append(scriptPath);
        StringBuilder apigen = new StringBuilder(basegen);
        apigen.append("/api.php?format=json&rawcontinue=1&");
//        if (maxlag >= 0)
//        {
//            apigen.append("maxlag=");
//            apigen.append(maxlag);
//            apigen.append("&");
//            basegen.append("/index.php?maxlag=");
//            basegen.append(maxlag);
//            basegen.append("&title=");
//        }
//        else
            basegen.append("/index.php?title=");
        base = basegen.toString();
        // the native API supports assertions as of MW 1.23
//        if ((assertion & ASSERT_BOT) == ASSERT_BOT)
//            apigen.append("assert=bot&");
//        else if ((assertion & ASSERT_USER) == ASSERT_USER)
//            apigen.append("assert=user&");
        apiUrl = apigen.toString();
        apigen.append("action=query&");
//        if (resolveredirect)
//            apigen.append("redirects&");
        query = apigen.toString();
    }

    protected void log(Level level, String method, String text)
    {
        logger.logp(level, "Wiki", method, "[{0}] {1}", new Object[]{domain, text});
    }

    public String getPageText(String title) throws IOException
    {
        // pitfall check
//        if (namespace(title) < 0)
//            throw new UnsupportedOperationException("Cannot retrieve Special: or Media: pages!");

        String url = base + URLEncoder.encode(normalize(title), "UTF-8") + "&action=raw";
        String temp = fetch(url, "getPageText");
        log(Level.INFO, "getPageText", "Successfully retrieved text of " + title);
        return temp;
    }

    protected String fetch(String url, String caller) throws IOException
    {
        // connect
        logurl(url, caller);
        URLConnection connection = makeConnection(url);
        connection.setConnectTimeout(CONNECTION_CONNECT_TIMEOUT_MSEC);
        connection.setReadTimeout(CONNECTION_READ_TIMEOUT_MSEC);
        setUserAgent(connection);
        //setCookies(connection);
        connection.connect();
        //grabCookies(connection);

        // check lag
        int lag = connection.getHeaderFieldInt("X-Database-Lag", -5);
        if (lag > maxlag)
        {
            try
            {
                synchronized(this)
                {
                    int time = connection.getHeaderFieldInt("Retry-After", 10);
                    log(Level.WARNING, caller, "Current database lag " + lag + " s exceeds " + maxlag + " s, waiting " + time + " s.");
                    Thread.sleep(time * 1000);
                }
            }
            catch (InterruptedException ex)
            {
                // nobody cares
            }
            return fetch(url, caller); // retry the request
        }

        // get the text
        String temp;
        BufferedReader in;
        try {
            if (zipped) {
                in = new BufferedReader(new InputStreamReader(new GZIPInputStream(connection.getInputStream())));
            } else {
                in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            }
            String line;
            StringBuilder text = new StringBuilder(100000);
            while ((line = in.readLine()) != null)
            {
                text.append(line);
                text.append("\n");
            }
            temp = text.toString();

            if (temp.contains("<error code="))
            {
                // assertions
                if ((assertion & ASSERT_BOT) == ASSERT_BOT && temp.contains("error code=\"assertbotfailed\""))
                    // assert !temp.contains("error code=\"assertbotfailed\"") : "Bot privileges missing or revoked, or session expired.";
                    throw new AssertionError("Bot privileges missing or revoked, or session expired.");
                if ((assertion & ASSERT_USER) == ASSERT_USER && temp.contains("error code=\"assertuserfailed\""))
                    // assert !temp.contains("error code=\"assertuserfailed\"") : "Session expired.";
                    throw new AssertionError("Session expired.");
                // Something *really* bad happened. Most of these are self-explanatory
                // and are indicative of bugs (not necessarily in this framework) or
                // can be avoided entirely.
                if (!temp.matches("code=\"(rvnosuchsection)")) // list "good" errors here
                    throw new UnknownError("MW API error. Server response was: " + temp);
            }
            return temp;
        }
        catch (Exception ex){ throw new IOException();}
//        try (BufferedReader in = new BufferedReader(new InputStreamReader(
//                zipped ? new GZIPInputStream(connection.getInputStream()) : connection.getInputStream(), "UTF-8")))
//        {
//            String line;
//            StringBuilder text = new StringBuilder(100000);
//            while ((line = in.readLine()) != null)
//            {
//                text.append(line);
//                text.append("\n");
//            }
//            temp = text.toString();
//        }
//        if (temp.contains("<error code="))
//        {
//            // assertions
//            if ((assertion & ASSERT_BOT) == ASSERT_BOT && temp.contains("error code=\"assertbotfailed\""))
//                // assert !temp.contains("error code=\"assertbotfailed\"") : "Bot privileges missing or revoked, or session expired.";
//                throw new AssertionError("Bot privileges missing or revoked, or session expired.");
//            if ((assertion & ASSERT_USER) == ASSERT_USER && temp.contains("error code=\"assertuserfailed\""))
//                // assert !temp.contains("error code=\"assertuserfailed\"") : "Session expired.";
//                throw new AssertionError("Session expired.");
//            // Something *really* bad happened. Most of these are self-explanatory
//            // and are indicative of bugs (not necessarily in this framework) or
//            // can be avoided entirely.
//            if (!temp.matches("code=\"(rvnosuchsection)")) // list "good" errors here
//                throw new UnknownError("MW API error. Server response was: " + temp);
//        }
//        return temp;
    }

//    protected void setCookies(URLConnection u)
//    {
//        StringBuilder cookie = new StringBuilder(100);
//        for (Map.Entry<String, String> entry : cookies.entrySet())
//        {
//            cookie.append(entry.getKey());
//            cookie.append("=");
//            cookie.append(entry.getValue());
//            cookie.append("; ");
//        }
//        u.setRequestProperty("Cookie", cookie.toString());
//
//        // enable gzip compression
//        if (zipped)
//            u.setRequestProperty("Accept-encoding", "gzip");
//        u.setRequestProperty("User-Agent", useragent);
//    }

    protected void setUserAgent(URLConnection u)
    {
        // enable gzip compression
        if (zipped)
            u.setRequestProperty("Accept-encoding", "gzip");
        u.setRequestProperty("User-Agent", useragent);
    }
//
//    private void grabCookies(URLConnection u)
//    {
//        String headerName;
//        for (int i = 1; (headerName = u.getHeaderFieldKey(i)) != null; i++)
//            if (headerName.equals("Set-Cookie"))
//            {
//                String cookie = u.getHeaderField(i);
//                cookie = cookie.substring(0, cookie.indexOf(';'));
//                String name = cookie.substring(0, cookie.indexOf('='));
//                String value = cookie.substring(cookie.indexOf('=') + 1, cookie.length());
//                // these cookies were pruned, but are still sent for some reason?
//                // TODO: when these cookies are no longer sent, remove this test
//                if (!value.equals("deleted"))
//                    cookies.put(name, value);
//            }
//    }

    protected URLConnection makeConnection(String url) throws IOException
    {
        return new URL(url).openConnection();
    }

    public String normalize(String s) throws IOException
    {
        // remove leading colon
        if (s.startsWith(":"))
            s = s.substring(1);
        if (s.isEmpty())
            return s;

//        int ns = namespace(s);
//        // localize namespace names
//        if (ns != MAIN_NAMESPACE)
//        {
//            int colon = s.indexOf(":");
//            s = namespaceIdentifier(ns) + s.substring(colon);
//        }
        char[] temp = s.toCharArray();
//        if (wgCapitalLinks)
//        {
//            // convert first character in the actual title to upper case
//            if (ns == MAIN_NAMESPACE)
//                temp[0] = Character.toUpperCase(temp[0]);
//            else
//            {
//                int index = namespaceIdentifier(ns).length() + 1; // + 1 for colon
//                temp[index] = Character.toUpperCase(temp[index]);
//            }
//        }

        for (int i = 0; i < temp.length; i++)
        {
            switch (temp[i])
            {
                // illegal characters
                case '{':
                case '}':
                case '<':
                case '>':
                case '[':
                case ']':
                case '|':
                    throw new IllegalArgumentException(s + " is an illegal title");
                case '_':
                    temp[i] = ' ';
                    break;
            }
        }
        // https://www.mediawiki.org/wiki/Unicode_normalization_considerations
        String temp2 = new String(temp).trim().replaceAll("\\s+", " ");
        return Normalizer.normalize(temp2, Normalizer.Form.NFC);
    }

    protected void logurl(String url, String method)
    {
        logger.logp(Level.INFO, "Wiki", method, "Fetching URL {0}", url);
    }

//    public String resolveRedirect(String title) throws IOException
//    {
//        return resolveRedirects(new String[] { title })[0];
//    }

//    public String[] resolveRedirects(String[] titles) throws IOException
//    {
//        StringBuilder url = new StringBuilder(query);
//        if (!resolveredirect)
//            url.append("redirects&");
//        url.append("titles=");
//        String[] ret = new String[titles.length];
//        String[] temp = constructTitleString(titles);
//        for (String blah : temp)
//        {
//            String line = fetch(url.toString() + blah, "resolveRedirects");
//
//            // expected form: <redirects><r from="Main page" to="Main Page"/>
//            // <r from="Home Page" to="Home page"/>...</redirects>
//            // TODO: look for the <r> tag instead
//            for (int j = line.indexOf("<r "); j > 0; j = line.indexOf("<r ", ++j))
//            {
//                String parsedtitle = parseAttribute(line, "from", j);
//                for (int i = 0; i < titles.length; i++)
//                    if (normalize(titles[i]).equals(parsedtitle))
//                        ret[i] = parseAttribute(line, "to", j);
//            }
//        }
//        return ret;
//    }
}
