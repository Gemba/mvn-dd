package com.github.gemba.artifactresolver;

/*******************************************************************************
 * Copyright (c) 2013 by Gemba
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution (see COPYING), and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

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
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.eclipse.aether.collection.DependencySelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for resolving dependencies with a set of remote repositories.
 * 
 * @author Gemba
 */
public class RepositorySystemHelper {

  private RepositorySystem repoSystem;
  private RepositorySystemSession session;
  private CollectRequest collectRequest;

  private static final Logger log = LoggerFactory.getLogger(RepositorySystemHelper.class);
  
  /**
   * Initalizes the aether environment. Uses these repositories:
   * <ul>
   * <li>http://central.maven.org/maven2/
   * </ul>
   * 
   * @param localRepoDir
   *          path where to put the downloaded dependencies
   * @param extraRepos
   *          map with extra repositories <id, url>.
   */
  public RepositorySystemHelper(String localRepoDir, Map<String, String> extraRepos, boolean withoutProvided) {
    repoSystem = newRepositorySystem();

    session = newSession(repoSystem, localRepoDir, withoutProvided);

    RemoteRepository central = new RemoteRepository.Builder("central", "default", "http://central.maven.org/maven2/").build();

    collectRequest = new CollectRequest();
    collectRequest.addRepository(central);

    RemoteRepository repo = null;
    Iterator<Entry<String, String>> iterator = extraRepos.entrySet().iterator();
    while (iterator.hasNext()) {
      Entry<String, String> e = iterator.next();
      repo = new RemoteRepository.Builder(e.getKey(), "default", e.getValue()).build();
      collectRequest.addRepository(repo);
      log.debug("Using extra repository '{}': {}", e.getKey(), e.getValue());
    }

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
  private RepositorySystemSession newSession(RepositorySystem system, final String localDownloadDir, boolean withoutProvided) {
    DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

    LocalRepository localRepo = new LocalRepository(localDownloadDir);
    session.setLocalRepositoryManager(system.newLocalRepositoryManager(session, localRepo));

    if (!withoutProvided) {
	    DependencySelector depFilter =
	        new AndDependencySelector(
	        new ScopeDependencySelector(JavaScopes.PROVIDED),
	        new OptionalDependencySelector(),
	        new ExclusionDependencySelector()
	    );
	    session.setDependencySelector(depFilter);
    }
    
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
