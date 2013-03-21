package com.neotys.nl.controller.report.transform;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FilenameUtils;
import org.jenkinsci.plugins.neoload_integration.supporting.XMLUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/** A wrapper for an xml document.
 * @author ajohnson
 * 
 */
public class NeoLoadReportDoc {

	/** The actual xml document. */
	private Document doc = null;

	/** Log various messages. */
	private static Logger logger = Logger.getLogger(NeoLoadReportDoc.class.getName());

	/** Constructor.
	 * @param xmlFilePath
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public NeoLoadReportDoc(String xmlFilePath) {
		try {
			if ((xmlFilePath != null) && 
					("xml".equalsIgnoreCase(FilenameUtils.getExtension(xmlFilePath)))) {

				doc = XMLUtilities.readXmlFile(xmlFilePath);
			} else {
				// to avoid npe
				doc = XMLUtilities.createNodeFromText("<empty></empty>").getOwnerDocument();
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			logger.log(Level.SEVERE, "Error reading xml file " + xmlFilePath + ". " + e.getMessage(), e);
		}
	}

	public NeoLoadReportDoc(Document doc) {
		this.doc = doc;
	}

	/**
	 * @return true if this is a valid NeoLoad report document. false otherwise.
	 * @throws XPathExpressionException
	 */
	public boolean isValidReportDoc() throws XPathExpressionException {
		List<Node> nodes = XMLUtilities.findByExpression("/report/summary/all-summary/statistic-item", doc);

		if ((nodes == null) || (nodes.size() == 0)) {
			return false;
		}

		return true;
	}

	/**
	 * @return
	 * @throws XPathExpressionException
	 */
	public Float getAverageResponseTime() throws XPathExpressionException {
		return getGenericAvgValue("/report/summary/all-summary/statistic-item", "virtualuser");
	}

	/**
	 * @return
	 * @throws XPathExpressionException
	 */
	public Float getErrorRatePercentage() throws XPathExpressionException {
		return getGenericAvgValue("/report/summary/all-summary/statistic-item", "httppage");
	}

	/**
	 * @return
	 * @throws XPathExpressionException
	 */
	private Float getGenericAvgValue(String path, String typeAttributeValue) throws XPathExpressionException {
		List<Node> nodes = XMLUtilities.findByExpression(path, doc);
		Float numVal = null;
		String val = null;

		// look at the nodes we found
		for (Node searchNode : nodes) {
			// look for the avg response time node
			val = XMLUtilities.findFirstValueByExpression("@type", searchNode);
			if (typeAttributeValue.equalsIgnoreCase(val)) {
				// this is the correct node. get the avg response time value.
				val = XMLUtilities.findFirstValueByExpression("@avg", searchNode);
				val = val.replaceAll(",", ".").replaceAll(" ", ""); // remove spaces etc
				val = val.replaceAll(Pattern.quote("%"), ""); // special case for percentages
				val = val.replaceAll(Pattern.quote("+"), ""); // special case for percentages
				if ("<0.01".equals(val)) { // special case for less than 0.01%
					numVal = 0f;
				} else {
					try {
						numVal = Float.valueOf(val);
					} catch (Exception e) {
						// we couldn't convert the result to an actual number so the value will not be included.
						// this could be +INF, -INF, " - ", NaN, etc. See com.neotys.nl.util.FormatUtils.java, getTextNumber().
					}
				}
			}
		}

		return numVal;
	}

	/** @return the doc */
	public Document getDoc() {
		return doc;
	}

}
