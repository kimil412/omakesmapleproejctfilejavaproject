package what.the.fox.say;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class LogstashClientMaker {
    
    private int startLog4jPortNumber = 2324;
    private int startLogbackPortNumber = 4560;
    
    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        makeProject();
        //runLogstashAndGradleTest();
    }
    
    public static void runLogstashAndGradleTest() throws IOException {
        
        int numberOfClient = 10;
        
        LogstashClientMaker clientMaker = new LogstashClientMaker();
        
        for(int i = 0; i< numberOfClient; i++) {
            clientMaker.runLogstashInstance(i);
            clientMaker.testProject(i);
        }
    }
    
    public static void makeProject() throws InterruptedException, ExecutionException {
        int numberOfClient = 10;
        
        LogstashClientMaker clientMaker = new LogstashClientMaker();
        
        for(int i = 0; i< numberOfClient; i++)
            clientMaker.startMakeShipperProject(i);
        
        //runBuildScript(clientMaker);
    }

    private static void runBuildScript(LogstashClientMaker clientMaker)
            throws InterruptedException, ExecutionException {
        
        int numberOfClient = 10;
        
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
        Future<Boolean>[] futures = new Future[numberOfClient];
        for(int i = 0; i< numberOfClient; i++) {
            System.out.println("Build project "+ i); 
            
            BuildTask buildTask = clientMaker.new BuildTask(i, clientMaker);
            
            futures[i] = executor.submit(buildTask);
        }
        
        
        int numberOfDone = 0;
        boolean[] checkedArray = new boolean[numberOfClient];
        StringBuffer completedOrderLogging = new StringBuffer();
        
        executor.shutdown();
        while( !executor.awaitTermination(1, TimeUnit.SECONDS) ){
            System.out.println("Not all tasks are done!");
            
            while(numberOfDone != numberOfClient) {
                for(int i = 0; i < numberOfClient; i++) {
                    if( futures[i].isDone() && !checkedArray[i]){
                        boolean b = futures[i].get();
                        completedOrderLogging.append(i + " project build return!").append("\n");
                        System.out.println(i + " project build return!");
                        checkedArray[i] = true;
                        numberOfDone++;
                    }
                }
            }
        }
        
        while(numberOfDone != numberOfClient) {
            for(int i = 0; i < numberOfClient; i++) {
                if( futures[i].isDone() && !checkedArray[i]){
                    boolean b = futures[i].get();
                    completedOrderLogging.append(i + " project build return!").append("\n");
                    System.out.println(i + " project build return!");
                    checkedArray[i] = true;
                    numberOfDone++;
                }
            }
        }
        /*Stream.of(futures)
        .filter(f -> {
                try{
                    return f.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            })
        .forEach((f) -> {
            int num = 0;
            for(int j = 0; j < numberOfClient; j++) {
                if ( f == futures[j] ) {
                    num = j;
                    break;
                }
            }
            System.out.println(num + " project build return!");
        });*/
        System.out.println("RESULT");
        System.out.println(completedOrderLogging.toString());
        System.out.println("done completed..");
    }
    
    public class BuildTask implements Callable<Boolean> {

        private int index;
        private LogstashClientMaker instance;
        
        public BuildTask() {
        }
        
        public BuildTask(int ith, LogstashClientMaker instance) {
            this.index = ith;
            this.instance = instance;
        }
        
        @Override
        public Boolean call() throws Exception {
            
            instance.buildProject(index);
            
            return true;
        }
        
    }
    
    private void startMakeShipperProject(int ith) {
        
        try {
            makeDefaultDirectories(ith);//
            makeLog4jProperties(ith);//
            makeLogbackXML(ith);//
            makeLogstashConfiguration(ith);//
            copyRequiredJavaFiles(ith);//
            copyRequiredCertificateFiles(ith);
            makeGradleBuild(ith);//
            //runLogstashInstance(ith);//
            
            //buildProject(ith);
            
            //testProject(ith);
        } catch (IOException | ParserConfigurationException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void copyRequiredCertificateFiles(int ith) throws IOException {
        Path keyFilePath = Paths.get(System.getProperty("user.dir") + File.separator);
        keyFilePath = Paths.get(keyFilePath.toString(), "lumberjack.key");
        
        if( !Files.exists(keyFilePath) ) {
            throw new NoSuchFileException("not found key file");
        }
        
        Path targetFilePath = Paths.get(System.getProperty("user.dir") + File.separator +"project"+ith + File.separator +"lumberjack.key");
        
        if ( !Files.exists(targetFilePath)) {
            Files.createDirectories(targetFilePath.getParent());
            //Files.createFile(targetFilePath);
            Files.copy(keyFilePath, targetFilePath);
        } else {
            Files.copy(keyFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        System.out.println(targetFilePath + " is now created...");
        //TODO:2016-01-27 copy test java file ... << done 2016-01-27 11:14 AM
        
        Path crtFilePath = Paths.get(System.getProperty("user.dir") + File.separator);
        crtFilePath = Paths.get(crtFilePath.toString(), "lumberjack.crt");
        
        if( !Files.exists(crtFilePath) ) {
            throw new NoSuchFileException("not found crt file");
        }
        
        Path targetTestFilePath = Paths.get(System.getProperty("user.dir") + File.separator +"project"+ith + File.separator +"lumberjack.crt");
        
        if ( !Files.exists(targetTestFilePath)) {
            Files.createDirectories(targetTestFilePath.getParent());
            //Files.createFile(targetFilePath);
            Files.copy(crtFilePath, targetTestFilePath);
        } else {
            Files.copy(crtFilePath, targetTestFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        System.out.println(targetTestFilePath + " is now created...");
    }


    private void exitLogstashInstance(int ith) {
        
    }
    
    private void runLogstashInstance(int ith) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir") + File.separator +"project"+ith);
        path = Paths.get(path.toString(), "src/main/resources");
        Runtime rtime = Runtime.getRuntime();
//        Process proc = rtime.exec("cmd /K cd project" + ith);
        final Process proc = rtime.exec("cmd /C logstash -f logstash.conf", null, path.toFile());
        
        BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        
        String line = null;
//        long startTime = System.currentTimeMillis();
        
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                try {
                    System.err.println("Before KILL");
                    TimeUnit.SECONDS.sleep(25);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.err.println("After KILL");
                proc.destroy();
                System.err.println("Completed KILL");
            }
        }).start();
        
        while ( (line = br.readLine()) != null ) {
            System.out.println(line);
        }
    }


    private void testProject(int ith) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir") + File.separator +"project"+ith);
        Runtime rtime = Runtime.getRuntime();
//        Process proc = rtime.exec("cmd /K cd project" + ith);
        Process proc = rtime.exec("cmd /C gradle clean test --info", null, path.toFile());
        
        BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        
        String line = null;
        while ( (line = br.readLine()) != null ) {
            System.out.println(line);
        }
    }


    private void buildProject(int ith) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir") + File.separator +"project"+ith);
        Runtime rtime = Runtime.getRuntime();
//        Process proc = rtime.exec("cmd /K cd project" + ith);
        Process proc = rtime.exec("cmd /C gradle build --info", null, path.toFile());
        
        BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        
        String line = null;
        while ( (line = br.readLine()) != null ) {
            System.out.println(line);
        }
    }


    private void copyRequiredJavaFiles(int ith) throws IOException {
        Path javaFilePath = Paths.get(System.getProperty("user.dir") + File.separator);
        javaFilePath = Paths.get(javaFilePath.toString(), "src/what/the/fox/say/copy/LoggerFactorySkeleton.java");
        
        if( !Files.exists(javaFilePath) ) {
            throw new NoSuchFileException("not found java file");
        }
        
        Path targetFilePath = Paths.get(System.getProperty("user.dir") + File.separator +"project"+ith + File.separator +"src/main/java/what/the/fox/say/copy/LoggerFactorySkeleton.java");
        
        Files.deleteIfExists(targetFilePath);
        
        if ( !Files.exists(targetFilePath)) {
            Files.createDirectories(targetFilePath.getParent());
            //Files.createFile(targetFilePath);
            Files.copy(javaFilePath, targetFilePath);
        } else {
            Files.copy(javaFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        System.out.println(targetFilePath + " is now created...");
        //TODO:2016-01-27 copy test java file ... << done 2016-01-27 11:14 AM
        
        Path javaTestFilePath = Paths.get(System.getProperty("user.dir") + File.separator);
        javaTestFilePath = Paths.get(javaTestFilePath.toString(), "src/what/the/fox/say/copy/LoggerFactorySkeletonTest.java");
        
        if( !Files.exists(javaTestFilePath) ) {
            throw new NoSuchFileException("not found java file");
        }
        
        Path targetTestFilePath = Paths.get(System.getProperty("user.dir") + File.separator +"project"+ith + File.separator +"src/test/java/what/the/fox/say/copy/LoggerFactorySkeletonTest.java");
        
        Files.deleteIfExists(targetTestFilePath);
        
        if ( !Files.exists(targetTestFilePath)) {
            Files.createDirectories(targetTestFilePath.getParent());
            //Files.createFile(targetFilePath);
            Files.copy(javaTestFilePath, targetTestFilePath);
        } else {
            Files.copy(javaTestFilePath, targetTestFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        System.out.println(targetTestFilePath + " is now created...");
    }


    private void makeLogstashConfiguration(int ith) throws IOException {
        StringBuffer contentStringBuffer = new StringBuffer();
        
        contentStringBuffer.append("input {").append("\n");
        contentStringBuffer.append("log4j {").append("\n");
        contentStringBuffer.append("port => \""+ (startLog4jPortNumber + ith) +"\"").append("\n");
        contentStringBuffer.append("mode => \"server\"").append("\n");
        contentStringBuffer.append("}").append("\n");
        
        contentStringBuffer.append("tcp {").append("\n");
        contentStringBuffer.append("port => "+(startLogbackPortNumber+ith)+"").append("\n");
        contentStringBuffer.append("codec => json_lines").append("\n");
        contentStringBuffer.append("}").append("\n");
        
        contentStringBuffer.append("}").append("\n");
        
        contentStringBuffer.append("filter {").append("\n");
        contentStringBuffer.append("}").append("\n");
        
        contentStringBuffer.append("output {").append("\n");
        contentStringBuffer.append("file {").append("\n");
        contentStringBuffer.append("path => \"E:\\\\ichatt_pc\\\\workspaces_git\\\\LogstashUtility\\\\src\\\\main\\\\resources\\\\lgogog").append(ith).append(".txt\"").append("\n");
        contentStringBuffer.append(" #codec => rubydebug").append("\n");
        contentStringBuffer.append("#flush_interval => 0").append("\n");
        contentStringBuffer.append(" }").append("\n");
        contentStringBuffer.append("stdout {").append("\n");
        contentStringBuffer.append("}").append("\n");
        
        contentStringBuffer.append("lumberjack {").append("\n");
        contentStringBuffer.append("    hosts => ['192.168.8.129']").append("\n");
        contentStringBuffer.append("    port => 2323").append("\n");
        contentStringBuffer.append("    ssl_certificate => \"E:\\\\ichatt_pc\\\\workspaces_git\\\\ToMakeSampleProjectFileJavaProejct\\\\project").append(ith).append("\\\\lumberjack.crt\"").append("\n");
        contentStringBuffer.append("}").append("\n");
        
        contentStringBuffer.append("}").append("\n");
        /**
         * 
        input {
        log4j {
                port => "2324"
                mode => "server"
        }
        tcp {
            port => 4560
            codec => json_lines
        }
        }
        filter {
        }
        output {
        file {
                    path => "E:\\ichatt_pc\\workspaces_git\\LogstashUtility\\src\\main\\resources\\lgogog.txt"
                    #codec => rubydebug
                    #flush_interval => 0
            }
        stdout {
        }
}
        *
        */
        
        Path logstashPath = Paths.get(System.getProperty("user.dir") + File.separator +"project"+ith + File.separator +"src");
        logstashPath = Paths.get(logstashPath.toString(), "main/resources/logstash.conf");
        Files.deleteIfExists(logstashPath);
        
        if(!Files.exists(logstashPath.getParent()))
            Files.createDirectories(logstashPath.getParent());
//        else
//            Files.deleteIfExists(logstashPath);
            
        Path newLogstashConfigurationFile = Files.createFile(logstashPath);
        
        try(BufferedWriter fileWriter = Files.newBufferedWriter(newLogstashConfigurationFile, Charset.defaultCharset())) {
            fileWriter.append(contentStringBuffer.toString()).flush();
        } catch (IOException exception) {
            System.err.println("Error Writng to file");
        }
        
        System.out.println(newLogstashConfigurationFile + " is now created...");
    }


    private void makeGradleBuild(int ith) throws IOException {
        
        final String logbackScriptPath = "copy_for_logback.script";
        final String log4jScriptPath = "copy_for_log4j.script";
        
        Path scriptPath = Paths.get(System.getProperty("user.dir") + File.separator);
        scriptPath = Paths.get(scriptPath.toString(), logbackScriptPath);
        
        Path targetFilePath = Paths.get(System.getProperty("user.dir") + File.separator +"project"+ith + File.separator +"build.gradle");
        
        Files.deleteIfExists(targetFilePath);
        
        if ( !Files.exists(targetFilePath)) {
            Files.createDirectories(targetFilePath.getParent());
            //Files.createFile(targetFilePath);
            Files.copy(scriptPath, targetFilePath);
        } else {
            Files.copy(scriptPath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        System.out.println(targetFilePath + " is now created...");
    }

    private Element makeElementWithNameOptionsText(Document document, String tagName, String[] keyOptions, String text) {
        Element node = document.createElement(tagName);
        
        if ( keyOptions != null ) {
            for(int i = 0; i < keyOptions.length/2; i++) {
                node.setAttribute(keyOptions[i*2], keyOptions[i*2+1]);
            }
        }
        
        if ( text != null ) {
            node.appendChild(document.createTextNode(text));
        }
        return node;
    }
    
    private void makeLogbackXML(int ith) throws IOException, ParserConfigurationException, TransformerFactoryConfigurationError, TransformerException {
        
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;

        builder = documentBuilderFactory.newDocumentBuilder();
        Document document = builder.newDocument();
        
        Element mainRootElement = document.createElement("configuration");
        mainRootElement.setAttribute("debug", "true");
        
        document.appendChild(mainRootElement);
        
        Element consoleAppenderTag = document.createElement("appender");
        consoleAppenderTag.setAttribute("name", "STDOUT");
        consoleAppenderTag.setAttribute("class", "ch.qos.logback.core.ConsoleAppender");
        
        Element encoderAppenderTag = document.createElement("encoder");
        
        Element patternAppenderTag = document.createElement("pattern");
        
        patternAppenderTag.appendChild(document.createTextNode("%-4relative [%thread] %-5level %logger{35} - %msg %n"));
        encoderAppenderTag.appendChild(patternAppenderTag);
        consoleAppenderTag.appendChild(encoderAppenderTag);
        mainRootElement.appendChild(consoleAppenderTag);
        
        Element appenderTag = makeElementWithNameOptionsText(document, "appender", new String[] {"name", "FILE", "class", "ch.qos.logback.core.FileAppender"}, null);
        Element fileTag = makeElementWithNameOptionsText(document, "file", null, "junit.log");
        Element appendTag = makeElementWithNameOptionsText(document, "append", null, "false");
        Element encoderTag = makeElementWithNameOptionsText(document, "encoder", null, null);
        Element patternTag = makeElementWithNameOptionsText(document, "pattern", null, "%-4r %-5level %logger{35}: %msg%n");
        
        encoderTag.appendChild(patternTag);
        appenderTag.appendChild(fileTag);
        appenderTag.appendChild(appendTag);
        appenderTag.appendChild(encoderTag);
        mainRootElement.appendChild(appenderTag);
        
        appenderTag = makeElementWithNameOptionsText(document, "appender", new String[] {"name", "SOCKET", "class", "ch.qos.logback.classic.net.SocketAppender"}, null);
        Element remoteHostTag = makeElementWithNameOptionsText(document, "RemoteHost", null, "localhost");
        Element portTag = makeElementWithNameOptionsText(document, "Port", null, (startLog4jPortNumber+ith)+"");
        Element reconnectionDelayTag = makeElementWithNameOptionsText(document, "reconnectionDelay", null, "10000");
        Element includeCallerDataTag = makeElementWithNameOptionsText(document, "includeCallerData", null, "false");
        
        appenderTag.appendChild(remoteHostTag);
        appenderTag.appendChild(portTag);
        appenderTag.appendChild(reconnectionDelayTag);
        appenderTag.appendChild(includeCallerDataTag);
        mainRootElement.appendChild(appenderTag);
        
        appenderTag = makeElementWithNameOptionsText(document, "appender", new String[] {"name", "stash", "class", "net.logstash.logback.appender.LogstashAccessTcpSocketAppender"}, null);
        Element destinationTag = makeElementWithNameOptionsText(document, "destination", null, "localhost:"+(startLogbackPortNumber+ith));
        encoderTag = makeElementWithNameOptionsText(document, "encoder", new String[]{"class",  "net.logstash.logback.encoder.LogstashEncoder"}, null);
        
        appenderTag.appendChild(destinationTag);
        appenderTag.appendChild(encoderTag);
        mainRootElement.appendChild(appenderTag);
        
        Element loggerTag = makeElementWithNameOptionsText(document, "logger", new String[]{"name", "junit", "level", "INFO"},  null);
        Element appenderRefTag = makeElementWithNameOptionsText(document, "appender-ref", new String[]{"ref", "STDOUT"}, null);
        
        loggerTag.appendChild(appenderRefTag);
        mainRootElement.appendChild(loggerTag);
        
        Element rootTag = makeElementWithNameOptionsText(document, "root", new String[]{"level", "INFO"}, null);
        appenderRefTag = makeElementWithNameOptionsText(document, "appender-ref", new String[]{"ref", "stash"}, null);
        
        rootTag.appendChild(appenderRefTag);
        mainRootElement.appendChild(rootTag);
        
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(document);
        /**
         * 
        <?xml version="1.0" encoding="UTF-8"?>
        <configuration debug="true">
            <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
                <!-- encoders are assigned the type ch.qos.logback.classic.encoder.PatternLayoutEncoder 
                    by default -->
                <encoder>
                    <pattern>%-4relative [%thread] %-5level %logger{35} - %msg %n
                    </pattern>
                </encoder>
            </appender>
            <appender name="FILE" class="ch.qos.logback.core.FileAppender">
                <file>junit.log</file>
                <append>false</append>
                <!-- encoders are assigned the type ch.qos.logback.classic.encoder.PatternLayoutEncoder 
                    by default -->
                <encoder>
                    <pattern>%-4r %-5level %logger{35}: %msg%n</pattern>
                </encoder>
            </appender>
            <appender name="SOCKET" class="ch.qos.logback.classic.net.SocketAppender">
                <RemoteHost>localhost</RemoteHost>
                <Port>2324</Port>
                <reconnectionDelay>10000</reconnectionDelay>
                <includeCallerData>false</includeCallerData>
            </appender>
            <appender name="stash" class="net.logstash.logback.appender.LogstashAccessTcpSocketAppender">
              <destination>localhost:4560</destination>
        
              <!-- encoder is required -->
              <encoder class="net.logstash.logback.encoder.LogstashEncoder" />
            </appender>
            <logger name="MLS_LOGGER" level="INFO">
                <appender-ref ref="SOCKET" />
            </logger>
            <root level="INFO">
                <!-- <appender-ref ref="FILE" /> -->
                <!-- <appender-ref ref="STDOUT" /> -->
                <!-- <appender-ref ref="SOCKET" /> -->
                <appender-ref ref="stash" />
            </root>
            <!-- We want error logging from this logger to go to an extra appender It 
                still inherits CONSOLE STDOUT from the root logger -->
            <logger name="junit" level="INFO">
                <appender-ref ref="STDOUT" />
            </logger>
        </configuration>
        *
        */
        
        Path logbackPath = Paths.get(System.getProperty("user.dir") + File.separator +"project"+ith + File.separator +"src");
        logbackPath = Paths.get(logbackPath.toString(), "main/resources/logback.xml");
        Files.deleteIfExists(logbackPath);
        
        if(!Files.exists(logbackPath.getParent()))
            Files.createDirectories(logbackPath.getParent());
//        else
//            Files.deleteIfExists(logbackPath);
            
        Path newLogbackXMLFile = Files.createFile(logbackPath);
        
        try(BufferedWriter fileWriter = Files.newBufferedWriter(newLogbackXMLFile, Charset.defaultCharset())) {
            StreamResult output = new StreamResult(fileWriter);
            transformer.transform(source, output);
        } catch (IOException exception) {
            System.err.println("Error Writng to file");
        }
        
        System.out.println(newLogbackXMLFile + " is now created...");
    }

    private void makeLog4jProperties(int ith) throws IOException {
        StringBuffer contentStringBuffer = new StringBuffer();
        
        contentStringBuffer.append("log4j.rootLogger=DEBUG, server").append("\n");
        contentStringBuffer.append("log4j.appender.server=org.apache.log4j.net.SocketAppender").append("\n");
        contentStringBuffer.append("log4j.appender.server.Port=").append(startLog4jPortNumber+ith).append("\n");
        contentStringBuffer.append("og4j.appender.server.RemoteHost=localhost").append("\n");
        contentStringBuffer.append("log4j.appender.server.ReconnectionDelay=10000").append("\n");
        /**
         * 
        log4j.rootLogger=DEBUG, server
        log4j.appender.server=org.apache.log4j.net.SocketAppender
        log4j.appender.server.Port=2324
        log4j.appender.server.RemoteHost=localhost
        log4j.appender.server.ReconnectionDelay=10000
        *
        */
        
        Path log4jPath = Paths.get(System.getProperty("user.dir") + File.separator +"project"+ith + File.separator +"src");
        log4jPath = Paths.get(log4jPath.toString(), "main/resources/log4j.properties");
        Files.deleteIfExists(log4jPath);
        
        if(!Files.exists(log4jPath.getParent()))
            Files.createDirectories(log4jPath.getParent());
//        else
//            Files.deleteIfExists(log4jPath);
            
        Path newLog4jPropertiesFile = Files.createFile(log4jPath);
        
        try(BufferedWriter fileWriter = Files.newBufferedWriter(newLog4jPropertiesFile, Charset.defaultCharset())) {
            fileWriter.append(contentStringBuffer.toString()).flush();
        } catch (IOException exception) {
            System.err.println("Error Writng to file");
        }
        
        System.out.println(newLog4jPropertiesFile + " is now created...");
    }


    private void makeDefaultDirectories(int ith) throws IOException {
        Path path = Paths.get(System.getProperty("user.dir") + File.separator +"project"+ith + File.separator +"src");
        System.out.println(path);
        
//        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwxr--");
//        FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(perms);
        
        if( !Files.exists(path) ) {
                Files.createDirectories(path);
        }
        
        Path mainjavaPath = Paths.get(path.toString(), "main/java");
        Path mainrescPath = Paths.get(path.toString(), "test/resources");
        Path testjavaPath = Paths.get(path.toString(), "main/java");
        Path testrescPath = Paths.get(path.toString(), "test/resources");
        
        System.out.println(mainjavaPath);
        System.out.println(mainrescPath);
        System.out.println(testjavaPath);
        System.out.println(testrescPath);                
        
        if( !Files.exists(mainjavaPath) )
            Files.createDirectories(mainjavaPath);
        if( !Files.exists(mainrescPath) )
            Files.createDirectories(mainrescPath);
        if( !Files.exists(testjavaPath) )
            Files.createDirectories(testjavaPath);
        if( !Files.exists(testrescPath) )
            Files.createDirectories(testrescPath);
    }


    public LogstashClientMaker() {
        System.out.println("Service started..");
    }
    
}
