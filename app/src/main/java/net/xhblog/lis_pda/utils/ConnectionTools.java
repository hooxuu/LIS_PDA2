package net.xhblog.lis_pda.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ConnectionTools {
    private String requestUrl;
    private String data;
    private ByteArrayOutputStream baos;
    private InputStream is;
    private OutputStream os;

    public ConnectionTools(String requestUrl, String data) {
        this.requestUrl = requestUrl;
        this.data = data;
    }

    public String getDataFromServer() {
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
}
