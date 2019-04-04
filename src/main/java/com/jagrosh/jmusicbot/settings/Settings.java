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
package com.jagrosh.jmusicbot.settings;

import com.jagrosh.jdautilities.command.GuildSettingsProvider;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;

/**
 * @author John Grosh <john.a.grosh@gmail.com>
 */
public class Settings implements GuildSettingsProvider {
    private final SettingsManager manager;
    protected long textId, voiceId, roleId, punRole, punVoice;
    private int volume;
    private String defaultPlaylist;
    private boolean repeatMode;

    public Settings(SettingsManager manager, String textId, String voiceId, String roleId, int volume, String defaultPlaylist, boolean repeatMode, String punRole, String punVoice) {
        this.manager = manager;
        try {
            this.textId = Long.parseLong(textId);
        } catch (NumberFormatException e) {
            this.textId = 0;
        }
        try {
            this.voiceId = Long.parseLong(voiceId);
        } catch (NumberFormatException e) {
            this.voiceId = 0;
        }
        try {
            this.roleId = Long.parseLong(roleId);
        } catch (NumberFormatException e) {
            this.roleId = 0;
        }
        try {
            this.punRole = Long.parseLong(punRole);
        } catch (NumberFormatException e) {
            this.punRole = 0;
        }
        try {
            this.punVoice = Long.parseLong(punVoice);
        } catch (NumberFormatException e) {
            this.punVoice = 0;
        }
        this.volume = volume;
        this.defaultPlaylist = defaultPlaylist;
        this.repeatMode = repeatMode;
    }

    public Settings(SettingsManager manager, long textId, long voiceId, long roleId, int volume, String defaultPlaylist, boolean repeatMode, long punRole, long punVoice) {
        this.manager = manager;
        this.textId = textId;
        this.voiceId = voiceId;
        this.roleId = roleId;
        this.volume = volume;
        this.defaultPlaylist = defaultPlaylist;
        this.repeatMode = repeatMode;
        this.punRole = punRole;
        this.punVoice = punVoice;
    }

    // Getters
    public TextChannel getTextChannel(Guild guild) {
        return guild == null ? null : guild.getTextChannelById(textId);
    }

    public VoiceChannel getVoiceChannel(Guild guild) {
        return guild == null ? null : guild.getVoiceChannelById(voiceId);
    }

    public Role getRole(Guild guild) {
        return guild == null ? null : guild.getRoleById(roleId);
    }

    public int getVolume() {
        return volume;
    }

    public String getDefaultPlaylist() {
        return defaultPlaylist;
    }

    public boolean getRepeatMode() {
        return repeatMode;
    }

    public long getPunRole() {
        return punRole;
    }

    public long getPunVoice() {
        return punVoice;
    }

    // Setters
    public void setTextChannel(TextChannel tc) {
        this.textId = tc == null ? 0 : tc.getIdLong();
        this.manager.writeSettings();
    }

    public void setVoiceChannel(VoiceChannel vc) {
        this.voiceId = vc == null ? 0 : vc.getIdLong();
        this.manager.writeSettings();
    }

    public void setDJRole(Role role) {
        this.roleId = role == null ? 0 : role.getIdLong();
        this.manager.writeSettings();
    }

    public void setVolume(int volume) {
        this.volume = volume;
        this.manager.writeSettings();
    }

    public void setDefaultPlaylist(String defaultPlaylist) {
        this.defaultPlaylist = defaultPlaylist;
        this.manager.writeSettings();
    }

    public void setRepeatMode(boolean mode) {
        this.repeatMode = mode;
        this.manager.writeSettings();
    }

    public void setPunRole(long punRole) {
        this.punRole = punRole;
    }

    public void setPunVoice(long punVoice) {
        this.punVoice = punVoice;
    }
}
