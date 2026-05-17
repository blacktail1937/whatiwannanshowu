package com.blacktail92.whatiwannashowu.client;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public static String put(ItemStack stack) {
        if (stack.isEmpty()) return "";

        try {
            var nbt = stack.serializeNBT();
//            LOGGER.info("before:{}, {}", nbt.size(), nbt.sizeInBytes());
            var bytes = compress(nbt);
//            LOGGER.info("after:{}", bytes.length);

            var hash = DigestUtils.sha1Hex(bytes).substring(0, 8);

            CACHE.put(hash, bytes);

            return hash;
        } catch (IOException e) {
            LOGGER.error("Error while compressing ItemStack.", e);
        }

        return "";
    }

    public static ItemStack get(String hash) {
        try {
            var data = CACHE.get(hash);
            if (data != null) {
                var nbt = decompress(data);

                return ItemStack.of(nbt);
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

        // limit 128K
        if (stream.size() > 128 * 1024) {
            tag.remove("tag");
            stream = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, stream);
        }

        return stream.toByteArray();
    }

    public static CompoundTag decompress(byte[] data) throws IOException {
        var stream = new ByteArrayInputStream(data);

        return NbtIo.readCompressed(stream);
    }
}
