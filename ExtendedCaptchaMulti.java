package anon.trollegle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static anon.trollegle.Util.t;

import anon.trollegle.*;


public class ExtendedCaptchaMulti extends CaptchaMulti {
  
    private long hugCooldown = 5000, stabCooldown = 5000, flipCooldown = 5000;
    
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
        long lastHug, lastStab, lastFlip;
      
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
