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
import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 * This utility downloads all artifacts dependencies into a local directory. The downloaded dependencies can be used for an
 * internet-less Nexus or in a local maven repository.
 * 
 * @author Gemba
 */
public class MavenDependencyDownloader {

  private static final String DEFAULT_LOCAL_DOWNLOAD_REPO = "local-repo";
  private static final String DEFAULT_DEPENDENCY_FILE = "dependencies.json";

  private static final Logger log = LoggerFactory.getLogger(MavenDependencyDownloader.class);

  private static Options options;

  private static boolean javadoc = false;
  private static boolean sources = false;
  private static String dependencyFile;
  private static String localRepo;
  private static ArrayList<DefaultArtifact> artifacts;
  private static DependencyResolver dependencyResolver;

  /**
   * Default constructor.
   */
  private MavenDependencyDownloader() {
    createOptions();
  }

  public static void main(String[] args) throws Exception {

    new MavenDependencyDownloader();

    parseCommandLine(args);

    RepositorySystemHelper repoSystemHelper = new RepositorySystemHelper(localRepo);
    dependencyResolver = new DependencyResolver(repoSystemHelper);

    if (artifacts.isEmpty()) {
      JSONParser jsonParser = new JSONParser();
      FileReader fileReader = new FileReader(new File(dependencyFile));
      JSONArray jsonArray = (JSONArray) jsonParser.parse(fileReader);
      fileReader.close();

      for (Object obj : jsonArray) {
        JSONObject jsonObj = (JSONObject) obj;
        String groupId = (String) jsonObj.get("groupId");
        String artifactId = (String) jsonObj.get("artifactId");
        String classifier = (String) jsonObj.get("classifier");
        String extension = (String) jsonObj.get("extension");
        String version = (String) jsonObj.get("version");

        DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, classifier, extension, version);
        dependencyResolver.downloadDependencyTree(artifact, javadoc, sources);
      }
    } else {
      for (DefaultArtifact artifact : artifacts) {
        dependencyResolver.downloadDependencyTree(artifact, javadoc, sources);
      }
    }
    log.info("Artifacts downloaded to \"{}\". Finished. Thank you.", localRepo);
  }

  private static void parseCommandLine(String[] args) {
    CommandLineParser parser = new GnuParser();
    CommandLine line = null;

    try {
      line = parser.parse(options, args);
    } catch (ParseException pex) {
      System.err.println("Parsing failed: " + pex.getMessage());
      System.exit(1);
    }

    if (line.hasOption('h')) {
      HelpFormatter formatter = new HelpFormatter();
      String header = "where each [coord] is expected in the format <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>, separate multiple [coord] by a space. If [coord] is provided the JSON file will be ignored. Defaults are: <extension>=jar, <classifier>=\"\".\n Options are:";
      formatter.printHelp(MavenDependencyDownloader.class.getSimpleName() + " [coords...] [options]", header, options, null);
      System.exit(0);
    }

    if (line.hasOption('j')) {
      javadoc = true;
    }

    if (line.hasOption('s')) {
      sources = true;
    }

    dependencyFile = line.getOptionValue('f', DEFAULT_DEPENDENCY_FILE);
    localRepo = line.getOptionValue('d', DEFAULT_LOCAL_DOWNLOAD_REPO);

    // look for CLI <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>
    artifacts = new ArrayList<DefaultArtifact>();
    for (String arg : line.getArgs()) {
      if (arg.contains(":")) {
        DefaultArtifact artifact = new DefaultArtifact(arg);
        artifacts.add(artifact);
      }
    }
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

    Option javadoc = OptionBuilder.withLongOpt("with-javadoc").withDescription("download javadoc attachment of artifact")
        .create('j');
    Option sources = OptionBuilder.withLongOpt("with-sources").withDescription("download source attachment of artifact")
        .create('s');

    options.addOption(help);
    options.addOption(depDir);
    options.addOption(jsonFile);
    options.addOption(javadoc);
    options.addOption(sources);
  }
}