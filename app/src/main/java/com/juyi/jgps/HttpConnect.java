package com.juyi.jgps;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * Created by Juyi on 2017/11/29.
 */

public class HttpConnect {

    //URL url = new URL("http://www.android.com/");
    URL url = new URL("http://140.113.193.2/");
    String urlParameters  = "Latitude=123&Longitude=456&Accuracy=16&Altitude=130&Speed=60&Bearing=90";

    byte[] postData = urlParameters.getBytes( StandardCharsets.UTF_8 );
    int postDataLength = postData.length;

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();


    public HttpConnect() throws IOException {
    }
}
