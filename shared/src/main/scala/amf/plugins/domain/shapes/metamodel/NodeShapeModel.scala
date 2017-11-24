package amf.plugins.domain.shapes.metamodel

import amf.framework.metamodel.Field
import amf.framework.metamodel.Type.{Array, Bool, Int, Str}
import amf.framework.metamodel.domain.DomainElementModel
import amf.plugins.domain.shapes.models.NodeShape
import amf.vocabulary.Namespace.{Shacl, Shapes}
import amf.vocabulary.ValueType

/**
  * Node shape metamodel.
  */
object NodeShapeModel extends ShapeModel with DomainElementModel {

  val MinProperties = Field(Int, Shapes + "minProperties")

  val MaxProperties = Field(Int, Shapes + "maxProperties")

  val Closed = Field(Bool, Shacl + "closed")

  val Discriminator = Field(Str, Shapes + "discriminator")

  val DiscriminatorValue = Field(Str, Shapes + "discriminatorValue")

  val ReadOnly = Field(Bool, Shapes + "readOnly")

  val Properties = Field(Array(PropertyShapeModel), Shacl + "property")

  val Dependencies = Field(Array(PropertyDependenciesModel), Shapes + "dependencies")

  override def fields: List[Field] =
    List(MinProperties,
         MaxProperties,
         Closed,
         Discriminator,
         DiscriminatorValue,
         ReadOnly,
         Properties,
         Dependencies) ++ ShapeModel.fields ++ DomainElementModel.fields

  override val `type`: List[ValueType] = List(Shacl + "NodeShape") ++ ShapeModel.`type` ++ DomainElementModel.`type`

  override def modelInstance = NodeShape()
}
