package school.redrover.common;

import org.testng.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FilterForTests implements IMethodInterceptor {

    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
        final String pathTemplate = "src/test/java/%s.java";

        String files = System.getenv("LIST_OF_CHANGED_FILES");
        String dependenciesClasses = System.getenv("LIST_OF_DEPENDENCIES_CLASSES");

        if (files != null && dependenciesClasses != null) {
            List<String> entryList = Arrays.stream(files.split(";")).toList();

            Set<String> changedFiles = entryList.stream()
                    .filter(e -> !e.startsWith("D="))
                    .map(e -> e.substring(e.lastIndexOf('=') + 1))
                    .collect(Collectors.toSet());

            System.out.println("Changed files" + changedFiles);

            Map<Class<?>, String> classMap = methods.stream()
                    .map(IMethodInstance::getMethod).map(ITestNGMethod::getTestClass).map(IClass::getRealClass)
                    .collect(Collectors.toMap(
                            Function.identity(),
                            clazz -> String.format(pathTemplate, clazz.getName().replace('.', '/')),
                            (pathA, pathB) -> pathA
                    ));
            System.out.println("Class map" + classMap);

            Map<String, Set<String>> dependencyGraph = Arrays.stream(dependenciesClasses.split(";"))
                    .map(s -> s.split("="))
                    .collect(Collectors.groupingBy(
                            p -> String.format(pathTemplate, p[0].replace('.', '/')),
                            Collectors.mapping(
                                    p -> String.format(pathTemplate, p[1].replace('.', '/')),
                                    Collectors.toSet()
                            )
                    ));
            System.out.println("Dependency graph" + dependencyGraph);

            Set<String> affectedFiles = new HashSet<>();
            Set<String> visitedFiles = new HashSet<>();

            for (String file : changedFiles) {
                collectLeaves(file, dependencyGraph, affectedFiles, visitedFiles);
            }

            System.out.println("Affected files" + affectedFiles);
            if (classMap.values().containsAll(affectedFiles)) {
                return methods.stream().filter(method -> affectedFiles.contains(classMap.get(method.getMethod().getTestClass().getRealClass()))).collect(Collectors.toList());
            }

        }

        return methods;
    }

    private static void collectLeaves(String currentFile, Map<String, Set<String>> dependencyGraph, Set<String> affectedFiles, Set<String> visitedFiles) {
        if (!visitedFiles.add(currentFile)) return;

        Set<String> children = dependencyGraph.get(currentFile);
        if (children == null || children.isEmpty()) {
            affectedFiles.add(currentFile);
            return;
        }

        for (String child : children) {
            collectLeaves(child, dependencyGraph, affectedFiles, visitedFiles);
        }
    }
}