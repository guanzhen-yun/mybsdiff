package com.inke.mybsdiff;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.inke.mybsdiff.utils.UriParseUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private Activity activity;

    //用于在应用程序启动时，加载本地的lib库
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activity = this;

        // 版本号显示
        TextView tv = findViewById(R.id.version);
        tv.setText(BuildConfig.VERSION_NAME);

        //运行时权限申请
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {// 6.0+
            String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            if(checkSelfPermission(perms[0]) == PackageManager.PERMISSION_DENIED || checkSelfPermission(perms[1]) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(perms, 200);
            }
        }
    }

    /**
     * 旧版本 + patch 差分包 合成新版本，然后安装这个apk
     * @param oldApk 旧的安装包，v1.1
     * @param patch 差分包
     * @param output 合成的新版本，v1.3(输入路径)
     */
    public native void bspatch(String oldApk, String patch, String output);

    private void downLoadPatchFile() {
        final File file = new File(getExternalCacheDir().getAbsolutePath(), "patch");
        if (file.exists()) {
            doPatch(file.getAbsolutePath());
        } else {
            String fileUrl = "http://192.168.19.161:8080/bsdiff/patch";
            OkHttpClient mOkHttpClient = new OkHttpClient();
            final Request request = new Request.Builder().url(fileUrl).build();
            final Call call = mOkHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    InputStream is = null;
                    byte[] buf = new byte[2048];
                    int len = 0;
                    FileOutputStream fos = null;
                    try {
                        long total = response.body().contentLength();
                        long current = 0;
                        is = response.body().byteStream();
                        fos = new FileOutputStream(file);
                        while ((len = is.read(buf)) != -1) {
                            current += len;
                            fos.write(buf, 0, len);
                        }
                        fos.flush();
                        handler.sendEmptyMessage(0);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (is != null) {
                                is.close();
                            }
                            if (fos != null) {
                                fos.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            File file = new File(getExternalCacheDir().getAbsolutePath(), "patch");
            doPatch(file.getAbsolutePath());
        }
    };

    private void doPatch(String patch) {
        // 不模拟网络下载(api接口)，直接放置在SDCard中测试
        new AsyncTask<Void, Void, File>() {

            //耗时操作，返回一个输出文件（合成好的新apk）
            @Override
            protected File doInBackground(Void... voids) {
                //获取现有的apk(用户目前版本)
                String oldApk = getApplicationInfo().sourceDir;
                //可以随意，有点争议
                String output = createNewApk().getAbsolutePath();
                try {
                    bspatch(oldApk, patch, output);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return new File(output);
            }

            //直到doInBackground()完成才调用,参数File就是上个方法返回值（合成后的新版本apk）
            @Override
            protected void onPostExecute(File file) {
                super.onPostExecute(file);
                //安装合成后的新版本apk
                UriParseUtils.installApk(activity, file);
            }
        }.execute();
    }

    // 点击检查版本
    public void update(View view) {
        downLoadPatchFile();
    }

    //创建合成后的新的apk文件
    private File createNewApk() {
        File newApk = new File(getExternalCacheDir().getAbsolutePath(), "bsdiff.apk");
        if(!newApk.exists()) {
            // 无中生有（为了演示） Android/data/packagename/download/apk/... (正常的路径)
            try {
                newApk.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return newApk;
    }
}