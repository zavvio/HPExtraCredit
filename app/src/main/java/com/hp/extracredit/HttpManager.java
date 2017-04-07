package com.hp.extracredit;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class HttpManager {
    private static String auth_token = "Basic ZDg1NGR4NG5taWJydXdqNXI2aXR5aWdmbGMyZWwxMzY6Ykh6Sks0Mkx0WEtoNkkxUFJNNHVnbmVhSWoyUUpHZ1U=";

    public static String getData(String uri) {
        BufferedReader reader = null;
        try {
            URL url = new URL(uri);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

            StringBuilder sb = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {
                    return null;
                }
            }
        }
    }

    public static String getAccessToken(String uri) {
        BufferedReader reader = null;
        try {
            //URL url = new URL("https://www.livepaperapi.com/auth/v2/token/");
            URL url = new URL(uri);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            con.setRequestProperty("User-Agent", "hp-extra-credit-v0.1");
            con.setRequestProperty("Authorization", auth_token);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestMethod("POST");

            String str = "grant_type=client_credentials&scope=default";
            byte[] outputInBytes = str.getBytes("UTF-8");
            OutputStream os = con.getOutputStream();
            os.write( outputInBytes );
            os.close();

            StringBuilder sb = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ex) {
                    return null;
                }
            }
        }
    }
}
