Model: file://amf-client/shared/src/test/resources/validations/examples/string-hierarchy.raml
Profile: RAML 1.0
Conforms? false
Number of results: 4

Level: Violation

- Source: http://a.ml/vocabularies/amf/parser#exampleError
  Message: should be string
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/examples/string-hierarchy.raml#/declarations/types/scalar/SomeType/example/invalidInt1
  Property: 
  Position: Some(LexicalInformation([(11,19)-(11,20)]))
  Location: file://amf-client/shared/src/test/resources/validations/examples/string-hierarchy.raml

- Source: http://a.ml/vocabularies/amf/parser#exampleError
  Message: should be string
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/examples/string-hierarchy.raml#/declarations/types/scalar/SomeType/example/invalidInt2
  Property: 
  Position: Some(LexicalInformation([(12,19)-(12,31)]))
  Location: file://amf-client/shared/src/test/resources/validations/examples/string-hierarchy.raml

- Source: http://a.ml/vocabularies/amf/parser#exampleError
  Message: should be string
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/examples/string-hierarchy.raml#/declarations/types/scalar/SomeType/example/invalidBoolean
  Property: 
  Position: Some(LexicalInformation([(13,22)-(13,26)]))
  Location: file://amf-client/shared/src/test/resources/validations/examples/string-hierarchy.raml

- Source: http://a.ml/vocabularies/amf/parser#exampleError
  Message: should be string
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/examples/string-hierarchy.raml#/declarations/types/scalar/SomeType/example/invalidNumber
  Property: 
  Position: Some(LexicalInformation([(14,21)-(14,24)]))
  Location: file://amf-client/shared/src/test/resources/validations/examples/string-hierarchy.raml