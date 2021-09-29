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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Generates a java mapper class as per convention from an xml mapping file.
 * S.S.Haider
 * 9-28-2021
 */
public class Generator {

    public static final String MAPPER_PACKAGE = "com.cjs.jworks.domain.mapper.";
    public static final String CONVERTER_PACKAGE = "com.cjs.jworks.domain.converter.";
    public static final String MAPPER_SUPER_CLASS = "BaseMapper";

    public static final String PATH_DOMAIN_MAPPINGS = "\\src\\main\\resources\\domain-mappings\\";
    public static final String SRC_MAIN_JAVA = "\\src\\main\\java\\";
    public static final String FILE_EXTENSION_XML = ".xml";

    public static final String ANNOTATION_MAPPER = "org.mapstruct.Mapper";
    public static final String ANNOTATION_MAPPING_TARGET = "org.mapstruct.MappingTarget";
    public static final String ANNOTATION_MAPPING = "org.mapstruct.Mapping";
    public static final String ANNOTATION_INHERIT_INVERSE_CONFIGURATION = "org.mapstruct.InheritInverseConfiguration";
    public static final String ANNOTATION_SPRING_AUTOWIRED = "org.springframework.beans.factory.annotation.Autowired";


    public static final String TODO = "/*TODO*/";

    public static final String METHOD_TO_DOMAIN_OBJECT = "toDomainObject";
    public static final String METHOD_TO_ENTITY = "toEntity";

    public static List<String> classLevelMappers = Arrays.asList("BooleanColumnMapper",
            "PrimaryKeyMapper");

    public static List<String> commonConverters = Arrays.asList("BooleanColumnConverter",
            "PrimaryKeyConverter",
            "CodeTableConverter",
            "CodeTableLabelConverter");


    public static void main(String[] args) {
        List<String> converterCreatedFieldsList = new ArrayList<String>();
        //Three command line arguments required
        //1-Project location for example; D:\MyDrive\Projects\jworks\ (last slash is important)
        //2-Name of file that needs to be created i.e. mapper file which we are going to write
        //3-Name of Mapping file (without extension) from which we need to read the mappings i.e. xml file
        String projectLocation = "";
        String javaMapperFileName = "";
        String xmlMappingFileName = "";

        if(args[0] == null || args[0].isEmpty()){
            System.out.println("Project location must be given.");
            return;
        }
        else{
            projectLocation = args[0];
            if(projectLocation.charAt(projectLocation.length()-1) != '\\'){
                projectLocation +="\\";
            }
        }

        if(args[1] == null || args[1].isEmpty()){
            System.out.println("Name required for mapper.");
            return;
        }
        else{
            javaMapperFileName = args[1];
        }

        if(args[2] == null || args[2].isEmpty()){
            System.out.println("Name required for XML file to be read.");
            return;
        }
        else{
            xmlMappingFileName = args[2];
            if(xmlMappingFileName.contains(".")){
                xmlMappingFileName = xmlMappingFileName.substring(0,xmlMappingFileName.indexOf("."));
            }
        }

        // Instantiate the Factory
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            JCodeModel codeModel = new JCodeModel();
            JDefinedClass definedClass = codeModel._class(JMod.PUBLIC |JMod.ABSTRACT, MAPPER_PACKAGE + javaMapperFileName,ClassType.CLASS);
            definedClass._extends(codeModel.ref(MAPPER_PACKAGE + MAPPER_SUPER_CLASS));

            JAnnotationUse annotationMapper = definedClass.annotate(codeModel.ref(ANNOTATION_MAPPER));
            JAnnotationArrayMember annotationMapperParamArray = annotationMapper.paramArray("uses");
            for(String mapper : classLevelMappers) {
                annotationMapperParamArray.param(codeModel.ref(CONVERTER_PACKAGE + mapper).dotclass());
            }

            // parse XML file
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(projectLocation + PATH_DOMAIN_MAPPINGS + xmlMappingFileName + FILE_EXTENSION_XML));

            doc.getDocumentElement().normalize();

            String entityClass = doc.getElementsByTagName("class-a").item(0).getTextContent();
            String domainClass = doc.getElementsByTagName("class-b").item(0).getTextContent();
            JClass jEntity = codeModel.ref(entityClass);
            JClass jDomain = codeModel.ref(domainClass);

            JMethod toDomainObjectMethod = definedClass.method(33,jDomain , METHOD_TO_DOMAIN_OBJECT);
            toDomainObjectMethod.param(jEntity,"entity");

