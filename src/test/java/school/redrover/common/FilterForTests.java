package school.redrover.common;

import org.testng.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FilterForTests implements IMethodInterceptor {

    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
        String files = System.getenv("LIST_OF_CHANGED_FILES");
        String dependenciesFiles = System.getenv("LIST_OF_DEPENDENCIES_FILES");

        if (files != null && dependenciesFiles != null) {
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
                            clazz -> String.format("src/test/java/%s.java", clazz.getName().replace('.', '/')),
                            (pathA, pathB) -> pathA
                    ));

            Map<String, Set<String>> graph = Arrays.stream(dependenciesFiles.split(";"))
                    .map(s -> s.split("="))
                    .collect(Collectors.groupingBy(
                            p -> String.format("src/test/java/%s.java", p[0].replace('.', '/')),
                            Collectors.mapping(
                                    p -> String.format("src/test/java/%s.java", p[1].replace('.', '/')),
                                    Collectors.toSet()
                            )
                    ));

            Set<String> affectedFiles = resolve(changedFiles, graph);

            System.out.println("Affected files" + affectedFiles);
            if (classMap.values().containsAll(affectedFiles)) {
                return methods.stream().filter(method -> affectedFiles.contains(classMap.get(method.getMethod().getTestClass().getRealClass()))).collect(Collectors.toList());
            }

        }

        return methods;
    }

    private Set<String> resolve(Set<String> changedFiles, Map<String, Set<String>> graph) {
        Set<String> result = new HashSet<>();
        for (String current : changedFiles) {
            resolveRecursive(current, graph, result);
        }

        return result;
    }

    private void resolveRecursive(String current, Map<String, Set<String>> graph, Set<String> result) {
        Set<String> children = graph.get(current);
        if (children == null || children.isEmpty()) {
            result.add(current);
            return;
        }
        for (String child : children) {
            resolveRecursive(child, graph, result);
        }
    }
}
