package com.example.examplemod.test;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.util.LinkedList;
import java.util.Queue;

public class CustomSoundPlayer {
    private static final Queue<byte[]> audioQueue = new LinkedList<>();
    private static volatile boolean isPlaying = false;


    public static synchronized void handleAudioPart(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            System.err.println("[CustomSoundPlayer] Received invalid audio data.");
            return;
        }

        synchronized (audioQueue) {
            audioQueue.offer(audioData);
            System.out.println("[CustomSoundPlayer] Added audio data to queue. Queue size: " + audioQueue.size());

            if (!isPlaying) {
                isPlaying = true;
                new Thread(CustomSoundPlayer::playAudioQueue).start();
            }
        }
    }

    private static void playAudioQueue() {
        SourceDataLine line = null;
        try {
            AudioFormat format = new AudioFormat(44100, 16, 2, true, false); // サンプルレート44100Hz、16bit、ステレオ、リトルエンディアン
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            while (true) {
                byte[] audioData;

                synchronized (audioQueue) {
                    if (audioQueue.isEmpty()) {
                        isPlaying = false;
                        break;
                    }
                    audioData = audioQueue.poll();
                }

                if (audioData != null) {
                    audioData = alignToFrameSize(audioData, format.getFrameSize());

                    line.write(audioData, 0, audioData.length); // データを直接ラインに書き込む
                }
            }

            line.drain(); // 残りのデータを再生
            System.out.println("[CustomSoundPlayer] Audio playback finished.");
        } catch (LineUnavailableException e) {
            System.err.println("[CustomSoundPlayer] Audio line unavailable: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (line != null) {
                line.stop();
                line.close();
            }
            resetPlayerState(); // 再生状態のリセット
            System.out.println("[CustomSoundPlayer] Playback thread exiting.");
        }
    }
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
     * 再生状態をリセットし、キューをクリアします。
     */
    private static synchronized void resetPlayerState() {
        synchronized (audioQueue) {
            audioQueue.clear();
        }
        isPlaying = false;
        System.out.println("[CustomSoundPlayer] Player state reset.");
    }
}
