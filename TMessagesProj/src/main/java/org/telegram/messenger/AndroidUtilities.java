/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2016.
 */

package org.telegram.messenger;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.CallLog;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.EdgeEffectCompat;
import android.telephony.TelephonyManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.StateSet;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.EdgeEffect;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.internal.telephony.ITelephony;

import ir.chatgeram.messenger.BuildConfig;
import ir.chatgeram.messenger.R;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;
import net.hockeyapp.android.UpdateManager;

import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.ForegroundDetector;
import org.telegram.ui.Components.NumberPicker;
import org.telegram.ui.Components.TypefaceSpan;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.util.regex.Pattern;

public class AndroidUtilities {

    private static final Hashtable<String, Typeface> typefaceCache = new Hashtable<>();
    private static int prevOrientation = -10;
    private static boolean waitingForSms = false;
    private static boolean waitingForCall = false;
    private static final Object smsLock = new Object();
    private static final Object callLock = new Object();

    public static int statusBarHeight = 0;
    public static float density = 1;
    public static Point displaySize = new Point();
    public static boolean incorrectDisplaySizeFix;
    public static Integer photoSize = null;
    public static DisplayMetrics displayMetrics = new DisplayMetrics();
    public static int leftBaseline;
    public static boolean usingHardwareInput;
    public static boolean isInMultiwindow;
    private static Boolean isTablet = null;
    private static int adjustOwnerClassGuid = 0;

    private static Paint roundPaint;
    private static RectF bitmapRect;

    public static Pattern WEB_URL = null;
    public static final String THEME_PREFS = "theme";
    public static final int THEME_PREFS_MODE = Activity.MODE_PRIVATE;

    public static final int defColor = 0xFF477295;// 0xFF477295;//0xff42A5F5;//0xff58BCD5;//0xff43C3DB;//0xff2f8cc9;58BCD5//0xff55abd2
    public static int themeColor = getIntColor("themeColor");

    public static boolean needRestart = false;

    public static boolean themeUpdated = false;

    static long lastCheck = -1;

