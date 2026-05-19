package com.blacktail92.whatiwannashowu.client;

import com.blacktail92.whatiwannashowu.Config;
import com.blacktail92.whatiwannashowu.WhatIWannaShowU;
import com.blacktail92.whatiwannashowu.networking.ModMessages;
import com.blacktail92.whatiwannashowu.networking.OpenJeiPayload;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.logging.LogUtils;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = WhatIWannaShowU.MODID)
public class CommandEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        if (!Config.IS_JEI_LOADED) return;

        LOGGER.info("register open jei gui command");
        var key = "hash";
        event.getDispatcher().register(
                Commands.literal("wiwsu_lookup")
                        .then(Commands.argument(key, StringArgumentType.word())
//                                .suggests(((context, builder) -> SharedSuggestionProvider.suggestResource(
//                                        ForgeRegistries.ITEMS.getKeys(), builder
//                                )))
                                .executes(context -> {
                                    try {
//                                        LOGGER.info("命令触发 IS_JEI_LOADED");
//                                        var itemId = ResourceLocationArgument.getId(context, key);
//                                        ModMessages.sendToPlayer(new OpenJeiPayload(itemId), context.getSource().getPlayerOrException());
                                        var hash = StringArgumentType.getString(context, key);
                                        ModMessages.sendToPlayer(new OpenJeiPayload(hash), context.getSource().getPlayerOrException());
                                    } catch (CommandRuntimeException e) {
                                        context.getSource().sendFailure(Component.literal("This command can only be executed by a player."));
                                    }

                                    return 1;
                                }))

        );
    }
}
