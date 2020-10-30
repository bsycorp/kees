package com.bsycorp.kees;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.converters.FileConverter;
import com.bsycorp.kees.models.Parameter;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.StringTemplateSource;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExportMain {

    private static Logger LOG = LoggerFactory.getLogger(ExportMain.class);

    private static AnnotationParser annotationParser = new AnnotationParser();

    @com.beust.jcommander.Parameter(names = {"--input", "-i"}, converter = FileConverter.class, required = true)
    File inputManifest;

    @com.beust.jcommander.Parameter(names = {"--template", "-t"}, converter = FileConverter.class, required = true)
    File exportTemplate;

    @com.beust.jcommander.Parameter(names = {"--module", "-m"}, required = true)
    String inputModuleName;

    @com.beust.jcommander.Parameter(names = {"--output", "-o"}, converter = FileConverter.class, required = false)
    File outputTerraform;

    public static void main(String... argv) throws Exception {
        ExportMain main = new ExportMain();
        JCommander.newBuilder()
                .addObject(main)
                .build()
                .parse(argv);
        try{
            main.run();
        } catch (Exception e){
            LOG.error("Error in execution", e);
            throw e;
        }
    }

    public void run() throws Exception {
        LOG.info("Starting export process, with configuration: \n\t input: {}\n\t output: {}",
                inputManifest,
                outputTerraform
        );

        String terraformResource = generateResourceForFile(inputModuleName, inputManifest, exportTemplate);

        if (outputTerraform == null) {
            LOG.info("Writing output to stdout: \n---\n {}\n---", terraformResource);
        } else {
            LOG.info("Writing output to {}", outputTerraform);
            FileUtils.writeStringToFile(outputTerraform, terraformResource, Charset.defaultCharset());
        }
        LOG.info("Done");
    }

    public static String generateResourceForFile(String moduleName, File inputFile, File exportTemplate) throws Exception {
        LOG.info("Parsing YAML to extract module name and annotations..");
        List<Parameter> parameters = annotationParser.parseYamlFile(inputFile);
        LOG.info("Constructed {} parameters from YAML.", parameters.size());

        //have params, generate terraform resource from input
        LOG.info("Generating terraform for input: {} with {} params and name: {}", inputFile, parameters.size(), moduleName);
        parameters.sort(Comparator.comparing(Parameter::getStorageSuffix));

        Handlebars handlebars = new Handlebars();
        handlebars.prettyPrint(true);
        handlebars.registerHelper("storageSuffix", (Helper<Parameter>) (context, options) -> context.getStorageSuffix());

        String templateSource = FileUtils.readFileToString(exportTemplate, "UTF-8");
        Template template = handlebars.compile(new StringTemplateSource("terraform-secrets", templateSource));
        Map<String, Object> context = new HashMap<>();
        context.put("moduleName", moduleName);
        context.put("parameters", parameters);
        return template.apply(Context.newContext(context));
    }
}
