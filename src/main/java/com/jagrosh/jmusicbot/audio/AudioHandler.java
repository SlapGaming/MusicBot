/*
 * Copyright 2016 John Grosh <john.a.grosh@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.jmusicbot.audio;

import com.jagrosh.jmusicbot.utils.FormatUtil;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;

import java.util.*;

import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.playlist.Playlist;
import com.jagrosh.jmusicbot.queue.FairQueue;
import com.vdurmont.emoji.EmojiParser;
import net.dv8tion.jda.core.audio.AudioSendHandler;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class AudioHandler extends AudioEventAdapter implements AudioSendHandler {
    private final AudioPlayer audioPlayer;
    private final long guildId;
    private final FairQueue<QueuedTrack> queue;
    private final Set<String> votes;
    private final List<AudioTrack> defaultQueue;
    private final Bot bot;
    private final List<String> trackAdvanceButtons = Arrays.asList(":one:", ":two:", ":three:", ":four:", ":five:", ":six:", ":seven:", ":eight:", ":nine:");
    private AudioFrame lastFrame;
    private long requester;
    public static boolean STAY_IN_CHANNEL;
    public static boolean SONG_IN_STATUS;
    public static long MAX_SECONDS = -1;
    public static boolean USE_NP_REFRESH;
    private Message trackAdvanceMessage;

    public AudioHandler(AudioPlayer audioPlayer, Guild guild, Bot bot) {
        this.audioPlayer = audioPlayer;
        this.guildId = guild.getIdLong();
        this.bot = bot;
        queue = new FairQueue<>();
        votes = new HashSet<>();
        defaultQueue = new LinkedList<>();

        registerTrackAdvanceButtonListener();
    }

    public int addTrack(AudioTrack track, User user) {
        if (audioPlayer.getPlayingTrack() == null) {
            requester = user.getIdLong();
            audioPlayer.playTrack(track);
            return -1;
        } else
            return queue.add(new QueuedTrack(track, user.getIdLong()));
    }

    public FairQueue<QueuedTrack> getQueue() {
        return queue;
    }

    public void stopAndClear() {
        queue.clear();
        defaultQueue.clear();
        audioPlayer.stopTrack();
        //current = null;
    }

    public boolean isMusicPlaying() {
        return bot.getJDA().getGuildById(guildId).getSelfMember().getVoiceState().inVoiceChannel() && audioPlayer.getPlayingTrack() != null;
    }

    public Set<String> getVotes() {
        return votes;
    }

    public AudioPlayer getPlayer() {
        return audioPlayer;
    }

    public long getRequester() {
        return requester;
    }

    public boolean playFromDefault() {
        if (!defaultQueue.isEmpty()) {
            audioPlayer.playTrack(defaultQueue.remove(0));
            return true;
        }
        Guild guild = bot.getJDA().getGuildById(guildId);
        if (bot.getSettings(guild) == null || bot.getSettings(guild).getDefaultPlaylist() == null)
            return false;
        Playlist pl = Playlist.loadPlaylist(bot.getSettings(guild).getDefaultPlaylist());
        if (pl == null || pl.getItems().isEmpty())
            return false;
        pl.loadTracks(bot.getAudioManager(), (at) -> {
            if (audioPlayer.getPlayingTrack() == null)
                audioPlayer.playTrack(at);
            else
                defaultQueue.add(at);
        }, () -> {
            if (pl.getTracks().isEmpty() && !STAY_IN_CHANNEL)
                guild.getAudioManager().closeAudioConnection();
        });
        return true;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason == AudioTrackEndReason.FINISHED && bot.getSettings(bot.getJDA().getGuildById(guildId)).getRepeatMode()) {
            queue.add(new QueuedTrack(track.makeClone(), requester));
        }
        requester = 0;
        if (queue.isEmpty()) {
            if (!playFromDefault()) {
                if (SONG_IN_STATUS)
                    bot.resetGame();
                if (!STAY_IN_CHANNEL)
                    bot.getThreadpool().submit(() -> bot.getJDA().getGuildById(guildId).getAudioManager().closeAudioConnection());
                bot.updateTopic(guildId, this);
            }
        } else {
            QueuedTrack qt = queue.pull();
            requester = qt.getIdentifier();
            player.playTrack(qt.getTrack());
        }

        trackAdvanceMessage.clearReactions().queue();
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        votes.clear();
        if (SONG_IN_STATUS) {
            if (bot.getJDA().getGuilds().stream().filter(g -> g.getSelfMember().getVoiceState().inVoiceChannel()).count() <= 1)
                bot.getJDA().getPresence().setGame(Game.listening(track.getInfo().title));
            else
                bot.resetGame();
        }
        bot.updateTopic(guildId, this);
        displayTrackAdvance();
    }

    private void displayTrackAdvance() {
        //build the message embed
        Guild guild = bot.getJDA().getGuildById(guildId);
        Message m = FormatUtil.nextTrackMessage(guild);

        //send new message and add reaction emoticons
        bot.getJDA().getTextChannelById(bot.getSettings(guild).getTextId()).sendMessage(m)
                .queue(consumedM -> {
                    trackAdvanceMessage = consumedM;
                    for (String e : trackAdvanceButtons) {
                        trackAdvanceMessage.addReaction(EmojiParser.parseToUnicode(e)).queue();
                    }
                });
    }

    private void registerTrackAdvanceButtonListener() {
        bot.getWaiter().waitForEvent(MessageReactionAddEvent.class,
                event -> {
                    if (event.getUser().isBot()) {
                        return false;
                    }

                    //Check if member is in VC
                    if (!event.getMember().getVoiceState().inVoiceChannel()) {
                        return false;
                    }

                    //Check if VC contains the bot
                    if (event.getMember().getVoiceState().getChannel().getMembers().stream()
                            .noneMatch(member -> member.equals(bot.getJDA().getGuildById(guildId).getSelfMember()))) {
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
                    audioPlayer.setVolume(trackAdvanceButtons.indexOf(EmojiParser.parseToAliases(re)) * 10 + 10);
                    event.getReaction().removeReaction(event.getUser()).queue();
                    trackAdvanceMessage.editMessage(FormatUtil.nextTrackMessage(bot.getJDA().getGuildById(guildId))).queue();
                    registerTrackAdvanceButtonListener();
                }
        );
    }

    @Override
    public boolean canProvide() {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Override
    public byte[] provide20MsAudio() {
        return lastFrame.data;
    }

    @Override
    public boolean isOpus() {
        return true;
    }

    public static boolean isTooLong(AudioTrack track) {
        if (MAX_SECONDS <= 0)
            return false;
        return Math.round(track.getDuration() / 1000.0) > MAX_SECONDS;
    }
}
