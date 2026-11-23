// src/com/toda/structure/TricycleQueue.java
package com.toda.structure;

import com.toda.model.QueuedTricycle;

/**
 * Specialised queue for the terminal line. Adds body-number helpers on top of the generic
 * {@link CustomQueue} so callers never have to write the same lookup lambda twice.
 */
public class TricycleQueue extends CustomQueue<QueuedTricycle> {

    public QueuedTricycle findByBodyNumber(String bodyNo) {
        return find(t -> t.getBodyNumber().equalsIgnoreCase(bodyNo));
    }

    public QueuedTricycle removeByBodyNumber(String bodyNo) {
        return remove(t -> t.getBodyNumber().equalsIgnoreCase(bodyNo));
    }

    public boolean moveToFront(String bodyNo) {
        return moveToFront(t -> t.getBodyNumber().equalsIgnoreCase(bodyNo));
    }

    public boolean moveUp(String bodyNo) {
        return moveUp(t -> t.getBodyNumber().equalsIgnoreCase(bodyNo));
    }

    public int positionOf(String bodyNo) {
        return positionOf(t -> t.getBodyNumber().equalsIgnoreCase(bodyNo));
    }
}
