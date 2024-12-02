package com.example.examplemod.test;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.particle.SuspendedParticle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientCommandHandler {

    private static final int CHUNK_SIZE = 8192; // 1チャンクあたりのサイズ (8KB)
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final List<AudioFileChunkMessage> chunkQueue = new ArrayList<>();
    private static int tickCounter = 0;
    private static final int intervalTicks = 5; // チャンク送信間隔 (5ティック)

    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                net.minecraft.commands.Commands.literal("playCustomSound")
                        .then(net.minecraft.commands.Commands.argument("path", StringArgumentType.greedyString())
                                .executes(context -> {
                                    String path = StringArgumentType.getString(context, "path");
                                    Minecraft mc = Minecraft.getInstance();
                                    ClientLevel world = mc.level;

                                    File file = new File(path);

                                    if (file.exists() && !file.isDirectory()) {
                                        try {
                                            byte[] fullData = Files.readAllBytes(file.toPath());
                                            int totalChunks = (int) Math.ceil((double) fullData.length / CHUNK_SIZE);

                                            UUID playerUUID = mc.player.getUUID();

                                            for (int i = 0; i < totalChunks; i++) {
                                                final int chunkIndex = i;

                                                // 送信スケジュール設定
                                                scheduler.schedule(() -> {
                                                    int start = chunkIndex * CHUNK_SIZE;
                                                    int end = Math.min(start + CHUNK_SIZE, fullData.length);
                                                    byte[] chunkData = new byte[end - start];
                                                    System.arraycopy(fullData, start, chunkData, 0, end - start);

                                                    NetworkHandler.CHANNEL.sendToServer(
                                                            new AudioFileChunkMessage(chunkData, chunkIndex, totalChunks, playerUUID)
                                                    );
                                                    System.out.println("Sent chunk " + chunkIndex + " of " + totalChunks);
                                                }, chunkIndex * 2, TimeUnit.MILLISECONDS); // 10ミリ秒ごとに送信
                                            }

                                        } catch (Exception e) {
                                            context.getSource().sendFailure(
                                                    Component.literal("Failed to read audio file.")
                                            );
                                            e.printStackTrace();
                                        }
                                    } else {
                                        context.getSource().sendFailure(
                                                Component.literal("File does not exist or is not valid.")
                                        );
                                    }

                                    return 1;
                                }
                                )
                        )
        );
    }
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (!chunkQueue.isEmpty()) {
            tickCounter++;
            if (tickCounter >= intervalTicks) {
                tickCounter = 0;
                AudioFileChunkMessage chunk = chunkQueue.remove(0); // 最初のチャンクを取得
                NetworkHandler.CHANNEL.sendToServer(chunk);
                System.out.println("Sent chunk " + chunk.chunkIndex + " / " + chunk.totalChunks);
            }
        }
    }
}