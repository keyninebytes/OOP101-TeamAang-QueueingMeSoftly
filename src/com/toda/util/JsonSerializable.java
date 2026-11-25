// src/com/toda/util/JsonSerializable.java
package com.toda.util;

import java.util.Map;

/** Anything that can describe itself as a JSON object (keeps serialisation inside each class). */
public interface JsonSerializable {
    Map<String, Object> toJson();
}
