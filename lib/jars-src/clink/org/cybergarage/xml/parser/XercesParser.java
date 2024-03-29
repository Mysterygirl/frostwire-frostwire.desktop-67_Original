/******************************************************************
*
*	CyberXML for Java
*
*	Copyright (C) Satoshi Konno 2002
*
*	File: XercesParser.java
*
*	Revision;
*
*	11/26/02
*		- first revision.
*	12/26/03
*		- Changed to the file name from Parser.java to XercesParser.java.
*		- Changed to implement org.cybergarage.xml.Parser interface.
*
******************************************************************/

package org.cybergarage.xml.parser;

import java.io.*;

import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.cybergarage.xml.*;

public class XercesParser extends org.cybergarage.xml.Parser
{
	////////////////////////////////////////////////
	//	Constructor
	////////////////////////////////////////////////

	public XercesParser()
	{
	}

	////////////////////////////////////////////////
	//	parse (Node)
	////////////////////////////////////////////////

	public org.cybergarage.xml.Node parse(org.cybergarage.xml.Node parentNode, org.w3c.dom.Node domNode, int rank)
	{
		int domNodeType = domNode.getNodeType();
//		if (domNodeType != Node.ELEMENT_NODE)
//			return;
			
		String domNodeName = domNode.getNodeName();
		String domNodeValue = domNode.getNodeValue();
		NamedNodeMap attrs = domNode.getAttributes(); 
		int arrrsLen = (attrs != null) ? attrs.getLength() : 0;

//		Debug.message("[" + rank + "] ELEM : " + domNodeName + ", " + domNodeValue + ", type = " + domNodeType + ", attrs = " + arrrsLen);

		if (domNodeType == org.w3c.dom.Node.TEXT_NODE) {
			parentNode.setValue(domNodeValue);
			return parentNode;
		}

		if (domNodeType != org.w3c.dom.Node.ELEMENT_NODE)
			return parentNode;

		org.cybergarage.xml.Node node = new org.cybergarage.xml.Node();
		node.setName(domNodeName);
		node.setValue(domNodeValue);

		if (parentNode != null)
			parentNode.addNode(node);

		NamedNodeMap attrMap = domNode.getAttributes(); 
		int attrLen = attrMap.getLength();
		//Debug.message("attrLen = " + attrLen);
		for (int n = 0; n<attrLen; n++) {
			org.w3c.dom.Node attr = attrMap.item(n);
			String attrName = attr.getNodeName();
			String attrValue = attr.getNodeValue();
			node.setAttribute(attrName, attrValue);
		}
		
		org.w3c.dom.Node child = domNode.getFirstChild();
		while (child != null) {
			parse(node, child, rank+1);
			child = child.getNextSibling();
		}
		
		return node;
	}

	public org.cybergarage.xml.Node parse(org.cybergarage.xml.Node parentNode, org.w3c.dom.Node domNode)
	{
		return parse(parentNode, domNode, 0);
	}
	
	////////////////////////////////////////////////
	//	parse
	////////////////////////////////////////////////

	public org.cybergarage.xml.Node parse(InputStream inStream) throws ParserException
	{
		org.cybergarage.xml.Node root = null;
		
		try {
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			InputSource inSrc = new InputSource(inStream);

			Document doc = parser.parse(inSrc);
			org.w3c.dom.Element docElem = doc.getDocumentElement();

			if (docElem != null)
				root = parse(root, docElem);
/*
			NodeList rootList = doc.getElementsByTagName("root");
			Debug.message("rootList = " + rootList.getLength());
			
			if (0 < rootList.getLength())
				root = parse(root, rootList.item(0));
*/
		}
		catch (Exception e) {
			throw new ParserException(e);
		}
		
		return root;
	}
	
}

