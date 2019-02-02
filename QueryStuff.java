package anon.trollegle;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Iterator;

import java.util.function.Predicate; 
import java.util.function.Function; 
import java.util.function.BiFunction;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

import java.util.Arrays;

import java.lang.IllegalArgumentException;
import java.util.ConcurrentModificationException;

import static anon.trollegle.Commands.argsToString;
import static anon.trollegle.Template.replace;

public class QueryStuff {
    private Polish<MultiUser> labels = new Polish<MultiUser>();
    
    private Hashtable<String, Function<MultiUser, String>> values = new Hashtable<String, Function<MultiUser, String>>();
    private Hashtable<String, BiFunction<MultiUser, String[], String>> funs = new Hashtable<String, BiFunction<MultiUser, String[], String>>();
    
    private Hashtable<String, String> addedLabels = new Hashtable<String, String>();
    
    private Multi m;
    
    public QueryStuff(Multi m) {
      this.m = m;
      
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
          int index = (int) Math.floor(Math.random()*(double)ponies.length);
          return ponies[index];
        });
        
        // This is used to escape "$[text]" by $[$][text]
        values.put("$", p -> "$");
        
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
       
        funs.put("choose", (p, args) -> args.length == 0 ? "" : args[(int) Math.floor(Math.random()*(double)args.length)]);
        funs.put("interpolate", (p, args) -> argsToString(0, args));
        funs.put("$", (__, ___) -> "$");
        funs.put("name", (p, args) -> {
          if (args.length == 0) return "";
          MultiUser u = m.userFromName(args[0]);
          return u == null ? "[user " + args[0] + " couldn't be found]" : u.getNick();
        });
        funs.put("id", (p, args) -> {
          if (args.length == 0) return "";
          MultiUser u = m.userFromName(args[0]);
          return u == null ? "[user " + args[0] + " couldn't be found]" : "" + u.getNumber();
        });
    }
  
    public static int indexOfCommand(String[] args) {
      int i = 0;
      for (i = 0; i < args.length; i++) {
        if (args[i].startsWith("/")) break;
      }
      
      return i < args.length ? i : -1;
    }
    
    // All documentation is provided above the respective command in ExtendedAdminCommands.java.
    
    public void allWho(String[] args, MultiUser target) {
      int index = Math.max(indexOfCommand(args), 1); // if the command wasn't found, assume it's at location 1
      String[] expression = Arrays.copyOfRange(args, 0, index);
      String command = argsToString(index, args);
      
      try {
        Predicate<MultiUser> pred = labels.makeCondition(expression);
        perform(target, pred, command);
      } catch (IllegalArgumentException e) {
        target.schedTell(argsToString(0, expression) + " isn't a well formed expression: " + e.getMessage());
      } catch (ConcurrentModificationException e) {
        target.schedTell("ConcurrentModificationException: the user collection was modified (perhaps by a /kick) while it was under iteration;\r\n"
                       + "if you were kicking users in /.allwho, try /.with after you get the list of users through /.allwho PREDICATE /.tell $[number].");
      }
    }
    
    public void with(String[] args, MultiUser target) {
      // defaults to one USER and the command starts on the second arg
      int index = Math.max(indexOfCommand(args), 1);
      String command = argsToString(index, args);
      
      for (int i = 0; i < index; i++) {
        MultiUser recipient = m.userFromName(args[i]);
        if (recipient == null)
          target.schedTell("User " + args[i] + " couldn't be found.");
        else
          perform(target, recipient, command);
      }
    }
    
    public void simulate(String[] args, MultiUser target) {
      int index = Math.max(indexOfCommand(args), 1);
      String command = argsToString(index, args);
      
      for (int i = 0; i < index; i++) {
        MultiUser recipient = m.userFromName(args[i]);
        if (recipient == null)
          target.schedTell("User " + args[i] + " couldn't be found.");
        else if (recipient.isDummy())
          target.schedTell("No /.simulating console terminal calls :/");
        else
          perform(recipient, recipient, command);
      }
    }
    
    public void ifThen(String[] args, MultiUser target) {
      // defaults to one USER and a one-token expression the command starts on the third arg
      int index = Math.max(indexOfCommand(args), 2);
      String[] expression = Arrays.copyOfRange(args, 1, index);
      
      int i = index + 1;
      for (i = index + 1; i < args.length; i++) {
        if (args[i].toLowerCase().equals("/.else"))
          break;
      }
      
      // To un-escape /..else to /.else and /...else to /..else ...
      Function<String, String> unescape = s -> "/" + s.substring(2);
      String match = "(?i)(\\/\\.{2,}else)";
      
      String consequent = replace(argsToString(0, Arrays.copyOfRange(args, index, i)), match, unescape);
      String otherwise = replace(argsToString(i + 1, args), match, unescape);
      
      try {
        Predicate<MultiUser> condition = labels.makeCondition(expression);
        
        MultiUser recipient = m.userFromName(args[0]);
        if (recipient == null) {
          target.schedTell("User " + args[0] + " couldn't be found.");
        } else if (condition.test(recipient)) {
          perform(target, recipient, consequent);
        } else if (otherwise.length() > 0) {
          perform(target, recipient, otherwise);
        };
      } catch (IllegalArgumentException e) {
        target.schedTell(argsToString(0, expression) + " isn't a well formed expression: " + e.getMessage());
      }
    }
    
    public void setLabel(String[] args, MultiUser target) {
      try {
        labels.put(args[0], Arrays.copyOfRange(args, 1, args.length));
        addedLabels.put(args[0], argsToString(1, args));
        target.schedTell("Label Added.");
      } catch (IllegalArgumentException e) {
        target.schedTell("There was a problem parsing your label: " + e.getMessage());
      }
    }
    
    public Hashtable<String, String> getLabels() {
      return addedLabels;
    }
    
    public Enumeration<String> tokens() {
      return labels.tokens();
    }
    
    public Enumeration<String> props() {
      return values.keys();
    }
    
    public Enumeration<String> funs() {
      return funs.keys();
    }
    
    // This method has source perform commands after replacing $[value]'s with recipient's properties.
    private void perform(MultiUser source, MultiUser recipient, String commands) {
          String uCommands = replace(commands, "\\$\\[(.+?)\\]", match -> {
            String[] args = match.split("\\s+");
            if (args.length > 0) {
              String property = args[0];
              
              Function<MultiUser, String> prop = values.get(property);
              if (prop != null) return prop.apply(recipient);
              
              if (args.length > 1) {
                BiFunction<MultiUser, String[], String> fun = funs.get(property);
                if (fun != null) return fun.apply(recipient, Arrays.copyOfRange(args, 1, args.length));
              }
              
              // the property couldn't be found! There is no warning like before to make placing /.with before a /.forEach (or a /.with inside a /.forEach command) possible
              return "$[" + property + "]"; 
            }
            
            return "$"; // shorthand escape for "$"
          });
          
          m.command(source, uCommands);
    }
    
    // This method is used to query-select recipients and perform a command on them.
    private void perform(MultiUser source, Predicate<MultiUser> recipients, String commands) throws ConcurrentModificationException {
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
    private void perform(Predicate<MultiUser> target, String commands) throws ConcurrentModificationException{
        synchronized (m.users) {
            Iterator<MultiUser> users = m.allUsers.iterator();
            MultiUser u = null;
            while (users.hasNext()) {
                u = users.next();
                if (target.test(u)) perform(u, u, commands);
            }
        }
    }
    
    // This isn't really a query commands, so this file has become a misnomer.
    public void each(String[] args, MultiUser target) {
      int index = Math.max(indexOfCommand(args), 1);
      String command = argsToString(index, args);
      
      Integer[] j = {0};
      for (int i = 0; i < index; i++) {
        j[0] = i;
        String filled = replace(command, "\\$\\[(.+?)\\]", match -> {
          String[] innerargs = match.split("\\s+");
          if (innerargs.length > 0) {
            String property = innerargs[0];
            
            BiFunction<MultiUser, String[], String> fun = funs.get(property);
            if (fun != null) return fun.apply(target, Arrays.copyOfRange(innerargs, 1, innerargs.length));
            
            if (property.equals("index")) return "" + j[0];
            if (property.equals("value")) return "" + args[j[0]];
            if (property.equals("collection")) return argsToString(0, Arrays.copyOfRange(args, 0, index));
            
            return "$[" + property + "]";
          }
          
          return "$";
        });
        
        m.command(target, filled);
      }
    }
}