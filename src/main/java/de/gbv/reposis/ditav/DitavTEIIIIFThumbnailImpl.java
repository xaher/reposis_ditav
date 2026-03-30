package de.gbv.reposis.ditav;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.mycore.datamodel.metadata.MCRDerivate;
import org.mycore.datamodel.metadata.MCRExpandedObject;
import org.mycore.datamodel.metadata.MCRMetaEnrichedLinkID;
import org.mycore.datamodel.metadata.MCRMetadataManager;
import org.mycore.datamodel.metadata.MCRObjectID;
import org.mycore.datamodel.niofs.MCRPath;
import org.mycore.iiif.image.impl.MCRIIIFImageNotFoundException;
import org.mycore.iview2.backend.MCRTileInfo;
import org.mycore.iview2.iiif.MCRThumbnailImageImpl;

public class DitavTEIIIIFThumbnailImpl extends MCRThumbnailImageImpl {

  private static final Logger LOGGER = LogManager.getLogger();
  private static final Namespace TEI_NAMESPACE = Namespace.getNamespace("tei", "http://www.tei-c.org/ns/1.0");

  public DitavTEIIIIFThumbnailImpl(String implName) {
    super(implName);
  }

  @Override
  protected MCRTileInfo createTileInfo(String id) throws MCRIIIFImageNotFoundException {
    if (MCRObjectID.isValid(id)) {
      MCRObjectID objectID = MCRObjectID.getInstance(id);
      if (!MCRDerivate.OBJECT_TYPE.equals(objectID.getTypeId())) {
        MCRExpandedObject mcrObject = MCRMetadataManager.retrieveMCRExpandedObject(objectID);

        for (MCRMetaEnrichedLinkID derlink : mcrObject.getStructure().getDerivates()) {
          MCRObjectID derID = derlink.getXLinkHrefID();
          String mainDoc = derlink.getMainDoc();
          MCRPath thumbnailPath = extractThumbnailPath(derID, mainDoc);
          if (thumbnailPath != null) {
            return new MCRTileInfo(derID.toString(), thumbnailPath.getOwnerRelativePath(), null);
          }
        }
      }
    }

    return super.createTileInfo(id);
  }

  protected MCRPath extractThumbnailPath(MCRObjectID derid, String maindoc) {
    if (derid == null || maindoc == null) {
      return null;
    }

    if (!maindoc.endsWith(".xml")) {
      return null;
    }

    MCRPath teiPath = MCRPath.getPath(derid.toString(), maindoc);

    List<String> facs;
    try (InputStream is = Files.newInputStream(teiPath)) {
      SAXBuilder saxBuilder = new SAXBuilder();
      Document document = saxBuilder.build(is);
      // extract pb facs attribute with xpath - find pb elements with non-empty facs attribute
      XPathExpression<Element> xpath = XPathFactory.instance()
          .compile("//tei:pb[@facs and string-length(@facs) > 0]",
                   org.jdom2.filter.Filters.element(),
                   null,
                   TEI_NAMESPACE);
      facs = xpath.evaluate(document).stream()
          .map(e -> e.getAttributeValue("facs"))
          .filter(Objects::nonNull)
          .toList();

    } catch (IOException | JDOMException e) {
      LOGGER.error("Error reading TEI file for thumbnail extraction: {}", teiPath, e);
      return null;
    }

    // check if facs referenced files exist
    // Workaround for MCR-3660: MCRPath.normalize() incorrectly removes leading ".." segments,
    // so we normalize using java.nio.file.Path instead.
    String parentRelPath = teiPath.getParent().getOwnerRelativePath();
    for (String fac : facs) {
        String normalizedPath = Path.of(parentRelPath).resolve(fac).normalize().toString();
        MCRPath facPath = MCRPath.getPath(derid.toString(), normalizedPath);
        MCRPath fromRootPath = MCRPath.getPath(derid.toString(), fac);
      if (Files.exists(facPath)) {
        return facPath;
      } else if (Files.exists(fromRootPath)) {
        return fromRootPath;
      }
    }

    return null;
  }
}
