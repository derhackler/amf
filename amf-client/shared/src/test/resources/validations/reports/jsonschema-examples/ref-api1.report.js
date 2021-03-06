Model: file://amf-client/shared/src/test/resources/validations/jsonschema/ref/api1.raml
Profile: RAML 1.0
Conforms? false
Number of results: 2

Level: Violation

- Source: http://a.ml/vocabularies/amf/validation#example-validation-error
  Message: foo should NOT have additional properties
foo should be boolean
foo should match some schema in anyOf

  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/jsonschema/ref/api1.raml#/web-api/end-points/%2Fep3/get/200/application%2Fjson/schema/example/default-example
  Property: file://amf-client/shared/src/test/resources/validations/jsonschema/ref/api1.raml#/web-api/end-points/%2Fep3/get/200/application%2Fjson/schema/example/default-example
  Position: Some(LexicalInformation([(48,0)-(51,0)]))
  Location: file://amf-client/shared/src/test/resources/validations/jsonschema/ref/api1.raml

- Source: http://a.ml/vocabularies/amf/validation#example-validation-error
  Message: foo should be boolean
foo should match some schema in anyOf
foo.foo should be boolean
foo.foo should be object
foo.foo should match some schema in anyOf

  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/jsonschema/ref/api1.raml#/web-api/end-points/%2Fep4/get/200/application%2Fjson/schema/example/default-example
  Property: file://amf-client/shared/src/test/resources/validations/jsonschema/ref/api1.raml#/web-api/end-points/%2Fep4/get/200/application%2Fjson/schema/example/default-example
  Position: Some(LexicalInformation([(59,0)-(60,27)]))
  Location: file://amf-client/shared/src/test/resources/validations/jsonschema/ref/api1.raml
