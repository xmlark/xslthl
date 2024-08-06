package net.sf.xslthl;

import javax.xml.transform.TransformerException;
import net.sf.saxon.Configuration;
import net.sf.saxon.lib.Initializer;

/**
 *
 * @author tgraham
 */
public class ConnectorSaxon9apiInitializer implements Initializer {

    @Override
    public void initialize(Configuration c) throws TransformerException {
        c.registerExtensionFunction(new ConnectorSaxon9api());
    }
    
}
