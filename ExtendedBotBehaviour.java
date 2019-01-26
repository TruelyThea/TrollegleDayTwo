package anon.trollegle;

import static anon.trollegle.Util.tn;

public class ExtendedBotBehaviour extends BotBehaviour {

    public ExtendedBotBehaviour(Multi m) {
        super(m);
    }

    
    protected void addAll() {
        super.addAll();

        UserBehaviour b = new ExtendedUserBehaviour(m);
        
        addMyCommand("stab", b.getCommand("stab"), null, null);
        addMyCommand("hug", b.getCommand("hug"), null, null);
        addMyCommand("pat", b.getCommand("pat"), null, null);
        addMyCommand("appear", b.getCommand("appear"), null, null);
        addMyCommand("magic", b.getCommand("magic"), null, null);
        
        addMyCommand("flip", b.getCommand("flip"), null, null);
        addMyCommand("shrug", b.getCommand("shrug"), null, null);
        
        addMyCommand("tell", b.getCommand("tell"), null, null);
        addMyCommand("do", b.getCommand("do"), null, null);
        addMyCommand("say", b.getCommand("say"), null, null);
        
        sanityCheck();
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