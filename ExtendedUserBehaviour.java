package anon.trollegle;

import static anon.trollegle.Util.tn;

public class ExtendedUserBehaviour extends UserBehaviour {

    public ExtendedUserBehaviour(Multi m) {
        super(m);
    }

    protected void addAll() {
        ExtendedCaptchaMulti c = (ExtendedCaptchaMulti) m;
        addMyCommand("hug", 1, (args, target) -> c.hugUser(target, args[0]), null);
        addMyCommand("stab", 1, (args, target) -> c.stabUser(target, args[0]), null);
        addMyCommand("appear", 0, (args, target) -> m.tellRoom("Chasidy appears!"), null);
        addMyCommand("magic", 0, (args, target) -> m.tellRoom("Chasidy prepares the Path to Exile card!"), null);
        
        super.addAll();
        
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
