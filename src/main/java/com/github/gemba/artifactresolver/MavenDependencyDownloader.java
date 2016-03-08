package com.github.gemba.artifactresolver;

/*******************************************************************************
 * Copyright (c) 2013 by Gemba
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution (see COPYING), and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This utility downloads all artifacts dependencies into a local directory. The
 * downloaded dependencies can be used for an internet-less Nexus or in a local
 * maven repository.
 * 
 * @author Gemba
 */
public class MavenDependencyDownloader {

  private static final String DEFAULT_LOCAL_DOWNLOAD_REPO = "local-repo";
  private static final String DEFAULT_DEPENDENCY_FILE = "dependencies.json";
  private static final String EXTRA_REPO_FILE = "extra-repos.json";

  private static final Logger log = LoggerFactory.getLogger(MavenDependencyDownloader.class);

  private static Options options;

  private static boolean javadoc = false;
  private static boolean sources = false;
  private static String dependencyFile;
  private static String localRepo;
  private static ArrayList<DefaultArtifact> artifacts;
  private static DependencyResolver dependencyResolver;
  private static Map<String, String> extraRepos = new HashMap<String, String>();

  /**
   * Default constructor.
   */
  private MavenDependencyDownloader() {
    createOptions();
  }

  public static void main(String[] args) throws Exception {

    new MavenDependencyDownloader();

    parseCommandLine(args);

    readExtraRepos();

    RepositorySystemHelper repoSystemHelper = new RepositorySystemHelper(localRepo, extraRepos);
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
    log.info("... artifacts downloaded to \"{}\". Finished. Thank you.", localRepo);
  }

  private static void parseCommandLine(String[] args) {
    CommandLineParser parser = new DefaultParser();
    CommandLine line = null;

    try {
      line = parser.parse(options, args);
    } catch (ParseException pex) {
      System.err.println("Parsing failed: " + pex.getMessage());
      System.exit(1);
    }

    if (line.hasOption('h')) {
      HelpFormatter formatter = new HelpFormatter();
      String header = "where each [coord] is expected in the format <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>, separate multiple [coord] by a space. If [coord] is provided the JSON file will be ignored. Defaults are: <extension>=jar, <classifier>=\"\".\n\n Options are:";
      String footer = "\nAdditonal repositories to be searched for dependencies can be added in file '" + EXTRA_REPO_FILE + "'";
      formatter.printHelp(MavenDependencyDownloader.class.getSimpleName() + " [coords...] [options]", header, options, footer);
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

    // look for CLI
    // <groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>
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
  private void createOptions() {
    options = new Options();

    Option help = new Option("h", "help", false, "print this usage and exit");

    Option jsonFile = Option.builder("f").longOpt("dependency-file")
        .desc("use this JSON dependency file (default:" + DEFAULT_DEPENDENCY_FILE + ")").hasArg().argName("JSON-File").build();

    Option depDir = Option.builder("d").longOpt("dependency-dir")
        .desc("download dependencies to this folder (default:" + DEFAULT_LOCAL_DOWNLOAD_REPO + ")").hasArg().argName("Directory")
        .build();

    Option javadoc = Option.builder("j").longOpt("with-javadoc").desc("download javadoc attachment of artifact").build();
    Option sources = Option.builder("s").longOpt("with-sources").desc("download source attachment of artifact").build();

    options.addOption(help);
    options.addOption(depDir);
    options.addOption(jsonFile);
    options.addOption(javadoc);
    options.addOption(sources);
  }

  /**
   * Reads extra repositories file. Expected format is JSON array.
   * 
   * @see #EXTRA_REPO_FILE
   * @throws Exception
   */
  private static void readExtraRepos() throws Exception {

    JSONParser jsonParser = new JSONParser();
    FileReader fileReader = null;
    try {
      fileReader = new FileReader(new File(EXTRA_REPO_FILE));
      JSONArray jsonArray = (JSONArray) jsonParser.parse(fileReader);
      fileReader.close();
      for (Object obj : jsonArray) {
        JSONObject jsonObj = (JSONObject) obj;
        String id = (String) jsonObj.get("id");
        String repourl = (String) jsonObj.get("repourl");
        extraRepos.put(id, repourl);
      }
    } catch (FileNotFoundException exc) {
      log.debug("No extra repositories defined. File not found: {}.", EXTRA_REPO_FILE);
      return;
    } finally {
      IOUtils.closeQuietly(fileReader);
    }
  }

}