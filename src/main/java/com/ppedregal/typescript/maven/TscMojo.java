package com.ppedregal.typescript.maven;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.commonjs.module.RequireBuilder;
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider;

/**
 * Goal which compiles a set of TypeScript files
 *
 * @goal tsc
 * 
 * @phase compile
 */
public class TscMojo
    extends AbstractMojo
{
    /**
     * Output directory for .js compiled files
     * @parameter expression="${ts.targetDirectory}" default-value="target/ts"
     * @required
     */
    private File targetDirectory;
    
    /**
     * Source directory for .ts source files
     * @parameter expression="${ts.sourceDirectory}" default-value="src/main/ts"
     */
    private File sourceDirectory;
    
    /**
     * Encoding for files
     * @parameter expression="${project.build.sourceEncoding}
     */
    private String encoding = "utf-8";
    
    private Script nodeScript;
    private Script tscScript;
    private ScriptableObject globalScope;
    
    public void execute()
        throws MojoExecutionException
    {
    	prepare(sourceDirectory);
    	prepare(targetDirectory);
    	
    	try {
			compileScripts();
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage());
		}
    	
    	int compiledFiles = 0;
    	Collection<File> filenames = FileUtils.listFiles(sourceDirectory, new String[]{"ts"}, true);
    	for (File file : filenames) {
    		try {
	    		String path = file.getPath().substring(sourceDirectory.getPath().length());
	    		String sourcePath = path;
	    		String targetPath = FilenameUtils.removeExtension(path)+".js";
	    		File sourceFile = new File(sourceDirectory,sourcePath).getAbsoluteFile();
	    		File targetFile = new File(targetDirectory,targetPath).getAbsoluteFile();	    		
	    		if (targetFile.exists() && sourceFile.lastModified()>targetFile.lastModified()){		    		
		    		getLog().info(String.format("Compiling: %s", sourcePath));	    		
		    		tsc("--out",targetFile.getPath(),sourceFile.getPath());
		    		getLog().info(String.format("Generated: %s", targetPath));	
		    		compiledFiles++;
	    		} 
    		} catch (TscInvocationException e){
    			getLog().error(e.getMessage());
    			if (getLog().isDebugEnabled()){
    				getLog().debug(e);
    			}
    		}   
		}
    	if (compiledFiles==0){
        	getLog().info("Nothing to compile");    		
    	} else {
    		getLog().info(String.format("Compiled %s files",compiledFiles));
    	}
    }
    
    private void compileScripts() throws IOException {
    	try {
        	Context.enter();
        	Context ctx = Context.getCurrentContext();
        	ctx.setOptimizationLevel(9);
        	globalScope = ctx.initStandardObjects();	    	
    		RequireBuilder require = new RequireBuilder();
    		require.setSandboxed(false);
    		require.setModuleScriptProvider(new SoftCachingModuleScriptProvider(new ClasspathModuleSourceProvider()));
    		require.createRequire(ctx, globalScope).install(globalScope);	
    		nodeScript = compile(ctx,"node.js");
    		tscScript = compile(ctx,"tsc.js");
    	} finally {
    		Context.exit();
    		
    	}
    	Context.enter();
    }
    
    private Script compile(Context context,String resource) throws IOException {
    	InputStream stream =  TscMojo.class.getClassLoader().getResourceAsStream(resource);
    	if (stream==null){
    		throw new FileNotFoundException("Resource open error: "+resource);
    	}
    	try {
    		return context.compileReader(new InputStreamReader(stream), resource, 1, null);
    	} catch (IOException e){
    		throw new IOException("Resource read error: "+resource);
    	} finally {
    		try {
    			stream.close();
    		} catch (IOException e){
    			throw new IOException("Resource close error: "+resource);
    		}
    		stream = null;
    	}
    }
    
    private void tsc(String...args) throws TscInvocationException {

    	try {
    		Context.enter();
	    	Context ctx = Context.getCurrentContext();

	    	nodeScript.exec(ctx, globalScope);
	    	
			NativeObject proc = (NativeObject)globalScope.get("process");
			NativeArray argv = (NativeArray)proc.get("argv");
			argv.defineProperty("length", 0, ScriptableObject.EMPTY);
			int i = 0;
			argv.put(i++, argv, "node");
			argv.put(i++, argv, "tsc.js");
			for (String s:args){
				argv.put(i++, argv, s);
			}
			proc.defineProperty("encoding", encoding, ScriptableObject.READONLY);

			NativeObject mainModule = (NativeObject)proc.get("mainModule");
			mainModule.defineProperty("filename", new File("tsc.js").getAbsolutePath(),ScriptableObject.READONLY);
			
			tscScript.exec(ctx,globalScope);
			
    	} catch (JavaScriptException e){
    		if (e.getValue() instanceof NativeJavaObject){
    			NativeJavaObject njo = (NativeJavaObject)e.getValue();
    			Object o = njo.unwrap();
    			if (o instanceof ProcessExit){
    				ProcessExit pe = (ProcessExit)o;
    				if (pe.getStatus()!=0){
    					throw new TscInvocationException("Process Error: "+pe.getStatus());
    				} 
    			} else {
    				throw new TscInvocationException("Javascript Error",e);
    			}
    		} else {
        		throw new TscInvocationException("Javascript Error",e);    			
    		}
    	} catch (RhinoException e){
    		throw new TscInvocationException("Rhino Error",e);
    	} finally {
        	Context.exit();    		
    	}
    	
    }
    
    /**
     * Create directory if did not exist
     * @param f
     */
    private void prepare(File f){
        if ( !f.exists() )
        {
            f.mkdirs();
        }
    }

	public File getTargetDirectory() {
		return targetDirectory;
	}

	public void setTargetDirectory(File targetDirectory) {
		this.targetDirectory = targetDirectory;
	}

	public File getSourceDirectory() {
		return sourceDirectory;
	}

	public void setSourceDirectory(File sourceDirectory) {
		this.sourceDirectory = sourceDirectory;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	

}
