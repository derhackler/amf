package amf.dialects

import amf.compiler.Root
import amf.dialects.RAML_1_0_DialectTopLevel.{NodeDefinitionObject, PropertyMappingObject}
import amf.document.{BaseUnit, Document}
import amf.domain.dialects.DomainEntity
import amf.metadata.Type
import amf.model.AmfScalar
import amf.spec.dialects._
import amf.vocabulary.Namespace

import scala.collection.mutable

/**
  * Created by Pavel Petrochenko on 15/09/17.
  */

case class NM(namespace: Namespace, name:String){ }
object NM{
  def apply(s:String): Option[NM] ={

    val ind: Int = Math.max(s.lastIndexOf('/'), s.lastIndexOf('#'))

    if (ind > 0) {
      val namespace = Namespace(s.substring(0, ind + 1))
      val str1 = s.substring(ind + 1)
      Some(NM(namespace,str1))
    } else {
      None
    }
  }
}

class DialectLoader {

  val builtins: TypeBuiltins = new TypeBuiltins(){

    override def resolveToEndity(root: Root, name: String, t: Type): Option[DomainEntity] = None
  }

  private def retrieveDomainEntity(unit:BaseUnit) = unit match {
    case document: Document => document.encodes.asInstanceOf[DomainEntity]
    case _                  => throw new Exception(s"Cannot load a dialect from a unit that is not a document $unit")
  }

  def loadDialect(document: BaseUnit): Dialect = loadDialect(retrieveDomainEntity(document))

  private def registerType (n:NodeDefinitionObject,dialectMap:mutable.Map[String,DialectNode]) =
    NM(n.classTerm().get) match {
      case Some(ns) =>
        dialectMap.put(n.entity.id, new DialectNode(ns.name, ns.namespace))
      case _        => // ignore
    }

  def loadDialect(domainEntity: DomainEntity): Dialect = {
    val dialectObject = RAML_1_0_DialectTopLevel.dialectObject(domainEntity)
    val rootEntity = for {
      ramlNode     <- dialectObject.raml()
      ramlDocument <- ramlNode.document()
      root         <- ramlDocument.resolvedEncodes()

    } yield {
      root
    }

    rootEntity match {

      case Some(encodedRootEntity) =>
        val dialectMap = mutable.Map[String,DialectNode]()
        val propertyMap = mutable.Map[DialectPropertyMapping, PropertyMappingObject]()

        dialectObject.nodeMappings().foreach {registerType(_, dialectMap)}

        if (!dialectMap.contains(encodedRootEntity.entity.id)){
          registerType(encodedRootEntity, dialectMap)
          parseNodeMapping(encodedRootEntity, dialectMap, propertyMap)
        }
        dialectObject.nodeMappings().foreach { n =>
          parseNodeMapping(n, dialectMap, propertyMap)
        }

        fillHashes( propertyMap)
        val fragmentList:mutable.Map[String,DialectNode]=mutable.Map();
        val dialect = for {
          dialectName    <- dialectObject.dialect()
          dialectVersion <- dialectObject.version()
          dialectNode    <- dialectMap.get(encodedRootEntity.entity.id)

        } yield {
          dialectObject.raml().foreach(r=>{
            r.fragments().foreach(f=>{
              f.encodes().foreach(rm=>{
                rm.resolvedDeclaredNode().foreach(rdn=>{
                  rm.name().foreach(fn=>{
                      dialectMap.get(rdn.entity.id).foreach(dn=>{
                        fragmentList.put(fn,dn);
                      })
                  });
                })
              })
            })
          })
          val dmap: mutable.Map[String, DialectNode] = fillModule(dialectMap,dialectObject)
          if (dmap.nonEmpty){
            var mn=new DialectNode("module",Namespace.Document);
            dmap.keys.foreach(k=>{
              mn.map(k,new DialectPropertyMapping("name",Type.Str,namespace=Some(Namespace.Schema)),dmap.get(k).get);
            })
            // now we have a library node
            Dialect(dialectName, dialectVersion, dialectNode, resolver = (root, refs) => BasicResolver(root, List(), refs),Some(mn),fragmentList.toMap)
          }
          else {
            Dialect(dialectName, dialectVersion, dialectNode, resolver = (root, refs) => BasicResolver(root, List(), refs),None,fragmentList.toMap)
          }
        }

        dialect match {
          case Some(dialectFound) => dialectFound
          case _                  => throw new Exception("Cannot load dialect, dialect not found")
        }

      case _ => throw new Exception("Cannot load dialect, root entity not found")
    }
  }


