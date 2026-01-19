package school.redrover.common.filter;

import org.testng.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static school.redrover.common.ProjectUtils.log;

public class FilterForTestsUtils {

    private static boolean logged = false;

    public static List<IMethodInstance> filter(List<String> fileList, String dependenciesClasses, List<IMethodInstance> methodList) {
        final String pathTemplate = "src/test/java/%s.java";

        Set<String> changedFiles = fileList.stream()
                .filter(e -> !e.startsWith("D="))
                .map(e -> e.substring(e.lastIndexOf('=') + 1))
                .collect(Collectors.toSet());

        Map<Class<?>, String> classMap = methodList.stream()
                .map(IMethodInstance::getMethod).map(ITestNGMethod::getTestClass).map(IClass::getRealClass)
                .collect(Collectors.toMap(
                        Function.identity(),
                        clazz -> String.format(pathTemplate, clazz.getName().replace('.', '/')),
                        (pathA, pathB) -> pathA
                ));

        Set<String> testFiles = new HashSet<>(classMap.values());

        Map<String, Set<String>> reversedGraph =
                Arrays.stream(dependenciesClasses.split(";"))
                        .filter(s -> s.contains("="))
                        .map(s -> s.split("="))
                        .collect(Collectors.groupingBy(
                                p -> String.format(pathTemplate, p[0].replace('.', '/')),   // изменяемый
                                Collectors.mapping(
                                        p -> String.format(pathTemplate, p[1].replace('.', '/')), // кто использует
                                        Collectors.toSet()
                                )
                        ));

        Set<String> affectedFiles = new HashSet<>();

        // 4. Process each changed file independently
        for (String changedFile : changedFiles) {
            Set<String> directDeps = reversedGraph.getOrDefault(changedFile, Collections.emptySet());

            // выбираем только тестовые файлы
            Set<String> directTests = directDeps.stream()
                    .filter(testFiles::contains)
                    .collect(Collectors.toSet());

            if (!directTests.isEmpty()) {
                affectedFiles.addAll(directTests);
            } else {
                // прямых тестов нет — файл критический
                affectedFiles.add(changedFile);
            }
        }

        if (!logged) {
            log("Filtered graph: " + reversedGraph);
            log("Affected files: " + affectedFiles);
            logged = true;
        }

        if (classMap.values().containsAll(affectedFiles)) {
            return methodList.stream().filter(method -> affectedFiles.contains(classMap.get(method.getMethod().getTestClass().getRealClass()))).collect(Collectors.toList());
        }

        return methodList;
    }
}