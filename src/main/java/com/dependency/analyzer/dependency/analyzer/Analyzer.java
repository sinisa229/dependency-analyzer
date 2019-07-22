package com.dependency.analyzer.dependency.analyzer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class Analyzer {

    @Value("${analyzer.root.path:/Users/smihajlovski/projects/spring-boot}")
    private String rootPath;
    private ReturnModel returnModel;

    @PostConstruct
    public void analyze() throws IOException {
        this.returnModel = new ReturnModel();
        Path path = Paths.get(rootPath);
        if (Files.isDirectory(path)) {
            traverseDir(path);
        } else {
            throw new RuntimeException(String.format("%s is not a directory.", rootPath));
        }
    }

    private void traverseDir(Path path) throws IOException {
        Files.newDirectoryStream(path).iterator().forEachRemaining(this::processPath);
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
        System.out.println(model.getArtifactId());
        returnModel.getNodes().add(new Node(model.getArtifactId(), 1));
        model.getDependencies().stream().filter(dependency -> dependency.getGroupId().startsWith("org.spring")).forEach(dependency -> {
            System.out.println("---->" + dependency.getArtifactId());
            returnModel.getLinks().add(new Link(model.getArtifactId(), dependency.getArtifactId(), 1));
            System.out.println(returnModel);
        });
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ReturnModel {
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
