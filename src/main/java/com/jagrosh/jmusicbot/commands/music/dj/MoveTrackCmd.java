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
package com.jagrosh.jmusicbot.commands.music.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jmusicbot.Bot;
import com.jagrosh.jmusicbot.audio.AudioHandler;
import com.jagrosh.jmusicbot.audio.QueuedTrack;
import com.jagrosh.jmusicbot.commands.AbstractMusicCommand;
import com.jagrosh.jmusicbot.queue.FairQueue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command that providers users with the ability to move a track in the playlist.
 */
public class MoveTrackCmd extends AbstractMusicCommand {

    private static final Pattern PATTERN = Pattern.compile("^(\\d+)\\s+(\\d+)$");

    public MoveTrackCmd(Bot bot) {
        super(bot);
        this.name = "movetrack";
        this.help = "Move a track in the current playlist to a different position";
        this.arguments = "<from> <to>";
        this.aliases = new String[]{"move"};
        this.bePlaying = true;
        this.category = bot.DJ;
    }

    @Override
    public void executeMusicCommand(CommandEvent event) {
        int from;
        int to;

        try {
            // Validate the args
            String args = event.getArgs().trim();
            Matcher matcher = PATTERN.matcher(args);
            if (!matcher.matches()) {
                event.replyError("That ain't right. Usage: movetrack <from> <to>");
                return;
            }

            from = Integer.parseInt(matcher.group(1));
            to = Integer.parseInt(matcher.group(2));
        } catch (NumberFormatException e) {
            // Should already be caught by the regex but ok..
            event.replyError("That ain't a number: " + e.getMessage());
            return;
        }

        if (from == to) {
            event.replySuccess("Wow! That was easy. Great job using this command. You're doing a great job.");
            return;
        }

        // Validate that these are both positions available
        AudioHandler handler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        FairQueue<QueuedTrack> queue = handler.getQueue();
        if (!isAvailablePosition(event, queue, from) || !isAvailablePosition(event, queue, to)) {
            return;
        }

        // Move the track
        QueuedTrack track = queue.moveItem(from - 1, to - 1);
        String trackTitle = track.getTrack().getInfo().title;
        String reply = String.format("Moved track '%s' from position %d to %d.", trackTitle, from, to);
        event.replySuccess(reply);
    }

    private boolean isAvailablePosition(CommandEvent event, FairQueue<QueuedTrack> queue, int position) {
        if (position < 1 || position > queue.size()) {
            event.replyError(position + " ain't a valid position.");
            return false;
        } else {
            return true;
        }
    }




}
