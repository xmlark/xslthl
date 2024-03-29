/*
 * xslthl - XSLT Syntax Highlighting 
 * https://sourceforge.net/projects/xslthl/
 * Copyright (C) 2005-2008 Michal Molhanec, Jirka Kosek, Michiel Hendriks
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty.  In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 * 1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 * 
 * Michal Molhanec <mol1111 at users.sourceforge.net>
 * Jirka Kosek <kosek at users.sourceforge.net>
 * Michiel Hendriks <elmuerte at users.sourceforge.net>
 */
package net.sf.xslthl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.saxon.event.Builder;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.om.SequenceIterator;
import net.sf.saxon.type.AnyType;
import net.sf.saxon.type.Type;

/**
 * A new saxon connector to be used with saxon 8.5 and later. This class uses
 * Java reflection to call Saxon classes in order to be able to compile both
 * connectors with the older Saxon library.
 */
public class ConnectorSaxonEE {

	/**
	 * The logging facility
	 */
	private static Logger logger = Logger
	        .getLogger("net.sf.xslthl.saxon9eeconnector");

	private static void blockToSaxon9Node(Block b, Builder builder,
	        Config config) throws Exception {
		if (b.isStyled()) {
			// int elemId = pool.allocate(config.prefix, config.uri,
			// ((StyledBlock) b).getStyle());

			// new FingerprintedQName(config.prefix, config.uri, ((StyledBlock)
			// b).getStyle())
			Object fpQname = null;
			try {
				Class fpQnameClazz = Class
				        .forName("net.sf.saxon.om.FingerprintedQName");
				Constructor constructor = fpQnameClazz
				        .getConstructor(new Class[] { String.class,
				                String.class, String.class });
				fpQname = constructor.newInstance(new Object[] { config.prefix,
				        config.uri, ((StyledBlock) b).getStyle() });
			} catch (Exception ex) {
				// Saxon 12.x and newer
				Class fpQnameClazz = Class
				        .forName("net.sf.saxon.om.FingerprintedQName");
				Class nsURIClass = Class
				        .forName("net.sf.saxon.om.NamespaceUri");
				Method of = nsURIClass.getMethod("of",
				        new Class[] { String.class });
				Constructor constructor = fpQnameClazz.getConstructor(
				        new Class[] { String.class, nsURIClass, String.class });
				fpQname = constructor.newInstance(new Object[] { config.prefix,
				        of.invoke(null, config.uri),
				        ((StyledBlock) b).getStyle() });
			}
			startElement(builder, fpQname);
			outputCharacters(builder, b.getText());
			builder.endElement();
		} else {
			outputCharacters(builder, b.getText());
		}
	}

	private static void startElement(Builder builder, Object fpQname)
	        throws Exception {
		try {
			// builder.startElement(fpQname, AnyType.getInstance(), 0, 0);
			Method startElement = builder.getClass().getMethod("startElement",
			        new Class[] { Class.forName("net.sf.saxon.om.NodeName"),
			                net.sf.saxon.type.SchemaType.class, int.class,
			                int.class });
			startElement.invoke(builder,
			        new Object[] { fpQname, AnyType.getInstance(), 0, 0 });
		} catch (Exception ex) {
			try {
				// Maybe Saxon 9.7.11 or newer
				// public void startElement(/*@NotNull*/ NodeName elemName,
				// SchemaType type, Location location, int properties) throws
				// XPathException {
				Method startElement = builder.getClass().getMethod(
				        "startElement",
				        new Class[] { Class.forName("net.sf.saxon.om.NodeName"),
				                net.sf.saxon.type.SchemaType.class,
				                Class.forName(
				                        "net.sf.saxon.expr.parser.Location"),
				                int.class });
				startElement.invoke(builder, new Object[] { fpQname,
				        AnyType.getInstance(), createFakeLocation(false), 0 });
			} catch (Exception ex2) {
				// Maybe Saxon 10 or newer
				Method startElement = builder.getClass().getMethod(
				        "startElement",
				        new Class[] { Class.forName("net.sf.saxon.om.NodeName"),
				                net.sf.saxon.type.SchemaType.class,
				                Class.forName("net.sf.saxon.om.AttributeMap"),
				                Class.forName("net.sf.saxon.om.NamespaceMap"),
				                Class.forName("net.sf.saxon.s9api.Location"),
				                int.class });
				// net.sf.saxon.om.EmptyAttributeMap.EmptyAttributeMap()
				Class emptyAttrClazz = Class
				        .forName("net.sf.saxon.om.EmptyAttributeMap");
				Method attrsInstance = emptyAttrClazz.getMethod("getInstance",
				        new Class[0]);

				Class emptyNMClazz = Class
				        .forName("net.sf.saxon.om.NamespaceMap");
				Method mapInstance = emptyNMClazz.getMethod("emptyMap",
				        new Class[0]);

				startElement.invoke(builder,
				        new Object[] { fpQname, AnyType.getInstance(),
				                attrsInstance.invoke(null, new Object[0]),
				                mapInstance.invoke(null, new Object[0]),
				                createFakeLocation(true), 0 });
			}
		}
	}

