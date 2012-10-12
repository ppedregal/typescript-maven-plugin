package com.ppedregal.typescript.maven;

import java.io.File;

import junit.framework.TestCase;

public class TscMojoTest extends TestCase {

	/**
     * @throws Exception if any
     */
    public void testExecute()
        throws Exception
    {
    	TscMojo mojo = new TscMojo();
    	mojo.setSourceDirectory(new File("src/test/resources/testproject1/src/main/ts"));
    	mojo.setTargetDirectory(new File("src/test/resources/testproject1/target/ts"));
    	mojo.execute();
    }
	
}
