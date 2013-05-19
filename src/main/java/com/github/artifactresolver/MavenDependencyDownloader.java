package com.github.artifactresolver;
/*******************************************************************************
 * Copyright (c) 2013 by Gemba
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution (see COPYING), and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import java.io.File;
import java.io.FileReader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.repository.internal.MavenServiceLocator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.resolution.DependencyResult;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;

/**
 * This utility downloads all artifacts dependencies into a local directory. The downloaded dependencies can be used for an
 * internet-less Nexus or in a local maven repository.
 * 
 * @author Gemba
 */
public class MavenDependencyDownloader {

  private static final String DEFAULT_LOCAL_DOWNLOAD_REPO = "local-repo";
  private static final String DEFAULT_DEPENDENCY_FILE = "dependencies.json";

  private static Logger log = LoggerFactory.getLogger(MavenDependencyDownloader.class);

  private static CollectRequest collectRequest;
  private static Options options;
  private static RepositorySystem repoSystem;
  private static RepositorySystemSession session;

  /**
   * Default constructor.
   */
  private MavenDependencyDownloader() {

    createOptions();
  }

  public static void main(String[] args) throws Exception {

    new MavenDependencyDownloader();

    CommandLineParser parser = new GnuParser();
    CommandLine line = null;
    try {
      line = parser.parse(options, args);
    } catch (ParseException exp) {
      System.err.println("Parsing failed: " + exp.getMessage());
      System.exit(1);
    }

    if (line.hasOption('h')) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(MavenDependencyDownloader.class.getSimpleName(), options);
      System.exit(0);
    }

    boolean javadoc = false;
    if (line.hasOption('j')) {
      javadoc = true;
    }

    boolean sources = false;
    if (line.hasOption('s')) {
      sources = true;
    }

    String dependencyFile = line.getOptionValue('f', DEFAULT_DEPENDENCY_FILE);
    String localRepo = line.getOptionValue('d', DEFAULT_LOCAL_DOWNLOAD_REPO);

    FileReader fileReader = new FileReader(new File(dependencyFile));

    JSONParser jsonParser = new JSONParser();
    JSONArray jsonArray = (JSONArray) jsonParser.parse(fileReader);
    fileReader.close();

    init(localRepo);

    for (Object obj : jsonArray) {
      JSONObject jsonObj = (JSONObject) obj;

      String groupId = (String) jsonObj.get("groupId");
      String artifactId = (String) jsonObj.get("artifactId");
      String classifier = (String) jsonObj.get("classifier");
      String extension = (String) jsonObj.get("extension");
      String version = (String) jsonObj.get("version");

      log.info("Resolving: {}...", jsonObj);

      DefaultArtifact defaultArtifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version);

      Dependency dependency = new Dependency(defaultArtifact, JavaScopes.COMPILE);

      collectRequest.setRoot(dependency);

      DependencyNode jarNode = repoSystem.collectDependencies(session, collectRequest).getRoot();
      DependencyRequest dependencyRequest = new DependencyRequest(jarNode, null);
      DependencyResult dependencyResult = repoSystem.resolveDependencies(session, dependencyRequest);

      PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
      jarNode.accept(nlg);
      log.info("...with following jar dependencies: {}", nlg.getClassPath());

      if (javadoc) {
        downloadAttachments(dependencyResult, "javadoc");
      }

      if (sources) {
        downloadAttachments(dependencyResult, "sources");
      }
    }
    log.info("Artifacts downloaded to \"{}\". Finished. Thank you.", localRepo);
  }

  /**
   * Set up CLI options.
   */
  @SuppressWarnings("static-access")
  private void createOptions() {
    options = new Options();

    Option help = new Option("h", "help", false, "print this usage and exit");

    Option jsonFile = OptionBuilder.withLongOpt("dependency-file")
        .withDescription("use this JSON dependency file (default:" + DEFAULT_DEPENDENCY_FILE + ")").hasArg()
        .withArgName("JSON-File").create('f');

    Option depDir = OptionBuilder.withLongOpt("dependency-dir")
        .withDescription("download dependencies to this folder (default:" + DEFAULT_LOCAL_DOWNLOAD_REPO + ")").hasArg()
        .withArgName("Directory").create('d');

    Option javadoc = OptionBuilder.withLongOpt("with-javadoc").withDescription("download javadoc attachment of artifact").create('j');
    Option sources = OptionBuilder.withLongOpt("with-sources").withDescription("download source attachment of artifact").create('s');

    options.addOption(help);
    options.addOption(depDir);
    options.addOption(jsonFile);
    options.addOption(javadoc);
    options.addOption(sources);
  }

  /**
   * Initalizes the aether environment.
   * 
   * @param localRepoDir
   *          path to downloaded dependencies
   */
  private static void init(String localRepoDir) {
    repoSystem = newRepositorySystem();

    session = newSession(repoSystem, localRepoDir);

    RemoteRepository central = new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/");
    RemoteRepository codehaus = new RemoteRepository("codehaus", "default", "http://dist.codehaus.org/");
    RemoteRepository ibiblio = new RemoteRepository("ibiblio", "default", "http://www.ibiblio.org/maven");

    collectRequest = new CollectRequest();
    collectRequest.addRepository(central);
    collectRequest.addRepository(codehaus);
    collectRequest.addRepository(ibiblio);
  }

  /**
   * Set up repository session for maven.
   * 
   * @param system
   *          the repository system
   * @param localDownloadDir
   *          the directory where to put the downloaded artifacts
   * @return the configured repository session
   */
  private static RepositorySystemSession newSession(RepositorySystem system, final String localDownloadDir) {
    MavenRepositorySystemSession session = new MavenRepositorySystemSession();

    LocalRepository localRepo = new LocalRepository(localDownloadDir);
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepo));

    return session;
  }

  /**
   * Set up repository system for maven.
   * 
   * @return the {@link RepositorySystem}
   */
  private static RepositorySystem newRepositorySystem() {
    MavenServiceLocator locator = new MavenServiceLocator();
    locator.setServices(WagonProvider.class, new ManualWagonProvider());
    locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);

    return locator.getService(RepositorySystem.class);
  }

  /**
   * Downloads additional artifacts like javadoc or sources.
   * 
   * @param depResult
   *          a set of resolved dependencies
   * @param attachment
   *          type of attachment. Either "javadoc" or "sources"
   * @throws DependencyCollectionException
   */
  private static void downloadAttachments(DependencyResult depResult, final String attachment) throws DependencyCollectionException {
    for (ArtifactResult artifactResult : depResult.getArtifactResults()) {

      Artifact artifact = artifactResult.getArtifact();
      String artifactId = artifact.getArtifactId();
      String groupId = artifact.getGroupId();
      String extension = artifact.getExtension();
      String version = artifact.getVersion();

      log.info("Resolving {} for {}", attachment, artifact);
      try {
        DefaultArtifact javadocArtifact = new DefaultArtifact(groupId, artifactId, attachment, extension, version);
        Dependency javadocDependency = new Dependency(javadocArtifact, JavaScopes.COMPILE);

        collectRequest.setRoot(javadocDependency);
        DependencyNode attachmentNode = repoSystem.collectDependencies(session, collectRequest).getRoot();

        DependencyRequest javadocDependencyRequest = new DependencyRequest(attachmentNode, null);
        repoSystem.resolveDependencies(session, javadocDependencyRequest);
      } catch (DependencyResolutionException de) {
        log.warn("No {} found for {}", attachment, artifact);
      }
    }
  }
}