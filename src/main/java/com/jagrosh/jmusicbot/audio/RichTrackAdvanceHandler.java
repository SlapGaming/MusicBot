package com.jagrosh.jmusicbot.audio;


import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.settings.Settings;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.vdurmont.emoji.EmojiParser;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;

import java.util.Arrays;
import java.util.List;

import static com.jagrosh.jmusicbot.utils.FormatUtil.formatTime;

public class RichTrackAdvanceHandler {
    private final AudioPlayer player;
    private final List<String> trackAdvanceButtons = Arrays.asList(":one:", ":two:", ":three:", ":four:", ":five:", ":six:", ":seven:", ":eight:", ":nine:");
    private final Bot bot;
    private final Guild guild;
    private final TextChannel musicTextChannel;
    private final Role djRole;

    private Message trackAdvanceMessage;

    public RichTrackAdvanceHandler(Bot bot, long guildId, AudioPlayer player) {
        this.bot = bot;
        this.player = player;
        this.guild = bot.getJDA().getGuildById(guildId);

        //Fetch the text channel set for this guild.
        Settings guildSettings = bot.getSettingsManager().getSettings(guild);
        if (guildSettings == null) {
            musicTextChannel = null;
            djRole = null;
        } else {
            //These might still return null
            musicTextChannel = guildSettings.getTextChannel(guild);
            djRole = guildSettings.getRole(guild);
            registerTrackAdvanceButtonListener();
        }
    }


    public void onTrackStart(AudioTrack track) {
        if (musicTextChannel != null && djRole != null) {
            //build the message embed
            Message m = richTrackMessage();


            //send new message and add reaction emoticons
            musicTextChannel.sendMessage(m)
                    .queue(consumedM -> {
                        trackAdvanceMessage = consumedM;
                        for (String e : trackAdvanceButtons) {
                            trackAdvanceMessage.addReaction(EmojiParser.parseToUnicode(e)).queue();
                        }
                    });
        }
    }


    public void onTrackEnd(AudioTrack track, AudioTrackEndReason endReason) {
        if (musicTextChannel != null && djRole != null) {
            trackAdvanceMessage.clearReactions().queue();
        }
    }


    private Message richTrackMessage() {
        MessageBuilder mb = new MessageBuilder();
        mb.append(" **Now Playing...**");
        EmbedBuilder eb = new EmbedBuilder();
        AudioHandler ah = (AudioHandler) guild.getAudioManager().getSendingHandler();
        eb.setColor(guild.getSelfMember().getColor());
        if (ah == null || !ah.isMusicPlaying(bot.getJDA())) {
            eb.setTitle("No music queued");
            eb.setDescription("\u23F9 " + FormatUtil.progressBar(-1) + " " + FormatUtil.volumeIcon(ah == null ? 100 : ah.getPlayer().getVolume()));
        } else {
            if (ah.getRequester() != 0) {
                User u = guild.getJDA().getUserById(ah.getRequester());
                if (u == null)
                    eb.setAuthor("Unknown (ID:" + ah.getRequester() + ")", null, null);
                else
                    eb.setAuthor(u.getName() + "#" + u.getDiscriminator(), null, u.getEffectiveAvatarUrl());
            }

            try {
                eb.setTitle(ah.getPlayer().getPlayingTrack().getInfo().title, ah.getPlayer().getPlayingTrack().getInfo().uri);
            } catch (Exception e) {
                eb.setTitle(ah.getPlayer().getPlayingTrack().getInfo().title);
            }

            if (ah.getPlayer().getPlayingTrack() instanceof YoutubeAudioTrack)
                eb.setThumbnail("https://img.youtube.com/vi/" + ah.getPlayer().getPlayingTrack().getIdentifier() + "/mqdefault.jpg");

            eb.addField("Volume", Integer.toString(ah.getPlayer().getVolume()), true);
            eb.addField("Length", formatTime(ah.getPlayer().getPlayingTrack().getDuration()), true);

            String slapLogoURL = "https://telluur.com/img/slaplogoemoji.jpg";
            eb.setFooter(EmojiParser.parseToUnicode("Use the number emojis :1234: to quickly set the volume, or use the volume command."), slapLogoURL);


        }
        return mb.setEmbed(eb.build()).build();
    }

    private void registerTrackAdvanceButtonListener() {
        bot.getWaiter().waitForEvent(MessageReactionAddEvent.class,
                event -> {
                    if (event.getUser().isBot()) {
                        return false;
                    }

                    //only allow DJ roles to press the buttons.
                    Member m = event.getMember();
                    if (m.getRoles().stream().noneMatch(role -> role.getIdLong() == djRole.getIdLong())) {
                        return false;
                    }

                    //Check if member is in VC
                    if (!m.getVoiceState().inVoiceChannel()) {
                        return false;
                    }

                    //Check if VC contains the bot
                    if (m.getVoiceState().getChannel().getMembers().stream()
                            .noneMatch(channelMembers -> channelMembers.equals(guild.getSelfMember()))) {
                        return false;
                    }

                    // If the message is not the same as the ButtonMenu
                    // currently being displayed.
                    if (!event.getMessageId().equals(trackAdvanceMessage.getId())) {
                        return false;
                    }

                    // If the reaction is an Emote we get the Snowflake,
                    // otherwise we get the unicode value.
                    String re = event.getReaction().getReactionEmote().isEmote()
                            ? event.getReaction().getReactionEmote().getId()
                            : event.getReaction().getReactionEmote().getName();

                    // If the value we got is not registered as a button to
                    // the ButtonMenu being displayed we return false.
                    if (!trackAdvanceButtons.contains(EmojiParser.parseToAliases(re))) {
                        return false;
                    }

                    // Last check is that the person who added the reaction
                    // is a valid user.
                    //return isValidUser(event.getUser(), event.getGuild());
                    return true;
                },
                (MessageReactionAddEvent event) -> {
                    String re = event.getReaction().getReactionEmote().isEmote()
                            ? event.getReaction().getReactionEmote().getId()
                            : event.getReaction().getReactionEmote().getName();
                    player.setVolume(trackAdvanceButtons.indexOf(EmojiParser.parseToAliases(re)) * 10 + 10);

                    event.getReaction().removeReaction(event.getUser()).queue();
                    trackAdvanceMessage.editMessage(richTrackMessage()).queue();
                    registerTrackAdvanceButtonListener();
                }
        );
    }

}
