package com.github.artifactresolver;

/*******************************************************************************
 * Copyright (c) 2013 by Gemba
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution (see COPYING), and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import org.apache.maven.repository.internal.MavenRepositorySystemSession;
import org.apache.maven.repository.internal.MavenServiceLocator;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.connector.wagon.WagonProvider;
import org.sonatype.aether.connector.wagon.WagonRepositoryConnectorFactory;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.resolution.DependencyResult;
import org.sonatype.aether.spi.connector.RepositoryConnectorFactory;
import org.sonatype.aether.util.artifact.JavaScopes;
import org.sonatype.aether.util.graph.selector.AndDependencySelector;
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector;
import org.sonatype.aether.util.graph.selector.OptionalDependencySelector;
import org.sonatype.aether.util.graph.selector.ScopeDependencySelector;
import org.sonatype.aether.collection.DependencySelector;

/**
 * Helper class for resolving dependencies with a set of remote repositories.
 * 
 * @author Gemba
 */
public class RepositorySystemHelper {

  private RepositorySystem repoSystem;
  private RepositorySystemSession session;
  private CollectRequest collectRequest;

  /**
   * Initalizes the aether environment. Uses these repositories:
   * <ul>
   * <li>http://repo1.maven.org/maven2/
   * <li>http://dist.codehaus.org/
   * <li>http://www.ibiblio.org/maven/
   * </ul>
   * 
   * @param localRepoDir
   *          path where to put the downloaded dependencies
   */
  public RepositorySystemHelper(String localRepoDir) {
    repoSystem = newRepositorySystem();

    session = newSession(repoSystem, localRepoDir);

    RemoteRepository central = new RemoteRepository("central", "default", "http://repo1.maven.org/maven2/");
    RemoteRepository codehaus = new RemoteRepository("codehaus", "default", "http://dist.codehaus.org/");
    RemoteRepository ibiblio = new RemoteRepository("ibiblio", "default", "http://www.ibiblio.org/maven/");

    collectRequest = new CollectRequest();
    collectRequest.addRepository(central);
    collectRequest.addRepository(codehaus);
    collectRequest.addRepository(ibiblio);
  }

  /**
   * Collects the dependencies for a artifact.
   * 
   * @param dependency
   *          the artifact to resolve
   * @return a tree structure of @link {@link DependencyNode}.
   * @throws DependencyCollectionException
   */
  public DependencyNode collectDependencies(Dependency dependency) throws DependencyCollectionException {
    collectRequest.setRoot(dependency);
    return repoSystem.collectDependencies(session, collectRequest).getRoot();
  }

  /**
   * Downloads the dependencies of an artifact.
   * 
   * @param dependencyRequest
   *          the dependency request
   * @return the result which contains the downloaded artifacts
   * @throws DependencyResolutionException
   */
  public DependencyResult resolveDependencies(DependencyRequest dependencyRequest) throws DependencyResolutionException {
    return repoSystem.resolveDependencies(session, dependencyRequest);
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
  private RepositorySystemSession newSession(RepositorySystem system, final String localDownloadDir) {
    MavenRepositorySystemSession session = new MavenRepositorySystemSession();

    LocalRepository localRepo = new LocalRepository(localDownloadDir);
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(localRepo));

    DependencySelector depFilter =
        new AndDependencySelector(
        new ScopeDependencySelector(JavaScopes.PROVIDED),
        new OptionalDependencySelector(),
        new ExclusionDependencySelector()
    );
    session.setDependencySelector(depFilter);

    return session;
  }

  /**
   * Set up repository system for maven.
   * 
   * @return the {@link RepositorySystemHelper}
   */
  private RepositorySystem newRepositorySystem() {
    MavenServiceLocator locator = new MavenServiceLocator();
    locator.setServices(WagonProvider.class, new ManualWagonProvider());
    locator.addService(RepositoryConnectorFactory.class, WagonRepositoryConnectorFactory.class);

    return locator.getService(RepositorySystem.class);
  }
}
