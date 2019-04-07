package amf.plugins.document.vocabularies.emitters.instances

import amf.core.annotations.{Aliases, LexicalInformation}
import amf.core.emitter.BaseEmitters.{ValueEmitter, _}
import amf.core.emitter.SpecOrdering.Lexical
import amf.core.emitter.{EntryEmitter, PartEmitter, SpecOrdering}
import amf.core.metamodel.Field
import amf.core.model.document.{BaseUnit, DeclaresModel}
import amf.core.model.domain._
import amf.core.parser.Position.ZERO
import amf.core.parser.{Annotations, FieldEntry, Position, Value}
import amf.core.utils._
import amf.core.vocabulary.Namespace
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.annotations.{AliasesLocation, CustomId, JsonPointerRef, RefInclude}
import amf.plugins.document.vocabularies.emitters.common.{ExternalEmitter, IdCounter}
import amf.plugins.document.vocabularies.metamodel.domain.DialectDomainElementModel
import amf.plugins.document.vocabularies.model.document._
import amf.plugins.document.vocabularies.model.domain._
import org.mulesoft.common.time.SimpleDateTime
import org.yaml.model.YDocument.{EntryBuilder, PartBuilder}
import org.yaml.model.{YDocument, YNode}

trait DialectEmitterHelper {
  val dialect: Dialect

  def externalEmitters[T <: amf.core.model.domain.AmfObject](model: ExternalContext[T],
                                                             ordering: SpecOrdering): Seq[EntryEmitter] = {
    if (model.externals.nonEmpty) {
      Seq(new EntryEmitter {
        override def emit(b: EntryBuilder): Unit = {
          b.entry("$external", _.obj({ b =>
            traverse(ordering.sorted(model.externals.map(external => ExternalEmitter(external, ordering))), b)
          }))
        }

        override def position(): Position = {
          model.externals
            .map(e => e.annotations.find(classOf[LexicalInformation]).map(_.range.start))
            .filter(_.nonEmpty)
            .map(_.get)
            .sortBy(_.line)
            .headOption
            .getOrElse(ZERO)
        }
      })
    } else {
      Nil
    }
  }

  def findNodeMappingById(nodeMappingId: String): (Dialect, NodeMappable) = {
    maybeFindNodeMappingById(nodeMappingId).getOrElse {
      throw new Exception(s"Cannot find node mapping $nodeMappingId")
    }
  }

  def maybeFindNodeMappingById(nodeMappingId: String): Option[(Dialect, NodeMappable)] = {
    val inDialectMapping = dialect.declares
      .find {
        case nodeMapping: NodeMappable => nodeMapping.id == nodeMappingId
        case _                         => false
      }
      .map { nodeMapping =>
        (dialect, nodeMapping)
      }
      .asInstanceOf[Option[(Dialect, NodeMappable)]]
      .orElse {
        dialect.references
          .collect {
            case lib: DialectLibrary =>
              lib.declares.find(_.id == nodeMappingId)
          }
          .collectFirst {
            case Some(mapping: NodeMappable) =>
              (dialect, mapping)
          }
      }
    inDialectMapping orElse {
      findNodeInRegistry(nodeMappingId)
    }
  }

  def findNodeInRegistry(nodeMappingId: String): Option[(Dialect, NodeMappable)] =
    AMLPlugin.registry.findNode(nodeMappingId)
}

case class ReferencesEmitter(baseUnit: BaseUnit, ordering: SpecOrdering, aliases: Map[String, (String, String)])
    extends EntryEmitter {
  override def emit(b: EntryBuilder): Unit = {
    val modules = baseUnit.references.collect({ case m: DeclaresModel => m })
    if (modules.nonEmpty) {

      b.entry("uses", _.obj { b =>
        traverse(ordering.sorted(modules.map(r => ReferenceEmitter(r, ordering, aliases))), b)
      })
    }
  }

  override def position(): Position =
    baseUnit.annotations.find(classOf[AliasesLocation]).map(annot => Position((annot.position, 0))).getOrElse(ZERO)
}

