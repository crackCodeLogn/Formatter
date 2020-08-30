package com.vv.export.formatter.util;

import android.content.Context;
import android.widget.Toast;

/**
 * @author Vivek Verma
 * @since 30/8/20
 */
public class Helper {

    private static void toaster(Context context, String msg, Integer duration) {
        Toast.makeText(context, msg, duration).show();
    }

    public static void shortToaster(Context context, String msg) {
        toaster(context, msg, Toast.LENGTH_SHORT);
    }

    public static void longToaster(Context context, String msg) {
        toaster(context, msg, Toast.LENGTH_LONG);
    }
}
