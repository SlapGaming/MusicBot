package com.jagrosh.jmusicbot.got;


import com.jagrosh.jmusicbot.Bot;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;

import java.awt.*;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GOTManager {
    private final String guildID = "276858200853184522"; //slap
    private final String channelID = "570699432941584412"; //slap
    //private final String guildID = "315608020337819648"; //test
    //private final String channelID = "315608020337819648"; //test

    private final Date[] episodeDates = {
            new Date(119, Calendar.APRIL, 15, 3, 0, 0),
            new Date(119, Calendar.APRIL, 22, 3, 0, 0),
            new Date(119, Calendar.APRIL, 29, 3, 0, 0),
            new Date(119, Calendar.MAY, 6, 3, 0, 0),
            new Date(119, Calendar.MAY, 13, 3, 0, 0),
            new Date(119, Calendar.MAY, 20, 3, 0, 0)
    }; //year is 1900 + 119 = 2019

    /*
    private final Date[] episodeDates = {
    };
    */

    private final MessageEmbed WARNING = new EmbedBuilder()
            .setColor(Color.BLUE)
            .setTitle(":tv: Huzzah, tonight another episode of GoT! :tv:")
            .setImage("https://telluur.com/img/thrones.jpg")
            .setDescription("This channel will be locked to give everyone the chance to watch the episode without spoilers.")
            .addField("Channel Locked", "2 AM BST", true)
            .addField("Channel Unlocked", "10 PM BST", true)
            .addField("Some things...",
                    "- Mute this chat by clicking the :bell: icon (avoids popup notifications).\n" +
                            "- Spoilers of the __released__ episode are allowed when the channel unlocks.\n" +
                            "- Posting GoT content anywhere else will earn you a ban from the server. :hammer:",
                    false)
            .build();

    private final MessageEmbed LOCK = new EmbedBuilder()
            .setColor(Color.BLUE)
            .setTitle(":lock: Channel is now locked. :lock:")
            .setDescription("Channel unlocks at 10 PM BST.")
            .build();

    private final MessageEmbed UNLOCK = new EmbedBuilder()
            .setColor(Color.BLUE)
            .setTitle(":unlock: Channel is now unlocked. :unlock:")
            .setDescription("Feel free to post your /r/freefolk crap now.")
            .build();


    public GOTManager(Bot bot) {
        JDA jda = bot.getJDA();
        if (jda == null) {
            System.out.println("jda");
            return;
        }
        TextChannel tc = jda.getTextChannelById(channelID);
        if (tc == null) {
            System.out.println("tc");
            return;
        }
        Guild guild = jda.getGuildById(guildID);
        if (guild == null) {
            System.out.println("guild");
            return;
        }
        Role everyone = guild.getPublicRole();
        if (everyone == null) {
            System.out.println("everyone");
            return;
        }


        Date now = new Date();
        System.out.println("Now | " + now.toString());
        for (Date episode : episodeDates) {

            //Warning
            long warntime = episode.getTime() - 14400000;
            long warndelay = warntime - now.getTime();
            if (warndelay > 0) {
                System.out.println("Warn   | " + new Date(warntime).toString());
                ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
                ses.schedule(() -> {
                    tc.sendMessage(WARNING).queue();
                }, warndelay, TimeUnit.MILLISECONDS);
            } else {
                System.out.println("Warn   | " + new Date(warntime).toString() + " SKIPPED");
            }

            //lock
            long lockdelay = episode.getTime() - now.getTime();
            if (lockdelay > 0) {
                System.out.println("Lock   | " + new Date(episode.getTime()).toString());
                ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
                ses.schedule(() -> {
                    tc.sendMessage(LOCK).queue();
                    tc.putPermissionOverride(everyone).setDeny(Permission.MESSAGE_WRITE).queue();
                }, lockdelay, TimeUnit.MILLISECONDS);
            } else {
                System.out.println("Lock   | " + new Date(episode.getTime()).toString() + " SKIPPED");
            }

            //unlock
            long unlocktime = episode.getTime() + 72000000;
            long unlockdelay = unlocktime - now.getTime();
            if (unlockdelay > 0) {
                System.out.println("Unlock | " + new Date(unlocktime).toString());
                ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor();
                ses.schedule(() -> {
                    tc.sendMessage(UNLOCK).queue();
                    tc.putPermissionOverride(everyone).setAllow(Permission.MESSAGE_WRITE).queue();
                }, unlockdelay, TimeUnit.MILLISECONDS);
            } else {
                System.out.println("Unlock | " + new Date(unlocktime).toString() + " SKIPPED");
            }
        }
    }
}
