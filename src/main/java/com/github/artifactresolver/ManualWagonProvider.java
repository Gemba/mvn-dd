package com.github.artifactresolver;

/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagon;
import org.apache.maven.wagon.providers.http.LightweightHttpWagonAuthenticator;
import org.sonatype.aether.connector.wagon.WagonProvider;

/**
 * A simplistic provider for wagon instances when no Plexus-compatible IoC container is used. Derived and slightly adapted from
 * {@link https://github.com/sonatype/sonatype-aether/blob/master/aether-demo/src/main/java/demo/manual/}
 */
public class ManualWagonProvider implements WagonProvider {

  @Override
  public Wagon lookup(String roleHint) throws Exception {
    if ("http".equals(roleHint)) {
      LightweightHttpWagon lightweightHttpWagon = new LightweightHttpWagon();

      // add a default Authenticator to avoid NPE
      LightweightHttpWagonAuthenticator authenticator = new LightweightHttpWagonAuthenticator();
      lightweightHttpWagon.setAuthenticator(authenticator);

      return lightweightHttpWagon;
    }
    return null;
  }

  @Override
  public void release(Wagon wagon) {
    // not implemented
  }

}
