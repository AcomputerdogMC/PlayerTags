package net.acomputerdog.playertags;

/**
 * A tag for a player's name.  Uniquely identified by id, and sortable by priority.
 */
public class Tag implements Comparable<Tag> {
    public final String id;

    public final String chatTag;
    public final String listTag;

    public final String permission;
    public final int priority;

    public Tag(String id, String chatTag, String listTag, String permission, int priority) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null!");
        }
        this.id = id;
        this.chatTag = chatTag;
        this.listTag = listTag;
        this.permission = permission;
        this.priority = priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tag)) return false;

        Tag tag = (Tag) o;

        return id.equals(tag.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public int compareTo(Tag o) {
        return this.priority - o.priority;
    }
}
