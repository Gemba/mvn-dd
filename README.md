Dependency Downloader for Maven Artifacts 
========================================= 

Summary
-------
Resolves and downloads all dependencies of a customizable set 
of maven artifacts.

Motivation 
---------- 
Sometimes it is desirable to resolve all dependency of maven artifacts 
without editing/copying your project pom (e.g. your pom is in a 
separated net without internet connection). This tool fills the gap: 
Enter the desired artifacts in a JSON file (see dependencies.json) or 
provide the artifacts on the command line (see Usage) and start this 
program. It will download the specified artifacts with all dependencies. 
Additionally this tool can also download the javadoc and source 
attachments. The downloaded artifacts can then be used in an 
"internet-less/offline" Nexus or in your local maven repository. 

Usage 
----- 
Since I assume you are familar with maven, compiling and starting is 
straight forward, eg.:

    $ mvn package exec:java -Dexec.args="--help" 

or easier

    $ ./mvn-dd --help
 
will give you the options to fiddle about. 

Sample usage after filling the file dependencies.json with the wanted 
artifacts to download artifacts with javadoc and source: 

    $ mvn package exec:java -Dexec.args="--with-javadoc --with-sources" 

Alternatively you may use the jar:

    $ java -jar target/mvn-dependency-downloader-jar-with-dependencies.jar --help

(for the lazy: there are also short-option names ;-) ) 

In either case you may provide the "coordinates" of the artifacts 
directly, e.g.: 

    $ java -jar target/mvn-dependency-downloader-jar-with-dependencies.jar org.apache.cxf:cxf-codegen-plugin:2.7.5 [...]

The coordinates of an artifact are expected in this format (separate 
each artifact by a space): 

    <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>

Limitations 
----------- 
  * Some artifacts do not provide source or javadoc 
    attachments. The program will state an warning in such cases. 
  * Does not download eg. test scope within the dependency tree. See also issue #1

License 
------- 
Eclipse Public License (see COPYING)

Changelog
---------

### v0.3 (Mar 2016) ###
  * Version bumps of referenced libraries
  * Added shell scripts Linux and Windows
  * Corrected package structure

### v0.2 (June 2013) ###
  * Providing artifact coordinates from CLI

### v0.1 (May 2013) ###
  * Initial release to Github

References
----------
[Aether Wiki](https://wiki.eclipse.org/Aether)




