package org.sonar.java.model;

import org.junit.ComparisonFailure;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * TODO shared identifiers in enum
 */
public class JParserTest {

  @org.junit.Ignore
  @Test
  public void invalid() {
    // TODO in CompareToResultTestCheckTest
    testExpression("(c++)++");
    // TODO in ForLoopIncrementSignCheckTest
    testExpression("(-i)++");
    // TODO in SystemExitCalledCheckTest
    testExpression("m()++");
  }

  @org.junit.Ignore("causes IndexOutOfBoundsException")
  @Test
  public void err() {
    // ASTNode.METHOD_DECLARATION with flag ASTNode.MALFORMED
    test("interface Foo { public foo(); // comment\n }");
  }

  @Test()
  public void err2() {
    // TODO without check for syntax errors will cause IndexOutOfBoundsException
    try {
      JParser.parse("class C");
      fail("exception expected");
    } catch (UnsupportedOperationException e) {
      assertEquals("line 1: Syntax error, insert \"ClassBody\" to complete CompilationUnit", e.getMessage());
    }
  }

  @Test
  public void wip() {
    test("class C { void m(String... s) { m(new String[] {}); /* comment */ m(new String[] {}); } }");
    test("abstract class C { abstract int method(); }");
    test("class C { int f; }");
  }

  /**
   * @see org.eclipse.jdt.core.dom.InfixExpression#extendedOperands()
   */
  @Test
  public void extended_operands() {
    test("class C { void m() { m( 1 - 2 - 3 ); } }");

    // TODO no extendedOperands in case of parenthesises ?
    test("class C { void m() { m( (1 - 2) - 3 ); } }");
    test("class C { void m() { m( 1 - (2 - 3) ); } }");
  }

  /**
   * @see org.eclipse.jdt.core.dom.MethodDeclaration#extraDimensions()
   * @see org.eclipse.jdt.core.dom.VariableDeclarationFragment#extraDimensions()
   * @see org.eclipse.jdt.core.dom.SingleVariableDeclaration#extraDimensions()
   */
  @Test
  public void extra_dimensions() {
    test("interface I { int m()[]; }");
    test("interface I { int m(int p[]); }");
    test("interface I { int f1[], f2[][]; }");
  }

  /**
   * @see Tree.Kind#VAR_TYPE
   */
  @Test
  public void type_var() {
    test("class C { void m() { var i = 42; } }");
  }

  @Test
  public void empty_declarations() {
    // after each import declaration
    test("import i; ;");

    try {
      test("import a; ; import b;");
      fail("exception expected");
    } catch (UnsupportedOperationException e) {
      // TODO syntax tree is actually correct even in presence of syntax error
      assertEquals("line 1: Syntax error on token \";\", delete this token", e.getMessage());
    }

    // before first and after each body declaration
    test("class C { ; void m(); ; }");
  }

  /**
   * @see org.eclipse.jdt.core.dom.SingleVariableDeclaration#isVarargs()
   */
  @Test
  public void varargs() {
    test("class I { void m(int... p) { m(1); } }");
  }

  @Test
  public void declaration_enum() {
    test("enum E { C1 , C2 }");
    test("enum E { C1 , C2 ; }");
  }

  @Test
  public void statement_for() {
    test("class C { void m() { for ( int i , j ; ; ) ; } }");
    test("class C { void m() { for ( int i = 0, j = 0 ; ; ) ; } }");
  }

  @Test
  public void statement_switch() {
    test("class C { void m() { switch (0) { case 0: } } }");

    // Java 12
    try {
      test("class C { void m() { switch (0) { case 0, 1: } } }");
      fail("expected exception");
    } catch (ComparisonFailure ignore) {
    }
    try {
      test("class C { void m() { switch (0) { case 0, 1 -> { break; } } } }");
      fail("expected exception");
    } catch (IndexOutOfBoundsException ignore) {
    }
  }

  @Test
  public void statement_try() {
    test("class C { void m() { try (R r1 = open(); r2;) { } } }");
  }

  @Test
  public void expression_literal() {
    testExpression("-2147483648"); // Integer.MIN_VALUE
    testExpression("-9223372036854775808L"); // Long.MIN_VALUE
  }

  @Test
  public void expression_array_creation() {
    testExpression("new int[0]");
    testExpression("new int[0][1]");

    testExpression("new int[][] { { } , { } }");
  }

  @Test
  public void expression_type_method_reference() {
    testExpression("java.util.Map.Entry<String, String>::getClass");
  }

  @Test
  public void expression_super_method_reference() {
    testExpression("C.super::<T, T>m");
  }

  // FIXME
  @Test(expected = IllegalStateException.class)
  public void type_name_qualified() {
    testExpression("new a. @Annotation d()");
  }

  @Test
  public void test_wip() throws IOException {
    String s = new String(Files.readAllBytes(Paths.get("/Users/evgeny.mandrikov/projects/sonarsource/sonar-enterprise/"
      + "server/sonar-server-common/src/main/java/org/sonar/server/es/newindex/KeywordFieldBuilder.java")));
    JParser.parse(s);
  }

  private void testExpression(String expression) {
    test("class C { Object m() { return " + expression + " ; } }");
  }

  private static void test(String source) {
    TreeFormatter formatter = new TreeFormatter();
    formatter.showTokens = true;

    CompilationUnitTree oldTree = (CompilationUnitTree) JavaParser.createParser().parse(source);
    SemanticModel.createFor(oldTree, new SquidClassLoader(Collections.emptyList()));
    String expected = formatter.toString(oldTree);
    System.out.println(expected);

    Tree newTree = JParser.parse(source);
    String actual = formatter.toString(newTree);

    assertEquals(expected, actual);
  }

}
