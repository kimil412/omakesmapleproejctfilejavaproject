package what.the.fox.say.copy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

public class LoggerFactorySkeleton {

    private static LoggerFactorySkeleton instance = null;
    
    public static LoggerFactorySkeleton getInstance() {
        if( instance == null ) {
            instance = new LoggerFactorySkeleton();
        }

        return instance;
    }

    public Logger getLogger() {
        return LoggerFactory.getLogger("appenderlogger");
    }
    
    public String getLoggerImplementationLibraryName(){
        StaticLoggerBinder binder = StaticLoggerBinder.getSingleton();
        return binder.getLoggerFactoryClassStr();
    }
    
    public boolean isLog4jImplementationBinded(){
        return getLoggerImplementationLibraryName().toLowerCase().contains("log4j");
    }
    
    public boolean isLogbackImplementationBinded(){
        return getLoggerImplementationLibraryName().toLowerCase().contains("logback");
    }
}
