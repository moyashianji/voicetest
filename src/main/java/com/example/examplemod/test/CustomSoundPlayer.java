package com.example.examplemod.test;
import com.example.examplemod.VoiceconfigConfiguration;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import javax.sound.sampled.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;

public class CustomSoundPlayer {
    // 各チャンネルのキューを管理するマップ
    private static final Map<Integer, Queue<byte[]>> channelQueues = new ConcurrentHashMap<>();
    private static final Map<Integer, Boolean> isPlayingMap = new ConcurrentHashMap<>();
    private static final AtomicInteger channelCounter = new AtomicInteger(0); // チャンネルIDを生成

    /**
     * 再生データを受け取り、新しいチャンネルを作成して再生を開始します。
     */
    public static synchronized void handleAudioPart(byte[] audioData, UUID senderUUID) {
        if (audioData == null || audioData.length == 0) {
            System.err.println("[CustomSoundPlayer] Received invalid audio data.");
            return;
        }

        int channelId = channelCounter.incrementAndGet(); // 新しいチャンネルIDを生成
        Queue<byte[]> audioQueue = new LinkedList<>();
        channelQueues.put(channelId, audioQueue);
        isPlayingMap.put(channelId, false);

        synchronized (audioQueue) {
            audioQueue.offer(audioData);
            System.out.println("[CustomSoundPlayer] Added audio data to channel " + channelId + ". Queue size: " + audioQueue.size());

            if (!isPlayingMap.get(channelId)) {
                isPlayingMap.put(channelId, true);
                new Thread(() -> playAudioQueue(senderUUID, channelId)).start();
            }
        }
    }

    /**
     * チャンネルごとの再生キューを順次再生し、距離に応じて音量を動的に調整します。
     */
    private static void playAudioQueue(UUID senderUUID, int channelId) {
        SourceDataLine line = null;
        try {
            AudioFormat format = new AudioFormat(44100, 16, 2, true, false); // サンプルレート44100Hz、16bit、ステレオ、リトルエンディアン
            int bufferSize = format.getFrameSize() * 2048; // バッファサイズを調整（2048フレーム）
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format, bufferSize);
            line.start();

            Queue<byte[]> audioQueue = channelQueues.get(channelId);
            if (audioQueue == null) {
                System.err.println("[CustomSoundPlayer] Channel " + channelId + " does not exist.");
                return;
            }

            while (true) {
                byte[] audioData;

                synchronized (audioQueue) {
                    if (audioQueue.isEmpty()) {
                        isPlayingMap.put(channelId, false);
                        break;
                    }
                    audioData = audioQueue.poll();
                }

                if (audioData != null) {
                    int frameSize = format.getFrameSize();
                    audioData = alignToFrameSize(audioData, frameSize); // フレームサイズに揃える

                    // チャンク単位で音量調整と再生
                    int chunkSize = frameSize * 1024; // チャンクサイズを1024フレームに調整
                    for (int offset = 0; offset < audioData.length; offset += chunkSize) {
                        int end = Math.min(offset + chunkSize, audioData.length);
                        byte[] chunk = new byte[end - offset];
                        System.arraycopy(audioData, offset, chunk, 0, end - offset);

                        // 距離に基づく音量を計算
                        float volume = calculateVolume(senderUUID);
                        byte[] adjustedChunk = adjustVolume(chunk, volume);

                        long processingStartTime = System.nanoTime();
                        line.write(adjustedChunk, 0, adjustedChunk.length);
                        long processingEndTime = System.nanoTime();

                        // 処理時間に基づいてスリープ時間を調整
                        long processingTimeMs = (processingEndTime - processingStartTime) / 1_000_000;
                        long desiredSleepTime = Math.max(0, 10 - processingTimeMs); // 最小スリープ時間を0に調整
                        Thread.sleep(desiredSleepTime);
                    }
                }
            }

            line.drain(); // 残りのデータを再生
            System.out.println("[CustomSoundPlayer] Audio playback finished on channel " + channelId);
        } catch (LineUnavailableException e) {
            System.err.println("[CustomSoundPlayer] Audio line unavailable: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (line != null) {
                line.stop();
                line.close();
            }
            resetPlayerState(channelId); // 再生状態のリセット
            System.out.println("[CustomSoundPlayer] Playback thread exiting for channel " + channelId);
        }
    }

    /**
     * 自分と送信者の距離に基づいて音量を計算します。
     */
    private static float calculateVolume(UUID senderUUID) {
        Minecraft mc = Minecraft.getInstance();
        Player self = mc.player;
        Player sender = mc.level.getPlayerByUUID(senderUUID);

        if (sender == null || self == null) {
            return 0.5f; // デフォルト音量（初期値: 50%）
        }

        double distance = self.distanceTo(sender);
        float maxDistance = VoiceconfigConfiguration.RANGE.get(); // 音が完全に聞こえなくなる距離
        return 0.5f * Math.max(0, 1 - (float) (distance / maxDistance)); // 初期音量0.5、距離に応じて減衰
    }

    /**
     * オーディオデータを指定された音量で調整します。
     */
    private static byte[] adjustVolume(byte[] audioData, float volume) {
        byte[] adjustedData = new byte[audioData.length];

        for (int i = 0; i < audioData.length; i += 2) {
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            sample = (short) (sample * volume);
            adjustedData[i] = (byte) (sample & 0xFF);
            adjustedData[i + 1] = (byte) ((sample >> 8) & 0xFF);
        }

        return adjustedData;
    }

    /**
     * オーディオデータをフレームサイズに揃える（不足分を切り捨てる）。
     */
    private static byte[] alignToFrameSize(byte[] audioData, int frameSize) {
        int remainder = audioData.length % frameSize;
        if (remainder == 0) {
            return audioData; // 既にフレーム単位で揃っている場合はそのまま返す
        }
        // フレーム単位に揃うように末尾を切り捨て
        int alignedLength = audioData.length - remainder;
        byte[] alignedData = new byte[alignedLength];
        System.arraycopy(audioData, 0, alignedData, 0, alignedLength);
        return alignedData;
    }

    /**
     * 指定したチャンネルの再生状態をリセットします。
     */
    private static synchronized void resetPlayerState(int channelId) {
        channelQueues.remove(channelId);
        isPlayingMap.remove(channelId);
        System.out.println("[CustomSoundPlayer] Player state reset for channel " + channelId);
    }
}
