package com.pystarter.app.net;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * OkHttp 网络客户端：连接 Spring Boot 在线排行榜后端。
 * 默认地址 http://10.0.2.2:8080（安卓模拟器访问宿主机本机的标准地址）；
 * 不联网或后端未启动时，返回友好离线提示，不影响 App 其他全部离线功能。
 */
public final class LeaderboardClient {

    public static final String DEFAULT_SERVER_URL = "http://10.0.2.2:8080";
    private static final String SIGN_SECRET = "PyStarter-2026-Secret";
    private static final String PREFS = "leaderboard_client";
    private static final String KEY_PLAYER_ID = "player_id";
    private static final String KEY_NICKNAME = "nickname";
    private static final String KEY_SERVER_URL = "server_url";

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build();
    private static final ExecutorService NET = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onResult(boolean ok, JSONObject data, String error);
    }

    private LeaderboardClient() {}

    public static String playerId(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String id = p.getString(KEY_PLAYER_ID, null);
        if (id == null) {
            id = "dev-" + UUID.randomUUID().toString().substring(0, 8);
            p.edit().putString(KEY_PLAYER_ID, id).apply();
        }
        return id;
    }

    public static String nickname(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_NICKNAME, "小骑士");
    }

    public static void saveProfile(Context ctx, String nickname, String serverUrl) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_NICKNAME, nickname.trim().isEmpty() ? "小骑士" : nickname.trim())
                .putString(KEY_SERVER_URL, serverUrl.trim().isEmpty() ? DEFAULT_SERVER_URL : serverUrl.trim())
                .apply();
    }

    public static String serverUrl(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_SERVER_URL, DEFAULT_SERVER_URL)
                .replaceAll("/+$", "");
    }

    public static void fetchTop50(Context ctx, Callback cb) {
        String url = serverUrl(ctx) + "/api/leaderboard";
        NET.execute(() -> {
            Request req = new Request.Builder().url(url).get().build();
            try (Response resp = HTTP.newCall(req).execute()) {
                String body = resp.body() != null ? resp.body().string() : "{}";
                if (!resp.isSuccessful()) {
                    post(cb, false, null, "服务器返回 " + resp.code());
                    return;
                }
                post(cb, true, new JSONObject(body), null);
            } catch (IOException e) {
                post(cb, false, null, "未连接排行榜后端（" + url + "），离线模式照常可用");
            } catch (Exception e) {
                post(cb, false, null, "解析失败：" + e.getMessage());
            }
        });
    }

    public static void submitScore(Context ctx, String nickname, int stars, int xp, int level, Callback cb) {
        String pid = playerId(ctx);
        String url = serverUrl(ctx) + "/api/leaderboard/submit";
        String sig = sha256(pid + ":" + stars + ":" + xp + ":" + SIGN_SECRET);

        NET.execute(() -> {
            try {
                JSONObject payload = new JSONObject()
                        .put("playerId", pid)
                        .put("nickname", nickname)
                        .put("stars", stars)
                        .put("xp", xp)
                        .put("level", level)
                        .put("signature", sig);
                Request req = new Request.Builder()
                        .url(url)
                        .post(RequestBody.create(payload.toString(), JSON))
                        .build();
                try (Response resp = HTTP.newCall(req).execute()) {
                    String body = resp.body() != null ? resp.body().string() : "{}";
                    JSONObject obj = new JSONObject(body);
                    if (!resp.isSuccessful()) {
                        post(cb, false, obj, obj.optString("error", "上传失败 " + resp.code()));
                        return;
                    }
                    post(cb, true, obj, null);
                }
            } catch (IOException e) {
                post(cb, false, null, "后端未启动或无网络（离线学习不受影响）");
            } catch (Exception e) {
                post(cb, false, null, "提交出错：" + e.getMessage());
            }
        });
    }

    private static void post(Callback cb, boolean ok, JSONObject data, String err) {
        MAIN.post(() -> cb.onResult(ok, data, err));
    }

    private static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
