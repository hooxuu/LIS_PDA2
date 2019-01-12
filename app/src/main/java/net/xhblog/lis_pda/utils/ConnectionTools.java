package net.xhblog.lis_pda.utils;

import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

public class ConnectionTools {
    private ByteArrayOutputStream baos;
    private InputStream is;
    private OutputStream os;
    private Context context;
    private Properties properties = null;
    private InputStream inputStream;

    public ConnectionTools(Context context) {
        this.context = context;
        init();
    }

    private void init() {
        try {
            properties = new Properties();
            inputStream = context.openFileInput("connect.properties");
            properties.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if(null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException ie) {
                    Log.w("IOerror", ie.getLocalizedMessage());
                }
            }
        }
    }

    public String getDataFromServer(String requestUrl, String data) {
        HttpURLConnection conn = null;
        try {

            conn = (HttpURLConnection) new URL(requestUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setReadTimeout(5000);
            conn.setConnectTimeout(5000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // Post方式不能缓存,需手动设置为false
            conn.setUseCaches(false);
            conn.connect();

            //获取输出流
            os = conn.getOutputStream();
            os.write(data.getBytes());
            os.flush();

            //获取返回的数据
            if(200 == conn.getResponseCode()) {
                is = conn.getInputStream();
                baos = new ByteArrayOutputStream();

                int len;
                byte[] buffer = new byte[1024];
                while(-1 != (len = is.read(buffer))) {
                    baos.write(buffer, 0, len);
                    baos.flush();
                }
                return new String(baos.toByteArray());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(baos != null) {
                    baos.close();
                }

                if(os != null) {
                    os.close();
                }

                if(is != null) {
                    is.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if(conn != null) {
                conn.disconnect();
            }
        }
        return "";
    }



    public String getConfigByPropertyName(String propertyname) {
        return properties.getProperty(propertyname);
    }
}
