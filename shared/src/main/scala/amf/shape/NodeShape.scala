package amf.shape

import amf.common.AMFAST
import amf.domain.{Annotations, Fields}
import amf.metadata.shape.NodeShapeModel._

/**
  * Node shape
  */
case class NodeShape(fields: Fields, annotations: Annotations) extends Shape {

  def minProperties: Int             = fields(MinProperties)
  def maxProperties: Int             = fields(MaxProperties)
  def closed: Boolean                = fields(Closed)
  def discriminator: String          = fields(Discriminator)
  def discriminatorValue: String     = fields(DiscriminatorValue)
  def readOnly: Boolean              = fields(ReadOnly)
  def properties: Seq[PropertyShape] = fields(Properties)

  override def adopted(parent: String): this.type = withId(parent + "/" + name)
}

object NodeShape {

  def apply(): NodeShape = apply(Annotations())

  def apply(ast: AMFAST): NodeShape = apply(Annotations(ast))

  def apply(annotations: Annotations): NodeShape = NodeShape(Fields(), annotations)
}
