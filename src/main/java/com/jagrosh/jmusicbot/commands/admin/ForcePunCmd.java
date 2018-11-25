package com.jagrosh.jmusicbot.commands.admin;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.pun.AbstractPunCmd;
import com.jagrosh.jmusicbot.pun.PunException;
import com.jagrosh.jmusicbot.pun.PunHandler;
import com.jagrosh.jmusicbot.pun.Punishment;

public class ForcePunCmd extends AbstractPunCmd {

    private Bot bot;

    public ForcePunCmd(Bot bot, PunHandler punHandler) {
        this.bot = bot;
        this.punHandler = punHandler;
        this.name = "fpun";
        this.help = "pun-ish a user without vote. ";
        this.arguments = "<@user> <?seconds=30>";
        this.guildOnly = true;

        //TODO, set to admin once vote is implemented.
        this.category = bot.DJ;
    }

    @Override
    protected void execute(CommandEvent event) {
        try {
            Punishment punishment = parseArguments(event);
            checkTimeoutBounds(event, punishment);
            event.reply("Forcing pun-ishment...");
            punHandler.punish(event, punishment);
        } catch (PunException e) {
            event.replyError(e.getMessage());
        }
    }
}
