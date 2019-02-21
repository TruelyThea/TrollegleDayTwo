package anon.trollegle;

import java.util.Timer;
import java.util.TimerTask;

import java.util.ArrayList;
import java.util.Hashtable;

import java.util.Arrays;

import java.lang.IllegalArgumentException;

import static anon.trollegle.Commands.argsToString;
import static anon.trollegle.Util.parseTime;

public class TimerStuff {
  
    private ArrayList<Timer> intervals = new ArrayList<Timer>();
    private Hashtable<Integer, String> intervalCommands = new Hashtable<Integer, String>();

    private ArrayList<Timer> defers = new ArrayList<Timer>();
    private Hashtable<Integer, String> deferCommands = new Hashtable<Integer, String>();
    
    private Multi m;
    
    public TimerStuff(Multi m) {
      this.m = m;
    }
    
    public void makeDefer(String[] args, MultiUser target) {
      makeTimer(args, target, defers, deferCommands, "defer", 0, 250);
    }
    
    public void makeInterval(String[] args, MultiUser target) {
      makeTimer(args, target, intervals, intervalCommands, "interval", 250, 0);
    }
    
    public void listDefers(MultiUser target) {
      listTimers(target, defers, deferCommands, "defer");
    }
    
    public void listIntervals(MultiUser target) {
      listTimers(target, intervals, intervalCommands, "interval");
    }
    
    public void cancelDefers(String[] args, MultiUser target) {
      cancelTimers(args, target, defers, "defer");
    }
    
    public void cancelIntervals(String[] args, MultiUser target) {
      cancelTimers(args, target, intervals, "interval");
    }
    
    
    private void makeTimer(String[] args, MultiUser target, ArrayList<Timer> timers, Hashtable<Integer, String> commandTable, String type, int minTime, int minTellTime) {
        String commands = argsToString(1, args);
        long time = 0;
        try {
          time = parseTime(args[0]);
        } catch (IllegalArgumentException e) {
          target.schedTell("IllegalArgumentException: " + e.getMessage());
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
        
        // if (time > minTellTime) // Why tell if it's done too quickly to react?
          // target.schedTell("The id of the scheduled " + type + " is " + id);
          
        commandTable.put(id, "(" + args[0] + ") " + commands);
        
        TimerTask task = new TimerTask() {
          @Override
          public void run() {
            m.command(target, commands);
            if (type.equals("defer")) {
              timers.set(id, null);
              // target.schedTell("Defer completed."); // TODO: uppercase first letter
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
          // target.schedTell("canceled all scheduled " + type + ".");
          return;
        }
        
        try {
          int id = Integer.parseInt(args[0]);
          Timer timer = timers.get(id);
          if (timer != null) timer.cancel();
          timers.set(id, null);
          // target.schedTell("canceled the " + type + " " + id);
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
}