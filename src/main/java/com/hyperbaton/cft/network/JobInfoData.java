package com.hyperbaton.cft.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record JobInfoData(String statusKey, int statusColor, List<JobDisplayEntry> entries) {

    public static void encode(ByteBuf buf, JobInfoData data) {
        ByteBufCodecs.STRING_UTF8.encode(buf, data.statusKey);
        buf.writeInt(data.statusColor);
        ByteBufCodecs.VAR_INT.encode(buf, data.entries.size());
        for (JobDisplayEntry entry : data.entries) {
            buf.writeByte(entry.type());
            ByteBufCodecs.STRING_UTF8.encode(buf, entry.labelKey());
            switch (entry.type()) {
                case JobDisplayEntry.TEXT -> {
                    ByteBufCodecs.STRING_UTF8.encode(buf, entry.textValue());
                    buf.writeInt(entry.intA());
                }
                case JobDisplayEntry.PROGRESS -> {
                    ByteBufCodecs.VAR_INT.encode(buf, entry.intA());
                    ByteBufCodecs.VAR_INT.encode(buf, entry.intB());
                    buf.writeBoolean(entry.textValue() != null);
                    if (entry.textValue() != null) {
                        ByteBufCodecs.STRING_UTF8.encode(buf, entry.textValue());
                    }
                }
                case JobDisplayEntry.ITEM -> {
                    ResourceLocation.STREAM_CODEC.encode(buf, entry.icon());
                    ByteBufCodecs.VAR_INT.encode(buf, entry.intA());
                }
            }
        }
    }

    public static JobInfoData decode(ByteBuf buf) {
        String statusKey = ByteBufCodecs.STRING_UTF8.decode(buf);
        int statusColor = buf.readInt();
        int count = ByteBufCodecs.VAR_INT.decode(buf);
        List<JobDisplayEntry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            byte type = buf.readByte();
            String labelKey = ByteBufCodecs.STRING_UTF8.decode(buf);
            switch (type) {
                case JobDisplayEntry.TEXT -> {
                    String textValue = ByteBufCodecs.STRING_UTF8.decode(buf);
                    int color = buf.readInt();
                    entries.add(JobDisplayEntry.text(labelKey, textValue, color));
                }
                case JobDisplayEntry.PROGRESS -> {
                    int current = ByteBufCodecs.VAR_INT.decode(buf);
                    int max = ByteBufCodecs.VAR_INT.decode(buf);
                    String displayText = buf.readBoolean() ? ByteBufCodecs.STRING_UTF8.decode(buf) : null;
                    entries.add(JobDisplayEntry.progress(labelKey, current, max, displayText));
                }
                case JobDisplayEntry.ITEM -> {
                    ResourceLocation icon = ResourceLocation.STREAM_CODEC.decode(buf);
                    int itemCount = ByteBufCodecs.VAR_INT.decode(buf);
                    entries.add(JobDisplayEntry.item(labelKey, icon, itemCount));
                }
            }
        }
        return new JobInfoData(statusKey, statusColor, entries);
    }
}
