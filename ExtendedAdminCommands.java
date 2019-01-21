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

// TODO: in time commands accept 1s, 1m, 1h, 1d forms
// TODO: accept commands without slashes where it is possible
// TODO: Temera's commands
// TODO: /roll /ship /coin
// TODO: make /.simulate USER /say alert user what was said on their behalf
// TODO: make /.addcommand [command] first fill arguments $1 $2 $3, ..., then append the rest

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
                }, "rep");
        
        addCommand("addcommand", "addcommand COMMAND [command]", "Makes the initial segment of a command available for easy calling. This may be the entire command or may be a proper initial segment that accepts arguments. Use the /. or /! notation. (Thea)", 2,
                (args, target) -> {
                    final String command = argsToString(1, args);
                    String name = args[0].toLowerCase(Locale.ROOT);
                    
                    if ((commands.get(name) == null && aliases.get(name) == null) || addedCommands.get(name) != null) {
                      addedCommands.put(name, command);
                      addCommand(name, null, null, 0, (innerargs, innertarget) -> {
                        m.command(innertarget, command + " " + argsToString(0, innerargs));
                      });
                      target.schedTell("Command added.");
                    } else
                      target.schedTell("There is already a command with that name.");
                }, "command", "setcommand");
                
        addCommand("commands", "commands", "Lists all of the commands that admins have added. These commands will never show up in /!help searches. (Thea)", 0,
                (args, target) -> {
                    listHashTable(target, addedCommands, "command");
                }, "listcommands");
        
        // This is useful in any of my higher-order commands that take other commands as arguments
        // because you might want to perform more than one command on interval or perform more than one command with selected users
        // This command may be stacked to run several commands at once: /.then COMMAND /.then COMMAND /.then COMMAND COMMAND
        addCommand("then", "then [command] [command]", "Performs both commands. The second command starts at the first argument after the first argument that begins with /. (Thea)", 2,
                (args, target) -> {
                    int index = 1;
                    for (index = 1; index < args.length; index++)
                      if (args[index].startsWith("/"))
                        break;
                    if (index == args.length)
                      index = 1;
                    m.command(target, String.join(" ", Arrays.copyOfRange(args, 0, index)));
                    m.command(target, argsToString(index, args));
                }, "andthen");
        
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
                }, "dosay");
        
        // This command queries all users selected by the PREDICATE
        // then performs the command on each with $[values] replaced by the selected users' properties.
        addCommand("allwho", "allwho PREDICATE [command]", "Does the command with all users who satisfy the PREDICATE. In [command], use $[value] to get a value of the user. (Thea)", 2,
                (args, target) -> {
                  query.allWho(args, target);
                }, "all", "filter");
        
        // This command queries the user(s) with USER nick or number, 
        // then performs the command with $[values] replaced by the selected users' properties
        // There are two uses of this command:
        //   (1) Do a command after substituting another user's properties
        // Since all $[values] are replaced immediately, 
        //   (2) you can prepend a /.with USER to a /.allwho or /.simulate to get another user's properties.
        addCommand("with", "with USER... [command]", "Does the command with every USER; you are still the target (caller). In [command], use $[value] to get a value of the USER. (Thea)", 2,
                (args, target) -> {
                  query.with(args, target);
                });
      
        addCommand("if", "if USER PREDICATE [command]", "Does the command if the USER satisfies the PREDICATE. (Thea)", 3,
                (args, target) -> {
                  query.ifThen(args, target);
                }, "onlyif", "ifthen");

        // This command queries the user(s) with USER nick or number. 
        // then makes that user perform the command with $[values] replaced by the selected users' properties.
        addCommand("simulate", "simulate USER [command]", "Has the USER do the [command]. In [command], use $[value] to get the value of the USER. (Thea)", 2,
                (args, target) -> {
                  query.simulate(args, target);
                });
                
        // So you can make more complex predicate query-selectors, determined by the Polish expression.
        addCommand("setlabel", "setlabel LABEL [Polish]", "Use existing labels, !, ||, &&, ->, and <-> as symbols. (Thea)", 2,
                (args, target) -> {
                  query.setLabel(args, target);
                }, "label", "addlabel");
                
        addCommand("labels", "labels", "Lists all of the labels that admins have added. (Thea)", 0,
                (args, target) -> {
                    listHashTable(target, query.getLabels(), "label");
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
    
    private void listHashTable(MultiUser target, Hashtable<String, String> hash, String type) {
        Enumeration<String> keys = hash.keys();
        String result = "Here are your added " + type + "s:", key;
        while (keys.hasMoreElements()) {
          key = keys.nextElement();
          result += "\r\n" + key + ": " + hash.get(key);
        }
        result += "\r\n--end--";
        target.schedTell(result);
    }
    
}