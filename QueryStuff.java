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

import java.util.Arrays;

import java.lang.IllegalArgumentException;

import static anon.trollegle.Commands.argsToString;

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
  
    private int indexOfCommand(String[] args) {
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
      }
    }
    
    public void with(String[] args, MultiUser target) {
      // defaults to one USER and the command starts on the second arg
      final int index = Math.max(indexOfCommand(args), 1);
      String command = argsToString(index, args);
      
      Predicate<MultiUser> recipients = p -> {
        for (int i = 0; i < index; i++) {
          if (("" + p.getNumber()).equals(args[i]) || p.getNick().equals(args[i]))
            return true;
        }
        
        return false;
      };
      
      perform(target, recipients, command);
    }
    
    public void ifThen(String[] args, MultiUser target) {
      // defaults to one USER and a one-token expression the command starts on the third arg
      final int index = Math.max(indexOfCommand(args), 2);
      String[] expression = Arrays.copyOfRange(args, 1, index);
      String command = argsToString(index, args);
      
      try {
        final Predicate<MultiUser> condition = labels.makeCondition(expression);
        Predicate<MultiUser> recipient = p -> {
          if (("" + p.getNumber()).equals(args[0]) || p.getNick().equals(args[0]))
            if (condition.test(p))
              return true;
          
          return false;
        };
        
        perform(target, recipient, command);
      } catch (IllegalArgumentException e) {
        target.schedTell(argsToString(0, expression) + " isn't a well formed expression: " + e.getMessage());
      }
    }
    
    public void simulate(String[] args, MultiUser target) {
      Predicate<MultiUser> pred = p -> (("" + p.getNumber()).equals(args[0]) || p.getNick().equals(args[0]));
      String command = argsToString(1, args);
      
      perform(pred, command);
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
          String uCommands = commands;
          
          String find = "\\$\\[(\\w+)([^\\]]*)\\]";
          Pattern p = Pattern.compile(find); // \\$\\[(\\w+)\\]
          Matcher match = p.matcher(uCommands);
          
          while (match.find()) {
            String property = match.group(1);
            Function<MultiUser, String> prop = values.get(property);
            if (prop != null) {
              uCommands = uCommands.replaceFirst(find, prop.apply(recipient));
            } else if (match.group(2) != null && match.group(2).length() > 1) {
              BiFunction<MultiUser, String[], String> fun = funs.get(property);
              // TODO: split after substring from length of first match to \\s+
              String[] args = match.group(2).substring(1).split("\\s+");
              
              // System.out.println(args[0] + " " + match.group(2));
              uCommands = uCommands.replaceFirst(find, fun != null ? fun.apply(recipient, args) : "[" + property + " wasn't found]");
            } else {
              // System.out.println(property + " wasn't found");
              uCommands = uCommands.replaceFirst(find, "[" + property + " wasn't found]");
              // alert that the property was not found
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
}