case class ReferenceEmitter(reference: BaseUnit, ordering: SpecOrdering, aliases: Map[String, (String, String)])
    extends EntryEmitter {

  override def emit(b: EntryBuilder): Unit = {
    aliases.get(reference.id) match {
      case Some((alias, location)) =>
        MapEntryEmitter(alias, location).emit(b)
      case _ => // TODO: emit violation
    }
  }

  override def position(): Position = ZERO
}

case class DiscriminatorHelper(mapping: NodeWithDiscriminator[_], dialectEmitter: DialectEmitterHelper) {
  // maybe we have a discriminator
  val discriminator = Option(mapping.typeDiscriminator()).orElse {
    val rangeId = mapping.objectRange().head.value()
    dialectEmitter.findNodeMappingById(rangeId) match {
      case (_, unionMapping: UnionNodeMapping) => Option(unionMapping.typeDiscriminator())
      case _                                   => None
    }
  }

  // maybe we have a discriminator name
  val discriminatorName = mapping.typeDiscriminatorName().option().orElse {
    val rangeId = mapping.objectRange().head.value()
    dialectEmitter.findNodeMappingById(rangeId) match {
      case (_, unionMapping: UnionNodeMapping) => unionMapping.typeDiscriminatorName().option()
      case _                                   => None
    }
  }

  // we build the discriminator mapping if we have a discriminator
  val discriminatorMappings: Map[String, NodeMapping] =
    discriminator.getOrElse(Map()).foldLeft(Map[String, NodeMapping]()) {
      case (acc, (alias, mappingId)) =>
        dialectEmitter.findNodeMappingById(mappingId) match {
          case (_, nodeMapping: NodeMapping) => acc + (alias -> nodeMapping)
          case _                             => acc // TODO: violation here
        }
    }

  def compute(dialectDomainElement: DialectDomainElement): Option[(String, String)] = {
    val elementTypes = dialectDomainElement.meta.`type`.map(_.iri())
    discriminatorMappings.find {
      case (_, discriminatorMapping) =>
        elementTypes.contains(discriminatorMapping.nodetypeMapping.value()).asInstanceOf[Boolean]
    } match {
      case Some((alias, _)) =>
        Some((discriminatorName.getOrElse("type"), alias))
      case _ =>
        None
    }
  }
}

case class DialectInstancesEmitter(instance: DialectInstance, dialect: Dialect) extends DialectEmitterHelper {
  val ordering: SpecOrdering                 = Lexical
  val aliases: Map[String, (String, String)] = collectAliases()

  def collectAliases(): Map[String, (String, String)] = {
    val vocabFile       = instance.location().getOrElse(instance.id).split("/").last
    val vocabFilePrefix = instance.location().getOrElse(instance.id).replace(vocabFile, "")

    val maps = instance.annotations
      .find(classOf[Aliases])
      .map { aliases =>
        aliases.aliases.foldLeft(Map[String, String]()) {
          case (acc, (alias, (fullUrl, relativeUrl))) =>
            acc + (fullUrl -> alias)
        }
      }
      .getOrElse(Map())
    val idCounter = new IdCounter()
    instance.references.foldLeft(Map[String, (String, String)]()) {
      case (acc: Map[String, (String, String)], m: DeclaresModel) =>
        val location = m.location().getOrElse(m.id).replace("#", "")
        val importLocation: String = if (location.contains(vocabFilePrefix)) {
          location.replace(vocabFilePrefix, "")
        } else {
          location.replace("file://", "")
        }

        if (maps.get(m.id).isDefined) {
          val alias = maps(m.id)
          acc + (m.id -> (alias, importLocation))
        } else {
          val nextAlias = idCounter.genId("uses_")
          acc + (m.id -> (nextAlias, importLocation))
        }
      case (acc: Map[String, (String, String)], _) => acc
    }
  }

  def emitInstance(): YDocument = {

    YDocument(b => {
      b.comment(s"%${dialect.name().value()} ${dialect.version().value()}")
      val (_, rootNodeMapping) = findNodeMappingById(dialect.documents().root().encoded().value())
      DialectNodeEmitter(
        instance.encodes.asInstanceOf[DialectDomainElement],
        rootNodeMapping,
        instance,
        dialect,
        ordering,
        aliases,
        None,
        rootNode = true,
        topLevelEmitters = externalEmitters(instance, ordering)
      ).emit(b)
    })
  }
}

