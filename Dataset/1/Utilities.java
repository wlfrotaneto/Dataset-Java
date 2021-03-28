/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package org.telegram.messenger;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Base64;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import org.telegram.TL.TLClassStore;
import org.telegram.TL.TLObject;
import org.telegram.ui.ApplicationLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

import javax.crypto.Cipher;

public class Utilities {
    public static Handler applicationHandler;
    public static int statusBarHeight = 0;
    public static float density = 1;
    private final static Integer lock = 1;

    public static ArrayList<String> goodPrimes = new ArrayList<String>();

    public static class TPFactorizedValue {
        public long p, q;
    }

    public static DispatchQueue stageQueue = new DispatchQueue("stageQueue");
    public static DispatchQueue globalQueue = new DispatchQueue("globalQueue");
    public static DispatchQueue cacheOutQueue = new DispatchQueue("cacheOutQueue");
    public static DispatchQueue imageLoadQueue = new DispatchQueue("imageLoadQueue");
    public static DispatchQueue fileUploadQueue = new DispatchQueue("fileUploadQueue");

    public native static long doPQNative(long _what);
    public native static byte[] aesIgeEncryption(byte[] _what, byte[] _key, byte[] _iv, boolean encrypt, boolean changeIv);

    static {
        density = ApplicationLoader.applicationContext.getResources().getDisplayMetrics().density;
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("primes", Context.MODE_PRIVATE);
        String primes = preferences.getString("primes", null);
        if (primes == null) {
            goodPrimes.add("C71CAEB9C6B1C9048E6C522F70F13F73980D40238E3E21C14934D037563D930F48198A0AA7C14058229493D22530F4DBFA336F6E0AC925139543AED44CCE7C3720FD51F69458705AC68CD4FE6B6B13ABDC9746512969328454F18FAF8C595F642477FE96BB2A941D5BCD1D4AC8CC49880708FA9B378E3C4F3A9060BEE67CF9A4A4A695811051907E162753B56B0F6B410DBA74D8A84B2A14B3144E0EF1284754FD17ED950D5965B4B9DD46582DB1178D169C6BC465B0D6FF9CA3928FEF5B9AE4E418FC15E83EBEA0F87FA9FF5EED70050DED2849F47BF959D956850CE929851F0D8115F635B105EE2E4E15D04B2454BF6F4FADF034B10403119CD8E3B92FCC5B");
        } else {
            try {
                byte[] bytes = Base64.decode(primes, Base64.DEFAULT);
                if (bytes != null) {
                    SerializedData data = new SerializedData(bytes);
                    int count = data.readInt32();
                    for (int a = 0; a < count; a++) {
                        goodPrimes.add(data.readString());
                    }
                }
            } catch (Exception e) {
                FileLog.e("tmessages", e);
                goodPrimes.clear();
                goodPrimes.add("C71CAEB9C6B1C9048E6C522F70F13F73980D40238E3E21C14934D037563D930F48198A0AA7C14058229493D22530F4DBFA336F6E0AC925139543AED44CCE7C3720FD51F69458705AC68CD4FE6B6B13ABDC9746512969328454F18FAF8C595F642477FE96BB2A941D5BCD1D4AC8CC49880708FA9B378E3C4F3A9060BEE67CF9A4A4A695811051907E162753B56B0F6B410DBA74D8A84B2A14B3144E0EF1284754FD17ED950D5965B4B9DD46582DB1178D169C6BC465B0D6FF9CA3928FEF5B9AE4E418FC15E83EBEA0F87FA9FF5EED70050DED2849F47BF959D956850CE929851F0D8115F635B105EE2E4E15D04B2454BF6F4FADF034B10403119CD8E3B92FCC5B");
            }
        }
        System.loadLibrary("tmessages");
    }

    static final Class<?>[] constructorSignature = new Class[] {Context.class, AttributeSet.class};

