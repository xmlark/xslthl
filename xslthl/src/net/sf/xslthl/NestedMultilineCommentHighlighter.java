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

import java.util.List;

class NestedMultilineCommentHighlighter extends MultilineCommentHighlighter {

    NestedMultilineCommentHighlighter(Params params) {
	super(params);
    }

    @Override
    boolean highlight(CharIter in, List<Block> out) {
	in.moveNext(start.length()); // skip start

	int depth = 1;
	while (!in.finished()) {
	    if (in.startsWith(end)) {
		in.moveNext(end.length());
		if (depth == 1) {
		    break;
		} else {
		    depth--;
		}
	    } else if (in.startsWith(start)) {
		depth++;
		in.moveNext(start.length());
	    } else {
		in.moveNext();
	    }
	}

	out.add(in.markedToStyledBlock("comment"));
	return true;
    }

}