case class DeclarationsGroupEmitter(declared: Seq[DomainElement],
                                    publicNodeMapping: PublicNodeMapping,
                                    nodeMappable: NodeMappable,
                                    instance: DialectInstance,
                                    dialect: Dialect,
                                    ordering: SpecOrdering,
                                    declarationsPath: Seq[String],
                                    aliases: Map[String, (String, String)],
                                    keyPropertyId: Option[String] = None)
    extends EntryEmitter
    with DialectEmitterHelper {

  def computeIdentifier(elem: DomainElement) = {
    elem match {
      case decl: DialectDomainElement =>
        decl.declarationName.option() match {
          case Some(name) => name
          case _ =>
            decl.id
              .split("#")
              .last
              .split("/")
              .last
              .urlDecoded // we are using the last part of the URL as the identifier in dialects
        }
      case decl: DomainElement        =>
        decl.fields.fields().find(_.field.value.iri() == (Namespace.Schema + "name").iri()) match {
          case Some(entry) => entry.value.value.toString
          case _           =>
            decl.id
              .split("#")
              .last
              .split("/")
              .last
              .urlDecoded // we are using the last part of the URL as the identifier in dialects
        }
    }

  }

  override def emit(b: EntryBuilder): Unit = {
    val discriminator = findNodeMappingById(publicNodeMapping.mappedNode().value()) match {
      case (_, unionMapping: UnionNodeMapping) => Some(DiscriminatorHelper(unionMapping, this))
      case _                                   => None
    }

    if (declarationsPath.isEmpty) {
      val declarationKey = publicNodeMapping.name().value()
      b.entry(
        declarationKey,
        _.obj { b =>
          sortedDeclarations().foreach { decl =>
            val identifier = computeIdentifier(decl)
            b.entry(
              YNode(identifier),
              b => {
                decl match {
                  case dialectDomainElement: DialectDomainElement =>
                    val discriminatorProperty = discriminator.flatMap(_.compute(dialectDomainElement))
                    DialectNodeEmitter(dialectDomainElement,
                      nodeMappable,
                      instance,
                      dialect,
                      ordering,
                      aliases,
                      discriminator = discriminatorProperty).emit(b)

                  case pluginDomainElement: DomainElement if nodeMappable.isInstanceOf[PluginNodeMapping] =>
                    PluginNodeEmitter(pluginDomainElement,
                      nodeMappable.asInstanceOf[PluginNodeMapping],
                      dialect).emit(b)
                }
              }
            )
          }
        }
      )
    } else {
      b.entry(
        declarationsPath.head,
        _.obj { b =>
          DeclarationsGroupEmitter(declared,
                                   publicNodeMapping,
                                   nodeMappable,
                                   instance,
                                   dialect,
                                   ordering,
                                   declarationsPath.tail,
                                   aliases,
                                   keyPropertyId).emit(b)
        }
      )
    }
  }

  override def position(): Position =
    declared
      .flatMap(_.annotations.find(classOf[LexicalInformation]).map { lexInfo =>
        lexInfo.range.start
      })
      .sorted
      .headOption
      .getOrElse(ZERO)

  def sortedDeclarations(): Seq[DomainElement] = {
    declared.sortBy(
      _.annotations
        .find(classOf[LexicalInformation])
        .map { lexInfo =>
          lexInfo.range.start
        }
        .getOrElse(ZERO))
  }
}

