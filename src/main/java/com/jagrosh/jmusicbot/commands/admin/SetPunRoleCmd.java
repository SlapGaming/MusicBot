package com.jagrosh.jmusicbot.commands.admin;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.utils.FinderUtil;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import net.dv8tion.jda.core.entities.Role;

import java.util.List;

public class SetPunRoleCmd extends Command {

    private final Bot bot;

    public SetPunRoleCmd(Bot bot) {
        this.bot = bot;
        this.name = "setpr";
        this.help = "sets the pun role for the pun timeout feature";
        this.arguments = "<rolename|NONE>";
        this.guildOnly = true;
        this.category = bot.ADMIN;
    }


    @Override
    protected void execute(CommandEvent event) {
        if (event.getArgs().isEmpty()) {
            event.reply(event.getClient().getError() + " Please include a role name or NONE");
        } else if (event.getArgs().equalsIgnoreCase("none")) {
            bot.clearPunRole(event.getGuild());
            event.reply(event.getClient().getSuccess() + " Pun role cleared. Pun feature won't work without a valid role");
        } else {
            List<Role> list = FinderUtil.findRole(event.getArgs(), event.getGuild());
            if (list.isEmpty())
                event.reply(event.getClient().getWarning() + " No Roles found matching \"" + event.getArgs() + "\"");
            else if (list.size() > 1)
                event.reply(event.getClient().getWarning() + FormatUtil.listOfRoles(list, event.getArgs()));
            else {
                bot.setPunRole(list.get(0));
                event.reply(event.getClient().getSuccess() + " Pun role has been set to the **" + list.get(0).getName() + "** role.");
            }
        }
    }
}
