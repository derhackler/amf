package amf.plugins.domain.shapes.validation

import amf.core.model.domain.ScalarNode
import amf.plugins.domain.shapes.models.NodeShape
import org.scalatest.{FunSuite, Matchers}

class ShapesNodesValidatorTest extends FunSuite with Matchers {

  test("generation of validation candidates for enums in shape") {
    val shape = NodeShape().withId("id")
    shape.withValues(List(ScalarNode("value 1", None), ScalarNode("value 1", None)))

    val candidates = ShapesNodesValidator.shapeEnumCandidates(shape)

    val candidateShape = candidates.head.shape

    candidates.size shouldBe 2
    // shape that is use for validation should not contain enum values to avoid performance issues
    candidateShape.values shouldBe empty
    // original shape must not be modified, its enum value should still be defined
    shape.values should not be empty

  }
}
