package com.equivant.jw2;

import com.sun.codemodel.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.reflect.Array;
import java.util.Arrays;

public class Generator {
    public static void main(String[] args) {

        // Instantiate the Factory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {

            JCodeModel codeModel = new JCodeModel();
            JDefinedClass definedClass = codeModel._class("com.cjs.jworks.domain.mapper."+args[1]);
            definedClass._extends(codeModel.ref("com.cjs.jworks.domain.mapper.BaseMapper"));

            definedClass.annotate(codeModel.ref("org.mapstruct.Mapper"))
                    .paramArray("uses")
                    .param(codeModel.ref("com.cjs.jworks.domain.converter.BooleanColumnMapper").dotclass())
                    .param(codeModel.ref("com.cjs.jworks.domain.converter.PrimaryKeyMapper").dotclass());

            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            // parse XML file
            DocumentBuilder db = dbf.newDocumentBuilder();

            Document doc = db.parse(new File(args[0]+"\\src\\main\\resources\\domain-mappings\\"+args[2]+".xml"));

            doc.getDocumentElement().normalize();

            String entityClass = doc.getElementsByTagName("class-a").item(0).getTextContent();
            String domainClass = doc.getElementsByTagName("class-b").item(0).getTextContent();
            JClass jEntity = codeModel.ref(entityClass);
            JClass jDomain = codeModel.ref(domainClass);

            JMethod toDomainObjectMethod = definedClass.method(33,jDomain , "toDomainObject");
            toDomainObjectMethod.param(jEntity,"entity");

            JMethod toEntityMethod = definedClass.method(33,jEntity , "toEntity");
            toEntityMethod.param(jDomain,"domain");
            toEntityMethod.param(jEntity,"destination").annotate(codeModel.ref("org.mapstruct.MappingTarget"));

            toEntityMethod.annotate(codeModel.ref("org.mapstruct.InheritInverseConfiguration"));
            toEntityMethod.annotate(codeModel.ref("org.mapstruct.Mapping"))
                    .param("target", "createdBy")
                    .param("ignore", true);
            toEntityMethod.annotate(codeModel.ref("org.mapstruct.Mapping"))
                    .param("target", "dtmCreated")
                    .param("ignore", true);
            toEntityMethod.annotate(codeModel.ref("org.mapstruct.Mapping"))
                    .param("target", "modBy")
                    .param("ignore", true);
            toEntityMethod.annotate(codeModel.ref("org.mapstruct.Mapping"))
                    .param("target", "dtmMod")
                    .param("ignore", true);

            NodeList list = doc.getElementsByTagName("field");

            for (int temp = 0; temp < list.getLength(); temp++) {

                Node node = list.item(temp);

                if (node.getNodeType() == Node.ELEMENT_NODE) {

                    Element element = (Element) node;

                    // get staff's attribute
                    String type = element.getAttribute("type");
                    String customConvertor = element.getAttribute("custom-converter");
                    String converterDependencyfield = "";
                    String staticInnerClass = "";
                    if (customConvertor != null && !customConvertor.isEmpty() && customConvertor.contains("$")){
                        converterDependencyfield = customConvertor.substring(customConvertor.indexOf("$")+1,customConvertor.length());
                        converterDependencyfield = converterDependencyfield.substring(0,1).toLowerCase().concat(converterDependencyfield.substring(1));
                        staticInnerClass = customConvertor.substring(customConvertor.lastIndexOf('.')+1,customConvertor.length());
                        staticInnerClass = staticInnerClass.replace('$','.');
                    }


                    // get text
                    String a_entityField = element.getElementsByTagName("a").item(0).getTextContent();
                    String b_domainField = element.getElementsByTagName("b").item(0).getTextContent();


                    if (customConvertor != null && !customConvertor.isEmpty()) {
                        toDomainObjectMethod.annotate(codeModel.ref("org.mapstruct.Mapping"))
                                .param("target", b_domainField)
                                .param("source", a_entityField)
                                .param("expression", JExpr.direct("/*TODO*/"));

                        if (customConvertor.contains("$")) {
                            JFieldVar field = definedClass.field(JMod.PROTECTED, codeModel.directClass(staticInnerClass), converterDependencyfield);
                            field.annotate(codeModel.ref("org.springframework.beans.factory.annotation.Autowired"));
                        }
                    }
                    else {
                        toDomainObjectMethod.annotate(codeModel.ref("org.mapstruct.Mapping"))
                                .param("target", b_domainField)
                                .param("source",a_entityField);
                    }

                    System.out.println("Current Element :" + node.getNodeName());
                    System.out.println("Type : " + type);
                    System.out.println("Custom Convertor : " + customConvertor);
                    System.out.println("a: " + a_entityField);
                    System.out.println("b : " + b_domainField);
                }
            }
            codeModel.build(new File(args[0]+"\\src\\main\\java\\"));

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JClassAlreadyExistsException e) {
            e.printStackTrace();
        }

    }
}
