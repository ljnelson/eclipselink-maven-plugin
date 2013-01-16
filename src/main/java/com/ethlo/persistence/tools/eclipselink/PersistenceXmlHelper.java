package com.ethlo.persistence.tools.eclipselink;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

/**
 * 
 * @author Morten Haraldsen
 */
public class PersistenceXmlHelper
{
	public static final String xmlNs = "http://java.sun.com/xml/ns/persistence";

	private static final XPathFactory factory = XPathFactory.newInstance();
	
	public static Document createXml(String name)
	{
		try
		{
			final InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("empty-persistence.xml");
			final Document doc = getBuilder().parse(input);
			final Element puElement = (Element) doc.getElementsByTagName("persistence-unit").item(0);
			puElement.setAttribute("name", name);
			return doc;
		}
		catch (IOException | SAXException | ParserConfigurationException exc)
		{
			throw new RuntimeException(exc.getMessage(), exc);
		}
	}

	public static void appendClasses(Document doc, Set<String> entityClasses)
	{
		final Node parent = doc.getDocumentElement().getElementsByTagNameNS(xmlNs, "persistence-unit").item(0);
		
		for (String entity : entityClasses)
    	{
    		final Element element = doc.createElementNS(xmlNs, "class");
    		element.setTextContent(entity);
    		parent.appendChild(element);
    	}
	}

	public static Document parseXml(File targetFile)
	{
		try
		{
			return getBuilder().parse(targetFile);
		}
		catch (IOException | SAXException | ParserConfigurationException exc)
		{
			throw new RuntimeException(exc.getMessage(), exc);
		}
	}
	
	public static Set<String> getClassesAlreadyDefined(Document doc)
	{
		final XPath xpath = factory.newXPath();
		xpath.setNamespaceContext(new NamespaceContext()
		{
		    public String getNamespaceURI(String prefix)
		    {
		    	if ("ns".equals(prefix))
		    	{
		    		return xmlNs;
		    	}
		    	return XMLConstants.NULL_NS_URI;
		    }

		    public String getPrefix(String uri)
		    {
		        throw new UnsupportedOperationException();
		    }

		    public Iterator<String> getPrefixes(String uri)
		    {
		        throw new UnsupportedOperationException();
		    }
		});
		
		try
		{
			final XPathExpression expr = xpath.compile("/ns:persistence/ns:persistence-unit/ns:class");
			final NodeList res = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
			final Set<String> existing = new TreeSet<>();
			for (int i = 0; i < res.getLength(); i++)
			{
				final String existingClassName = res.item(i).getTextContent();
				existing.add(existingClassName);
			}
			return existing;
		}
		catch (XPathExpressionException e)
		{
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	private static DocumentBuilder getBuilder() throws ParserConfigurationException
	{
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
    	return dbFactory.newDocumentBuilder();
	}
	
	public static void outputXml(Document doc, File targetFile)
	{
		if (! targetFile.exists())
		{
			targetFile.getParentFile().mkdirs();
		}
		
		try (final Writer writer = new FileWriter(targetFile))
		{
			prettyPrint(doc, writer);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	public static void prettyPrint(Document document, Writer writer)
	{
		final DOMImplementation domImplementation = document.getImplementation();
		final DOMImplementationLS domImplementationLS = (DOMImplementationLS) domImplementation.getFeature("LS", "3.0");
		final LSSerializer lsSerializer = domImplementationLS.createLSSerializer();
		lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
		final LSOutput lsOutput = domImplementationLS.createLSOutput();
		lsOutput.setEncoding("UTF-8");
		lsOutput.setCharacterStream(writer);
		lsSerializer.write(document, lsOutput);
	}
}
