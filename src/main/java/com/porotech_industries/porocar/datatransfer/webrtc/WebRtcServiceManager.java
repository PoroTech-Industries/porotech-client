package com.porotech_industries.porocar.datatransfer.webrtc;

import com.porotech_industries.porocar.utils.logger.PoroLogger;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebRtcServiceManager {

    private Process pythonProcess;

    public void startPythonWebRtcService() throws IOException {
        ProcessBuilder builder = new ProcessBuilder("python",
                "src/main/resources/webrtc/webrtc_streamer.py");
        builder.redirectErrorStream(true);
        pythonProcess = builder.start();
        PoroLogger.info("WebRTC", "Python WebRTC-Service started");

    }

    public boolean isHealthy() {
        try {
            URL url = new URL("http://localhost:6060/health");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            return conn.getResponseCode() == 200;
        } catch (IOException e) {
            return false;
        }
    }

    public void stopPythonService() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            pythonProcess.destroy();
        }
    }
}