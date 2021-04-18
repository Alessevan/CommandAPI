package fr.bakaaless.api.command.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RunSubCommand {

    String command();

    String[] subCommand();

    int arguments() default 0;

    String usage() default "{command} {subCommand}";

    String permission() default "";

    RunCommand.ExecutorType executor() default RunCommand.ExecutorType.ALL;

}
