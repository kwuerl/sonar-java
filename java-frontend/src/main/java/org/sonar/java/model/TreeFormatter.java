/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.model;

import com.google.common.collect.Iterators;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class TreeFormatter {

  // TODO separators
  boolean showTokens = true;

  // FIXME to compare semantic it also should be computed for newTree
  boolean showSemantic = false;

  /**
   * Only for testing.
   */
  @Deprecated
  public static void compare(CompilationUnitTree newTree, CompilationUnitTree oldTree) {
//    SemanticModel.createFor(oldTree, new SquidClassLoader(Collections.emptyList()));

    String actual = new TreeFormatter().toString(newTree);
    String expected = new TreeFormatter().toString(oldTree);

//    System.out.println(expected);

    if (!expected.equals(actual)) {
      try {
        throw (AssertionError) Class.forName("org.junit.ComparisonFailure")
          .getConstructor(String.class, String.class, String.class)
          .newInstance("", expected, actual);
      } catch (ReflectiveOperationException e) {
        throw new RuntimeException(e);
      }
    }
  }

  String toString(Tree node) {
    StringBuilder out = new StringBuilder();
    append(out, 0, node);
    return out.toString();
  }

  private void append(StringBuilder out, int indent, Tree node) {
    for (int i = 0; i < indent; i++) {
      out.append(' ');
    }
    out.append(node.kind());

    if (node.is(Tree.Kind.TRIVIA)) {
      out.append(' ').append(((SyntaxTrivia) node).comment());

    } else if (node.is(Tree.Kind.TOKEN)) {
      out.append(' ').append(((SyntaxToken) node).text());

    } else if (node.is(Tree.Kind.IDENTIFIER)) {
      out.append(" name=").append(((IdentifierTree) node).name());
    }

    if (showSemantic) {
      appendSemantic(out, node);
    }

    out.append('\n');
    indent += 2;

    Iterator<? extends Tree> i = iteratorFor(node);
    while (i.hasNext()) {
      Tree child = i.next();
      if (child.is(Tree.Kind.TOKEN) && !showTokens) {
        continue;
      }
      append(out, indent, child);
    }
  }

  private void appendSemantic(StringBuilder out, Tree node) {
    if (node.is(Tree.Kind.VARIABLE)) {
      VariableTreeImpl n = (VariableTreeImpl) node;
      out.append(" symbol=").append(n.symbol() == null ? "null" : n.symbol().name());
    }

    if (node.is(Tree.Kind.IDENTIFIER)) {
      IdentifierTree n = (IdentifierTree) node;
      out.append(" symbol.name=").append(n.symbol().name());
    }

    if (node.is(Tree.Kind.METHOD_INVOCATION)) {
      MethodInvocationTree n = (MethodInvocationTree) node;
      out.append(" symbol.name=").append(n.symbol().name());
    }
  }

  private static Iterator<? extends Tree> iteratorFor(Tree node) {
    if (node.kind() == Tree.Kind.TOKEN) {
      return ((SyntaxToken) node).trivias().iterator();
    }
    if (node.kind() == Tree.Kind.INFERED_TYPE || node.kind() == Tree.Kind.TRIVIA) {
      // old tree throws exception
      return Collections.emptyIterator();
    }
    final Iterator<Tree> iterator = ((JavaTree) node).getChildren().iterator();
    return Iterators.filter(
      iterator,
      child -> child != null
        && /* not empty list: */ !(child.is(Tree.Kind.LIST) && ((List) child).isEmpty())
//        && /* not comma */ !(child.is(Tree.Kind.TOKEN) && ",".equals(((SyntaxToken) child).text()))
    );
  }

}
