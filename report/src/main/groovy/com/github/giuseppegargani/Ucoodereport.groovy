package com.github.giuseppegargani

import groovy.json.JsonOutput
import org.gradle.api.Plugin
import org.gradle.api.Project

class Ucoodereport implements Plugin<Project> {

    void apply(Project project) {
        project.android {

            testVariants.all { variant ->
                variant.connectedInstrumentTest.doLast {
                    println "The name of the test type: $connectedInstrumentTest.name"
                    println "The type of test $connectedInstrumentTest.class"
                }
                //SI DEVE TOGLIERE COME HARDCODED e mettere project
                project.connectedAndroidTest.finalizedBy(':app:ucoodeTest')
                project.assembleDebugAndroidTest.finalizedBy(':app:ucoodeTest')
            }

            testOptions {

                execution 'ANDROIDX_TEST_ORCHESTRATOR'

                unitTests.all {

                    def testResults = []
                    def intermedi = []
                    def finali = []
                    def clssName = ""
                    def pckgName = ""

                    ignoreFailures = true

                    beforeTest { descriptor -> }

                    afterTest { desc, result ->
                        def singleTest = ["outcome":result.toString(), "testName":desc.name.toString()]
                        testResults.add(singleTest)
                    }

                    beforeSuite { desc ->
                        if(desc.displayName.startsWith("Gradle Test Run ")) {println("INIZIATA UNA SUITE ${desc.properties}")}
                    }
                    afterSuite { desc, result ->

                        //questo se termina una classe di test singola (verifica aggiuntiva del numero di test interni)
                        if((desc.className!=null)&&(testResults.size()>0)){

                            //it divides the classname in segments of path and it assigns them
                            String[] pathArray = desc.className.split('[.]')
                            pckgName = pathArray[0]+"."+pathArray[1]+"."+pathArray[2]
                            clssName = (desc.className).minus(pckgName+".")

                            def singleTestClass = ["test_class_name":clssName,"tests_list":testResults]
                            intermedi.add(singleTestClass)
                            testResults = []
                        }
                        //the outer class
                        if(desc.displayName.startsWith("Gradle Test Run ")) {
                            finali = ["package_name":pckgName, "test_classes_list":intermedi]
                            //it saves on json
                            def json = JsonOutput.toJson(finali)
                            new File("ReportUnitTest.json").write(json)
                        }
                        //print the results more clearly
                        if (!desc.parent) { // will match the outermost suite
                            def output = "Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} passed, ${result.failedTestCount} failed, ${result.skippedTestCount} skipped)"
                            def startItem = '|  ', endItem = '  |'
                            def repeatLength = startItem.length() + output.length() + endItem.length()
                            println('\n' + ('-' * repeatLength) + '\n' + startItem + output + endItem + '\n' + ('-' * repeatLength))
                        }
                    }

                    //Granularity in console about the results of the unit tests (verified that 3 corresponds to the single test methods)
                    testLogging {
                        minGranularity 3
                        maxGranularity 3
                    }
                    onOutput { descriptor, event ->
                        logger.lifecycle("Test: " + descriptor + " produced standard out/err: " + event.message )
                    }
                }
            }
        }

        /*project.task('hello') {
            doLast {
                println "${extension.message.get()} from ${extension.greeter.get()}"
            }
        }*/
        // Create a task using the task type
        project.task('hello') {
            doLast {
                println("CIAO DALLA CUSTOM TASK")
            }
        }

        //project.tasks.register('hello')

        project.task('ucoodeTest') {
            def nomeapp = 'nome'

            doLast {
                nomeapp = project.android.defaultConfig.applicationId
                println "The package name of the app: $nomeapp"
            }
            doLast {
                try{
                    project.exec {
                        //we could also put in just one line: '&&', 'adb', 'pull', 'sdcard/android/data/com.example.jsonreport/files/JsonTestReport.json'
                        commandLine 'adb', 'shell', 'am', 'instrument', '-w', "${nomeapp}.test/androidx.test.runner.AndroidJUnitRunner"
                    }
                    //println("The PREVIOUS report has been correctly transfered and if you want to update the REPORT please retry this OR MANUALLY PASS THE FILE TO THE ROOT PROJECT FOLDER BY TERMINAL: gradlew passReport")
                    //e legge per verifica
                    //String fileContents = new File('JsonTestReport.json').text
                    //println(fileContents)
                } catch(all){
                    println("THE REPORT HAS NOT YET BEEN TRANSFERED SO PLEASE RETRY OR MANUALLY PASS THE FILE TO THE ROOT PROJECT FOLDER BY TERMINAL: gradlew passReport")
                }
            }
            doLast {
                try{
                    project.exec{
                        commandLine 'adb', 'pull', "sdcard/android/data/${nomeapp}/files/JsonTestReport.json", '../'
                    }
                    println("THE REPORT HAS BEEN TRANSFERED CORRECTLY")
                    String fileContents = new File('JsonTestReport.json').text
                    println(fileContents)
                } catch(all){
                    println("THE REPORT HAS NOT YET BEEN TRANSFERED SO PLEASE RETRY OR MANUALLY PASS THE FILE TO THE ROOT PROJECT FOLDER BY TERMINAL: gradlew passReport")
                }

            }
        }

        //VERIFICARE CHE TRASFERISCA PER DIVERSI COMANDI TEST STRUMENTALE, CON IF CONDIZIONALE (NON ERRORE) E SENZA ANDROIDJUNITRUNNER
        project.task('passReport') {
            def nomeapp = 'nome'

            doLast {
                nomeapp = it.android.defaultConfig.applicationId
                println "The package name is: $nomeapp"
            }
            doLast {
                try{
                    project.exec {
                        //copy the file from emulator to root project folder
                        commandLine 'adb', 'pull', "sdcard/android/data/${nomeapp}/files/JsonTestReport.json", '../'
                    }
                    println("The report has been correctly transfered to the root project folder")
                    //e legge per verifica
                    String fileContents = new File('JsonTestReport.json').text
                    println(fileContents)
                } catch(all){
                    println("SOME PROBLEM HAS OCCURED WITH THE MANUAL TRANSFER OF REPORT: please inform us by email")
                }
            }
        }

        project.task('listaFile'){
            //from 'src/main/groovy/com/example/plugin/prova.txt'
            /*FileTree tree = fileTree(dir: 'src')
            tree.each {File file ->
                println file
            }*/
            //def stringa = getClass().getResource('../prova.txt').text
            //def variabile = it.fileTree('./').toList()
            //def valore = getClass().getResource(null).text
            def cartella = getPath()
            def cartellina = getClass().location
            println("SCRITTURA e proprietà: ${getClass().properties} $cartella e: $cartellina")
        }

    }
}
