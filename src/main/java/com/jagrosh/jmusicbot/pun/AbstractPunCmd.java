package com.jagrosh.jmusicbot.pun;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.entities.Member;

import java.util.List;
import java.util.Optional;

public abstract class AbstractPunCmd extends Command {

    protected PunHandler punHandler;

    protected Punishment parseArguments(CommandEvent event) throws PunException {
        //Check if user is in a voice channel
        if (!event.getMember().getVoiceState().inVoiceChannel()) {

            throw new PunException("You must be in a voice channel to use this command.");
        }

        //Check if sufficient arguments are supplied
        if ("".equals(event.getArgs())) {
            throw new PunException("No arguments, minimal: " + arguments);
        }

        String[] args = event.getArgs().split("\\s+");

        //Get users from calling voice channel
        List<Member> vcMembers = event.getGuild().getMember(event.getAuthor()).getVoiceState().getChannel().getMembers();

        //Parse first <
        Optional<Member> maybeMember = vcMembers.stream()
                .filter(member -> member.getUser().getAsMention().equals(args[0].replaceAll("!", "")))
                .findFirst();

        Member punMember;
        if (maybeMember.isPresent()) {
            punMember = maybeMember.get();
        } else {
            throw new PunException("First argument should be <@user> in voice channel");
        }

        int timeout = 30;
        if (args.length > 1) {
            //Try to parse first argument and check team bounds
            try {
                timeout = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                throw new PunException("Second argument should be a number.", e);
            }

        }

        return new Punishment(punMember, timeout);
    }


    protected void checkTimeoutBounds(CommandEvent event, Punishment punishment) throws PunException {
        int timeout = punishment.getTimeout();
        if (timeout < 10) {
            throw new PunException("The Discord gods will tickle you in inappropriate places if you spam their API. Minimal timeout 10 seconds.");
        } else if (timeout > 300) {
            throw new PunException(event.getAuthor().getAsMention() + ", you're being a real asshole, pall. Trying to timeout someone for over 5 minutes.");
        }
    }

}
