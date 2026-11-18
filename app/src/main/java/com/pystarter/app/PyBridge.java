package com.pystarter.app;

import android.os.Handler;
import android.os.Looper;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Java 和 Python 之间的“桥”。
 *
 * 所有 Python 函数（pystarter/api.py）都返回 JSON 字符串，这里统一转成 JSONObject。
 * 运行代码、调用 AI 可能要几秒，必须放到后台线程，否则界面会卡住；
 * 结果再切回主线程交给页面显示（安卓规定只有主线程能改界面）。
 */
public final class PyBridge {

    public interface Callback {
        void onResult(JSONObject result);
    }

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private PyBridge() {}

    private static PyObject api() {
        return Python.getInstance().getModule("pystarter.api");
    }

    /** 在后台线程调用 Python 函数，结果回到主线程。 */
    public static void call(Callback callback, String function, Object... args) {
        WORKER.execute(() -> {
            JSONObject result;
            try {
                String json = api().callAttr(function, args).toString();
                result = new JSONObject(json);
            } catch (Exception e) {
                result = new JSONObject();
                try {
                    result.put("error", e.getMessage());
                } catch (JSONException ignored) {
                }
            }
            JSONObject finalResult = result;
            MAIN.post(() -> callback.onResult(finalResult));
        });
    }

    /** 关卡列表返回的是 JSON 数组，单独处理。在后台线程调用。 */
    public static void listLessons(ArrayCallback callback) {
        WORKER.execute(() -> {
            JSONArray array;
            try {
                array = new JSONArray(api().callAttr("list_lessons").toString());
            } catch (Exception e) {
                array = new JSONArray();
            }
            JSONArray finalArray = array;
            MAIN.post(() -> callback.onResult(finalArray));
        });
    }

    public interface ArrayCallback {
        void onResult(JSONArray result);
    }
}