	private static void outputCharacters(Builder builder, String text)
	        throws Exception {
		Method characters = null;
		try {
			characters = builder.getClass().getMethod("characters",
			        new Class[] { String.class, int.class, int.class });
			characters.invoke(builder, new Object[] { text, 0, 0 });
		} catch (Exception ex) {
			try {
				// Maybe Saxon 9.7.11 or newer
				characters = builder.getClass()
				        .getMethod("characters",
				                new Class[] { CharSequence.class, Class.forName(
				                        "net.sf.saxon.expr.parser.Location"),
				                        int.class });
				characters.invoke(builder,
				        new Object[] { text, createFakeLocation(false), 0 });
			} catch (Exception ex2) {
				try {
					// Maybe Saxon 10.x and newer
					characters = builder.getClass().getMethod("characters",
					        new Class[] { CharSequence.class,
					                Class.forName(
					                        "net.sf.saxon.s9api.Location"),
					                int.class });
					characters.invoke(builder,
					        new Object[] { text, createFakeLocation(true), 0 });
				} catch (Exception ex3) {
					// Maybe Saxon 12.x and newer
					Class stClass = Class
					        .forName("net.sf.saxon.str.StringTool");
					Method fromCS = stClass.getMethod("fromCharSequence",
					        new Class[] { CharSequence.class });
					Object unicodeObj = fromCS.invoke(null,
					        new Object[] { text });
					Class unicodeStrClass = Class
					        .forName("net.sf.saxon.str.UnicodeString");
					characters = builder.getClass().getMethod("characters",
					        new Class[] { unicodeStrClass,
					                Class.forName(
					                        "net.sf.saxon.s9api.Location"),
					                int.class });
					characters.invoke(builder, new Object[] { unicodeObj,
					        createFakeLocation(true), 0 });
				}
			}
		}
	}

	private static Object createFakeLocation(boolean saxon10OrNewer)
	        throws IllegalArgumentException, ClassNotFoundException {
		return Proxy
		        .newProxyInstance(ConnectorSaxonEE.class.getClassLoader(),
		                new Class[] { Class.forName(saxon10OrNewer
		                        ? "net.sf.saxon.s9api.Location"
		                        : "net.sf.saxon.expr.parser.Location") },
		                new InvocationHandler() {
			                @Override
			                public Object invoke(Object proxy, Method method,
			                        Object[] args) throws Throwable {
				                if ("saveLocation".equals(method.getName())) {
					                return proxy;
				                } else if ("getSystemId"
				                        .equals(method.getName())
				                        || "getPublicId"
				                                .equals(method.getName())) {
					                return null;
				                } else if ("getLineNumber"
				                        .equals(method.getName())
				                        || "getColumnNumber"
				                                .equals(method.getName())) {
					                return Integer.valueOf(0);
				                }
				                return null;
			                }
		                });
	}

	/**
	 * Highlight the nodes using the standard configuration file
	 * 
	 * @param context
	 * @param hlCode
	 * @param nodes
	 * @return
	 * @throws Exception
	 */
	public static SequenceIterator highlight(XPathContext context,
	        String hlCode, SequenceIterator nodes) throws Exception {
		return highlight(context, hlCode, nodes, null);
	}

