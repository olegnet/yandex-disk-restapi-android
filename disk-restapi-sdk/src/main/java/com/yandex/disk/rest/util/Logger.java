/*
* (C) 2015 Yandex LLC (https://yandex.com/)
*
* The source code of Java SDK for Yandex.Disk REST API
* is available to use under terms of Apache License,
* Version 2.0. See the file LICENSE for the details.
*/

package com.yandex.disk.rest.util;

public interface Logger {

    boolean isDebugEnabled();

    void debug(String message);

    void debug(String message, Throwable th);

    void info(String message);

    void info(String message, Throwable th);

    void warn(String message);

    void warn(String message, Throwable th);

    void error(String message);

    void error(String message, Throwable th);
}
