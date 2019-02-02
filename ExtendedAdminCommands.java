package anon.trollegle;

import java.lang.NumberFormatException;
import java.lang.IllegalArgumentException;

import java.lang.String;
import java.util.Arrays;
import java.util.Locale;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import java.util.function.Predicate; 
import java.util.function.Function; 
import java.util.function.BiFunction;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.*;

import static anon.trollegle.Util.t;
import static anon.trollegle.Template.replace;

// TODO: accept commands without slashes where it is possible
// TODO: Temera's commands
// TODO: override /.saveandkill and the inital --json load to include hug/stab count, commands, and labels

public class ExtendedAdminCommands extends AdminCommands {
    
    private TimerStuff timer = new TimerStuff(m);
    
    private QueryStuff query = new QueryStuff(m);
    
    private Hashtable<String, String> addedCommands = new Hashtable<String, String>();

    public ExtendedAdminCommands(Multi m) {
        super(m);
    }
    
    protected void addAll() {
        super.addAll();
                
        addCommand("extendedhelp", "extendedhelp", "Tells you all the secrets.", 0, (args, target) -> {
          target.schedTell("Did you know that you search for commands whose usage or descriptions contain a phrase or for a command of a specific name?\r\n"
              + "Type \"/.help [phrase/command name]\" to get a shortened help response that only contains relevant commands.\r\n"
              + "Additionally, all of the new commands are marked with the writer's name, so you may type \"/.help Thea\" to get all of my commands.\r\n"
              + "The best way to learn about the new commands is reading the documentation, which describes the uses of the commands and shows example usage.\r\n"
              + "Documentation: https://github.com/TruelyThea/TrollegleDayTwo/blob/master/documentation.md\r\n"
              + "I hope you enjoy using these new features! ^^\r\n"
              + "~Thea");
        });
                
        addCommand("defer", "defer TIME [command]", "Waits TIME before executing [command]. (Thea)", 2,
                (args, target) -> {
                    timer.makeDefer(args, target);
                }, "delay", "setdefer");
                
        addCommand("defers", "defers", "List the currently scheduled defers with their ids. (Thea)", 0,
                (args, target) -> {
                    timer.listDefers(target);
                }, "listdefers");
        
        addCommand("canceldefer", "canceldefer id", "Cancels the defer at id if given, or all the defers if not. (Thea)", 0,
                (args, target) -> {
                    timer.cancelDefers(args, target);
                }, "canceldefers", "cleardefer", "cleardefers");
        
        addCommand("interval", "interval TIME [command]", "Repeats [command] every TIME ms. (Thea)", 2,
                (args, target) -> {
                    timer.makeInterval(args, target);
                }, "setinterval", "int");
        
        // Remembering all the interval id's can become hard, so you can call /.intervals to see them.
        addCommand("intervals", "intervals", "List currently scheduled intervals with their ids. (Thea)", 0,
                (args, target) -> {
                    timer.listIntervals(target);
                }, "listintervals");
                
        addCommand("cancelinterval", "cancelinterval id", "Cancels the interval at id if given, or all the intervals if not. (Thea)", 0,
                (args, target) -> {
                    timer.cancelIntervals(args, target);
                }, "cancelintervals", "clearinterval", "clearintervals");
        
        addCommand("repeat", "repeat TIMES [command]", "Repeats [command] TIMES times. (Thea)", 2,
                (args, target) -> {
                    String commands = argsToString(1, args);
                    int times = 0;
                    
                    try {
                      times = Integer.parseInt(args[0]);
                    } catch (NumberFormatException e) {
                      target.schedTell("invalid integer argument for TIMES");
                      return;
                    }
                    
                    for (int i = 0; i < times; i++) m.command(target, commands);
                    
                    target.schedTell("Completed the repetition");
                }, "rep", "times");
        
        addCommand("addcommand", "addcommand COMMAND [command]", "Makes /!COMMAND a shorthand for [command]. [command] may be an entire command, an initial segment for args to be appended to, or allow the first few args to fill where $0 $1 $2, ... appear. Escape $d... though $0d.... Additionally you may use ... after an arg like '$0...' to get all the arguments from that one (rest operator). (Thea)", 2,
        //"Makes the initial segment of a command available for easy calling. This may be the entire command or may be a proper initial segment that accepts arguments. Use the /. or /! notation. (Thea)", 2,
                (args, target) -> { // This function is really ugly right now.
                    final String command = argsToString(1, args);
                    String name = args[0].toLowerCase(Locale.ROOT);
                    String regex = "\\$(\\d+(\\.\\.\\.)?)";
                    
                    Pattern p = Pattern.compile(regex);
                    Matcher match = p.matcher(command);
                    int max = 0;
                    while (match.find())
                      if (!match.group(1).startsWith("0") ||  match.group(1).length() == (match.group(2) == null ? 1 : 4)) {
                        if (match.group(2) != null) max = Integer.MAX_VALUE;
                        else max = Math.max(max, Integer.parseInt(match.group(1)) + 1);
                      }
                    final int from = max;
                    
                    if ((commands.get(name) == null && aliases.get(name) == null) || addedCommands.get(name) != null) {
                      addedCommands.put(name, command);
                      // TODO: Should I set the required number of arguments to max instead of 0?
                      // Should I add a REQUIRED optional argument to /.addcommand COMMAND REQUIRED [command]
                      // so that it sets the required number of arguments to REQUIRED?
                      addCommand(name, null, null, 0, (innerargs, innertarget) -> {
                        
                        String filledCommand = replace(command, regex, mtch -> {
                          // escape $d... through $0d...
                          if (mtch.length() > (mtch.endsWith("...") ? 4 : 1) && mtch.startsWith("0")) return "$" + mtch.substring(1);
                          
                          if (mtch.endsWith("...")) {
                            // the negative shorthand confuses me sometimes, so I'll just write long hand
                            int index = Integer.parseInt(mtch.substring(0, mtch.length() - 3)); 
                            return index < innerargs.length ? argsToString(index, innerargs) : "";
                          }
                          int index = Integer.parseInt(mtch);
                          return index < innerargs.length ? innerargs[index] : "";
                        });
                        
                        m.command(innertarget, filledCommand + " " + argsToString(from, innerargs));
                      });
                      // target.schedTell("Command added.");
                    } else
                      target.schedTell("There is already a command with that name.");
                }, "command", "setcommand");
                
        addCommand("commands", "commands", "Lists all of the commands that admins have added. These commands will never show up in /!help searches. (Thea)", 0,
                (args, target) -> {
                    Predicate<String> predicate = null;
                    if (args.length > 0) {
                      String filter = argsToString(0, args);
                      predicate = s -> s.contains(filter);
                    }
                    listHashTable(target, addedCommands, "command", predicate);
                }, "listcommands");
        
        // This is useful in any of my higher-order commands that take other commands as arguments
        // because you might want to perform more than one command on interval or perform more than one command with selected users
        // This command may be stacked to run several commands at once: /.then COMMAND /.then COMMAND /.then COMMAND COMMAND
        // Limitations: no 'textual' /word's in the first command, no /.if + /.else in first command, no user-added commands that take a command in the first command.
        addCommand("then", "then [command] [command]", "Performs both commands. (Thea)", 2,
                (args, target) -> {
                    int index = indexOfSecondCommand(args);
                    m.command(target, String.join(" ", Arrays.copyOfRange(args, 0, index)));
                    m.command(target, argsToString(index, args));
                }, "andthen");
                
        addCommand("foreach", "forEach LIST... [command]", "Preforms the command on each element of the list. Fills the command with $[value]'s like a query command, but only accepts the $[function args...] values and the special values: index, value, collection. (Thea)", 2,
                (args, target) -> {
                    query.each(args, target);
                }, "each");
                
        addCommand("noop", "noop ARGS...", "Ignores the arguments (Thea)", 2,
                (args, target) -> {
                    return;
                });
        
        // The following three commands are duplicated from ExtendedUserBehavior.java to allow both the /!command and /command notations.
        // The comments are presented only here.
        
        // This command relays the phrase to yourself, and is an abbreviation of /.annpm SELF [phrase].
        // This is useful for sending yourself another user's properties, as obtained via /.with or /.allwho.
        // It's also necessary inside a /.with or /.allwho block within a command added via /.addcommand because you can't hard code the caller's id.
        addCommand("tell", "tell [phrase]", "This command relays the phrase to the caller. You can also use /tell. (Thea)", 1,
                (args, target) -> {
                    m.annPrivate(target, target.getNumber() + "", argsToString(0, args));
                }, "relay", "inform");
                
        // This command is useful for wrapping commands and phrases, most notably making a user say a regular phrase in /.simulate USER.
        addCommand("do", "do [command/phrase]", "This command has the caller perform the command or say the phrase. You can also use /do. (Thea)", 1,
                (args, target) -> {
                    m.hear(target, argsToString(0, args));
                }, "perform", "doperform");
        
        // This is useful for allowing a simulation of a normal message in /.simulate 
        // or for saying messages that would otherwise be interpreted as a command.
        addCommand("say", "say [phrase]", "This command has the caller say the phrase, regardless of wheter it looks like a command. You can also use /say. (Thea)", 1,
                (args, target) -> {
                    String data = argsToString(0, args);
                    m.relay("normal", target, data);
                    // maybe TODO: if target is muted, relay the data to verbose admins, ... 
                    // (although they'll already see the command); I haven't implemented this because the showVerbose variable in Multi.java
                    // I'm leaning against doing this because I think verbose admins should see only what is typed
                }, "dosay");
        
        // Query Commands
        
        // In the following four "query" commands, to query users and access their properties, PREDICATE may now be either a set label or an arbitrary Polish expression
        
        // The $[values] are replaced by the first selected user so "/.with 0 /.simulate Thea /say The admin has $[patCount] pats"
        // will make Thea say the console dummy's pat count
        // You can get around this by adding commands with /.addcommand. After calling "/.addcommand myPatCount /.with Thea /say Thea has $[patCount] pats"
        // calling "/.simulate 0 /.myPatCount" will make the console dummy say Thea's pat count.
        // This happens because commands in /.addcommand will internally call m.command
        // which will "start the process afresh" with the new caller and the new command, no $[values] replaced yet.
        
        // This command queries all users selected by the PREDICATE
        // then the caller performs the command on each with $[values] replaced by the selected users' properties.
        addCommand("allwho", "allwho PREDICATE [command]", "Does the command with all users who satisfy the PREDICATE. In [command], use $[value] to get a value of the user. (Thea)", 2,
                (args, target) -> {
                  query.allWho(args, target);
                }, "all", "filter", "withall", "withallwho");
        // Example: /.allwho || isVerbose isAdmin /.apm $[number] you either are an admin or want to be an admin ^^
        // Example: /.allwho ALL /.apm $[number] $[nick], your pat count is currently $[patCount].
        
        
        // This command queries the user(s) with USER nick or number, 
        // then performs the command with $[values] replaced by the selected user(s)' properties
        // There are two uses of this command:
        //   (1) Do a command after substituting another user's properties
        // Since all $[values] are replaced immediately, 
        //   (2) you can prepend a /.with USER to a /.allwho or /.simulate to get another user's properties.
        // In both /.with and /.simulate you may repeat users.
        addCommand("with", "with USER... [command]", "Does the command with every USER; you are still the target (caller). In [command], use $[value] to get a value of the USER. (Thea)", 2,
                (args, target) -> {
                  query.with(args, target);
                });
        // Example: /.with NRP CL Bibi PUB 0 /.apm $[number] Your pat count is currently at $[patCount] and your hug count is currently at $[hugCount]
        // Example: /.with 0 /say The admin currently has only $[patCount] pats.
        
        
        // This command will have the caller do the command if the USER satisfies the PREDICATE
        // This command also can be used to see whether a USER exists, with /.if USER 1 /tell yes
        // also replaces $[value]'s with the selected user's properties.
        addCommand("if", "if USER PREDICATE [command]", "Does the command if the USER exists and satisfies the PREDICATE. Optionally add an /.else after the command to do when the PREDICATE doesn't hold (but the user exists), and possible stacking: /.if USER PRED command /.else /.if USER PRED command /.else command. (Thea)", 3,
                (args, target) -> {
                  query.ifThen(args, target);
                }, "onlyif", "ifthen", "withif", "ifelse");
        // Example: /.if NRP ! isAdmin /.then /.d NRP /.g Hello ^^
        // Example: 
        //     /.setLabel doGreet true
        //     /.addCommand testForQs /.if qs doGreet /.then /say qs has finally joined! Welcome, qs. ^^ /.setlabel doGreet false
        //     /.simulate 0 /.setinterval 15000 /.testForQs
        // Explanation: You can't know the interval id ahead of time to stop the interval, so we couldn't necessarily call /.cancelInterval id
        //     the simulate call is just so we don't the "user doesn't exist" alert every 15s
        //     for expressions, "true" is "1" is always true, "false" is "0" is always false
        
        
        // This command queries the user(s) with USER nick or number. 
        // then makes that user(s) perform the command with $[values] replaced by the selected users' properties.
        addCommand("simulate", "simulate USER... [command]", "Has the USER do the [command]. In [command], use $[value] to get the value of the USER. (Thea)", 2,
                (args, target) -> {
                  query.simulate(args, target);
                }, "sim");
        // Example: /.then /.g Were you looking for this command? /.simulate Shĭz /.help thea
        // Example: /.setinterval 15000 /.simulate 13 /n $[randomPony]
        
        
        // So you can make more complex predicate query-selectors, determined by the Polish expression.
        // now you can use arbitrary Polish expressions in /.allwho and /.if, without setting a label
        addCommand("setlabel", "setlabel LABEL [Polish]", "Use existing labels, !, ||, &&, ->, and <-> as symbols. (Thea)", 2,
                (args, target) -> {
                  query.setLabel(args, target);
                }, "label", "addlabel");
                
        addCommand("labels", "labels", "Lists all of the labels that admins have added. (Thea)", 0,
                (args, target) -> {
                    Predicate<String> predicate = null;
                    if (args.length > 0) {
                      String filter = argsToString(0, args);
                      predicate = s -> s.contains(filter);
                    }
                    listHashTable(target, query.getLabels(), "label", predicate);
                }, "listlabels");
        
        addCommand("labelhelp", "labelhelp", "spells out the documentation for labels. (Thea)", 0,
                (args, target) -> {
                  Enumeration<String> enumer = query.tokens();
                  String listPred = enumer.nextElement();
                  while (enumer.hasMoreElements()) listPred = listPred + ", " + enumer.nextElement();
                  
                  Enumeration<String> val = query.props();
                  String listVal = val.nextElement();
                  while (val.hasMoreElements()) listVal = listVal + ", " + val.nextElement();
                  
                  val = query.funs();
                  String listFuns = val.nextElement();
                  while (val.hasMoreElements()) listFuns = listFuns + ", " + val.nextElement();
                  
                  target.schedTell("Here are some docs: \r\n"
                        + "The available labels are " + listPred + ", \r\n\r\n" 
                        + "the values available are " + listVal + ",\r\n\r\n" 
                        + "and the functions available are " + listFuns + ".\r\n\r\n"
                        + "Polish notation for labels is also known as prefix notation because all operators are listed before their operands. "
                        + "This notation allows complete omission of commas and parentheses. In fact, they aren't allowed here. Read more here https://en.wikipedia.org/wiki/Polish_notation.\r\n"
                        + "For example, you may write \"/.setlabel label || ! doesShowNumbers && isAdmin isVerbose\" for (!doesShowNumbers) || (isAdmin && isVerbose). \r\n\r\n" 
                        + "In commands you may write $[value] to get the selected user's value. \r\n" 
                        + "For example you could write \"/.allwho isAdmin /msg 0 $[nick] is an Admin with $[patCount] pats.\" \r\n\r\n"
                        + "Commands that take another command are stackable; however, $[value] will be replaced by the selected user in the largest scope's $[value]. \r\n"
                        + "For example, \"/.with 0 /.simulate NRP /msg xzxzy The Admin currently has $[hugCount] hugs.\" will have NRP send the Admin's hug count, not NRP's because the Admin is queried first. \r\n"
                        + "Hopefully more docs will be made soon. You could also read the comments in the source code file.");
                }, "labelshelp");
                
        sanityCheck();
    }
    
