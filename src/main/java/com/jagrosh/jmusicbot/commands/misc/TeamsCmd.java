package com.jagrosh.jmusicbot.commands.misc;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.jdahacks.EmbeddedButtonMenu;
import com.vdurmont.emoji.EmojiParser;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.PermissionException;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class TeamsCmd extends Command {

    private final static String SHUFFLE = EmojiParser.parseToUnicode(":arrows_counterclockwise:");
    private final static String SLAP_LOGO = "https://telluur.com/img/slaplogoemoji.jpg";
    private Bot bot;

    public TeamsCmd(Bot bot) {
        this.bot = bot;
        this.name = "teams";
        this.help = "creates random teams from users in VC";
        this.arguments = "<number of teams> [excluded @mentioned users]";
        this.guildOnly = true;
    }


    @Override
    protected void execute(CommandEvent event) {
        //Check if user is in a voice channel
        if (!event.getMember().getVoiceState().inVoiceChannel()) {
            event.replyError("You must be in a voice channel to use this command.");
            return;
        }

        //Check if sufficient arguments are supplied
        if ("".equals(event.getArgs())) {
            event.replyError("No arguments, minimal: " + this.arguments);
            return;
        }

        String[] args = event.getArgs().split("\\s+");


        int numberOfTeams;
        //Try to parse first argument and check team bounds
        try {
            numberOfTeams = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            event.replyError("First argument should be a number [2-9].");
            return;
        }

        if (numberOfTeams < 2) {
            event.replyError("This is a __teams__ command. No worries though, for people like you we've added this helpful error message: Number of teams should be ≥ 2");
            return;
        } else if (numberOfTeams > 9) {
            event.replyError("Number of teams should be ≤ 9");
            return;
        }

        //Get users from calling voice channel
        List<Member> vcMembers = event.getGuild().getMember(event.getAuthor()).getVoiceState().getChannel().getMembers();

        //Assume the second+ argument is a mentioned user.
        List<String> mentions = Arrays.stream(Arrays.copyOfRange(args, 1, args.length))
                .map(m -> m.replaceAll("!", ""))//Discord is sheit and adds ! to user ID's when nicknamed
                .collect(Collectors.toList());

        //Filter out the mentions
        List<Member> pool = vcMembers.stream()
                .filter(member -> !member.getUser().isBot() && !mentions.contains(member.getUser().getAsMention())) //Filter out bots/mentioned users
                .collect(Collectors.toList());

        if (numberOfTeams > pool.size()) {
            event.replyError("Good job, you're trying to generate more teams than the number of participants available. You must have done well at school.");
            return;
        }

        showMessage(event, numberOfTeams, pool, 0);
    }

    private void showMessage(CommandEvent event, int numberOfTeams, List<Member> pool, int shuffles) {
        Consumer<Message> callback = m -> new EmbeddedButtonMenu.Builder()
                .setMessageEmbed(createMessageEmbedTeams(numberOfTeams, pool))
                .setChoices(SHUFFLE)
                .setTimeout(30, TimeUnit.SECONDS)
                .setEventWaiter(bot.getWaiter())
                .setUsers(pool.stream().map(Member::getUser).toArray(User[]::new))
                .setAction(re -> {
                    if (SHUFFLE.equals(re.getName())) {
                        m.delete().queue();
                        if (shuffles == 5) {
                            event.replyWarning("How often are you going to bash that shuffle button, hmm?");
                        } else if (shuffles == 6) {
                            event.replyError("I've had enough of your shit. Make your own teams.");
                            return;
                        }
                        showMessage(event, numberOfTeams, pool, shuffles + 1);
                    }
                })
                .setFinalAction(a ->
                {
                    try {
                        a.clearReactions().queue();

                    } catch (PermissionException ignored) {
                    }
                })
                .build().display(m);

        event.reply(new EmbedBuilder()
                .setTitle("Random teams")
                .setColor(Color.GREEN)
                .addField("", "Generating...", false)
                .build(), callback);

    }

    private MessageEmbed createMessageEmbedTeams(int numberOfTeams, List<Member> memberPool) {
        //Create the team objects
        List<List<String>> teams = new LinkedList<>();
        for (int i = 0; i < numberOfTeams; i++) {
            teams.add(new LinkedList<>());
        }

        //Randomize the teams by shuffling the pool, and then filling the teams.

        Collections.shuffle(memberPool);
        int assign = 0;
        for (Member teamMate : memberPool) {
            teams.get(assign).add(teamMate.getEffectiveName());
            assign = (assign + 1) % numberOfTeams;
        }

        //Create the embedded message reply
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("Random teams")
                .setColor(Color.GREEN);

        int teamNo = 1;
        for (List<String> team : teams) {
            eb.addField("Team " + teamNo++, String.join("\r\n", team), true);
        }

        eb.setFooter(String.format("Click %s to shuffle the teams", SHUFFLE), SLAP_LOGO);

        return eb.build();
    }
}