package com.dependency.analyzer.dependency.analyzer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

@SpringBootApplication
public class DependencyAnalyzerApplication implements ApplicationListener<ContextRefreshedEvent> {

	@Value("${analyzer.root.path:/Users/SMihajlovski/projects/}")
	private String rootPath;
	@Value("${analyzer.root.prefix:com.esure}")
	private String mavenGroupPrefix;

	public static void main(String[] args) {
		SpringApplication.run(DependencyAnalyzerApplication.class, args);
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
		new Analyzer(rootPath, mavenGroupPrefix).analyze();
	}

}
