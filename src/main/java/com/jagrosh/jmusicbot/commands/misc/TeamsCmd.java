package com.jagrosh.jmusicbot.commands.misc;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class TeamsCmd extends Command {

    public TeamsCmd() {
        this.name = "teams";
        this.help = "creates random teams from users in VC";
        this.arguments = "<number of teams>";
    }

    @Override
    protected void execute(CommandEvent event) {

        //TODO: Parse and validate user input: empty/crappy input
        int numberOfTeams = Integer.parseInt(event.getArgs());
        List<Member> vcMembers = event.getGuild().getMember(event.getAuthor()).getVoiceState().getChannel().getMembers();
        System.out.println(event.getAuthor().getName());
        System.out.println("---------------------");
        System.out.println(printList(vcMembers));
        List<Member> pool = vcMembers.stream().filter(member -> !member.getUser().isBot()).collect(Collectors.toList());
        System.out.println("---------------------");
        System.out.println(printList(pool));


        //Create the team objects
        List<List<Member>> teams = new LinkedList<>();
        for (int i = 0; i < numberOfTeams; i++) {
            teams.add(new LinkedList<>());

        }


        //Randomize the teams by shuffling the pool, and then filling the teams.
        Collections.shuffle(pool);
        int assign = 0;
        for (Member teamMate : pool) {
            teams.get(assign).add(teamMate);
            assign = (assign + 1) % numberOfTeams;
        }


        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Random teams");
        eb.setColor(Color.GREEN);

        int teamNo = 1;
        for (List<Member> team : teams) {
            eb.addField("Team " + teamNo++, printList(team), false);
        }


        event.reply(eb.build());


    }

    private String printList(List<Member> memberList) {
        return String.join(", ", memberList.stream().map(Member::getEffectiveName).collect(Collectors.toList()));

    }
}
