package com.musicplayer.http;

import com.musicplayer.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class DemoAudioHandler implements HttpHandler {
    private static final int SAMPLE_RATE = 44100;
    private static final int DURATION_SECONDS = 8;

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        int toneSeed = 1;
        if (fileName.matches("\\d+\\.wav")) {
            toneSeed = Integer.parseInt(fileName.replace(".wav", ""));
        }
        byte[] bytes = buildWave(toneSeed);
        HttpUtil.send(exchange, 200, "audio/wav", bytes);
        exchange.close();
    }

    private static byte[] buildWave(int seed) throws IOException {
        int sampleCount = SAMPLE_RATE * DURATION_SECONDS;
        int byteRate = SAMPLE_RATE * 2;
        int dataSize = sampleCount * 2;
        int riffSize = 36 + dataSize;
        double frequency = 220.0 + (seed % 6) * 55.0;

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(output)) {
            writeString(data, "RIFF");
            writeIntLE(data, riffSize);
            writeString(data, "WAVE");
            writeString(data, "fmt ");
            writeIntLE(data, 16);
            writeShortLE(data, (short) 1);
            writeShortLE(data, (short) 1);
            writeIntLE(data, SAMPLE_RATE);
            writeIntLE(data, byteRate);
            writeShortLE(data, (short) 2);
            writeShortLE(data, (short) 16);
            writeString(data, "data");
            writeIntLE(data, dataSize);

            for (int i = 0; i < sampleCount; i++) {
                double time = i / (double) SAMPLE_RATE;
                double sample = Math.sin(2 * Math.PI * frequency * time) * 0.25;
                short value = (short) (sample * Short.MAX_VALUE);
                writeShortLE(data, value);
            }
        }
        return output.toByteArray();
    }

    private static void writeString(DataOutputStream data, String value) throws IOException {
        data.writeBytes(value);
    }

    private static void writeIntLE(DataOutputStream data, int value) throws IOException {
        data.writeByte(value & 0xFF);
        data.writeByte((value >> 8) & 0xFF);
        data.writeByte((value >> 16) & 0xFF);
        data.writeByte((value >> 24) & 0xFF);
    }

    private static void writeShortLE(DataOutputStream data, short value) throws IOException {
        data.writeByte(value & 0xFF);
        data.writeByte((value >> 8) & 0xFF);
    }
}
