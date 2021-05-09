# CommandAPI

## Présentation

<p>➠  Développeur(s) : BakaAless</p>
<p>➠  État : Fini</p>


## Description

➠  API permettant de faciliter la création de commandes avec un plugin.
Le système utilise des annotations afin de créer des commandes et sous-commandes.

➠  Créer une commande facilement :

```java
import fr.bakaaless.api.command.CommandRunner;
import fr.bakaaless.api.command.annotations.RunCommand;
import org.bukkit.command.CommandSender;

import java.util.Collections;

@RunCommand(command = "maNouvelleCommande")
public class MaSuperbeCommande implements CommandRunner {

    @Override
    public boolean run(final CommandSender sender, final List<String> args) {
        sender.sendMessage("Salut, t'as l'air trop cool :)");
        return true;
    }

    @Override
    public List<String> tabCompleter(final CommandSender sender, final List<String> args) {
        return Collections.singletonList("secret");
    }
}
```
➠  Créer une sous-commande simplement via une classe :

```java
import fr.bakaaless.api.command.CommandRunner;
import fr.bakaaless.api.command.annotations.RunCommand;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

@RunSubCommand(command = "maNouvelleCommande", subCommand = {"secret", "mystère"})
public class MaSuperbeSousCommande implements CommandRunner {

    @Override
    public boolean run(final CommandSender sender, final List<String> args) {
        sender.sendMessage("Bien joué tu as trouvé la commande secrète ;)");
        return true;
    }

    @Override
    public List<String> tabCompleter(final CommandSender sender, final List<String> args) {
        return new ArrayList<>();
    }
}
```

➠  Créer un manager de commandes et enregistrer des classes :
```java
import fr.bakaaless.api.command.CommandManager;
import org.bukkit.plugin.JavaPlugin;

public class Main extends JavaPlugin {
 
    private CommandManager commandManager;
    
    @Override
    public void onEnable() {
        this.manager = new CommandManager(this);
        this.manager.registerRunners(MaSuperbeCommande.class, MaSuperbeSousCommande.class);
    }
}
```

## Intégration

[![Release](https://jitpack.io/v/BakaAless/CommandAPI.svg)](https://jitpack.io/#BakaAless/CommandAPI)

➠  Pour intégrer ce code à gradle, en remplaçant `Version` par la version ci-dessus :
```groovy
repositories {
  maven { url 'https://jitpack.io' }
}

dependencies {
  implementation group: 'com.github.BakaAless', name: 'CommandAPI', version: 'VERSION'
}
```

## Licence

CommandAPI is under GPL-3.0 License.
