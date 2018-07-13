package module.nrlwallet.com.nrlwalletsdk.Utils;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

import static android.content.Context.FINGERPRINT_SERVICE;

public class Util
{
    public static final String TAG = Util.class.getName();

    public static byte[] concat(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    public static String toBitString(final byte[] b) {
        final char[] bits = new char[8 * b.length];
        for (int i = 0; i < b.length; i++) {
            final byte byteval = b[i];
            int bytei = i << 3;
            int mask = 0x1;
            for (int j = 7; j >= 0; j--) {
                final int bitval = byteval & mask;
                if (bitval == 0) {
                    bits[bytei + j] = '0';
                } else {
                    bits[bytei + j] = '1';
                }
                mask <<= 1;
            }
        }
        return String.valueOf(bits);
    }

    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isNullOrEmpty(byte[] arr) {
        return arr == null || arr.length == 0;
    }

    public static boolean isNullOrEmpty(Collection collection) {
        return collection == null || collection.size() == 0;
    }

    public static int getPixelsFromDps(Context context, int dps) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dps * scale + 0.5f);
    }

    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    public static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String getAgentString(Context app, String cfnetwork) {

        int versionNumber = 0;
        if (app != null) {
            try {
                PackageInfo pInfo = null;
                pInfo = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
                versionNumber = pInfo.versionCode;

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        String release = Build.VERSION.RELEASE;
//        return String.format("%s/%d %s %s/%s", "Bread", versionNumber, cfnetwork, "Android", release);
        return "Bread/" + String.valueOf(versionNumber) + " " + cfnetwork + " Android/" + release;
    }

    public static String reverseHex(String hex) {
        if (hex == null) return null;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i <= hex.length() - 2; i = i + 2) {
            result.append(new StringBuilder(hex.substring(i, i + 2)).reverse());
        }
        return result.reverse().toString();
    }

    public static void correctTextSizeIfNeeded(TextView v) {
        int limit = 100;
        int lines = v.getLineCount();
        float px = v.getTextSize();
        while (lines > 1 && !v.getText().toString().contains("\n")) {
            limit--;
            px -= 1;
            v.setTextSize(TypedValue.COMPLEX_UNIT_PX, px);
            lines = v.getLineCount();
            if (limit <= 0) {
                Log.e(TAG, "correctTextSizeIfNeeded: Failed to rescale, limit reached, final: " + px);
                break;
            }
        }
    }
}
