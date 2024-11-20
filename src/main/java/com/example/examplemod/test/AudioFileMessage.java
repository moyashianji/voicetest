package com.example.examplemod.test;


import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class AudioFileMessage {

    public final byte[] audioData;

    public AudioFileMessage(byte[] audioData) {
        this.audioData = audioData;
        System.out.println("3");
    }

    public AudioFileMessage(FriendlyByteBuf buf) {
        this.audioData = buf.readByteArray();
        System.out.println("5");

    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeByteArray(audioData);
        System.out.println("4");

    }




    public static void handle(AudioFileMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            CustomSoundPlayer.handleAudioPart(message.audioData);
        });
        context.setPacketHandled(true);
    }
}