    static {
        try {
            final String GOOD_IRI_CHAR = "a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";
            final Pattern IP_ADDRESS = Pattern.compile(
                    "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                            + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                            + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                            + "|[1-9][0-9]|[0-9]))");
            final String IRI = "[" + GOOD_IRI_CHAR + "]([" + GOOD_IRI_CHAR + "\\-]{0,61}[" + GOOD_IRI_CHAR + "]){0,1}";
            final String GOOD_GTLD_CHAR = "a-zA-Z\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";
            final String GTLD = "[" + GOOD_GTLD_CHAR + "]{2,63}";
            final String HOST_NAME = "(" + IRI + "\\.)+" + GTLD;
            final Pattern DOMAIN_NAME = Pattern.compile("(" + HOST_NAME + "|" + IP_ADDRESS + ")");
            WEB_URL = Pattern.compile(
                    "((?:(http|https|Http|Https):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
                            + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
                            + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
                            + "(?:" + DOMAIN_NAME + ")"
                            + "(?:\\:\\d{1,5})?)" // plus option port number
                            + "(\\/(?:(?:[" + GOOD_IRI_CHAR + "\\;\\/\\?\\:\\@\\&\\=\\#\\~"  // plus option query params
                            + "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
                            + "(?:\\b|$)");
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    static {
        leftBaseline = isTablet() ? 80 : 72;
        checkDisplaySize(ApplicationLoader.applicationContext, null);
    }

    public static int[] calcDrawableColor(Drawable drawable) {
        int bitmapColor = 0xff000000;
        int result[] = new int[2];
        try {
            if (drawable instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                if (bitmap != null) {
                    Bitmap b = Bitmaps.createScaledBitmap(bitmap, 1, 1, true);
                    if (b != null) {
                        bitmapColor = b.getPixel(0, 0);
                        b.recycle();
                    }
                }
            } else if (drawable instanceof ColorDrawable) {
                bitmapColor = ((ColorDrawable) drawable).getColor();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }

        double[] hsv = rgbToHsv((bitmapColor >> 16) & 0xff, (bitmapColor >> 8) & 0xff, bitmapColor & 0xff);
        hsv[1] = Math.min(1.0, hsv[1] + 0.05 + 0.1 * (1.0 - hsv[1]));
        hsv[2] = Math.max(0, hsv[2] * 0.65);
        int rgb[] = hsvToRgb(hsv[0], hsv[1], hsv[2]);
        result[0] = Color.argb(0x66, rgb[0], rgb[1], rgb[2]);
        result[1] = Color.argb(0x88, rgb[0], rgb[1], rgb[2]);
        return result;
    }

    private static double[] rgbToHsv(int r, int g, int b) {
        double rf = r / 255.0;
        double gf = g / 255.0;
        double bf = b / 255.0;
        double max = (rf > gf && rf > bf) ? rf : (gf > bf) ? gf : bf;
        double min = (rf < gf && rf < bf) ? rf : (gf < bf) ? gf : bf;
        double h, s;
        double d = max - min;
        s = max == 0 ? 0 : d / max;
        if (max == min) {
            h = 0;
        } else {
            if (rf > gf && rf > bf) {
                h = (gf - bf) / d + (gf < bf ? 6 : 0);
            } else if (gf > bf) {
                h = (bf - rf) / d + 2;
            } else {
                h = (rf - gf) / d + 4;
            }
            h /= 6;
        }
        return new double[]{h, s, max};
    }

    private static int[] hsvToRgb(double h, double s, double v) {
        double r = 0, g = 0, b = 0;
        double i = (int) Math.floor(h * 6);
        double f = h * 6 - i;
        double p = v * (1 - s);
        double q = v * (1 - f * s);
        double t = v * (1 - (1 - f) * s);
        switch ((int) i % 6) {
            case 0:
                r = v;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = v;
                b = p;
                break;
            case 2:
                r = p;
                g = v;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = v;
                break;
            case 4:
                r = t;
                g = p;
                b = v;
                break;
            case 5:
                r = v;
                g = p;
                b = q;
                break;
        }
        return new int[]{(int) (r * 255), (int) (g * 255), (int) (b * 255)};
    }

    public static void requestAdjustResize(Activity activity, int classGuid) {
        if (activity == null || isTablet()) {
            return;
        }
        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        adjustOwnerClassGuid = classGuid;
    }

    public static void removeAdjustResize(Activity activity, int classGuid) {
        if (activity == null || isTablet()) {
            return;
        }
        if (adjustOwnerClassGuid == classGuid) {
            activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }
    }

    public static boolean isGoogleMapsInstalled(final BaseFragment fragment) {
        try {
            ApplicationLoader.applicationContext.getPackageManager().getApplicationInfo("com.google.android.apps.maps", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            if (fragment.getParentActivity() == null) {
                return false;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity());
            builder.setMessage("Install Google Maps?");
            builder.setCancelable(true);
            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.maps"));
                        fragment.getParentActivity().startActivityForResult(intent, 500);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
            });
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            fragment.showDialog(builder.create());
            return false;
        }
    }

    public static boolean isInternalUri(Uri uri) {
        String pathString = uri.getPath();
        if (pathString == null) {
            return false;
        }
        while (true) {
            String newPath = Utilities.readlink(pathString);
            if (newPath == null || newPath.equals(pathString)) {
                break;
            }
            pathString = newPath;
        }
        if (pathString != null) {
            try {
                String path = new File(pathString).getCanonicalPath();
                if (path != null) {
                    pathString = path;
                }
            } catch (Exception e) {
                pathString.replace("/./", "/");
                //igonre
            }
        }
        return pathString != null && pathString.toLowerCase().contains("/data/data/" + ApplicationLoader.applicationContext.getPackageName() + "/files");
    }

    public static void lockOrientation(Activity activity) {
        if (activity == null || prevOrientation != -10) {
            return;
        }
        try {
            prevOrientation = activity.getRequestedOrientation();
            WindowManager manager = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
            if (manager != null && manager.getDefaultDisplay() != null) {
                int rotation = manager.getDefaultDisplay().getRotation();
                int orientation = activity.getResources().getConfiguration().orientation;

                if (rotation == Surface.ROTATION_270) {
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    } else {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    }
                } else if (rotation == Surface.ROTATION_90) {
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    } else {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    }
                } else if (rotation == Surface.ROTATION_0) {
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    } else {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    }
                } else {
                    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                    } else {
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void unlockOrientation(Activity activity) {
        if (activity == null) {
            return;
        }
        try {
            if (prevOrientation != -10) {
                activity.setRequestedOrientation(prevOrientation);
                prevOrientation = -10;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static Typeface getTypeface(String assetPath) {
        synchronized (typefaceCache) {
            if (!typefaceCache.containsKey(assetPath)) {
                try {
                    SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", 0);

                    if (!sharedPreferences.getString("myFont", "rmedium.ttf").equalsIgnoreCase("rmedium.ttf")) {
                        assetPath = "fonts/" + sharedPreferences.getString("myFont", "rmedium.ttf");
                    }

                    Typeface t = Typeface.createFromAsset(ApplicationLoader.applicationContext.getAssets(), assetPath);
                    typefaceCache.put(assetPath, t);

                } catch (Exception e) {
                    FileLog.e("Could not get typeface '" + assetPath + "' because " + e.getMessage());
                    return null;
                }
            }
            return typefaceCache.get(assetPath);
        }
    }

    public static boolean isWaitingForSms() {
        boolean value;
        synchronized (smsLock) {
            value = waitingForSms;
        }
        return value;
    }

    public static void setWaitingForSms(boolean value) {
        synchronized (smsLock) {
            waitingForSms = value;
        }
    }

    public static boolean isWaitingForCall() {
        boolean value;
        synchronized (callLock) {
            value = waitingForCall;
        }
        return value;
    }

    public static void setWaitingForCall(boolean value) {
        synchronized (callLock) {
            waitingForCall = value;
        }
    }

    public static void showKeyboard(View view) {
        if (view == null) {
            return;
        }
        try {
            InputMethodManager inputManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static boolean isKeyboardShowed(View view) {
        if (view == null) {
            return false;
        }
        try {
            InputMethodManager inputManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            return inputManager.isActive(view);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return false;
    }

    public static void hideKeyboard(View view) {
        if (view == null) {
            return;
        }
        try {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (!imm.isActive()) {
                return;
            }
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static File getCacheDir() {
        String state = null;
        try {
            state = Environment.getExternalStorageState();
        } catch (Exception e) {
            FileLog.e(e);
        }
        if (state == null || state.startsWith(Environment.MEDIA_MOUNTED)) {
            try {
                File file = ApplicationLoader.applicationContext.getExternalCacheDir();
                if (file != null) {
                    return file;
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        try {
            File file = ApplicationLoader.applicationContext.getCacheDir();
            if (file != null) {
                return file;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return new File("");
    }

    public static int dp(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.ceil(density * value);
    }

    public static int dp2(float value) {
        if (value == 0) {
            return 0;
        }
        return (int) Math.floor(density * value);
    }

    public static void setScrollViewEdgeEffectColor(ScrollView scrollView, int color) {
        if (Build.VERSION.SDK_INT >= 21) {
            try {
                Field field = ScrollView.class.getDeclaredField("mEdgeGlowTop");
                field.setAccessible(true);
                EdgeEffect mEdgeGlowTop = (EdgeEffect) field.get(scrollView);
                if (mEdgeGlowTop != null) {
                    mEdgeGlowTop.setColor(color);
                }

                field = ScrollView.class.getDeclaredField("mEdgeGlowBottom");
                field.setAccessible(true);
                EdgeEffect mEdgeGlowBottom = (EdgeEffect) field.get(scrollView);
                if (mEdgeGlowBottom != null) {
                    mEdgeGlowBottom.setColor(color);
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    public static int compare(int lhs, int rhs) {
        if (lhs == rhs) {
            return 0;
        } else if (lhs > rhs) {
            return 1;
        }
        return -1;
    }

    public static float dpf2(float value) {
        if (value == 0) {
            return 0;
        }
        return density * value;
    }


    public static void checkDisplaySize() {
        try {
            Configuration configuration = ApplicationLoader.applicationContext.getResources().getConfiguration();
            usingHardwareInput = configuration.keyboard != Configuration.KEYBOARD_NOKEYS && configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
            WindowManager manager = (WindowManager) ApplicationLoader.applicationContext.getSystemService(Context.WINDOW_SERVICE);
            if (manager != null) {
                Display display = manager.getDefaultDisplay();
                if (display != null) {
                    display.getMetrics(displayMetrics);
                    if (android.os.Build.VERSION.SDK_INT < 13) {
                        displaySize.set(display.getWidth(), display.getHeight());
                    } else {
                        display.getSize(displaySize);
                    }
                    FileLog.e("display size = " + displaySize.x + " " + displaySize.y + " " + displayMetrics.xdpi + "x" + displayMetrics.ydpi);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }


    public static void checkDisplaySize(Context context, Configuration newConfiguration) {
        try {
            density = context.getResources().getDisplayMetrics().density;
            Configuration configuration = newConfiguration;
            if (configuration == null) {
                configuration = context.getResources().getConfiguration();
            }
            usingHardwareInput = configuration.keyboard != Configuration.KEYBOARD_NOKEYS && configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO;
            WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            if (manager != null) {
                Display display = manager.getDefaultDisplay();
                if (display != null) {
                    display.getMetrics(displayMetrics);
                    display.getSize(displaySize);
                }
            }
            if (configuration.screenWidthDp != Configuration.SCREEN_WIDTH_DP_UNDEFINED) {
                int newSize = (int) Math.ceil(configuration.screenWidthDp * density);
                if (Math.abs(displaySize.x - newSize) > 3) {
                    displaySize.x = newSize;
                }
            }
            if (configuration.screenHeightDp != Configuration.SCREEN_HEIGHT_DP_UNDEFINED) {
                int newSize = (int) Math.ceil(configuration.screenHeightDp * density);
                if (Math.abs(displaySize.y - newSize) > 3) {
                    displaySize.y = newSize;
                }
            }
            FileLog.e("display size = " + displaySize.x + " " + displaySize.y + " " + displayMetrics.xdpi + "x" + displayMetrics.ydpi);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static float getPixelsInCM(float cm, boolean isX) {
        return (cm / 2.54f) * (isX ? displayMetrics.xdpi : displayMetrics.ydpi);
    }

    public static long makeBroadcastId(int id) {
        return 0x0000000100000000L | ((long) id & 0x00000000FFFFFFFFL);
    }

    public static int getMyLayerVersion(int layer) {
        return layer & 0xffff;
    }

    public static int getPeerLayerVersion(int layer) {
        return (layer >> 16) & 0xffff;
    }

    public static int setMyLayerVersion(int layer, int version) {
        return layer & 0xffff0000 | version;
    }

    public static int setPeerLayerVersion(int layer, int version) {
        return layer & 0x0000ffff | (version << 16);
    }

    public static void runOnUIThread(Runnable runnable) {
        runOnUIThread(runnable, 0);
    }

    public static void runOnUIThread(Runnable runnable, long delay) {
        if (delay == 0) {
            ApplicationLoader.applicationHandler.post(runnable);
        } else {
            ApplicationLoader.applicationHandler.postDelayed(runnable, delay);
        }
    }

    public static void cancelRunOnUIThread(Runnable runnable) {
        ApplicationLoader.applicationHandler.removeCallbacks(runnable);
    }

    public static boolean isTablet() {
        if (isTablet == null) {
            isTablet = ApplicationLoader.applicationContext.getResources().getBoolean(R.bool.isTablet);
        }
        return isTablet;
    }

    public static boolean isSmallTablet() {
        float minSide = Math.min(displaySize.x, displaySize.y) / density;
        return minSide <= 700;
    }

    public static int getMinTabletSide() {
        if (!isSmallTablet()) {
            int smallSide = Math.min(displaySize.x, displaySize.y);
            int leftSide = smallSide * 35 / 100;
            if (leftSide < dp(320)) {
                leftSide = dp(320);
            }
            return smallSide - leftSide;
        } else {
            int smallSide = Math.min(displaySize.x, displaySize.y);
            int maxSide = Math.max(displaySize.x, displaySize.y);
            int leftSide = maxSide * 35 / 100;
            if (leftSide < dp(320)) {
                leftSide = dp(320);
            }
            return Math.min(smallSide, maxSide - leftSide);
        }
    }

    public static int getPhotoSize() {
        if (photoSize == null) {
            if (Build.VERSION.SDK_INT >= 16) {
                photoSize = 1280;
            } else {
                photoSize = 800;
            }
        }
        return photoSize;
    }

    public static String formatTTLString(int ttl) {
        if (ttl < 60) {
            return LocaleController.formatPluralString("Seconds", ttl);
        } else if (ttl < 60 * 60) {
            return LocaleController.formatPluralString("Minutes", ttl / 60);
        } else if (ttl < 60 * 60 * 24) {
            return LocaleController.formatPluralString("Hours", ttl / 60 / 60);
        } else if (ttl < 60 * 60 * 24 * 7) {
            return LocaleController.formatPluralString("Days", ttl / 60 / 60 / 24);
        } else {
            int days = ttl / 60 / 60 / 24;
            if (ttl % 7 == 0) {
                return LocaleController.formatPluralString("Weeks", days / 7);
            } else {
                return String.format("%s %s", LocaleController.formatPluralString("Weeks", days / 7), LocaleController.formatPluralString("Days", days % 7));
            }
        }
    }

    public static AlertDialog.Builder buildTTLAlert(final Context context, final TLRPC.EncryptedChat encryptedChat) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("MessageLifetime", R.string.MessageLifetime));
        final NumberPicker numberPicker = new NumberPicker(context);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(20);
        if (encryptedChat.ttl > 0 && encryptedChat.ttl < 16) {
            numberPicker.setValue(encryptedChat.ttl);
        } else if (encryptedChat.ttl == 30) {
            numberPicker.setValue(16);
        } else if (encryptedChat.ttl == 60) {
            numberPicker.setValue(17);
        } else if (encryptedChat.ttl == 60 * 60) {
            numberPicker.setValue(18);
        } else if (encryptedChat.ttl == 60 * 60 * 24) {
            numberPicker.setValue(19);
        } else if (encryptedChat.ttl == 60 * 60 * 24 * 7) {
            numberPicker.setValue(20);
        } else if (encryptedChat.ttl == 0) {
            numberPicker.setValue(0);
        }
        numberPicker.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                if (value == 0) {
                    return LocaleController.getString("ShortMessageLifetimeForever", R.string.ShortMessageLifetimeForever);
                } else if (value >= 1 && value < 16) {
                    return AndroidUtilities.formatTTLString(value);
                } else if (value == 16) {
                    return AndroidUtilities.formatTTLString(30);
                } else if (value == 17) {
                    return AndroidUtilities.formatTTLString(60);
                } else if (value == 18) {
                    return AndroidUtilities.formatTTLString(60 * 60);
                } else if (value == 19) {
                    return AndroidUtilities.formatTTLString(60 * 60 * 24);
                } else if (value == 20) {
                    return AndroidUtilities.formatTTLString(60 * 60 * 24 * 7);
                }
                return "";
            }
        });
        builder.setView(numberPicker);
        builder.setNegativeButton(LocaleController.getString("Done", R.string.Done), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int oldValue = encryptedChat.ttl;
                which = numberPicker.getValue();
                if (which >= 0 && which < 16) {
                    encryptedChat.ttl = which;
                } else if (which == 16) {
                    encryptedChat.ttl = 30;
                } else if (which == 17) {
                    encryptedChat.ttl = 60;
                } else if (which == 18) {
                    encryptedChat.ttl = 60 * 60;
                } else if (which == 19) {
                    encryptedChat.ttl = 60 * 60 * 24;
                } else if (which == 20) {
                    encryptedChat.ttl = 60 * 60 * 24 * 7;
                }
                if (oldValue != encryptedChat.ttl) {
                    SecretChatHelper.getInstance().sendTTLMessage(encryptedChat, null);
                    MessagesStorage.getInstance().updateEncryptedChatTTL(encryptedChat);
                }
            }
        });
        return builder;
    }

    public static void clearCursorDrawable(EditText editText) {
        if (editText == null) {
            return;
        }
        try {
            Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            mCursorDrawableRes.setAccessible(true);
            mCursorDrawableRes.setInt(editText, 0);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void setProgressBarAnimationDuration(ProgressBar progressBar, int duration) {
        if (progressBar == null) {
            return;
        }
        try {
            Field mCursorDrawableRes = ProgressBar.class.getDeclaredField("mDuration");
            mCursorDrawableRes.setAccessible(true);
            mCursorDrawableRes.setInt(progressBar, duration);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static Intent createShortcutIntent(long did, boolean forDelete) {
        Intent shortcutIntent = new Intent(ApplicationLoader.applicationContext, OpenChatReceiver.class);

        int lower_id = (int) did;
        int high_id = (int) (did >> 32);

        TLRPC.User user = null;
        TLRPC.Chat chat = null;
        if (lower_id == 0) {
            shortcutIntent.putExtra("encId", high_id);
            TLRPC.EncryptedChat encryptedChat = MessagesController.getInstance().getEncryptedChat(high_id);
            if (encryptedChat == null) {
                return null;
            }
            user = MessagesController.getInstance().getUser(encryptedChat.user_id);
        } else if (lower_id > 0) {
            shortcutIntent.putExtra("userId", lower_id);
            user = MessagesController.getInstance().getUser(lower_id);
        } else if (lower_id < 0) {
            chat = MessagesController.getInstance().getChat(-lower_id);
            shortcutIntent.putExtra("chatId", -lower_id);
        } else {
            return null;
        }
        if (user == null && chat == null) {
            return null;
        }

        String name;
        TLRPC.FileLocation photo = null;

        if (user != null) {
            name = ContactsController.formatName(user.first_name, user.last_name);
            if (user.photo != null) {
                photo = user.photo.photo_small;
            }
        } else {
            name = chat.title;
            if (chat.photo != null) {
                photo = chat.photo.photo_small;
            }
        }

        shortcutIntent.setAction("com.tmessages.openchat" + did);
        shortcutIntent.addFlags(0x4000000);

        Intent addIntent = new Intent();
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        addIntent.putExtra("duplicate", false);
        if (!forDelete) {
            Bitmap bitmap = null;
            if (photo != null) {
                try {
                    File path = FileLoader.getPathToAttach(photo, true);
                    bitmap = BitmapFactory.decodeFile(path.toString());
                    if (bitmap != null) {
                        int size = AndroidUtilities.dp(58);
                        Bitmap result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                        result.eraseColor(Color.TRANSPARENT);
                        Canvas canvas = new Canvas(result);
                        BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                        if (roundPaint == null) {
                            roundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                            bitmapRect = new RectF();
                        }
                        float scale = size / (float) bitmap.getWidth();
                        canvas.save();
                        canvas.scale(scale, scale);
                        roundPaint.setShader(shader);
                        bitmapRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
                        canvas.drawRoundRect(bitmapRect, bitmap.getWidth(), bitmap.getHeight(), roundPaint);
                        canvas.restore();
                        Drawable drawable = ApplicationLoader.applicationContext.getResources().getDrawable(R.drawable.ic_launcher);
                        int w = AndroidUtilities.dp(15);
                        int left = size - w - AndroidUtilities.dp(2);
                        int top = size - w - AndroidUtilities.dp(2);
                        drawable.setBounds(left, top, left + w, top + w);
                        drawable.draw(canvas);
                        try {
                            canvas.setBitmap(null);
                        } catch (Exception e) {
                            //don't promt, this will crash on 2.x
                        }
                        bitmap = result;
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
            if (bitmap != null) {
                addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
            } else {
                if (user != null) {
                    if (user.bot) {
                        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(ApplicationLoader.applicationContext, R.drawable.book_bot));
                    } else {
                        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(ApplicationLoader.applicationContext, R.drawable.book_user));
                    }
                } else if (chat != null) {
                    if (ChatObject.isChannel(chat) && !chat.megagroup) {
                        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(ApplicationLoader.applicationContext, R.drawable.book_channel));
                    } else {
                        addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(ApplicationLoader.applicationContext, R.drawable.book_group));
                    }
                }
            }
        }
        return addIntent;
    }

    public static void installShortcut(long did) {
        try {
            Intent addIntent = createShortcutIntent(did, false);
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            ApplicationLoader.applicationContext.sendBroadcast(addIntent);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void uninstallShortcut(long did) {
        try {
            Intent addIntent = createShortcutIntent(did, true);
            addIntent.setAction("com.android.launcher.action.UNINSTALL_SHORTCUT");
            ApplicationLoader.applicationContext.sendBroadcast(addIntent);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static Field mAttachInfoField;
    private static Field mStableInsetsField;

    public static int getViewInset(View view) {
        if (view == null || Build.VERSION.SDK_INT < 21 || view.getHeight() == AndroidUtilities.displaySize.y || view.getHeight() == AndroidUtilities.displaySize.y - statusBarHeight) {
            return 0;
        }
        try {
            if (mAttachInfoField == null) {
                mAttachInfoField = View.class.getDeclaredField("mAttachInfo");
                mAttachInfoField.setAccessible(true);
            }
            Object mAttachInfo = mAttachInfoField.get(view);
            if (mAttachInfo != null) {
                if (mStableInsetsField == null) {
                    mStableInsetsField = mAttachInfo.getClass().getDeclaredField("mStableInsets");
                    mStableInsetsField.setAccessible(true);
                }
                Rect insets = (Rect) mStableInsetsField.get(mAttachInfo);
                return insets.bottom;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return 0;
    }

    public static Point getRealScreenSize() {
        Point size = new Point();
        try {
            WindowManager windowManager = (WindowManager) ApplicationLoader.applicationContext.getSystemService(Context.WINDOW_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                windowManager.getDefaultDisplay().getRealSize(size);
            } else {
                try {
                    Method mGetRawW = Display.class.getMethod("getRawWidth");
                    Method mGetRawH = Display.class.getMethod("getRawHeight");
                    size.set((Integer) mGetRawW.invoke(windowManager.getDefaultDisplay()), (Integer) mGetRawH.invoke(windowManager.getDefaultDisplay()));
                } catch (Exception e) {
                    size.set(windowManager.getDefaultDisplay().getWidth(), windowManager.getDefaultDisplay().getHeight());
                    FileLog.e(e);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return size;
    }

    public static CharSequence getTrimmedString(CharSequence src) {
        if (src == null || src.length() == 0) {
            return src;
        }
        while (src.length() > 0 && (src.charAt(0) == '\n' || src.charAt(0) == ' ')) {
            src = src.subSequence(1, src.length());
        }
        while (src.length() > 0 && (src.charAt(src.length() - 1) == '\n' || src.charAt(src.length() - 1) == ' ')) {
            src = src.subSequence(0, src.length() - 1);
        }
        return src;
    }

    public static void setListViewEdgeEffectColor(AbsListView listView, int color) {
        if (Build.VERSION.SDK_INT >= 21) {
            try {
                Field field = AbsListView.class.getDeclaredField("mEdgeGlowTop");
                field.setAccessible(true);
                EdgeEffect mEdgeGlowTop = (EdgeEffect) field.get(listView);
                if (mEdgeGlowTop != null) {
                    mEdgeGlowTop.setColor(color);
                }

                field = AbsListView.class.getDeclaredField("mEdgeGlowBottom");
                field.setAccessible(true);
                EdgeEffect mEdgeGlowBottom = (EdgeEffect) field.get(listView);
                if (mEdgeGlowBottom != null) {
                    mEdgeGlowBottom.setColor(color);
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    @SuppressLint("NewApi")
    public static void clearDrawableAnimation(View view) {
        if (Build.VERSION.SDK_INT < 21 || view == null) {
            return;
        }
        Drawable drawable;
        if (view instanceof ListView) {
            drawable = ((ListView) view).getSelector();
            if (drawable != null) {
                drawable.setState(StateSet.NOTHING);
            }
        } else {
            drawable = view.getBackground();
            if (drawable != null) {
                drawable.setState(StateSet.NOTHING);
                drawable.jumpToCurrentState();
            }
        }
    }

    public static final int FLAG_TAG_BR = 1;
    public static final int FLAG_TAG_BOLD = 2;
    public static final int FLAG_TAG_COLOR = 4;
    public static final int FLAG_TAG_ALL = FLAG_TAG_BR | FLAG_TAG_BOLD | FLAG_TAG_COLOR;

    public static SpannableStringBuilder replaceTags(String str) {
        return replaceTags(str, FLAG_TAG_ALL);
    }

    public static SpannableStringBuilder replaceTags(String str, int flag) {
        try {
            int start;
            int end;
            StringBuilder stringBuilder = new StringBuilder(str);
            if ((flag & FLAG_TAG_BR) != 0) {
                while ((start = stringBuilder.indexOf("<br>")) != -1) {
                    stringBuilder.replace(start, start + 4, "\n");
                }
                while ((start = stringBuilder.indexOf("<br/>")) != -1) {
                    stringBuilder.replace(start, start + 5, "\n");
                }
            }
            ArrayList<Integer> bolds = new ArrayList<>();
            if ((flag & FLAG_TAG_BOLD) != 0) {
                while ((start = stringBuilder.indexOf("<b>")) != -1) {
                    stringBuilder.replace(start, start + 3, "");
                    end = stringBuilder.indexOf("</b>");
                    if (end == -1) {
                        end = stringBuilder.indexOf("<b>");
                    }
                    stringBuilder.replace(end, end + 4, "");
                    bolds.add(start);
                    bolds.add(end);
                }
            }
            ArrayList<Integer> colors = new ArrayList<>();
            if ((flag & FLAG_TAG_COLOR) != 0) {
                while ((start = stringBuilder.indexOf("<c#")) != -1) {
                    stringBuilder.replace(start, start + 2, "");
                    end = stringBuilder.indexOf(">", start);
                    int color = Color.parseColor(stringBuilder.substring(start, end));
                    stringBuilder.replace(start, end + 1, "");
                    end = stringBuilder.indexOf("</c>");
                    stringBuilder.replace(end, end + 4, "");
                    colors.add(start);
                    colors.add(end);
                    colors.add(color);
                }
            }
            SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(stringBuilder);
            for (int a = 0; a < bolds.size() / 2; a++) {
                spannableStringBuilder.setSpan(new TypefaceSpan(AndroidUtilities.getTypeface("fonts/rmedium.ttf")), bolds.get(a * 2), bolds.get(a * 2 + 1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            for (int a = 0; a < colors.size() / 3; a++) {
                spannableStringBuilder.setSpan(new ForegroundColorSpan(colors.get(a * 3 + 2)), colors.get(a * 3), colors.get(a * 3 + 1), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            return spannableStringBuilder;
        } catch (Exception e) {
            FileLog.e(e);
        }
        return new SpannableStringBuilder(str);
    }

    public static boolean needShowPasscode(boolean reset) {
        boolean wasInBackground = ForegroundDetector.getInstance().isWasInBackground(reset);
        if (reset) {
            ForegroundDetector.getInstance().resetBackgroundVar();
        }
        return UserConfig.passcodeHash.length() > 0 && wasInBackground &&
                (UserConfig.appLocked || UserConfig.autoLockIn != 0 && UserConfig.lastPauseTime != 0 && !UserConfig.appLocked && (UserConfig.lastPauseTime + UserConfig.autoLockIn) <= ConnectionsManager.getInstance().getCurrentTime());
    }

    public static void shakeView(final View view, final float x, final int num) {
        if (num == 6) {
            view.setTranslationX(0);
            return;
        }
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(ObjectAnimator.ofFloat(view, "translationX", AndroidUtilities.dp(x)));
        animatorSet.setDuration(50);
        animatorSet.addListener(new AnimatorListenerAdapterProxy() {
            @Override
            public void onAnimationEnd(Animator animation) {
                shakeView(view, num == 5 ? 0 : -x, num + 1);
            }
        });
        animatorSet.start();
    }

    /*public static String ellipsize(String text, int maxLines, int maxWidth, TextPaint paint) {
        if (text == null || paint == null) {
            return null;
        }
        int count;
        int offset = 0;
        StringBuilder result = null;
        TextView
        for (int a = 0; a < maxLines; a++) {
            count = paint.breakText(text, true, maxWidth, null);
            if (a != maxLines - 1) {
                if (result == null) {
                    result = new StringBuilder(count * maxLines + 1);
                }
                boolean foundSpace = false;
                for (int c = count - 1; c >= offset; c--) {
                    if (text.charAt(c) == ' ') {
                        foundSpace = true;
                        result.append(text.substring(offset, c - 1));
                        offset = c - 1;
                    }
                }
                if (!foundSpace) {
                    offset = count;
                }
                text = text.substring(0, offset);
            } else if (maxLines == 1) {
                return text.substring(0, count);
            } else {
                result.append(text.substring(0, count));
            }
        }
        return result.toString();
    }*/

    /*public static void turnOffHardwareAcceleration(Window window) {
        if (window == null || Build.MODEL == null) {
            return;
        }
        if (Build.MODEL.contains("GT-S5301") ||
                Build.MODEL.contains("GT-S5303") ||
                Build.MODEL.contains("GT-B5330") ||
                Build.MODEL.contains("GT-S5302") ||
                Build.MODEL.contains("GT-S6012B") ||
                Build.MODEL.contains("MegaFon_SP-AI")) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }
    }*/

    public static void checkForCrashes(Activity context) {
        CrashManager.register(context, BuildVars.DEBUG_VERSION ? BuildVars.HOCKEY_APP_HASH_DEBUG : BuildVars.HOCKEY_APP_HASH, new CrashManagerListener() {
            @Override
            public boolean includeDeviceData() {
                return true;
            }
        });
    }

    public static void checkForUpdates(Activity context) {
        if (BuildVars.DEBUG_VERSION) {
            UpdateManager.register(context, BuildVars.DEBUG_VERSION ? BuildVars.HOCKEY_APP_HASH_DEBUG : BuildVars.HOCKEY_APP_HASH);
        }
    }

    public static void unregisterUpdates() {
        if (BuildVars.DEBUG_VERSION) {
            UpdateManager.unregister();
        }
    }

    public static void addToClipboard(CharSequence str) {
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("label", str);
            clipboard.setPrimaryClip(clip);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void addMediaToGallery(String fromPath) {
        if (fromPath == null) {
            return;
        }
        File f = new File(fromPath);
        Uri contentUri = Uri.fromFile(f);
        addMediaToGallery(contentUri);
    }

    public static void addMediaToGallery(Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(uri);
            ApplicationLoader.applicationContext.sendBroadcast(mediaScanIntent);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static File getAlbumDir() {
        if (Build.VERSION.SDK_INT >= 23 && ApplicationLoader.applicationContext.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_CACHE);
        }
        File storageDir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Telegram");
            if (!storageDir.mkdirs()) {
                if (!storageDir.exists()) {
                    FileLog.d("failed to create directory");
                    return null;
                }
            }
        } else {
            FileLog.d("External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    @SuppressLint("NewApi")
    public static String getPath(final Uri uri) {
        try {
            final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
            if (isKitKat && DocumentsContract.isDocumentUri(ApplicationLoader.applicationContext, uri)) {
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                } else if (isDownloadsDocument(uri)) {
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                    return getDataColumn(ApplicationLoader.applicationContext, contentUri, null, null);
                } else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    switch (type) {
                        case "image":
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                            break;
                        case "video":
                            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                            break;
                        case "audio":
                            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                            break;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{
                            split[1]
                    };

                    return getDataColumn(ApplicationLoader.applicationContext, contentUri, selection, selectionArgs);
                }
            } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                return getDataColumn(ApplicationLoader.applicationContext, uri, null, null);
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                String value = cursor.getString(column_index);
                if (value.startsWith("content://") || !value.startsWith("/") && !value.startsWith("file://")) {
                    return null;
                }
                return value;
            }
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static File generatePicturePath() {
        try {
            File storageDir = getAlbumDir();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            return new File(storageDir, "IMG_" + timeStamp + ".jpg");
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    public static CharSequence generateSearchName(String name, String name2, String q) {
        if (name == null && name2 == null) {
            return "";
        }
        SpannableStringBuilder builder = new SpannableStringBuilder();
        String wholeString = name;
        if (wholeString == null || wholeString.length() == 0) {
            wholeString = name2;
        } else if (name2 != null && name2.length() != 0) {
            wholeString += " " + name2;
        }
        wholeString = wholeString.trim();
        String lower = " " + wholeString.toLowerCase();

        int index;
        int lastIndex = 0;
        while ((index = lower.indexOf(" " + q, lastIndex)) != -1) {
            int idx = index - (index == 0 ? 0 : 1);
            int end = q.length() + (index == 0 ? 0 : 1) + idx;

            if (lastIndex != 0 && lastIndex != idx + 1) {
                builder.append(wholeString.substring(lastIndex, idx));
            } else if (lastIndex == 0 && idx != 0) {
                builder.append(wholeString.substring(0, idx));
            }

            String query = wholeString.substring(idx, end);
            if (query.startsWith(" ")) {
                builder.append(" ");
            }
            query = query.trim();
            builder.append(AndroidUtilities.replaceTags("<c#ff4d83b3>" + query + "</c>"));

            lastIndex = end;
        }

        if (lastIndex != -1 && lastIndex != wholeString.length()) {
            builder.append(wholeString.substring(lastIndex, wholeString.length()));
        }

        return builder;
    }

    public static File generateVideoPath() {
        try {
            File storageDir = getAlbumDir();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            return new File(storageDir, "VID_" + timeStamp + ".mp4");
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    public static String formatFileSize(long size) {
        if (size < 1024) {
            return String.format("%d B", size);
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0f);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / 1024.0f / 1024.0f);
        } else {
            return String.format("%.1f GB", size / 1024.0f / 1024.0f / 1024.0f);
        }
    }

    public static byte[] decodeQuotedPrintable(final byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        for (int i = 0; i < bytes.length; i++) {
            final int b = bytes[i];
            if (b == '=') {
                try {
                    final int u = Character.digit((char) bytes[++i], 16);
                    final int l = Character.digit((char) bytes[++i], 16);
                    buffer.write((char) ((u << 4) + l));
                } catch (Exception e) {
                    FileLog.e(e);
                    return null;
                }
            } else {
                buffer.write(b);
            }
        }
        byte[] array = buffer.toByteArray();
        try {
            buffer.close();
        } catch (Exception e) {
            FileLog.e(e);
        }
        return array;
    }

    public static boolean copyFile(InputStream sourceFile, File destFile) throws IOException {
        OutputStream out = new FileOutputStream(destFile);
        byte[] buf = new byte[4096];
        int len;
        while ((len = sourceFile.read(buf)) > 0) {
            Thread.yield();
            out.write(buf, 0, len);
        }
        out.close();
        return true;
    }

    public static boolean copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }
        FileInputStream source = null;
        FileOutputStream destination = null;
        try {
            source = new FileInputStream(sourceFile);
            destination = new FileOutputStream(destFile);
            destination.getChannel().transferFrom(source.getChannel(), 0, source.getChannel().size());
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
        return true;
    }

    public static byte[] calcAuthKeyHash(byte[] auth_key) {
        byte[] sha1 = Utilities.computeSHA1(auth_key);
        byte[] key_hash = new byte[16];
        System.arraycopy(sha1, 0, key_hash, 0, 16);
        return key_hash;
    }

    //Powergram

    public static void brandGlowEffect(Context context, int brandColor) {
        //glow
        int glowDrawableId = context.getResources().getIdentifier("overscroll_glow", "drawable", "android");
        Drawable androidGlow = context.getResources().getDrawable(glowDrawableId);
        androidGlow.setColorFilter(brandColor, PorterDuff.Mode.SRC_IN);
        //edge
        int edgeDrawableId = context.getResources().getIdentifier("overscroll_edge", "drawable", "android");
        Drawable androidEdge = context.getResources().getDrawable(edgeDrawableId);
        androidEdge.setColorFilter(brandColor, PorterDuff.Mode.SRC_IN);
    }

    public static int getIntColor(String key) {
        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(THEME_PREFS, THEME_PREFS_MODE);
        return themePrefs.getInt(key, defColor);//Def color is Teal
    }

    public static int getIntTColor(String key) {
        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(THEME_PREFS, THEME_PREFS_MODE);
        int def = themePrefs.getInt("themeColor", defColor);
        return themePrefs.getInt(key, def);//Def color is theme color
    }

    public static int getIntDef(String key, int def) {
        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(THEME_PREFS, THEME_PREFS_MODE);
        return themePrefs.getInt(key, def);
    }

    public static int getIntAlphaColor(String key, int def, float factor) {
        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(THEME_PREFS, THEME_PREFS_MODE);
        int color = themePrefs.getInt(key, def);
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    public static int getIntDarkerColor(String key, int factor) {
        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(THEME_PREFS, THEME_PREFS_MODE);
        int color = themePrefs.getInt(key, defColor);
        return setDarkColor(color, factor);
    }

    public static int setDarkColor(int color, int factor) {
        int alpha = Color.alpha(color);
        int red = Color.red(color) - factor;
        int green = Color.green(color) - factor;
        int blue = Color.blue(color) - factor;
        if (factor < 0) {
            red = (red > 0xff) ? 0xff : red;
            green = (green > 0xff) ? 0xff : green;
            blue = (blue > 0xff) ? 0xff : blue;
            if (red == 0xff && green == 0xff && blue == 0xff) {
                red = factor;
                green = factor;
                blue = factor;
            }
        }
        if (factor > 0) {
            red = (red < 0) ? 0 : red;
            green = (green < 0) ? 0 : green;
            blue = (blue < 0) ? 0 : blue;
            if (red == 0 && green == 0 && blue == 0) {
                red = factor;
                green = factor;
                blue = factor;
            }
        }
        //return Color.argb(0xff, red, green, blue);
        return Color.argb(alpha, red, green, blue);
    }
    //Same as setDarkColor but maintains alpha
    /*public static int setDarkWithAlphaColor(int color, int factor){
        int alpha = Color.alpha(color);
        int red = Color.red(color) - factor;
        int green = Color.green(color) - factor;
        int blue = Color.blue(color) - factor;
        if(factor < 0){
            red = (red > 0xff) ? 0xff : red;
            green = (green > 0xff) ? 0xff : green;
            blue = (blue > 0xff) ? 0xff : blue;
            if(red == 0xff && green == 0xff && blue == 0xff){
                red = factor;
                green = factor;
                blue = factor;
            }
        }
        if(factor > 0){
            red = (red < 0) ? 0 : red;
            green = (green < 0) ? 0 : green;
            blue = (blue < 0) ? 0 : blue;
            if(red == 0 && green == 0 && blue == 0){
                red = factor;
                green = factor;
                blue = factor;
            }
        }
        return Color.argb(alpha, red, green, blue);
    }

    public static void setIntColor(String key, int value){
        SharedPreferences themePrefs = ApplicationLoader.applicationContext.getSharedPreferences(THEME_PREFS, THEME_PREFS_MODE);
        SharedPreferences.Editor e = themePrefs.edit();
        e.putInt(key, value);
        e.commit();
    }

    public static void setBoolPref(Context context, String key, Boolean b){
        SharedPreferences sharedPref = context.getSharedPreferences(THEME_PREFS, 0);
        SharedPreferences.Editor e = sharedPref.edit();
        e.putBoolean(key, b);
        e.commit();
    }*/

    public static void setStringPref(Context context, String key, String s) {
        SharedPreferences sharedPref = context.getSharedPreferences(THEME_PREFS, 0);
        SharedPreferences.Editor e = sharedPref.edit();
        e.putString(key, s);
        e.commit();
    }

    public static boolean getBoolPref(String key) {
        boolean s = false;
        if (ApplicationLoader.applicationContext.getSharedPreferences(THEME_PREFS, 0).getBoolean(key, false))
            s = true;
        return s;
    }

    public static boolean getBoolMain(String key) {
        boolean s = false;
        if (ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE).getBoolean(key, false))
            s = true;
        return s;
    }
/*
    public static int getIntPref(Context context,String key){
        int i=0;
        if(key.contains("picker")){
            int intColor = context.getSharedPreferences(THEME_PREFS, 0).getInt(key, Color.WHITE );
            i=intColor;
        }
        return i;
    }

    public static int getIntColorDef(Context context, String key, int def){
        int i=def;
        if(context.getSharedPreferences(THEME_PREFS, 0).getBoolean(key, false)){
            i = context.getSharedPreferences(THEME_PREFS, 0).getInt(key.replace("_check", "_picker"), def);
        }
        return i;
    }

    public static void setTVTextColor(Context ctx, TextView tv, String key, int def){
        if(tv==null)return;
        if(getBoolPref(ctx, key))
            def = getIntPref(ctx, key.replace("_check", "_picker"));
        tv.setTextColor(def);
    }

    public static int getSizePref(Context context,String key, int def){
        if(key.contains("picker"))return context.getSharedPreferences(THEME_PREFS, 0).getInt(key, def);
        return def;
    }

    public static void paintActionBarHeader(Activity a, ActionBar ab, String hdCheck, String gdMode){
        if(ab==null)return;
        ab.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#54759E")));
        if(getBoolPref(a, hdCheck)){
            int i = getIntPref(a, hdCheck.replace("_check", "_picker"));
            Drawable d = new ColorDrawable(i);
            try{
                ab.setBackgroundDrawable(d);
                d = paintGradient(a,i,gdMode.replace("mode","color_picker"),gdMode);
                if(d!=null)ab.setBackgroundDrawable(d);
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    public static Drawable paintGradient(Context c, int mainColor, String gColor, String g){
        GradientDrawable gd = null;
        int[] a ={mainColor,getIntPref(c,gColor)};
        int gType = Integer.parseInt(c.getSharedPreferences(THEME_PREFS, 0).getString(g, "0"));
        if(gType==2) gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,a);
        if(gType==1) gd = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM ,a);
        return gd;
    }

    public static Drawable paintDrawable(Context c, int resId, int resIdW, String color){
        Drawable d = c.getResources().getDrawable(resId);
        if(color.contains("_check")){
            if(getBoolPref(color)){
                d = c.getResources().getDrawable(resIdW);
                d.setColorFilter(getIntPref(c, color.replace("_check", "_picker")), PorterDuff.Mode.MULTIPLY);
            }
        }
        return d;
    }*/

    public static int getDefBubbleColor() {
        int color = 0xffd1efff;//0xff80cbc4;
        if (getIntColor("themeColor") != 0xffd1efff) {
            color = AndroidUtilities.getIntDarkerColor("themeColor", -0x50);
        }
        return color;
    }

    public static void checkForThemes(final Activity context) {
        //if (!BuildConfig.DEBUG) {
        //}
        try {
            long myDelay = (30L * 24L * 60L * 60L * 1000L);
            String packageName = "es.rafalense.themes";
            if (BuildConfig.DEBUG) packageName = "es.rafalense.themes.beta";
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (intent != null) {
                return;
            } else {
                SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
                //long last = preferences.getLong("lastTimeActionDone", 0);
                //Log.e("checkForThemes0", ":lastCheck:" + lastCheck);
                //Log.e("checkForThemes0", System.currentTimeMillis() - lastCheck + ":" + myDelay);
                if (lastCheck < 0 || (System.currentTimeMillis() - lastCheck < myDelay && lastCheck > 0)) {
                    //lastCheck++;
                    lastCheck = preferences.getLong("lastTime", 0);
                    //Log.e("checkForThemes1", ":lastCheck:" + lastCheck);
                    return;
                } else {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putLong("lastTime", System.currentTimeMillis());
                    editor.apply();
//                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
//                    builder.setTitle(LocaleController.getString("Themes", R.string.Themes));
//                    builder.setMessage(LocaleController.getString("ThemesAppMsg", R.string.ThemesAppMsg));
//                    final String pck = packageName;
//                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//                            AndroidUtilities.runOnUIThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    try{
//                                        Intent in = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pck));
//                                        if (BuildConfig.DEBUG)in = new Intent(Intent.ACTION_VIEW, Uri.parse("https://rink.hockeyapp.net/apps/b5860b775ca122d3335685f39917e68f"));
//                                        context.startActivityForResult(in, 503);
//                                    } catch (Exception e) {
//                                        Intent in = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=es.rafalense.themes"));
//                                        context.startActivityForResult(in, 503);
//                                        FileLog.e( e);
//                                    }
//                                }
//                            });
//                        }
//                    });
//                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
//                    builder.create().show();
                    lastCheck = preferences.getLong("lastTime", 0);
                    //Log.e("checkForThemes2", ":lastCheck:" + lastCheck);
                }
            }

        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    /*
        static void modifyXMLfile(File preffile,String sname){
            try {
                File file = preffile;
                //Log.e("modifyXMLfile",preffile.getAbsolutePath());
                //Log.e("modifyXMLfile",preffile.exists()+"");
                List<String> lines = new ArrayList<String>();
                // first, read the file and store the changes
                BufferedReader in = new BufferedReader(new FileReader(file));
                String line = in.readLine();
                while (line != null) {
                    if (!line.contains(sname))lines.add(line);
                    //Log.e("modifyXMLfile",line);
                    line = in.readLine();
                }
                in.close();
                // now, write the file again with the changes
                PrintWriter out = new PrintWriter(file);
                for (String l : lines)
                    out.println(l);
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/


    public static void openForView(TLObject media, Activity activity) throws Exception {
        if (media == null || activity == null) {
            return;
        }
        String fileName = FileLoader.getAttachFileName(media);
        File f = FileLoader.getPathToAttach(media, true);
        if (f != null && f.exists()) {
            String realMimeType = null;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            MimeTypeMap myMime = MimeTypeMap.getSingleton();
            int idx = fileName.lastIndexOf('.');
            if (idx != -1) {
                String ext = fileName.substring(idx + 1);
                realMimeType = myMime.getMimeTypeFromExtension(ext.toLowerCase());
                if (realMimeType == null) {
                    if (media instanceof TLRPC.TL_document) {
                        realMimeType = ((TLRPC.TL_document) media).mime_type;
                    }
                    if (realMimeType == null || realMimeType.length() == 0) {
                        realMimeType = null;
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= 24) {
                intent.setDataAndType(FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", f), realMimeType != null ? realMimeType : "text/plain");
            } else {
                intent.setDataAndType(Uri.fromFile(f), realMimeType != null ? realMimeType : "text/plain");
            }
            if (realMimeType != null) {
                try {
                    activity.startActivityForResult(intent, 500);
                } catch (Exception e) {
                    if (Build.VERSION.SDK_INT >= 24) {
                        intent.setDataAndType(FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", f), "text/plain");
                    } else {
                        intent.setDataAndType(Uri.fromFile(f), "text/plain");
                    }
                    activity.startActivityForResult(intent, 500);
                }
            } else {
                activity.startActivityForResult(intent, 500);
            }
        }
    }

    public static void openForView(MessageObject message, Activity activity) throws Exception {
        File f = null;
        String fileName = message.getFileName();
        if (message.messageOwner.attachPath != null && message.messageOwner.attachPath.length() != 0) {
            f = new File(message.messageOwner.attachPath);
        }
        if (f == null || !f.exists()) {
            f = FileLoader.getPathToMessage(message.messageOwner);
        }
        if (f != null && f.exists()) {
            String realMimeType = null;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            MimeTypeMap myMime = MimeTypeMap.getSingleton();
            int idx = fileName.lastIndexOf('.');
            if (idx != -1) {
                String ext = fileName.substring(idx + 1);
                realMimeType = myMime.getMimeTypeFromExtension(ext.toLowerCase());
                if (realMimeType == null) {
                    if (message.type == 9 || message.type == 0) {
                        realMimeType = message.getDocument().mime_type;
                    }
                    if (realMimeType == null || realMimeType.length() == 0) {
                        realMimeType = null;
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= 24) {
                intent.setDataAndType(FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", f), realMimeType != null ? realMimeType : "text/plain");
            } else {
                intent.setDataAndType(Uri.fromFile(f), realMimeType != null ? realMimeType : "text/plain");
            }
            if (realMimeType != null) {
                try {
                    activity.startActivityForResult(intent, 500);
                } catch (Exception e) {
                    if (Build.VERSION.SDK_INT >= 24) {
                        intent.setDataAndType(FileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".provider", f), "text/plain");
                    } else {
                        intent.setDataAndType(Uri.fromFile(f), "text/plain");
                    }
                    activity.startActivityForResult(intent, 500);
                }
            } else {
                activity.startActivityForResult(intent, 500);
            }
        }
    }

    public static void setRectToRect(Matrix matrix, RectF src, RectF dst, int rotation, Matrix.ScaleToFit align) {
        float tx, sx;
        float ty, sy;
        if (rotation == 90 || rotation == 270) {
            sx = dst.height() / src.width();
            sy = dst.width() / src.height();
        } else {
            sx = dst.width() / src.width();
            sy = dst.height() / src.height();
        }
        if (align != Matrix.ScaleToFit.FILL) {
            if (sx > sy) {
                sx = sy;
            } else {
                sy = sx;
            }
        }
        tx = -src.left * sx;
        ty = -src.top * sy;

        matrix.setTranslate(dst.left, dst.top);
        if (rotation == 90) {
            matrix.preRotate(90);
            matrix.preTranslate(0, -dst.width());
        } else if (rotation == 180) {
            matrix.preRotate(180);
            matrix.preTranslate(-dst.width(), -dst.height());
        } else if (rotation == 270) {
            matrix.preRotate(270);
            matrix.preTranslate(-dst.height(), 0);
        }

        matrix.preScale(sx, sy);
        matrix.preTranslate(tx, ty);
    }

    public static File generatePictureInCachePath() {
        try {
            return new File(getCacheDir(), "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg");
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }

    public static void setViewPagerEdgeEffectColor(ViewPager viewPager, int color) {
        if (Build.VERSION.SDK_INT >= 21) {
            try {
                Field field = ViewPager.class.getDeclaredField("mLeftEdge");
                field.setAccessible(true);
                EdgeEffectCompat mLeftEdge = (EdgeEffectCompat) field.get(viewPager);
                if (mLeftEdge != null) {
                    field = EdgeEffectCompat.class.getDeclaredField("mEdgeEffect");
                    field.setAccessible(true);
                    EdgeEffect mEdgeEffect = (EdgeEffect) field.get(mLeftEdge);
                    if (mEdgeEffect != null) {
                        mEdgeEffect.setColor(color);
                    }
                }

                field = ViewPager.class.getDeclaredField("mRightEdge");
                field.setAccessible(true);
                EdgeEffectCompat mRightEdge = (EdgeEffectCompat) field.get(viewPager);
                if (mRightEdge != null) {
                    field = EdgeEffectCompat.class.getDeclaredField("mEdgeEffect");
                    field.setAccessible(true);
                    EdgeEffect mEdgeEffect = (EdgeEffect) field.get(mRightEdge);
                    if (mEdgeEffect != null) {
                        mEdgeEffect.setColor(color);
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    private static ContentObserver callLogContentObserver;
    private static Runnable unregisterRunnable;
    private static boolean hasCallPermissions = Build.VERSION.SDK_INT >= 23;

    @SuppressWarnings("unchecked")
    public static void endIncomingCall() {
        if (!hasCallPermissions) {
            return;
        }
        try {
            TelephonyManager tm = (TelephonyManager) ApplicationLoader.applicationContext.getSystemService(Context.TELEPHONY_SERVICE);
            Class c = Class.forName(tm.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            ITelephony telephonyService = (ITelephony) m.invoke(tm);
            telephonyService = (ITelephony) m.invoke(tm);
            telephonyService.silenceRinger();
            telephonyService.endCall();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }


    public static boolean checkPhonePattern(String pattern, String phone) {
        if (TextUtils.isEmpty(pattern) || pattern.equals("*")) {
            return true;
        }
        String args[] = pattern.split("\\*");
        phone = PhoneFormat.stripExceptNumbers(phone);
        int checkStart = 0;
        int index;
        for (int a = 0; a < args.length; a++) {
            String arg = args[a];
            if (!TextUtils.isEmpty(arg)) {
                if ((index = phone.indexOf(arg, checkStart)) == -1) {
                    return false;
                }
                checkStart = index + arg.length();
            }
        }
        return true;
    }


    public static String obtainLoginPhoneCall(String pattern) {
        if (!hasCallPermissions) {
            return null;
        }
        Cursor cursor = null;
        try {
            cursor = ApplicationLoader.applicationContext.getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    new String[]{CallLog.Calls.NUMBER, CallLog.Calls.DATE},
                    CallLog.Calls.TYPE + " IN (" + CallLog.Calls.MISSED_TYPE + "," + CallLog.Calls.INCOMING_TYPE + "," + CallLog.Calls.REJECTED_TYPE + ")",
                    null,
                    "date DESC LIMIT 5");
            while (cursor.moveToNext()) {
                String number = cursor.getString(0);
                long date = cursor.getLong(1);
                FileLog.e("number = " + number);
                if (Math.abs(System.currentTimeMillis() - date) >= 60 * 60 * 1000) {
                    continue;
                }
                if (checkPhonePattern(pattern, number)) {
                    return number;
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private static void registerLoginContentObserver(boolean shouldRegister, final String number) {
        if (shouldRegister) {
            if (callLogContentObserver != null) {
                return;
            }
            ApplicationLoader.applicationContext.getContentResolver().registerContentObserver(
                    android.provider.CallLog.Calls.CONTENT_URI,
                    true,
                    callLogContentObserver = new ContentObserver(new Handler()) {
                        @Override
                        public boolean deliverSelfNotifications() {
                            return true;
                        }

                        @Override
                        public void onChange(boolean selfChange) {
                            registerLoginContentObserver(false, number);
                            removeLoginPhoneCall(number, false);
                        }
                    });
            runOnUIThread(unregisterRunnable = new Runnable() {
                @Override
                public void run() {
                    unregisterRunnable = null;
                    registerLoginContentObserver(false, number);
                }
            }, 10000);
        } else {
            if (callLogContentObserver == null) {
                return;
            }
            if (unregisterRunnable != null) {
                cancelRunOnUIThread(unregisterRunnable);
                unregisterRunnable = null;
            }
            try {
                ApplicationLoader.applicationContext.getContentResolver().unregisterContentObserver(callLogContentObserver);
            } catch (Exception ignore) {

            } finally {
                callLogContentObserver = null;
            }
        }
    }

    public static void removeLoginPhoneCall(String number, boolean first) {
        if (!hasCallPermissions) {
            return;
        }
        Cursor cursor = null;
        try {
            cursor = ApplicationLoader.applicationContext.getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    new String[]{CallLog.Calls._ID, CallLog.Calls.NUMBER},
                    CallLog.Calls.TYPE + " IN (" + CallLog.Calls.MISSED_TYPE + "," + CallLog.Calls.INCOMING_TYPE + "," + CallLog.Calls.REJECTED_TYPE + ")",
                    null,
                    "date DESC LIMIT 5");
            boolean removed = false;
            while (cursor.moveToNext()) {
                String phone = cursor.getString(1);
                if (phone.contains(number) || number.contains(phone)) {
                    removed = true;
                    ApplicationLoader.applicationContext.getContentResolver().delete(
                            CallLog.Calls.CONTENT_URI,
                            CallLog.Calls._ID + " = ? ",
                            new String[]{String.valueOf(cursor.getInt(0))});
                    break;
                }
            }
            if (!removed && first) {
                registerLoginContentObserver(true, number);
            }
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }


    public static String replacePersianNumber(String paramString) {
        return paramString.replaceAll("0", "\u0660").replaceAll("1", "\u06f1").replaceAll("2", "\u06f2").replaceAll("3", "\u06f3").replaceAll("4", "\u06f4").replaceAll("5", "\u06f5").replaceAll("6", "\u06f6").replaceAll("7", "\u06f7").replaceAll("8", "\u06f8").replaceAll("9", "\u06f9");
    }


}