case class PluginNodeEmitter(node: DomainElement,
                             pluginNode: PluginNodeMapping,
                             dialect: Dialect)
  extends PartEmitter
  with DialectEmitterHelper {

  override def emit(b: PartBuilder): Unit =
    pluginNode.emit(b, node)

  override def position(): Position =
    node.annotations.find(classOf[LexicalInformation]).map(_.range.start).getOrElse(ZERO)
}
case class DialectNodeEmitter(node: DialectDomainElement,
                              nodeMappable: NodeMappable,
                              instance: DialectInstance,
                              dialect: Dialect,
                              ordering: SpecOrdering,
                              aliases: Map[String, (String, String)],
                              keyPropertyId: Option[String] = None,
                              rootNode: Boolean = false,
                              discriminatorMappable: Option[NodeWithDiscriminator[Any]] = None,
                              discriminator: Option[(String, String)] = None,
                              emitDialect: Boolean = false,
                              topLevelEmitters: Seq[EntryEmitter] = Nil)
    extends PartEmitter
    with DialectEmitterHelper {

  def isLink(node: DomainElement): Boolean = {
    node match {
      case linkable: Linkable => linkable.isLink
      case _                  => false
    }
  }
  override def emit(b: PartBuilder): Unit = {
    if (isLink(node)) {
      if (isFragment(node, instance)) emitLink(node).emit(b)
      else if (isLibrary(node, instance)) {
        emitLibrarRef(node, instance, b)
      } else {
        emitRef(node, b)
      }
    } else {
      var emitters: Seq[EntryEmitter] = topLevelEmitters
      if (emitDialect) {
        emitters ++= Seq(MapEntryEmitter("$dialect", nodeMappable.id))
      }

      if (discriminator.isDefined) {
        val (discriminatorName, discriminatorValue) = discriminator.get
        emitters ++= Seq(MapEntryEmitter(discriminatorName, discriminatorValue))
      }
      if (node.annotations.find(classOf[CustomId]).isDefined) {
        val customId = if (node.id.contains(dialect.location().getOrElse(""))) {
          node.id.replace(dialect.id, "")
        } else {
          node.id
        }
        emitters ++= Seq(MapEntryEmitter("$id", customId))
      }
      node.meta.fields.foreach { field =>
        findPropertyMapping(field) foreach { propertyMapping =>
          if (keyPropertyId.isEmpty || propertyMapping.nodePropertyMapping().value() != keyPropertyId.get) {

            val key                    = propertyMapping.name().value()
            val propertyClassification = propertyMapping.classification()

            val nextEmitter: Seq[EntryEmitter] = node.valueForField(field) match {
              case Some(entry) if entry.value.isInstanceOf[AmfScalar] =>
                val scalar = entry.value.asInstanceOf[AmfScalar]
                emitScalar(key, field, scalar, Some(entry.annotations))

              case Some(entry)
                  if entry.value.isInstanceOf[AmfArray] && propertyClassification == LiteralPropertyCollection =>
                val array = entry.value.asInstanceOf[AmfArray]
                emitScalarArray(key, field, array, Some(entry.annotations))

              case Some(entry)
                  if entry.value
                    .isInstanceOf[DialectDomainElement] && propertyClassification == ExtensionPointProperty =>
                val element = entry.value.asInstanceOf[DialectDomainElement]
                emitExternalObject(key, element, propertyMapping)

              case Some(entry)
                  if entry.value
                    .isInstanceOf[DomainElement] && propertyClassification == ObjectProperty && !propertyMapping.isUnion =>
                val element = entry.value.asInstanceOf[DomainElement]
                emitObjectEntry(key, element, propertyMapping, Some(entry.annotations))

              case Some(entry)
                  if entry.value
                    .isInstanceOf[AmfArray] && propertyClassification == ObjectPropertyCollection && !propertyMapping.isUnion =>
                val array = entry.value.asInstanceOf[AmfArray]
                emitObjectEntry(key, array, propertyMapping, Some(entry.annotations))

              case Some(entry) if entry.value.isInstanceOf[AmfArray] && propertyClassification == ObjectMapProperty =>
                val array = entry.value.asInstanceOf[AmfArray]
                emitObjectEntry(key, array, propertyMapping, Some(entry.annotations))

              case Some(entry)
                  if entry.value
                    .isInstanceOf[DomainElement] && propertyClassification == ObjectProperty && propertyMapping.isUnion =>
                val element = entry.value.asInstanceOf[DomainElement]
                emitObjectEntry(key, element, propertyMapping)

              case Some(entry)
                  if entry.value
                    .isInstanceOf[AmfArray] && propertyClassification == ObjectPropertyCollection && propertyMapping.isUnion =>
                val array = entry.value.asInstanceOf[AmfArray]
                emitObjectEntry(key, array, propertyMapping, Some(entry.annotations))

              case Some(entry) if entry.value.isInstanceOf[AmfArray] && propertyClassification == ObjectPairProperty =>
                val array = entry.value.asInstanceOf[AmfArray]
                emitObjectPairs(key, array, propertyMapping, Some(entry.annotations))
              case None => Nil // ignore
            }
            emitters ++= nextEmitter
          }
        }
      }

      // in case this is the root dialect node, we look for declarations
      if (rootNode)
        emitters ++= declarationsEmitters(b)

      // and also for use of libraries
      if (rootNode)
        emitters ++= Seq(ReferencesEmitter(instance, ordering, aliases))

      // finally emit the object
      b.obj { b =>
        ordering.sorted(emitters).foreach(_.emit(b))
      }
    }
  }

  override def position(): Position =
    node.annotations.find(classOf[LexicalInformation]).map(_.range.start).getOrElse(ZERO)

  protected def emitLink(node: DialectDomainElement): PartEmitter = new PartEmitter {
    override def emit(b: PartBuilder): Unit = {
      if (node.annotations.contains(classOf[RefInclude])) {
        b.obj { m =>
          m.entry("$include", node.includeName)
        }
      } else if (node.annotations.contains(classOf[JsonPointerRef])) {
        b.obj { m =>
          m.entry("$ref", node.linkLabel.option().getOrElse(node.linkTarget.get.id))
        }
      } else {
        b += YNode.include(node.includeName)
      }
    }

    override def position(): Position =
      node.annotations.find(classOf[LexicalInformation]).map(_.range.start).getOrElse(ZERO)
  }

  protected def emitRef(node: DialectDomainElement, b: PartBuilder): Unit = {
    if (node.annotations.contains(classOf[JsonPointerRef])) {
      b.obj { m =>
        m.entry("$ref", node.linkLabel.option().getOrElse(node.linkTarget.get.id))
      }
    } else {
      TextScalarEmitter(node.localRefName, node.annotations).emit(b)
    }
  }

  protected def emitScalar(key: String,
                           field: Field,
                           scalar: AmfScalar,
                           annotations: Option[Annotations] = None): Seq[EntryEmitter] = {
    val formatted = scalar.value match {
      case date: SimpleDateTime => date.rfc3339
      case other                => other
    }

    Seq(ValueEmitter(key, FieldEntry(field, Value(AmfScalar(formatted), annotations.getOrElse(scalar.annotations)))))
  }

  protected def emitScalarArray(key: String,
                                field: Field,
                                array: AmfArray,
                                annotations: Option[Annotations]): Seq[EntryEmitter] =
    Seq(ArrayEmitter(key, FieldEntry(field, Value(array, annotations.getOrElse(array.annotations))), ordering))

  protected def findAllNodeMappings(mappableId: String): Seq[NodeMappable] = {
    findNodeMappingById(mappableId) match {
      case (_, nodeMapping: NodeMapping)             =>
        Seq(nodeMapping)
      case (_, unionMapping: UnionNodeMapping)       =>
        val mappables = unionMapping.objectRange() map { rangeId =>
          findNodeMappingById(rangeId.value())._2
        }
        mappables.collect { case nodeMapping: NodeMapping => nodeMapping }
      case (_, pluginNodeMapping: PluginNodeMapping) =>
        Seq(pluginNodeMapping)
      case _                                         =>
        Nil
    }
  }

  protected def emitObjectEntry(key: String,
                                target: AmfElement,
                                propertyMapping: PropertyMapping,
                                annotations: Option[Annotations] = None): Seq[EntryEmitter] = {
    // lets first extract the target values to emit, always as an array
    val elements: Seq[DomainElement] = target match {
      case amfArray: AmfArray if amfArray.values.forall(_.isInstanceOf[DomainElement]) =>
        amfArray.values.asInstanceOf[Seq[DomainElement]]
      case dialectDomainElement: DomainElement =>
        Seq(dialectDomainElement)
    }

    val isArray                            = target.isInstanceOf[AmfArray]
    val discriminator: DiscriminatorHelper = DiscriminatorHelper(propertyMapping, this)

    Seq(new EntryEmitter {
      // this can be multiple mappings if we have a union in the range or a range pointing to a union mapping
      val nodeMappings = propertyMapping.objectRange().flatMap { rangeNodeMapping =>
        findAllNodeMappings(rangeNodeMapping.value())
      }

      // val key property id, so we can pass it to the nested emitter and it is not emitted
      val keyPropertyId = propertyMapping.mapKeyProperty().option()

      override def emit(b: EntryBuilder): Unit = {
        // collect the emitters for each element, based on the available mappings
        val mappedElements: Map[PartEmitter, DomainElement] =
          elements.foldLeft(Map[PartEmitter, DomainElement]()) {
            case (acc, dialectDomainElement: DialectDomainElement) =>
              // This is a case for a normal node, it is a DialectDomainElement
              // Let's see if this element has a discriminator to add
              nodeMappings.find(nodeMapping =>
                dialectDomainElement.meta.`type`.map(_.iri()).contains(nodeMapping.asInstanceOf[NodeMapping].nodetypeMapping.value())) match {
                case Some(nextNodeMapping) =>
                  val nodeEmitter = DialectNodeEmitter(
                    dialectDomainElement,
                    nextNodeMapping,
                    instance,
                    dialect,
                    ordering,
                    aliases,
                    discriminator = discriminator.compute(dialectDomainElement),
                    keyPropertyId = keyPropertyId
                  )
                  acc + (nodeEmitter -> dialectDomainElement)
                case _ =>
                  acc // TODO: raise violation
              }
            case (acc, domainElement) =>
              // This is an special case for plugin node mappings
              // They produce domain elements, not DialectDomaineElements
              if (nodeMappings.forall(_.isInstanceOf[PluginNodeMapping])) {
                val pluginNodeMapping = nodeMappings.head.asInstanceOf[PluginNodeMapping]
                val nodeEmitter = PluginNodeEmitter(domainElement, pluginNodeMapping, dialect)
                acc + (nodeEmitter -> domainElement)
              } else {
                acc // TODO: raise violation
              }
          }

        if (keyPropertyId.isDefined) {
          // emit map of nested objects by property
          emitMap(b, mappedElements)
        } else if (isArray) {
          // arrays of objects
          emitArray(b, mappedElements)
        } else {
          // single object
          emitSingleElement(b, mappedElements)
        }
      }

      def emitMap(b: EntryBuilder, mapElements: Map[PartEmitter, DomainElement]): Unit = {
        b.entry(
          key,
          _.obj { b =>
            ordering.sorted(mapElements.keys.toSeq).foreach { emitter =>
              mapElements(emitter) match {
                case dialectDomainElement: DialectDomainElement =>
                  val mapKeyField =
                    dialectDomainElement.meta.fields.find(_.value.iri() == propertyMapping.mapKeyProperty().value()).get
                  val mapKeyValue = dialectDomainElement.valueForField(mapKeyField).get.toString
                  EntryPartEmitter(mapKeyValue, emitter).emit(b)
                case _ =>
                  // ignore
              }
            }
          }
        )
      }

      def emitArray(b: EntryBuilder, mappedElements: Map[PartEmitter, DomainElement]): Unit = {
        b.entry(key, _.list { b =>
          ordering.sorted(mappedElements.keys.toSeq).foreach(_.emit(b))
        })
      }

      def emitSingleElement(b: EntryBuilder, mappedElements: Map[PartEmitter, DomainElement]): Unit = {
        mappedElements.keys.headOption.foreach { emitter =>
          EntryPartEmitter(key, emitter).emit(b)
        }
      }

      override def position(): Position = {
        annotations.getOrElse(target.annotations).find(classOf[LexicalInformation]).map(_.range.start).getOrElse(ZERO)
      }

    })
  }

  protected def emitExternalObject(key: String,
                                   element: DialectDomainElement,
                                   propertyMapping: PropertyMapping): Seq[EntryEmitter] = {
    val (externalDialect, nextNodeMapping) = findNodeMappingById(element.definedBy.id)
    Seq(EntryPartEmitter(
      key,
      DialectNodeEmitter(element, nextNodeMapping, instance, externalDialect, ordering, aliases, emitDialect = true)))
  }

  protected def emitObjectPairs(key: String,
                                array: AmfArray,
                                propertyMapping: PropertyMapping,
                                annotations: Option[Annotations] = None): Seq[EntryEmitter] = {
    val keyProperty   = propertyMapping.mapKeyProperty().value()
    val valueProperty = propertyMapping.mapValueProperty().value()

    Seq(new EntryEmitter() {
      override def emit(b: EntryBuilder): Unit = {
        b.entry(
          key,
          _.obj {
            b =>
              val sortedElements = array.values.sortBy { elem =>
                elem.annotations.find(classOf[LexicalInformation]).map(_.range.start).getOrElse(ZERO)
              }
              sortedElements.foreach {
                case element: DialectDomainElement =>
                  val keyField   = element.meta.fields.find(_.value.iri() == keyProperty)
                  val valueField = element.meta.fields.find(_.value.iri() == valueProperty)
                  if (keyField.isDefined && valueField.isDefined) {
                    val keyLiteral   = element.valueForField(keyField.get).map(_.value)
                    val valueLiteral = element.valueForField(valueField.get).map(_.value)
                    (keyLiteral, valueLiteral) match {
                      case (Some(keyScalar: AmfScalar), Some(valueScalar: AmfScalar)) =>
                        MapEntryEmitter(keyScalar.value.toString, valueScalar.value.toString).emit(b)
                      case _ =>
                        throw new Exception("Cannot generate object pair without scalar values for key and value")
                    }
                  } else {
                    throw new Exception("Cannot generate object pair with undefined key or value")
                  }
                case _ => // ignore
              }
          }
        )
      }

      override def position(): Position =
        annotations.getOrElse(array.annotations).find(classOf[LexicalInformation]).map(_.range.start).getOrElse(ZERO)
    })
  }

  def isFragment(elem: DomainElement, instance: DialectInstance): Boolean = {
    elem match {
      case linkable: Linkable =>
        linkable.linkTarget match {
          case Some(domainElement) =>
            instance.references.exists {
              case ref: DialectInstanceFragment => ref.encodes.id == domainElement.id
              case _                            => false
            }
          case _ => throw new Exception(s"Cannot check fragment for an element without target for element ${elem.id}")
        }
      case _                  =>
        false
    }
  }

  def isLibrary(elem: DialectDomainElement, instance: DialectInstance): Boolean = {
    instance.references.exists {
      case lib: DeclaresModel =>
        lib.declares.exists(_.id == elem.linkTarget.get.id)
      case _ => false
    }
  }

  def emitLibrarRef(elem: DialectDomainElement, instance: DialectInstance, b: PartBuilder): Unit = {
    if (elem.annotations.contains(classOf[JsonPointerRef])) {
      b.obj { m =>
        m.entry("$ref", node.linkLabel.option().getOrElse(node.linkTarget.get.id))
      }
    } else {
      val lib = instance.references.find {
        case lib: DeclaresModel =>
          lib.declares.exists(_.id == elem.linkTarget.get.id)
        case _ => false
      }
      val alias = aliases(lib.get.id)._1
      TextScalarEmitter(s"$alias.${elem.localRefName}", elem.annotations).emit(b)
    }
  }

  def declarationsEmitters(b: PartBuilder): Seq[EntryEmitter] = {
    val emitters = for {
      docs <- Option(dialect.documents())
      root <- Option(docs.root())
    } yield {
      if (root.encoded().value() == node.id) {
        Nil
      } else {
        root.declaredNodes().foldLeft(Seq[EntryEmitter]()) {
          case (acc, publicNodeMapping) =>
            val publicMappings = findAllNodeMappings(publicNodeMapping.mappedNode().value()).map(_.id).toSet
            // these are regular dialect domain elements
            val declaredDialectElements = instance.declares.collect {
              case elem: DialectDomainElement if publicMappings.contains(elem.definedBy.id) => elem
            }
            // these are potential domain elements parsed by a plugin
            val domainElements = instance.declares.collect {
              case elem: Linkable if !elem.isInstanceOf[DialectDomainElement] => elem
            }
            // Let's try to emit the dialect domain elements
            if (declaredDialectElements.nonEmpty) {
              findNodeMappingById(publicNodeMapping.mappedNode().value()) match {
                case (_, nodeMappable: NodeMapping) =>
                  acc ++ Seq(
                    DeclarationsGroupEmitter(declaredDialectElements,
                                             publicNodeMapping,
                                             nodeMappable,
                                             instance,
                                             dialect,
                                             ordering,
                                             docs.declarationsPath().option().getOrElse("/").split("/"),
                                             aliases))
                case _                              =>
                  acc
              }
            } else if(domainElements.nonEmpty) {
              // and now we try to emit domain elements from plugins
              findNodeMappingById(publicNodeMapping.mappedNode().value()) match {
                case (_, pluginNodeMapping: PluginNodeMapping) =>
                  val plugins = pluginNodeMapping.findSyntaxPlugin
                  // we need to filter, otherwise we could emit domain elements generated by a different plugin
                  val elementsForPlugin: Seq[DomainElement] = domainElements.filter { elem =>
                    plugins.exists(_.modelEntities.exists(e => e == elem.meta))
                  }
                  acc ++ Seq(
                    DeclarationsGroupEmitter(
                      elementsForPlugin,
                      publicNodeMapping,
                      pluginNodeMapping,
                      instance,
                      dialect,
                      ordering,
                      docs.declarationsPath().option().getOrElse("/").split("/"),
                      aliases))
                case _                              =>
                  acc
              }
            } else acc
        }
      }
    }
    emitters.getOrElse(Nil)
  }

  protected def findPropertyMapping(field: Field): Option[PropertyMapping] = {
    val iri = field.value.iri()
    nodeMappable match {
      case nodeMapping: NodeMapping => nodeMapping.propertiesMapping().find(_.nodePropertyMapping().value() == iri)
      case unionMapping: UnionNodeMapping =>
        val rangeIds: Seq[String] = unionMapping.objectRange().map(_.value())
        val nodeMappingsInRange = rangeIds.map { id: String =>
          findNodeMappingById(id) match {
            case (_, nodeMapping: NodeMapping) => Some(nodeMapping)
            case _                             => None
          }
        } collect { case Some(nodeMapping: NodeMapping) => nodeMapping }
        nodeMappingsInRange.flatMap(_.propertiesMapping()).find(_.nodePropertyMapping().value() == iri)
    }
  }

}