	/**
	 * highlight the nodes using a specific interface
	 * 
	 * @param context
	 * @param hlCode
	 * @param seq
	 * @param configFilename
	 * @return
	 * @throws Exception
	 */
	public static SequenceIterator highlight(XPathContext context,
	        String hlCode, SequenceIterator seq, String configFilename)
	        throws Exception {
		try {
			Config c = Config.getInstance(configFilename);
			MainHighlighter hl = c.getMainHighlighter(hlCode);

			// Axis info obtained via Java reflection.
			Object childType = Class.forName("net.sf.saxon.om.AxisInfo")
			        .getField("CHILD").get(null);
			Method iterateAxis = null;
			try {
				iterateAxis = Class.forName("net.sf.saxon.om.NodeInfo")
				        .getMethod("iterateAxis", new Class[] { byte.class });
			} catch (NoSuchMethodException t) {
				// Try with Saxon 10
				iterateAxis = Class.forName("net.sf.saxon.om.NodeInfo")
				        .getMethod("iterateAxis", new Class[] { int.class });
			}
			Class axisIterClazz = Class
			        .forName("net.sf.saxon.tree.iter.AxisIterator");
			Method next = axisIterClazz.getMethod("next", new Class[0]);

			List<Item> resultNodes = new ArrayList<Item>();
			Item itm = null;
			while ((itm = seq.next()) != null) {
				// Item itm = seq.current();
				if (itm instanceof NodeInfo) {
					NodeInfo ni = (NodeInfo) itm;
					SequenceIterator ae = (SequenceIterator) iterateAxis
					        .invoke(ni, new Object[] { childType });
					// SequenceIterator ae = ni.iterateAxis(childType,
					// net.sf.saxon.pattern.AnyNodeTest.getInstance());
					Item itm2 = null;
					while ((itm2 = ae.next()) != null) {
						if (itm2 instanceof NodeInfo) {
							NodeInfo n2i = (NodeInfo) itm2;
							if (n2i.getNodeKind() == Type.TEXT) {
								if (hl != null) {
									try {
										Builder builder = context
										        .getController().makeBuilder();
										builder.open();
										builder.startDocument(0);
										List<Block> l = hl.highlight(
										        n2i.getStringValue());
										for (Block b : l) {
											blockToSaxon9Node(b, builder, c);
										}
										builder.endDocument();
										builder.close();
										NodeInfo doc = builder.getCurrentRoot();

										Object elms = iterateAxis.invoke(doc,
										        new Object[] { childType });
										// Object elms =
										// doc.iterateAxis(childType,net.sf.saxon.pattern.AnyNodeTest);
										Item crt = null;
										while ((crt = (Item) next.invoke(elms,
										        new Object[0])) != null) {
											resultNodes.add(crt);
										}
									} catch (Exception ex) {
										logger.log(Level.SEVERE, String.format(
										        "Highligher threw unhandled error at position %s: %s",
										        n2i.getStringValue(),
										        ex.getMessage()), ex);
										resultNodes.add(n2i); // No
										                      // highlighting,
										                      // but visible at
										                      // least
									}
								} else {
									resultNodes.add(n2i);
								}
							} else {
								resultNodes.add(n2i);
							}
						} else {
							resultNodes.add(itm2);
						}
					}
				} else {
					resultNodes.add(itm);
				}
			}
			try {
				Class lstIterClassName = Class
				        .forName("net.sf.saxon.tree.iter.ListIterator");
				Constructor constructor = lstIterClassName
				        .getConstructor(new Class[] { List.class });
				return (SequenceIterator) constructor
				        .newInstance(new Object[] { resultNodes });
			} catch (Exception ex) {
				// Saxon 12.x and newer
				// ListIterator it = new ListIterator.Of<>(nodes);
				Class ofClass = Class
				        .forName("net.sf.saxon.tree.iter.ListIterator$Of");
				Constructor ofConstructor = ofClass
				        .getConstructor(new Class[] { List.class });
				return (SequenceIterator) ofConstructor
				        .newInstance(new Object[] { resultNodes });
			}
		} catch (Throwable e) {
			e.printStackTrace();
			return null;
		}
	}
}
