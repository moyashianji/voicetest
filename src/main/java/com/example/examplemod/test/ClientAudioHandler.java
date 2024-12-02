package com.example.examplemod.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientAudioHandler {
    private static final List<byte[]> receivedChunks = new ArrayList<>();
    private static int totalChunks = 0;

    public static synchronized void handleChunk(AudioFileChunkMessage message) {
        if (totalChunks == 0) {
            totalChunks = message.totalChunks;
        }

        // チャンク受信
        while (receivedChunks.size() <= message.chunkIndex) {
            receivedChunks.add(null);
        }
        receivedChunks.set(message.chunkIndex, message.chunkData);

        // 全チャンク受信後の処理
        if (receivedChunks.stream().filter(c -> c != null).count() == totalChunks) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                for (byte[] chunk : receivedChunks) {
                    outputStream.write(chunk);
                }

                byte[] completeAudioData = addWavHeader(outputStream.toByteArray(), 44100, 2, 16);

                // 再生
                CustomSoundPlayer.handleAudioPart(completeAudioData,message.senderUUID);

                // 状態リセット
                receivedChunks.clear();
                totalChunks = 0;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static byte[] addWavHeader(byte[] audioData, int sampleRate, int channels, int bitDepth) {
        int byteRate = sampleRate * channels * (bitDepth / 8);
        int blockAlign = channels * (bitDepth / 8);
        int dataSize = audioData.length;

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            outputStream.write("RIFF".getBytes());
            outputStream.write(intToLittleEndian(36 + dataSize));
            outputStream.write("WAVE".getBytes());
            outputStream.write("fmt ".getBytes());
            outputStream.write(intToLittleEndian(16));
            outputStream.write(shortToLittleEndian((short) 1));
            outputStream.write(shortToLittleEndian((short) channels));
            outputStream.write(intToLittleEndian(sampleRate));
            outputStream.write(intToLittleEndian(byteRate));
            outputStream.write(shortToLittleEndian((short) blockAlign));
            outputStream.write(shortToLittleEndian((short) bitDepth));
            outputStream.write("data".getBytes());
            outputStream.write(intToLittleEndian(dataSize));
            outputStream.write(audioData);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outputStream.toByteArray();
    }

    private static byte[] intToLittleEndian(int value) {
        return new byte[]{
                (byte) (value & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 24) & 0xFF)
        };
    }

    private static byte[] shortToLittleEndian(short value) {
        return new byte[]{
                (byte) (value & 0xFF),
                (byte) ((value >> 8) & 0xFF)
        };
    }
}