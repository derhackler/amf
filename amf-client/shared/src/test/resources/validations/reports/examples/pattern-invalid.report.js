Model: file://amf-client/shared/src/test/resources/validations/examples/pattern-invalid.raml
Profile: RAML
Conforms? false
Number of results: 1

Level: Violation

- Source: http://a.ml/vocabularies/amf/parser#exampleError
  Message: {"keyword":"pattern","dataPath":".signature","schemaPath":"#/properties/signature/pattern","params":{"pattern":"^\\d{3}-\\w{12}$"},"message":"should match pattern \"^\\d{3}-\\w{12}$\""}
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/examples/pattern-invalid.raml#/web-api/end-points/%2Fusers/clearanceLevel/object_1
  Property: 
  Position: Some(LexicalInformation([(15,0)-(16,23)]))
  Location: 