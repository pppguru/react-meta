package org.visallo.core.model.properties.types;

import org.vertexium.Metadata;
import org.vertexium.Visibility;
import org.visallo.core.model.properties.VisalloProperties;
import org.visallo.core.user.User;
import org.visallo.web.clientapi.model.VisibilityJson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PropertyMetadata {
    private final Date modifiedDate;
    private final User modifiedBy;
    private final Double confidence;
    private final VisibilityJson visibilityJson;
    private final Visibility visibility;
    private final List<AdditionalMetadataItem> additionalMetadataItems = new ArrayList<>();

    public PropertyMetadata(User modifiedBy, VisibilityJson visibilityJson, Visibility visibility) {
        this(new Date(), modifiedBy, visibilityJson, visibility);
    }

    public PropertyMetadata(Date modifiedDate, User modifiedBy, VisibilityJson visibilityJson, Visibility visibility) {
        this(modifiedDate, modifiedBy, null, visibilityJson, visibility);
    }

    public PropertyMetadata(
            Date modifiedDate,
            User modifiedBy,
            Double confidence,
            VisibilityJson visibilityJson,
            Visibility visibility
    ) {
        this.modifiedDate = modifiedDate;
        this.modifiedBy = modifiedBy;
        this.confidence = confidence;
        this.visibilityJson = visibilityJson;
        this.visibility = visibility;
    }

    public PropertyMetadata(PropertyMetadata metadata) {
        this(
                metadata.getModifiedDate(),
                metadata.getModifiedBy(),
                metadata.getConfidence(),
                metadata.getVisibilityJson(),
                metadata.getVisibility()
        );
        for (AdditionalMetadataItem item : metadata.getAdditionalMetadataItems()) {
            add(item.getKey(), item.getValue(), item.getVisibility());
        }
    }

    public Metadata createMetadata() {
        Metadata metadata = new Metadata();
        VisalloProperties.MODIFIED_DATE_METADATA.setMetadata(metadata, modifiedDate, visibility);
        VisalloProperties.MODIFIED_BY_METADATA.setMetadata(metadata, modifiedBy.getUserId(), visibility);
        VisalloProperties.VISIBILITY_JSON_METADATA.setMetadata(metadata, visibilityJson, visibility);
        if (confidence != null) {
            VisalloProperties.CONFIDENCE_METADATA.setMetadata(metadata, confidence, visibility);
        }
        for (AdditionalMetadataItem additionalMetadataItem : additionalMetadataItems) {
            metadata.add(
                    additionalMetadataItem.getKey(),
                    additionalMetadataItem.getValue(),
                    additionalMetadataItem.getVisibility()
            );
        }
        return metadata;
    }

    public User getModifiedBy() {
        return modifiedBy;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public Double getConfidence() {
        return confidence;
    }

    public VisibilityJson getVisibilityJson() {
        return visibilityJson;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public Iterable<AdditionalMetadataItem> getAdditionalMetadataItems() {
        return additionalMetadataItems;
    }

    public void add(String key, Object value, Visibility visibility) {
        additionalMetadataItems.add(new AdditionalMetadataItem(key, value, visibility));
    }

    private static class AdditionalMetadataItem {
        private final String key;
        private final Object value;
        private final Visibility visibility;

        public AdditionalMetadataItem(String key, Object value, Visibility visibility) {
            this.key = key;
            this.value = value;
            this.visibility = visibility;
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return value;
        }

        public Visibility getVisibility() {
            return visibility;
        }
    }
}
