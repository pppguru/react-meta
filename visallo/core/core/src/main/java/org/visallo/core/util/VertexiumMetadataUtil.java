package org.visallo.core.util;

import org.json.JSONObject;
import org.vertexium.Metadata;
import org.vertexium.Visibility;

public class VertexiumMetadataUtil {
    public static Metadata metadataStringToMap(String metadataString, Visibility visibility) {
        Metadata metadata = new Metadata();
        if (metadataString != null && metadataString.length() > 0) {
            JSONObject metadataJson = new JSONObject(metadataString);
            for (Object keyObj : metadataJson.keySet()) {
                String key = "" + keyObj;
                metadata.add(key, metadataJson.get(key), visibility);
            }
        }
        return metadata;
    }

    public static Metadata mergeMetadata(Metadata... metadatas) {
        Metadata mergedMetadata = new Metadata();
        for (Metadata metadata : metadatas) {
            for (Metadata.Entry entry : metadata.entrySet()) {
                mergedMetadata.add(entry.getKey(), entry.getValue(), entry.getVisibility());
            }
        }
        return mergedMetadata;
    }
}
