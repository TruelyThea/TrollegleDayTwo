package anon.trollegle;

import java.lang.NumberFormatException;
import java.lang.String;
import java.util.ArrayList;
import java.lang.IndexOutOfBoundsException;

import java.util.Hashtable;
import java.util.HashSet;
import java.util.function.Predicate; 
import java.util.function.Function; 
import java.util.function.BiFunction;
import java.lang.IllegalArgumentException;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.Iterator;

import java.io.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.ObjIntConsumer;
import java.util.Locale;

import static anon.trollegle.Util.t;

import java.util.concurrent.ThreadLocalRandom;

public class ExtendedAdminCommands extends AdminCommands {//tell, do, say?, {NAMES... }
    
    private TimerStuff timer = new TimerStuff(m);
    
    private Polish<MultiUser> labels = new Polish<MultiUser>();
    
    private Hashtable<String, Function<MultiUser, String>> values = new Hashtable<String, Function<MultiUser, String>>();
    private Hashtable<String, BiFunction<MultiUser, String[], String>> funs = new Hashtable<String, BiFunction<MultiUser, String[], String>>();
    
    private Hashtable<String, String> addedCommands = new Hashtable<String, String>();
    private Hashtable<String, String> addedLabels = new Hashtable<String, String>();

    public ExtendedAdminCommands(Multi m) {
        super(m);
        
        labels.put("isAdmin", p -> m.isAdmin(p));
        labels.put("isVerboseAdmin", p -> m.isVerboseAdmin(p));
        labels.put("isCreated", p -> p.isCreated());
        labels.put("isConnected", p -> p.isConnected());
        labels.put("isQuestionMode", p -> p.isQuestionMode());
        labels.put("isPulse", p -> p.isPulse());
        labels.put("isPulseEver", p -> p.isPulseEver());
        labels.put("isDummy", p -> p.isDummy());
        labels.put("isTyping", p -> p.isTyping());
        labels.put("isAccepted", p -> p.isAccepted());
        labels.put("isMuted", p -> p.isMuted());
        labels.put("isVerbose", p -> p.isVerbose());
        labels.put("isFlash", p -> p.isFlash());
        labels.put("isBot", p -> p.isBot());
        labels.put("doesShowNumbers", p -> p.showsNumbers());
        labels.put("isInformed", p -> p.isInformed());
        labels.put("isLurker", p -> p.isLurker());
        labels.put("ALL", p -> true);
        
        values.put("patCount", p -> "" + p.patCount);
        values.put("lastPat", p -> "" + p.lastPat);
        values.put("hugCount", p -> {
          ExtendedCaptchaMulti.ExtendedCaptchaUser c = (ExtendedCaptchaMulti.ExtendedCaptchaUser) p;
          return "" + c.hugCount;
        });
        values.put("lastHug", p -> {
          ExtendedCaptchaMulti.ExtendedCaptchaUser c = (ExtendedCaptchaMulti.ExtendedCaptchaUser) p;
          return "" + c.lastHug;
        });
        values.put("stabCount", p -> {
          ExtendedCaptchaMulti.ExtendedCaptchaUser c = (ExtendedCaptchaMulti.ExtendedCaptchaUser) p;
          return "" + c.stabCount;
        });
        values.put("lastStab", p -> {
          ExtendedCaptchaMulti.ExtendedCaptchaUser c = (ExtendedCaptchaMulti.ExtendedCaptchaUser) p;
          return "" + c.lastStab;
        });
        values.put("number", p -> "" + p.getNumber());
        values.put("nick", p -> p.getNick());
        values.put("displayNick", p -> p.getDisplayNick());
        values.put("pulse", p -> p.getPulseWords());
        values.put("messageCount", p -> "" + p.getMsgCount());
        values.put("kickReason", p -> p.getKickReason());
        
        // This is useful for "having fun".
        values.put("randomPony", p -> {
          String[] ponies = {"TwilightSparkle", "Applejack", "Fluttershy", "Rarity", "PinkiePie", "RainbowDash", "Spike", "AppleBloom", "Scootaloo", "SweetieBelle"};
          int index = ThreadLocalRandom.current().nextInt(0, ponies.length);
          return ponies[index];
        });
        
        values.put("isAdmin", p -> "" + m.isAdmin(p));
        values.put("isVerboseAdmin", p -> "" + m.isVerboseAdmin(p));
        values.put("isCreated", p -> "" + p.isCreated());
        values.put("isConnected", p -> "" + p.isConnected());
        values.put("isQuestionMode", p -> "" + p.isQuestionMode());
        values.put("isPulse", p -> "" + p.isPulse());
        values.put("isPulseEver", p -> "" + p.isPulseEver());
        values.put("isDummy", p -> "" + p.isDummy());
        values.put("isTyping", p -> "" + p.isTyping());
        values.put("isAccepted", p -> "" + p.isAccepted());
        values.put("isMuted", p -> "" + p.isMuted());
        values.put("isVerbose", p -> "" + p.isVerbose());
        values.put("isFlash", p -> "" + p.isFlash());
        values.put("isBot", p -> "" + p.isBot());
        values.put("doesShowNumbers", p -> "" + p.showsNumbers());
        values.put("isInformed", p -> "" + p.isInformed());
        values.put("isLurker", p -> "" + p.isLurker());
       
        funs.put("choose", (p, args) -> args[(int) Math.floor(Math.random()*(double)args.length)]);
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
        addCommand("then", "then COMMAND COMMAND", "Performs both commands. The second command starts at the first argument after the first argument that begins with /. (Thea)", 2,
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
        
        // This command queries all users selected by the LABAL predicate, 
        // then performs the command on each with $[values] replaced by the selected users' properties.
        addCommand("allwho", "allwho LABEL [command]", "Does the command with all users who satisfy LABEL predicate. In [command], use $[value] to get a value of the user. (Thea)", 2,
                (args, target) -> {
                  Predicate<MultiUser> pred = labels.get(args[0]);
                  String commands = argsToString(1, args);
                  if (pred == null) {
                    target.schedTell("That label " + args[0] + " wasn't recognized.");
                    return;
                  }
                  
                  perform(target, pred, commands);
                  
                  // I used to tell the caller that the command was all done, but it didn't really indicate 
                  // whether the command was successful so it didn't seem that useful
                  // esp. since it would say the same message regardless of how many users were queried
                  // `/.with NonexistantUser command` would do nothing with any user and still say "All done."
                  // target.schedTell("All done.");
                }, "all", "filter");
        
        // This command queries the user(s) with USER nick or number, 
        // then performs the command with $[values] replaced by the selected users' properties
        // There are two uses of this command:
        //   (1) Do a command after substituting another user's properties
        // Since all $[values] are replaced immediately, 
        //   (2) you can prepend a /.with USER to a /.allwho or /.simulate to get another user's properties.
        addCommand("with", "with USER... [command]", "Does the command with every USER; you are still the target (caller). In [command], use $[value] to get a value of the USER. (Thea)", 2,
                (args, target) -> {
                  // find the index of the command so we can perform it with each user before it
                  int command = 1;
                  for (command = 1; command < args.length; command++)
                    if (args[command].indexOf("/") == 0)
                      break;
                  // defaults to one USER and the command starts on the second arg
                  if (command == args.length) command = 1;
                  
                  final int loc = command;
                  
                  String commands = argsToString(command, args);
                  
                  Predicate<MultiUser> recipients = p -> {
                    for (int i = 0; i < loc; i++) {
                      if (("" + p.getNumber()).equals(args[i]) || p.getNick().equals(args[i])) return true;
                    }
                    
                    return false;
                  };
                  
                  perform(target, recipients, commands);
                  
                  // target.schedTell("All done.");
                });

        // This command queries the user(s) with USER nick or number. 
        // then makes that user perform the command with $[values] replaced by the selected users' properties.
        addCommand("simulate", "simulate USER [command]", "Has the USER do the [command]. In [command], use $[value] to get the value of the USER. (Thea)", 2,
                (args, target) -> {
                  Predicate<MultiUser> pred = p -> (("" + p.getNumber()).equals(args[0]) || p.getNick().equals(args[0]));
                  String commands = argsToString(1, args);
                  
                  MultiUser user;
                  
                  perform(pred, commands);
                  
                  // target.schedTell("All done.");
                });
                
        // So you can make more complex predicate query-selectors, determined by the Polish expression.
        addCommand("setlabel", "setlabel LABEL [Polish]", "Use existing labels, !, ||, &&, ->, and <-> as symbols. (Thea)", 2,
                (args, target) -> {
                  try {
                    labels.put(args[0], Arrays.copyOfRange(args, 1, args.length));
                    addedLabels.put(args[0], argsToString(1, args));
                    target.schedTell("Label Added.");
                  } catch (IllegalArgumentException e) {
                    target.schedTell("There was a problem parsing your label: " + e.getMessage());
                  }
                }, "label", "addlabel");
                
        addCommand("labels", "labels", "Lists all of the labels that admins have added. (Thea)", 0,
                (args, target) -> {
                    listHashTable(target, addedLabels, "label");
                }, "listlabels");
        
        addCommand("labelhelp", "labelhelp", "spells out the documentation for labels. (Thea)", 0,
                (args, target) -> {
                  Enumeration<String> enumer = labels.tokens();
                  String listPred = enumer.nextElement();
                  while (enumer.hasMoreElements()) listPred = listPred + ", " + enumer.nextElement();
                  
                  Enumeration<String> val = values.keys();
                  String listVal = val.nextElement();
                  while (val.hasMoreElements()) listVal = listVal + ", " + val.nextElement();
                  
                  val = funs.keys();
                  String listFuns = val.nextElement();
                  while (val.hasMoreElements()) listFuns = listFuns + ", " + val.nextElement();
                  
                  target.schedTell("Here are some docs: \r\n"
                        + "The available labels are " + listPred + ", \r\n\r\n" 
                        + "the values available are " + listVal + ",\r\n\r\n" 
                        + "and the functions available are " + listFuns + ".\r\n\r\n"
                        + "Polish notation for labels is also known as prefix notation because all operators are listed before their operands. "
                        + "This notation allows complete ommision of commas and parentheses. In fact, they aren't allowed here. Read more here https://en.wikipedia.org/wiki/Polish_notation.\r\n"
                        + "For example, you may write \"/.setlabel label || ! doesShowNumbers && isAdmin isVerbose\" for (!doesShowNumbers) || (isAdmin && isVerbose). \r\n\r\n" 
                        + "In commands you may write $[value] to get the selected user's value. \r\n" 
                        + "For example you could write \"/.allwho isAdmin /msg 0 $[nick] is an Admin with $[patCount] pats.\" \r\n\r\n"
                        + "Commands that take another command are stackable; however, $[value] will be replaced by the selected user in the largest scope's $[value]. \r\n"
                        + "For example, \"/.with 0 /.simulate NRP /msg xzxzy The Admin currently has $[hugCount] hugs.\" will have NRP send the Admin's hug count, not NRP's because the Admin is queried first. \r\n"
                        + "Hopefully more docs will be made soon. You could also read the comments in the source code file.");
                }, "labelshelp");
                
        sanityCheck();
    }
    
    // This method has source perform commands after replacing $[value]'s with recipient's properties.
    private void perform(MultiUser source, MultiUser recipient, String commands) {
          String uCommands = commands;
          
          String find = "\\$\\[(\\w+)([^\\]]*)\\]";
          Pattern p = Pattern.compile(find); // \\$\\[(\\w+)\\]
          Matcher match = p.matcher(uCommands);
          
          while (match.find()) {
            String property = match.group(1);
            Function<MultiUser, String> prop = values.get(property);
            if (prop != null) {
              uCommands = uCommands.replaceFirst(find, prop.apply(recipient));
            } else {
              BiFunction<MultiUser, String[], String> fun = funs.get(property);
              // TODO: split after substring from length of first match to \\s+
              String[] args = match.group(2).substring(1).split("\\s+");
              // System.out.println(args[0] + " " + match.group(2));
              uCommands = uCommands.replaceFirst(find, fun != null ? fun.apply(recipient, args) : "");
            }
            match = p.matcher(uCommands);
          }
          
          m.command(source, uCommands);
    }
    
    // This method is used to query-select recipients and perform a command on them.
    private void perform(MultiUser source, Predicate<MultiUser> recipients, String commands) {
        synchronized (m.users) {
            // Iterating over m.users array would skip the consoleDummy. This is solved by iterating over m.allUsers.
            Iterator<MultiUser> users = m.allUsers.iterator();
            MultiUser u = null;
            while (users.hasNext()) {
                u = users.next();
                if (recipients.test(u)) perform(source, u, commands);
            }
        }
    }
    
    // This method is used to query-select targets and make them perform a command.
    private void perform(Predicate<MultiUser> target, String commands) {
        synchronized (m.users) {
            Iterator<MultiUser> users = m.allUsers.iterator();
            MultiUser u = null;
            while (users.hasNext()) {
                u = users.next();
                if (target.test(u)) perform(u, u, commands);
            }
        }
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