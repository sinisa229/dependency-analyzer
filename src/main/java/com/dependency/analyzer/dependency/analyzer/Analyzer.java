package com.dependency.analyzer.dependency.analyzer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Analyzer {

    private final DependenciesModel projectDependenciesModel;
    private final DependenciesModel serviceDependencyModel;
    private final String rootPath;
    private final String mavenGroupPrefix;

    public Analyzer(String rootPath, String mavenGroupPrefix) {
        this.rootPath = rootPath;
        this.mavenGroupPrefix = mavenGroupPrefix;
        projectDependenciesModel = new DependenciesModel();
        serviceDependencyModel = new DependenciesModel();
    }

    @SneakyThrows
    public void analyze() {
        Path path = Paths.get(rootPath);
        if (Files.isDirectory(path)) {
            traverseDir(path);
        } else {
            throw new RuntimeException(String.format("%s is not a directory.", rootPath));
        }

        projectDependenciesModel.getLinks().forEach(link -> {
            final Node node = new Node(link.getTarget(), 1);
            if (!projectDependenciesModel.getNodes().contains(node)) {
                projectDependenciesModel.getNodes().add(node);
            }
        });

        toJson(this.projectDependenciesModel);
        System.out.println(this.projectDependenciesModel);

        String service = "api-swg-oraclegateway";
        extractDependencies(service);
        toJson(serviceDependencyModel);
    }

    private void toJson(DependenciesModel dependenciesModel) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            System.out.println(mapper.writeValueAsString(dependenciesModel));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private void extractDependencies(String service) {
        serviceDependencyModel.getNodes().add(new Node(service, 1));
        projectDependenciesModel.getLinks().stream().filter(link -> link.getSource().equals(service)).forEach(link -> {
            serviceDependencyModel.getLinks().add(link);
            extractDependencies(link.getTarget());
        });
    }

    private void traverseDir(Path path) throws IOException {
        List<Path> subDirectories = new ArrayList<>();
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(path)) {
            paths.iterator().forEachRemaining(subDirectories::add);
            subDirectories.forEach(this::processPath);
        }
    }

    @SneakyThrows
    private void processPath(Path path) {
        if (Files.isDirectory(path)) {
            traverseDir(path);
        } else if (path.getFileName().endsWith("pom.xml")) {
            processPom(path);
        }

    }

    @SneakyThrows
    private void processPom(Path path) {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = reader.read(new FileReader(path.toFile()));
        projectDependenciesModel.getNodes().add(new Node(model.getArtifactId(), 1));
        model.getDependencies().stream().filter(dependency -> dependency.getGroupId().startsWith(mavenGroupPrefix)).forEach(dependency -> {
            projectDependenciesModel.getLinks().add(new Link(model.getArtifactId(), dependency.getArtifactId(), 1));
        });
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class DependenciesModel {
        private List<Node> nodes = new ArrayList<>();
        private List<Link> links = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Node {
        private String id;
        private Integer group = 1;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Link {
        private String source;
        private String target;
        private Integer value = 1;
    }

}
