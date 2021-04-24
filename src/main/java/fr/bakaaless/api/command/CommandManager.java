package fr.bakaaless.api.command;

import fr.bakaaless.api.command.annotations.RunCommand;
import fr.bakaaless.api.command.annotations.RunSubCommand;
import fr.bakaaless.api.utils.Pair;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final JavaPlugin pluginInstance;

    private final List<CommandRunner> commands;
    private final Map<String, List<CommandRunner>> subCommands;

    public CommandManager(final JavaPlugin instance) {
        this(instance.getClass());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        final StringBuilder rawArgs = new StringBuilder();
        final StringBuilder builder = new StringBuilder();
        Pair<Boolean, String> concatenateArgs = Pair.from(false, "");
        for (final String arg : args) {
            rawArgs.append(" ").append(arg);
            if (concatenateArgs.getFirst()) {
                if (arg.endsWith("\"") && !arg.endsWith("\\\"") && concatenateArgs.getSecond().equalsIgnoreCase("\"")) {
                    concatenateArgs = Pair.from(false, "");
                    builder.append(" ").append(arg, 0, arg.length() - 1).append("\n");
                } else if (arg.endsWith("'") && !arg.endsWith("\\'") && concatenateArgs.getSecond().equalsIgnoreCase("'")) {
                    concatenateArgs = Pair.from(false, "");
                    builder.append(" ").append(arg, 0, arg.length() - 1).append("\n");
                } else
                    builder.append(" ").append(arg);
            } else {
                if (arg.startsWith("\"") && !arg.endsWith("\"") && !arg.startsWith("\\\"")) {
                    concatenateArgs = Pair.from(true, "\"");
                    builder.append(arg.substring(1));
                } else if (arg.startsWith("'") && !arg.endsWith("'") && !arg.startsWith("\\'")) {
                    concatenateArgs = Pair.from(true, "'");
                    builder.append(arg.substring(1));
                } else
                    builder.append(arg).append("\n");

            }
        }

        String argument = "";
        if (rawArgs.length() > 0) {
            rawArgs.deleteCharAt(0);
            argument = (builder.charAt(builder.length() - 1) == '\n' ? builder.deleteCharAt(builder.length() - 1).toString() : builder.toString()).replace("\\\"", "\"");
        }

        String containsCommand = "";
        Pair<CommandRunner, String> toExec = null;
        boolean isCommandInExec = false;

        for (final CommandRunner commands : this.commands) {
            final RunCommand annotation = getAnnotationCommand(commands);
            if (annotation == null)
                continue;

            if (command.getName().equalsIgnoreCase(annotation.command())) {
                containsCommand = command.getName();
                toExec = Pair.from(commands, command.getName());
                isCommandInExec = true;
                break;
            }
        }

        for (final Map.Entry<String, List<CommandRunner>> subCommands : this.subCommands.entrySet()) {
            if (!containsCommand.isEmpty()) {
                if (!subCommands.getKey().equalsIgnoreCase(containsCommand))
                    continue;
                for (final CommandRunner executor : subCommands.getValue()) {
                    final RunSubCommand annotation = getAnnotationSubCommand(executor);
                    if (annotation == null)
                        continue;
                    for (final String aliases : annotation.subCommand()) {

                        if (!argument.toLowerCase().startsWith(aliases.replace(" ", "\n").toLowerCase()))
                            continue;

                        if (isCommandInExec) {
                            toExec = Pair.from(executor, aliases);
                            isCommandInExec = false;
                        }
                        else if (toExec.getSecond().length() < aliases.length())
                            toExec = Pair.from(executor, aliases);
                    }
                }
            }
        }

        if (toExec != null) {

            String permission;
            RunCommand.ExecutorType executorType;
            int argumentSize;

            if (isCommandInExec) {
                final RunCommand annotation = getAnnotationCommand(toExec.getFirst());
                permission = annotation.permission();
                executorType = annotation.executor();
                argumentSize = annotation.arguments();
            }
            else {
                final RunSubCommand annotation = getAnnotationSubCommand(toExec.getFirst());
                permission = annotation.permission();
                executorType = annotation.executor();
                argumentSize = annotation.arguments();
            }

            if (!sender.hasPermission(permission)) {
                sender.sendMessage(Messages.ERROR_COMMAND_PERMISSION.get());
                return false;
            }

            if (executorType != RunCommand.ExecutorType.ALL) {
                if (executorType == RunCommand.ExecutorType.CONSOLE && sender instanceof Player) {
                    if (isCommandInExec)
                        sender.sendMessage(Messages.ERROR_COMMAND_EXECUTOR_CONSOLE.get(this.reformatUsage(label, getAnnotationCommand(toExec.getFirst()))));
                    else
                        sender.sendMessage(Messages.ERROR_COMMAND_EXECUTOR_CONSOLE.get(this.reformatUsage(label, toExec.getSecond(), getAnnotationSubCommand(toExec.getFirst()))));
                    return false;
                } else if (executorType == RunCommand.ExecutorType.PLAYERS && !(sender instanceof Player)) {
                    if (isCommandInExec)
                        sender.sendMessage(Messages.ERROR_COMMAND_EXECUTOR_PLAYER.get(this.reformatUsage(label, getAnnotationCommand(toExec.getFirst()))));
                    else
                        sender.sendMessage(Messages.ERROR_COMMAND_EXECUTOR_PLAYER.get(this.reformatUsage(label, toExec.getSecond(), getAnnotationSubCommand(toExec.getFirst()))));
                    return false;
                }
            }

            final String finalArg = argument.replace(toExec.getSecond().replace(" ", "\n"), "");


            final List<String> arguments = (finalArg.equals("") ?
                    new ArrayList<>() :
                    new ArrayList<>(Arrays.asList(finalArg.split("\n")))
            );

            if (arguments.size() >= argumentSize) {
                return toExec.getFirst().run(sender, arguments);
            } else {
                if (isCommandInExec)
                    sender.sendMessage(
                            Messages.ERROR_COMMAND_ARGUMENTS.get(
                                    this.reformatUsage(label, getAnnotationCommand(toExec.getFirst())),
                                    String.valueOf(getAnnotationCommand(toExec.getFirst()).arguments() - arguments.size())
                            )
                    );
                else
                    sender.sendMessage(
                            Messages.ERROR_COMMAND_ARGUMENTS.get(
                                    this.reformatUsage(label, toExec.getSecond(), getAnnotationSubCommand(toExec.getFirst())),
                                    String.valueOf(getAnnotationSubCommand(toExec.getFirst()).arguments() - arguments.size())
                            )
                    );
            }
        } else {
            sender.sendMessage(Messages.ERROR_COMMAND_NEXISTS.get(label + " " + rawArgs));
        }
        return false;
    }

    public CommandManager(final Class<? extends JavaPlugin> clazz) {
        this.pluginInstance = JavaPlugin.getPlugin(clazz);
        this.commands = new ArrayList<>();
        this.subCommands = new HashMap<>();
        Messages.PREFIX_ERROR.setMessage("&c&l" + this.pluginInstance.getName() + " &4&l»");
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        final StringBuilder builder = new StringBuilder();
        final StringBuilder rawArgs = new StringBuilder();
        Pair<Boolean, String> concatenateArgs = Pair.from(false, "");
        for (final String arg : args) {
            rawArgs.append(" ").append(arg);
            if (concatenateArgs.getFirst()) {
                if (arg.endsWith("\"") && !arg.endsWith("\\\"") && concatenateArgs.getSecond().equalsIgnoreCase("\"")) {
                    concatenateArgs = Pair.from(false, "");
                    builder.append(" ").append(arg, 0, arg.length() - 1).append(",");
                } else if (arg.endsWith("'") && !arg.endsWith("\\'") && concatenateArgs.getSecond().equalsIgnoreCase("'")) {
                    concatenateArgs = Pair.from(false, "");
                    builder.append(" ").append(arg, 0, arg.length() - 1).append(",");
                } else
                    builder.append(" ").append(arg);
            } else {
                if (arg.startsWith("\"") && !arg.endsWith("\"") && !arg.startsWith("\\\"")) {
                    concatenateArgs = Pair.from(true, "\"");
                    builder.append(arg.substring(1));
                } else if (arg.startsWith("'") && !arg.endsWith("'") && !arg.startsWith("\\'")) {
                    concatenateArgs = Pair.from(true, "'");
                    builder.append(arg.substring(1));
                } else
                    builder.append(arg).append(",");

            }
        }

        String argument = "";
        if (rawArgs.length() > 0) {
            rawArgs.deleteCharAt(0);
            argument = (builder.charAt(builder.length() - 1) == ',' ? builder.deleteCharAt(builder.length() - 1).toString() : builder.toString());
        }

        String containsCommand = "";
        Pair<CommandRunner, String> toExec = null;
        boolean isCommandInExec = false;

        for (final CommandRunner commands : this.commands) {
            final RunCommand annotation = getAnnotationCommand(commands);
            if (annotation == null)
                continue;

            if (command.getName().equalsIgnoreCase(annotation.command())) {
                containsCommand = command.getName();
                toExec = Pair.from(commands, command.getName());
                isCommandInExec = true;
                break;
            }
        }

        for (final Map.Entry<String, List<CommandRunner>> subCommands : this.subCommands.entrySet()) {
            if (!containsCommand.isEmpty()) {
                if (!subCommands.getKey().equalsIgnoreCase(containsCommand))
                    continue;
                for (final CommandRunner executor : subCommands.getValue()) {
                    final RunSubCommand annotation = getAnnotationSubCommand(executor);
                    if (annotation == null)
                        continue;
                    for (final String aliases : annotation.subCommand()) {

                        if (!argument.toLowerCase().startsWith(aliases.replace(" ", ",").toLowerCase()))
                            continue;

                        if (isCommandInExec) {
                            toExec = Pair.from(executor, aliases);
                            isCommandInExec = false;
                        }
                        else if (toExec.getSecond().length() < aliases.length())
                            toExec = Pair.from(executor, aliases);
                    }
                }
            }
        }

        if (toExec != null) {
            final String finalArg = argument.replace(toExec.getSecond().replace(" ", ","), "")
                    .replaceFirst("[,]", "");

            final List<String> arguments = (finalArg.equals("") ?
                    new ArrayList<>() :
                    new ArrayList<>(Arrays.asList(finalArg.split(",")))
            );

            return Optional.ofNullable(toExec.getFirst().tabCompleter(sender, arguments)).orElse(new ArrayList<>());
        }
        return new ArrayList<>();
    }

    @SafeVarargs
    public final void registerRunners(final Class<? extends CommandRunner>... executors) {

        executors:
        for (final Class<? extends CommandRunner> executor : executors) {

            final Annotation[] annotations = executor.getDeclaredAnnotations();
            final List<Annotation> annotationList = Arrays.stream(annotations).filter(
                    annotation -> annotation instanceof RunCommand || annotation instanceof RunSubCommand
            ).collect(Collectors.toList());

            if (annotationList.size() == 0)
                continue;

            final Annotation realAnnotation = annotationList.get(0);
            if (realAnnotation instanceof RunCommand) {

                final RunCommand runCommand = (RunCommand) realAnnotation;
                final String baseCommand = runCommand.command();

                for (final CommandRunner otherCommands : this.commands) {

                    final RunCommand otherRunCommand = otherCommands.getClass().getDeclaredAnnotation(RunCommand.class);
                    if (otherRunCommand == null)
                        continue;

                    if (otherRunCommand.command().equals(baseCommand))
                        continue executors;

                }

                PluginCommand basePluginCommand = this.pluginInstance.getServer().getPluginCommand(baseCommand);
                if (basePluginCommand == null) {
                    try {
                        final Constructor<PluginCommand> c = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
                        c.setAccessible(true);
                        basePluginCommand = c.newInstance(baseCommand, this.pluginInstance);
                        basePluginCommand.setDescription(runCommand.description());
                        basePluginCommand.setAliases(Arrays.asList(runCommand.aliases()));
                        basePluginCommand.setPermission(runCommand.permission());
                        final SimplePluginManager manager = ((SimplePluginManager) Bukkit.getPluginManager());
                        final Field field = manager.getClass().getDeclaredField("commandMap");
                        field.setAccessible(true);
                        ((SimpleCommandMap) field.get(manager)).register(this.pluginInstance.getDescription().getName(), basePluginCommand);
                    } catch (Exception e) {
                        this.pluginInstance.getLogger().log(Level.SEVERE, Messages.ERROR_COMMAND_UNKNOWN.get(), e);
                        continue;
                    }
                }
                basePluginCommand.setExecutor(this);
                basePluginCommand.setTabCompleter(this);

                try {
                    this.commands.add(executor.getConstructor().newInstance());
                    this.subCommands.putIfAbsent(baseCommand.toLowerCase(), new ArrayList<>());
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    this.pluginInstance.getLogger().log(Level.SEVERE, Messages.ERROR_COMMAND_UNKNOWN.get(), e);
                }

            } else if (realAnnotation instanceof RunSubCommand) {

                final RunSubCommand runSubCommand = (RunSubCommand) realAnnotation;
                this.subCommands.putIfAbsent(runSubCommand.command(), new ArrayList<>());

                try {
                    this.subCommands.get(runSubCommand.command()).add(executor.getConstructor().newInstance());
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    this.pluginInstance.getLogger().log(Level.SEVERE, Messages.ERROR_COMMAND_UNKNOWN.get(), e);
                }
            }
        }

    }

    @SafeVarargs
    public final void unregisterRunners(final Class<? extends CommandRunner>... executors) {

        for (final Class<? extends CommandRunner> executor : executors) {

            final Annotation[] annotations = executor.getDeclaredAnnotations();
            final List<Annotation> annotationList = Arrays.stream(annotations).filter(
                    annotation -> annotation instanceof RunCommand || annotation instanceof RunSubCommand
            ).collect(Collectors.toList());

            if (annotationList.size() == 0)
                continue;

            final Annotation realAnnotation = annotationList.get(0);
            if (realAnnotation instanceof RunCommand) {

                CommandRunner toRemove = null;
                for (final CommandRunner allCommands : this.commands) {
                    if (allCommands.getClass().hashCode() == executor.hashCode()) {
                        toRemove = allCommands;
                        break;
                    }
                }

                if (toRemove == null)
                    return;

                this.commands.remove(toRemove);
                RunCommand annotation = getAnnotationCommand(toRemove);
                if (annotation == null)
                    return;
                PluginCommand basePluginCommand = this.pluginInstance.getServer().getPluginCommand(annotation.command());
                if (basePluginCommand == null)
                    return;
                basePluginCommand.setExecutor(null);
                basePluginCommand.setTabCompleter(null);
                final SimplePluginManager manager = ((SimplePluginManager) Bukkit.getPluginManager());
                final Field field;
                try {
                    field = manager.getClass().getDeclaredField("commandMap");
                    field.setAccessible(true);
                    final SimpleCommandMap map = ((SimpleCommandMap) field.get(manager));
                    Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
                    knownCommandsField.setAccessible(true);
                    final HashMap<String, org.bukkit.command.Command> commands = (HashMap<String, org.bukkit.command.Command>) knownCommandsField.get(map);
                    final List<String> names = new ArrayList<>();
                    for (final Map.Entry<String, org.bukkit.command.Command> commandEntry : commands.entrySet()) {
                        if (commandEntry.getValue().getName().equalsIgnoreCase(basePluginCommand.getName())) {
                            names.add(commandEntry.getKey());
                            break;
                        }
                    }
                    basePluginCommand.unregister(map);
                    names.forEach(name ->
                        commands.remove(name, basePluginCommand)
                    );
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    this.pluginInstance.getLogger().log(Level.SEVERE, Messages.ERROR_COMMAND_UNKNOWN.get(), e);
                }

            } else if (realAnnotation instanceof RunSubCommand) {

                Pair<String, CommandRunner> toRemove = null;
                subCommands:
                for (final Map.Entry<String, List<CommandRunner>> subCommands : this.subCommands.entrySet()) {
                    for (final CommandRunner subCommandRunner : subCommands.getValue()) {
                        if (subCommandRunner.getClass().hashCode() == executor.hashCode()) {
                            toRemove = Pair.from(subCommands.getKey(), subCommandRunner);
                            break subCommands;
                        }
                    }
                }

                if (toRemove != null)
                    this.subCommands.get(toRemove.getFirst()).remove(toRemove.getSecond());
            }
        }

    }

    private String reformatUsage(final String command, final RunCommand annotation) {
        return annotation.usage().replace("{command}", command);
    }

    private String reformatUsage(final String command, final String subCommand, final RunSubCommand annotation) {
        return annotation.usage().replace("{command}", command).replace("{subCommand}", subCommand);
    }

    @Nullable
    private static RunCommand getAnnotationCommand(final CommandRunner executor) {
        return getAnnotationCommand(executor.getClass());
    }

    @Nullable
    private static RunCommand getAnnotationCommand(final Class<? extends CommandRunner> executor) {
        return executor.getDeclaredAnnotation(RunCommand.class);
    }

    @Nullable
    private static RunSubCommand getAnnotationSubCommand(final CommandRunner executor) {
        return getAnnotationSubCommand(executor.getClass());
    }

    @Nullable
    private static RunSubCommand getAnnotationSubCommand(final Class<? extends CommandRunner> executor) {
        return executor.getDeclaredAnnotation(RunSubCommand.class);
    }

    public enum Messages {

        PREFIX_ERROR(""),


        ERROR_COMMAND_ARGUMENTS("%prefix_error% &cThe command &6/{0} &cneed &6{1} &cmore &6argument(s)&c."),
        ERROR_COMMAND_EXECUTOR_PLAYER("%prefix_error% &cA player is required to execute the command &6/{0}&c."),
        ERROR_COMMAND_EXECUTOR_CONSOLE("%prefix_error% &cThe console is the only executor which can perform the command  &6/{0}&c."),
        ERROR_COMMAND_NEXISTS("%prefix_error% &cThe command &6/{0} &cdoes not exist."),
        ERROR_COMMAND_PERMISSION("%prefix_error% &cYou do not have the privilege to do that."),
        ERROR_COMMAND_UNKNOWN("%prefix_error% &cOoops, something went wrong.");

        private String message;

        Messages(final String message) {
            this.message = message;
        }

        public void setMessage(final String message) {
            this.message = message;
        }

        public String getFullRaw() {
            return this.message;
        }

        public String get(final String... arguments) {
            String result = this.message;
            for (int i = 0; i < arguments.length; i++)
                result = result.replace("{" + i + "}", arguments[i]);
            return ChatColor.translateAlternateColorCodes('&', result
                    .replace("%prefix_error%", PREFIX_ERROR.message)
            );
        }
    }

}
