package org.visallo.core.model.properties.types;

import org.apache.commons.io.IOUtils;
import org.vertexium.Element;
import org.vertexium.property.StreamingPropertyValue;
import org.visallo.core.exception.VisalloException;

import java.io.IOException;

/**
 * An IdentityVisalloProperty for StreamingPropertyValues.
 */
public class StreamingVisalloProperty extends IdentityVisalloProperty<StreamingPropertyValue> {
    /**
     * Create a new StreamingVisalloProperty.
     *
     * @param key the property key
     */
    public StreamingVisalloProperty(String key) {
        super(key);
    }

    public byte[] getFirstPropertyValueAsBytes(Element element) {
        StreamingPropertyValue propertyValue = getFirstPropertyValue(element);
        if (propertyValue == null) {
            return null;
        }
        try {
            return IOUtils.toByteArray(propertyValue.getInputStream());
        } catch (IOException e) {
            throw new VisalloException("Could not get byte[] from StreamingPropertyValue", e);
        }
    }

    public String getFirstPropertyValueAsString(Element element) {
        StreamingPropertyValue propertyValue = getFirstPropertyValue(element);
        if (propertyValue == null) {
            return null;
        }
        try {
            return IOUtils.toString(propertyValue.getInputStream());
        } catch (IOException e) {
            throw new VisalloException("Could not get string from StreamingPropertyValue", e);
        }
    }
}
