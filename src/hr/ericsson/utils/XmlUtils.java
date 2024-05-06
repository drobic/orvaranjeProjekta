package hr.ericsson.utils;

import javax.xml.parsers.DocumentBuilderFactory;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.*;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

public class XmlUtils {
	public String getIzvadak(InputStream xmlContent) {
		Document doc=null;
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = dbf.newDocumentBuilder();
			doc = builder.parse(xmlContent);
			
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
		return extractPDF(doc);
	}

	public String extractPDF(Document doc) {
		Node node=null;
		try {
			XPath xPath = XPathFactory.newInstance().newXPath();
			node = (Node) xPath.evaluate("/Envelope/Body/DocumentResponse/Data", doc, XPathConstants.NODE);
			//node = (Node) xPath.evaluate("/Envelope/Body/SearchResponse/Dokument/KatastarskaOpcina", doc, XPathConstants.NODE);
			//node = (Node) xPath.evaluate("/Envelope/Body/Fault/faultcode", doc, XPathConstants.NODE);

		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
		return node.getTextContent();
	}
		
}
