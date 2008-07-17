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
package net.sf.xslthl.highlighters;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import net.sf.xslthl.Block;
import net.sf.xslthl.CharIter;
import net.sf.xslthl.HighlighterConfigurationException;
import net.sf.xslthl.Params;
import net.sf.xslthl.WholeHighlighter;
import net.sf.xslthl.highlighters.xml.ElementPrefix;
import net.sf.xslthl.highlighters.xml.ElementSet;
import net.sf.xslthl.highlighters.xml.RealElementSet;

/**
 * XML/SGML highlighter. It has a couple of default styles: tag, attribute,
 * value, directive. Accepted parameters:
 * <dl>
 * <dt>elementSet</dt>
 * <dd>Specialized highlighting for set elements</dd>
 * <dt>elementPrefix</dt>
 * <dd>Specialized highlighting for element prefixes</dd>
 * </dl>
 */
public class XMLHighlighter extends WholeHighlighter {

    /**
     * Style to use for elements
     */
    final static String STYLE_ELEMENT = "tag";
    /**
     * The style for attributes
     */
    final static String STYLE_ATTRIBUTE = "attribute";
    /**
     * The style for attribute values
     */
    final static String STYLE_VALUE = "value";
    /**
     * The style for processing instructions
     */
    final static String STYLE_PI = "directive";
    /**
     * The style for comments
     */
    final static String STYLE_COMMENT = "comment";

    final static Character APOSTROPHE = '\'';
    final static Character EQUALS = '=';
    final static Character EXCLAMATION_MARK = '!';
    final static Character GREATER_THAN = '>';
    final static Character HYPHEN = '-';
    final static Character LESS_THAN = '<';
    final static Character QUESTION_MARK = '?';
    final static Character QUOTE = '"';
    final static Character SLASH = '/';

    /**
     * Overriden styles
     */
    protected Collection<ElementSet> elementSets = new HashSet<ElementSet>();

