package amf.plugins.domain.webapi.metamodel.bindings

import amf.core.metamodel.Field
import amf.core.metamodel.Type.Str
import amf.core.metamodel.domain.{ModelDoc, ModelVocabularies, ShapeModel}
import amf.core.model.domain.AmfObject
import amf.core.vocabulary.Namespace.ApiBinding
import amf.core.vocabulary.ValueType
import amf.plugins.domain.webapi.models.bindings.kafka.{KafkaMessageBinding, KafkaOperationBinding}

object KafkaOperationBindingModel extends OperationBindingModel with BindingVersion {
  val GroupId =
    Field(ShapeModel,
          ApiBinding + "groupId",
          ModelDoc(ModelVocabularies.ApiBinding, "groupId", "Schema that defines the id of the consumer group"))

  val ClientId =
    Field(
      ShapeModel,
      ApiBinding + "clientId",
      ModelDoc(ModelVocabularies.ApiBinding,
               "clientId",
               "Schema that defines the id of the consumer inside a consumer group")
    )

  override def modelInstance: AmfObject = KafkaOperationBinding()

  override def fields: List[Field] = List(GroupId, ClientId, BindingVersion) ++ OperationBindingModel.fields

  override val key: Field = Type

  override val `type`: List[ValueType] = ApiBinding + "KafkaOperationBinding" :: OperationBindingModel.`type`

  override val doc: ModelDoc = ModelDoc(
    ModelVocabularies.ApiBinding,
    "KafkaOperationBinding",
    ""
  )
}

object KafkaMessageBindingModel extends OperationBindingModel with BindingVersion {
  val MessageKey =
    Field(ShapeModel,
          ApiBinding + "messageKey",
          ModelDoc(ModelVocabularies.ApiBinding, "key", "Schema that defines the message key"))

  override def modelInstance: AmfObject = KafkaMessageBinding()

  override def fields: List[Field] = List(MessageKey, BindingVersion) ++ MessageBindingModel.fields

  override val `type`: List[ValueType] = ApiBinding + "KafkaMessageBinding" :: MessageBindingModel.`type`

  override val key: Field = Type

  override val doc: ModelDoc = ModelDoc(
    ModelVocabularies.ApiBinding,
    "KafkaMessageBinding",
    ""
  )
}