    private void listHashTable(MultiUser target, Hashtable<String, String> hash, String type, Predicate<String> predicate) {
        if (predicate == null) predicate = s -> true;
        Enumeration<String> keys = hash.keys();
        String result = "Here are your added " + type + "s:", key, value;
        while (keys.hasMoreElements()) {
          key = keys.nextElement();
          value = hash.get(key);
          if (predicate.test(value) || predicate.test(key))
            result += "\r\n" + key + ": " + value;
        }
        result += "\r\n--end--";
        target.schedTell(result);
    }
    
    private int indexOfSecondCommand(String[] args) {
      ArrayList<Command> oneArys = new ArrayList<Command>();
      String[] oneAryNames = {"defer", "interval", "repeat", "addcommand", "foreach", "allwho", "with", "simulate", "if"};
      for (int i = 0; i < oneAryNames.length; i++)
        oneArys.add(commands.get(oneAryNames[i]));
      
      if (!args[0].startsWith("/"))
        return 1;
      
      int count = 0, index = 0;
      for (index = 0; index < args.length; index++) {
          String word = args[index];
          if (word.startsWith("/")) {
            if (count == -1) return index;
            if (word.startsWith("/.")) {
              String command = word.substring(2).toLowerCase();
              if (oneArys.contains(commands.get(command)) || oneArys.contains(aliases.get(command)) || command.equals("else"))
                count += 1 - 1;
              else if (command.equals("then") || command.equals("andthen"))
                count += 2 - 1;
              else
                count += 0 - 1;
            } else
              count += 0 - 1;
          }
      }
      
      return 1;
    }
    
}