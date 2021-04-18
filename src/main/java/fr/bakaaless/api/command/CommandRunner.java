package fr.bakaaless.api.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface CommandRunner {

    boolean run(final CommandSender sender, final List<String> args);

    List<String> tabCompleter(final CommandSender sender, final List<String> args);

}
