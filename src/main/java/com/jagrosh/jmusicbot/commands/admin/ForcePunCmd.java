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
        this.name = "fpun";
        this.help = "sets the voice channel for pun timeout";
        this.arguments = "<@user> <?seconds>";
        this.guildOnly = true;
        this.category = bot.ADMIN;

        this.punHandler = punHandler;
    }

    @Override
    protected void execute(CommandEvent event) {

        try {
            Punishment punishment = parseArguments(event);
            event.reply("Forcing pun-ishment...");
            punHandler.punish(event, punishment);
        } catch (PunException e) {
            event.replyError(e.getMessage());

        }
    }
}
