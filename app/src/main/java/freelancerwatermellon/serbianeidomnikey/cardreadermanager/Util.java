package freelancerwatermellon.serbianeidomnikey.cardreadermanager;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

public class Util {
    public static void logDebug(String msg) {
       // if (CardReaderApplication.getContext().getSharedPreferences(Constants.PREFERENCES_NAME, 4).getBoolean(Constants.KEY_ENABLE_DEBUG, false)) {
            log(Thread.currentThread().getStackTrace()[3], msg, 3);
        //}
    }

    public static void logInfo(String msg) {
        log(Thread.currentThread().getStackTrace()[3], msg, 4);
    }

    public static void logWarn(String msg) {
        log(Thread.currentThread().getStackTrace()[3], msg, 5);
    }

    public static void logError(String msg) {
        log(Thread.currentThread().getStackTrace()[3], msg, 6);
    }

    private static void log(StackTraceElement e, String msg, int level) {
        String fullClassName = e.getClassName();
        String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
        String methodName = e.getMethodName();
        Log.println(level, className + ":" + e.getLineNumber(), "[" + Thread.currentThread().getId() + "] (" + methodName + ") " + msg);
    }

    public static byte[] stringToByteArray(String string) {
        if (string.length() % 2 != 0) {
            return null;
        }
        byte[] array = new byte[(string.length() / 2)];
        int i = 0;
        int j = 0;
        while (i < array.length) {
            array[i] = (byte) (((Integer.parseInt(string.charAt(j) + BuildConfig.FLAVOR, 16) & 15) << 4) | (Integer.parseInt(string.charAt(j + 1) + BuildConfig.FLAVOR, 16) & 15));
            i++;
            j += 2;
        }
        return array;
    }

    public static String byteArrayToString(byte[] array) {
        if (array == null) {
            return BuildConfig.FLAVOR;
        }
        String string = BuildConfig.FLAVOR;
        for (byte formatByte : array) {
            string = string + formatByte(formatByte) + " ";
        }
        return string;
    }

    public static String formatByte(int value) {
        return "0x" + Integer.toHexString(value & 255);
    }

    public static Set<String> stringToSet(String string, String separatorRegex) {
        String[] array = string.split(separatorRegex);
        Set<String> set = new HashSet(array.length);
        for (String s : array) {
            set.add(s);
        }
        return set;
    }

    public static String setToString(Set<String> set, String separator) {
        String string = BuildConfig.FLAVOR;
        for (String s : set) {
            string = string + s + separator;
        }
        return string.substring(0, string.length() - 1);
    }

    public static byte lrc(byte[] data) {
        byte lrcSum = (byte) 0;
        for (byte b : data) {
            lrcSum = (byte) (b ^ lrcSum);
        }
        return lrcSum;
    }

    public static CharSequence readEula(InputStream in) throws Throwable {
        CharSequence stringBuilder;
        Throwable th;
        BufferedReader bufferedReader = null;
        try {
            BufferedReader bIn = new BufferedReader(new InputStreamReader(in));
            try {
                stringBuilder = new StringBuilder();
                while (true) {
                    String line = bIn.readLine();
                    if (line == null) {
                        break;
                    }
                    // This shit couldn't compile so I commented it
                    // stringBuilder.append(line + "\n");
                }
                if (bIn != null) {
                    try {
                        bIn.close();
                    } catch (IOException e) {
                    }
                }
                bufferedReader = bIn;
            } catch (IOException e2) {
                bufferedReader = bIn;
                try {
                    stringBuilder = BuildConfig.FLAVOR;
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e3) {
                        }
                    }
                    return stringBuilder;
                } catch (Throwable th2) {
                    th = th2;
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        } catch (IOException e4) {
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                bufferedReader = bIn;
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                throw th;
            }
        } catch (IOException e5) {
            stringBuilder = BuildConfig.FLAVOR;
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            return stringBuilder;
        }
        return stringBuilder;
    }
}
