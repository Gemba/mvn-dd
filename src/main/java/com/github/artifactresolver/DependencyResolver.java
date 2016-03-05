package com.github.artifactresolver;

/*******************************************************************************
 * Copyright (c) 2013 by Gemba
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution (see COPYING), and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.resolution.DependencyResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.artifact.JavaScopes;

/**
 * Instrumentation class for dependencies. Delegates the resolution to the {@link RepositorySystemHelper}
 * 
 * @author Gemba
 */
public class DependencyResolver {

  private static final Logger log = LoggerFactory.getLogger(DependencyResolver.class);

  private RepositorySystemHelper repoSystemHelper;

  /**
   * Default constructor.
   * 
   * @param repoSystemHelper
   *          does the actual work
   */
  public DependencyResolver(RepositorySystemHelper repoSystemHelper) {
    this.repoSystemHelper = repoSystemHelper;
  }

  /**
   * Resolves and downloads an artifact with its dependencies.
   * 
   * @param artifact
   *          artifact to resolve
   * @param javadoc
   *          <code>true</code> if javadoc attachment should be retrieved too
   * @param sources
   *          <code>true</code> if sources attachment should be retrieved too
   * @throws DependencyCollectionException
   *           if the dependency graph could not be properly assembled
   * @throws DependencyResolutionException
   *           if a dependency is not resolvable
   */
  public void downloadDependencyTree(DefaultArtifact artifact, boolean javadoc, boolean sources)
      throws DependencyCollectionException, DependencyResolutionException {
    log.info("Resolving: {} with these dependencies", artifact.toString());

    Dependency dependency = new Dependency(artifact, null);

    DependencyNode jarNode = repoSystemHelper.collectDependencies(dependency);

    jarNode.accept(new DependencyGraphPrinter());

    DependencyRequest dependencyRequest = new DependencyRequest(jarNode, null);
    DependencyResult dependencyResult = repoSystemHelper.resolveDependencies(dependencyRequest);

    if (javadoc) {
      downloadAttachments(dependencyResult, "javadoc");
    }

    if (sources) {
      downloadAttachments(dependencyResult, "sources");
    }
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
  private void downloadAttachments(DependencyResult depResult, final String attachment) throws DependencyCollectionException {

    for (ArtifactResult artifactResult : depResult.getArtifactResults()) {

      final Artifact artifact = artifactResult.getArtifact();
      final String artifactId = artifact.getArtifactId();
      final String groupId = artifact.getGroupId();
      final String extension = artifact.getExtension();
      final String version = artifact.getVersion();

      log.info("Resolving {} for {}", attachment, artifact);

      try {
        DefaultArtifact extraArtifact = new DefaultArtifact(groupId, artifactId, attachment, extension, version);
        Dependency attachedDependency = new Dependency(extraArtifact, JavaScopes.COMPILE);

        DependencyNode attachmentNode = repoSystemHelper.collectDependencies(attachedDependency);
        DependencyRequest javadocDependencyRequest = new DependencyRequest(attachmentNode, null);

        repoSystemHelper.resolveDependencies(javadocDependencyRequest);
      } catch (DependencyResolutionException de) {
        log.warn("No {} found for {}", attachment, artifact);
      }
    }
  }
}
