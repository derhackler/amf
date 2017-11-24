package amf.model

// import amf.model.DomainExtension

import amf.framework.metamodel
import amf.framework.model.domain
import amf.framework.model.domain.templates
import amf.framework.parser.Range
import amf.plugins.domain.shapes
import amf.plugins.domain.webapi.models
import amf.plugins.domain.webapi.models.{ExternalDomainElement, extensions}

import scala.scalajs.js
import scala.scalajs.js.JSConverters._

/**
  * Domain element.
  */
trait DomainElement {
  private[amf] def element: domain.DomainElement

  lazy val customDomainProperties: js.Iterable[DomainExtension] =
    element.customDomainProperties.map(DomainExtension).toJSArray
  lazy val `extends`: js.Iterable[DomainElement] = element.extend.map {
    case pd: templates.ParametrizedDeclaration => ParametrizedDeclaration(pd)
    case op: models.Operation                          => Operation(op)
    case e: models.EndPoint                            => EndPoint(e)
  }.toJSArray

  def withCustomDomainProperties(customProperties: js.Iterable[DomainExtension]): this.type = {
    element.withCustomDomainProperties(customProperties.map(_.domainExtension).toSeq)
    this
  }

  def withExtends(extend: js.Iterable[ParametrizedDeclaration]): this.type = {
    element.withExtends(extend.map(_.element).toSeq)
    this
  }

  def withResourceType(name: String): ParametrizedResourceType =
    ParametrizedResourceType(element.withResourceType(name))

  def withTrait(name: String): ParametrizedTrait = ParametrizedTrait(element.withTrait(name))

  def position(): Range = element.position() match {
    case Some(pos) => pos
    case _         => null
  }

  // API for direct property manipulation

  def getId(): String = element.id

  def getTypeIds(): js.Iterable[String] = element.getTypeIds().toJSArray

  def getPropertyIds(): js.Iterable[String] = element.getPropertyIds().toJSArray

  def getScalarByPropertyId(propertyId: String): js.Iterable[Object] =
    element.getScalarByPropertyId(propertyId).map(_.asInstanceOf[Object]).toJSArray

  def getObjectByPropertyId(propertyId: String): js.Iterable[DomainElement] =
    element.getObjectByPropertyId(propertyId).map(d => DomainElement(d)).toJSArray
}

// TODO: Make this modular
object DomainElement {
  def apply(domainElement: domain.DomainElement): DomainElement = domainElement match {
    case o: models.WebApi                              => WebApi(o)
    case o: models.Operation                           => Operation(o)
    case o: models.Organization                        => Organization(o)
    case o: ExternalDomainElement               => throw new Exception("Not supported yet")
    case o: models.Parameter                           => Parameter(o)
    case o: models.Payload                             => Payload(o)
    case o: models.CreativeWork                            => CreativeWork(o)
    case o: models.EndPoint                            => EndPoint(o)
    case o: models.Request                             => Request(o)
    case o: models.Response                            => Response(o)
    case o: amf.plugins.domain.webapi.models.security.ParametrizedSecurityScheme => ParametrizedSecurityScheme(o)
    case o: amf.plugins.domain.webapi.models.security.SecurityScheme             => SecurityScheme(o)
    case o: domain.ObjectNode               => ObjectNode(o)
    case o: domain.ScalarNode               => ScalarNode(o)
    case o: models.CustomDomainProperty     => CustomDomainProperty(o)
    case o: domain.ArrayNode                => ArrayNode(o)
    case o: extensions.DomainExtension          => DomainExtension(o)
    case o: shapes.models.Shape                                => Shape(o)
    case o: amf.domain.dialects.DomainEntity               => DomainEntity(o)
    case o =>
      new DomainElement {
        override private[amf] def element = o
      }
  }
}

trait Linkable { this: DomainElement with Linkable =>

  private[amf] def element: domain.DomainElement with domain.Linkable

  def linkTarget: Option[DomainElement with Linkable]

  def isLink: Boolean           = linkTarget.isDefined
  def linkLabel: Option[String] = element.linkLabel

  def linkCopy(): DomainElement with Linkable

  def withLinkTarget(target: DomainElement with Linkable): this.type = {
    element.withLinkTarget(target.element)
    this
  }

  def withLinkLabel(label: String): this.type = {
    element.withLinkLabel(label)
    this
  }

  def link[T](label: Option[String] = None): T = {
    val href = linkCopy()
    href.withLinkTarget(this)
    label.map(href.withLinkLabel)

    href.asInstanceOf[T]
  }
}
