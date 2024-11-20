package com.example.examplemod.test;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class AudioFileChunkMessage {
    public final byte[] chunkData; // チャンクデータ
    public final int chunkIndex;  // 現在のチャンク番号
    public final int totalChunks; // 全チャンク数
    public final UUID senderUUID; // 送信者のUUID

    public AudioFileChunkMessage(byte[] chunkData, int chunkIndex, int totalChunks, UUID senderUUID) {
        this.chunkData = chunkData;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.senderUUID = senderUUID; // UUID を設定
        System.out.println("4a");
    }

    public AudioFileChunkMessage(FriendlyByteBuf buf) {
        this.chunkData = buf.readByteArray();
        this.chunkIndex = buf.readInt();
        this.totalChunks = buf.readInt();
        this.senderUUID = buf.readUUID(); // UUID を読み込む
        System.out.println("5a");
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByteArray(chunkData);
        buf.writeInt(chunkIndex);
        buf.writeInt(totalChunks);
        buf.writeUUID(senderUUID); // UUID を書き込む
        System.out.println("6a");
    }

    public static void handle(AudioFileChunkMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            System.out.println("7a");
            // サーバー側で受信データを処理
            ServerEventHandler.handleChunkAndSend(message);
        });
        context.setPacketHandled(true);
    }
}