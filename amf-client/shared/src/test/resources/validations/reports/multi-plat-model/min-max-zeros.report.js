Model: file://amf-client/shared/src/test/resources/validations/facets/min-max-zeros.raml
Profile: RAML 1.0
Conforms? false
Number of results: 1

Level: Violation

- Source: http://a.ml/vocabularies/amf/validation#example-validation-error
  Message: should be <= 0
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/facets/min-max-zeros.raml#/shape/property/SSN%3F/scalar/SSN%3F/example/default-example
  Property: file://amf-client/shared/src/test/resources/validations/facets/min-max-zeros.raml#/shape/property/SSN%3F/scalar/SSN%3F/example/default-example
  Position: Some(LexicalInformation([(7,13)-(7,22)]))
  Location: file://amf-client/shared/src/test/resources/validations/facets/min-max-zeros.raml
