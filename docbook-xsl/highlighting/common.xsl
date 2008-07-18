<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
		xmlns:d="http://docbook.org/ns/docbook"

		xmlns:s6hl="http://net.sf.xslthl/ConnectorSaxon6" 
		xmlns:sbhl="http://net.sf.xslthl/ConnectorSaxonB" 
		xmlns:xhl="http://net.sf.xslthl/ConnectorXalan"
		xmlns:saxon6="http://icl.com/saxon" 
		xmlns:saxonb="http://saxon.sf.net/" 
		xmlns:xalan="http://xml.apache.org/xalan"
		
		xmlns:exsl="http://exslt.org/common"
		xmlns:xslthl="http://xslthl.sf.net"
		exclude-result-prefixes="exsl d xslthl s6hl sbhl xhl"
		version='1.0'>

<!-- ********************************************************************
     $Id: common.xsl 7266 2007-08-22 11:58:42Z xmldoc $
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://docbook.sf.net/release/xsl/current/ for
     and other information.

     ******************************************************************** -->

<!-- this parameter is used to set the location of the xslthl configuration filename -->
<xsl:param name="xslthl.config" />

<!-- this construction is needed to have the saxon and xalan connectors working alongside each other -->
<xalan:component prefix="xhl" functions="highlight">
	<xalan:script lang="javaclass" src="xalan://net.sf.xslthl.ConnectorXalan" />
</xalan:component>

<!-- for saxon 6 -->
<saxon6:script implements-prefix="s6hl" language="java" src="java:net.sf.xslthl.ConnectorSaxon6" />

<!-- for saxon 8.5 and later -->
<saxonb:script implements-prefix="sbhl" language="java" src="java:net.sf.xslthl.ConnectorSaxonB" />  


<!-- You can override this template to do more complex mapping of
     language attribute to highlighter language ID (see xslthl-config.xml) -->
<xsl:template name="language.to.xslthl">
  <xsl:param name="context"/>

  <xsl:choose>
    <xsl:when test="$context/@language != ''">
      <xsl:value-of select="$context/@language"/>
    </xsl:when>
    <xsl:when test="$highlight.default.language != ''">
      <xsl:value-of select="$highlight.default.language"/>
    </xsl:when>
  </xsl:choose>
</xsl:template>

<!-- a fallback when the specific style isn't recognized -->
<xsl:template match="xslthl:*">
  <xsl:message>
    <xsl:text>unprocessed xslthl style: </xsl:text>
    <xsl:value-of select="local-name(.)" />
  </xsl:message>
  <xsl:value-of select="." />
</xsl:template>

<xsl:template name="apply-highlighting">
  <xsl:choose>
    <!-- Do we want syntax highlighting -->
    <xsl:when test="$highlight.source != 0">
      <xsl:variable name="language">
	<xsl:call-template name="language.to.xslthl">
	  <xsl:with-param name="context" select="."/>
	</xsl:call-template>
      </xsl:variable>
      <xsl:choose>
	<xsl:when test="$language != ''">
	  <xsl:choose>
	    <xsl:when test="function-available('s6hl:highlight')">
	      <xsl:apply-templates select="s6hl:highlight($language, exsl:node-set(.), $xslthl.config)"/>
	    </xsl:when>
	    <xsl:when test="function-available('sbhl:highlight')">
	      <xsl:apply-templates select="sbhl:highlight($language, exsl:node-set(.), $xslthl.config)"/>
	    </xsl:when>
	    <xsl:when test="function-available('xhl:highlight')">
	      <xsl:apply-templates select="xhl:highlight($language, exsl:node-set(.), $xslthl.config)"/>
	    </xsl:when>
	    <xsl:otherwise>
	      <xsl:apply-templates />
	    </xsl:otherwise>
	  </xsl:choose>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:apply-templates/>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <!-- No syntax highlighting -->
    <xsl:otherwise>
      <xsl:apply-templates/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>

