package com.blacktail92.whatiwannashowu.client;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.codec.digest.DigestUtils;
import com.blacktail92.whatiwannashowu.Config;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class ItemCache {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int MAX = 500;
    /// hash-compressed nbt
    private static final Map<String, byte[]> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(MAX, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry eldest) {
                    return size() > MAX;
                }
            });

    public static String put(ItemStack stack, HolderLookup.Provider provider) {
        if (stack.isEmpty()) return "";

        try {
            var nbt = (CompoundTag) stack.save(provider);
//            LOGGER.info("before:{}, {}", nbt.size(), nbt.sizeInBytes());
            var bytes = compress(nbt);
//            LOGGER.info("after:{}", bytes.length);

            var hash = DigestUtils.sha1Hex(bytes);//.substring(0, 8);

            CACHE.put(hash, bytes);

            return hash;
        } catch (IOException e) {
            LOGGER.error("Error while compressing ItemStack.", e);
        }

        return "";
    }

    public static ItemStack get(String hash, HolderLookup.Provider provider) {
        try {
            var data = CACHE.get(hash);
            if (data != null) {
                var nbt = decompress(data);

                return ItemStack.parseOptional(provider, nbt);
            }
        } catch (IOException e) {
            LOGGER.error("Error while decompressing ItemStack.", e);
        }

        return ItemStack.EMPTY;
    }

    public static void clear() {
        CACHE.clear();
    }

    public static byte[] compress(CompoundTag tag) throws IOException {
        var stream = new ByteArrayOutputStream();
        NbtIo.writeCompressed(tag, stream);

        if (stream.size() > Config.MAX_COMPRESSED_SIZE.get() * 1024) {
            tag.remove("components");
            stream = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, stream);
        }

        return stream.toByteArray();
    }

    public static CompoundTag decompress(byte[] data) throws IOException {
        var stream = new ByteArrayInputStream(data);

        return NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
    }
}