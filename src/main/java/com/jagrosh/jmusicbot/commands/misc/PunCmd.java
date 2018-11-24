package com.jagrosh.jmusicbot.commands.misc;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.pun.AbstractPunCmd;
import com.jagrosh.jmusicbot.pun.PunException;
import com.jagrosh.jmusicbot.pun.PunHandler;
import com.jagrosh.jmusicbot.pun.Punishment;

public class PunCmd extends AbstractPunCmd {
    private final static String SHUFFLE = "\uD83D\uDD04";
    private final static String SLAP_LOGO = "https://telluur.com/img/slaplogoemoji.jpg";
    private Bot bot;


    public PunCmd(Bot bot, PunHandler punHandler){
        this.bot = bot;
        this.punHandler = punHandler;
        this.name = "pun";
        this.help = "Starts a vote to move <@user> to the punishment timeout channel";
        this.arguments = "<@user> <?seconds=30>";
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        try {
            //These abstract functions throw PunExceptions when they fail.
            Punishment punishment = parseArguments(event);
            checkTimeoutBounds(event, punishment);
            punHandler.canPunish(event, punishment);

            //TODO do vote thingie
            //TODO keep track of in progress votes.




            punHandler.punish(event, punishment);
        } catch (PunException e) {
            event.replyError(e.getMessage());

        }

    }
}
