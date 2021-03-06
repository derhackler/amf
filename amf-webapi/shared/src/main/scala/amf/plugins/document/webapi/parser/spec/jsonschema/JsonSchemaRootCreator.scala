package amf.plugins.document.webapi.parser.spec.jsonschema

import amf.core.Root
import amf.core.model.document.{BaseUnit, ExternalFragment, Fragment, RecursiveUnit}
import amf.core.parser.{ParsedReference, Reference, SchemaReference, SyamlParsedDocument}
import amf.core.parser.errorhandler.ParserErrorHandler
import amf.validations.ParserSideValidations.UnableToParseJsonSchema
import org.yaml.model.{YDocument, YMap, YNode}

object JsonSchemaRootCreator {

  def createRootFrom(inputFragment: Fragment, pointer: Option[String], errorHandler: ParserErrorHandler): Root = {
    val encoded: YNode = getYNodeFrom(inputFragment, errorHandler)
    createRoot(inputFragment, pointer, encoded)
  }

  def getYNodeFrom(inputFragment: Fragment, errorHandler: ParserErrorHandler): YNode = {
    inputFragment match {
      case fragment: ExternalFragment                        => fragment.encodes.parsed.getOrElse(parsedFragment(inputFragment, errorHandler))
      case fragment: RecursiveUnit if fragment.raw.isDefined => parsedFragment(inputFragment, errorHandler)
      case _ =>
        errorHandler.violation(UnableToParseJsonSchema,
          inputFragment,
          None,
          "Cannot parse JSON Schema from unit with missing syntax information")
        YNode(YMap(IndexedSeq(), ""))
    }
  }

  private def parsedFragment(inputFragment: Fragment, eh: ParserErrorHandler) =
    JsonYamlParser(inputFragment)(eh).document().node

  private def createRoot(inputFragment: Fragment, pointer: Option[String], encoded: YNode): Root = {
    Root(
      SyamlParsedDocument(YDocument(encoded)),
      buildJsonReference(inputFragment, pointer),
      "application/json",
      toParsedReferences(inputFragment.references),
      SchemaReference,
      inputFragment.raw.getOrElse("")
    )
  }

  private def buildJsonReference(inputFragment: Fragment, pointer: Option[String]) = {
    val url = inputFragment.location().getOrElse(inputFragment.id)
    JsonReference(url, pointer).toString
  }

  private def toParsedReferences(references: Seq[BaseUnit]) = {
    references.map(ref => ParsedReference(ref, Reference(ref.location().getOrElse(""), Nil), None))
  }
}
