package amf.dumper

import amf.common.Tests
import amf.core.client.GenerationOptions
import amf.emit.AMFUnitFixtureTest
import amf.framework.remote.Syntax.{Json, Yaml}
import amf.remote._
import amf.framework.unsafe.PlatformSecrets
import amf.framework.remote.{Amf, Oas, Raml}
import org.scalatest.{Assertion, FunSuite}

/**
  * AMF Unit DumperTest
  */
class AMFDumperTest extends FunSuite with PlatformSecrets with AMFUnitFixtureTest {

  test("Test simple oas/json") {
    val expected =
      """{
        |  "swagger": "2.0",
        |  "info": {
        |    "title": "test",
        |    "description": "test description",
        |    "termsOfService": "termsOfService",
        |    "version": "1.1"
        |  },
        |  "host": "localhost.com",
        |  "basePath": "api",
        |    "consumes": [
        |    "application/json"
        |  ],
        |    "produces": [
        |    "application/json"
        |  ],
        |  "schemes": [
        |    "http",
        |    "https"
        |  ],
        |  "paths": {}
        |}""".stripMargin

    val actual = new AMFDumper(`document/api/bare`, Oas, Json, GenerationOptions()).dumpToString
    assert(actual, expected)
  }

  test("Test simple raml/yaml") {
    val expected =
      """#%RAML 1.0
        |title: test
        |description: test description
        |mediaType:
        |  - application/json
        |version: 1.1
        |(termsOfService): termsOfService
        |protocols:
        |  - http
        |  - https
        |baseUri: localhost.com/api""".stripMargin

    val actual = new AMFDumper(`document/api/bare`, Raml, Yaml, GenerationOptions()).dumpToString
    assert(actual, expected)
  }

  test("Test simple amf/jsonld") {
    val expected =
      """[
        |  {
        |    "@id": "file:///tmp/test",
        |    "@type": [
        |      "http://raml.org/vocabularies/document#Document",
        |      "http://raml.org/vocabularies/document#Fragment",
        |      "http://raml.org/vocabularies/document#Module",
        |      "http://raml.org/vocabularies/document#Unit"
        |    ],
        |    "http://raml.org/vocabularies/document#encodes": [
        |      {
        |        "@id": "file:///tmp/test#/web-api",
        |        "@type": [
        |          "http://schema.org/WebAPI",
        |          "http://raml.org/vocabularies/document#DomainElement"
        |        ],
        |        "http://schema.org/name": [
        |          {
        |            "@value": "test"
        |          }
        |        ],
        |        "http://schema.org/description": [
        |          {
        |            "@value": "test description"
        |          }
        |        ],
        |        "http://raml.org/vocabularies/http#host": [
        |          {
        |            "@value": "localhost.com"
        |          }
        |        ],
        |        "http://raml.org/vocabularies/http#scheme": [
        |          {
        |            "@value": "http"
        |          },
        |          {
        |            "@value": "https"
        |          }
        |        ],
        |        "http://raml.org/vocabularies/http#basePath": [
        |          {
        |            "@value": "api"
        |          }
        |        ],
        |        "http://raml.org/vocabularies/http#accepts": [
        |          {
        |            "@value": "application/json"
        |          }
        |        ],
        |        "http://raml.org/vocabularies/http#contentType": [
        |          {
        |            "@value": "application/json"
        |          }
        |        ],
        |        "http://schema.org/version": [
        |          {
        |            "@value": "1.1"
        |          }
        |        ],
        |        "http://schema.org/termsOfService": [
        |          {
        |            "@value": "termsOfService"
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]""".stripMargin

    val actual = new AMFDumper(`document/api/bare`, Amf, Json, GenerationOptions()).dumpToString
    assert(actual, expected)
  }

