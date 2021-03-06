package amf.plugins.document.webapi.contexts

import amf.core.model.domain.Shape
import amf.core.remote.Vendor
import amf.core.validation.SeverityLevels
import amf.plugins.document.webapi.contexts.parser.{OasLikeSpecVersionFactory, OasLikeWebApiContext}
import amf.plugins.document.webapi.parser.spec.{CustomSyntax, SpecNode, SpecSyntax}
import org.yaml.model.{YMap, YNode, YPart}

class CustomClosedShapeContextDecorator(decorated: OasLikeWebApiContext, customSyntax: CustomSyntax)
    extends OasLikeWebApiContext(
      decorated.loc,
      decorated.refs,
      decorated.options,
      decorated,
      Some(decorated.declarations)
    ) {

  override val syntax: SpecSyntax = decorated.syntax
  override val vendor: Vendor     = decorated.vendor

  override def link(node: YNode): Either[String, YNode] = decorated.link(node)

  override def autoGeneratedAnnotation(s: Shape): Unit = decorated.autoGeneratedAnnotation(s)

  override val factory: OasLikeSpecVersionFactory = decorated.factory

  override def makeCopy(): OasLikeWebApiContext = decorated.makeCopy()

  override def nextValidation(node: String, shape: String, ast: YMap): Unit = {
    if (customSyntax.contains(shape)) {
      val keys = ast.entries.map(getEntryKey)
      validateCustomSyntax(node, ast, shape, keys)
    } else super.nextValidation(node, shape, ast)
  }

  private def validateCustomSyntax(node: String, ast: YMap, shape: String, keys: Seq[String]): Unit = {
    if (customSyntax.contains(shape)) {
      val SpecNode(requiredFields, possible) = customSyntax(shape)

      // if entries don't contain required fields
      requiredFields.foreach { field =>
        if (!keys.contains(field.name)) {
          val isWarning = field.severity == SeverityLevels.WARNING
          throwClosedShapeError(node, s"Property '${field.name}' is required in a $vendor $shape node", ast, isWarning)
        }
      }

      // if invalid fields are present
      val required = requiredFields.map(_.name)
      keys.foreach(key => {
        if (!possible.contains(key) && !required.contains(key) && !ignore(shape, key)) {
          throwClosedShapeError(node,
                                s"Property '$key' not supported in a $vendor $shape node",
                                getAstEntry(ast, key),
                                isWarning = true)
        }
      })
    }
  }

  private def getAstEntry(ast: YMap, entry: String): YPart =
    ast.entries.find(yMapEntry => yMapEntry.key.asScalar.map(_.text).get == entry).get
}
