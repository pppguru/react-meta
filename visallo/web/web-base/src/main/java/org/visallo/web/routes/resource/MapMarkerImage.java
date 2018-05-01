package org.visallo.web.routes.resource;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.v5analytics.webster.ParameterizedHandler;
import com.v5analytics.webster.annotations.Handle;
import com.v5analytics.webster.annotations.Optional;
import com.v5analytics.webster.annotations.Required;
import org.visallo.core.model.artifactThumbnails.ArtifactThumbnailRepository;
import org.visallo.core.model.ontology.Concept;
import org.visallo.core.model.ontology.OntologyRepository;
import org.visallo.core.user.User;
import org.visallo.core.util.VisalloLogger;
import org.visallo.core.util.VisalloLoggerFactory;
import org.visallo.web.VisalloResponse;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class MapMarkerImage implements ParameterizedHandler {
    private static final VisalloLogger LOGGER = VisalloLoggerFactory.getLogger(MapMarkerImage.class);

    private final OntologyRepository ontologyRepository;
    private ArtifactThumbnailRepository artifactThumbnailRepository;
    private final Cache<String, byte[]> imageCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    @Inject
    public MapMarkerImage(
            final OntologyRepository ontologyRepository,
            final ArtifactThumbnailRepository artifactThumbnailRepository
    ) {
        this.ontologyRepository = ontologyRepository;
        this.artifactThumbnailRepository = artifactThumbnailRepository;
    }

    @Handle
    public void handle(
            @Required(name = "type") String typeStr,
            @Optional(name = "scale", defaultValue = "1") long scale,
            @Optional(name = "heading", defaultValue = "0.0") double headingParam,
            @Optional(name = "selected", defaultValue = "false") boolean selected,
            VisalloResponse response,
            User user
    ) throws Exception {
        int heading = roundHeadingAngle(headingParam);
        typeStr = typeStr.isEmpty() ? "http://www.w3.org/2002/07/owl#Thing" : typeStr;
        String cacheKey = typeStr + scale + heading + (selected ? "selected" : "unselected");
        byte[] imageData = imageCache.getIfPresent(cacheKey);
        if (imageData == null) {
            LOGGER.info("map marker cache miss %s (scale: %d, heading: %d)", typeStr, scale, heading);

            Concept concept = ontologyRepository.getConceptByIRI(typeStr);
            if (concept == null) {
                concept = ontologyRepository.getConceptByIRI(typeStr);
            }

            boolean isMapGlyphIcon = false;
            byte[] glyphIcon = getMapGlyphIcon(concept, user);
            if (glyphIcon != null) {
                isMapGlyphIcon = true;
            } else {
                glyphIcon = getGlyphIcon(concept, user);
                if (glyphIcon == null) {
                    response.respondWithNotFound();
                    return;
                }
            }

            imageData = getMarkerImage(new ByteArrayInputStream(glyphIcon), scale, selected, heading, isMapGlyphIcon);
            imageCache.put(cacheKey, imageData);
        }

        response.setHeader("Cache-Control", "max-age=" + (5 * 60));
        response.write(imageData);
    }

    private int roundHeadingAngle(double heading) {
        while (heading < 0.0) {
            heading += 360.0;
        }
        while (heading > 360.0) {
            heading -= 360.0;
        }
        return (int) (Math.round(heading / 10.0) * 10.0);
    }

    private byte[] getMarkerImage(InputStream resource, long scale, boolean selected, int heading, boolean isMapGlyphIcon) throws IOException {
        BufferedImage resourceImage = ImageIO.read(resource);
        if (resourceImage == null) {
            return null;
        }

        if (heading != 0) {
            resourceImage = rotateImage(resourceImage, heading);
        }

        BufferedImage backgroundImage = getBackgroundImage(scale, selected);
        if (backgroundImage == null) {
            return null;
        }
        int[] resourceImageDim = new int[]{resourceImage.getWidth(), resourceImage.getHeight()};

        BufferedImage image = new BufferedImage(backgroundImage.getWidth(), backgroundImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        if (isMapGlyphIcon) {
            int[] boundary = new int[]{backgroundImage.getWidth(), backgroundImage.getHeight()};
            int[] scaledDims = artifactThumbnailRepository.getScaledDimension(resourceImageDim, boundary);
            g.drawImage(resourceImage, 0, 0, scaledDims[0], scaledDims[1], null);
        } else {
            g.drawImage(backgroundImage, 0, 0, backgroundImage.getWidth(), backgroundImage.getHeight(), null);
            int size = image.getWidth() * 2 / 3;
            int[] boundary = new int[]{size, size};
            int[] scaledDims = artifactThumbnailRepository.getScaledDimension(resourceImageDim, boundary);
            int x = (backgroundImage.getWidth() - scaledDims[0]) / 2;
            int y = (backgroundImage.getWidth() - scaledDims[1]) / 2;
            g.drawImage(resourceImage, x, y, scaledDims[0], scaledDims[1], null);
        }
        g.dispose();
        return imageToBytes(image);
    }

    private BufferedImage rotateImage(BufferedImage image, int angleDeg) {
        BufferedImage rotatedImage = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics2D g = rotatedImage.createGraphics();
        g.rotate(Math.toRadians(angleDeg), rotatedImage.getWidth() / 2, rotatedImage.getHeight() / 2);
        g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        g.dispose();
        return rotatedImage;
    }

    private BufferedImage getBackgroundImage(long scale, boolean selected) throws IOException {
        String imageFileName;
        if (scale == 1) {
            imageFileName = selected ? "marker-background-selected.png" : "marker-background.png";
        } else if (scale == 2) {
            imageFileName = selected ? "marker-background-selected-2x.png" : "marker-background-2x.png";
        } else {
            return null;
        }

        try (InputStream resourceInputStream = MapMarkerImage.class.getResourceAsStream(imageFileName)) {
            checkNotNull(resourceInputStream, "Could not find image resource: " + imageFileName);
            return ImageIO.read(resourceInputStream);
        }
    }

    private byte[] imageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream imageData = new ByteArrayOutputStream();
        ImageIO.write(image, "png", imageData);
        imageData.close();
        return imageData.toByteArray();
    }

    private byte[] getMapGlyphIcon(Concept concept, User user) {
        byte[] mapGlyphIcon = null;
        for (Concept con = concept; mapGlyphIcon == null && con != null; con = ontologyRepository.getParentConcept(con)) {
            mapGlyphIcon = con.getMapGlyphIcon();
        }
        return mapGlyphIcon;
    }

    private byte[] getGlyphIcon(Concept concept, User user) {
        byte[] glyphIcon = null;
        for (Concept con = concept; glyphIcon == null && con != null; con = ontologyRepository.getParentConcept(con)) {
            glyphIcon = con.hasGlyphIconResource() ? con.getGlyphIcon() : null;
        }
        return glyphIcon;
    }
}
