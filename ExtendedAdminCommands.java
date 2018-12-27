package anon.trollegle;

import java.util.Timer;
import java.util.TimerTask;

import java.lang.NumberFormatException;
import java.lang.String;
import java.util.ArrayList;
import java.lang.IndexOutOfBoundsException;

import java.util.Hashtable;
import java.util.HashSet;
import java.util.function.Predicate; 
import java.util.function.Function; 
import java.lang.IllegalArgumentException;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.util.Iterator;

import java.io.*;
import java.util.Arrays;//important
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.ObjIntConsumer;
import java.util.Locale;

import static anon.trollegle.Util.t;

import java.util.concurrent.ThreadLocalRandom;

public class ExtendedAdminCommands extends AdminCommands {
  
    private Timer timer = null;
    private Timer findNew = null;
    private String headlines[] = null;
    
    private ArrayList<Timer> intervals = new ArrayList<Timer>();
    private Hashtable<Integer, String> intervalCommands = new Hashtable<Integer, String>();

    private ArrayList<Timer> defers = new ArrayList<Timer>();
    private Hashtable<Integer, String> deferCommands = new Hashtable<Integer, String>();
    
    private Hashtable<String, Predicate<MultiUser>> labels = new Hashtable<String, Predicate<MultiUser>>();
    private Hashtable<String, Function<MultiUser, String>> values = new Hashtable<String, Function<MultiUser, String>>();
    
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
                    makeTimer(args, target, defers, deferCommands, "defer", 0, 250);
                }, "delay", "setdefer");
                
        addCommand("defers", "defers", "List the currently scheduled defers with their ids. (Thea)", 0,
                (args, target) -> {
                    listTimers(target, defers, deferCommands, "defer");
                }, "listdefers");
        
        addCommand("canceldefer", "canceldefer id", "Cancels the defer at id if given, or all the defers if not. (Thea)", 0,
                (args, target) -> {
                    cancelTimers(args, target, defers, "defer");
                }, "canceldefers");
        
        addCommand("interval", "interval TIME [command]", "Repeats [command] every TIME ms. (Thea)", 2,
                (args, target) -> {
                    makeTimer(args, target, intervals, intervalCommands, "interval", 250, 0);
                }, "setinterval", "int");
        
        // Remembering all the interval id's can become hard, so you can call /.intervals to see them.
        addCommand("intervals", "intervals", "List currently scheduled intervals with their ids. (Thea)", 0,
                (args, target) -> {
                    listTimers(target, intervals, intervalCommands, "interval");
                }, "listintervals");
                
        addCommand("cancelinterval", "cancelinterval id", "Cancels the interval at id if given, or all the intervals if not. (Thea)", 0,
                (args, target) -> {
                    cancelTimers(args, target, intervals, "interval");
                }, "cancelintervals");
        
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
        
        addCommand("addcommand", "addcommand COMMAND [command]", "Makes the zero-argument command available for easy calling. Use the /. or /! notation. (Thea)", 2,
                (args, target) -> {
                    final String command = argsToString(1, args);
                    String name = args[0].toLowerCase(Locale.ROOT);
                    
                    if ((commands.get(name) == null && aliases.get(name) == null) || addedCommands.get(name) != null) {
                      addedCommands.put(name, command);
                      addCommand(name, null, null, 0, (__, innertarget) -> {
                        m.command(innertarget, command);
                      });
                      target.schedTell("Command added.");
                    } else
                      target.schedTell("There is already a command with that name.");
                }, "command", "setcommand");
                
        addCommand("commands", "commands", "Lists all of the commands that admins have added. These commands will never show up in /!help searches. (Thea)", 0,
                (args, target) -> {
                    listHashTable(target, addedCommands, "command");
                }, "listcommands");
        
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
                  
                  target.schedTell("All done.");
                }, "all");
        
        // This command queries the user(s) with USER nick or number, 
        // then performs the command with $[values] replaced by the selected users' properties
        // There are two uses of this command:
        //   (1) Do a command after substituting another user's properties
        // Since all $[values] are replaced immediately, 
        //   (2) you can prepend a /.with USER to a /.allwho or /.simulate to get another user's properties.
        addCommand("with", "with USER [command]", "Does the command with USER; you are still the target (caller). In [command], use $[value] to get a value of the USER. (Thea)", 2,
                (args, target) -> {
                  Predicate<MultiUser> pred = p -> (("" + p.getNumber()).equals(args[0]) || p.getNick().equals(args[0]));
                  String commands = argsToString(1, args);
                  
                  perform(target, pred, commands);
                  
                  target.schedTell("All done.");
                });

        // This command queries the user(s) with USER nick or number. 
        // then makes that user perform the command with $[values] replaced by the selected users' properties.
        addCommand("simulate", "simulate USER [command]", "Has the USER do the [command]. In [command], use $[value] to get the value of the USER. (Thea)", 2,
                (args, target) -> {
                  Predicate<MultiUser> pred = p -> (("" + p.getNumber()).equals(args[0]) || p.getNick().equals(args[0]));
                  String commands = argsToString(1, args);
                  
                  MultiUser user;
                  
                  perform(pred, commands);
                  
                  target.schedTell("All done.");
                });
                
        // So you can make more complex predicate query-selectors, determined by the Polish expression.
        addCommand("setlabel", "setlabel LABEL [Polish]", "Use existing labels, !, ||, &&, ->, and <-> as symbols. (Thea)", 2,
                (args, target) -> {
                  try {
                    labels.put(args[0], makeCondition(Arrays.copyOfRange(args, 1, args.length)));
                    addedLabels.put(args[0], argsToString(1, args));
                    target.schedTell("Label Added.");
                  } catch (IllegalArgumentException e) {
                    target.schedTell("There was a problem parsing your label: " + e.getMessage());
                  }
                }, "label");
                
        addCommand("labels", "labels", "Lists all of the labels that admins have added. (Thea)", 0,
                (args, target) -> {
                    listHashTable(target, addedLabels, "label");
                }, "listlabels");
        
        addCommand("labelhelp", "labelhelp", "spells out the documentation for labels. (Thea)", 0,
                (args, target) -> {
                  Enumeration<String> enumer = labels.keys();
                  String listPred = enumer.nextElement();
                  while (enumer.hasMoreElements()) listPred = listPred + ", " + enumer.nextElement();
                  
                  Enumeration<String> val = values.keys();
                  String listVal = val.nextElement();
                  while (val.hasMoreElements()) listVal = listVal + ", " + val.nextElement();
                  
                  
                  target.schedTell("Here are some docs: \r\n"
                        + "The available labels are " + listPred + ", \r\n\r\n" 
                        + "and the values available are " + listVal + ".\r\n\r\n" 
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
    
    // This method is used to recursively build up Predicates with the meaning given by the expression args.
    // In other words, it parses a Polish expression and returns the associated Predicate Function.
    // See https://en.wikipedia.org/wiki/Polish_notation
    // We use Polish notation because it's simple to parse, and doesn't require parentheses or commas.
    // ...like I'm going to write an infix parser, no way! :P
    // These Predicates are used as selectors in queries and values in $[]'s.
    private Predicate<MultiUser> makeCondition(String[] args) throws IllegalArgumentException { // Polish parser
      // this code checks that the expression is well-formed
      if (count(args) != -1) throw new IllegalArgumentException("not well formed");
      for (int i = 0; i < args.length - 1; i++) 
        if (count(Arrays.copyOfRange(args, 0, i)) < 0) throw new IllegalArgumentException("not well formed");
      
      // this code recursively parses the expression
      // TODO: Isolate this in a helper method so the above check doesn't run each time.
      if (labels.get(args[0]) != null) 
        return labels.get(args[0]);
      // A mathematical theorem for Polish notation says that at each position there is one and only one subexpression.
      // We choose the terms s.t. the above condition holds, so they are subexpressions
      // This implies that they are *the* subexpressions that we need to parse.
      if (args[0].equals("!")) {
        String[] term1 = Arrays.copyOfRange(args, 1, args.length);
        return makeCondition(term1).negate();
      }
      // I wanted to make a helper method to isolate the code for parsing expressions p Tau Rho with p binary, 
      // but Java is so hard. :/ Hence why this form is repeated four times.
      if (args[0].equals("&&")) {
        int i = 2;
        String[] term1 = Arrays.copyOfRange(args, 1, i);
        while (count(term1) >= 0) {
          i++;
          term1 = Arrays.copyOfRange(args, 1, i);
        }
        String[] term2 = Arrays.copyOfRange(args, i, args.length);
        return makeCondition(term1).and(makeCondition(term2));
      }
      if (args[0].equals("||")) {
        int i = 2;
        String[] term1 = Arrays.copyOfRange(args, 1, i);
        while (count(term1) >= 0) {
          i++;
          term1 = Arrays.copyOfRange(args, 1, i);
        }
        String[] term2 = Arrays.copyOfRange(args, i, args.length);
        return makeCondition(term1).or(makeCondition(term2));
      }
      if (args[0].equals("->")) {
        int i = 2;
        String[] term1 = Arrays.copyOfRange(args, 1, i);
        while (count(term1) >= 0) {
          i++;
          term1 = Arrays.copyOfRange(args, 1, i);
        }
        String[] term2 = Arrays.copyOfRange(args, i, args.length);
        return (makeCondition(term1).negate()).or(makeCondition(term2));
      }
      if (args[0].equals("<->")) {
        int i = 2;
        String[] term1 = Arrays.copyOfRange(args, 1, i);
        while (count(term1) >= 0) {
          i++;
          term1 = Arrays.copyOfRange(args, 1, i);
        }
        String[] term2 = Arrays.copyOfRange(args, i, args.length);
        return (makeCondition(term1).negate().and(makeCondition(term2).negate())).or(makeCondition(term1).and(makeCondition(term2)));
      }
      throw new IllegalArgumentException("this will never throw, hopefully");
    }
    
    // This helper method is used to check whether a Polish expression is well-formed.
    // A mathematical theorem says an expression Tau is well formed if and only if 
    // count(Tau) = -1 and count(Rho) >= 0 for each proper initial segment of Tau.
    private int count(String[] args) throws IllegalArgumentException {
      int count = 0;
      for (int i = 0; i < args.length; i ++) {
        if (args[i].equals("||") || args[i].equals("&&")  || args[i].equals("<->")  || args[i].equals("->")) count += 2-1;
        else if (args[i].equals("!")) count += 1-1;
        else if (labels.get(args[i]) != null) count += 0-1;
        else throw new IllegalArgumentException("unknown token `" + args[i] + "'");
      }
      return count;
    }
    
    // This method has source perform commands after replacing $[value]'s with recipient's properties.
    private void perform(MultiUser source, MultiUser recipient, String commands) {
          String uCommands = commands;
          
          Pattern p = Pattern.compile("\\$\\[(\\w+)\\]");
          Matcher match = p.matcher(uCommands);
          
          while (match.find()) {
            String property = match.group(1);
            Function<MultiUser, String> f = values.get(property);
            uCommands = uCommands.replaceFirst("\\$\\[(\\w+)\\]", f != null ? f.apply(recipient) : "");
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
    
    void makeTimer(String[] args, MultiUser target, ArrayList<Timer> timers, Hashtable<Integer, String> commandTable, String type, int minTime, int minTellTime) {
        String commands = argsToString(1, args);
        int time = 0;
        try {
          time = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
          target.schedTell("invalid integer argument for TIMES");
          return;
        }
        
        if (time < minTime) {
          target.schedTell("Not scheduling the " + type + ": TIME < 0ms.");
          return;
        }
        
        // So we can later cancel the timer.
        Timer timer = new Timer();
        int i = timers.indexOf(null);
        // Reuse the index so timers doesn't grow needlessly
        if (i >= 0) timers.set(i, timer);
        else timers.add(timer);
        
        final int id = timers.indexOf(timer);
        
        if (time > minTellTime) // Why tell if it's done too quickly to react?
          target.schedTell("The id of the scheduled " + type + " is " + id);
          
        commandTable.put(id, "(" + time + "ms) " + commands);
        
        TimerTask task = new TimerTask() {
          @Override
          public void run() {
            m.command(target, commands);
            if (type.equals("defer")) {
              timers.set(id, null);
              target.schedTell("Defer completed."); // TODO: uppercase first letter
            }
          }
        };
        
        if (type.equals("defer"))
          timer.schedule(task, time);
        else
          timer.schedule(task, 0, time);
    }
    
    private void cancelTimers(String[] args, MultiUser target, ArrayList<Timer> timers, String type) {
        if (args.length == 0) { // no id was given
          int length = timers.size();
          for (int i = 0; i < length; i++) {
            Timer timer = timers.get(i);
            if (timer != null) timer.cancel();
            timers.set(i, null);
          }
          target.schedTell("canceled all scheduled " + type + ".");
          return;
        }
        
        try {
          int id = Integer.parseInt(args[0]);
          Timer timer = timers.get(id);
          if (timer != null) timer.cancel();
          timers.set(id, null);
          target.schedTell("canceled the " + type + " " + id);
        } catch (NumberFormatException e) {
          target.schedTell("invalid integer argument for id: not an integer");
        } catch (IndexOutOfBoundsException e) {
          target.schedTell("invalid integer argument for id: out of bounds");
        }
    }
    
    private void listTimers(MultiUser target, ArrayList<Timer> timers, Hashtable<Integer, String> commandTable, String type) {
        String result = "The currently scheduled " + type + "s: ";
        for (int i = 0; i < timers.size(); i++) {
          if (timers.get(i) != null)
            result += "\r\n" + i + ": " + commandTable.get(i);
        }
        target.schedTell(result);
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