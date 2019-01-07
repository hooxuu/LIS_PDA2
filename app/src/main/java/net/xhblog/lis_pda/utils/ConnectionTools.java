package net.xhblog.lis_pda.utils;

import android.content.Context;
import net.xhblog.lis_pda.R;

import java.io.ByteArrayOutputStream;
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

    public ConnectionTools(Context context) {
        this.context = context;
        init();
    }

    private void init() {
        try {
            properties = new Properties();
            InputStream inputStream = context.getResources().openRawResource(R.raw.connect);
            properties.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getDataFromServer(String requestUrl, String data) {
        try {

            HttpURLConnection conn = (HttpURLConnection) new URL(requestUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setReadTimeout(5000);
            conn.setConnectTimeout(5000);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // Post方式不能缓存,需手动设置为false
            conn.setUseCaches(false);

            //获取输出流
            os = conn.getOutputStream();
            os.write(data.getBytes());
            os.flush();

            //获取返回的数据
            if(200 == conn.getResponseCode()) {
                is = conn.getInputStream();
                baos = new ByteArrayOutputStream();

                int len = 0;
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
        }
        return "";
    }

    public String getConfigByPropertyName(String propertyname) {
        return properties.getProperty(propertyname);
    }
}
