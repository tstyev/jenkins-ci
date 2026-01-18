package school.redrover.common.filter;

import org.testng.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static school.redrover.common.ProjectUtils.log;

public class FilterForTestsUtils {

    public static List<IMethodInstance> filter(List<String> fileList, String dependenciesClasses, List<IMethodInstance> methods) {
        final String pathTemplate = "src/test/java/%s.java";

        Set<String> changedFiles = fileList.stream()
                .filter(e -> !e.startsWith("D="))
                .map(e -> e.substring(e.lastIndexOf('=') + 1))
                .collect(Collectors.toSet());
        //System.out.println("Changed files: " + changedFiles);

        Map<Class<?>, String> classMap = methods.stream()
                .map(IMethodInstance::getMethod).map(ITestNGMethod::getTestClass).map(IClass::getRealClass)
                .collect(Collectors.toMap(
                        Function.identity(),
                        clazz -> String.format(pathTemplate, clazz.getName().replace('.', '/')),
                        (pathA, pathB) -> pathA
                ));
        //System.out.println("Class map: " + classMap);

        Map<String, Set<String>> dependenciesFilesMap = Arrays.stream(dependenciesClasses.split(";"))
                .filter(s -> s.contains("="))
                .map(s -> s.split("="))
                .collect(Collectors.groupingBy(
                        parts -> String.format(pathTemplate, parts[0].replace('.', '/')),
                        Collectors.mapping(parts -> String.format(pathTemplate, parts[1].replace('.', '/')), Collectors.toSet())
                ));
        //System.out.println("Dependencies graph:" + dependenciesFilesMap);

        Map<String, Set<String>> filteredGraph = new HashMap<>();
        for (String file : changedFiles) {
            Set<String> children = dependenciesFilesMap.getOrDefault(file, Collections.emptySet())
                    .stream()
                    .filter(child -> !child.equals(file)) // только убираем самоссылки
                    .filter(child -> !child.contains("$")) // убираем inner-классы
                    .collect(Collectors.toSet());
            filteredGraph.put(file, children);
        }

        Set<String> affectedFiles = new HashSet<>();
        Set<String> visitedFiles = new HashSet<>();

        for (String file : changedFiles) {
            collectLeaves(file, filteredGraph, affectedFiles, visitedFiles);
        }

        log("Affected files: " + affectedFiles);

        if (classMap.values().containsAll(affectedFiles)) {
            return methods.stream().filter(method -> affectedFiles.contains(classMap.get(method.getMethod().getTestClass().getRealClass()))).collect(Collectors.toList());
        }

        return methods;
    }

    private static void collectLeaves(String currentFile,
                                      Map<String, Set<String>> dependencyGraph,
                                      Set<String> affectedFiles,
                                      Set<String> visitedFiles) {

        if (!visitedFiles.add(currentFile)) return; // защита от циклов

        Set<String> children = dependencyGraph.getOrDefault(currentFile, Collections.emptySet())
                .stream()
                .filter(child -> !child.equals(currentFile)) // убираем самоссылки
                .filter(child -> !child.contains("$")) // убираем inner-классы
                .collect(Collectors.toSet());

        if (children.isEmpty()) {
            affectedFiles.add(currentFile);
            return;
        }

        for (String child : children) {
            collectLeaves(child, dependencyGraph, affectedFiles, visitedFiles);
        }
    }
}