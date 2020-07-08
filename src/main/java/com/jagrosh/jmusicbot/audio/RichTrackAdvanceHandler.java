package com.jagrosh.jmusicbot.audio;


import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.settings.Settings;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.vdurmont.emoji.EmojiParser;
import lombok.AccessLevel;
import lombok.Getter;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.jagrosh.jmusicbot.utils.FormatUtil.formatTime;

public class RichTrackAdvanceHandler {
    private static final String low_volume = ":speaker:";
    private static final String med_volume = ":sound:";
    private static final String high_volume = ":loud_sound:";
    private static final String stop_playback = ":black_square_for_stop:";
    private static final String skip_track = ":black_right_pointing_double_triangle_with_vertical_bar:";
    private static final List<String> trackAdvanceButtons = Arrays.asList(low_volume, med_volume, high_volume, stop_playback, skip_track);


    private final AudioHandler handler;
    private final Bot bot;
    private final Guild guild;
    private final TextChannel musicTextChannel;
    private final Role djRole;

    private Message trackAdvanceMessage;

    public RichTrackAdvanceHandler(Bot bot, long guildId, AudioHandler audioHandler) {

        this.bot = bot;
        this.handler = audioHandler;
        this.guild = bot.getJDA().getGuildById(guildId);

        //Fetch the text channel set for this guild.
        Settings guildSettings = Objects.requireNonNull(bot.getSettingsManager().getSettings(guild), "Bot settings was null");
        musicTextChannel = Objects.requireNonNull(guildSettings.getTextChannel(guild), "Music tx was null");
        djRole = Objects.requireNonNull(guildSettings.getRole(guild), "DJ role was null");
        registerTrackAdvanceButtonListener();
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
        trackAdvanceMessage.clearReactions().queue();
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
            String footerMsg = String.format("%s for volume. %s stop and clear queue. %s skips current track.", med_volume, stop_playback, skip_track);
            eb.setFooter(EmojiParser.parseToUnicode(footerMsg), slapLogoURL);


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
                    switch (EmojiParser.parseToAliases(re)) {
                        case low_volume:
                            handler.getPlayer().setVolume(10);
                            break;
                        case med_volume:
                            handler.getPlayer().setVolume(20);
                            break;
                        case high_volume:
                            handler.getPlayer().setVolume(30);
                            break;
                        case stop_playback:
                            handler.stopAndClear(); //wipes the queue, then stops the current track.
                            break;
                        case skip_track:
                            handler.getPlayer().stopTrack(); //This stops the current track, playing the next in queue if available.
                            break;
                    }

                    event.getReaction().removeReaction(event.getUser()).queue();
                    trackAdvanceMessage.editMessage(richTrackMessage()).queue();
                    registerTrackAdvanceButtonListener();
                }
        );
    }

    class RichTrackAdvanceButtonListener extends ListenerAdapter {

        @Override
        public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {

            //Only interact with tracked message.
            if (trackAdvanceMessage == null || trackAdvanceMessage.getIdLong() != event.getMessageIdLong()) {
                return;
            }

            //Filter bots
            if (event.getUser().isBot()) {
                return;
            }


            //Check if member is in VC
            Member member = event.getMember();
            if (!member.getVoiceState().inVoiceChannel()) {
                return;
            }

            //Check if VC contains the bot
            if (member.getVoiceState().getChannel().getMembers().stream()
                    .noneMatch(channelMembers -> channelMembers.equals(guild.getSelfMember()))) {
                return;
            }

            // If the reaction is an Emote we get the Snowflake,
            // otherwise we get the unicode value.
            String re = event.getReaction().getReactionEmote().isEmote()
                    ? event.getReaction().getReactionEmote().getId()
                    : event.getReaction().getReactionEmote().getName();

            // If the value we got is not registered as a button to
            // the ButtonMenu being displayed we return false.
            String parsedRe = EmojiParser.parseToAliases(re);
            if (!trackAdvanceButtons.contains(parsedRe)) {
                return;
            }


            switch (parsedRe) {
                case low_volume:
                    changeVolume(member, 10);
                    break;
                case med_volume:
                    changeVolume(member, 20);
                    break;
                case high_volume:
                    changeVolume(member, 30);
                    break;
                case stop_playback:
                    if (isDJ()) {
                        handler.stopAndClear(); //wipes the queue, then stops the current track.
                        musicTextChannel.sendMessage(String.format("**%s** has stopped playback and cleared the queue.", member.getEffectiveName())).queue();
                    }
                    break;
                case skip_track:
                    handler.getPlayer().stopTrack(); //This stops the current track, playing the next in queue if available.
                    break;
            }

            event.getReaction().removeReaction(event.getUser()).queue();
            trackAdvanceMessage.editMessage(richTrackMessage()).queue();
            registerTrackAdvanceButtonListener();



        /*
        //only allow DJ roles to press the buttons.

        if (m.getRoles().stream().noneMatch(role -> role.getIdLong() == djRole.getIdLong())) {
            return false;
        }
         */

        }

        private boolean isDJ(Member member) {
            return member.getRoles().stream().anyMatch(role -> role.getIdLong() == djRole.getIdLong());
        }

        private boolean isRequester(Member member) {
            return member.getUser().getIdLong() == handler.getRequester();
        }

        private void changeVolume(Member member, int volume) {
            if (isDJ(member)) {
                handler.getPlayer().setVolume(volume);
                if (musicTextChannel != null) {
                    musicTextChannel.sendMessage(String.format("**%s** changed volume to %i.", member.getEffectiveName(), volume)).queue();
                }
            }
        }
    }
}
