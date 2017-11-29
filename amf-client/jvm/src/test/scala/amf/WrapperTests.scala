package amf

import amf.core.unsafe.PlatformSecrets
import amf.model.document.Document
import amf.model.domain.{ScalarShape, WebApi}
import amf.plugins.document.vocabularies.registries.PlatformDialectRegistry
import org.scalatest.AsyncFunSuite

import scala.concurrent.ExecutionContext

class WrapperTests extends AsyncFunSuite with PlatformSecrets {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  test("Parsing test") {
    AMF.init().get()
    val parser = new Raml10Parser()
    val baseUnit = parser.parseFileAsync("file://amf-client/shared/src/test/resources/api/zencoder.raml").get()
    assert(baseUnit.location == "file://amf-client/shared/src/test/resources/api/zencoder.raml")
    val api = baseUnit.asInstanceOf[Document].encodes.asInstanceOf[WebApi]
    val endpoint = api.endPoints.get(0)
    assert(endpoint.path == "/v3.5/path")
    assert(api.endPoints.size() == 1)
    assert(endpoint.operations.size() == 1)
    val post = endpoint.operations.get(0)
    assert(post.method == "get")
    assert(post.request.payloads.size() == 1)
    assert(post.request.payloads.get(0).mediaType == "application/json")
    assert(post.request.payloads.get(0).schema.getTypeIds().contains("http://www.w3.org/ns/shacl#ScalarShape"))
    assert(post.request.payloads.get(0).schema.getTypeIds().contains("http://www.w3.org/ns/shacl#Shape"))
    assert(post.request.payloads.get(0).schema.getTypeIds().contains("http://raml.org/vocabularies/shapes#Shape"))
    assert(post.request.payloads.get(0).schema.getTypeIds().contains("http://raml.org/vocabularies/document#DomainElement"))
    assert(post.responses.get(0).payloads.get(0).schema.asInstanceOf[ScalarShape].dataType == "http://www.w3.org/2001/XMLSchema#string")
    assert(post.request.payloads.get(0).schema.asInstanceOf[ScalarShape].dataType == "http://www.w3.org/2001/XMLSchema#string")
    assert(post.responses.get(0).statusCode == "200")
  }

  test("Generation test") {
    AMF.init().get()
    val parser = new Raml10Parser()
    val baseUnit = parser.parseFileAsync("file://amf-client/shared/src/test/resources/api/zencoder.raml").get()
    assert(new Raml10Generator().generateString(baseUnit) != "") // TODO: test this properly
    assert(new Oas20Generator().generateString(baseUnit) != "")
    assert(new AmfGraphGenerator().generateString(baseUnit) != "")
  }

  test("Resolution test") {
    AMF.init().get()
    val parser = new Raml10Parser()
    val baseUnit = parser.parseFileAsync("file://amf-client/shared/src/test/resources/api/zencoder.raml").get()
    val report = AMF.validate(baseUnit, "RAML").get()
    assert(report.conforms)
    AMF.loadValidationProfile("file://amf-client/shared/src/test/resources/api/validation/custom-profile.raml").get()
    val custom = AMF.validate(baseUnit, "Banking").get()
    assert(!custom.conforms)
  }

  test("Vocabularies test") {
    AMF.init().get()

    AMF.registerDialect("file://amf-client/shared/src/test/resources/api/dialects/eng-demos.raml").get()

    val parser = new Raml10Parser()
    val baseUnit = parser.parseFileAsync("file://amf-client/shared/src/test/resources/api/examples/libraries/demo.raml").get()


    PlatformDialectRegistry
    val report = AMF.validate(baseUnit, "Eng Demos 0.1").get()
    assert(report.conforms)

    AMF.registerNamespace("eng-demos", "http://mulesoft.com/vocabularies/eng-demos#")
    val elem = baseUnit.asInstanceOf[Document].encodes
    val speakers = elem.getObjectByPropertyId("eng-demos:speakers")
    assert(speakers.size() > 0)
  }

}