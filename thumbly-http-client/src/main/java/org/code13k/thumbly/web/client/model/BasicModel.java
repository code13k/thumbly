package org.code13k.thumbly.web.client.model;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class BasicModel implements Serializable {
    /**
     * toMap()
     */
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        try {
            Field[] fields = ((Object) this).getClass().getDeclaredFields();
            for (Field field : fields) {
                try {
                    if (field.getName().equals("mLogger") == false) {
                        field.setAccessible(true);
                        result.put(field.getName(), field.get(this));
                        field.setAccessible(false);
                    }
                } catch (IllegalAccessException ex) {
                    // Nothing
                }
            }
        } catch (Exception e) {
            // Nothing
        } finally {
            return result;
        }
    }

    /**
     * toString()
     */
    @Override
    public String toString() {
        return toMap().toString();
    }
}
