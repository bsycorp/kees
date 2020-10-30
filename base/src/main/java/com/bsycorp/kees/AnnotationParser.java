package com.bsycorp.kees;

import com.bsycorp.kees.models.Parameter;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnnotationParser {

    public List<Parameter> parseYamlFile(File yamlFile) throws IOException {
        List<Parameter> result = new ArrayList<>();

        //parser file, get Parameters
        Yaml yaml = new Yaml();
        //retrieve from deep nested yaml
        Map<String, Object> manifest = (Map<String, Object>) yaml.loadAll(new FileInputStream(yamlFile)).iterator().next();
        Map<String, Object> spec = (Map<String, Object>) manifest.get("spec");
        Map<String, Object> template = (Map<String, Object>) spec.get("template");
        Map<String, Object> metadata = (Map<String, Object>) template.get("metadata");
        Map<String, Object> annotations = (Map<String, Object>) metadata.get("annotations");

        for (Map.Entry<String, Object> entry : annotations.entrySet()) {
            String key = entry.getKey();
            String value = (String) entry.getValue();
            // parser param and add to parameters
            Parameter parameter = Parameter.construct(key, value);
            if (parameter != null) {
                result.add(parameter);
            }
        }

        return result;
    }

    public List<Parameter> parseFlatFile(File flatFile) {
        List<Parameter> result = new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(flatFile.toPath());
            //walk through each line in file, one annotation per line
            for (String line : lines) {
                //split each line based on first equals '=' sign
                String key = line.substring(0, line.indexOf("="));
                String value = line.substring(line.indexOf("=") + 1);
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }

                //try and construct a param obj, returns null if unsupported
                Parameter parameter = Parameter.construct(key, value);
                //if supported param then add to results
                if (parameter != null) {
                    result.add(parameter);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public List<Parameter> parseMap(Map<String, String> annotationMap) {
        List<Parameter> result = new ArrayList<>();
        if(annotationMap == null){
            return result;
        }
        try {
            for (Map.Entry<String, String> entry : annotationMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                // parser param and add to parameters
                Parameter parameter = Parameter.construct(key, value);
                if (parameter != null) {
                    result.add(parameter);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
}
