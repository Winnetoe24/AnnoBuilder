package de.alexander.brand.annobuilder.prozessor;

import com.squareup.javapoet.ClassName;
import lombok.Getter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.StandardLocation;
import java.beans.beancontext.BeanContext;
import java.beans.beancontext.BeanContextServices;
import java.beans.beancontext.BeanContextServicesSupport;
import java.beans.beancontext.BeanContextSupport;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;


@Getter
public class ConfigProzessor {

    private final Map<ClassName, ClassName> collectionConstructorMap = new HashMap<>();

    /**
     * Beim Setzen des Package des Builders wird bei alle Entries {@code String.replac(key,value)} angewendet.
     */
    private final Map<String, String> packageNameReplacements = new HashMap<>();

    /**
     * Das Package, was für alle Builder genutzt wird, wenn das Package nicht angegeben ist.
     * $PACKAGE wird mit der Package der Klasse ersetzt
     */
    private final String packageString;

    /**
     * Das Package der Klasse für den der Builder erzeugt wird, wird mit dieser Maske gefiltert → {@code String.replace(packageMaske, "")}
     */
    private final String packageMask;

    private final String addMethodSuffix;
    private final String defaultProvider;
    private final ValueHandlingMode valueHandlingMode;

    public ConfigProzessor(ProcessingEnvironment processingEnvironment) {


        Properties properties = new Properties();
        try (InputStream systemResourceAsStream = processingEnvironment.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "builder.properties").openInputStream()) {
            properties.load(systemResourceAsStream);
        } catch (IOException e) {
            //Ignored
        }
        packageString = properties.getProperty("package", "de.builder");
        addMethodSuffix = properties.getProperty("addMethodSuffix","$N");
        defaultProvider = properties.getProperty("provider",null);
        packageMask = properties.getProperty("packageMask","");
        valueHandlingMode = ValueHandlingMode.valueOf(properties.getProperty("valueHandlingMode",ValueHandlingMode.ALWAYS_SET.name()));

        initPackageNameReplacements(properties.getProperty("packageNameReplacements"));

        String collectionClassMap = properties.getProperty("collectionClassMap");
        if (collectionClassMap == null) {
            System.out.println("No ClassMap Properties found");
            return;
        }
        Properties classMapProperties = new Properties();
        try (InputStream systemResourceAsStream = processingEnvironment.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", collectionClassMap).openInputStream()) {
            classMapProperties.load(systemResourceAsStream);
        } catch (IOException e) {
            //Ignored
        }

        initCollectionConstructorMap(classMapProperties);


    }

    /* --- Initialisier --- */

    private void initCollectionConstructorMap(Properties classMapProperties) {
        collectionConstructorMap.put(ClassName.get(List.class), ClassName.get(ArrayList.class));
        collectionConstructorMap.put(ClassName.get(Set.class), ClassName.get(HashSet.class));
        collectionConstructorMap.put(ClassName.get(Collection.class), ClassName.get(ArrayList.class));
        collectionConstructorMap.put(ClassName.get(Map.class), ClassName.get(HashMap.class));
        collectionConstructorMap.put(ClassName.get(AbstractList.class), ClassName.get(ArrayList.class));
        collectionConstructorMap.put(ClassName.get(AbstractCollection.class), ClassName.get(ArrayList.class));
        collectionConstructorMap.put(ClassName.get(AbstractQueue.class), ClassName.get(PriorityQueue.class));
        collectionConstructorMap.put(ClassName.get(AbstractSequentialList.class), ClassName.get(LinkedList.class));
        collectionConstructorMap.put(ClassName.get(BeanContext.class), ClassName.get(BeanContextSupport.class));
        collectionConstructorMap.put(ClassName.get(BeanContextServices.class), ClassName.get(BeanContextServicesSupport.class));
        collectionConstructorMap.put(ClassName.get(BlockingDeque.class), ClassName.get(LinkedBlockingDeque.class));
        collectionConstructorMap.put(ClassName.get(BlockingQueue.class), ClassName.get(LinkedBlockingQueue.class));
        collectionConstructorMap.put(ClassName.get(Deque.class), ClassName.get(ArrayDeque.class));
        collectionConstructorMap.put(ClassName.get(Queue.class), ClassName.get(ArrayDeque.class));

        for (String stringPropertyName : classMapProperties.stringPropertyNames()) {
            ClassName className = TypeUtils.toClassName(stringPropertyName);
            if (className == null) {
                System.err.println("No valid ClassName:" + stringPropertyName);
                continue;
            }
            String property = classMapProperties.getProperty(stringPropertyName);
            if (property == null) {
                System.err.println("No value:" + stringPropertyName);
                continue;
            }
            ClassName propertyClassName = TypeUtils.toClassName(property);
            if (propertyClassName == null) {
                System.err.println("No valid ClassName:" + property);
                continue;
            }
            collectionConstructorMap.put(className, propertyClassName);
            System.out.println(className + ":" + propertyClassName);
        }
    }

    private void initPackageNameReplacements(String configValue) {
        if (configValue == null || configValue.isBlank()) return;
        String[] split = configValue.split(";");
        for (String entry : split) {
            String[] pair = entry.split("=");
            packageNameReplacements.put(pair[0], pair[1]);
            System.out.println("Pair:"+pair);
        }
    }


    public String getAddMethodSuffix(String variableName) {
        if (variableName.endsWith("s")) {
            variableName = variableName.substring(0, variableName.length()-1);
        }
        System.out.println("variableName:"+variableName);
        return addMethodSuffix.replace("$N",variableName);
    }

}
