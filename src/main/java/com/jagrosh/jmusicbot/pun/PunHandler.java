package com.jagrosh.jmusicbot.pun;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.utils.RoleUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.managers.GuildController;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PunHandler {

    private Bot bot;
    private Map<Guild, List<Member>> punishedGuilds = new HashMap<>();

    public PunHandler(Bot bot) {
        this.bot = bot;
        forceUnpunish();
    }

    private void forceUnpunish(){
        //TODO Implement a clear role of all channels on bot startup.
    }

    public void canPunish(CommandEvent event, Punishment punishment) throws PunException {
        getPunishedIfValidPunishment(event, punishment);
    }

    private List<Member> getPunishedIfValidPunishment(CommandEvent event, Punishment punishment) throws PunException {
        Guild guild = event.getGuild();
        Member punMember = punishment.getPunMember();
        List<Member> punished;
        if (punishedGuilds.containsKey(guild)) {
            punished = punishedGuilds.get(guild);

            //check if member is already punished
            if (punished.contains(punMember)) {
                throw new PunException(punMember.getAsMention() + " is already punished.");
            }

            //check if caller is being punished
            if (punished.contains(event.getMember())) {
                throw new PunException(event.getMember() + " is a bit salty and tried to punish while being punished. BAD BOI.");
            }
        } else {
            punished = new ArrayList<>();
            punishedGuilds.put(guild, punished);
        }
        return punished;
    }


    public void punish(CommandEvent event, Punishment punishment) throws PunException {
        Member punMember = punishment.getPunMember();
        int timeout = punishment.getTimeout();

        Guild guild = event.getGuild();
        long punRoleId = bot.getSettings(guild).getPunRoleId();
        long punVCId = bot.getSettings(guild).getPunVCId();

        if (punRoleId == 0) {
            throw new PunException("Pun role not set in this guild.");
        }

        if (punVCId == 0) {
            throw new PunException("Pun voice channel not set in this guild.");
        }

        //fetch punished members of guild, create if doesn't exist yet
        List<Member> punished = getPunishedIfValidPunishment(event, punishment);

        GuildController gc = guild.getController();
        Role punRole = guild.getRoleById(punRoleId);
        VoiceChannel punVC = guild.getVoiceChannelById(punVCId);
        VoiceChannel origin = punMember.getVoiceState().getChannel();
        final boolean punRolable = !RoleUtils.hasHigherRoleThanBot(punMember);

        if (punRolable){
            System.out.println(punMember.getAsMention() + " has higher role than bot.");
        }


        //Add role and move user if still in voice.
        punished.add(punMember);
        if (punRolable) {
            gc.addSingleRoleToMember(punMember, punRole).complete();
        }
        if (punMember.getVoiceState().inVoiceChannel()) {
            gc.moveVoiceMember(punMember, punVC).complete();
        }

        //Start Async task for moving the user back/removing pun role
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        Runnable unpunish = () -> {
            if (punRolable) {
                gc.removeSingleRoleFromMember(punMember, punRole).complete();
            }

            //check if user has left voice
            if (punMember.getVoiceState().inVoiceChannel()) {
                gc.moveVoiceMember(punMember, origin).complete();
            }
            punished.remove(punMember);
        };
        executor.schedule(unpunish, timeout, TimeUnit.SECONDS);
        event.reply(punMember.getAsMention() + " has been punished for " + timeout + " seconds.");
    }


}
