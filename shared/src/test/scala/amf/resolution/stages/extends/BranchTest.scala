package amf.resolution.stages.`extends`

import amf.framework.model.domain.templates.Variable
import amf.plugins.document.webapi.resolution.stages.{Branch, BranchContainer, Key}
import org.scalatest.FunSuite
import org.scalatest.Matchers._

class BranchTest extends FunSuite {

  test("Test branch container flatten and merge") {

    val y_a = branch("y", Seq("param" -> "alpha"))
    val x   = branch("x")
    val a   = parent("a", Seq(x, y_a))

    val y_b = branch("y", Seq("param" -> "beta"))
    val z   = branch("z")
    val p   = branch("p")
    val b   = parent("b", Seq(z, p, y_b))

    val w = branch("w")
    val h = branch("h")
    val c = parent("c", Seq(z, w, h, a))

    val alphaContainer = container(a, b, c).flatten()
    alphaContainer should contain theSameElementsInOrderAs List(a, b, c, x, y_a, z, p, y_b, w, h)

    val d = parent("d", Seq(x, y_a))

    val n = branch("n")
    val e = parent("e", Seq(z, w, n))

    val betaContainer = container(d, c, e).flatten()
    betaContainer should contain theSameElementsInOrderAs List(d, c, e, x, y_a, z, w, h, a, n)

    val merge = BranchContainer.merge(alphaContainer, betaContainer)

    merge should contain theSameElementsInOrderAs List(a, b, c, x, y_a, z, p, y_b, w, h, d, e, n)
  }

  private def container(branches: Branch*) = BranchContainer(branches.toSeq)

  private def branch(name: String) = MockBranch(Key(name, Set[Variable]()), Seq())

  private def parent(name: String, children: Seq[Branch]) = MockBranch(Key(name, Set[Variable]()), children)

  private def branch(name: String, variables: Seq[(String, String)]): Branch =
    MockBranch(Key(name, variables.map(Variable.tupled).toSet), Seq())

  case class MockBranch(key: Key, children: Seq[Branch]) extends Branch
}
