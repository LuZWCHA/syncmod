package io.nettyrouter.processors;

import io.nettyrouter.annotation.NettyRouter;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@AutoService(Processor.class)
public class RouterProcessor extends AbstractProcessor {

    private ProcessingEnvironment processingEnvironment;
    private Filer filter;
    private final String PACKAGE_NAME = "com.router";
    private final String CLASS_NAME = "RouterSource";

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        processingEnvironment = processingEnv;
        filter = processingEnv.getFiler();
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> set = new HashSet<>();
        set.add(NettyRouter.Router.class.getCanonicalName());
        return set;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        
        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.NOTE,"process...");

        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(NettyRouter.Router.class);
        Map<String ,Element> classNameMap = new HashMap<>();//className -> element
        Map<String,Element> methodNameMap = new HashMap<>();//url -> element
        elements.forEach((Consumer<Element>) element -> {
            if(element.getKind().isClass()){
                TypeElement typeElement = (TypeElement) element;
                classNameMap.put(typeElement.getSimpleName().toString(),typeElement);
                List<? extends Element> elementSet = element.getEnclosedElements();
                elementSet.forEach((Consumer<Element>) element1 -> {
                    if(element1.getKind() == ElementKind.METHOD){
                        Optional.ofNullable(element1.getAnnotation(NettyRouter.RouterHandler.class))
                                .ifPresent(routerHandler -> methodNameMap.put(routerHandler.httpMethod()+ ":"+
                                        routerHandler.routerUri(), element1));

                    }
                });

            }
        });

        if(!classNameMap.isEmpty() && !methodNameMap.isEmpty()){
            genJarFile(classNameMap,methodNameMap);
        }
        return true;
    }

    private void genJarFile(Map<String, Element> classNameMap,Map<String,Element>methodNameMap){
        FieldSpec classInstances = FieldSpec.builder(ParameterizedTypeName.get(Map.class,String.class,Object.class),"classInstances",Modifier.PRIVATE)
                .build();
        FieldSpec methodMap = FieldSpec.builder(ParameterizedTypeName.get(Map.class,String.class,String.class),"methodMap",Modifier.PRIVATE)
                .build();
        MethodSpec.Builder init_builder = MethodSpec.methodBuilder("init")
                .addModifiers(Modifier.PRIVATE)
                .returns(TypeName.VOID)
                .addStatement("$N = new $T<>()",classInstances,HashMap.class)
                .addStatement("$N = new $T<>()",methodMap,HashMap.class);


        classNameMap.forEach(new BiConsumer<String, Element>() {
            @Override
            public void accept(String s, Element element) {
                init_builder.addStatement("$N.put($S,new $L())",classInstances,s,element.asType().toString());
            }
        });

        methodNameMap.forEach(new BiConsumer<String, Element>() {
            @Override
            public void accept(String s, Element element) {
                init_builder.addStatement("$N.put($S,$S)",
                        methodMap,s,element.getEnclosingElement().getSimpleName().toString()+"$"+element.getSimpleName().toString());
            }
        });

        MethodSpec init = init_builder.build();

        MethodSpec con = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addStatement("init()")
                .build();

        MethodSpec getMethodNameMap = MethodSpec.methodBuilder("methodNameMap")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(Map.class,String.class,String.class))
                .addStatement("return $N",methodMap)
                .build();

        MethodSpec getClassInstance = MethodSpec.methodBuilder("classInstances")
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(Override.class)
                .returns(ParameterizedTypeName.get(Map.class,String.class,Object.class))
                .addStatement("return $N",classInstances)
                .build();

        TypeSpec controlClass = TypeSpec.classBuilder(CLASS_NAME)
                .addModifiers(Modifier.PUBLIC)
                .addField(classInstances)
                .addField(methodMap)
                .addMethod(con)
                .addMethod(init)
                .addMethod(getMethodNameMap)
                .addMethod(getClassInstance)
                .addSuperinterface(NettyRouter.RouterSource.class)
                .build();

        JavaFile javaFile = JavaFile.builder(PACKAGE_NAME,controlClass)
                .build();

        try {
            JavaFileObject javaFileObject = filter.createSourceFile(PACKAGE_NAME +"."+CLASS_NAME);
            processingEnvironment.getMessager().printMessage(Diagnostic.Kind.NOTE,"creating "+javaFileObject.getName());

            Writer writer = javaFileObject.openWriter();
            javaFile.writeTo(writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR,e.toString());

        }
        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.NOTE,"create success");
    }
}