    public static int externalCacheNotAvailableState = 0;
    public static File getCacheDir() {
        if (externalCacheNotAvailableState == 1 || externalCacheNotAvailableState == 0 && Environment.getExternalStorageState().startsWith(Environment.MEDIA_MOUNTED)) {
            externalCacheNotAvailableState = 1;
            return ApplicationLoader.applicationContext.getExternalCacheDir();
        }
        externalCacheNotAvailableState = 2;
        return ApplicationLoader.applicationContext.getCacheDir();
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static int dp(int value) {
        return (int)(density * value);
    }

    public static boolean isGoodPrime(byte[] prime, int g) {
        if (!(g >= 2 && g <= 7)) {
            return false;
        }

        if (prime.length != 256 || prime[0] >= 0) {
            return false;
        }

        String hex = bytesToHex(prime);
        for (String cached : goodPrimes) {
            if (cached.equals(hex)) {
                return true;
            }
        }

        BigInteger dhBI = new BigInteger(1, prime);

        if (g == 2) { // p mod 8 = 7 for g = 2;
            BigInteger res = dhBI.mod(BigInteger.valueOf(8));
            if (res.intValue() != 7) {
                return false;
            }
        } else if (g == 3) { // p mod 3 = 2 for g = 3;
            BigInteger res = dhBI.mod(BigInteger.valueOf(3));
            if (res.intValue() != 2) {
                return false;
            }
        } else if (g == 5) { // p mod 5 = 1 or 4 for g = 5;
            BigInteger res = dhBI.mod(BigInteger.valueOf(5));
            int val = res.intValue();
            if (val != 1 && val != 4) {
                return false;
            }
        } else if (g == 6) { // p mod 24 = 19 or 23 for g = 6;
            BigInteger res = dhBI.mod(BigInteger.valueOf(24));
            int val = res.intValue();
            if (val != 19 && val != 23) {
                return false;
            }
        } else if (g == 7) { // p mod 7 = 3, 5 or 6 for g = 7.
            BigInteger res = dhBI.mod(BigInteger.valueOf(7));
            int val = res.intValue();
            if (val != 3 && val != 5 && val != 6) {
                return false;
            }
        }

        BigInteger dhBI2 = dhBI.subtract(BigInteger.valueOf(1)).divide(BigInteger.valueOf(2));
        if (!dhBI.isProbablePrime(30) || !dhBI2.isProbablePrime(30)) {
            return false;
        }

        goodPrimes.add(hex);

        globalQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    SerializedData data = new SerializedData();
                    data.writeInt32(goodPrimes.size());
                    for (String pr : goodPrimes) {
                        data.writeString(pr);
                    }
                    byte[] bytes = data.toByteArray();
                    SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("primes", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("primes", Base64.encodeToString(bytes, Base64.DEFAULT));
                    editor.commit();
                } catch (Exception e) {
                    FileLog.e("tmessages", e);
                }
            }
        });

        return true;
    }

    public static boolean isGoodGaAndGb(BigInteger g_a, BigInteger p) {
        return !(g_a.compareTo(BigInteger.valueOf(1)) != 1 || g_a.compareTo(p.subtract(BigInteger.valueOf(1))) != -1);
    }

    public static TPFactorizedValue getFactorizedValue(long what) {
        long g = doPQNative(what);
        if (g > 1 && g < what) {
            long p1 = g;
            long p2 = what / g;
            if (p1 > p2) {
                long tmp = p1;
                p1 = p2;
                p2 = tmp;
            }

            TPFactorizedValue result = new TPFactorizedValue();
            result.p = p1;
            result.q = p2;

            return result;
        } else {
            FileLog.e("tmessages", String.format("**** Factorization failed for %d", what));
            TPFactorizedValue result = new TPFactorizedValue();
            result.p = 0;
            result.q = 0;
            return result;
        }
    }

    public static byte[] computeSHA1(byte[] convertme, int offset, int len) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(convertme, offset, len);
            return md.digest();
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
        return null;
    }

    public static byte[] computeSHA1(byte[] convertme) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return md.digest(convertme);
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
        return null;
    }

    public static byte[] encryptWithRSA(BigInteger[] key, byte[] data) {
        try {
            KeyFactory fact = KeyFactory.getInstance("RSA");
            RSAPublicKeySpec keySpec = new RSAPublicKeySpec(key[0], key[1]);
            PublicKey publicKey = fact.generatePublic(keySpec);
            final Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
        return null;
    }

    public static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(x);
        return buffer.array();
    }

    public static long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getLong();
    }

    public static int bytesToInt(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getInt();
    }

    public static MessageKeyData generateMessageKeyData(byte[] authKey, byte[] messageKey, boolean incoming) {
        MessageKeyData keyData = new MessageKeyData();
        if (authKey == null || authKey.length == 0) {
            keyData.aesIv = null;
            keyData.aesKey = null;
            return keyData;
        }

        int x = incoming ? 8 : 0;

        SerializedData data = new SerializedData();
        data.writeRaw(messageKey);
        data.writeRaw(authKey, x, 32);
        byte[] sha1_a = Utilities.computeSHA1(data.toByteArray());

        data = new SerializedData();
        data.writeRaw(authKey, 32 + x, 16);
        data.writeRaw(messageKey);
        data.writeRaw(authKey, 48 + x, 16);
        byte[] sha1_b = Utilities.computeSHA1(data.toByteArray());

        data = new SerializedData();
        data.writeRaw(authKey, 64 + x, 32);
        data.writeRaw(messageKey);
        byte[] sha1_c = Utilities.computeSHA1(data.toByteArray());

        data = new SerializedData();
        data.writeRaw(messageKey);
        data.writeRaw(authKey, 96 + x, 32);
        byte[] sha1_d = Utilities.computeSHA1(data.toByteArray());

        SerializedData aesKey = new SerializedData();
        aesKey.writeRaw(sha1_a, 0, 8);
        aesKey.writeRaw(sha1_b, 8, 12);
        aesKey.writeRaw(sha1_c, 4, 12);
        keyData.aesKey = aesKey.toByteArray();

        SerializedData aesIv = new SerializedData();
        aesIv.writeRaw(sha1_a, 8, 12);
        aesIv.writeRaw(sha1_b, 0, 8);
        aesIv.writeRaw(sha1_c, 16, 4);
        aesIv.writeRaw(sha1_d, 0, 8);
        keyData.aesIv = aesIv.toByteArray();

        return keyData;
    }

    public static TLObject decompress(byte[] data, TLObject parentObject) {
        final int BUFFER_SIZE = 512;
        ByteArrayInputStream is = new ByteArrayInputStream(data);
        GZIPInputStream gis;
        try {
            gis = new GZIPInputStream(is, BUFFER_SIZE);
            ByteArrayOutputStream bytesOutput = new ByteArrayOutputStream();
            data = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = gis.read(data)) != -1) {
                bytesOutput.write(data, 0, bytesRead);
            }
            gis.close();
            is.close();
            SerializedData stream = new SerializedData(bytesOutput.toByteArray());
            return TLClassStore.Instance().TLdeserialize(stream, stream.readInt32(), parentObject);
        } catch (IOException e) {
            FileLog.e("tmessages", e);
        }
        return null;
    }

    private static final String TAG = "Typefaces";
    private static final Hashtable<String, Typeface> cache = new Hashtable<String, Typeface>();

    public static Typeface getTypeface(String assetPath) {
        synchronized (cache) {
            if (!cache.containsKey(assetPath)) {
                try {
                    Typeface t = Typeface.createFromAsset(ApplicationLoader.applicationContext.getAssets(),
                            assetPath);
                    cache.put(assetPath, t);
                } catch (Exception e) {
                    FileLog.e(TAG, "Could not get typeface '" + assetPath + "' because " + e.getMessage());
                    return null;
                }
            }
            return cache.get(assetPath);
        }
    }

    public static void showKeyboard(View view) {
        if (view == null) {
            return;
        }
        InputMethodManager inputManager = (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);

        ((InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(view, 0);
    }

    public static boolean isKeyboardShowed(View view) {
        if (view == null) {
            return false;
        }
        InputMethodManager inputManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        return inputManager.isActive(view);
    }

    public static void hideKeyboard(View view) {
        if (view == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (!imm.isActive()) {
            return;
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static ProgressDialog progressDialog;
    public static void ShowProgressDialog(final Activity activity, final String message) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!activity.isFinishing()) {
                    progressDialog = new ProgressDialog(activity);
                    if (message != null) {
                        progressDialog.setMessage(message);
                    }
                    progressDialog.setCanceledOnTouchOutside(false);
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                }
            }
        });
    }

    public static FastDateFormat formatterDay;
    public static FastDateFormat formatterWeek;
    public static FastDateFormat formatterMonth;
    public static FastDateFormat formatterYear;
    public static FastDateFormat formatterYearMax;
    public static FastDateFormat chatDate;
    public static FastDateFormat chatFullDate;

    static {
        Locale locale = Locale.getDefault();
        String lang = locale.getLanguage();
        if (lang.equals("en")) {
            formatterMonth = FastDateFormat.getInstance("MMM dd", locale);
            formatterYear = FastDateFormat.getInstance("dd.MM.yy", locale);
            formatterYearMax = FastDateFormat.getInstance("dd.MM.yyyy", locale);
            chatDate = FastDateFormat.getInstance("MMMM d", locale);
            chatFullDate = FastDateFormat.getInstance("MMMM d, yyyy", locale);
        } else {
            formatterMonth = FastDateFormat.getInstance("dd MMM", locale);
            formatterYear = FastDateFormat.getInstance("dd.MM.yy", locale);
            formatterYearMax = FastDateFormat.getInstance("dd.MM.yyyy", locale);
            chatDate = FastDateFormat.getInstance("d MMMM", locale);
            chatFullDate = FastDateFormat.getInstance("d MMMM yyyy", locale);
        }
        formatterWeek = FastDateFormat.getInstance("EEE", locale);

        if (lang != null) {
            if (DateFormat.is24HourFormat(ApplicationLoader.applicationContext)) {
                formatterDay = FastDateFormat.getInstance("HH:mm", locale);
            } else {
                if (lang.toLowerCase().equals("ar")) {
                    formatterDay = FastDateFormat.getInstance("h:mm a", locale);
                } else {
                    formatterDay = FastDateFormat.getInstance("h:mm a", Locale.US);
                }
            }
        } else {
            formatterDay = FastDateFormat.getInstance("h:mm a", Locale.US);
        }
    }

    public static String formatDateChat(long date) {
        Calendar rightNow = Calendar.getInstance();
        int year = rightNow.get(Calendar.YEAR);

        rightNow.setTimeInMillis(date * 1000);
        int dateYear = rightNow.get(Calendar.YEAR);

        if (year == dateYear) {
            return chatDate.format(date * 1000);
        }
        return chatFullDate.format(date * 1000);
    }

    public static String formatDate(long date) {
        Calendar rightNow = Calendar.getInstance();
        int day = rightNow.get(Calendar.DAY_OF_YEAR);
        int year = rightNow.get(Calendar.YEAR);
        rightNow.setTimeInMillis(date * 1000);
        int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
        int dateYear = rightNow.get(Calendar.YEAR);

        if (dateDay == day && year == dateYear) {
            return formatterDay.format(new Date(date * 1000));
        } else if (dateDay + 1 == day && year == dateYear) {
            return ApplicationLoader.applicationContext.getResources().getString(R.string.Yesterday);
        } else if (year == dateYear) {
            return formatterMonth.format(new Date(date * 1000));
        } else {
            return formatterYear.format(new Date(date * 1000));
        }
    }

    public static String formatDateOnline(long date) {
        Calendar rightNow = Calendar.getInstance();
        int day = rightNow.get(Calendar.DAY_OF_YEAR);
        int year = rightNow.get(Calendar.YEAR);
        rightNow.setTimeInMillis(date * 1000);
        int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
        int dateYear = rightNow.get(Calendar.YEAR);

        if (dateDay == day && year == dateYear) {
            return String.format("%s %s", ApplicationLoader.applicationContext.getResources().getString(R.string.TodayAt), formatterDay.format(new Date(date * 1000)));
        } else if (dateDay + 1 == day && year == dateYear) {
            return String.format("%s %s", ApplicationLoader.applicationContext.getResources().getString(R.string.YesterdayAt), formatterDay.format(new Date(date * 1000)));
        } else if (year == dateYear) {
            return String.format("%s %s %s", formatterMonth.format(new Date(date * 1000)), ApplicationLoader.applicationContext.getResources().getString(R.string.OtherAt), formatterDay.format(new Date(date * 1000)));
        } else {
            return String.format("%s %s %s", formatterYear.format(new Date(date * 1000)), ApplicationLoader.applicationContext.getResources().getString(R.string.OtherAt), formatterDay.format(new Date(date * 1000)));
        }
    }

    public static void HideProgressDialog(Activity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
            }
        });
    }

    public static boolean copyFile(File sourceFile, File destFile) throws IOException {
        if(!destFile.exists()) {
            destFile.createNewFile();
        }
        FileChannel source = null;
        FileChannel destination = null;
        boolean result = true;
        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } catch (Exception e) {
            FileLog.e("tmessages", e);
            result = false;
        } finally {
            if(source != null) {
                source.close();
            }
            if(destination != null) {
                destination.close();
            }
        }
        return result;
    }

    public static void RunOnUIThread(Runnable runnable) {
        synchronized (lock) {
            if (applicationHandler == null) {
                applicationHandler = new Handler(ApplicationLoader.applicationContext.getMainLooper());
            }
            applicationHandler.post(runnable);
        }
    }

    public static int[] arrColors = {0xffee4928, 0xff41a903, 0xffe09602, 0xff0f94ed, 0xff8f3bf7, 0xfffc4380, 0xff00a1c4, 0xffeb7002};
    public static int[] arrUsersAvatars = {
            R.drawable.user_red,
            R.drawable.user_green,
            R.drawable.user_yellow,
            R.drawable.user_blue,
            R.drawable.user_violet,
            R.drawable.user_pink,
            R.drawable.user_aqua,
            R.drawable.user_orange};

    public static int[] arrGroupsAvatars = {
            R.drawable.group_green,
            R.drawable.group_red,
            R.drawable.group_blue,
            R.drawable.group_yellow};

    public static int getColorIndex(int id) {
        int[] arr;
        if (id >= 0) {
            arr = arrUsersAvatars;
        } else {
            arr = arrGroupsAvatars;
        }
        try {
            String str;
            if (id >= 0) {
                str = String.format(Locale.US, "%d%d", id, UserConfig.clientUserId);
            } else {
                str = String.format(Locale.US, "%d", id);
            }
            if (str.length() > 15) {
                str = str.substring(0, 15);
            }
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(str.getBytes());
            int b = digest[Math.abs(id % 16)];
            if (b < 0) {
                b += 256;
            }
            return Math.abs(b) % arr.length;
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
        return id % arr.length;
    }

    public static int getColorForId(int id) {
        if (id == 333000) {
            return 0xff0f94ed;
        }
        return arrColors[getColorIndex(id)];
    }

    public static int getUserAvatarForId(int id) {
        if (id == 333000) {
            return R.drawable.telegram_avatar;
        }
        return arrUsersAvatars[getColorIndex(id)];
    }

    public static int getGroupAvatarForId(int id) {
        return arrGroupsAvatars[getColorIndex(-id)];
    }

    public static String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte anArray : array) {
                sb.append(Integer.toHexString((anArray & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            FileLog.e("tmessages", e);
        }
        return null;
    }

    public static void addMediaToGallery(String fromPath) {
        if (fromPath == null) {
            return;
        }
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(fromPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        ApplicationLoader.applicationContext.sendBroadcast(mediaScanIntent);
    }

    public static void addMediaToGallery(Uri uri) {
        if (uri == null) {
            return;
        }
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(uri);
        ApplicationLoader.applicationContext.sendBroadcast(mediaScanIntent);
    }

    private static File getAlbumDir() {
        File storageDir = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ApplicationLoader.applicationContext.getResources().getString(R.string.AppName));
            if (storageDir != null) {
                if (! storageDir.mkdirs()) {
                    if (! storageDir.exists()){
                        FileLog.d("tmessages", "failed to create directory");
                        return null;
                    }
                }
            }
        } else {
            FileLog.d("tmessages", "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }

    public static File generatePicturePath() {
        try {
            File storageDir = getAlbumDir();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "IMG_" + timeStamp + "_";
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
        return null;
    }

    public static CharSequence generateSearchName(String name, String name2, String q) {
        if (name == null && name2 == null) {
            return "";
        }
        int index;
        SpannableStringBuilder builder = new SpannableStringBuilder();
        String wholeString = name;
        if (wholeString == null || wholeString.length() == 0) {
            wholeString = name2;
        } else if (name2 != null && name2.length() != 0) {
            wholeString += " " + name2;
        }
        wholeString = wholeString.trim();
        String[] args = wholeString.split(" ");

        for (String arg : args) {
            String str = arg;
            if (str != null) {
                String lower = str.toLowerCase();
                if (lower.startsWith(q)) {
                    if (builder.length() != 0) {
                        builder.append(" ");
                    }
                    String query = str.substring(0, q.length());
                    builder.append(Html.fromHtml("<font color=\"#357aa8\">" + query + "</font>"));
                    str = str.substring(q.length());
                    builder.append(str);
                } else {
                    if (builder.length() != 0) {
                        builder.append(" ");
                    }
                    builder.append(str);
                }
            }
        }
        return builder;
    }

    public static File generateVideoPath() {
        try {
            File storageDir = getAlbumDir();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "VID_" + timeStamp + "_";
            return File.createTempFile(imageFileName, ".mp4", storageDir);
            /*

            String fileName = "VID" + id + ".mp4";
            return new File(ApplicationLoader.applicationContext.getCacheDir(), fileName);
             */
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
        return null;
    }

    public static String formatName(String firstName, String lastName) {
        String result = firstName;
        if (result == null || result.length() == 0) {
            result = lastName;
        } else if (result.length() != 0 && lastName.length() != 0) {
            result += " " + lastName;
        }
        return result;
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

    public static String stringForMessageListDate(long date) {
        Calendar rightNow = Calendar.getInstance();
        int day = rightNow.get(Calendar.DAY_OF_YEAR);
        int year = rightNow.get(Calendar.YEAR);
        rightNow.setTimeInMillis(date * 1000);
        int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
        int dateYear = rightNow.get(Calendar.YEAR);

        if (year != dateYear) {
            return formatterYear.format(new Date(date * 1000));
        } else {
            int dayDiff = dateDay - day;
            if(dayDiff == 0 || dayDiff == -1 && (int)(System.currentTimeMillis() / 1000) - date < 60 * 60 * 8) {
                return formatterDay.format(new Date(date * 1000));
            } else if(dayDiff > -7 && dayDiff <= -1) {
                return formatterWeek.format(new Date(date * 1000));
            } else {
                return formatterMonth.format(new Date(date * 1000));
            }
        }
    }
}
