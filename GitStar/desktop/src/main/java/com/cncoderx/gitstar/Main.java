package com.cncoderx.gitstar;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import sun.misc.BASE64Encoder;

/**
 * @author cncoderx
 */
public class Main {
    static final String URL_LOGIN = "http://gitstar.top:88/api/user/login";
    static final String URL_RECOMMEND = "http://gitstar.top:88/api/users/%s/status/recommend";
    static final String URL_STAR = "https://api.github.com/user/starred/%s";

    static String gitstarUser = "";
    static String gitstarPwd = "";
    static String githubUser = "";
    static String githubPwd = "";

    static HttpClient httpClient;
    static String basicAuth = "";

    public static void main(String[] args) throws IOException {
        loadSetting();
        String cookie = login();
        if (cookie != null) {
            Map<String, String> result = getRepoMap(cookie);
            Set<Map.Entry<String, String>> entries = result.entrySet();
            int size = entries.size();
            for (Map.Entry<String, String> entry : entries) {
                String key = entry.getKey();
                String value = entry.getValue();
                star(value);
                System.out.println(String.format("\u5df2\u5bf9\"%s\"\u70b9\u8d5e", value));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(String.format("\u4e00\u5171\u70b9\u8d5e\u4e86%d\u4e2a\u9879\u76ee\uff0c\u6309\u3010Enter\u3011\u7ed3\u675f", size));
            Scanner scanner = new Scanner(System.in);
            if (scanner.nextLine().equals("\n")) {
                System.exit(0);
            }
        }
    }

    private static boolean loadSetting() {
        Properties props = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream("setting.properties");
            props.load(input);
            gitstarUser = props.getProperty("gitstarUser");
            gitstarPwd = props.getProperty("gitstarPwd");
            githubUser = props.getProperty("githubUser");
            githubPwd = props.getProperty("githubPwd");
            basicAuth = getBasicAuth();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private static String login() throws IOException {
        List<NameValuePair> param = new ArrayList<>();
        param.add(new BasicNameValuePair("username", gitstarUser));
        param.add(new BasicNameValuePair("password", gitstarPwd));

        HttpPost httpPost = new HttpPost(URL_LOGIN);
        httpPost.setEntity(new UrlEncodedFormEntity(param));
        HttpResponse response = getDefaultHttpClient().execute(httpPost);
        String entity = EntityUtils.toString(response.getEntity());
        JSONObject result = JSON.parseObject(entity);
        System.out.println(result.getString("Msg"));
        return result.getIntValue("Code") == 200 ? response.getFirstHeader("Set-Cookie").getValue() : null;
    }

    private static Map<String, String> getRepoMap(String cookie) throws IOException {
        Map<String, String> map = new HashMap<>();
        HttpGet httpGet = new HttpGet(String.format(URL_RECOMMEND, gitstarUser));
        httpGet.addHeader("Accept", "application/json");
        httpGet.addHeader("Cookie", cookie);
        HttpResponse response = getDefaultHttpClient().execute(httpGet);
        String entity = EntityUtils.toString(response.getEntity());
        JSONArray array = JSON.parseArray(entity);
        for (int i = 0, size = array.size(); i < size; i++) {
            JSONObject object = array.getJSONObject(i);
            String target = object.getString("Target");
            String repo = object.getString("Repo");
            int score = object.getIntValue("Score");
            if (score <= 0) {
                if (!isStarred(repo)) {
                    map.put(target, repo);
                }
            }
        }
        return map;
    }

    private static boolean isStarred(String repo) throws IOException {
        HttpGet httpGet = new HttpGet(String.format(URL_STAR, repo));
        httpGet.setHeader("Authorization", basicAuth);
        HttpResponse response = getDefaultHttpClient().execute(httpGet);
        return response.getStatusLine().getStatusCode() != 404;
    }

    private static void star(String repo) throws IOException {
        HttpPut httpPut = new HttpPut(String.format(URL_STAR, repo));
        httpPut.setHeader("Authorization", basicAuth);
        getDefaultHttpClient().execute(httpPut);
    }

    private static void unstar(String repo) throws IOException {
        HttpDelete httpDelete = new HttpDelete(String.format(URL_STAR, repo));
        httpDelete.setHeader("Authorization", basicAuth);
        getDefaultHttpClient().execute(httpDelete);
    }

    private static String getBasicAuth() {
        String auth = githubUser + ":" + githubPwd;
        String encodedAuth;
        try {
            encodedAuth = new BASE64Encoder().encode(auth.getBytes("US-ASCII"));
        } catch (UnsupportedEncodingException e) {
            encodedAuth = new BASE64Encoder().encode(auth.getBytes());
        }
        return "Basic " + encodedAuth;
    }

    static HttpClient getDefaultHttpClient() {
        if (httpClient == null) {
            httpClient = HttpClients.createDefault();
        }
        return httpClient;
    }
}