  test("Test full amf/jsonld") {
    val expected =
      """[
        |  {
        |    "@id": "file:///tmp/test",
        |    "@type": [
        |      "http://raml.org/vocabularies/document#Document",
        |      "http://raml.org/vocabularies/document#Fragment",
        |      "http://raml.org/vocabularies/document#Module",
        |      "http://raml.org/vocabularies/document#Unit"
        |    ],
        |    "http://raml.org/vocabularies/document#encodes": [
        |      {
        |        "@id": "file:///tmp/test#/web-api",
        |        "@type": [
        |          "http://schema.org/WebAPI",
        |          "http://raml.org/vocabularies/document#DomainElement"
        |        ],
        |        "http://schema.org/name": [
        |          {
        |            "@value": "test"
        |          }
        |        ],
        |        "http://schema.org/description": [
        |          {
        |            "@value": "test description"
        |          }
        |        ],
        |        "http://raml.org/vocabularies/http#host": [
        |          {
        |            "@value": "localhost.com"
        |          }
        |        ],
        |        "http://raml.org/vocabularies/http#scheme": [
        |          {
        |            "@value": "http"
        |          },
        |          {
        |            "@value": "https"
        |          }
        |        ],
        |        "http://raml.org/vocabularies/http#basePath": [
        |          {
        |            "@value": "api"
        |          }
        |        ],
        |        "http://raml.org/vocabularies/http#accepts": [
        |          {
        |            "@value": "application/json"
        |          }
        |        ],
        |        "http://raml.org/vocabularies/http#contentType": [
        |          {
        |            "@value": "application/json"
        |          }
        |        ],
        |        "http://schema.org/version": [
        |          {
        |            "@value": "1.1"
        |          }
        |        ],
        |        "http://schema.org/termsOfService": [
        |          {
        |            "@value": "termsOfService"
        |          }
        |        ],
        |        "http://schema.org/provider": [
        |          {
        |            "@id": "file:///tmp/test#/web-api/organization",
        |            "@type": [
        |              "http://schema.org/Organization",
        |              "http://raml.org/vocabularies/document#DomainElement"
        |            ],
        |            "http://schema.org/url": [
        |              {
        |                "@id": "organizationUrl"
        |              }
        |            ],
        |            "http://schema.org/name": [
        |              {
        |                "@value": "organizationName"
        |              }
        |            ],
        |            "http://schema.org/email": [
        |              {
        |                "@value": "test@test"
        |              }
        |            ]
        |          }
        |        ],
        |        "http://schema.org/license": [
        |          {
        |            "@id": "file:///tmp/test#/web-api/license",
        |            "@type": [
        |              "http://raml.org/vocabularies/http#License",
        |              "http://raml.org/vocabularies/document#DomainElement"
        |            ],
        |            "http://schema.org/url": [
        |              {
        |                "@id": "licenseUrl"
        |              }
        |            ],
        |            "http://schema.org/name": [
        |              {
        |                "@value": "licenseName"
        |              }
        |            ]
        |          }
        |        ],
        |        "http://schema.org/documentation": [
        |          {
        |            "@id": "file:///tmp/test#/web-api/creative-work/creativoWorkUrl",
        |            "@type": [
        |              "http://schema.org/CreativeWork",
        |              "http://raml.org/vocabularies/document#DomainElement"
        |            ],
        |            "http://schema.org/url": [
        |              {
        |                "@id": "creativoWorkUrl"
        |              }
        |            ],
        |            "http://schema.org/description": [
        |              {
        |                "@value": "creativeWorkDescription"
        |              }
        |            ]
        |          }
        |        ],
        |        "http://raml.org/vocabularies/http#endpoint": [
        |          {
        |            "@id": "file:///tmp/test#/web-api/end-points/%2Fendpoint",
        |            "@type": [
        |              "http://raml.org/vocabularies/http#EndPoint",
        |              "http://raml.org/vocabularies/document#DomainElement"
        |            ],
        |            "http://raml.org/vocabularies/http#path": [
        |              {
        |                "@value": "/endpoint"
        |              }
        |            ],
        |            "http://schema.org/name": [
        |              {
        |                "@value": "endpoint"
        |              }
        |            ],
        |            "http://schema.org/description": [
        |              {
        |                "@value": "test endpoint"
        |              }
        |            ],
        |            "http://www.w3.org/ns/hydra/core#supportedOperation": [
        |              {
        |                "@id": "file:///tmp/test#/web-api/end-points/%2Fendpoint/get",
        |                "@type": [
        |                  "http://www.w3.org/ns/hydra/core#Operation",
        |                  "http://raml.org/vocabularies/document#DomainElement"
        |                ],
        |                "http://www.w3.org/ns/hydra/core#method": [
        |                  {
        |                    "@value": "get"
        |                  }
        |                ],
        |                "http://schema.org/name": [
        |                  {
        |                    "@value": "test get"
        |                  }
        |                ],
        |                "http://schema.org/description": [
        |                  {
        |                    "@value": "test operation get"
        |                  }
        |                ],
        |                "http://raml.org/vocabularies/http#guiSummary": [
        |                  {
        |                    "@value": "summary of operation get"
        |                  }
        |                ],
        |                "http://schema.org/documentation": [
        |                  {
        |                    "@id": "file:///tmp/test#/web-api/end-points/%2Fendpoint/get/creative-work/localhost:8080/endpoint/operation",
        |                    "@type": [
        |                      "http://schema.org/CreativeWork",
        |                      "http://raml.org/vocabularies/document#DomainElement"
        |                    ],
        |                    "http://schema.org/url": [
        |                      {
        |                        "@id": "localhost:8080/endpoint/operation"
        |                      }
        |                    ],
        |                    "http://schema.org/description": [
        |                      {
        |                        "@value": "documentation operation"
        |                      }
        |                    ]
        |                  }
        |                ],
        |                "http://raml.org/vocabularies/http#scheme": [
        |                  {
        |                    "@value": "http"
        |                  }
        |                ]
        |              },
        |              {
        |                "@id": "file:///tmp/test#/web-api/end-points/%2Fendpoint/post",
        |                "@type": [
        |                  "http://www.w3.org/ns/hydra/core#Operation",
        |                  "http://raml.org/vocabularies/document#DomainElement"
        |                ],
        |                "http://www.w3.org/ns/hydra/core#method": [
        |                  {
        |                    "@value": "post"
        |                  }
        |                ],
        |                "http://schema.org/name": [
        |                  {
        |                    "@value": "test post"
        |                  }
        |                ],
        |                "http://schema.org/description": [
        |                  {
        |                    "@value": "test operation post"
        |                  }
        |                ],
        |                "http://raml.org/vocabularies/document#deprecated": [
        |                  {
        |                    "@value": true
        |                  }
        |                ],
        |                "http://raml.org/vocabularies/http#guiSummary": [
        |                  {
        |                    "@value": "summary of operation post"
        |                  }
        |                ],
        |                "http://schema.org/documentation": [
        |                  {
        |                    "@id": "file:///tmp/test#/web-api/end-points/%2Fendpoint/post/creative-work/localhost:8080/endpoint/operation",
        |                    "@type": [
        |                      "http://schema.org/CreativeWork",
        |                      "http://raml.org/vocabularies/document#DomainElement"
        |                    ],
        |                    "http://schema.org/url": [
        |                      {
        |                        "@id": "localhost:8080/endpoint/operation"
        |                      }
        |                    ],
        |                    "http://schema.org/description": [
        |                      {
        |                        "@value": "documentation operation"
        |                      }
        |                    ]
        |                  }
        |                ],
        |                "http://raml.org/vocabularies/http#scheme": [
        |                  {
        |                    "@value": "http"
        |                  }
        |                ]
        |              }
        |            ]
        |          }
        |        ]
        |      }
        |    ]
        |  }
        |]""".stripMargin

    val actual = new AMFDumper(`document/api/full`, Amf, Json, GenerationOptions()).dumpToString
    assert(actual, expected)
  }

  private def assert(actual: String, expected: String): Assertion = {
    Tests.checkDiff(actual, expected)
    succeed
  }
}
