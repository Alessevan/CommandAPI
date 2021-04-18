package fr.bakaaless.api.command.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RunCommand {

    String command();

    String[] aliases() default {""};

    int arguments() default 0;

    String usage() default "{command}";

    String description() default "";

    String permission() default "";

    ExecutorType executor() default ExecutorType.ALL;

    enum ExecutorType {
        ALL,
        PLAYERS,
        CONSOLE
    }

}