    /**
     * @param tagName
     * @return
     */
    protected String getStyleForTagName(String tagName) {
	for (ElementSet es : elementSets) {
	    if (es.matches(tagName)) {
		return es.getStyle();
	    }
	}
	return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.xslthl.WholeHighlighter#init(net.sf.xslthl.Params)
     */
    @Override
    public void init(Params params) throws HighlighterConfigurationException {
	super.init(params);
	if (params != null) {
	    params.getMultiParams("elementSet", elementSets,
		    new Params.ParamsLoader<RealElementSet>() {
			public RealElementSet load(Params params)
				throws HighlighterConfigurationException {
			    return new RealElementSet(params);
			}
		    });
	    params.getMultiParams("elementPrefix", elementSets,
		    new Params.ParamsLoader<ElementPrefix>() {
			public ElementPrefix load(Params params)
				throws HighlighterConfigurationException {
			    return new ElementPrefix(params);
			}
		    });
	}
    }

    /**
     * @param in
     * @param out
     */
    void readTagContent(CharIter in, List<Block> out) {
	while (!in.finished()
		&& !XMLHighlighter.GREATER_THAN.equals(in.current())
		&& !XMLHighlighter.SLASH.equals(in.current())) {
	    if (!Character.isWhitespace(in.current())) {
		if (in.isMarked()) {
		    out.add(in.markedToBlock());
		}
		while (!in.finished()
			&& !XMLHighlighter.EQUALS.equals(in.current())
			&& !Character.isWhitespace(in.current())) {
		    in.moveNext();
		}
		out.add(in.markedToStyledBlock(STYLE_ATTRIBUTE));
		while (!in.finished() && Character.isWhitespace(in.current())) {
		    in.moveNext();
		}
		if (in.finished()
			|| !XMLHighlighter.EQUALS.equals(in.current())) { // HTML
		    // no-value
		    // attributes
		    continue;
		}
		in.moveNext(); // skip '='
		while (!in.finished() && Character.isWhitespace(in.current())) {
		    in.moveNext();
		}
		out.add(in.markedToBlock());
		if (XMLHighlighter.QUOTE.equals(in.current())
			|| XMLHighlighter.APOSTROPHE.equals(in.current())) {
		    Character boundary = in.current();
		    in.moveNext();
		    while (!in.finished() && !boundary.equals(in.current())) {
			in.moveNext();
		    }
		    if (!in.finished()) {
			in.moveNext();
		    }
		    out.add(in.markedToStyledBlock(STYLE_VALUE));
		} else {
		    while (!in.finished()
			    && !XMLHighlighter.GREATER_THAN
				    .equals(in.current())
			    && !XMLHighlighter.SLASH.equals(in.current())
			    && !Character.isWhitespace(in.current())) {
			in.moveNext();
		    }
		    out.add(in.markedToStyledBlock(STYLE_VALUE));
		}
	    } else {
		in.moveNext();
	    }
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.xslthl.Highlighter#highlight(net.sf.xslthl.CharIter,
     * java.util.List)
     */
    @Override
    public boolean highlight(CharIter in, List<Block> out) {
	while (!in.finished()) {
	    if (XMLHighlighter.LESS_THAN.equals(in.current())) {
		out.add(in.markedToBlock());
		in.moveNext(); // skip <
		if (XMLHighlighter.SLASH.equals(in.current())) {
		    // closing tag -> tag
		    while (!in.finished()
			    && !XMLHighlighter.GREATER_THAN
				    .equals(in.current())) {
			in.moveNext();
		    }
		    String style = getStyleForTagName(in.getMarked().trim()
			    .substring(2));
		    // </dfsdf > trims to </dfsdf> and than to <dfsdf>
		    in.moveNext(); // get >
		    if (style != null) {
			out.add(in.markedToStyledBlock(style));
		    } else {
			out.add(in.markedToStyledBlock(STYLE_ELEMENT));
		    }
		} else if (XMLHighlighter.QUESTION_MARK.equals(in.current())) {
		    // processing instruction -> directive
		    while (!in.finished()
			    && !(XMLHighlighter.GREATER_THAN.equals(in
				    .current()) && XMLHighlighter.QUESTION_MARK
				    .equals(in.prev()))) {
			in.moveNext();
		    }
		    in.moveNext();
		    out.add(in.markedToStyledBlock(STYLE_PI));
		} else if (XMLHighlighter.EXCLAMATION_MARK.equals(in.current())
			&& XMLHighlighter.HYPHEN.equals(in.next())
			&& XMLHighlighter.HYPHEN.equals(in.next(2))) {
		    // comment
		    while (!in.finished()
			    && !(XMLHighlighter.GREATER_THAN.equals(in
				    .current())
				    && XMLHighlighter.HYPHEN.equals(in.prev()) && XMLHighlighter.HYPHEN
				    .equals(in.prev(2)))) {
			in.moveNext();
		    }
		    in.moveNext();
		    out.add(in.markedToStyledBlock(STYLE_COMMENT));
		} else if (XMLHighlighter.EXCLAMATION_MARK.equals(in.current())
			&& in.startsWith("[CDATA[", 1)) {
		    // CDATA section
		    in.moveNext(8);
		    out.add(in.markedToStyledBlock(STYLE_ELEMENT));
		    int idx = in.indexOf("]]>");
		    if (idx == -1) {
			in.moveToEnd();
		    } else {
			in.moveNext(idx);
		    }
		    out.add(in.markedToBlock());
		    if (idx != -1) {
			in.moveNext(3);
			out.add(in.markedToStyledBlock(STYLE_ELEMENT));
		    }
		} else {
		    // normal tag
		    while (!in.finished()
			    && !XMLHighlighter.GREATER_THAN
				    .equals(in.current())
			    && !XMLHighlighter.SLASH.equals(in.current())
			    && !Character.isWhitespace(in.current())) {
			in.moveNext();
		    }
		    String style = getStyleForTagName(in.getMarked().trim()
			    .substring(1));

		    // find short tag
		    boolean shortTag = false;
		    int cnt = 0;
		    while (!in.finished()
			    && !XMLHighlighter.GREATER_THAN
				    .equals(in.current())
			    && !XMLHighlighter.SLASH.equals(in.current())
			    && Character.isWhitespace(in.current())) {
			in.moveNext();
			++cnt;
		    }
		    if (XMLHighlighter.SLASH.equals(in.current())) {
			in.moveNext();
			++cnt;
		    }
		    if (XMLHighlighter.GREATER_THAN.equals(in.current())) {
			in.moveNext();
			shortTag = true;
		    } else {
			in.moveNext(-cnt);
		    }

		    if (style != null) {
			out.add(in.markedToStyledBlock(style));
		    } else {
			out.add(in.markedToStyledBlock(STYLE_ELEMENT));
		    }
		    if (!shortTag && !in.finished()
			    && Character.isWhitespace(in.current())) {
			readTagContent(in, out);

			if (!in.finished()) {
			    if (XMLHighlighter.SLASH.equals(in.current())) {
				in.moveNext();
			    }
			    in.moveNext();
			    if (style != null) {
				out.add(in.markedToStyledBlock(style));
			    } else {
				out.add(in.markedToStyledBlock(STYLE_ELEMENT));
			    }
			}
		    }
		}
		if (!in.finished()) {
		    in.moveNext();
		}
	    } else {
		in.moveNext();
	    }
	}
	if (in.isMarked()) {
	    out.add(in.markedToBlock());
	}
	return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see net.sf.xslthl.Highlighter#getDefaultStyle()
     */
    @Override
    public String getDefaultStyle() {
	// not really used
	return "xml";
    }
}