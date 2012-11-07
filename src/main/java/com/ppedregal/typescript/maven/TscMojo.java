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
import java.util.List;

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
     * Source directory for .d.ts source files
     * @parameter expression="${ts.libraryDirectory}" default-value="src/main/d.ts"
     */
    private File libraryDirectory;

    /**
     * Encoding for files
     * @parameter expression="${project.build.sourceEncoding}
     */
    private String encoding = "utf-8";

    /**
     * Set to true to watch for changes to files and re-compile them on the fly.
     *
     * @parameter expression="${ts.watch}"
     */
    private boolean watch = false;

    /**
     * Set to true to ignore the lib.d.ts by default
     *
     * @parameter expression="${ts.nolib}"
     */
    private boolean noStandardLib = true;

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
            throw createMojoExecutionException(e);
        }

        doCompileFiles(false);

        if (watch) {

        }
    }

    private void doCompileFiles(boolean checkTimestamp) throws MojoExecutionException {
        try {
            int compiledFiles = 0;
            getLog().info("Searching directory " + sourceDirectory.getCanonicalPath());
            Collection<File> filenames = FileUtils.listFiles(sourceDirectory, new String[] {"ts"}, true);
            for (File file : filenames) {
                try {
                    String path = file.getPath().substring(sourceDirectory.getPath().length());
                    String sourcePath = path;
                    String targetPath = FilenameUtils.removeExtension(path) + ".js";
                    File sourceFile = new File(sourceDirectory, sourcePath).getAbsoluteFile();
                    File targetFile = new File(targetDirectory, targetPath).getAbsoluteFile();
                    if (!targetFile.exists() || !checkTimestamp || sourceFile.lastModified() > targetFile.lastModified()) {
                        String sourceFilePath = sourceFile.getPath();
                        getLog().info(String.format("Compiling: %s", sourceFilePath));
                        String generatePath = targetFile.getPath();
                        tsc("--out", generatePath, sourceFilePath);
                        getLog().info(String.format("Generated: %s", generatePath));
                        compiledFiles++;
                    }
                } catch (TscInvocationException e) {
                    getLog().error(e.getMessage());
                    if (getLog().isDebugEnabled()) {
                        getLog().debug(e);
                    }
                }
            }
            if (compiledFiles == 0) {
                getLog().info("Nothing to compile");
            } else {
                getLog().info(String.format("Compiled %s file(s)", compiledFiles));
            }
        } catch (IOException e) {
            throw createMojoExecutionException(e);
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
            if (noStandardLib) {
                argv.put(i++, argv, "--nolib");
            }
			for (String s:args){
				argv.put(i++, argv, s);
			}
            if (libraryDirectory.exists()) {
                File[] libFiles = libraryDirectory.listFiles();
                if (libFiles != null) {
                    for (File libFile : libFiles) {
                        if (libFile.getName().endsWith(".d.ts") && libFile.exists()) {
                            String path = libFile.getAbsolutePath();
                            getLog().info("Adding library file " + libFile);
                            argv.put(i++, argv, path);
                        }
                    }
                }
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

    private MojoExecutionException createMojoExecutionException(IOException e) {
        return new MojoExecutionException(e.getMessage());
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
