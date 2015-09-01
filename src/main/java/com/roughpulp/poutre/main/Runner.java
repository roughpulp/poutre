package com.roughpulp.poutre.main;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Charsets;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class Runner {

    private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

    public static void main(final String... argv) throws Exception {
        org.apache.log4j.Logger.getRootLogger().removeAllAppenders();
        org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
        org.apache.log4j.Logger.getRootLogger().addAppender(new ConsoleAppender(new PatternLayout("%d{yyyy-MM-dd HH:mm:ss} [%t] %-5p %c{1} %x - %m%n")));
        LOGGER.info("starting ...");

        final Args args = new Args();
        {
            final JCommander jCommander = new JCommander(args);
            try {
                jCommander.parse(argv);
            } catch (ParameterException ex) {
                System.err.println(ex.getMessage());
                final StringBuilder sb = new StringBuilder();
                jCommander.usage(sb);
                System.err.println(sb.toString());
                System.exit(1);
            }
        }
        runJsScript(new File(args.script));
        LOGGER.info("finished");
    }

    public static void runJsScript(File scriptFile) throws Exception {
        LOGGER.info("running script " + scriptFile.getPath() + " ...");
        try ( final Reader scriptIn = new InputStreamReader(new FileInputStream(scriptFile), Charsets.UTF_8) ) {
            runJsScript(scriptFile.getName(), scriptIn);
        }
    }

    public static void runJsScript(String scriptName, Reader scriptIn) throws Exception {
        final Context ctx = Context.enter();
        try {
            final Scriptable scope = ctx.initStandardObjects();
            final Script script = ctx.compileReader(scriptIn, scriptName, 1, null);
            script.exec(ctx, scope);
        } finally {
            Context.exit();
        }
    }
}
