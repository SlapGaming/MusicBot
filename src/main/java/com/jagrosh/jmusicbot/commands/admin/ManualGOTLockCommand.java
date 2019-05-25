package com.jagrosh.jmusicbot.commands.admin;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.commands.AdminCommand;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;

public class ManualGOTLockCommand extends AdminCommand {
    private Bot bot;
    private boolean locked = false;

    private final String guildID = "276858200853184522"; //slap
    private final String channelID = "570699432941584412"; //slap


    public ManualGOTLockCommand(Bot bot) {
        this.name = "got";
        this.aliases = new String[]{"lock"};
        this.help = "manually locks/unlocks the GoT channel";
        this.bot = bot;
    }


    @Override
    protected void execute(CommandEvent commandEvent) {
        Role everyone = bot.getJDA().getGuildById(guildID).getPublicRole();
        TextChannel tc = bot.getJDA().getTextChannelById(channelID);

        if(locked){
            tc.putPermissionOverride(everyone).setAllow(Permission.MESSAGE_WRITE).queue();
            commandEvent.replySuccess("Forcing unlock.");
            locked = false;
        }else{
            tc.putPermissionOverride(everyone).setDeny(Permission.MESSAGE_WRITE).queue();
            commandEvent.replySuccess("Forcing lock.");
            locked = true;
        }


    }
}
