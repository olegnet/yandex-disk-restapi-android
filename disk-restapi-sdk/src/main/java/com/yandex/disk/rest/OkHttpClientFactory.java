/*
* (C) 2015 Yandex LLC (https://yandex.com/)
*
* The source code of Java SDK for Yandex.Disk REST API
* is available to use under terms of Apache License,
* Version 2.0. See the file LICENSE for the details.
*/

package com.yandex.disk.rest;

import okhttp3.OkHttpClient;

import static java.util.concurrent.TimeUnit.SECONDS;

class OkHttpClientFactory {

    static OkHttpClient.Builder makeClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(30, SECONDS);
        builder.readTimeout(30, SECONDS);
        builder.writeTimeout(30, SECONDS);
        builder.followSslRedirects(true);
        builder.followRedirects(true);
        return builder;
    }
}
