package mb

import groovy.io.FileType
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import mb.argumentprocessing.ArgumentsMerge
import mb.usecase.ArgumentsValidatorInteractor
import org.apache.commons.lang.RandomStringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.stereotype.Component

@Component
class Main {

    @Autowired
    ArgumentsValidatorInteractor argumentsValidatorInteractor

    @Autowired
    ArgumentsMerge argumentsMerge

    static void main(String[] args) {

        def defaultArguments = [
                'inputdir': [required: false, defaultValue: 'json'],
                'outputdir': [required: false, defaultValue: 'combinedjson'],
                'maxfilesizebytes': [required: false, defaultValue: '15728640'],

        ]

        ApplicationContext ctx =
            new AnnotationConfigApplicationContext("mb");

        Main main = ctx.getBean(Main.class);

        main.run(defaultArguments, args.toList())

    }

    void run(Map defaultArguments, List<String> arguments) {

        def errorOutputter = { println it }

        if (!argumentsValidatorInteractor.isValid(errorOutputter, defaultArguments, arguments)) {
            return
        }

        Map settingsMap = argumentsMerge.merge(defaultArguments, arguments)

        File inputDirectory = new File(settingsMap.inputdir.toString())

        if (!inputDirectory.exists()) {
            println "Failed to find input directory. Quitting"
            return
        }

        File outputDirectory = new File(settingsMap.outputdir.toString())

        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            println "Failed to create output directory. Quitting"
            return
        }

        Stack<File> filesToProcess = fetchFilesInDirectory(inputDirectory)

        List<File> filesToMerge = []

        while (filesToProcess) {
            Long filesToMergeBytes = filesToMerge*.size().sum() ?: 0

            Long newFileInBytes = filesToProcess.peek().size() + filesToMergeBytes

            Boolean isSmallerThanMaxFileSizeBytes = newFileInBytes < settingsMap.maxfilesizebytes.toLong()

            if (isSmallerThanMaxFileSizeBytes) {
                filesToMerge << filesToProcess.pop()
            }

            if (!filesToProcess || !isSmallerThanMaxFileSizeBytes) {

                writeToFile(filesToMerge, settingsMap)
                filesToMerge = []
            }

        }

    }

    void writeToFile(ArrayList<File> filesToMerge, Map settingsMap) {

        def j = combineFilesToArray(filesToMerge)
        String randomString = RandomStringUtils.random(9, true, true)
        def f = new File("${settingsMap.outputdir}/${randomString}.json")
        f.write(JsonOutput.toJson(j))
    }

    List combineFilesToArray(List<File> files) {

        JsonSlurper s = new JsonSlurper()
        files.collect {
            println "Merging ${it.absolutePath} with size ${it.size()}"
            s.parseText(it.text)
        }

    }

    private Stack<File> fetchFilesInDirectory(File inputDirectory) {

        Stack<File> list = []
        inputDirectory.eachFileRecurse(FileType.FILES) { File f ->
            list << f
        }

        list
    }

}
