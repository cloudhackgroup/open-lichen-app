package org.cloudhack.openlichen.services;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class RestApi extends AsyncTask<String, String, Void> {
    private static final String TAG = "RestApi";
    private static final String host = "openlichen.herokuapp.com";
    //private static final String port = "80";
    private static final String version = "v2";

    private static final String urlString = "http://" + host + /*":" + port + */ "/report" + version;

    @Override
    protected Void doInBackground(String... params) {
        String data = params[0]; //data to post
        OutputStream out = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(false);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty( "Content-Type", "application/json");

            out = new BufferedOutputStream(urlConnection.getOutputStream());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
            writer.write(data);
            writer.flush();
            writer.close();
            out.close();
            urlConnection.connect();
            int code = urlConnection.getResponseCode();
            if(code >= 200 && code < 300){
                Log.d(TAG, "Send succesfull !");
            } else {
                Log.e(TAG, "Send to server error");
            }
            urlConnection.disconnect();
        } catch (IOException e) {
            Log.e(TAG, "Send to server error", e);
        }
        return null;
    }
}
