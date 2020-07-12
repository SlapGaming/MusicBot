package com.jagrosh.jmusicbot.commands.general;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class VersionCmd extends Command {
    private final String version;

    public VersionCmd(String version) {
        this.name = "version";
        this.help = "Displays the bots version.";
        this.guildOnly = false;

        this.version = version;
    }

    @Override
    protected void execute(CommandEvent event) {
        event.reply(String.format("MusicBot version: `%s`", version));
    }
}
