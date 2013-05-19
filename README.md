Dependency Downloader for Maven Artifacts 
========================================= 

Motivation 
---------- 
Sometimes it is desirable to resolve all dependency of maven artifacts 
without editing/copying your project pom (e.g. your pom is in a 
separated net without internet connection). This tool fills the gap: 
Enter the desired artifacts in a JSON file (see dependencies.json) and 
start this program. It will download the specifies artifacts with all 
dependencies. Additionally this tool can also download the javadoc and 
source attachments. The downloaded artifacts can then be used in an 
"internet-less" Nexus or in your local maven repository. 

Usage 
----- 
Since I assume you are familar with maven, compiling and starting is 
straight forward:

  $ mvn package exec:java -Dexec.args="--help" 
 
will give you the options to fiddle about. 

Sample usage after editing dependencies.json to 
download artifacts with javadoc and source: 

  $ mvn package exec:java -Dexec.args="--with-javadoc --with-sources" 

(for the lazy: there are also short-option names ;-) ) 

Limitations 
----------- 
Some artifacts do not provide source or javadoc 
attachments. The program will state an warning in such cases. 

License 
------- 
Eclipse Public License (see COPYING) 

