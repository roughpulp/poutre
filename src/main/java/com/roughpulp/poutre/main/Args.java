package com.roughpulp.poutre.main;

import com.beust.jcommander.Parameter;

public class Args {
    @Parameter(names = "--script", required = true, description = "path to the script file")
    public String script;
}