            JMethod toEntityMethod = definedClass.method(33,jEntity , METHOD_TO_ENTITY);
            toEntityMethod.param(jDomain,"domain");
            toEntityMethod.param(jEntity,"destination").annotate(codeModel.ref(ANNOTATION_MAPPING_TARGET));

            toEntityMethod.annotate(codeModel.ref(ANNOTATION_INHERIT_INVERSE_CONFIGURATION));
            toEntityMethod.annotate(codeModel.ref(ANNOTATION_MAPPING))
                    .param("target", "createdBy")
                    .param("ignore", true);
            toEntityMethod.annotate(codeModel.ref(ANNOTATION_MAPPING))
                    .param("target", "dtmCreated")
                    .param("ignore", true);
            toEntityMethod.annotate(codeModel.ref(ANNOTATION_MAPPING))
                    .param("target", "modBy")
                    .param("ignore", true);
            toEntityMethod.annotate(codeModel.ref(ANNOTATION_MAPPING))
                    .param("target", "dtmMod")
                    .param("ignore", true);

            NodeList list = doc.getElementsByTagName("field");

            for (int temp = 0; temp < list.getLength(); temp++) {

                Node node = list.item(temp);

                if (node.getNodeType() == Node.ELEMENT_NODE) {

                    Element element = (Element) node;

                    // get field names
                    String a_entityField = element.getElementsByTagName("a").item(0).getTextContent();
                    String b_domainField = element.getElementsByTagName("b").item(0).getTextContent();

                    // get mapping attributes
                    String type = element.getAttribute("type");
                    String customConvertor = element.getAttribute("custom-converter");


                    String converterDependencyfield = "";
                    String outterClass = "";
                    String staticInnerClass = "";

                    if (customConvertor != null && !customConvertor.isEmpty() ){
                        toDomainObjectMethod.annotate(codeModel.ref(ANNOTATION_MAPPING))
                                .param("target", b_domainField)
                                .param("source", a_entityField)
                                .param("expression", JExpr.direct(TODO));

                        if(customConvertor.contains("$")) {
                            converterDependencyfield = customConvertor.substring(customConvertor.indexOf("$") + 1, customConvertor.length());
                            converterDependencyfield = converterDependencyfield.substring(0, 1).toLowerCase().concat(converterDependencyfield.substring(1));
                            outterClass = customConvertor.substring(0, customConvertor.indexOf('$'));
                            staticInnerClass = customConvertor.substring(customConvertor.lastIndexOf('.') + 1, customConvertor.length());
                            staticInnerClass = staticInnerClass.replace('$', '.');

                            //add converter dependency static inner converter not created correctly , needs tweaking afterwards
                            if(!converterCreatedFieldsList.contains(converterDependencyfield)){
                                JFieldVar field = definedClass.field(JMod.PROTECTED, codeModel.ref(outterClass), converterDependencyfield);
                                field.annotate(codeModel.ref(ANNOTATION_SPRING_AUTOWIRED));
                                converterCreatedFieldsList.add(converterDependencyfield);
                            }
                        }
                        else{
                            converterDependencyfield = customConvertor.substring(customConvertor.lastIndexOf('.') + 1, customConvertor.length());
                            converterDependencyfield = converterDependencyfield.substring(0, 1).toLowerCase().concat(converterDependencyfield.substring(1));

                            //For common converters we dont need any dependency variable to be created so we have negation condition
                            //Also if any converter is dependency is created once then  we remember it and not created it next time
                            if(!commonConverters.contains(converterDependencyfield) && !converterCreatedFieldsList.contains(converterDependencyfield)){
                                //add converter dependency only if its not a common converter but a specialized one
                                JFieldVar field = definedClass.field(JMod.PROTECTED, codeModel.ref(customConvertor), converterDependencyfield);
                                field.annotate(codeModel.ref(ANNOTATION_SPRING_AUTOWIRED));
                                converterCreatedFieldsList.add(converterDependencyfield);
                            }
                        }
                    }
                    else {
                        toDomainObjectMethod.annotate(codeModel.ref(ANNOTATION_MAPPING))
                                .param("target", b_domainField)
                                .param("source", a_entityField);
                    }


                    System.out.println("Current Element :" + node.getNodeName());
                    System.out.println("Type : " + type);
                    System.out.println("Custom Convertor : " + customConvertor);
                    System.out.println("a: " + a_entityField);
                    System.out.println("b : " + b_domainField);
                }
            }
            codeModel.build(new File(projectLocation + SRC_MAIN_JAVA));

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
