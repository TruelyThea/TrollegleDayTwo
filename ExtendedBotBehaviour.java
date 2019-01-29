package anon.trollegle;

import static anon.trollegle.Util.tn;

public class ExtendedBotBehaviour extends BotBehaviour {

    public ExtendedBotBehaviour(Multi m) {
        super(m);
    }

    
    protected void addAll() {
        super.addAll();

        UserBehaviour b = new ExtendedUserBehaviour(m);
        
        String[] names = {"hug", "pat", "stab", "appear", "magic", "flip", "roll", "ship", "shrug", "tell", "do", "say"};
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            Command command = b.getCommand(name);
            addMyCommand(name, command, null, command.usage);
        }

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
