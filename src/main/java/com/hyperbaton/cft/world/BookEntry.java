package com.hyperbaton.cft.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One entry in the world's roster of writer-produced books: the source of truth for a
 * manuscript's content, independent of whether any physical copy of it still exists.
 */
public record BookEntry(int id, UUID leaderId, String title, String authorName, List<String> pages) {

    private static final String TAG_ID = "id";
    private static final String TAG_LEADER = "leaderId";
    private static final String TAG_TITLE = "title";
    private static final String TAG_AUTHOR = "authorName";
    private static final String TAG_PAGES = "pages";

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(TAG_ID, id);
        tag.putUUID(TAG_LEADER, leaderId);
        tag.putString(TAG_TITLE, title);
        tag.putString(TAG_AUTHOR, authorName);
        ListTag pageTags = new ListTag();
        for (String page : pages) {
            pageTags.add(StringTag.valueOf(page));
        }
        tag.put(TAG_PAGES, pageTags);
        return tag;
    }

    public static BookEntry fromTag(CompoundTag tag) {
        List<String> pages = new ArrayList<>();
        for (Tag pageTag : tag.getList(TAG_PAGES, Tag.TAG_STRING)) {
            pages.add(pageTag.getAsString());
        }
        return new BookEntry(
                tag.getInt(TAG_ID),
                tag.getUUID(TAG_LEADER),
                tag.getString(TAG_TITLE),
                tag.getString(TAG_AUTHOR),
                pages);
    }
}
