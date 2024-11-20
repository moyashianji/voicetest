package com.example.examplemod.test;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("voicerec", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        CHANNEL.registerMessage(0, AudioFileMessage.class, AudioFileMessage::encode, AudioFileMessage::new, AudioFileMessage::handle);
        CHANNEL.registerMessage(1, AudioFileChunkMessage.class, AudioFileChunkMessage::encode, AudioFileChunkMessage::new, AudioFileChunkMessage::handle);

    }
}