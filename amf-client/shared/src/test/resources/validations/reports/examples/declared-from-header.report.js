Model: file://amf-client/shared/src/test/resources/validations/examples/declared-from-header.raml
Profile: RAML
Conforms? false
Number of results: 1

Level: Violation

- Source: http://a.ml/vocabularies/amf/parser#exampleError
  Message: {"keyword":"maxLength","dataPath":"","schemaPath":"#/maxLength","params":{"limit":2},"message":"should NOT be longer than 2 characters"}

  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/examples/declared-from-header.raml#/declarations/types/scalar/person/example/default-example
  Property: file://amf-client/shared/src/test/resources/validations/examples/declared-from-header.raml#/declarations/types/scalar/person/example/default-example
  Position: Some(LexicalInformation([(8,13)-(8,20)]))
  Location: file://amf-client/shared/src/test/resources/validations/examples/declared-from-header.raml