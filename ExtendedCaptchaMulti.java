package anon.trollegle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static anon.trollegle.Util.t;

import anon.trollegle.*;


public class ExtendedCaptchaMulti extends CaptchaMulti {
  
    private long hugCooldown = 5000, stabCooldown = 5000, flipCooldown = 5000, rollCooldown = 5000, shipCooldown = 5000;
    
    @Override
    protected AdminCommands makeAdminCommands() { return new  ExtendedAdminCommands(this); }
    @Override
    protected UserBehaviour makeUserBehaviour() { return new ExtendedUserBehaviour(this); }//Extended
    @Override
    protected BotBehaviour makeBotBehaviour() { return new ExtendedBotBehaviour(this); }//Extended
    
    protected void hugUser(MultiUser source, String name) {
        ExtendedCaptchaUser target = (ExtendedCaptchaUser) userFromName(name);
        ExtendedCaptchaUser src = (ExtendedCaptchaUser) source;
        if (target == null) {
            src.schedTell("Usage: /hug USER");
        } else if (src == target) {
            src.schedTell("No self-hugs");
        } else if (System.currentTimeMillis() - src.lastHug < hugCooldown) {
            src.schedTell("You're hugging too fast");
        } else {
            src.lastHug = System.currentTimeMillis();
            target.hugCount++;
            relay("system", src, String.format("%1$s hugs %2$s (hug count: %3$d)", src.getDisplayNick(), target.getDisplayNick(), target.hugCount));
        }
    }

    protected void stabUser(MultiUser source, String name) {
        ExtendedCaptchaUser target = (ExtendedCaptchaUser) userFromName(name);
        ExtendedCaptchaUser src = (ExtendedCaptchaUser) source;
        if (target == null) {
            src.schedTell("Usage: /stab USER");
        } else if (src == target) {
            src.schedTell("No self-stabs");
        } else if (System.currentTimeMillis() - src.lastStab < stabCooldown) {
            src.schedTell("You're stabbing too fast");
        } else {
            src.lastStab = System.currentTimeMillis();
            target.stabCount++;
            relay("system", src, String.format("%1$s stabs %2$s! (stab count: %3$d)", src.getDisplayNick(), target.getDisplayNick(), target.stabCount));
        }
    }
    
    protected void flip(MultiUser source) {
        ExtendedCaptchaUser src = (ExtendedCaptchaUser) source;
        if (System.currentTimeMillis() - src.lastFlip < flipCooldown) {
            src.schedTell("You're flipping coins too quickly");
        } else {
            src.lastFlip = System.currentTimeMillis();
            int theCoin = ThreadLocalRandom.current().nextInt(0,2);
            String coinResult = (theCoin == 1) ? "heads" : "tails";
            relay("system", src, String.format("%1$s flips a coin and gets %2$s!", src.getDisplayNick(), coinResult));
        }
    }
    
    protected void roll(MultiUser source, String limit, String nDice) {
        ExtendedCaptchaUser src = (ExtendedCaptchaUser) source;
        try {
            int rollLimit = Integer.parseInt(limit);
            int dice = Integer.parseInt(nDice);
            if (rollLimit > 1000) {
                src.schedTell("Your integer is too large");
            } else if (rollLimit <= 1) {
                src.schedTell("Your integer is too small");
            } else if (dice <= 0) {
                src.schedTell("You can't roll that few dice");
            } else if (dice > 5) {
                src.schedTell("You can't roll that many dice");
            } else if (System.currentTimeMillis() - src.lastRoll < rollCooldown) {
                src.schedTell("You're rolling dice too quickly");
            } else {
                src.lastRoll = System.currentTimeMillis();

                List<Integer> rolls = new ArrayList<>();
                for (int i = 1; i <= dice; i++) {
                    rolls.add(ThreadLocalRandom.current().nextInt(1, rollLimit + 1));
                }

                if (dice == 1){
                    relay("system", src, String.format("%1$s rolls a %2$d-sided die and gets %3$d!", src.getDisplayNick(), rollLimit, rolls.get(0)));
                } else {
                    String rollStr = rolls.toString();
                    rollStr = rollStr.replace("[", "").replace("]", "").trim();
                    relay("system", src, String.format("%1$s rolls %2$d %3$d-sided dice and gets: %4$s!", src.getDisplayNick(), dice, rollLimit, rollStr));
                }
            }
        } catch (NumberFormatException e) {
            src.schedTell("At least one of your inputs is invalid");
        }
    }
                          
    protected void ship(MultiUser source, String name1, String name2) {
        ExtendedCaptchaUser src = (ExtendedCaptchaUser) source;
        ExtendedCaptchaUser target1 = (ExtendedCaptchaUser) userFromName(name1);
        ExtendedCaptchaUser target2 = (ExtendedCaptchaUser) userFromName(name2);
        if (target1 == null||target2 == null) {
            src.schedTell("Usage: /ship USER1 USER2");
        } else if (src == target1||src == target2) {
            src.schedTell("You can't target yourself with /ship");
        } else if (target1 == target2) {
            src.schedTell("You can't ship a person with themself");
        } else if (System.currentTimeMillis() - src.lastShip < shipCooldown){
            src.schedTell("You're shipping too quickly");
        } else {
            src.lastShip = System.currentTimeMillis();
            relay("system", src, String.format("%1$s ships %2$s and %3$s! Awww <3", source.getDisplayNick(), target1.getDisplayNick(), target2.getDisplayNick()));
        }
    }
  
    protected void shrug(MultiUser source, String message) {
        message = message + " ¯\\_(ツ)_/¯";
        relay("normal", source, message);
        if (source.isMuted())
            relay("special", source, message, "mute", to -> isVerboseAdmin(to) && !to.isMuted());
    }
  
    public static class ExtendedCaptchaUser extends CaptchaUser {
      
        @Persistent
        int hugCount, stabCount;
        @Persistent
        long lastHug, lastStab, lastFlip, lastRoll, lastShip;
      
        public ExtendedCaptchaUser(Callback<? super MultiUser> callback) { super(callback); }
        
    }
    
    @Override
    protected MultiUser makeUser() {
        return new ExtendedCaptchaUser(this);
    }
    
    // Compile: javac -d [directory of trollegle clone] -encoding utf-8 [directory of extension clone]/*.java
    // Run: java -Dfile.encoding=utf-8 anon.trollegle.ExtendedCaptchaMulti
    public static void main(String[] args) {
        Multi m = new ExtendedCaptchaMulti();
        m.parseArgs(args);
        m.run();
    }
    
}
