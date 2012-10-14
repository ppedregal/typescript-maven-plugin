<h1>TypeScript Maven Plugin</h1>
Maven plugin that integrates <a href="http://typescript.codeplex.com/">TypeScript</a> compiler into maven builds

To use the plugin in maven you need to follow these steps:

1) Add the following plugin repository to your pom.xml

<pre>
    &lt;pluginRepository&gt;
      &lt;id&gt;typescript-maven-plugin&lt;/id&gt;
      &lt;url&gt;https://raw.github.com/ppedregal/typescript-maven-plugin/master/repo&lt;/url&gt;
    &lt;/pluginRepository&gt;
</pre>

2) Add the following build plugin to your pom.xml

<pre>
      &lt;plugin&gt;
        &lt;groupId&gt;com.ppedregal.typescript.maven&lt;/groupId&gt;
      	&lt;artifactId&gt;typescript-maven-plugin&lt;/artifactId&gt;        
        &lt;configuration&gt;
        	&lt;sourceDirectory&gt;src/main/ts&lt;/sourceDirectory&gt;
        	&lt;targetDirectory&gt;target/ts&lt;/targetDirectory&gt;
        &lt;/configuration&gt;        
      &lt;/plugin&gt;
</pre>

More documentation in the generated maven site <a href="http://ppedregal.github.com/typescript-maven-plugin/">here</a>
