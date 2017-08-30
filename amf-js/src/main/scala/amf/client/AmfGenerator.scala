package amf.client

import amf.model.BaseUnit
import amf.remote.Amf
import amf.remote.Syntax.Json

import scala.scalajs.js
import scala.scalajs.js.JSConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

/**
  * [[amf.remote.Amf]] generator.
  */
@JSExportTopLevel("AmfGenerator")
class AmfGenerator extends BaseGenerator(Amf, Json) {

  @JSExport
  def generateFile(unit: BaseUnit, url: String, options: GenerationOptions, handler: FileHandler): Unit =
    generate(unit.unit, url, options, UnitHandlerAdapter(handler))

  @JSExport
  def generateString(unit: BaseUnit, options: GenerationOptions, handler: StringHandler): Unit =
    generate(unit.unit, options, StringHandlerAdapter(handler))

  @JSExport
  def generateFile(unit: BaseUnit, url: String, options: GenerationOptions): js.Promise[String] =
    generate(unit.unit, url, options).toJSPromise

  @JSExport
  def generateString(unit: BaseUnit, options: GenerationOptions): js.Promise[String] =
    generate(unit.unit, options).toJSPromise
}
