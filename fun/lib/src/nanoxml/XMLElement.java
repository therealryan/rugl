/*
 * XMLElement.java $Revision: 1.4 $ $Date: 2002/03/24 10:27:59 $
 * $Name: RELEASE_2_2_1 $ This file is part of NanoXML 2 Lite.
 * Copyright (C) 2000-2002 Marc De Scheemaecker, All Rights Reserved.
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any
 * damages arising from the use of this software. Permission is
 * granted to anyone to use this software for any purpose, including
 * commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions: 1. The origin of
 * this software must not be misrepresented; you must not claim that
 * you wrote the original software. If you use this software in a
 * product, an acknowledgment in the product documentation would be
 * appreciated but is not required. 2. Altered source versions must be
 * plainly marked as such, and must not be misrepresented as being the
 * original software. 3. This notice may not be removed or altered
 * from any source distribution.
 */

package nanoxml;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * XMLElement is a representation of an XML object. The object is able
 * to parse XML code.
 * <P>
 * <DL>
 * <DT><B>Parsing XML Data</B></DT>
 * <DD>You can parse XML data using the following code:
 * <UL>
 * <CODE>
 * XMLElement xml = new XMLElement();<BR>
 * FileReader reader = new FileReader("filename.xml");<BR>
 * xml.parseFromReader(reader);
 * </CODE>
 * </UL>
 * </DD>
 * </DL>
 * <DL>
 * <DT><B>Retrieving Attributes</B></DT>
 * <DD>You can enumerate the attributes of an element using the method
 * {@link #enumerateAttributeNames() enumerateAttributeNames}. The
 * attribute values can be retrieved using the method
 * {@link #getStringAttribute(java.lang.String) getStringAttribute}.
 * The following example shows how to list the attributes of an
 * element:
 * <UL>
 * <CODE>
 * XMLElement element = ...;<BR>
 * Enumeration enum = element.getAttributeNames();<BR>
 * while (enum.hasMoreElements()) {<BR>
 * &nbsp;&nbsp;&nbsp;&nbsp;String key = (String) enum.nextElement();<BR>
 * &nbsp;&nbsp;&nbsp;&nbsp;String value = element.getStringAttribute(key);<BR>
 * &nbsp;&nbsp;&nbsp;&nbsp;System.out.println(key + " = " + value);<BR>
 * }
 * </CODE>
 * </UL>
 * </DD>
 * </DL>
 * <DL>
 * <DT><B>Retrieving Child Elements</B></DT>
 * <DD>You can enumerate the children of an element using
 * {@link #enumerateChildren() enumerateChildren}. The number of child
 * elements can be retrieved using {@link #countChildren()
 * countChildren}.</DD>
 * </DL>
 * <DL>
 * <DT><B>Elements Containing Character Data</B></DT>
 * <DD>If an elements contains character data, like in the following
 * example:
 * <UL>
 * <CODE>
 * &lt;title&gt;The Title&lt;/title&gt;
 * </CODE>
 * </UL>
 * you can retrieve that data using the method {@link #getContent()
 * getContent}.</DD>
 * </DL>
 * <DL>
 * <DT><B>Subclassing XMLElement</B></DT>
 * <DD>When subclassing XMLElement, you need to override the method
 * {@link #createAnotherElement() createAnotherElement} which has to
 * return a new copy of the receiver.</DD>
 * </DL>
 * <P>
 * 
 * @see nanoxml.XMLParseException
 * @author Marc De Scheemaecker &lt;<A
 *         href="mailto:cyberelf@mac.com">cyberelf@mac.com</A>&gt;
 *         Made a bit more nano by ryanm, via the expedient of
 *         removing stuff I don't want
 */
public class XMLElement
{

	/**
	 * The attributes given to the element.
	 * <dl>
	 * <dt><b>Invariants:</b></dt>
	 * <dd>
	 * <ul>
	 * <li>The field can be empty.
	 * <li>The field is never <code>null</code>.
	 * <li>The keys and the values are strings.
	 * </ul>
	 * </dd>
	 * </dl>
	 */
	private Hashtable<String, String> attributes;

	/**
	 * Child elements of the element.
	 * <dl>
	 * <dt><b>Invariants:</b></dt>
	 * <dd>
	 * <ul>
	 * <li>The field can be empty.
	 * <li>The field is never <code>null</code>.
	 * <li>The elements are instances of <code>XMLElement</code> or a
	 * subclass of <code>XMLElement</code>.
	 * </ul>
	 * </dd>
	 * </dl>
	 */
	private ArrayList<XMLElement> children;

	/**
	 * The name of the element.
	 * <dl>
	 * <dt><b>Invariants:</b></dt>
	 * <dd>
	 * <ul>
	 * <li>The field is <code>null</code> iff the element is not
	 * initialized by either parse or setName.
	 * <li>If the field is not <code>null</code>, it's not empty.
	 * <li>If the field is not <code>null</code>, it contains a valid
	 * XML identifier.
	 * </ul>
	 * </dd>
	 * </dl>
	 */
	private String name;

	/**
	 * The #PCDATA content of the object.
	 * <dl>
	 * <dt><b>Invariants:</b></dt>
	 * <dd>
	 * <ul>
	 * <li>The field is <code>null</code> iff the element is not a
	 * #PCDATA element.
	 * <li>The field can be any string, including the empty string.
	 * </ul>
	 * </dd>
	 * </dl>
	 */
	private String contents = null;

	/**
	 * Conversion table for &amp;...; entities. The keys are the entity
	 * names without the &amp; and ; delimiters.
	 * <dl>
	 * <dt><b>Invariants:</b></dt>
	 * <dd>
	 * <ul>
	 * <li>The field is never <code>null</code>.
	 * <li>The field always contains the following associations:
	 * "lt"&nbsp;=&gt;&nbsp;"&lt;", "gt"&nbsp;=&gt;&nbsp;"&gt;",
	 * "quot"&nbsp;=&gt;&nbsp;"\"", "apos"&nbsp;=&gt;&nbsp;"'",
	 * "amp"&nbsp;=&gt;&nbsp;"&amp;"
	 * <li>The keys are strings
	 * <li>The values are char arrays
	 * </ul>
	 * </dd>
	 * </dl>
	 */
	private Hashtable<String, char[]> entities;

	/**
	 * The line number where the element starts.
	 * <dl>
	 * <dt><b>Invariants:</b></dt>
	 * <dd>
	 * <ul>
	 * <li><code>lineNr &gt= 0</code>
	 * </ul>
	 * </dd>
	 * </dl>
	 */
	private int lineNr;

	/**
	 * <code>true</code> if the case of the element and attribute names
	 * are case insensitive.
	 */
	private boolean ignoreCase;

	/**
	 * <code>true</code> if the leading and trailing whitespace of
	 * #PCDATA sections have to be ignored.
	 */
	private boolean ignoreWhitespace;

	/**
	 * Character read too much. This character provides push-back
	 * functionality to the input reader without having to use a
	 * PushbackReader. If there is no such character, this field is
	 * '\0'.
	 */
	private char charReadTooMuch;

	/**
	 * The reader provided by the caller of the parse method.
	 * <dl>
	 * <dt><b>Invariants:</b></dt>
	 * <dd>
	 * <ul>
	 * <li>The field is not <code>null</code> while the parse method is
	 * running.
	 * </ul>
	 * </dd>
	 * </dl>
	 */
	private Reader reader;

	/**
	 * The current line number in the source content.
	 * <dl>
	 * <dt><b>Invariants:</b></dt>
	 * <dd>
	 * <ul>
	 * <li>parserLineNr &gt; 0 while the parse method is running.
	 * </ul>
	 * </dd>
	 * </dl>
	 */
	private int parserLineNr;

	/**
	 * Creates and initializes a new XML element. Calling the
	 * construction is equivalent to:
	 * <ul>
	 * <code>new XMLElement(new Hashtable(), false, true)
     * </code>
	 * </ul>
	 * <dl>
	 * <dt><b>Postconditions:</b></dt>
	 * <dd>
	 * <ul>
	 * <li>countChildren() => 0
	 * <li>enumerateChildren() => empty enumeration
	 * <li>enumeratePropertyNames() => empty enumeration
	 * <li>getChildren() => empty vector
	 * <li>getContent() => ""
	 * <li>getLineNr() => 0
	 * <li>getName() => null
	 * </ul>
	 * </dd>
	 * </dl>
	 * 
	 * @see nanoxml.XMLElement#XMLElement(java.util.Hashtable)
	 *      XMLElement(Hashtable)
	 * @see nanoxml.XMLElement#XMLElement(boolean)
	 * @see nanoxml.XMLElement#XMLElement(java.util.Hashtable,boolean)
	 *      XMLElement(Hashtable, boolean)
	 */
	public XMLElement()
	{
		this( new Hashtable<String, char[]>(), false, true, true );
	}

	/**
	 * Creates and initializes a new XML element. Calling the
	 * construction is equivalent to:
	 * <ul>
	 * <code>new XMLElement(entities, false, true)
     * </code>
	 * </ul>
	 * 
	 * @param entities
	 *           The entity conversion table. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>entities != null</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 *           <dt><b>Postconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>countChildren() => 0
	 *           <li>enumerateChildren() => empty enumeration
	 *           <li>enumeratePropertyNames() => empty enumeration
	 *           <li>getChildren() => empty vector
	 *           <li>getContent() => ""
	 *           <li>getLineNr() => 0
	 *           <li>getName() => null
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 * @see nanoxml.XMLElement#XMLElement()
	 * @see nanoxml.XMLElement#XMLElement(boolean)
	 * @see nanoxml.XMLElement#XMLElement(java.util.Hashtable,boolean)
	 *      XMLElement(Hashtable, boolean)
	 */
	public XMLElement( Hashtable<String, char[]> entities )
	{
		this( entities, false, true, true );
	}

	/**
	 * Creates and initializes a new XML element. Calling the
	 * construction is equivalent to:
	 * <ul>
	 * <code>new XMLElement(new Hashtable(), skipLeadingWhitespace, true)
     * </code>
	 * </ul>
	 * 
	 * @param skipLeadingWhitespace
	 *           <code>true</code> if leading and trailing whitespace
	 *           in PCDATA content has to be removed. </dl>
	 *           <dl>
	 *           <dt><b>Postconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>countChildren() => 0
	 *           <li>enumerateChildren() => empty enumeration
	 *           <li>enumeratePropertyNames() => empty enumeration
	 *           <li>getChildren() => empty vector
	 *           <li>getContent() => ""
	 *           <li>getLineNr() => 0
	 *           <li>getName() => null
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 * @see nanoxml.XMLElement#XMLElement()
	 * @see nanoxml.XMLElement#XMLElement(java.util.Hashtable)
	 *      XMLElement(Hashtable)
	 * @see nanoxml.XMLElement#XMLElement(java.util.Hashtable,boolean)
	 *      XMLElement(Hashtable, boolean)
	 */
	public XMLElement( boolean skipLeadingWhitespace )
	{
		this( new Hashtable<String, char[]>(), skipLeadingWhitespace, true, true );
	}

	/**
	 * Creates and initializes a new XML element. Calling the
	 * construction is equivalent to:
	 * <ul>
	 * <code>new XMLElement(entities, skipLeadingWhitespace, true)
     * </code>
	 * </ul>
	 * 
	 * @param entities
	 *           The entity conversion table.
	 * @param skipLeadingWhitespace
	 *           <code>true</code> if leading and trailing whitespace
	 *           in PCDATA content has to be removed. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>entities != null</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 *           <dt><b>Postconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>countChildren() => 0
	 *           <li>enumerateChildren() => empty enumeration
	 *           <li>enumeratePropertyNames() => empty enumeration
	 *           <li>getChildren() => empty vector
	 *           <li>getContent() => ""
	 *           <li>getLineNr() => 0
	 *           <li>getName() => null
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 * @see nanoxml.XMLElement#XMLElement()
	 * @see nanoxml.XMLElement#XMLElement(boolean)
	 * @see nanoxml.XMLElement#XMLElement(java.util.Hashtable)
	 *      XMLElement(Hashtable)
	 */
	public XMLElement( Hashtable<String, char[]> entities, boolean skipLeadingWhitespace )
	{
		this( entities, skipLeadingWhitespace, true, true );
	}

	/**
	 * Creates and initializes a new XML element.
	 * 
	 * @param entities
	 *           The entity conversion table.
	 * @param skipLeadingWhitespace
	 *           <code>true</code> if leading and trailing whitespace
	 *           in PCDATA content has to be removed.
	 * @param ignoreCase
	 *           <code>true</code> if the case of element and attribute
	 *           names have to be ignored. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>entities != null</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 *           <dt><b>Postconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>countChildren() => 0
	 *           <li>enumerateChildren() => empty enumeration
	 *           <li>enumeratePropertyNames() => empty enumeration
	 *           <li>getChildren() => empty vector
	 *           <li>getContent() => ""
	 *           <li>getLineNr() => 0
	 *           <li>getName() => null
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 * @see nanoxml.XMLElement#XMLElement()
	 * @see nanoxml.XMLElement#XMLElement(boolean)
	 * @see nanoxml.XMLElement#XMLElement(java.util.Hashtable)
	 *      XMLElement(Hashtable)
	 * @see nanoxml.XMLElement#XMLElement(java.util.Hashtable,boolean)
	 *      XMLElement(Hashtable, boolean)
	 */
	public XMLElement( Hashtable<String, char[]> entities, boolean skipLeadingWhitespace,
			boolean ignoreCase )
	{
		this( entities, skipLeadingWhitespace, true, ignoreCase );
	}

	/**
	 * Creates and initializes a new XML element.
	 * <P>
	 * This constructor should <I>only</I> be called from
	 * {@link #createAnotherElement() createAnotherElement} to create
	 * child elements.
	 * 
	 * @param entities
	 *           The entity conversion table.
	 * @param skipLeadingWhitespace
	 *           <code>true</code> if leading and trailing whitespace
	 *           in PCDATA content has to be removed.
	 * @param fillBasicConversionTable
	 *           <code>true</code> if the basic entities need to be
	 *           added to the entity list.
	 * @param ignoreCase
	 *           <code>true</code> if the case of element and attribute
	 *           names have to be ignored. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt><dd>
	 *           <ul>
	 *           <li><code>entities != null</code> <li>if <code>
	 *           fillBasicConversionTable == false</code> then <code>
	 *           entities</code> contains at least the following
	 *           entries: <code>amp</code>, <code>lt</code>, <code>gt
	 *           </code>, <code>apos</code> and <code>quot</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 *           <dt><b>Postconditions:</b></dt><dd>
	 *           <ul>
	 *           <li>countChildren() => 0 <li>enumerateChildren() =>
	 *           empty enumeration <li>enumeratePropertyNames() =>
	 *           empty enumeration <li>getChildren() => empty vector
	 *           <li>getContent() => "" <li>getLineNr() => 0 <li>
	 *           getName() => null
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 * @see nanoxml.XMLElement#createAnotherElement()
	 */
	protected XMLElement( Hashtable<String, char[]> entities, boolean skipLeadingWhitespace,
			boolean fillBasicConversionTable, boolean ignoreCase )
	{
		ignoreWhitespace = skipLeadingWhitespace;
		this.ignoreCase = ignoreCase;
		name = null;
		contents = "";
		attributes = new Hashtable<String, String>();
		children = new ArrayList<XMLElement>();
		this.entities = entities;
		lineNr = 0;

		if( fillBasicConversionTable )
		{
			this.entities.put( "amp", new char[] { '&' } );
			this.entities.put( "quot", new char[] { '"' } );
			this.entities.put( "apos", new char[] { '\'' } );
			this.entities.put( "lt", new char[] { '<' } );
			this.entities.put( "gt", new char[] { '>' } );
		}
	}

	/**
	 * Adds a child element.
	 * 
	 * @param child
	 *           The child element to add. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>child != null</code>
	 *           <li><code>child.getName() != null</code>
	 *           <li><code>child</code> does not have a parent element
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 *           <dt><b>Postconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>countChildren() => old.countChildren() + 1
	 *           <li>enumerateChildren() => old.enumerateChildren() +
	 *           child
	 *           <li>getChildren() => old.enumerateChildren() + child
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 * @see nanoxml.XMLElement#countChildren()
	 * @see nanoxml.XMLElement#enumerateChildren()
	 * @see nanoxml.XMLElement#getChildren()
	 * @see nanoxml.XMLElement#removeChild(nanoxml.XMLElement)
	 *      removeChild(XMLElement)
	 */
	public void addChild( XMLElement child )
	{
		children.add( child );
	}

	/**
	 * Adds or modifies an attribute.
	 * 
	 * @param name
	 *           The name of the attribute.
	 * @param value
	 *           The value of the attribute. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>name != null</code>
	 *           <li><code>name</code> is a valid XML identifier
	 *           <li><code>value != null</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 *           <dt><b>Postconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>enumerateAttributeNames() =>
	 *           old.enumerateAttributeNames() + name
	 *           <li>getAttribute(name) => value
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 * @see nanoxml.XMLElement#setDoubleAttribute(java.lang.String,
	 *      double) setDoubleAttribute(String, double)
	 * @see nanoxml.XMLElement#setIntAttribute(java.lang.String, int)
	 *      setIntAttribute(String, int)
	 * @see nanoxml.XMLElement#enumerateAttributeNames()
	 * @see nanoxml.XMLElement#getAttribute(java.lang.String)
	 *      getAttribute(String)
	 * @see nanoxml.XMLElement#getAttribute(java.lang.String,
	 *      java.lang.Object) getAttribute(String, Object)
	 * @see nanoxml.XMLElement#getAttribute(java.lang.String,
	 *      java.util.Hashtable, java.lang.String, boolean)
	 *      getAttribute(String, Hashtable, String, boolean)
	 * @see nanoxml.XMLElement#getStringAttribute(java.lang.String)
	 *      getStringAttribute(String)
	 * @see nanoxml.XMLElement#getStringAttribute(java.lang.String,
	 *      java.lang.String) getStringAttribute(String, String)
	 * @see nanoxml.XMLElement#getStringAttribute(java.lang.String,
	 *      java.util.Hashtable, java.lang.String, boolean)
	 *      getStringAttribute(String, Hashtable, String, boolean)
	 */
	public void setAttribute( String name, String value )
	{
		if( ignoreCase )
		{
			name = name.toLowerCase();
		}

		attributes.put( name, value );
	}

	/**
	 * Returns the number of child elements of the element.
	 * <dl>
	 * <dt><b>Postconditions:</b></dt>
	 * <dd>
	 * <ul>
	 * <li><code>result >= 0</code>
	 * </ul>
	 * </dd>
	 * </dl>
	 * 
	 * @see nanoxml.XMLElement#addChild(nanoxml.XMLElement)
	 *      addChild(XMLElement)
	 * @see nanoxml.XMLElement#enumerateChildren()
	 * @see nanoxml.XMLElement#getChildren()
	 * @see nanoxml.XMLElement#removeChild(nanoxml.XMLElement)
	 *      removeChild(XMLElement)
	 */
	public int countChildren()
	{
		return children.size();
	}

	/**
	 * Enumerates the attribute names.
	 * <dl>
	 * <dt><b>Postconditions:</b></dt>
	 * <dd>
	 * <ul>
	 * <li><code>result != null</code>
	 * </ul>
	 * </dd>
	 * </dl>
	 * 
	 * @see nanoxml.XMLElement#setDoubleAttribute(java.lang.String,
	 *      double) setDoubleAttribute(String, double)
	 * @see nanoxml.XMLElement#setIntAttribute(java.lang.String, int)
	 *      setIntAttribute(String, int)
	 * @see nanoxml.XMLElement#setAttribute(java.lang.String,
	 *      java.lang.Object) setAttribute(String, Object)
	 * @see nanoxml.XMLElement#removeAttribute(java.lang.String)
	 *      removeAttribute(String)
	 * @see nanoxml.XMLElement#getAttribute(java.lang.String)
	 *      getAttribute(String)
	 * @see nanoxml.XMLElement#getAttribute(java.lang.String,
	 *      java.lang.Object) getAttribute(String, String)
	 * @see nanoxml.XMLElement#getAttribute(java.lang.String,
	 *      java.util.Hashtable, java.lang.String, boolean)
	 *      getAttribute(String, Hashtable, String, boolean)
	 * @see nanoxml.XMLElement#getStringAttribute(java.lang.String)
	 *      getStringAttribute(String)
	 * @see nanoxml.XMLElement#getStringAttribute(java.lang.String,
	 *      java.lang.String) getStringAttribute(String, String)
	 * @see nanoxml.XMLElement#getStringAttribute(java.lang.String,
	 *      java.util.Hashtable, java.lang.String, boolean)
	 *      getStringAttribute(String, Hashtable, String, boolean)
	 * @see nanoxml.XMLElement#getIntAttribute(java.lang.String)
	 *      getIntAttribute(String)
	 * @see nanoxml.XMLElement#getIntAttribute(java.lang.String, int)
	 *      getIntAttribute(String, int)
	 * @see nanoxml.XMLElement#getIntAttribute(java.lang.String,
	 *      java.util.Hashtable, java.lang.String, boolean)
	 *      getIntAttribute(String, Hashtable, String, boolean)
	 * @see nanoxml.XMLElement#getDoubleAttribute(java.lang.String)
	 *      getDoubleAttribute(String)
	 * @see nanoxml.XMLElement#getDoubleAttribute(java.lang.String,
	 *      double) getDoubleAttribute(String, double)
	 * @see nanoxml.XMLElement#getDoubleAttribute(java.lang.String,
	 *      java.util.Hashtable, java.lang.String, boolean)
	 *      getDoubleAttribute(String, Hashtable, String, boolean)
	 * @see nanoxml.XMLElement#getBooleanAttribute(java.lang.String,
	 *      java.lang.String, java.lang.String, boolean)
	 *      getBooleanAttribute(String, String, String, boolean)
	 */
	public Iterator<String> iterateAttributeNames()
	{
		return attributes.keySet().iterator();
	}

	/**
	 * Enumerates the child elements.
	 * <dl>
	 * <dt><b>Postconditions:</b></dt>
	 * <dd>
	 * <ul>
	 * <li><code>result != null</code>
	 * </ul>
	 * </dd>
	 * </dl>
	 * 
	 * @see nanoxml.XMLElement#addChild(nanoxml.XMLElement)
	 *      addChild(XMLElement)
	 * @see nanoxml.XMLElement#countChildren()
	 * @see nanoxml.XMLElement#getChildren()
	 * @see nanoxml.XMLElement#removeChild(nanoxml.XMLElement)
	 *      removeChild(XMLElement)
	 */
	public Iterator<XMLElement> iterateChildren()
	{
		return children.iterator();
	}

	/**
	 * Returns the child elements as a Vector. It is safe to modify
	 * this Vector.
	 * <dl>
	 * <dt><b>Postconditions:</b></dt>
	 * <dd>
	 * <ul>
	 * <li><code>result != null</code>
	 * </ul>
	 * </dd>
	 * </dl>
	 * 
	 * @see nanoxml.XMLElement#addChild(nanoxml.XMLElement)
	 *      addChild(XMLElement)
	 * @see nanoxml.XMLElement#countChildren()
	 * @see nanoxml.XMLElement#enumerateChildren()
	 * @see nanoxml.XMLElement#removeChild(nanoxml.XMLElement)
	 *      removeChild(XMLElement)
	 */
	public List<XMLElement> getChildren()
	{
		List<XMLElement> l = new ArrayList<XMLElement>( children.size() );

		l.addAll( children );

		return l;
	}

	/**
	 * Returns the PCDATA content of the object. If there is no such
	 * content, <CODE>null</CODE> is returned.
	 * 
	 * @see nanoxml.XMLElement#setContent(java.lang.String)
	 *      setContent(String)
	 */
	public String getContent()
	{
		return contents;
	}

	/**
	 * Returns the line nr in the source data on which the element is
	 * found. This method returns <code>0</code> there is no associated
	 * source data.
	 * <dl>
	 * <dt><b>Postconditions:</b></dt>
	 * <dd>
	 * <ul>
	 * <li><code>result >= 0</code>
	 * </ul>
	 * </dd>
	 * </dl>
	 */
	public int getLineNr()
	{
		return lineNr;
	}

	/**
	 * Returns an attribute of the element. If the attribute doesn't
	 * exist, <code>null</code> is returned.
	 * 
	 * @param name
	 *           The name of the attribute. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>name != null</code>
	 *           <li><code>name</code> is a valid XML identifier
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 * @see nanoxml.XMLElement#setAttribute(java.lang.String,
	 *      java.lang.Object) setAttribute(String, Object)
	 * @see nanoxml.XMLElement#removeAttribute(java.lang.String)
	 *      removeAttribute(String)
	 * @see nanoxml.XMLElement#enumerateAttributeNames()
	 * @see nanoxml.XMLElement#getAttribute(java.lang.String,
	 *      java.lang.Object) getAttribute(String, Object)
	 * @see nanoxml.XMLElement#getAttribute(java.lang.String,
	 *      java.util.Hashtable, java.lang.String, boolean)
	 *      getAttribute(String, Hashtable, String, boolean)
	 */
	public String getAttribute( String name )
	{
		return this.getAttribute( name, null );
	}

	/**
	 * Returns an attribute of the element. If the attribute doesn't
	 * exist, <code>defaultValue</code> is returned.
	 * 
	 * @param name
	 *           The name of the attribute.
	 * @param defaultValue
	 *           Key to use if the attribute is missing. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>name != null</code>
	 *           <li><code>name</code> is a valid XML identifier
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 * @see nanoxml.XMLElement#setAttribute(java.lang.String,
	 *      java.lang.Object) setAttribute(String, Object)
	 * @see nanoxml.XMLElement#removeAttribute(java.lang.String)
	 *      removeAttribute(String)
	 * @see nanoxml.XMLElement#enumerateAttributeNames()
	 * @see nanoxml.XMLElement#getAttribute(java.lang.String)
	 *      getAttribute(String)
	 * @see nanoxml.XMLElement#getAttribute(java.lang.String,
	 *      java.util.Hashtable, java.lang.String, boolean)
	 *      getAttribute(String, Hashtable, String, boolean)
	 */
	public String getAttribute( String name, String defaultValue )
	{
		if( ignoreCase )
		{
			name = name.toLowerCase();
		}
		String value = attributes.get( name );
		if( value == null )
		{
			value = defaultValue;
		}
		return value;
	}

	/**
	 * Returns the name of the element.
	 * 
	 * @see nanoxml.XMLElement#setName(java.lang.String)
	 *      setName(String)
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Reads one XML element from a java.io.Reader and parses it.
	 * 
	 * @param reader
	 *           The reader from which to retrieve the XML data. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>reader != null</code>
	 *           <li><code>reader</code> is not closed
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 *           <dt><b>Postconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>the state of the receiver is updated to reflect
	 *           the XML element parsed from the reader
	 *           <li>the reader points to the first character following
	 *           the last '&gt;' character of the XML element
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 * @throws java.io.IOException
	 *            If an error occured while reading the input.
	 * @throws nanoxml.XMLParseException
	 *            If an error occured while parsing the read data.
	 */
	public void parseFromReader( Reader reader ) throws IOException, XMLParseException
	{
		this.parseFromReader( reader, /* startingLineNr */1 );
	}

	/**
	 * Reads one XML element from a java.io.Reader and parses it.
	 * 
	 * @param reader
	 *           The reader from which to retrieve the XML data.
	 * @param startingLineNr
	 *           The line number of the first line in the data. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>reader != null</code>
	 *           <li><code>reader</code> is not closed
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 *           <dt><b>Postconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>the state of the receiver is updated to reflect
	 *           the XML element parsed from the reader
	 *           <li>the reader points to the first character following
	 *           the last '&gt;' character of the XML element
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 * @throws java.io.IOException
	 *            If an error occured while reading the input.
	 * @throws nanoxml.XMLParseException
	 *            If an error occured while parsing the read data.
	 */
	public void parseFromReader( Reader reader, int startingLineNr ) throws IOException,
			XMLParseException
	{
		name = null;
		contents = "";
		attributes = new Hashtable<String, String>();
		children = new ArrayList<XMLElement>();
		charReadTooMuch = '\0';
		this.reader = reader;
		parserLineNr = startingLineNr;

		for( ;; )
		{
			char ch = this.scanWhitespace();

			if( ch != '<' )
			{
				throw expectedInput( "<" );
			}

			ch = readChar();

			if( ch == '!' || ch == '?' )
			{
				skipSpecialTag( 0 );
			}
			else
			{
				unreadChar( ch );
				scanElement( this );
				return;
			}
		}
	}

	/**
	 * Reads one XML element from a String and parses it.
	 * 
	 * @param reader
	 *           The reader from which to retrieve the XML data. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>string != null</code>
	 *           <li><code>string.length() &gt; 0</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 *           <dt><b>Postconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>the state of the receiver is updated to reflect
	 *           the XML element parsed from the reader
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 * @throws nanoxml.XMLParseException
	 *            If an error occured while parsing the string.
	 */
	public void parseString( String string ) throws XMLParseException
	{
		try
		{
			this.parseFromReader( new StringReader( string ),
			/* startingLineNr */1 );
		}
		catch( IOException e )
		{
			// Java exception handling suxx
		}
	}

	/**
	 * Reads one XML element from a String and parses it.
	 * 
	 * @param reader
	 *           The reader from which to retrieve the XML data.
	 * @param offset
	 *           The first character in <code>string</code> to scan.
	 *           </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>string != null</code>
	 *           <li><code>offset &lt; string.length()</code>
	 *           <li><code>offset &gt;= 0</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 *           <dt><b>Postconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>the state of the receiver is updated to reflect
	 *           the XML element parsed from the reader
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 * @throws nanoxml.XMLParseException
	 *            If an error occured while parsing the string.
	 */
	public void parseString( String string, int offset ) throws XMLParseException
	{
		this.parseString( string.substring( offset ) );
	}

	/**
	 * Reads one XML element from a String and parses it.
	 * 
	 * @param reader
	 *           The reader from which to retrieve the XML data.
	 * @param offset
	 *           The first character in <code>string</code> to scan.
	 * @param end
	 *           The character where to stop scanning. This character
	 *           is not scanned. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>string != null</code>
	 *           <li><code>end &lt;= string.length()</code>
	 *           <li><code>offset &lt; end</code>
	 *           <li><code>offset &gt;= 0</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 *           <dt><b>Postconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>the state of the receiver is updated to reflect
	 *           the XML element parsed from the reader
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 * @throws nanoxml.XMLParseException
	 *            If an error occured while parsing the string.
	 */
	public void parseString( String string, int offset, int end ) throws XMLParseException
	{
		this.parseString( string.substring( offset, end ) );
	}

	/**
	 * Reads one XML element from a String and parses it.
	 * 
	 * @param reader
	 *           The reader from which to retrieve the XML data.
	 * @param offset
	 *           The first character in <code>string</code> to scan.
	 * @param end
	 *           The character where to stop scanning. This character
	 *           is not scanned.
	 * @param startingLineNr
	 *           The line number of the first line in the data. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>string != null</code>
	 *           <li><code>end &lt;= string.length()</code>
	 *           <li><code>offset &lt; end</code>
	 *           <li><code>offset &gt;= 0</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 *           <dt><b>Postconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>the state of the receiver is updated to reflect
	 *           the XML element parsed from the reader
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 * @throws nanoxml.XMLParseException
	 *            If an error occured while parsing the string.
	 */
	public void parseString( String string, int offset, int end, int startingLineNr )
			throws XMLParseException
	{
		string = string.substring( offset, end );
		try
		{
			this.parseFromReader( new StringReader( string ), startingLineNr );
		}
		catch( IOException e )
		{
			// Java exception handling suxx
		}
	}

	/**
	 * Reads one XML element from a char array and parses it.
	 * 
	 * @param reader
	 *           The reader from which to retrieve the XML data.
	 * @param offset
	 *           The first character in <code>string</code> to scan.
	 * @param end
	 *           The character where to stop scanning. This character
	 *           is not scanned. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>input != null</code>
	 *           <li><code>end &lt;= input.length</code>
	 *           <li><code>offset &lt; end</code>
	 *           <li><code>offset &gt;= 0</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 *           <dt><b>Postconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>the state of the receiver is updated to reflect
	 *           the XML element parsed from the reader
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 * @throws nanoxml.XMLParseException
	 *            If an error occured while parsing the string.
	 */
	public void parseCharArray( char[] input, int offset, int end ) throws XMLParseException
	{
		this.parseCharArray( input, offset, end, /* startingLineNr */1 );
	}

	/**
	 * Reads one XML element from a char array and parses it.
	 * 
	 * @param reader
	 *           The reader from which to retrieve the XML data.
	 * @param offset
	 *           The first character in <code>string</code> to scan.
	 * @param end
	 *           The character where to stop scanning. This character
	 *           is not scanned.
	 * @param startingLineNr
	 *           The line number of the first line in the data. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>input != null</code>
	 *           <li><code>end &lt;= input.length</code>
	 *           <li><code>offset &lt; end</code>
	 *           <li><code>offset &gt;= 0</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 *           <dt><b>Postconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>the state of the receiver is updated to reflect
	 *           the XML element parsed from the reader
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 * @throws nanoxml.XMLParseException
	 *            If an error occured while parsing the string.
	 */
	public void parseCharArray( char[] input, int offset, int end, int startingLineNr )
			throws XMLParseException
	{
		try
		{
			Reader reader = new CharArrayReader( input, offset, end );
			this.parseFromReader( reader, startingLineNr );
		}
		catch( IOException e )
		{
			// This exception will never happen.
		}
	}

	/**
	 * Removes a child element.
	 * 
	 * @param child
	 *           The child element to remove. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>child != null</code>
	 *           <li><code>child</code> is a child element of the
	 *           receiver
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 *           <dt><b>Postconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>countChildren() => old.countChildren() - 1
	 *           <li>enumerateChildren() => old.enumerateChildren() -
	 *           child
	 *           <li>getChildren() => old.enumerateChildren() - child
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 * @see nanoxml.XMLElement#addChild(nanoxml.XMLElement)
	 *      addChild(XMLElement)
	 * @see nanoxml.XMLElement#countChildren()
	 * @see nanoxml.XMLElement#enumerateChildren()
	 * @see nanoxml.XMLElement#getChildren()
	 */
	public void removeChild( XMLElement child )
	{
		children.remove( child );
	}

	/**
	 * Removes an attribute.
	 * 
	 * @param name
	 *           The name of the attribute. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>name != null</code>
	 *           <li><code>name</code> is a valid XML identifier
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 *           <dt><b>Postconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>enumerateAttributeNames() =>
	 *           old.enumerateAttributeNames() - name
	 *           <li>getAttribute(name) => <code>null</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 * @see nanoxml.XMLElement#enumerateAttributeNames()
	 * @see nanoxml.XMLElement#setDoubleAttribute(java.lang.String,
	 *      double) setDoubleAttribute(String, double)
	 * @see nanoxml.XMLElement#setIntAttribute(java.lang.String, int)
	 *      setIntAttribute(String, int)
	 * @see nanoxml.XMLElement#setAttribute(java.lang.String,
	 *      java.lang.Object) setAttribute(String, Object)
	 * @see nanoxml.XMLElement#getAttribute(java.lang.String)
	 *      getAttribute(String)
	 * @see nanoxml.XMLElement#getAttribute(java.lang.String,
	 *      java.lang.Object) getAttribute(String, Object)
	 * @see nanoxml.XMLElement#getAttribute(java.lang.String,
	 *      java.util.Hashtable, java.lang.String, boolean)
	 *      getAttribute(String, Hashtable, String, boolean)
	 * @see nanoxml.XMLElement#getStringAttribute(java.lang.String)
	 *      getStringAttribute(String)
	 * @see nanoxml.XMLElement#getStringAttribute(java.lang.String,
	 *      java.lang.String) getStringAttribute(String, String)
	 * @see nanoxml.XMLElement#getStringAttribute(java.lang.String,
	 *      java.util.Hashtable, java.lang.String, boolean)
	 *      getStringAttribute(String, Hashtable, String, boolean)
	 * @see nanoxml.XMLElement#getIntAttribute(java.lang.String)
	 *      getIntAttribute(String)
	 * @see nanoxml.XMLElement#getIntAttribute(java.lang.String, int)
	 *      getIntAttribute(String, int)
	 * @see nanoxml.XMLElement#getIntAttribute(java.lang.String,
	 *      java.util.Hashtable, java.lang.String, boolean)
	 *      getIntAttribute(String, Hashtable, String, boolean)
	 * @see nanoxml.XMLElement#getDoubleAttribute(java.lang.String)
	 *      getDoubleAttribute(String)
	 * @see nanoxml.XMLElement#getDoubleAttribute(java.lang.String,
	 *      double) getDoubleAttribute(String, double)
	 * @see nanoxml.XMLElement#getDoubleAttribute(java.lang.String,
	 *      java.util.Hashtable, java.lang.String, boolean)
	 *      getDoubleAttribute(String, Hashtable, String, boolean)
	 * @see nanoxml.XMLElement#getBooleanAttribute(java.lang.String,
	 *      java.lang.String, java.lang.String, boolean)
	 *      getBooleanAttribute(String, String, String, boolean)
	 */
	public void removeAttribute( String name )
	{
		if( ignoreCase )
		{
			name = name.toUpperCase();
		}
		attributes.remove( name );
	}

	/**
	 * Creates a new similar XML element.
	 * <P>
	 * You should override this method when subclassing XMLElement.
	 */
	protected XMLElement createAnotherElement()
	{
		return new XMLElement( entities, ignoreWhitespace, false, ignoreCase );
	}

	/**
	 * Changes the content string.
	 * 
	 * @param content
	 *           The new content string.
	 */
	public void setContent( String content )
	{
		contents = content;
	}

	/**
	 * Changes the name of the element.
	 * 
	 * @param name
	 *           The new name. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>name != null</code>
	 *           <li><code>name</code> is a valid XML identifier
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 * @see nanoxml.XMLElement#getName()
	 */
	public void setName( String name )
	{
		this.name = name;
	}

	/**
	 * Writes the XML element to a string.
	 * 
	 * @see nanoxml.XMLElement#write(java.io.Writer) write(Writer)
	 */
	@Override
	public String toString()
	{
		StringWriter sw = new StringWriter();

		try
		{
			write( sw );
			sw.flush();
		}
		catch( IOException e )
		{
			e.printStackTrace();
		}

		return sw.toString();
	}

	public String indentPrint()
	{
		StringBuilder buff = new StringBuilder();

		indentPrint( buff, 0 );

		return buff.toString();
	}

	private void indentPrint( StringBuilder buff, int indent )
	{
		for( int i = 0; i < indent; i++ )
		{
			buff.append( "\t" );
		}

		buff.append( "<" ).append( name );

		for( Map.Entry<String, String> e : attributes.entrySet() )
		{
			buff.append( " " ).append( e.getKey() ).append( "=\"" );
			writeEncoded( buff, e.getValue() );
			buff.append( "\"" );
		}

		buff.append( ">" );

		if( !children.isEmpty() )
		{
			assert contents == "";

			for( XMLElement child : children )
			{
				buff.append( "\n" );
				child.indentPrint( buff, indent + 1 );
			}

			buff.append( "\n" );
			for( int i = 0; i < indent; i++ )
			{
				buff.append( "\t" );
			}
		}
		else if( contents != null )
		{
			buff.append( contents );
		}

		buff.append( "</" ).append( name ).append( ">" );
	}

	/**
	 * Writes the XML element to a writer.
	 * 
	 * @param writer
	 *           The writer to write the XML data to. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>writer != null</code>
	 *           <li><code>writer</code> is not closed
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 * @throws java.io.IOException
	 *            If the data could not be written to the writer.
	 * @see nanoxml.XMLElement#toString()
	 */
	public void write( Writer writer ) throws IOException
	{
		if( name == null )
		{
			writeEncoded( writer, contents );
			return;
		}
		writer.write( '<' );
		writer.write( name );
		if( !attributes.isEmpty() )
		{
			Iterator<String> iter = attributes.keySet().iterator();
			while( iter.hasNext() )
			{
				writer.write( ' ' );
				String key = iter.next();
				String value = attributes.get( key );
				writer.write( key );
				writer.write( '=' );
				writer.write( '"' );
				writeEncoded( writer, value );
				writer.write( '"' );
			}
		}
		if( contents != null && contents.length() > 0 )
		{
			writer.write( '>' );
			writeEncoded( writer, contents );
			writer.write( '<' );
			writer.write( '/' );
			writer.write( name );
			writer.write( '>' );
		}
		else if( children.isEmpty() )
		{
			writer.write( '/' );
			writer.write( '>' );
		}
		else
		{
			writer.write( '>' );
			Iterator<XMLElement> iter = iterateChildren();

			while( iter.hasNext() )
			{
				XMLElement child = iter.next();
				child.write( writer );
			}
			writer.write( '<' );
			writer.write( '/' );
			writer.write( name );
			writer.write( '>' );
		}
	}

	protected void writeEncoded( StringBuilder buff, String str )
	{
		if( str != null )
		{
			for( int i = 0; i < str.length(); i += 1 )
			{
				char ch = str.charAt( i );
				switch( ch )
				{
					case '<':
						buff.append( '&' );
						buff.append( 'l' );
						buff.append( 't' );
						buff.append( ';' );
						break;
					case '>':
						buff.append( '&' );
						buff.append( 'g' );
						buff.append( 't' );
						buff.append( ';' );
						break;
					case '&':
						buff.append( '&' );
						buff.append( 'a' );
						buff.append( 'm' );
						buff.append( 'p' );
						buff.append( ';' );
						break;
					case '"':
						buff.append( '&' );
						buff.append( 'q' );
						buff.append( 'u' );
						buff.append( 'o' );
						buff.append( 't' );
						buff.append( ';' );
						break;
					case '\'':
						buff.append( '&' );
						buff.append( 'a' );
						buff.append( 'p' );
						buff.append( 'o' );
						buff.append( 's' );
						buff.append( ';' );
						break;
					default:
						int unicode = ch;
						if( unicode < 32 || unicode > 126 )
						{
							buff.append( '&' );
							buff.append( '#' );
							buff.append( 'x' );
							buff.append( Integer.toString( unicode, 16 ) );
							buff.append( ';' );
						}
						else
						{
							buff.append( ch );
						}
				}
			}
		}
	}

	/**
	 * Writes a string encoded to a writer.
	 * 
	 * @param writer
	 *           The writer to write the XML data to.
	 * @param str
	 *           The string to write encoded. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>writer != null</code>
	 *           <li><code>writer</code> is not closed
	 *           <li><code>str != null</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 */
	protected void writeEncoded( Writer writer, String str ) throws IOException
	{
		if( str != null )
		{
			for( int i = 0; i < str.length(); i += 1 )
			{
				char ch = str.charAt( i );
				switch( ch )
				{
					case '<':
						writer.write( '&' );
						writer.write( 'l' );
						writer.write( 't' );
						writer.write( ';' );
						break;
					case '>':
						writer.write( '&' );
						writer.write( 'g' );
						writer.write( 't' );
						writer.write( ';' );
						break;
					case '&':
						writer.write( '&' );
						writer.write( 'a' );
						writer.write( 'm' );
						writer.write( 'p' );
						writer.write( ';' );
						break;
					case '"':
						writer.write( '&' );
						writer.write( 'q' );
						writer.write( 'u' );
						writer.write( 'o' );
						writer.write( 't' );
						writer.write( ';' );
						break;
					case '\'':
						writer.write( '&' );
						writer.write( 'a' );
						writer.write( 'p' );
						writer.write( 'o' );
						writer.write( 's' );
						writer.write( ';' );
						break;
					default:
						int unicode = ch;
						if( unicode < 32 || unicode > 126 )
						{
							writer.write( '&' );
							writer.write( '#' );
							writer.write( 'x' );
							writer.write( Integer.toString( unicode, 16 ) );
							writer.write( ';' );
						}
						else
						{
							writer.write( ch );
						}
				}
			}
		}
	}

	/**
	 * Scans an identifier from the current reader. The scanned
	 * identifier is appended to <code>result</code>.
	 * 
	 * @param result
	 *           The buffer in which the scanned identifier will be
	 *           put. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>result != null</code>
	 *           <li>The next character read from the reader is a valid
	 *           first character of an XML identifier.
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 *           <dt><b>Postconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>The next character read from the reader won't be
	 *           an identifier character.
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 *           <dl>
	 */
	protected void scanIdentifier( StringBuffer result ) throws IOException
	{
		for( ;; )
		{
			char ch = readChar();
			if( ( ch < 'A' || ch > 'Z' ) && ( ch < 'a' || ch > 'z' ) && ( ch < '0' || ch > '9' )
					&& ch != '_' && ch != '.' && ch != ':' && ch != '-' && ch <= '\u007E' )
			{
				unreadChar( ch );
				return;
			}
			result.append( ch );
		}
	}

	/**
	 * This method scans an identifier from the current reader.
	 * 
	 * @return the next character following the whitespace.
	 */
	protected char scanWhitespace() throws IOException
	{
		for( ;; )
		{
			char ch = readChar();
			switch( ch )
			{
				case ' ':
				case '\t':
				case '\n':
				case '\r':
					break;
				default:
					return ch;
			}
		}
	}

	/**
	 * This method scans an identifier from the current reader. The
	 * scanned whitespace is appended to <code>result</code>.
	 * 
	 * @return the next character following the whitespace. </dl>
	 *         <dl>
	 *         <dt><b>Preconditions:</b></dt>
	 *         <dd>
	 *         <ul>
	 *         <li><code>result != null</code>
	 *         </ul>
	 *         </dd>
	 *         </dl>
	 */
	protected char scanWhitespace( StringBuffer result ) throws IOException
	{
		for( ;; )
		{
			char ch = readChar();
			switch( ch )
			{
				case ' ':
				case '\t':
				case '\n':
					result.append( ch );
				case '\r':
					break;
				default:
					return ch;
			}
		}
	}

	/**
	 * This method scans a delimited string from the current reader.
	 * The scanned string without delimiters is appended to
	 * <code>string</code>. </dl>
	 * <dl>
	 * <dt><b>Preconditions:</b></dt>
	 * <dd>
	 * <ul>
	 * <li><code>string != null</code>
	 * <li>the next char read is the string delimiter
	 * </ul>
	 * </dd>
	 * </dl>
	 */
	protected void scanString( StringBuffer string ) throws IOException
	{
		char delimiter = readChar();
		if( delimiter != '\'' && delimiter != '"' )
		{
			throw expectedInput( "' or \"" );
		}
		for( ;; )
		{
			char ch = readChar();
			if( ch == delimiter )
			{
				return;
			}
			else if( ch == '&' )
			{
				resolveEntity( string );
			}
			else
			{
				string.append( ch );
			}
		}
	}

	/**
	 * Scans a #PCDATA element. CDATA sections and entities are
	 * resolved. The next &lt; char is skipped. The scanned data is
	 * appended to <code>data</code>. </dl>
	 * <dl>
	 * <dt><b>Preconditions:</b></dt>
	 * <dd>
	 * <ul>
	 * <li><code>data != null</code>
	 * </ul>
	 * </dd>
	 * </dl>
	 */
	protected void scanPCData( StringBuffer data ) throws IOException
	{
		for( ;; )
		{
			char ch = readChar();
			if( ch == '<' )
			{
				ch = readChar();
				if( ch == '!' )
				{
					checkCDATA( data );
				}
				else
				{
					unreadChar( ch );
					return;
				}
			}
			else if( ch == '&' )
			{
				resolveEntity( data );
			}
			else
			{
				data.append( ch );
			}
		}
	}

	/**
	 * Scans a special tag and if the tag is a CDATA section, append
	 * its content to <code>buf</code>. </dl>
	 * <dl>
	 * <dt><b>Preconditions:</b></dt>
	 * <dd>
	 * <ul>
	 * <li><code>buf != null</code>
	 * <li>The first &lt; has already been read.
	 * </ul>
	 * </dd>
	 * </dl>
	 */
	protected boolean checkCDATA( StringBuffer buf ) throws IOException
	{
		char ch = readChar();
		if( ch != '[' )
		{
			unreadChar( ch );
			skipSpecialTag( 0 );
			return false;
		}
		else if( !checkLiteral( "CDATA[" ) )
		{
			skipSpecialTag( 1 ); // one [ has already been read
			return false;
		}
		else
		{
			int delimiterCharsSkipped = 0;
			while( delimiterCharsSkipped < 3 )
			{
				ch = readChar();
				switch( ch )
				{
					case ']':
						if( delimiterCharsSkipped < 2 )
						{
							delimiterCharsSkipped += 1;
						}
						else
						{
							buf.append( ']' );
							buf.append( ']' );
							delimiterCharsSkipped = 0;
						}
						break;
					case '>':
						if( delimiterCharsSkipped < 2 )
						{
							for( int i = 0; i < delimiterCharsSkipped; i++ )
							{
								buf.append( ']' );
							}
							delimiterCharsSkipped = 0;
							buf.append( '>' );
						}
						else
						{
							delimiterCharsSkipped = 3;
						}
						break;
					default:
						for( int i = 0; i < delimiterCharsSkipped; i += 1 )
						{
							buf.append( ']' );
						}
						buf.append( ch );
						delimiterCharsSkipped = 0;
				}
			}
			return true;
		}
	}

	/**
	 * Skips a comment. </dl>
	 * <dl>
	 * <dt><b>Preconditions:</b></dt>
	 * <dd>
	 * <ul>
	 * <li>The first &lt;!-- has already been read.
	 * </ul>
	 * </dd>
	 * </dl>
	 */
	protected void skipComment() throws IOException
	{
		int dashesToRead = 2;
		while( dashesToRead > 0 )
		{
			char ch = readChar();
			if( ch == '-' )
			{
				dashesToRead -= 1;
			}
			else
			{
				dashesToRead = 2;
			}
		}
		if( readChar() != '>' )
		{
			throw expectedInput( ">" );
		}
	}

	/**
	 * Skips a special tag or comment.
	 * 
	 * @param bracketLevel
	 *           The number of open square brackets ([) that have
	 *           already been read. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>The first &lt;! has already been read.
	 *           <li><code>bracketLevel >= 0</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 */
	protected void skipSpecialTag( int bracketLevel ) throws IOException
	{
		int tagLevel = 1; // <
		char stringDelimiter = '\0';
		if( bracketLevel == 0 )
		{
			char ch = readChar();
			if( ch == '[' )
			{
				bracketLevel += 1;
			}
			else if( ch == '-' )
			{
				ch = readChar();
				if( ch == '[' )
				{
					bracketLevel += 1;
				}
				else if( ch == ']' )
				{
					bracketLevel -= 1;
				}
				else if( ch == '-' )
				{
					skipComment();
					return;
				}
			}
		}
		while( tagLevel > 0 )
		{
			char ch = readChar();
			if( stringDelimiter == '\0' )
			{
				if( ch == '"' || ch == '\'' )
				{
					stringDelimiter = ch;
				}
				else if( bracketLevel <= 0 )
				{
					if( ch == '<' )
					{
						tagLevel += 1;
					}
					else if( ch == '>' )
					{
						tagLevel -= 1;
					}
				}
				if( ch == '[' )
				{
					bracketLevel += 1;
				}
				else if( ch == ']' )
				{
					bracketLevel -= 1;
				}
			}
			else
			{
				if( ch == stringDelimiter )
				{
					stringDelimiter = '\0';
				}
			}
		}
	}

	/**
	 * Scans the data for literal text. Scanning stops when a character
	 * does not match or after the complete text has been checked,
	 * whichever comes first.
	 * 
	 * @param literal
	 *           the literal to check. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>literal != null</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 */
	protected boolean checkLiteral( String literal ) throws IOException
	{
		int length = literal.length();
		for( int i = 0; i < length; i += 1 )
		{
			if( readChar() != literal.charAt( i ) )
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Reads a character from a reader.
	 */
	protected char readChar() throws IOException
	{
		if( charReadTooMuch != '\0' )
		{
			char ch = charReadTooMuch;
			charReadTooMuch = '\0';
			return ch;
		}
		else
		{
			int i = reader.read();
			if( i < 0 )
			{
				throw unexpectedEndOfData();
			}
			else if( i == 10 )
			{
				parserLineNr += 1;
				return '\n';
			}
			else
			{
				return ( char ) i;
			}
		}
	}

	/**
	 * Scans an XML element.
	 * 
	 * @param elt
	 *           The element that will contain the result. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>The first &lt; has already been read.
	 *           <li><code>elt != null</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 */
	protected void scanElement( XMLElement elt ) throws IOException
	{
		StringBuffer buf = new StringBuffer();
		scanIdentifier( buf );
		String name = buf.toString();
		elt.setName( name );
		char ch = this.scanWhitespace();
		while( ch != '>' && ch != '/' )
		{
			buf.setLength( 0 );
			unreadChar( ch );
			scanIdentifier( buf );
			String key = buf.toString();
			ch = this.scanWhitespace();
			if( ch != '=' )
			{
				throw expectedInput( "=" );
			}
			unreadChar( this.scanWhitespace() );
			buf.setLength( 0 );
			scanString( buf );
			elt.setAttribute( key, buf.toString() );
			ch = this.scanWhitespace();
		}
		if( ch == '/' )
		{
			ch = readChar();
			if( ch != '>' )
			{
				throw expectedInput( ">" );
			}
			return;
		}
		buf.setLength( 0 );
		ch = this.scanWhitespace( buf );
		if( ch != '<' )
		{
			unreadChar( ch );
			scanPCData( buf );
		}
		else
		{
			for( ;; )
			{
				ch = readChar();
				if( ch == '!' )
				{
					if( checkCDATA( buf ) )
					{
						scanPCData( buf );
						break;
					}
					else
					{
						ch = this.scanWhitespace( buf );
						if( ch != '<' )
						{
							unreadChar( ch );
							scanPCData( buf );
							break;
						}
					}
				}
				else
				{
					if( ch != '/' || ignoreWhitespace )
					{
						buf.setLength( 0 );
					}
					if( ch == '/' )
					{
						unreadChar( ch );
					}
					break;
				}
			}
		}
		if( buf.length() == 0 )
		{
			while( ch != '/' )
			{
				if( ch == '!' )
				{
					ch = readChar();
					if( ch != '-' )
					{
						throw expectedInput( "Comment or Element" );
					}
					ch = readChar();
					if( ch != '-' )
					{
						throw expectedInput( "Comment or Element" );
					}
					skipComment();
				}
				else
				{
					unreadChar( ch );
					XMLElement child = createAnotherElement();
					scanElement( child );
					elt.addChild( child );
				}
				ch = this.scanWhitespace();
				if( ch != '<' )
				{
					throw expectedInput( "<" );
				}
				ch = readChar();
			}
			unreadChar( ch );
		}
		else
		{
			if( ignoreWhitespace )
			{
				elt.setContent( buf.toString().trim() );
			}
			else
			{
				elt.setContent( buf.toString() );
			}
		}
		ch = readChar();
		if( ch != '/' )
		{
			throw expectedInput( "/" );
		}
		unreadChar( this.scanWhitespace() );
		if( !checkLiteral( name ) )
		{
			throw expectedInput( name );
		}
		if( this.scanWhitespace() != '>' )
		{
			throw expectedInput( ">" );
		}
	}

	/**
	 * Resolves an entity. The name of the entity is read from the
	 * reader. The value of the entity is appended to <code>buf</code>.
	 * 
	 * @param buf
	 *           Where to put the entity value. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>The first &amp; has already been read.
	 *           <li><code>buf != null</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 */
	protected void resolveEntity( StringBuffer buf ) throws IOException
	{
		char ch = '\0';
		StringBuffer keyBuf = new StringBuffer();
		for( ;; )
		{
			ch = readChar();
			if( ch == ';' )
			{
				break;
			}
			keyBuf.append( ch );
		}
		String key = keyBuf.toString();
		if( key.charAt( 0 ) == '#' )
		{
			try
			{
				if( key.charAt( 1 ) == 'x' )
				{
					ch = ( char ) Integer.parseInt( key.substring( 2 ), 16 );
				}
				else
				{
					ch = ( char ) Integer.parseInt( key.substring( 1 ), 10 );
				}
			}
			catch( NumberFormatException e )
			{
				throw unknownEntity( key );
			}
			buf.append( ch );
		}
		else
		{
			char[] value = entities.get( key );
			if( value == null )
			{
				throw unknownEntity( key );
			}
			buf.append( value );
		}
	}

	/**
	 * Pushes a character back to the read-back buffer.
	 * 
	 * @param ch
	 *           The character to push back. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li>The read-back buffer is empty.
	 *           <li><code>ch != '\0'</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 */
	protected void unreadChar( char ch )
	{
		charReadTooMuch = ch;
	}

	/**
	 * Creates a parse exception for when an invalid valueset is given
	 * to a method.
	 * 
	 * @param name
	 *           The name of the entity. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>name != null</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 */
	protected XMLParseException invalidValueSet( String name )
	{
		String msg = "Invalid value set (entity name = \"" + name + "\")";
		return new XMLParseException( getName(), parserLineNr, msg );
	}

	/**
	 * Creates a parse exception for when an invalid value is given to
	 * a method.
	 * 
	 * @param name
	 *           The name of the entity.
	 * @param value
	 *           The value of the entity. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>name != null</code>
	 *           <li><code>value != null</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 */
	protected XMLParseException invalidValue( String name, String value )
	{
		String msg =
				"Attribute \"" + name + "\" does not contain a valid " + "value (\"" + value + "\")";
		return new XMLParseException( getName(), parserLineNr, msg );
	}

	/**
	 * Creates a parse exception for when the end of the data input has
	 * been reached.
	 */
	protected XMLParseException unexpectedEndOfData()
	{
		String msg = "Unexpected end of data reached";
		return new XMLParseException( getName(), parserLineNr, msg );
	}

	/**
	 * Creates a parse exception for when a syntax error occured.
	 * 
	 * @param context
	 *           The context in which the error occured. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>context != null</code>
	 *           <li><code>context.length() &gt; 0</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 */
	protected XMLParseException syntaxError( String context )
	{
		String msg = "Syntax error while parsing " + context;
		return new XMLParseException( getName(), parserLineNr, msg );
	}

	/**
	 * Creates a parse exception for when the next character read is
	 * not the character that was expected.
	 * 
	 * @param charSet
	 *           The set of characters (in human readable form) that
	 *           was expected. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>charSet != null</code>
	 *           <li><code>charSet.length() &gt; 0</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 */
	protected XMLParseException expectedInput( String charSet )
	{
		String msg = "Expected: " + charSet;
		return new XMLParseException( getName(), parserLineNr, msg );
	}

	/**
	 * Creates a parse exception for when an entity could not be
	 * resolved.
	 * 
	 * @param name
	 *           The name of the entity. </dl>
	 *           <dl>
	 *           <dt><b>Preconditions:</b></dt>
	 *           <dd>
	 *           <ul>
	 *           <li><code>name != null</code>
	 *           <li><code>name.length() &gt; 0</code>
	 *           </ul>
	 *           </dd>
	 *           </dl>
	 */
	protected XMLParseException unknownEntity( String name )
	{
		String msg = "Unknown or invalid entity: &" + name + ";";
		return new XMLParseException( getName(), parserLineNr, msg );
	}

}
