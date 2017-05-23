package com.laihuan.okhttp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_getrequest).setOnClickListener(this);
        findViewById(R.id.btn_postrequest).setOnClickListener(this);
        findViewById(R.id.btn_postUploadFile).setOnClickListener(this);
        findViewById(R.id.btn_uploadMultipartFile).setOnClickListener(this);
        findViewById(R.id.btn_cacheHttp).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btn_getrequest){
            getAsynHttpsVerifier(); // 异步 get 请求
        }else if(v.getId() == R.id.btn_postrequest){
            postAsynHttp(); // 异步 post 请求
        }else if(v.getId() == R.id.btn_postUploadFile){
            postAsynFile(); // 异步上传文件
        }else if(v.getId() == R.id.btn_downAsynFile){
            downAsynFile(); // 异步下载文件
        }else if(v.getId() == R.id.btn_uploadMultipartFile){
            sendMultipart(); // 上传携带 key-value 等数据的文件
        }else if(v.getId() == R.id.btn_cacheHttp){
            cacheHttp(); // 数据缓存
        }
    }

    /**
     * get 异步请求，并进行 https 证书认证
     */
    private void getAsynHttpsVerifier() {
        try {
            HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
                    boolean result = hv.verify("*.csc108.com", session);

                    return result;
                }
            };

            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            InputStream inStream = getAssets().open("csc108.cer");
            final X509Certificate certificate = (X509Certificate)certificateFactory.generateCertificate(inStream);

            SSLContext sslContext = SSLContext.getInstance("TLS");

            boolean isCertificate = false;
            if(isCertificate) {

                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
                trustStore.setCertificateEntry("trust", certificate);

                String trustAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(trustAlgorithm);
                trustManagerFactory.init(trustStore);
                TrustManager[] managers = trustManagerFactory.getTrustManagers();
                sslContext.init(null, managers, null);
            }else {

                TrustManager tm = new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                        boolean success = true;
                        if(chain == null || chain.length < 0) {
                            success = false;
                            return;
                        }

                        for (X509Certificate x509Certificate : chain) {
                            x509Certificate.checkValidity();
                            try {
                                PublicKey key = certificate.getPublicKey();
                                x509Certificate.verify(key);
                            }catch(Exception e){
                                success = false;
                                e.printStackTrace();
                            }
                        }
                        int a = 0;
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                };

                sslContext.init(null, new TrustManager[]{tm}, new SecureRandom());
            }

            OkHttpClient mOkHttpClient = new OkHttpClient().newBuilder()
                    .hostnameVerifier(DO_NOT_VERIFY)
                    .sslSocketFactory(sslContext.getSocketFactory()).build();

            String url = "https://accounttest.csc108.com:9801/api/system/init/66099/100000"; //  63.1 环境下配置的域名. 11111.cer 可用的

            Request.Builder requestBuilder = new Request.Builder().url(url);
            //可以省略，默认是GET请求
            requestBuilder.method("GET", null);

            Request request = requestBuilder.build();
            Call mcall = mOkHttpClient.newCall(request);
            mcall.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "请求失败！", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    // response 响应参见源码主目录下【图1】
                    if (null != response.cacheResponse()) {
                        String str = response.cacheResponse().toString();
                        Log.i("wangshu", "cache---" + str);
                    } else {
                        String protocol = response.protocol().toString();
                        int code = response.code();
                        String message = response.message();
                        String date = response.header("Date");
                        String body = response.body().string();
                        String str = response.networkResponse().toString();
                        Log.i("wangshu", "network---" + str);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "请求成功", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * post 数据请求
     */
    private void postAsynHttp() {
        OkHttpClient mOkHttpClient=new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .add("size", "10")
                .build();
        Request request = new Request.Builder()
                .url("http://api.1-blog.com/biz/bizserver/article/list.do")
                .post(formBody)
                .build();
        Call call = mOkHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String str = response.body().string();
                Log.i("wangshu", str);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "请求成功", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        });
    }

    /**
     * post 异步文件上传
     */
    private void postAsynFile() {
        OkHttpClient mOkHttpClient=new OkHttpClient();
        File file = new File("/sdcard/wangshu.txt");
        Request request = new Request.Builder()
                .url("https://api.github.com/markdown/raw")
                .post(RequestBody.create(MediaType.parse("text/x-markdown; charset=utf-8"), file))
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i("wangshu",response.body().string());
            }
        });
    }

    /**
     * 异步文件下载
     */
    private void downAsynFile() {
        OkHttpClient mOkHttpClient = new OkHttpClient();
        String url = "http://img.my.csdn.net/uploads/201603/26/1458988468_5804.jpg";
        Request request = new Request.Builder().url(url).build();
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) {
                InputStream inputStream = response.body().byteStream();
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(new File("/sdcard/wangshu.jpg"));
                    byte[] buffer = new byte[2048];
                    int len = 0;
                    while ((len = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, len);
                    }
                    fileOutputStream.flush();
                } catch (IOException e) {
                    Log.i("wangshu", "IOException");
                    e.printStackTrace();
                }
                Log.d("wangshu", "文件下载成功");
            }
        });
    }

    /**
     * 上传携带 key-value 信息的文件
     */
    private void sendMultipart(){
        OkHttpClient mOkHttpClient = new OkHttpClient();
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("title", "wangshu")
                .addFormDataPart("image", "wangshu.jpg",
                        RequestBody.create(MediaType.parse("image/png"), new File("/sdcard/wangshu.jpg")))
                .build();

        Request request = new Request.Builder()
                .header("Authorization", "Client-ID " + "...")
                .url("https://api.imgur.com/3/image")
                .post(requestBody)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i("wangshu", response.body().string());
            }
        });
    }

    /**
     * 数据缓存
     */
    private void cacheHttp() {

            int maxCacheSize = 10 * 1024 * 1024; //缓存大小的限制：10M

            //创建cache 类，需要两个参数（文件目录，文件大小）
            Cache cache = new Cache(new File("/Users/gym/source"), maxCacheSize);

            OkHttpClient client = new OkHttpClient.Builder().cache(cache).build();

            Request request = new Request.Builder().url("http://www.qq.com/").
                    cacheControl(new CacheControl.Builder().maxStale(365, TimeUnit.DAYS).build()).
                    build();

        try{
            Response response = client.newCall(request).execute();

            String body1 = response.body().string();
            System.out.println("network response " + response.networkResponse());
            System.out.println("cache response " + response.cacheResponse());

            System.out.println("**************************");

            Response response1 = client.newCall(request).execute();

            String body2 = response1.body().string();
            System.out.println("network response " + response1.networkResponse());
            System.out.println("cache response " + response1.cacheResponse());
        }catch(IOException e){

        }
        /*
        关于以上代码需要注意的是：response请求资源后的相应有两个相关方法，networkResponse()和cacheResponse()。
        从字面意义上理解，一个是从网络请求中获取资源，另一个是从缓存中获取资源。
        如果该资源是从服务器获取的，networkResponse()返回值不会为null，即cacheResponse()返回值为null；
        如果是从缓存中获取的，networkResponse()返回值为null，cacheResponse()返回值不为null。
         */
    }
}
