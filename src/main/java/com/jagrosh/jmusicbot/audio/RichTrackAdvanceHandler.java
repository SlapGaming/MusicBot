package com.jagrosh.jmusicbot.audio;


import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.settings.Settings;
import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.vdurmont.emoji.EmojiParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

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
        djRole = guildSettings.getRole(guild);
        bot.getJDA().addEventListener(new RichTrackAdvanceButtonListener());
    }


    public void onTrackStart() {
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


    public void onTrackEnd() {
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
                    if (isDJ(member)) {
                        int volume = 10;
                        handler.getPlayer().setVolume(volume);
                        musicTextChannel.sendMessage(String.format("**%s** changed volume to %d.", member.getEffectiveName(), volume)).queue();
                    }
                    break;
                case med_volume:
                    if (isDJ(member)) {
                        int volume = 20;
                        handler.getPlayer().setVolume(volume);
                        musicTextChannel.sendMessage(String.format("**%s** changed volume to %d.", member.getEffectiveName(), volume)).queue();
                    }
                    break;
                case high_volume:
                    if (isDJ(member)) {
                        int volume = 30;
                        handler.getPlayer().setVolume(volume);
                        musicTextChannel.sendMessage(String.format("**%s** changed volume to %d.", member.getEffectiveName(), volume)).queue();
                    }
                    break;
                case stop_playback:
                    if (isDJ(member)) {
                        handler.stopAndClear(); //wipes the queue, then stops the current track.
                        musicTextChannel.sendMessage(String.format("**%s** has stopped playback and cleared the queue.", member.getEffectiveName())).queue();
                    }
                    break;
                case skip_track:
                    if (isDJ(member) || isRequester(member)) {
                        String skippedTrack = handler.getPlayer().getPlayingTrack().getInfo().title;
                        handler.getPlayer().stopTrack(); //This stops the current track, playing the next in queue if available.
                        musicTextChannel.sendMessage(String.format("**%s** has skipped **%s**.", member.getEffectiveName(), skippedTrack)).queue();
                    }
                    break;
            }

            event.getReaction().removeReaction(event.getUser()).queue();
            trackAdvanceMessage.editMessage(richTrackMessage()).queue();
        }

        private boolean isDJ(Member member) {
            return djRole != null && member.getRoles().stream().anyMatch(role -> role.getIdLong() == djRole.getIdLong());
        }

        private boolean isRequester(Member member) {
            return member.getUser().getIdLong() == handler.getRequester();
        }
    }
}
