/*
* (C) 2015 Yandex LLC (https://yandex.com/)
*
* The source code of Java SDK for Yandex.Disk REST API
* is available to use under terms of Apache License,
* Version 2.0. See the file LICENSE for the details.
*/

package com.yandex.disk.rest.util;

import android.util.Log;

import com.yandex.android.rest.BuildConfig;

public class LoggerFactory {

    public static Logger getLogger(final Class clazz) {
        final String tag = clazz.getSimpleName();
        return new Logger() {

            @Override
            public boolean isDebugEnabled() {
                return BuildConfig.DEBUG;
            }

            @Override
            public void debug(String message) {
                Log.d(tag, message);
            }

            @Override
            public void debug(String message, Throwable throwable) {
                Log.d(tag, message, throwable);
            }

            @Override
            public void info(String message) {
                Log.d(tag, message);
            }

            @Override
            public void info(String message, Throwable throwable) {
                Log.i(tag, message, throwable);
            }

            @Override
            public void warn(String message) {
                Log.w(tag, message);
            }

            @Override
            public void warn(String message, Throwable throwable) {
                Log.w(tag, message, throwable);
            }

            @Override
            public void error(String message) {
                Log.e(tag, message);
            }

            @Override
            public void error(String message, Throwable throwable) {
                Log.e(tag, message, throwable);
            }
        };
    }
}
