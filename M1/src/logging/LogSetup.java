//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package logging;

import java.io.IOException;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class LogSetup {
    private static Logger logger = Logger.getRootLogger();
    private String logdir;

    public LogSetup(String logdir, Level logLevel) throws IOException {
        this.logdir = logdir;
        this.initialize(logLevel);
    }

    private void initialize(Level logLevel) throws IOException {
        PatternLayout layout = new PatternLayout("%d{ISO8601} %-5p [%t] %c: %m%n");
        FileAppender fileAppender = new FileAppender(layout, this.logdir, true);
        ConsoleAppender consoleAppender = new ConsoleAppender(layout);
        logger.addAppender(consoleAppender);
        logger.addAppender(fileAppender);
        logger.setLevel(logLevel);
    }
}