  private def fillModule(dialectMap: mutable.Map[String,DialectNode],dialectObject: RAML_1_0_DialectTopLevel.dialectObject) = {
    val dmap: mutable.Map[String, DialectNode] = mutable.Map();
    dialectObject.raml().foreach(v => {
      v.module().foreach(m => {
        m.declares().foreach(d => {
          for {
            nodeName <- d.name();
            resolvedNode <- d.resolvedDeclaredNode()
          } yield {
              dialectMap.get(resolvedNode.entity.id).foreach(d => dmap.put(nodeName, d));
            }
        })
      })
    })
    dmap
  }

  private def fillHashes(propertyMap: mutable.Map[DialectPropertyMapping, PropertyMappingObject]) = {
    for {
      (dialectPropertyMapping, v) <- propertyMap
      hash <- v.hash()
    } yield
      {
        val r=dialectPropertyMapping.range
        if (dialectPropertyMapping.unionTypes.isDefined){
          dialectPropertyMapping.unionTypes.get.foreach { uoption =>
            processHashRange(dialectPropertyMapping, hash, uoption)
          }
        }
        else {
          processHashRange(dialectPropertyMapping, hash, r)
        }
      }
  }

  private def processHashRange(dialectPropertyMapping: DialectPropertyMapping, hash: String, r: Type) = {
    r match {
      case rangeNode: DialectNode =>
        for {
          property <- rangeNode.mappings() if property.iri() == hash
        } yield {
          connectHash(dialectPropertyMapping, property,r)
        }
      case _ => // ignore
    }
  }

  private def connectHash(hashedProperty: DialectPropertyMapping, hashProperty: DialectPropertyMapping, r: Type) = {
    hashedProperty.owningNode.get.add(hashedProperty.copy(hash = Option(hashProperty)))
    r.asInstanceOf[DialectNode].add(hashProperty.copy(noRAML = true))
  }

  def parsePropertyMapping(domainEntity: PropertyMappingObject, dialects: mutable.Map[String,DialectNode], props: mutable.Map[DialectPropertyMapping,PropertyMappingObject]): DialectPropertyMapping = {

    val name = domainEntity.name()
    val `type`: List[Type] = resolveType(domainEntity, dialects,props)

    var res =  DialectPropertyMapping(name.get,`type`.head)
    if (`type`.size>1) {
      res=res.copy(unionTypes = Some(`type`))
    }
    domainEntity.mandatory().foreach { mandatory =>
      res = res.copy(required = mandatory)
    }

    domainEntity.allowMultiple().foreach { isCollection =>
      res = res.copy(collection = isCollection)
    }

    domainEntity.propertyTerm().foreach { term =>
      NM(term) foreach { ns =>
        res = res.copy(namespace = Some(ns.namespace), rdfName = Some(ns.name))
      }
    }
    domainEntity.maximum().foreach { m =>
      res=res.copy(maximum = Some(m.toInt))
    }
    domainEntity.minimum().foreach { m =>
      res=res.copy(minimum = Some(m.toInt))
    }
    domainEntity.pattern().foreach { p =>
      res=res.copy(pattern = Some(p))
    }
    domainEntity.defaultValue().foreach(v=>{
      val sc:AmfScalar=res.range match {
        case Type.Int=>AmfScalar(v.toInt)
        case Type.Bool=>AmfScalar(v.toBoolean)
        case _=>AmfScalar(v)
      }
      res=res.copy(defaultValue = Some(sc))
    })
    val ev=domainEntity.`enum`()

    if (ev.nonEmpty){
      res=res.copy(`enum` = Some(ev))
    }


    res
  }

  private def resolveType(domainEntity: PropertyMappingObject, dialects: mutable.Map[String, DialectNode], props: mutable.Map[DialectPropertyMapping,PropertyMappingObject]): List[Type] = {
    val strings = domainEntity.entity.rawstrings(PropertyMapping.range)
    domainEntity.resolvedRange().zip(strings).map { rr =>
      val (range,rangeString) = rr
      if (range.isDefined) {
        // this is locally defined type
        val value = range.get
        val id = value.entity.id
        if (dialects.contains(id)) {
          // return node from global declarations
          dialects(id)
        }
        else {
          // this is inplace range type definition
          registerType(value, dialects)
          parseNodeMapping(value, dialects, props)
        }
      }
      else{
        // this is built in type
        builtins.buitInType(rangeString) match {
          case Some(t) => t
          case None =>
            throw new Exception(s"Cannot find dialect node type for $range")
        }
      }
    }
  }

  def parseNodeMapping(domainEntity: NodeDefinitionObject, dialects: mutable.Map[String,DialectNode], props: mutable.Map[DialectPropertyMapping,PropertyMappingObject]): DialectNode = {
    val node = dialects(domainEntity.entity.id)

    domainEntity.mapping().foreach { p=>
      val mapping = node.add(parsePropertyMapping(p, dialects,props))
      props.put(mapping,p);
    }

    node
  }
}
