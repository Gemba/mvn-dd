package com.github.artifactresolver;

/*******************************************************************************
 * Copyright (c) 2013 by Gemba
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution (see COPYING), and is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
import java.util.HashMap;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;

/**
 * Writes the dependencies of an artifact as tree to a logger.
 * 
 * @author Gemba
 */
public class DependencyGraphPrinter implements DependencyVisitor {

  private static final Logger log = LoggerFactory.getLogger(DependencyGraphPrinter.class);

  private static final String DEFAULT_INDENT = "|   ";

  private static final String DEFAULT_BRANCH = "+---";
  private static final String DEFAULT_TERMINAL = "\\---";

  private String indent = "";

  // stack for visited nodes
  private Stack<DependencyNode> nodes;

  // map to count visits per indent-level
  private HashMap<Integer, Integer> dependencyCounter;

  /**
   * Default constructor.
   */
  public DependencyGraphPrinter() {
    nodes = new Stack<DependencyNode>();
    dependencyCounter = new HashMap<Integer, Integer>();
  }

  @Override
  public boolean visitEnter(DependencyNode node) {
    int children = node.getChildren().size();

    int parentChildren = 0;
    if (nodes.size() > 0) {
      parentChildren = nodes.peek().getChildren().size();
    }
    nodes.push(node);

    Integer count = dependencyCounter.get(indent.length());
    if (count == null) {
      count = 1;
    } else {
      count++;
    }
    dependencyCounter.put(indent.length(), count);

    String in;
    if (count == parentChildren) {
      in = indent.replace(DEFAULT_BRANCH, DEFAULT_TERMINAL);
    } else {
      in = indent.replace(DEFAULT_TERMINAL, DEFAULT_BRANCH);
    }

    log.info("  {}{}", in, node);

    if (indent.length() == 0) {
      indent = DEFAULT_BRANCH;
    } else {
      if (children == 1 && children == parentChildren) {
        // remove heading "|"
        indent = DEFAULT_INDENT + " " + indent.substring(1);
      } else {
        indent = DEFAULT_INDENT + indent;
      }
    }
    return true;
  }

  @Override
  public boolean visitLeave(DependencyNode node) {
    dependencyCounter.remove(indent.length());
    nodes.pop();
    indent = indent.substring(DEFAULT_INDENT.length());
    return true;
  }
}
