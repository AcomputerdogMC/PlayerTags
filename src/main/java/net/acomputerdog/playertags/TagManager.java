package net.acomputerdog.playertags;

import java.util.*;
import java.util.function.Predicate;

/**
 * Stores and sorts tags
 */
public class TagManager {
    /**
     * Map of String IDs to Tag instances
     */
    private final Map<String, Tag> idMap = new HashMap<>();

    /**
     * List of tags sorted by priority
     */
    private final List<Tag> priorityList = new ArrayList<>();

    /**
     * Adds a tag to the registry, if it does not already exist
     *
     * @param tag The tag to add.
     */
    public void addTag(Tag tag) {
        if (tag == null) {
            throw new IllegalArgumentException("Tag cannot be null!");
        }
        if (containsTag(tag)) {
            throw new IllegalArgumentException("Duplicate tag ID!");
        }
        idMap.put(tag.id, tag);
        addPriority(tag);
    }

    /**
     * Adds a tag to the priority list.  Does not check for existence, so the passed tag MUST be new!
     *
     * @param tag The tag to add.  Cannot already exist in list.
     */
    private void addPriority(Tag tag) {
        priorityList.add(tag); //add the tag to the list
        priorityList.sort(Tag::compareTo); //sort the list using the Tag compare method
    }

    /**
     * Checks if the registry contains the specified tag
     *
     * @param tag The tag to check
     * @return return true if the tag exists
     */
    public boolean containsTag(Tag tag) {
        return tag != null && containsId(tag.id);
    }

    /**
     * Checks if the registry contains a specified ID
     *
     * @param id The ID to check
     * @return return true if the ID exists
     */
    public boolean containsId(String id) {
        return id != null && idMap.containsKey(id);
    }

    /**
     * Gets an immutable map of Tag IDs to Tags
     *
     * @return return the ID map
     */
    public Map<String, Tag> getIdMap() {
        return Collections.unmodifiableMap(idMap);
    }

    /**
     * Gets an immutable sorted list of Tags
     *
     * @return the priority-sorted list of tags
     */
    public List<Tag> getPriorityList() {
        return Collections.unmodifiableList(priorityList);
    }

    /**
     * Creates a list of tags matching the specified filter
     *
     * @param filter The filter
     */
    public List<Tag> matchTags(Predicate<Tag> filter) {
        List<Tag> tags = new ArrayList<>();
        for (Tag tag : priorityList) {
            if (filter.test(tag)) {
                tags.add(tag);
            }
        }
        return tags;
    }
}
