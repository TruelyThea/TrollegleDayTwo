package anon.trollegle;

import static anon.trollegle.Util.tn;

public class ExtendedUserBehaviour extends UserBehaviour {

    public ExtendedUserBehaviour(Multi m) {
        super(m);
    }

    protected void addAll() {
        super.addAll();
        
        ExtendedCaptchaMulti c = (ExtendedCaptchaMulti) m;
        addMyCommand("hug", 1, (args, target) -> c.hugUser(target, args[0]), "Hug another user");
        addMyCommand("stab", 1, (args, target) -> c.stabUser(target, args[0]), "Stab another user");
        addMyCommand("pat", 1, (args, target) -> m.patUser(target, args[0]), "Pat another user");
        addMyCommand("appear", 0, (args, target) -> m.tellRoom("Chasidy appears!"), null);
        addMyCommand("magic", 0, (args, target) -> m.tellRoom("Chasidy prepares the Path to Exile card!"), null);
        
        addMyCommand("flip", 0, (args, target) -> c.flip(target), "Flip a coin");
        addMyCommand("roll", 1, (args, target) -> {
            if (args.length == 1)
                c.roll(target, args[0], "1");
            else
                c.roll(target, args[0], args[1]);
            }, "Roll COUNT INT-sided dice");
        addMyCommand("ship", 2, (args, target) -> c.ship(target, args[0], args[1]), "Ship two other users");
        addMyCommand("shrug", 0, (args, target) -> c.shrug(target, argsToString(0, args)), "¯\\_(ツ)_/¯");
        
        String[] aliasesTell = {"relay", "inform"};
        String[] aliasesDo = {"perform", "doperform"};
        String[] aliasesSay = {"dosay"};
        
        // See the comments for the analogous commands in ExtendedAdminCommands.java
        addMyCommand("tell", new BodyCommand(null, null,  1,
                (args, target) -> {
                    m.annPrivate(target, target.getNumber() + "", argsToString(0, args));
                }), aliasesTell, null);
        addMyCommand("do", new BodyCommand(null, null, 1,
                (args, target) -> {
                    String data = argsToString(0, args);
                    m.hear(target, data);
                    m.annPrivate(target, target.getNumber() + "", "just now: [" + target.getNick() + "] " + data);
                }), aliasesDo, null);
        addMyCommand("say", new BodyCommand(null, null, 1,
                (args, target) -> {
                    String data = argsToString(0, args);
                    m.relay("normal", target, data);
                    m.annPrivate(target, target.getNumber() + "", "just now: [" + target.getNick() + "] " + data);
                }), aliasesSay, null);
        
        sanityCheck();
        
    }
    
    public Command addMyCommand(String main, int arglen, Body body, String help) {
        return addMyCommand(main, new BodyCommand(null, help, arglen, body), null, null);
    }
    
    public Command addMyCommand(String main, Command command, String[] aliases, String usage) {
        if (command.usage == null)
            if (usage == null)
                command.usage = "/" + main;
            else
                command.usage = "/" + main + " " + usage;
        return addCommand(main, command, aliases);
    }
}
