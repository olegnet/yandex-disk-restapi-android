/*
* (C) 2015 Yandex LLC (https://yandex.com/)
*
* The source code of Java SDK for Yandex.Disk REST API
* is available to use under terms of Apache License,
* Version 2.0. See the file LICENSE for the details.
*/

package com.yandex.disk.rest.retrofit;

import com.yandex.disk.rest.CustomHeader;

import java.io.IOException;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class RequestInterceptor implements Interceptor {

    private final List<CustomHeader> headers;

    public RequestInterceptor(final List<CustomHeader> headers) {
        this.headers = headers;
    }

    @Override
    public Response intercept(final Interceptor.Chain chain) throws IOException {
        Request original = chain.request();
        Request.Builder builder = original.newBuilder();
        for (CustomHeader header : headers) {
            builder.addHeader(header.getName(), header.getValue());
        }
        Request request = builder.method(original.method(), original.body())
                .build();
        return chain.proceed(request);
    }
}