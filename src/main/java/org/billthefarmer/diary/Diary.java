//  Diary - Personal diary for Android
//  Copyright © 2012  Josep Portella Florit <hola@josep-portella.com>
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.billthefarmer.diary;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.Toast;
import android.widget.ViewSwitcher;
import android.support.v4.content.FileProvider;

import android.util.Log;

import org.billthefarmer.markdown.MarkdownView;
import org.billthefarmer.view.CustomCalendarDialog;
import org.billthefarmer.view.CustomCalendarView;
import org.billthefarmer.view.DayDecorator;
import org.billthefarmer.view.DayView;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// Diary
public class Diary extends Activity
        implements DatePickerDialog.OnDateSetListener,
        CustomCalendarDialog.OnDateSetListener
{
    private final static int ADD_MEDIA = 1;

    private final static int REQUEST_READ = 1;
    private final static int REQUEST_WRITE = 2;
    private final static int REQUEST_TEMPLATE = 3;
    private final static int REQUEST_CAMERA = 4;
    private final static int GET_IMAGE = 5;
    private final static int image_request = 6;
    Uri fileUri;
    String photoPath = "";

    private final static int POSITION_DELAY = 128;
    private final static int BUFFER_SIZE = 4096;
    private final static int SCALE_RATIO = 128;
    private final static int FIND_DELAY = 256;

    // Indices for the ViewSwitchers
    private static final int EDIT_TEXT = 0;
    private static final int MARKDOWN = 1;
    private static final int ACCEPT = 0;
    private static final int EDIT = 1;
    private int cam_num=0;

    public final static String DIARY = "Diary";

    public final static String YEAR = "year";
    public final static String MONTH = "month";
    public final static String DAY = "day";

    public final static String SAVED = "saved";
    public final static String SHOWN = "shown";
    public final static String ENTRY = "entry";
    String CurrentImagePath = null;

    // Patterns
    public final static Pattern PATTERN_CHARS =
            Pattern.compile("[\\(\\)\\[\\]\\{\\}\\<\\>\"'`]");
    public final static Pattern MEDIA_PATTERN =
            Pattern.compile("!\\[(.*)\\]\\((.+)\\)", Pattern.MULTILINE);
    public final static Pattern EVENT_PATTERN =
            Pattern.compile("^@ *(\\d{1,2}:\\d{2}) +(.+)$", Pattern.MULTILINE);
    public final static Pattern MAP_PATTERN =
            Pattern.compile("\\[(?:osm:)?(-?\\d+[,.]\\d+)[,;] ?(-?\\d+[,.]\\d+)\\]",
                    Pattern.MULTILINE);
    public final static Pattern GEO_PATTERN =
            Pattern.compile("geo:(-?\\d+[.]\\d+), ?(-?\\d+[.]\\d+).*");
    public final static Pattern DATE_PATTERN =
            Pattern.compile("\\[(.+)\\]\\(date:(\\d+.\\d+.\\d+)\\)",
                    Pattern.MULTILINE);
    public final static Pattern POSN_PATTERN =
            Pattern.compile("^ ?\\[([<#>])\\]: ?#(?: ?\\((\\d+)\\))? *$",
                    Pattern.MULTILINE);
    public final static Pattern FILE_PATTERN =
            Pattern.compile("([0-9]{4}).([0-9]{2}).([0-9]{2}).txt$");

    public final static String YEAR_DIR = "^[0-9]{4}$";
    public final static String MONTH_DIR = "^[0-9]{2}$";
    public final static String DAY_FILE = "^[0-9]{2}.txt$";

    public final static String ZIP = ".zip";
    public final static String HELP = "help.md";
    public final static String STYLES = "file:///android_asset/styles.css";
    public final static String CSS_STYLES = "css/styles.css";
    public final static String JS_SCRIPT = "js/script.js";
    public final static String FILE_PROVIDER =
            "org.billthefarmer.diary.fileprovider";

    public final static String MEDIA_TEMPLATE = "![%s](%s)\n";
    public final static String LINK_TEMPLATE = "[%s](%s)\n";
    public final static String AUDIO_TEMPLATE =
            "<audio controls src=\"%s\"></audio>\n";
    public final static String VIDEO_TEMPLATE =
            "<video controls src=\"%s\"></video>\n";
    public final static String EVENT_TEMPLATE = "@:$1 $2";
    public final static String MAP_TEMPLATE =
            "<iframe width=\"560\" height=\"420\" " +
                    "src=\"http://www.openstreetmap.org/export/embed.html?" +
                    "bbox=%f,%f,%f,%f&amp;layer=mapnik\">" +
                    "</iframe><br/><small>" +
                    "<a href=\"http://www.openstreetmap.org/#map=16/%f/%f\">" +
                    "View Larger Map</a></small>\n";
    public final static String GEO_TEMPLATE = "![osm](geo:%f,%f)";
    public final static String POSN_TEMPLATE = "[#]: # (%d)";
    public final static String EVENTS_TEMPLATE = "@:%s %s\n";

    public final static String BRACKET_CHARS = "([{<";
    public final static String DIARY_IMAGE = "Diary.png";

    public final static String GEO = "geo";
    public final static String OSM = "osm";
    public final static String HTTP = "http";
    public final static String HTTPS = "https";
    public final static String CONTENT = "content";
    public final static String TEXT_PLAIN = "text/plain";
    public final static String IMAGE_PNG = "image/png";
    public final static String WILD_WILD = "*/*";
    public final static String IMAGE = "image";
    public final static String AUDIO = "audio";
    public final static String VIDEO = "video";


    public int wordLength = 0;


    public File imagefile;

    private boolean custom = true;
    private boolean markdown = true;
    private boolean external = false;
    private boolean useIndex = false;
    private boolean useTemplate = false;
    private boolean copyMedia = false;
    private boolean darkTheme = false;

    private boolean changed = false;
    private boolean shown = true;

    private boolean multi = false;
    private boolean entry = false;

    private long saved = 0;

    private float minScale = 1000;

    private boolean canSwipe = true;
    private boolean haveMedia = false;

    private long indexPage;
    private long templatePage;

    private String folder = DIARY;

    private Calendar prevEntry;
    private Calendar currEntry;
    private Calendar nextEntry;

    private EditText textView;
    private ScrollView scrollView;

    private MarkdownView markdownView;
    private ViewSwitcher layoutSwitcher;
    private ViewSwitcher buttonSwitcher;
    private ImageButton favouriteBtn;
    private ImageButton moodBtn;
    private ImageButton locationBtn;

    private SearchView searchView;
    private Menu activityMenu;
    private MenuItem searchItem;

    private GestureDetector gestureDetector;

    private Deque<Calendar> entryStack;

    private Toast toast;
    private View accept;
    private View edit;

    String currentImagePath = null;
    // sortFiles
    private static File[] sortFiles(File[] files)
    {
        if (files == null)
            return new File[0];
        // compare
        Arrays.sort(files, (file1, file2) ->
                file2.getName().compareTo(file1.getName()));
        return files;
    }

    // listYears
    private static File[] listYears(File home)
    {
        // accept
        return sortFiles(home.listFiles((dir, filename) ->
                filename.matches(YEAR_DIR)));
    }

    // listMonths
    private static File[] listMonths(File yearDir)
    {
        // accept
        return sortFiles(yearDir.listFiles((dir, filename) ->
                filename.matches(MONTH_DIR)));
    }

    // listDays
    private static File[] listDays(File monthDir)
    {
        // accept
        return sortFiles(monthDir.listFiles((dir, filename) ->
                filename.matches(DAY_FILE)));
    }

    // yearValue
    private static int yearValue(File yearDir)
    {
        return Integer.parseInt(yearDir.getName());
    }

    // monthValue
    private static int monthValue(File monthDir)
    {
        return Integer.parseInt(monthDir.getName()) - 1;
    }

    // dayValue
    private static int dayValue(File dayFile)
    {
        return Integer.parseInt(dayFile.getName().split("\\.")[0]);
    }

    // read
    private static CharSequence read(File file)
    {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file)))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                text.append(line);
                text.append(System.getProperty("line.separator"));
            }

            return text;
        }

        catch (Exception e) {}

        return null;
    }

    // parseTime
    private static long parseTime(File file)
    {
        Matcher matcher = FILE_PATTERN.matcher(file.getPath());
        if (matcher.find())
        {
            try
            {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2)) - 1;
                int dayOfMonth = Integer.parseInt(matcher.group(3));

                return new GregorianCalendar
                        (year, month, dayOfMonth).getTimeInMillis();
            }

            catch (NumberFormatException e)
            {
                return -1;
            }
        }

        return -1;
    }

    // listEntries
    private static void listEntries(File directory, List<File> fileList)
    {
        // Get all entry files from a directory.
        File[] files = directory.listFiles();
        if (files != null)
            for (File file : files)
            {
                if (file.isFile() && file.getName().matches(DAY_FILE))
                {
                    fileList.add(file);
                }

                else if (file.isDirectory())
                {
                    listEntries(file, fileList);
                }
            }
    }

    // listFiles
    private static void listFiles(File directory, List<File> fileList)
    {
        // Get all entry files from a directory.
        File[] files = directory.listFiles();
        if (files != null)
            for (File file : files)
            {
                if (file.isFile())
                {
                    fileList.add(file);
                }

                else if (file.isDirectory())
                {
                    fileList.add(file);
                    listFiles(file, fileList);
                }
            }
    }

    // onCreate
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Get preferences
        getPreferences();

        if (darkTheme)
            setTheme(R.style.AppDarkTheme);

        setContentView(R.layout.main);

        textView = findViewById(R.id.text);
        scrollView = findViewById(R.id.scroll);
        markdownView = findViewById(R.id.markdown);

        accept = findViewById(R.id.accept);
        edit = findViewById(R.id.edit);

        layoutSwitcher = findViewById(R.id.layout_switcher);
        buttonSwitcher = findViewById(R.id.button_switcher);

        favouriteBtn = findViewById(R.id.favourite);
        moodBtn = findViewById(R.id.mood_button);
        locationBtn = findViewById(R.id.location_button);

        WebSettings settings = markdownView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        setListeners();

        gestureDetector =
                new GestureDetector(this, new GestureListener());

        entryStack = new ArrayDeque<>();

        // Check startup
        if (savedInstanceState == null)
        {
            Intent intent = getIntent();

            // Check index and start from launcher
            if (useIndex && Intent.ACTION_MAIN.equals(intent.getAction()))
                index();

                // Set the date
            else
                today();

            // Check for sent media
            mediaCheck(intent);
        }
    }

    // onRestoreInstanceState
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        markdownView.restoreState(savedInstanceState);

        setDate(new GregorianCalendar(savedInstanceState.getInt(YEAR),
                savedInstanceState.getInt(MONTH),
                savedInstanceState.getInt(DAY)));

        shown = savedInstanceState.getBoolean(SHOWN);
        entry = savedInstanceState.getBoolean(ENTRY);
        saved = savedInstanceState.getLong(SAVED);
    }

    // onResume
    @Override
    protected void onResume()
    {
        super.onResume();

        boolean dark = darkTheme;

        // Get preferences
        getPreferences();

        // Recreate
        if (dark != darkTheme && Build.VERSION.SDK_INT != Build.VERSION_CODES.M)
            recreate();

        // Set date
        setDate(currEntry);

        // Reload if modified
        if (getFile().lastModified() > saved)
            load();

        // Clear cache
        markdownView.clearCache(true);

        // Copy help text to today's page if no entries
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) &&
                prevEntry == null && nextEntry == null && textView.length() == 0)
            textView.setText(readAssetFile(HELP));

        if (markdown && changed)
            loadMarkdown();


        setVisibility();

    }

    // onSaveInstanceState
    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        markdownView.saveState(outState);

        if (currEntry != null)
        {
            outState.putInt(YEAR, currEntry.get(Calendar.YEAR));
            outState.putInt(MONTH, currEntry.get(Calendar.MONTH));
            outState.putInt(DAY, currEntry.get(Calendar.DATE));

            outState.putBoolean(SHOWN, shown);
            outState.putBoolean(ENTRY, entry);
            outState.putLong(SAVED, saved);
        }
    }

    // onPause
    @Override
    public void onPause()
    {
        super.onPause();
        if (changed)
            save();

        saved = getFile().lastModified();
    }

    // onCreateOptionsMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);

        searchItem = menu.findItem(R.id.search);

        // Set up search view and action expand listener
        if (searchItem != null)
        {
            searchView = (SearchView) searchItem.getActionView();
            searchItem.setOnActionExpandListener(new MenuItem
                    .OnActionExpandListener()
            {
                @Override
                public boolean onMenuItemActionCollapse(MenuItem item)
                {
                    invalidateOptionsMenu();
                    return true;
                }

                @Override
                public boolean onMenuItemActionExpand(MenuItem item)
                {
                    return true;
                }
            });
        }

        // Set up search view options and listener
        if (searchView != null)
        {
            searchView.setSubmitButtonEnabled(true);
            searchView.setImeOptions(EditorInfo.IME_ACTION_GO);
            searchView.setOnQueryTextListener(new QueryTextListener());
        }

        activityMenu = menu;

        return true;
    }

    // onPrepareOptionsMenu
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        Calendar today = GregorianCalendar.getInstance();
        menu.findItem(R.id.today).setEnabled(currEntry == null ||
                currEntry.get(Calendar.YEAR) !=
                        today.get(Calendar.YEAR) ||
                currEntry.get(Calendar.MONTH) !=
                        today.get(Calendar.MONTH) ||
                currEntry.get(Calendar.DATE) !=
                        today.get(Calendar.DATE));
        menu.findItem(R.id.nextEntry).setEnabled(nextEntry != null);
        menu.findItem(R.id.prevEntry).setEnabled(prevEntry != null);
        menu.findItem(R.id.index).setVisible(useIndex);

        //check if the mood is there
        String moodValue = "Remove Mood";
        if(hasAttribute("mood")){
            moodValue = getValueForAttribute("mood");
        }
        updateVisualsForAttribute("mood", moodValue);

        //check if the location is there
        String locationValue = "Remove Location";
        if(hasAttribute("location")){
            locationValue = getValueForAttribute("location");
        }
        updateVisualsForAttribute("location", locationValue);

        //check if it is a favourite
        ImageButton btn = findViewById(R.id.favourite);

        if(isFavourite()){
            btn.setImageResource(R.drawable.filled_star);
        } else {
            btn.setImageResource(R.drawable.unfilled_star);
        }

        // Show find all item
        if (menu.findItem(R.id.search).isActionViewExpanded())
            menu.findItem(R.id.findAll).setVisible(true);
        else
            menu.findItem(R.id.findAll).setVisible(false);

        return true;
    }

    // onOptionsItemSelected
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.sortDate:
                dateSort(getFile());
                break;
            case R.id.lengthSort:
                lengthSort();
                break;
        case android.R.id.home:
            onBackPressed();
            break;
        case R.id.prevEntry:
            prevEntry();
            break;
        case R.id.nextEntry:
            nextEntry();
            break;
        case R.id.today:
            today();
            break;
        case R.id.goToDate:
            goToDate(currEntry);
            break;
        case R.id.index:
            index();
            break;
        case R.id.findAll:
            findAll();
            break;
        case R.id.share:
            share();
            break;
        case R.id.addTime:
            addTime();
            break;
        case R.id.addEvents:
            addEvents();
            break;
        case R.id.addMedia:
            addMedia();
            break;
        case R.id.backup:
            backup();
            break;
        case R.id.settings:
            settings();
            break;
        case R.id.searchHappy:
            sortMood("happy");
            break;
        case R.id.camera:
            open_camera();
            break;
        case R.id.searchSad:
            sortMood("sad");
            break;
        case R.id.searchStress:
            sortMood("stressed");
            break;
        case R.id.searchSchool:
            sortLocation("School");
            break;
        case R.id.searchWork:
            sortLocation("Work");
            break;
        case R.id.searchHome:
            sortLocation("Home");
            break;
        case R.id.searchRec:
            sortLocation("Recreational Area");
            break;
        case R.id.searchFavourite:
            sortFavourite();
            break;
        default:
            return super.onOptionsItemSelected(item);
        }

        // Close text search
        if (searchItem.isActionViewExpanded() &&
                item.getItemId() != R.id.findAll)
            searchItem.collapseActionView();

        return true;
    }

    // onBackPressed
    @Override
    public void onBackPressed()
    {
        // Calendar entry
        if (entry)
        {
            if (!entryStack.isEmpty())
                changeDate(entryStack.pop());

            else
                super.onBackPressed();
        }

        // External
        else
        {
            if (markdownView.canGoBack())
            {
                markdownView.goBack();

                if (!markdownView.canGoBack())
                    changeDate(currEntry);
            }

            else
                super.onBackPressed();
        }
    }

    // onActivityResult
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        // Do nothing if cancelled
        if (resultCode != RESULT_OK)
            return;

        if (requestCode == ADD_MEDIA) {
            Log.i("image", "Add media");
            // Get uri
            Uri uri = data.getData();
            // Resolve content uri
            if (CONTENT.equalsIgnoreCase(uri.getScheme()))
                uri = resolveContent(uri);

            if (uri != null) {
                String type;

                // Get type
                if (CONTENT.equalsIgnoreCase(uri.getScheme()))
                    type = getContentResolver().getType(uri);

                else
                    type = FileUtils.getMimeType(this, uri);

                if (type == null)
                    addLink(uri, uri.getLastPathSegment(), false);

                else if (type.startsWith(IMAGE) ||
                        type.startsWith(AUDIO) ||
                        type.startsWith(VIDEO))
                    addMedia(uri, false);

                else
                    addLink(uri, uri.getLastPathSegment(), false);
            }
        }
        //The request code being sent here as image_request
        if (requestCode == image_request){
            try
            {
                //grabbing the URI and passing it through the function addMedia(), also checking if its null or not
                String type;
                Uri photoss = fileUri;
                if(photoss!=null) {

                    // Get type
                    if (CONTENT.equalsIgnoreCase(photoss.getScheme()))
                        type = getContentResolver().getType(photoss);

                    else
                        type = FileUtils.getMimeType(this, photoss);

                    if (type == null)
                        addLink(photoss, photoss.getLastPathSegment(), false);

                    else if (type.startsWith(IMAGE) ||
                            type.startsWith(AUDIO) ||
                            type.startsWith(VIDEO)) {
                        addMedia(photoss, false);
                        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        //adding the picture to the phone gallery
                        mediaScanIntent.setData(photoss);

                        this.sendBroadcast(mediaScanIntent);
                    }
                    else
                        addLink(photoss, photoss.getLastPathSegment(), false);

                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
    }
    }

    // onDateSet
    @Override
    public void onDateSet(DatePicker view, int year, int month, int day)
    {
        entryStack.push(currEntry);
        changeDate(new GregorianCalendar(year, month, day));

        if (haveMedia)
            addMedia(getIntent());
    }

    // onDateSet
    @Override
    public void onDateSet(CustomCalendarView view, int year, int month, int day)
    {
        entryStack.push(currEntry);
        changeDate(new GregorianCalendar(year, month, day));

        if (haveMedia)
            addMedia(getIntent());
    }

    // dispatchTouchEvent
    @Override
    public boolean dispatchTouchEvent(MotionEvent event)
    {
        if (event.getPointerCount() > 1)
            multi = true;

        gestureDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    private void setListeners()
    {
        if (textView != null)
            textView.addTextChangedListener(new TextWatcher()
            {
                // afterTextChanged
                @Override
                public void afterTextChanged(Editable s)
                {
                    // Text changed
                    changed = true;
                }

                // beforeTextChanged
                @Override
                public void beforeTextChanged(CharSequence s,
                                              int start,
                                              int count,
                                              int after)
                {
                }


            // onTextChanged
            @Override
            public void onTextChanged(CharSequence s,
                                      int start,
                                      int before,
                                      int count)
            {
                //Log.d("something","s b c: " + start +" "+ before +" "+ count);
                wordLength = getWordLength(s);
                Log.d("something","w: " + wordLength);
                assignWordLength(Integer.toString(wordLength));
            }
        });


        if (markdownView != null)
        {
            markdownView.setWebViewClient(new WebViewClient()
            {
                // onPageFinished
                @Override
                public void onPageFinished(WebView view, String url)
                {
                    // Check if entry
                    if (entry)
                    {
                        if (entryStack.isEmpty())
                            getActionBar().setDisplayHomeAsUpEnabled(false);

                        else
                            getActionBar().setDisplayHomeAsUpEnabled(true);

                        setTitleDate(currEntry.getTime());

                        view.clearHistory();
                    }
                    else
                    {
                        if (view.canGoBack())
                        {
                            getActionBar().setDisplayHomeAsUpEnabled(true);

                            // Get page title
                            if (view.getTitle() != null)
                                setTitle(view.getTitle());
                        }
                        else
                        {
                            getActionBar().setDisplayHomeAsUpEnabled(false);
                            setTitleDate(currEntry.getTime());
                        }
                    }

                }

                // onScaleChanged
                @Override
                public void onScaleChanged(WebView view,
                                           float oldScale,
                                           float newScale)
                {
                    if (minScale > oldScale)
                        minScale = oldScale;
                    canSwipe = (Math.abs(newScale - minScale) <
                            minScale / SCALE_RATIO);
                }

                // shouldOverrideUrlLoading
                @Override
                @SuppressWarnings("deprecation")
                public boolean shouldOverrideUrlLoading(WebView view,
                                                        String url)
                {
                    Calendar calendar = diaryEntry(url);
                    // Diary entry
                    if (calendar != null)
                    {
                        entryStack.push(currEntry);
                        changeDate(calendar);
                        return true;
                    }

                    // Use external browser
                    if (external)
                    {
                        Uri uri = Uri.parse(url);
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        if (intent.resolveActivity(getPackageManager()) != null)
                            startActivity(intent);
                        return true;
                    }

                    entry = false;
                    return false;
                }
            });

            // On long click
            markdownView.setOnLongClickListener(v ->
            {
                // Reveal button
                edit.setVisibility(View.VISIBLE);
                return false;
            });
        }

        if (accept != null)
        {
            // On click
            accept.setOnClickListener(v ->
            {
                // Check flag
                if (changed)
                {
                    // Save text
                    save();
                    // Get text
                    loadMarkdown();
                    // Clear flag
                    changed = false;
                    // Set flag
                    entry = true;
                }

                // Animation
                animateAccept();

                // Close text search
                if (searchItem.isActionViewExpanded())
                    searchItem.collapseActionView();

                shown = true;
            });

            // On long click
            accept.setOnLongClickListener(v ->
            {
                // Hide button
                v.setVisibility(View.INVISIBLE);
                return true;
            });
        }

        if (favouriteBtn != null)
        {
            // On click
            favouriteBtn.setOnClickListener(v ->
            {
                toggleFavourite();
            });
        }

        if (moodBtn != null)
        {
            // On click
            moodBtn.setOnClickListener(v ->
            {
                String [] toShow;
                if(hasAttribute("mood")) {
                    //mood is already associated with the entry
                    toShow = new String[]{"Happy", "Sad", "Stressed", "Remove Mood"};
                } else {
                    toShow = new String[]{"Happy", "Sad", "Stressed"};
                }

                String title = "Select a mood for this entry:";
                showAttributeOptions(title, "mood", toShow);
            });
        }

        if (locationBtn != null)
        {
            // On click
            locationBtn.setOnClickListener(v ->
            {
                String [] toShow;
                if(hasAttribute("location")) {
                    //location is already associated with the entry
                    toShow = new String[]{"Home", "School", "Work", "Recreational Area", "Remove Location"};
                } else {
                    toShow = new String[]{"Home", "School", "Work", "Recreational Area"};
                }

                String title = "Select a location for this entry:";
                showAttributeOptions(title, "location", toShow);
            });
        }

        if (edit != null)
        {
            // On click
            edit.setOnClickListener(v ->
            {
                // Animation
                animateEdit();

                // Close text search
                if (searchItem.isActionViewExpanded())
                    searchItem.collapseActionView();

                // Scroll after delay
                edit.postDelayed(() ->
                {
                    // Get selection
                    int selection = textView.getSelectionStart();

                    // Get text position
                    int line = textView.getLayout().getLineForOffset(selection);
                    int position = textView.getLayout().getLineBaseline(line);

                    // Scroll to it
                    int height = scrollView.getHeight();
                    scrollView.smoothScrollTo(0, position - height / 2);
                }, POSITION_DELAY);

                shown = false;
            });

            // On long click
            edit.setOnLongClickListener(v ->
            {
                // Hide button
                v.setVisibility(View.INVISIBLE);
                return true;
            });
        }

        if (textView != null)
        {
            // onFocusChange
            textView.setOnFocusChangeListener((v, hasFocus) ->
            {
                // Hide keyboard
                InputMethodManager imm = (InputMethodManager)
                        getSystemService(INPUT_METHOD_SERVICE);
                if (!hasFocus)
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            });

            // On long click
            textView.setOnLongClickListener(v ->
            {
                // Reveal button
                accept.setVisibility(View.VISIBLE);
                return false;
            });
        }
    }

    /**
     *  Modified from https://www.java67.com/2016/09/3-ways-to-count-words-in-java-string.html
     * @param s is the sequence
     * @return int integer from the word count
     */
    public int getWordLength (CharSequence s)
    {

        String word = s.toString();
        if (word == null || word.isEmpty()) {
            return 0;
        }

        int wordCount = 0;

        boolean isWord = false;
        int endOfLine = word.length() - 1;
        char[] characters = word.toCharArray();

        for (int i = 0; i < characters.length; i++) {

            if (!Character.isWhitespace(characters[i]) && i != endOfLine)
            {//if the current character isn't a space, flag it is the start of word
                isWord = true;


            } else if (Character.isWhitespace(characters[i]) && isWord)
            {//if the character is a space and the previous characters were considered a word, a word has ended
                wordCount++;
                isWord = false;


            } else if (!Character.isWhitespace(characters[i]) && i == endOfLine)
            {//if the last character is a letter, consider it a word
                wordCount++;
            }
        }

        return wordCount;
        //return count;
    }


    // animateAccept
    public void animateAccept()
    {
        // Animation
        layoutSwitcher.setDisplayedChild(MARKDOWN);
        buttonSwitcher.setDisplayedChild(EDIT);
    }

    // animateEdit
    private void animateEdit()
    {
        // Animation
        layoutSwitcher.setDisplayedChild(EDIT_TEXT);
        buttonSwitcher.setDisplayedChild(ACCEPT);
    }

    // getPreferences
    private void getPreferences()
    {
        // Get preferences
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        custom = preferences.getBoolean(Settings.PREF_CUSTOM, true);
        markdown = preferences.getBoolean(Settings.PREF_MARKDOWN, true);
        external = preferences.getBoolean(Settings.PREF_EXTERNAL, false);
        useIndex = preferences.getBoolean(Settings.PREF_USE_INDEX, false);
        useTemplate = preferences.getBoolean(Settings.PREF_USE_TEMPLATE, false);
        copyMedia = preferences.getBoolean(Settings.PREF_COPY_MEDIA, false);
        darkTheme = preferences.getBoolean(Settings.PREF_DARK_THEME, false);

        // Index page
        indexPage = preferences.getLong(Settings.PREF_INDEX_PAGE,
                DatePickerPreference.DEFAULT_VALUE);
        // Template page
        templatePage = preferences.getLong(Settings.PREF_TEMPLATE_PAGE,
                DatePickerPreference.DEFAULT_VALUE);
        // Folder
        folder = preferences.getString(Settings.PREF_FOLDER, DIARY);
    }

    // mediaCheck
    private void mediaCheck(Intent intent)
    {
        // Check for sent media
        if (Intent.ACTION_SEND.equals(intent.getAction()) ||
                Intent.ACTION_VIEW.equals(intent.getAction()) ||
                Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()))
        {
            haveMedia = true;
            goToDate(currEntry);
        }
    }

    // eventCheck
    private String eventCheck(CharSequence text)
    {
        Matcher matcher = EVENT_PATTERN.matcher(text);

        DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);

        // Find matches
        while (matcher.find())
        {
            // Parse time
            Date date;
            try
            {
                date = dateFormat.parse(matcher.group(1));
            }

            // Ignore errors
            catch (Exception e)
            {
                continue;
            }

            Calendar time = Calendar.getInstance();
            time.setTime(date);

            Calendar startTime =
                    new GregorianCalendar(currEntry.get(Calendar.YEAR),
                            currEntry.get(Calendar.MONTH),
                            currEntry.get(Calendar.DATE),
                            time.get(Calendar.HOUR_OF_DAY),
                            time.get(Calendar.MINUTE));
            Calendar endTime =
                    new GregorianCalendar(currEntry.get(Calendar.YEAR),
                            currEntry.get(Calendar.MONTH),
                            currEntry.get(Calendar.DATE),
                            time.get(Calendar.HOUR_OF_DAY),
                            time.get(Calendar.MINUTE));
            // Add an hour
            endTime.add(Calendar.HOUR, 1);

            String title = matcher.group(2);

            QueryHandler.insertEvent(this, startTime.getTimeInMillis(),
                    endTime.getTimeInMillis(), title);
        }

        return matcher.replaceAll(EVENT_TEMPLATE);
    }

    // loadMarkdown
    private void loadMarkdown()
    {
        CharSequence text = textView.getText();
        loadMarkdown(text);
    }

    // loadMarkdown
    private void loadMarkdown(CharSequence text)
    {
        markdownView.loadMarkdown(getBaseUrl(), markdownCheck(text),
                getStyles(), getScript());
    }

    // markdownCheck
    private String markdownCheck(CharSequence text)
    {
        // Date check
        text = dateCheck(text);

        // Check for map
        text = mapCheck(text);

        // Check for media
        return mediaCheck(text).toString();
    }

    // mediaCheck
    private CharSequence mediaCheck(CharSequence text)
    {
        StringBuffer buffer = new StringBuffer();

        Matcher matcher = MEDIA_PATTERN.matcher(text);

        // Find matches
        while (matcher.find())
        {
            File file = new File(matcher.group(2));
            String type = FileUtils.getMimeType(file);

            if (type == null)
            {
                Matcher geoMatcher = GEO_PATTERN.matcher(matcher.group(2));

                if (geoMatcher.matches())
                {
                    NumberFormat parser =
                            NumberFormat.getInstance(Locale.ENGLISH);

                    double lat;
                    double lng;

                    try
                    {
                        lat = parser.parse(geoMatcher.group(1)).doubleValue();
                        lng = parser.parse(geoMatcher.group(2)).doubleValue();
                    }

                    // Ignore parse error
                    catch (Exception e)
                    {
                        continue;
                    }

                    // Create replacement iframe
                    String replace =
                            String.format(Locale.ENGLISH, MAP_TEMPLATE,
                                    lng - 0.005, lat - 0.005,
                                    lng + 0.005, lat + 0.005,
                                    lat, lng);

                    // Append replacement
                    matcher.appendReplacement(buffer, replace);
                }
                else
                {
                }
            }
            else if (type.startsWith(IMAGE))
            {
                // Do nothing, handled by markdown view
            }
            else if (type.startsWith(AUDIO))
            {
                // Create replacement
                String replace =
                        String.format(AUDIO_TEMPLATE, matcher.group(2));

                // Append replacement
                matcher.appendReplacement(buffer, replace);
            }
            else if (type.startsWith(VIDEO))
            {
                // Create replacement
                String replace =
                        String.format(VIDEO_TEMPLATE, matcher.group(2));

                // Append replacement
                matcher.appendReplacement(buffer, replace);
            }
        }

        // Append rest of entry
        matcher.appendTail(buffer);

        return buffer;
    }

    // mapCheck
    private CharSequence mapCheck(CharSequence text)
    {
        StringBuffer buffer = new StringBuffer();

        Matcher matcher = MAP_PATTERN.matcher(text);

        // Find matches
        while (matcher.find())
        {
            double lat;
            double lng;

            try
            {
                lat = Double.parseDouble(matcher.group(1));
                lng = Double.parseDouble(matcher.group(2));
            }

            // Ignore parse error
            catch (Exception e)
            {
                continue;
            }

            // Create replacement iframe
            String replace =
                    String.format(Locale.ENGLISH, GEO_TEMPLATE,
                            lat, lng);

            // Substitute replacement
            matcher.appendReplacement(buffer, replace);
        }

        // Append rest of entry
        matcher.appendTail(buffer);

        return buffer;
    }

    // dateCheck
    private CharSequence dateCheck(CharSequence text)
    {
        StringBuffer buffer = new StringBuffer();
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);

        Matcher matcher = DATE_PATTERN.matcher(text);

        // Find matches
        while (matcher.find())
        {
            try
            {
                // Parse date
                Date date = dateFormat.parse(matcher.group(2));
                calendar.setTime(date);
            }

            // Ignore parse error
            catch (Exception e)
            {
                continue;
            }

            // Get file
            File file = getDay(calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DATE));

            // Get uri
            Uri uri = Uri.fromFile(file);

            // Create replacement
            String replace =
                    String.format(Locale.ROOT, LINK_TEMPLATE,
                            matcher.group(1), uri.toString());
            // Substitute replacement
            matcher.appendReplacement(buffer, replace);
        }

        // Append rest of entry
        matcher.appendTail(buffer);

        return buffer;
    }

    // addMedia
    private void addMedia(Intent intent)
    {
        String type = intent.getType();

        if (type == null)
        {
            // Get uri
            Uri uri = intent.getData();
            if (GEO.equalsIgnoreCase(uri.getScheme()))
                addMap(uri);
        }
        else if (type.equalsIgnoreCase(TEXT_PLAIN))
        {
            // Get the text
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);

            // Check text
            if (text != null)
            {
                // Check if it's an URL
                Uri uri = Uri.parse(text);
                if ((uri != null) && (uri.getScheme() != null) &&
                        (uri.getScheme().equalsIgnoreCase(HTTP) ||
                                uri.getScheme().equalsIgnoreCase(HTTPS)))
                    addLink(uri, intent.getStringExtra(Intent.EXTRA_TITLE),
                            true);
                else
                {
                    textView.append(text);
                    loadMarkdown();
                }
            }

            // Get uri
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

            // Check uri
            if (uri != null)
            {
                // Resolve content uri
                if (CONTENT.equalsIgnoreCase(uri.getScheme()))
                    uri = resolveContent(uri);

                addLink(uri, intent.getStringExtra(Intent.EXTRA_TITLE), true);
            }
        }
        else if (type.startsWith(IMAGE) ||
                type.startsWith(AUDIO) ||
                type.startsWith(VIDEO))
        {
            if (Intent.ACTION_SEND.equals(intent.getAction()))
            {
                // Get the media uri
                Uri media =
                        intent.getParcelableExtra(Intent.EXTRA_STREAM);

                // Resolve content uri
                if (CONTENT.equalsIgnoreCase(media.getScheme()))
                    media = resolveContent(media);

                // Attempt to get web uri
                String path = intent.getStringExtra(Intent.EXTRA_TEXT);

                if (path != null)
                {
                    // Try to get the path as an uri
                    Uri uri = Uri.parse(path);
                    // Check if it's an URL
                    if ((uri != null) &&
                            (HTTP.equalsIgnoreCase(uri.getScheme()) ||
                                    HTTPS.equalsIgnoreCase(uri.getScheme())))
                        media = uri;
                }

                addMedia(media, true);
            }
            else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()))
            {
                // Get the media
                ArrayList<Uri> media =
                        intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                for (Uri uri : media)
                {
                    // Resolve content uri
                    if (CONTENT.equalsIgnoreCase(uri.getScheme()))
                        uri = resolveContent(uri);

                    addMedia(uri, true);
                }
            }
        }

        // Reset the flag
        haveMedia = false;
    }

    // getBaseUrl
    private String getBaseUrl()
    {
        return Uri.fromFile(getCurrent()).toString() + File.separator;
    }

    // getCurrent
    private File getCurrent()
    {
        return getMonth(currEntry.get(Calendar.YEAR),
                currEntry.get(Calendar.MONTH));
    }

    // getStyles
    private String getStyles()
    {
        File cssFile = new File(getHome(), CSS_STYLES);

        if (cssFile.exists())
            return Uri.fromFile(cssFile).toString();

        return STYLES;
    }

    // getScript
    private String getScript()
    {
        File jsFile = new File(getHome(), JS_SCRIPT);

        if (jsFile.exists())
            return Uri.fromFile(jsFile).toString();

        return null;
    }

    // setVisibility
    private void setVisibility()
    {
        if (markdown)
        {
            buttonSwitcher.setVisibility(View.VISIBLE);
            // Check if shown
            if (shown)
            {
                layoutSwitcher.setDisplayedChild(MARKDOWN);
                buttonSwitcher.setDisplayedChild(EDIT);
            }
            else
            {
                layoutSwitcher.setDisplayedChild(EDIT_TEXT);
                buttonSwitcher.setDisplayedChild(ACCEPT);
            }
        }
        else
        {
            layoutSwitcher.setDisplayedChild(EDIT_TEXT);
            buttonSwitcher.setVisibility(View.GONE);
        }
    }

    // goToDate
    private void goToDate(Calendar date)
    {
        if (custom)
            showCustomCalendarDialog(date);

        else
            showDatePickerDialog(date);
    }

    // showCustomCalendarDialog
    private void showCustomCalendarDialog(Calendar date)
    {
        CustomCalendarDialog dialog = new
                CustomCalendarDialog(this, this,
                date.get(Calendar.YEAR),
                date.get(Calendar.MONTH),
                date.get(Calendar.DATE));
        // Show the dialog
        dialog.show();

        // Get the decorators
        List<DayDecorator> decorators = new ArrayList<DayDecorator>();
        decorators.add(new EntryDecorator());

        // Get the calendar
        CustomCalendarView calendarView = dialog.getCalendarView();

        // Set the decorators
        calendarView.setDecorators(decorators);

        // Refresh the calendar
        calendarView.refreshCalendar(date);
    }

    // showDatePickerDialog
    private void showDatePickerDialog(Calendar date)
    {
        DatePickerDialog dialog = new
                DatePickerDialog(this, this,
                date.get(Calendar.YEAR),
                date.get(Calendar.MONTH),
                date.get(Calendar.DATE));

        //Prevent future dates from being able to be selected (Kaitlin)
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        // Show the dialog
        dialog.show();
    }

    // findAll
    public void findAll()
    {
        // Get search string
        String search = searchView.getQuery().toString();

        // Execute find task
        FindTask findTask = new FindTask(this);
        findTask.execute(search);
    }

    // share
    @SuppressWarnings("deprecation")
    public void share()
    {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_SUBJECT,
                getString(R.string.appName) + ": " +
                        getTitle().toString());
        if (shown)
        {
            intent.setType(IMAGE_PNG);
            View v = markdownView.getRootView();
            v.setDrawingCacheEnabled(true);
            Bitmap bitmap = Bitmap.createBitmap(v.getDrawingCache());
            v.setDrawingCacheEnabled(false);

            File image = new File(getCacheDir(), DIARY_IMAGE);
            try (FileOutputStream out = new FileOutputStream(image))
            {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            }

            catch (Exception e) {}
            Uri imageUri = FileProvider
                    .getUriForFile(this, FILE_PROVIDER, image);
            intent.putExtra(Intent.EXTRA_STREAM, imageUri);
        }

        else
        {
            intent.setType(TEXT_PLAIN);
            intent.putExtra(Intent.EXTRA_TEXT, textView.getText().toString());
        }

        startActivity(Intent.createChooser(intent, null));
    }

    // addTime
    public void addTime()
    {
        DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT);
        String time = format.format(new Date());
        Editable editable = textView.getEditableText();
        int position = textView.getSelectionStart();
        editable.insert(position, time);
        loadMarkdown();
    }

    // addEvents
    public void addEvents()
    {
        GregorianCalendar endTime = new
                GregorianCalendar(currEntry.get(Calendar.YEAR),
                currEntry.get(Calendar.MONTH),
                currEntry.get(Calendar.DATE));
        endTime.add(Calendar.DATE, 1);
        QueryHandler.queryEvents(this, currEntry.getTimeInMillis(),
                endTime.getTimeInMillis(),
                (startTime, title) ->
                {
                    DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT);
                    String time = format.format(startTime);
                    String event = String.format(EVENTS_TEMPLATE, time, title);
                    Editable editable = textView.getEditableText();
                    int position = textView.getSelectionStart();
                    editable.insert(position, event);
                    loadMarkdown();
                });
    }

    // addMedia
    public void addMedia()
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(WILD_WILD);
        startActivityForResult(Intent.createChooser(intent, null), ADD_MEDIA);
    }


    // editStyles
    public void editStyles()
    {
        File file = new File(getHome(), CSS_STYLES);
        Uri uri = Uri.fromFile(file);
        startActivity(new Intent(Intent.ACTION_EDIT, uri, this, Editor.class));
    }

    // editScript
    public void editScript()
    {
        File file = new File(getHome(), JS_SCRIPT);
        Uri uri = Uri.fromFile(file);
        startActivity(new Intent(Intent.ACTION_EDIT, uri, this, Editor.class));
    }

    // backup
    public void backup()
    {
        ZipTask zipTask = new ZipTask(this);
        zipTask.execute();
    }

    // settings
    private void settings()
    {
        startActivity(new Intent(this, Settings.class));
    }

    // getHome
    private File getHome()
    {
        File file = new File(folder);
        if (file.isAbsolute() && file.isDirectory() && file.canWrite())
            return file;

        return new File(Environment.getExternalStorageDirectory(), folder);
    }

    // getYear
    private File getYear(int year)
    {
        return new File(getHome(), String.format(Locale.ENGLISH,
                "%04d", year));
    }

    // getMonth
    private File getMonth(int year, int month)
    {
        return new File(getYear(year), String.format(Locale.ENGLISH,
                "%02d", month + 1));
    }

    // getDay
    private File getDay(int year, int month, int day)
    {
        return new File(getMonth(year, month), String.format(Locale.ENGLISH,
                "%02d.txt", day));
    }

    // getFile
    private File getFile()
    {
        return getFile(currEntry);
    }

    // getFile
    private File getFile(Calendar entry)
    {
        return getDay(entry.get(Calendar.YEAR),
                entry.get(Calendar.MONTH),
                entry.get(Calendar.DATE));
    }

    // prevYear
    private int prevYear(int year)
    {
        int prev = -1;
        for (File yearDir : listYears(getHome()))
        {
            int n = yearValue(yearDir);
            if (n < year && n > prev)
                prev = n;
        }
        return prev;
    }

    // prevMonth
    private int prevMonth(int year, int month)
    {
        int prev = -1;
        for (File monthDir : listMonths(getYear(year)))
        {
            int n = monthValue(monthDir);
            if (n < month && n > prev)
                prev = n;
        }
        return prev;
    }

    // prevDay
    private int prevDay(int year, int month, int day)
    {
        int prev = -1;
        for (File dayFile : listDays(getMonth(year, month)))
        {
            int n = dayValue(dayFile);
            if (n < day && n > prev)
                prev = n;
        }
        return prev;
    }

    // getPrevEntry
    private Calendar getPrevEntry(int year, int month, int day)
    {
        int prev;
        if ((prev = prevDay(year, month, day)) == -1)
        {
            if ((prev = prevMonth(year, month)) == -1)
            {
                if ((prev = prevYear(year)) == -1)
                    return null;
                return getPrevEntry(prev, Calendar.DECEMBER, 32);
            }
            return getPrevEntry(year, prev, 32);
        }
        return new GregorianCalendar(year, month, prev);
    }

    // nextYear
    private int nextYear(int year)
    {
        int next = -1;
        for (File yearDir : listYears(getHome()))
        {
            int n = yearValue(yearDir);
            if (n > year && (next == -1 || n < next))
                next = n;
        }
        return next;
    }

    // nextMonth
    private int nextMonth(int year, int month)
    {
        int next = -1;
        for (File monthDir : listMonths(getYear(year)))
        {
            int n = monthValue(monthDir);
            if (n > month && (next == -1 || n < next))
                next = n;
        }
        return next;
    }

    // nextDay
    private int nextDay(int year, int month, int day)
    {
        int next = -1;
        for (File dayFile : listDays(getMonth(year, month)))
        {
            int n = dayValue(dayFile);
            if (n > day && (next == -1 || n < next))
                next = n;
        }
        return next;
    }

    // getNextEntry
    private Calendar getNextEntry(int year, int month, int day)
    {
        int next;
        if ((next = nextDay(year, month, day)) == -1)
        {
            if ((next = nextMonth(year, month)) == -1)
            {
                if ((next = nextYear(year)) == -1)
                    return null;
                return getNextEntry(next, Calendar.JANUARY, -1);
            }
            return getNextEntry(year, next, -1);
        }
        return new GregorianCalendar(year, month, next);
    }

    // diaryEntry
    private Calendar diaryEntry(String url)
    {
        // Get home folder
        String home = Uri.fromFile(getHome()).toString();

        // Check url
        if (!url.startsWith(home))
            return null;

        // Get uri
        Uri uri = Uri.parse(url);
        File file = new File(uri.getPath());

        // Check file
        if (!file.exists())
            return null;

        // Get segments
        List<String> segments = uri.getPathSegments();
        int size = segments.size();

        // Parse segments
        try
        {
            int day = Integer.parseInt(segments.get(--size).split("\\.")[0]);
            int month = Integer.parseInt(segments.get(--size)) - 1;
            int year = Integer.parseInt(segments.get(--size));

            return new GregorianCalendar(year, month, day);
        }

        catch (Exception e) {}

        return null;
    }

    // onRequestPermissionsResult
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults)
    {
        switch (requestCode)
        {
            case REQUEST_WRITE:
                for (int i = 0; i < grantResults.length; i++)
                    if (permissions[i].equals(Manifest.permission
                            .WRITE_EXTERNAL_STORAGE) &&
                            grantResults[i] == PackageManager.PERMISSION_GRANTED)
                        // Granted, save
                        save();
                break;

            case REQUEST_READ:
                for (int i = 0; i < grantResults.length; i++)
                    if (permissions[i].equals(Manifest.permission
                            .READ_EXTERNAL_STORAGE) &&
                            grantResults[i] == PackageManager.PERMISSION_GRANTED)
                        // Granted, load
                        load();
                break;

            case REQUEST_TEMPLATE:
                for (int i = 0; i < grantResults.length; i++)
                    if (permissions[i].equals(Manifest.permission
                            .READ_EXTERNAL_STORAGE) &&
                            grantResults[i] == PackageManager.PERMISSION_GRANTED)
                        // Granted, template
                        template();
                break;
            case REQUEST_CAMERA:
                for (int i = 0; i < grantResults.length; i++)
                    if (permissions[i].equals(Manifest.permission
                            .CAMERA) &&
                            grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        // Granted, template
                            open_camera();
                    }

                break;

        }
    }

    /**
     * Adapted from //adapted from https://www.youtube.com/watch?feature=youtu.be&v=VqgZxiU2knM&app=desktop - Andrew Cheung
     * @return
     */
    private File getImageFile() throws IOException
    {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageName = "jpg_" + timeStamp+"_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imagefile = File.createTempFile(imageName,".jpg",storageDir);
        currentImagePath = imagefile.getAbsolutePath();
        Log.i("image","made a file?");
        return imagefile;

    }



    // save
    private void save()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                        {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_CALENDAR,
                                Manifest.permission.READ_CALENDAR,Manifest.permission.CAMERA}, REQUEST_WRITE);

                return;
            }
        }

        if (currEntry != null)
        {
            CharSequence text = textView.getText();

            // Check for events
            text = eventCheck(text);

            // Check for maps
            text = mapCheck(text);

            // Check for cursor position
            text = positionCheck(text);

            // Save text
            save(text);
        }
    }

    // save
    private void save(CharSequence text)
    {
        File file = getFile();
        if (text.length() == 0)
        {
            if (file.exists())
                file.delete();
            File parent = file.getParentFile();
            if (parent.exists() && parent.list().length == 0)
            {
                parent.delete();
                File grandParent = parent.getParentFile();
                if (grandParent.exists()
                        && grandParent.list().length == 0)
                    grandParent.delete();
            }
        }
        else
        {
            file.getParentFile().mkdirs();
            try (FileWriter fileWriter = new FileWriter(file))
            {
                fileWriter.append(text);
            }

            catch (Exception e)
            {
                alertDialog(R.string.appName, e.getMessage(),
                        android.R.string.ok);

                e.printStackTrace();
            }
        }
    }

    // alertDialog
    private void alertDialog(int title, String message,
                             int neutralButton)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);

        // Add the button
        builder.setNeutralButton(neutralButton, null);

        // Create the AlertDialog
        builder.show();
    }

    // readAssetFile
    private CharSequence readAssetFile(String file)
    {
        try
        {
            // Open file
            try (InputStream input = getAssets().open(file))
            {
                BufferedReader bufferedReader =
                        new BufferedReader(new InputStreamReader(input));
                StringBuilder content =
                        new StringBuilder(input.available());
                String line;
                while ((line = bufferedReader.readLine()) != null)
                {
                    content.append(line);
                    content.append(System.getProperty("line.separator"));
                }

                changed = true;
                return content;
            }
        }

        catch (Exception e) {}

        return null;
    }

    // load
    private void load()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                        {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_CALENDAR,
                                Manifest.permission.READ_CALENDAR,Manifest.permission.CAMERA}, REQUEST_READ);

                return;
            }
        }

        CharSequence text = read(getFile());
        textView.setText(text);
        changed = false;
        if (markdown)
            loadMarkdown();

        if (text != null)
            checkPosition(text);


    }

    // checkPosition
    private void checkPosition(CharSequence text)
    {
        // Get a pattern and a matcher for position pattern
        Matcher matcher = POSN_PATTERN.matcher(text);
        // Check pattern
        if (matcher.find())
        {
            switch (matcher.group(1))
            {
                // Start
                case "<":
                    textView.setSelection(0);
                    break;

                // End
                case ">":
                    textView.setSelection(textView.length());
                    break;

                // Saved position
                case "#":
                    try
                    {
                        textView.setSelection(Integer.parseInt(matcher.group(2)));
                    }

                    catch (Exception e)
                    {
                        textView.setSelection(textView.length());
                    }
                    break;
            }
        }

        else
            textView.setSelection(textView.length());

        // Scroll after delay
        textView.postDelayed(() ->
        {
            // Get selection
            int selection = textView.getSelectionStart();

            // Get text position
            int line = textView.getLayout().getLineForOffset(selection);
            int position = textView.getLayout().getLineBaseline(line);

            // Scroll to it
            int height = scrollView.getHeight();
            scrollView.smoothScrollTo(0, position - height / 2);
        }, POSITION_DELAY);
    }

    // positionCheck
    private CharSequence positionCheck(CharSequence text)
    {
        // Get a pattern and a matcher for position pattern
        Matcher matcher = POSN_PATTERN.matcher(text);
        // Check pattern
        if (matcher.find())
        {
            // Save position
            if ("#".equals(matcher.group(1)))
            {
                // Create replacement
                String replace =
                        String.format(Locale.ROOT, POSN_TEMPLATE,
                                textView.getSelectionStart());
                return matcher.replaceFirst(replace);
            }
        }

        return text;
    }

    // setDate
    private void setDate(Calendar date)
    {
        setTitleDate(date.getTime());

        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH);
        int day = date.get(Calendar.DATE);

        Calendar calendar = Calendar.getInstance();
        Calendar today = new GregorianCalendar(calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DATE));

        prevEntry = getPrevEntry(year, month, day);
        if ((prevEntry == null || today.compareTo(prevEntry) > 0) &&
                today.compareTo(date) < 0)
            prevEntry = today;
        currEntry = date;
        nextEntry = getNextEntry(year, month, day);
        if ((nextEntry == null || today.compareTo(nextEntry) < 0) &&
                today.compareTo(date) > 0)
            nextEntry = today;

        invalidateOptionsMenu();


    }

    // setTitleDate
    private void setTitleDate(Date date)
    {
        Configuration config = getResources().getConfiguration();
        switch (config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
        {
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
                setTitle(DateFormat.getDateInstance(DateFormat.MEDIUM)
                        .format(date));
                break;

            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                switch (config.orientation)
                {
                    case Configuration.ORIENTATION_PORTRAIT:
                        setTitle(DateFormat.getDateInstance(DateFormat.MEDIUM)
                                .format(date));
                        break;

                    case Configuration.ORIENTATION_LANDSCAPE:
                        setTitle(DateFormat.getDateInstance(DateFormat.FULL)
                                .format(date));
                        break;
                }
                break;

            default:
                setTitle(DateFormat.getDateInstance(DateFormat.FULL)
                        .format(date));
                break;
        }
    }

    // changeDate
    private void changeDate(Calendar date)
    {
        if (changed)
            save();

        setDate(date);
        load();

        if (markdown && !shown)
        {
            animateAccept();
            shown = true;
        }

        entry = true;
    }

    // prevEntry
    private void prevEntry()
    {
        entryStack.push(currEntry);
        changeDate(prevEntry);
    }

    // nextEntry
    private void nextEntry()
    {
        entryStack.push(currEntry);
        changeDate(nextEntry);
    }

    // today
    private void today()
    {
        Calendar calendar = Calendar.getInstance();
        Calendar today = new GregorianCalendar(calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DATE));
        entryStack.clear();
        changeDate(today);

        // Check template
        if (useTemplate && textView.length() == 0)
            template();
    }

    /**
     * @author Clayton Darlington
     * @param theArray
     * @return
     *
     * Helper function to convert an array of files to a LinkedList of file paths for parsing
     */
    private LinkedList<String> convertFileArray(File[] theArray) {

        if(theArray == null) {
            return null;
        }

        LinkedList<String> theList = new LinkedList<>();
        for(File f : theArray) {
            theList.add(f.getAbsolutePath());
        }

        return theList;
    }

    /**
     * @author Clayton Darlington
     * @param filePath
     * @return
     *
     *  Helper function to covnert a file path to a human readable string
     */
    private String makeReadable(String filePath) {

        int info = filePath.indexOf("Diary");
        int end = filePath.length() - 4;

        String temp = filePath.substring(info + 6, end);
        temp.replaceAll("/","");

        return temp;
    }


    /**
     * @author Clayton Darlington
     * @param entry
     *
     *  This method takes an entry as a string, and creates a Calendar from the to load the associated entry
     */
    private GregorianCalendar makeCalendar(String entry) {

        //parse the entry string fro year month day
        String[] dateInfo = entry.split("/");
        int year = Integer.parseInt(dateInfo[0]);
        int month = Integer.parseInt(dateInfo[1]) - 1;
        int day = Integer.parseInt(dateInfo[2]);

        GregorianCalendar theCal = new GregorianCalendar(year, month, day);

        return theCal;

    }

    /**
     * @author Clayton Darlington
     *
     * Creates an alert dialog in response to a sorted list
     *
     */
    private void sortList(String[] theList) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Select an entry to open");

        //build the alert dialog
        builder.setItems(theList, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {

                //Load the selected Entry
                GregorianCalendar theCal = makeCalendar(theList[which]);
                changeDate(theCal);

                //temp toast for testing
                Toast toast = Toast.makeText(getApplicationContext(), "Changed date to: " + theList[which], Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    /**
     * @author Clayton Darlington
     *
     * Creates an alert dialog in response to a sorted list, contains additional details about each entry
     *
     */
    private void sortListDetail(String[] theList, String[] details) {

        String[] modifiedList = new String[theList.length];

        for(int i = 0; i < theList.length; i++) {
            if(details[i] != null) {
                String temp = theList[i].concat("  ");
                String temp2 = temp.concat(details[i]);
                modifiedList[i] = temp2;
            } else {
                modifiedList[i] = theList[i];
            }


        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Select an entry to open");

        //build the alert dialog
        builder.setItems(modifiedList, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {

                String x;
                //Load the selected Entry
                if(modifiedList[which].indexOf(" ") != -1) {
                    int regex = modifiedList[which].indexOf(" ");
                    x = modifiedList[which].substring(0, regex);
                } else {
                    x = modifiedList[which];
                }

                GregorianCalendar theCal = makeCalendar(x);
                changeDate(theCal);

                //temp toast for testing
                Toast toast = Toast.makeText(getApplicationContext(), "Changed date to: " + modifiedList[which], Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void lengthSort() {

        class Length {
            String name;
            String value;
        }

        File theFile = getFile();

        String searchFileName = "search_values.txt";

        File searchFile = new File(getHome(), searchFileName);

        //2 lists to hold the values for entry length
        ArrayList<Length> theList = new ArrayList<>();


        //get the length values for every entry
        if (searchFile.exists()) {
            try {
                InputStream inStream = openFileInput(searchFileName);
                InputStreamReader reader = new InputStreamReader(inStream);
                BufferedReader check = new BufferedReader(reader);

                //Search for Location in file (Borrowed heavily from Scarlett's implementation)
                String line;

                try {
                    //line by line
                    while ((line = check.readLine()) != null) {
                        try {
                            JSONObject checking = (JSONObject) new JSONTokener(line).nextValue();
                            Length alength = new Length();
                            alength.value = checking.getString("wordlength");

                            //get the values for wordLength here
                            String fName = checking.getString("name");
                            fName = fName.replace('_', '/');
                            fName = fName.concat(".txt");
                            Log.d("FNAME", fName);
                            alength.name = getHome().getPath().concat("/").concat(fName);
                            theList.add(alength);


                        } catch (JSONException e) {
                        }
                    }
                } catch (IOException e) {
                }
                String[] array = new String[theList.size()];
                //add all the values in order here
                for (int i = 0; i < theList.size(); i++) {
                    array[i] = theList.get(i).name;
                }

                Log.d("ARRAY", array.toString());
                String[] humanArray = new String[array.length];
                for(int i = 0; i < array.length; i++) {
                    humanArray[i] = makeReadable(array[i]);
                }
                sortList(humanArray);
            } catch (FileNotFoundException e) {
            }
        }
    }



    private void sortDateList(LinkedList<String> theList) {

        //parse each file and get the name
        Log.d("STRINGLIST", theList.toString());
    }

    /**
     * @author Clayton Darlington
     * @param file - the current entry
     *
     *             This method is used to generate a list of journal entries.
     *             The list is then sorted in descending order by date.
     */
    private void dateSort(File file) {

        /* get the Diary folder, it holds all the user entries */
        File theDiary = file.getParentFile().getParentFile().getParentFile();

        //Array list to store file names
        LinkedList<String> theList = new LinkedList<>();

        //if the Diary folder is a directory (Always should be), then check its contents
        if(theDiary.isDirectory()) {
            //get list of years in Diary
            File[] years = theDiary.listFiles();

            if(years != null) {
                for (File months : years) {
                    //should only contain directories here
                    if(months.isDirectory()) {
                        //get list of month directories
                        File[] month = months.listFiles();

                        //convert each file inside of the month directory
                        for(File days : month) {
                            //day contains each entry in a month
                            File[] day = days.listFiles();

                            for (File entry : day) {
                                //if the file is not the search file add it to the list
                                if(!entry.getName().contains("search"))
                                    //add the file to the List
                                    theList.add(entry.getAbsolutePath());
                            }

                        }

                    }
                }
            }

        }


        LinkedList<String> readableList = new LinkedList<>();

        //Convert the file paths to human readable strings
        for (String s : theList) {
            readableList.add(makeReadable(s));
        }

        // Apply ascending sort to the list
        Collections.sort(readableList);

        int size = readableList.size();

        String[] sortedList = new String[size];

        for(int j = 0; j < readableList.size(); j++) {
            sortedList[j] = readableList.get(j);
        }

        //build and display the GUI element for sort results
        sortList(sortedList);
    }


    /**
     * @author Kaitlin Venneri
     *
     * This method is used to retrieve the map of mood values and their associated mood image names
     *
     * @return map of moods as keys and image names as values
     */
    private HashMap<String, String> getMoodMap(){
        HashMap<String, String> moodMap = new HashMap<>();

        moodMap.put("Happy", "happy");
        moodMap.put("Sad", "sad");
        moodMap.put("Stressed", "stressed");
        moodMap.put("Remove Mood", "happy");

        return moodMap;
    }

    /**
     * @author Kaitlin Venneri
     *
     * This method is used to retrieve the map of location values and their associated location image names
     *
     * @return map of locations as keys and image names as values
     */
    private HashMap<String, String> getLocationMap(){
        HashMap<String, String> locationMap = new HashMap<>();

        locationMap.put("Home", "home");
        locationMap.put("School", "school");
        locationMap.put("Work", "briefcase");
        locationMap.put("Recreational Area", "recreation");
        locationMap.put("Remove Location", "home");

        return locationMap;
    }

    /**
     * @authors Kaitlin
     *
     * Function to update the UI components associated with the attribute and value
     *
     * @param attribute to update visuals for
     * @param value to update visuals with
     */
    private void updateVisualsForAttribute(String attribute, String value){
        ImageButton buttonToChange;
        ImageView plusToChange;
        HashMap<String,String> attributeMap;

        if(attribute.equals("mood")){
            buttonToChange = moodBtn;
            plusToChange = findViewById(R.id.mood_plus);
            attributeMap = getMoodMap();
        } else {
            //location assumed at this point
            buttonToChange = locationBtn;
            plusToChange = findViewById(R.id.location_plus);
            attributeMap = getLocationMap();
        }

        //set the image based on the attribute value selected (Kaitlin)
        String imageName = attributeMap.get(value);
        int imageResID = getResources().getIdentifier(imageName, "drawable", getPackageName());
        buttonToChange.setImageResource(imageResID);

        //Either show or remove the plus to the button, depending on the assignment
        if(value.contains("Remove")){
            plusToChange.setVisibility(View.VISIBLE);
        } else {
            plusToChange.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * @authors Kaitlin & Scarlett
     *
     * Function to assign a value to an attribute in the save file for the current entry
     *
     * @param attribute to assign a value to in the save file for the current entry
     * @param value to assign to the attribute in the save file for the current entry
     */
    private void assignValueToAttribute(String attribute, String value){
        //Create the name of the search file (Scarlett)
        String searchFilename = "search_values.txt";

        //Create the searchFile object (Scarlett)
        File searchFile = new File(getHome(), searchFilename);
        String addName = getSearchName();

        if(searchFile.exists()){
            try {
                //Open file to make sure it doesn't already have mood (Scarlett)
                InputStream inStream = openFileInput(searchFilename);
                InputStreamReader inRead = new InputStreamReader(inStream);
                BufferedReader check = new BufferedReader(inRead);

                //Search for mood in file (Scarlett)
                String line;
                boolean entryExists = false;
                JSONObject toModify = null;
                ArrayList<String> lines = new ArrayList<>();
                int index = 0;

                try {
                    //goes line by line
                    while ((line = check.readLine()) != null) {
                        lines.add(line);
                        //turns the string into an object
                        JSONObject checking = (JSONObject) new JSONTokener(line).nextValue();
                        //checks if the name of the file is right
                        if (checking.getString("name").equals(addName)) {
                            entryExists = true;
                            toModify = checking;
                            index = lines.size() - 1;
                        }
                    }
                } catch (IOException e) {}
                catch (JSONException e) {}

                //if mood not in file, add it (Scarlett)
                //if the entry for the file exists but doesn't have a mood
                if (entryExists) {
                    //modify the json
                    if (toModify != null) {
                        try {
                            if(value.contains("Remove")){
                                toModify.remove(attribute);
                            } else {
                                toModify.put(attribute, value);
                            }
                        } catch (JSONException e) {}
                    }
                    //remove the old line and add the new line
                    lines.add(toModify.toString());
                    lines.remove(index);
                    try {
                        //open the file
                        FileOutputStream search = openFileOutput(searchFile.getName(), MODE_PRIVATE);
                        OutputStreamWriter output = new OutputStreamWriter(search);
                        //write the first line to the file again
                        output.write(lines.get(0));
                        output.append("\n");
                        //append the remaining lines to the file
                        for (int i = 1; i < lines.size(); i++) {
                            output.append(lines.get(i));
                            output.append("\n");
                        }
                        output.close();
                    } catch (FileNotFoundException e) {}
                    catch (IOException e) {}
                }
                //if the entry for the file doesn't exist
                if (!entryExists) {
                    try {
                        FileOutputStream search = openFileOutput(searchFile.getName(), MODE_APPEND);
                        OutputStreamWriter output = new OutputStreamWriter(search);
                        output.append("{ \"name\" : \""+addName+"\" , \""+attribute+"\" : \""+value+"\" }\n");
                        output.close();
                    } catch (FileNotFoundException e) {}
                    catch (IOException e) {}
                }
                try {
                    check.close();
                } catch (IOException e) {}
            } catch (FileNotFoundException e) {}

        } else {
            //Create the file & create the search file (Kaitlin)
            try {
                searchFile.createNewFile();
            } catch (IOException e) {}

            //Append the mood to the search file (Scarlett)
            try {
                FileOutputStream search = openFileOutput(searchFile.getName(), MODE_APPEND);
                OutputStreamWriter output = new OutputStreamWriter(search);
                output.append("{ \"name\" : \""+addName+"\" , \""+attribute+"\" : \""+value+"\" }\n");
                output.close();
            } catch (FileNotFoundException e) {}
            catch (IOException e) {}
        }
    }

    /**
     * @authors Clayton, Kaitlin, Scarlett
     *
     * Function to check if the attribute has an associated value for the current entry
     * @param attribute to search for
     * @return true if entry has attribute, false otherwise
     */
    private Boolean hasAttribute(String attribute) {
        Boolean attributeFound = false;

        //Create the name of the search file and the file reference
        String searchFilename = "search_values.txt";
        File searchFile = new File(getHome(), searchFilename);
        String addName = getSearchName();

        //check if the searchFile exists
        if (searchFile.exists()) {
            try {
                //Open file (Scarlett)
                InputStream inStream = openFileInput(searchFilename);
                InputStreamReader inRead = new InputStreamReader(inStream);
                BufferedReader check = new BufferedReader(inRead);

                //Search for attribute in file (Scarlett)
                String line;

                try {
                    //goes line by line
                    while ((line = check.readLine()) != null) {
                        //turns the string into an object
                        JSONObject checking = (JSONObject) new JSONTokener(line).nextValue();
                        //checks if the name of the file is right
                        if (checking.getString("name").equals(addName)) {
                            //checks if it has the attribute
                            if (!checking.getString(attribute).isEmpty()) {
                                attributeFound = true;

                                check.close();
                            }
                        }
                    }
                } catch (IOException e) {}
                catch (JSONException e) {}

                try {
                    check.close();
                } catch (IOException e) {}
            } catch (FileNotFoundException e) {}

        }
        return attributeFound;
    }

    /**
     * @authors Clayton, Kaitlin, Scarlett
     *
     * Function to get the string value associated with the attribute
     * @param attribute to search for
     * @return value for attribute or "none" if not found
     */
    private String getValueForAttribute(String attribute) {
        String attributeValue = "none";

        //Create the name of the search file and the file reference
        String searchFilename = "search_values.txt";
        File searchFile = new File(getHome(), searchFilename);
        String addName = getSearchName();

        //check if the searchFile exists
        if (searchFile.exists()) {
            try {
                //Open file (Scarlett)
                InputStream inStream = openFileInput(searchFilename);
                InputStreamReader inRead = new InputStreamReader(inStream);
                BufferedReader check = new BufferedReader(inRead);

                //Search for attribute in file (Scarlett)
                String line;

                try {
                    //goes line by line
                    while ((line = check.readLine()) != null) {
                        //turns the string into an object
                        JSONObject checking = (JSONObject) new JSONTokener(line).nextValue();
                        //checks if the name of the file is in the search file
                        if (checking.getString("name").equals(addName)) {
                            //checks if it has the attribute
                            if (!checking.getString(attribute).isEmpty()) {
                                attributeValue = checking.getString(attribute);

                                check.close();
                            }
                        }
                    }
                } catch (IOException e) {}
                catch (JSONException e) {}

                try {
                    check.close();
                } catch (IOException e) {}
            } catch (FileNotFoundException e) {}

        }
        return attributeValue;
    }
    /**
     * @authors Scarlett, Kaitlin
     * @param value
     *
     *
     */
    public void assignWordLength(String value) {
        File file = getFile();

        //Create the name of the search file (Scarlett)
        String searchFilename = "search_values.txt";


        //Create the searchFile object (Scarlett)
        File searchFile = new File(getHome(), searchFilename);
        String addName = getSearchName();

        if (searchFile.exists()) {
            //Here is where we would just create a search file from file that exists (Kaitlin)
            try {
                //Open file to make sure it doesn't already have mood (Scarlett)
                InputStream inStream = openFileInput(searchFilename);
                InputStreamReader inRead = new InputStreamReader(inStream);
                BufferedReader check = new BufferedReader(inRead);

                //Search for wordlength in file (Scarlett)
                String line;
                boolean entryExists = false;
                JSONObject toModify = null;
                ArrayList<String> lines = new ArrayList<>();
                int index = 0;

                try {
                    //goes line by line
                    while ((line = check.readLine()) != null) {
                        lines.add(line);
                        //turns the string into an object
                        JSONObject checking = (JSONObject) new JSONTokener(line).nextValue();
                        //checks if the name of the file is right
                        if (checking.getString("name").equals(addName)) {
                            entryExists = true;
                            toModify = checking;
                            index = lines.size() - 1;
                        }
                    }
                } catch (IOException e) {
                } catch (JSONException e) {
                }

                //if mood not in file, add it (Scarlett)
                //if the entry for the file exists but doesn't have a wordlength
                if (entryExists) {
                    //modify the json
                    if (toModify != null) {
                        try {
                            toModify.put("wordlength", value);
                        } catch (JSONException e) {
                        }
                    }
                    //remove the old line and add the new line
                    lines.add(toModify.toString());
                    lines.remove(index);
                    try {
                        //open the file
                        FileOutputStream search = openFileOutput(searchFile.getName(), MODE_PRIVATE);
                        OutputStreamWriter output = new OutputStreamWriter(search);
                        //write the first line to the file again
                        output.write(lines.get(0));
                        output.append("\n");
                        //append the remaining lines to the file
                        for (int i = 1; i < lines.size(); i++) {
                            output.append(lines.get(i));
                            output.append("\n");
                        }
                        output.close();
                    } catch (FileNotFoundException e) {
                    } catch (IOException e) {
                    }
                }
                //if the entry for the file doesn't exist
                if (!entryExists) {
                    try {
                        FileOutputStream search = openFileOutput(searchFile.getName(), MODE_APPEND);
                        OutputStreamWriter output = new OutputStreamWriter(search);
                        output.append("{ \"name\" : \"" + addName + "\" , \"wordlength\" : \"" + value + "\" }\n");
                        output.close();
                    } catch (FileNotFoundException e) {
                    } catch (IOException e) {
                    }
                }
                try {
                    check.close();
                } catch (IOException e) {
                }
            } catch (FileNotFoundException e) {
            }

        } else {
            //Create the file & create the search file (Kaitlin)
            try {
                searchFile.createNewFile();
            } catch (IOException e) {
            }

            //Append the mood to the search file (Scarlett)
            try {
                FileOutputStream search = openFileOutput(searchFile.getName(), MODE_APPEND);
                OutputStreamWriter output = new OutputStreamWriter(search);
                output.append("{ \"name\" : \"" + addName + "\" , \"wordlength\" : \"" + value + "\" }\n");
                output.close();
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }
        }
    }


    /**
     * @author Kaitlin
     *
     *
     */
    public void toggleFavourite()
    {
        //Create the name of the search file (Scarlett)
        String searchFilename = "search_values.txt";

        ImageButton btn = findViewById(R.id.favourite);

        if(isFavourite()){
            btn.setImageResource(R.drawable.unfilled_star);
        } else {
            btn.setImageResource(R.drawable.filled_star);
        }

        //Create the searchFile object (Scarlett)
        File searchFile = new File(getHome(), searchFilename);
        String addName = getSearchName();

        if(searchFile.exists()){
            //Here is where we would just create a search file from file that exists (Kaitlin)
            try {
                //Open file to check if it has been favourited or not (Scarlett)
                InputStream inStream = openFileInput(searchFilename);
                InputStreamReader inRead = new InputStreamReader(inStream);
                BufferedReader check = new BufferedReader(inRead);

                //Search to check if entry is favourited in file (Scarlett)
                String line;
                boolean entryExists = false;
                JSONObject toModify = null;
                ArrayList<String> lines = new ArrayList<>();
                int index = 0;

                try {
                    //goes line by line
                    while ((line = check.readLine()) != null) {
                        lines.add(line);
                        //turns the string into an object
                        JSONObject checking = (JSONObject) new JSONTokener(line).nextValue();
                        //checks if the name of the file is right
                        if (checking.getString("name").equals(addName)) {
                            entryExists = true;
                            toModify = checking;
                            index = lines.size() - 1;
                        }
                    }
                } catch (IOException e) {}
                catch (JSONException e) {}

                //if favourited not in file, add it (Scarlett)
                //if the entry for the file exists but doesn't have a favourite attribute
                if (entryExists) {
                    //modify the json
                    if (toModify != null) {
                        try {
                            if(isFavourite()) {
                                //should no longer be a favourite
                                toModify.remove("favourite");
                            } else {
                                toModify.put("favourite", "true");
                            }
                        } catch (JSONException e) {}
                    }
                    //remove the old line and add the new line
                    lines.add(toModify.toString());
                    lines.remove(index);
                    try {
                        //open the file
                        FileOutputStream search = openFileOutput(searchFile.getName(), MODE_PRIVATE);
                        OutputStreamWriter output = new OutputStreamWriter(search);
                        //write the first line to the file again
                        output.write(lines.get(0));
                        output.append("\n");
                        //append the remaining lines to the file
                        for (int i = 1; i < lines.size(); i++) {
                            output.append(lines.get(i));
                            output.append("\n");
                        }
                        output.close();
                    } catch (FileNotFoundException e) {}
                    catch (IOException e) {}
                }
                //if the entry for the file doesn't exist
                if (!entryExists) {
                    try {
                        FileOutputStream search = openFileOutput(searchFile.getName(), MODE_APPEND);
                        OutputStreamWriter output = new OutputStreamWriter(search);
                        output.append("{ \"name\" : \""+addName+"\" , \"favourite\" : \""+"true"+"\" }\n");
                        output.close();
                    } catch (FileNotFoundException e) {}
                    catch (IOException e) {}
                }
                try {
                    check.close();
                } catch (IOException e) {}
            } catch (FileNotFoundException e) {}

        } else {
            //Create the file & create the search file (Kaitlin)
            try {
                searchFile.createNewFile();
            } catch (IOException e) {}

            //Append the favourite attribute to the search file (Scarlett)
            try {
                FileOutputStream search = openFileOutput(searchFile.getName(), MODE_APPEND);
                OutputStreamWriter output = new OutputStreamWriter(search);
                output.append("{ \"name\" : \""+addName+"\" , \"favourite\" : \""+"true"+"\" }\n");
                output.close();
            } catch (FileNotFoundException e) {}
            catch (IOException e) {}
        }
    }

    public boolean isFavourite(){
        boolean favourited = false;

        //Create the name of the search file and the file reference
        String searchFilename = "search_values.txt";
        File searchFile = new File(getHome(), searchFilename);
        String addName = getSearchName();

        //check if the searchFile exists
        if (searchFile.exists()) {

            try {
                //Open file to check if entry is favourited (Scarlett)
                InputStream inStream = openFileInput(searchFilename);
                InputStreamReader inRead = new InputStreamReader(inStream);
                BufferedReader check = new BufferedReader(inRead);

                //Search for if entry is favourited in file (Scarlett)
                String line;

                try {
                    //goes line by line
                    while ((line = check.readLine()) != null) {
                        //turns the string into an object
                        JSONObject checking = (JSONObject) new JSONTokener(line).nextValue();
                        //checks if the name of the file is right
                        if (checking.getString("name").equals(addName)) {
                            //checks if it is favourited
                            if (!checking.getString("favourite").isEmpty()) {
                                favourited = true;

                                check.close();
                            }
                        }
                    }
                } catch (IOException e) {}
                catch (JSONException e) {}

                try {
                    check.close();
                } catch (IOException e) {}
            } catch (FileNotFoundException e) {}

        }

        return favourited;
    }

    /**
     * @author Kaitlin
     *
     * Creates an alert dialog for attribute options and handles user input
     *
     */
    private void showAttributeOptions(String title, String attributeName, String[] theList) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(title);

        //build the alert dialog
        builder.setItems(theList, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int selected) {
                assignValueToAttribute(attributeName,theList[selected]);
                updateVisualsForAttribute(attributeName,theList[selected]);
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * @authors Mahjabeen & Andrew
     * @param
     *
     *
     */
    private void open_camera(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                        {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_CALENDAR,
                                Manifest.permission.CAMERA,
                                Manifest.permission.READ_CALENDAR}, REQUEST_CAMERA);

                return;
            }

            String fileName = System.currentTimeMillis()+".jpg";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, fileName);
            fileUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
            startActivityForResult(intent, image_request);
        }
    }

    /**
     * @author Scarlett Bootsma
     *
     */
    private String getSearchName() {
        int day = currEntry.get(Calendar.DATE);
        int month = currEntry.get(Calendar.MONTH) + 1;
        int year = currEntry.get(Calendar.YEAR);

        return (year) + "_" + (month) + "_" + (day);
    }

    private void sortMood(String mood) {
        //Create the name of the search file (Scarlett)
        String searchFilename = "search_values.txt";

        File searchFile = new File(getHome(), searchFilename);

        ArrayList<File> sorted = new ArrayList<>();

        if(searchFile.exists()) {

            try {

                InputStream inStream = openFileInput(searchFilename);
                InputStreamReader inRead = new InputStreamReader(inStream);
                BufferedReader check = new BufferedReader(inRead);

                //Search for Location in file (Scarlett)
                String line;
                ArrayList<File> happy = new ArrayList<>();
                ArrayList<File> sad = new ArrayList<>();
                ArrayList<File> stressed = new ArrayList<>();
                String fileName;

                try {
                    //goes line by line
                    while ((line = check.readLine()) != null) {
                        try {

                            //turns the string into an object
                            JSONObject checking = (JSONObject) new JSONTokener(line).nextValue();

                            //adds filenames to the list of moods that the file has

                            if (checking.getString("mood").equals("Happy")) {

                                fileName = checking.getString("name");
                                fileName = fileName.replace("_", "/");
                                fileName = fileName.concat(".txt");
                                fileName = getHome().getPath().concat("/").concat(fileName);
                                File adding = new File(fileName);
                                happy.add(adding);

                            } else if (checking.getString("mood").equals("Sad")) {

                                fileName = checking.getString("name");
                                fileName = fileName.replace("_", "/");
                                fileName = fileName.concat(".txt");
                                fileName = getHome().getPath().concat("/").concat(fileName);
                                File adding = new File(fileName);
                                sad.add(adding);

                            } else if (checking.getString("mood").equals("Stressed")) {

                                fileName = checking.getString("name");
                                fileName = fileName.replace("_", "/");
                                fileName = fileName.concat(".txt");
                                fileName = getHome().getPath().concat("/").concat(fileName);
                                File adding = new File(fileName);
                                stressed.add(adding);

                            }
                        } catch (JSONException e) {}
                    }
                } catch (IOException e) {}

                //add all values to the sorted list in the correct order
                if (mood.equals("happy")) {
                    sorted.addAll(happy);
                    sorted.addAll(sad);
                    sorted.addAll(stressed);
                } else if (mood.equals("sad")) {
                    sorted.addAll(sad);
                    sorted.addAll(happy);
                    sorted.addAll(stressed);
                } else if (mood.equals("stressed")) {
                    sorted.addAll(stressed);
                    sorted.addAll(happy);
                    sorted.addAll(sad);
                }

                sorted.trimToSize();

                String[] toShow = new String[sorted.size()];
                String[] values = new String[sorted.size()];

                for (int i = 0; i < sorted.size(); i++) {
                    toShow[i] = makeReadable(sorted.get(i).getPath());
                }
                if(mood.equals("happy")) {
                    int i;
                    for(i = 0; i < happy.size(); i++) {
                        values[i] = "Happy";
                    }
                    for(int j = 0; j < sad.size(); j++) {
                        values[i] = "Sad";
                        i++;
                    }
                    for(int g = 0; g < stressed.size(); g++) {
                        values[i] = "Stressed";
                        i++;
                    }
                } else if (mood.equals("sad")) {
                    int i;

                    for(i = 0; i < sad.size(); i++) {
                        values[i] = "Sad";
                    }
                    for(int j = 0; j < happy.size(); j++) {
                        values[i] = "Happy";
                        i++;
                    }
                    for(int g = 0; g < stressed.size(); g++) {
                        values[i] = "Stressed";
                        i++;
                    }
                } else if (mood.equals("stressed")){
                    int i;
                    for(i = 0; i < stressed.size(); i++) {
                        values[i] = "Stressed";
                    }
                    for(int j = 0; j < happy.size(); j++) {
                        values[i] = "Happy";
                        i++;
                    }
                    for(int g = 0; g < sad.size(); g++) {
                        values[i] = "Sad";
                        i++;
                    }
                }

                //send to gui here
                sortListDetail(toShow, values);

            } catch (FileNotFoundException e) {}
        }
    }

    private void sortFavourite() {
        //Create the name of the search file (Scarlett)
        String searchFilename = "search_values.txt";

        File searchFile = new File(getHome(), searchFilename);

        ArrayList<File> sorted = new ArrayList<>();

        if(searchFile.exists()) {

            try {

                InputStream inStream = openFileInput(searchFilename);
                InputStreamReader inRead = new InputStreamReader(inStream);
                BufferedReader check = new BufferedReader(inRead);

                //Search for Location in file (Scarlett)
                String line;
                ArrayList<File> favourite = new ArrayList<>();
                String fileName;

                try {
                    //goes line by line
                    while ((line = check.readLine()) != null) {
                        try {

                            //turns the string into an object
                            JSONObject checking = (JSONObject) new JSONTokener(line).nextValue();

                            //adds filenames to the list of moods that the file has

                            if (!checking.getString("favourite").isEmpty()) {

                                fileName = checking.getString("name");
                                fileName = fileName.replace("_", "/");
                                fileName = fileName.concat(".txt");
                                fileName = getHome().getPath().concat("/").concat(fileName);
                                File adding = new File(fileName);
                                favourite.add(adding);

                            }
                        } catch (JSONException e) {}
                    }
                } catch (IOException e) {}
                //add all values to the sorted list in the correct order
                sorted.addAll(favourite);

                sorted.trimToSize();

                String[] toShow = new String[sorted.size()];
                for (int i = 0; i < sorted.size(); i++) {
                    toShow[i] = makeReadable(sorted.get(i).getPath());
                }

                Arrays.sort(toShow, Collections.reverseOrder());

                //send to gui here
                sortList(toShow);

            } catch (FileNotFoundException e) {}
        }
    }

    private void sortLocation(String Location) {
        //Create the name of the search file (Scarlett)
        String searchFilename = "search_values.txt";

        File searchFile = new File(getHome(), searchFilename);

        ArrayList<File> sorted = new ArrayList<>();

        if(searchFile.exists()) {

            try {

                InputStream inStream = openFileInput(searchFilename);
                InputStreamReader inRead = new InputStreamReader(inStream);
                BufferedReader check = new BufferedReader(inRead);

                //Search for Location in file (Scarlett)
                String line;
                ArrayList<File> home = new ArrayList<>();
                ArrayList<File> work = new ArrayList<>();
                ArrayList<File> school = new ArrayList<>();
                ArrayList<File> rec = new ArrayList<>();
                String fileName;

                try {
                    //goes line by line
                    while ((line = check.readLine()) != null) {
                        try {

                            //turns the string into an object
                            JSONObject checking = (JSONObject) new JSONTokener(line).nextValue();

                            //adds filenames to the list of moods that the file has

                            if (checking.getString("location").equals("Home")) {

                                fileName = checking.getString("name");
                                fileName = fileName.replace("_", "/");
                                fileName = fileName.concat(".txt");
                                fileName = getHome().getPath().concat("/").concat(fileName);
                                File adding = new File(fileName);
                                home.add(adding);

                            } else if (checking.getString("location").equals("Work")) {

                                fileName = checking.getString("name");
                                fileName = fileName.replace("_", "/");
                                fileName = fileName.concat(".txt");
                                fileName = getHome().getPath().concat("/").concat(fileName);
                                File adding = new File(fileName);
                                work.add(adding);

                            } else if (checking.getString("location").equals("School")) {

                                fileName = checking.getString("name");
                                fileName = fileName.replace("_", "/");
                                fileName = fileName.concat(".txt");
                                fileName = getHome().getPath().concat("/").concat(fileName);
                                File adding = new File(fileName);
                                school.add(adding);

                            } else if (checking.getString("location").equals("Recreational Area")) {

                                fileName = checking.getString("name");
                                fileName = fileName.replace("_", "/");
                                fileName = fileName.concat(".txt");
                                fileName = getHome().getPath().concat("/").concat(fileName);
                                File adding = new File(fileName);
                                rec.add(adding);

                            }
                        } catch (JSONException e) {}
                    }
                } catch (IOException e) {}

                //add all values to the sorted list in the correct order
                if (Location.equals("Home")) {
                    sorted.addAll(home);
                    sorted.addAll(work);
                    sorted.addAll(school);
                    sorted.addAll(rec);
                } else if (Location.equals("Work")) {
                    sorted.addAll(work);
                    sorted.addAll(home);
                    sorted.addAll(school);
                    sorted.addAll(rec);
                } else if (Location.equals("School")) {
                    sorted.addAll(school);
                    sorted.addAll(home);
                    sorted.addAll(work);
                    sorted.addAll(rec);
                } else if (Location.equals("Recreational Area")) {
                    sorted.addAll(rec);
                    sorted.addAll(school);
                    sorted.addAll(home);
                    sorted.addAll(work);
                }

                sorted.trimToSize();

                String[] toShow = new String[sorted.size()];
                String[] values = new String[sorted.size()];

                for (int i = 0; i < sorted.size(); i++) {
                    toShow[i] = makeReadable(sorted.get(i).getPath());
                }

                if(Location.equals("Home")) {
                    int i;
                    for(i = 0; i < home.size(); i++) {
                        values[i] = "Home";
                    }
                    for(int j = 0; j < work.size(); j++) {
                        values[i] = "Sad";
                        i++;
                    }
                    for(int g = 0; g < school.size(); g++) {
                        values[i] = "Stressed";
                        i++;
                    }
                    for(int h = 0; h < rec.size(); h++) {
                        values[i] = "Recreational";
                        i++;
                    }
                } else if (Location.equals("Work")) {
                    int i;
                    for(i = 0; i < work.size(); i++) {
                        values[i] = "Work";
                    }
                    for(int j = 0; j < home.size(); j++) {
                        values[i] = "Home";
                        i++;
                    }
                    for(int g = 0; g < school.size(); g++) {
                        values[i] = "School";
                        i++;
                    }
                    for(int h = 0; h < rec.size(); h++) {
                        values[i] = "Recreational";
                        i++;
                    }
                } else if (Location.equals("School")){
                    int i;
                    for(i = 0; i < school.size(); i++) {
                        values[i] = "School";
                    }
                    for(int j = 0; j < home.size(); j++) {
                        values[i] = "Home";
                        i++;
                    }
                    for(int g = 0; g < work.size(); g++) {
                        values[i] = "Work";
                        i++;
                    }
                    for(int h = 0; h < rec.size(); h++) {
                        values[i] = "Recreational";
                        i++;
                    }
                } else if (Location.equals("Recreational Area")) {
                    int i;
                    for(i = 0; i < rec.size(); i++) {
                        values[i] = "Recreational";
                    }
                    for(int j = 0; j < school.size(); j++) {
                        values[i] = "School";
                    }
                    for(int g = 0; g < home.size(); g++) {
                        values[i] = "Home";
                    }
                    for(int h = 0; h < work.size(); h++) {
                        values[i] = "Work";
                    }
                }


                //send to gui here
                sortListDetail(toShow, values);

            } catch (FileNotFoundException e) {}
        }
    }

    // index
    private void index()
    {
        entryStack.clear();
        Calendar index = Calendar.getInstance();
        index.setTimeInMillis(indexPage);
        changeDate(index);
    }

    // template
    private void template()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]
                        {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_CALENDAR,
                                Manifest.permission.READ_CALENDAR,Manifest.permission.CAMERA}, REQUEST_TEMPLATE);

                return;
            }
        }

        Calendar template = Calendar.getInstance();
        template.setTimeInMillis(templatePage);
        CharSequence text = read(getFile(template));
        textView.setText(text);

        if (markdown)
            loadMarkdown();
    }

    // addMedia
    private void addMedia(Uri media, boolean append)
    {
        String name = media.getLastPathSegment();
        // Copy media file to diary folder
        // TODO: as for now, only for images because video and audio
        // are too time-consuming to be copied on the main thread
        if (copyMedia)
        {
            // Get type
            String type = FileUtils.getMimeType(this, media);
            if (type != null && type.startsWith(IMAGE))
            {
                File newMedia = new
                        File(getCurrent(), UUID.randomUUID().toString() +
                        FileUtils.getExtension(media.toString()));
                File oldMedia = FileUtils.getFile(this, media);
                try
                {
                    FileUtils.copyFile(oldMedia, newMedia);
                    String newName =
                            Uri.fromFile(newMedia).getLastPathSegment();
                    media = Uri.parse(newName);
                }

                catch (Exception e) {}
            }
        }

        String mediaText = String.format(MEDIA_TEMPLATE,
                name,
                media.toString());
        if (append)
            textView.append(mediaText);

        else
        {
            Editable editable = textView.getEditableText();
            int position = textView.getSelectionStart();
            editable.insert(position, mediaText);
        }

        loadMarkdown();
    }

    // addLink
    private void addLink(Uri uri, String title, boolean append)
    {
        if ((title == null) || (title.length() == 0))
            title = uri.getLastPathSegment();

        String url = uri.toString();
        String linkText = String.format(LINK_TEMPLATE, title, url);

        if (append)
            textView.append(linkText);

        else
        {
            Editable editable = textView.getEditableText();
            int position = textView.getSelectionStart();
            editable.insert(position, linkText);
        }

        loadMarkdown();
    }

    // addMap
    private void addMap(Uri uri)
    {
        String mapText = String.format(MEDIA_TEMPLATE,
                OSM,
                uri.toString());
        if (true)
            textView.append(mapText);

        else
        {
            Editable editable = textView.getEditableText();
            int position = textView.getSelectionStart();
            editable.insert(position, mapText);
        }

        loadMarkdown();
    }

    // resolveContent
    private Uri resolveContent(Uri uri)
    {
        String path = FileUtils.getPath(this, uri);

        if (path != null)
        {
            File file = new File(path);
            if (file.canRead())
                uri = Uri.fromFile(file);
        }

        return uri;
    }

    // getNextCalendarDay
    private Calendar getNextCalendarDay()
    {
        Calendar nextDay =
                new GregorianCalendar(currEntry.get(Calendar.YEAR),
                        currEntry.get(Calendar.MONTH),
                        currEntry.get(Calendar.DATE));
        nextDay.add(Calendar.DATE, 1);
        return nextDay;
    }

    // getPrevCalendarDay
    private Calendar getPrevCalendarDay()
    {
        Calendar prevDay =
                new GregorianCalendar(currEntry.get(Calendar.YEAR),
                        currEntry.get(Calendar.MONTH),
                        currEntry.get(Calendar.DATE));

        prevDay.add(Calendar.DATE, -1);
        return prevDay;
    }

    // getNextCalendarMonth
    private Calendar getNextCalendarMonth()
    {
        Calendar nextMonth =
                new GregorianCalendar(currEntry.get(Calendar.YEAR),
                        currEntry.get(Calendar.MONTH),
                        currEntry.get(Calendar.DATE));
        nextMonth.add(Calendar.MONTH, 1);
        return nextMonth;
    }

    // getPrevCalendarMonth
    private Calendar getPrevCalendarMonth()
    {
        Calendar prevMonth =
                new GregorianCalendar(currEntry.get(Calendar.YEAR),
                        currEntry.get(Calendar.MONTH),
                        currEntry.get(Calendar.DATE));

        prevMonth.add(Calendar.MONTH, -1);
        return prevMonth;
    }

    // showToast
    void showToast(int id)
    {
        String text = getString(id);
        showToast(text);
    }

    // showToast
    void showToast(String text)
    {
        // Cancel the last one
        if (toast != null)
            toast.cancel();

        // Make a new one
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    // animateSwipeLeft
    private void animateSwipeLeft()
    {
        Animation viewSwipeIn =
                AnimationUtils.loadAnimation(this, R.anim.swipe_left_in);

        markdownView.startAnimation(viewSwipeIn);
    }

    // animateSwipeRight
    private void animateSwipeRight()
    {
        Animation viewSwipeIn =
                AnimationUtils.loadAnimation(this, R.anim.swipe_right_in);

        markdownView.startAnimation(viewSwipeIn);
    }

    // onSwipeLeft
    private void onSwipeLeft()
    {
        if (!canSwipe && shown)
            return;

        Calendar nextDay = getNextCalendarDay();
        changeDate(nextDay);

        if (markdown && shown)
            animateSwipeLeft();
    }

    // onSwipeRight
    private void onSwipeRight()
    {
        if (!canSwipe && shown)
            return;

        Calendar prevDay = getPrevCalendarDay();
        changeDate(prevDay);

        if (markdown && shown)
            animateSwipeRight();
    }

    // onSwipeDown
    private void onSwipeDown()
    {
        if (!canSwipe && shown)
            return;

        Calendar prevMonth = getPrevCalendarMonth();
        changeDate(prevMonth);

        if (markdown && shown)
            animateSwipeRight();
    }

    // onSwipeUp
    private void onSwipeUp()
    {
        if (!canSwipe && shown)
            return;

        Calendar nextMonth = getNextCalendarMonth();
        changeDate(nextMonth);

        if (markdown && shown)
            animateSwipeLeft();
    }

    // onActionModeStarted
    @Override
    public void onActionModeStarted(ActionMode mode)
    {
        super.onActionModeStarted(mode);

        // Not on markdown view
        if (!shown)
        {
            // Get the start and end of the selection
            int start = textView.getSelectionStart();
            int end = textView.getSelectionEnd();
            // And the text
            CharSequence text = textView.getText();

            // Get a pattern and a matcher for delimiter characters
            Matcher matcher = PATTERN_CHARS.matcher(text);

            // Find the first match after the end of the selection
            if (matcher.find(end))
            {
                // Update the selection end
                end = matcher.start();

                // Get the matched char
                char c = text.charAt(end);

                // Check for opening brackets
                if (BRACKET_CHARS.indexOf(c) == -1)
                {
                    switch (c)
                    {
                        // Check for close brackets and look for the
                        // open brackets
                        case ')':
                            c = '(';
                            break;

                        case ']':
                            c = '[';
                            break;

                        case '}':
                            c = '{';
                            break;

                        case '>':
                            c = '<';
                            break;
                    }

                    String string = text.toString();
                    // Do reverse search
                    start = string.lastIndexOf(c, start) + 1;

                    // Check for included newline
                    if (start > string.lastIndexOf('\n', end))
                        // Update selection
                        textView.setSelection(start, end);
                }
            }
        }
    }

    // ZipTask
    private static class ZipTask
            extends AsyncTask<Void, Void, Void>
    {
        private WeakReference<Diary> diaryWeakReference;

        // ZipTask
        public ZipTask(Diary diary)
        {
            diaryWeakReference = new WeakReference<>(diary);
        }

        // onPreExecute
        @Override
        protected void onPreExecute()
        {
            final Diary diary = diaryWeakReference.get();
            if (diary != null)
                diary.showToast(R.string.start);
        }

        // doInBackground
        @Override
        protected Void doInBackground(Void... noparams)
        {
            final Diary diary = diaryWeakReference.get();
            if (diary == null)
                return null;

            File home = diary.getHome();

            // Create output stream
            try (ZipOutputStream output = new
                    ZipOutputStream(new FileOutputStream(home.getPath() + ZIP)))
            {
                byte[] buffer = new byte[BUFFER_SIZE];

                // Get entry list
                List<File> files = new ArrayList<>();
                listFiles(home, files);

                for (File file: files)
                {
                    // Get path
                    String path = file.getPath();
                    path = path.substring(home.getPath().length() + 1);

                    if (file.isDirectory())
                    {
                        ZipEntry entry = new ZipEntry(path + File.separator);
                        entry.setMethod(ZipEntry.STORED);
                        entry.setTime(file.lastModified());
                        entry.setSize(0);
                        entry.setCompressedSize(0);
                        entry.setCrc(0);
                        output.putNextEntry(entry);
                    }

                    else if (file.isFile())
                    {
                        ZipEntry entry = new ZipEntry(path);
                        entry.setMethod(ZipEntry.DEFLATED);
                        entry.setTime(file.lastModified());
                        output.putNextEntry(entry);

                        try (BufferedInputStream input = new
                                BufferedInputStream(new FileInputStream(file)))
                        {
                            while (input.available() > 0)
                            {
                                int size = input.read(buffer);
                                output.write(buffer, 0, size);
                            }
                        }
                    }
                }

                // Close last entry
                output.closeEntry();
            }

            catch (Exception e)
            {
                diary.runOnUiThread (() ->
                        diary.alertDialog(R.string.appName, e.getMessage(),
                                android.R.string.ok));
                e.printStackTrace();
            }

            return null;
        }

        // onPostExecute
        @Override
        protected void onPostExecute(Void noresult)
        {
            final Diary diary = diaryWeakReference.get();
            if (diary != null)
                diary.showToast(R.string.complete);
        }
    }

    // FindTask
    private static class FindTask
            extends AsyncTask<String, Void, List<String>>
    {
        private WeakReference<Diary> diaryWeakReference;
        private String search;

        // FindTask
        public FindTask(Diary diary)
        {
            diaryWeakReference = new WeakReference<>(diary);
        }

        // doInBackground
        @Override
        protected List<String> doInBackground(String... params)
        {
            // Create a list of matches
            List<String> matches = new ArrayList<>();
            final Diary diary = diaryWeakReference.get();
            if (diary == null)
                return matches;

            search = params[0];
            Pattern pattern =
                    Pattern.compile(search, Pattern.CASE_INSENSITIVE |
                            Pattern.LITERAL | Pattern.UNICODE_CASE);
            // Get entry list
            List<File> entries = new ArrayList<>();
            listEntries(diary.getHome(), entries);

            DateFormat dateFormat =
                    DateFormat.getDateInstance(DateFormat.MEDIUM);

            // Check the entries
            for (File file : entries)
            {
                CharSequence content = read(file);
                Matcher matcher = pattern.matcher(content);
                if (matcher.find())
                {
                    String headline = content.toString().split("\n")[0];
                    matches.add
                            (dateFormat.format(parseTime(file)) + "  " + headline);
                }
            }

            return matches;
        }

        // onPostExecute
        @Override
        protected void onPostExecute(List<String> matches)
        {
            final Diary diary = diaryWeakReference.get();
            if (diary == null)
                return;

            // Build dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(diary);
            builder.setTitle(R.string.findAll);

            // If found populate dialog
            if (!matches.isEmpty())
            {
                final String[] choices = matches.toArray(new String[0]);
                builder.setItems(choices, (dialog, which) ->
                {
                    String choice = choices[which];
                    DateFormat format =
                            DateFormat.getDateInstance(DateFormat.MEDIUM);

                    // Get the entry chosen
                    try
                    {
                        Date date = format.parse(choice);
                        Calendar entry = Calendar.getInstance();
                        entry.setTime(date);
                        diary.changeDate(entry);

                        // Put the search text back - why it
                        // disappears I have no idea or why I have to
                        // do it after a delay
                        diary.searchView.postDelayed(() ->
                                        diary.searchView.setQuery(search, false),
                                FIND_DELAY);
                    }

                    catch (Exception e) {}
                });
            }

            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
        }
    }

    // QueryTextListener
    private class QueryTextListener
            implements SearchView.OnQueryTextListener
    {
        private BackgroundColorSpan span = new
                BackgroundColorSpan(Color.YELLOW);
        private Editable editable;
        private Pattern pattern;
        private Matcher matcher;
        private int index;
        private int height;

        // onQueryTextChange
        @Override
        @SuppressWarnings("deprecation")
        public boolean onQueryTextChange(String newText)
        {
            // Use web view functionality
            if (shown)
                markdownView.findAll(newText);

                // Use regex search and spannable for highlighting
            else
            {
                height = scrollView.getHeight();
                editable = textView.getEditableText();

                // Reset the index and clear highlighting
                if (newText.length() == 0)
                {
                    index = 0;
                    editable.removeSpan(span);
                }

                // Get pattern
                pattern = Pattern.compile(newText,
                        Pattern.CASE_INSENSITIVE |
                                Pattern.LITERAL |
                                Pattern.UNICODE_CASE);
                // Find text
                matcher = pattern.matcher(editable);
                if (matcher.find(index))
                {
                    // Get index
                    index = matcher.start();

                    // Get text position
                    int line = textView.getLayout().getLineForOffset(index);
                    int position = textView.getLayout().getLineBaseline(line);

                    // Scroll to it
                    scrollView.smoothScrollTo(0, position - height / 2);

                    // Highlight it
                    editable.setSpan(span, matcher.start(), matcher.end(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            return true;
        }

        // onQueryTextSubmit
        @Override
        public boolean onQueryTextSubmit(String query)
        {
            // Use web view functionality
            if (shown)
                markdownView.findNext(true);

                // Use regex search and spannable for highlighting
            else
            {
                // Find next text
                if (matcher.find())
                {
                    // Get index
                    index = matcher.start();

                    // Get text position
                    int line = textView.getLayout().getLineForOffset(index);
                    int position = textView.getLayout().getLineBaseline(line);

                    // Scroll to it
                    scrollView.smoothScrollTo(0, position - height / 2);

                    // Highlight it
                    editable.setSpan(span, matcher.start(), matcher.end(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                // Reset matcher
                if (matcher.hitEnd())
                    matcher.reset();
            }

            return true;
        }
    }

    // GestureListener
    private class GestureListener
            extends GestureDetector.SimpleOnGestureListener
    {
        private static final int SWIPE_THRESHOLD = 256;
        private static final int SWIPE_VELOCITY_THRESHOLD = 256;

        // onDown
        @Override
        public boolean onDown(MotionEvent e)
        {
            return true;
        }

        // onFling
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2,
                               float velocityX, float velocityY)
        {
            boolean result = false;

            try
            {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY))
                {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD &&
                            Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD)
                    {
                        if (diffX > 0)
                        {
                            onSwipeRight();
                        }
                        else
                        {
                            onSwipeLeft();
                        }
                    }

                    result = true;
                }
                else
                {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD &&
                            Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD &&
                            multi)
                    {
                        if (diffY > 0)
                        {
                            onSwipeDown();
                        }
                        else
                        {
                            onSwipeUp();
                        }
                    }

                    result = true;
                }

                multi = false;
            }

            catch (Exception e) {}

            return result;
        }

        // onDoubleTap
        @Override
        public boolean onDoubleTap(MotionEvent e)
        {
            if (shown)
            {
                int[] l = new int[2];
                markdownView.getLocationOnScreen(l);

                // Get tap position
                float y = e.getY();
                y -= l[1];

                int scrollY = markdownView.getScrollY();
                int contentHeight = markdownView.getContentHeight();
                float density = getResources().getDisplayMetrics().density;

                // Get markdown position
                final float p = (y + scrollY) / (contentHeight * density);

                // Animation
                animateEdit();

                // Close text search
                if (searchItem.isActionViewExpanded())
                    searchItem.collapseActionView();

                // Scroll after delay
                textView.postDelayed(() ->
                {
                    int h = textView.getLayout().getHeight();
                    int v = Math.round(h * p);

                    // Get line
                    int line = textView.getLayout().getLineForVertical(v);
                    int offset = textView.getLayout()
                            .getOffsetForHorizontal(line, 0);
                    textView.setSelection(offset);

                    // get text position
                    int position = textView.getLayout().getLineBaseline(line);

                    // Scroll to it
                    int height = scrollView.getHeight();
                    scrollView.smoothScrollTo(0, position - height / 2);
                }, POSITION_DELAY);

                shown = false;

                return true;
            }

            return false;
        }
    }

    // EntryDecorator
    private class EntryDecorator
            implements DayDecorator
    {
        // EntryDecorator
        private EntryDecorator()
        {
        }

        // decorate
        @Override
        public void decorate(DayView dayView)
        {
            Calendar cellDate = dayView.getDate();
            File dayFile = getDay(cellDate.get(Calendar.YEAR),
                    cellDate.get(Calendar.MONTH),
                    cellDate.get(Calendar.DATE));

            if (dayFile.exists())
                dayView.setBackgroundResource(R.drawable.diary_entry);
        }
    }
}
