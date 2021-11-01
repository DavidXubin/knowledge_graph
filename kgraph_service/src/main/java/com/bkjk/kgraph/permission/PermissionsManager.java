package com.bkjk.kgraph.permission;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class PermissionsManager {

    private static Logger logger = LoggerFactory.getLogger(PermissionsManager.class);

    private static final Gson GSON = new Gson();

    @Value("${permission.url}")
    private String url;

    public MetaResult validate(String user, String sql, String type, String platform) {
        StringBuffer response = new StringBuffer();

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8");
            String sqlEncoded = URLEncoder.encode(sql, "UTF-8");
            String urlParameters = "user=" + user + "&sql=" + sqlEncoded + "&type=" + type + "&platform=" + platform;
            logger.info("=====urlParameters=====" + urlParameters);
            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(45000);
            connection.setRequestProperty("Content-Length", postData.length + "");

            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.write(postData);
            }
            if (connection.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");
                    }
                    MetaResult result = GSON.fromJson(response.toString(), MetaResult.class);

                    if (result != null) {
                        return result;
                    } else {
                        return new MetaResult(-1, String.format(
                                "call permissions application error it return wrong format data ." +
                                        " please contact admin (G-DATA-INFRA@bkjk.com) , response: {%s} ",
                                response.toString()));
                    }
                }
            } else {
                return new MetaResult(-2, String.format(
                        "call permissions application error it return error http respond code . " +
                                "please contact admin (G-DATA-INFRA@bkjk.com). return code is {%s} ",
                        connection.getResponseCode()));
            }
        } catch (Exception e) {
            logger.error(String.format(
                    "call permissions application error, " +
                            "please contact admin (G-DATA-INFRA@bkjk.com). url : %s", url), e);
            return new MetaResult(-3, String.format("call permissions application error, " +
                            "please contact admin (G-DATA-INFRA@bkjk.com). url : %s . errorMessage: %s ",
                    url, e.getMessage()));
        }

    }

}
