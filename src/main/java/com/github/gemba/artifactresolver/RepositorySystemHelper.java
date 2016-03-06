package com.github.gemba.artifactresolver;

/*******************************************************************************
 * Copyright (c) 2013 by Gemba
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution (see COPYING), and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.spi.log.LoggerFactory;
import org.eclipse.aether.spi.log.NullLoggerFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

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

    RemoteRepository central = new RemoteRepository.Builder("central", "default", "http://central.maven.org/maven2/").build();
    RemoteRepository codehaus = new RemoteRepository.Builder("codehaus", "default", "http://dist.codehaus.org/").build();
    RemoteRepository ibiblio = new RemoteRepository.Builder("ibiblio", "default", "http://www.ibiblio.org/maven/").build();

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
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

    LocalRepository localRepo = new LocalRepository(localDownloadDir);
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

    return session;
  }

  /**
   * Set up repository system for maven.
   * 
   * @return the {@link RepositorySystemHelper}
   */
  private RepositorySystem newRepositorySystem() {
    DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();

    locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
    locator.addService(TransporterFactory.class, FileTransporterFactory.class);
    locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

    locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
      @Override
      public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
        exception.printStackTrace();
      }
    });

    return locator.getService(RepositorySystem.class);
  }
}
