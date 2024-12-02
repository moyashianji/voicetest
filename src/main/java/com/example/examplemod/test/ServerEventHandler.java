package com.example.examplemod.test;

import net.minecraftforge.network.PacketDistributor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerEventHandler {
    private static final Map<String, List<byte[]>> chunkDataMap = new HashMap<>();
    private static final Map<String, Integer> receivedChunksMap = new HashMap<>();
    private static final Map<String, Integer> totalChunksMap = new HashMap<>();
    private static final Map<String, Integer> lastProcessedChunksMap = new HashMap<>();
    private static final int CHUNKS_PER_BATCH = 30; // 1バッチで結合するチャンク数
    public static void handleChunkAndSend(AudioFileChunkMessage message) {
        // クライアントから受け取ったチャンクを全クライアントに送信
        NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }
 //   public static void handleChunkAndSend(AudioFileChunkMessage message) {
 //       String senderKey = message.senderUUID + "-" + message.totalChunks;
//
 //       chunkDataMap.putIfAbsent(senderKey, new ArrayList<>());
 //       receivedChunksMap.putIfAbsent(senderKey, 0);
 //       totalChunksMap.putIfAbsent(senderKey, message.totalChunks);
 //       lastProcessedChunksMap.putIfAbsent(senderKey, 0);
//
 //       List<byte[]> chunks = chunkDataMap.get(senderKey);
//
 //       // チャンクを保存
 //       while (chunks.size() <= message.chunkIndex) {
 //           chunks.add(null);
 //       }
//
 //       if (chunks.get(message.chunkIndex) == null) {
 //           chunks.set(message.chunkIndex, message.chunkData);
 //           receivedChunksMap.put(senderKey, receivedChunksMap.get(senderKey) + 1);
 //       }
//
 //       int receivedChunks = receivedChunksMap.get(senderKey);
 //       int totalChunks = totalChunksMap.get(senderKey);
 //       int lastProcessedChunks = lastProcessedChunksMap.get(senderKey);
//
 //       // 30チャンクごとに結合して送信
 //       if (receivedChunks - lastProcessedChunks >= CHUNKS_PER_BATCH || receivedChunks == totalChunks) {
 //           ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//
 //           try {
 //               for (int i = lastProcessedChunks; i < receivedChunks; i++) {
 //                   if (chunks.get(i) != null) {
 //                       outputStream.write(chunks.get(i));
 //                   }
 //               }
//
 //               byte[] partialData = addWavHeader(outputStream.toByteArray(), 44100, 2, 16); // ヘッダを付与
//
 //               // 保存処理を追加
 //               saveWavToFile(partialData, "output_" + senderKey + "_" + lastProcessedChunks + ".wav");
//
 //               NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), new AudioFileMessage(partialData));
//
 //               // 最後に処理したチャンクの数を更新
 //               lastProcessedChunksMap.put(senderKey, receivedChunks);
 //           } catch (IOException e) {
 //               e.printStackTrace();
 //           }
 //       }
//
 //       // すべてのチャンクを受信した場合、メモリ解放
 //       if (receivedChunks == totalChunks) {
 //           chunkDataMap.remove(senderKey);
 //           receivedChunksMap.remove(senderKey);
 //           totalChunksMap.remove(senderKey);
 //           lastProcessedChunksMap.remove(senderKey);
 //       }
 //   }
 //public static void handleChunkAndSend(AudioFileChunkMessage message) {
 //    NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(), message);
 //}
 //   private static void saveWavToFile(byte[] audioData, String fileName) {
 //       try {
 //           File file = new File("output_wav/" + fileName);
 //           file.getParentFile().mkdirs(); // ディレクトリが存在しない場合は作成
 //           try (FileOutputStream fos = new FileOutputStream(file)) {
 //               fos.write(audioData);
 //               System.out.println("Saved WAV file: " + file.getAbsolutePath());
 //           }
 //       } catch (IOException e) {
 //           System.err.println("Failed to save WAV file: " + e.getMessage());
 //       }
 //   }
//
 //   private static byte[] addWavHeader(byte[] audioData, int sampleRate, int channels, int bitDepth) {
 //       int byteRate = sampleRate * channels * (bitDepth / 8);
 //       int blockAlign = channels * (bitDepth / 8);
 //       int dataSize = audioData.length;
//
 //       ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//
 //       try {
 //           outputStream.write("RIFF".getBytes());
 //           outputStream.write(intToLittleEndian(36 + dataSize));
 //           outputStream.write("WAVE".getBytes());
 //           outputStream.write("fmt ".getBytes());
 //           outputStream.write(intToLittleEndian(16));
 //           outputStream.write(shortToLittleEndian((short) 1));
 //           outputStream.write(shortToLittleEndian((short) channels));
 //           outputStream.write(intToLittleEndian(sampleRate));
 //           outputStream.write(intToLittleEndian(byteRate));
 //           outputStream.write(shortToLittleEndian((short) blockAlign));
 //           outputStream.write(shortToLittleEndian((short) bitDepth));
 //           outputStream.write("data".getBytes());
 //           outputStream.write(intToLittleEndian(dataSize));
 //           outputStream.write(audioData);
 //       } catch (IOException e) {
 //           e.printStackTrace();
 //       }
//
 //       return outputStream.toByteArray();
 //   }

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
