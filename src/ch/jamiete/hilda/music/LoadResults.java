/*
 * Copyright 2017 jamietech
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.jamiete.hilda.music;

import java.util.List;
import java.util.logging.Level;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.Util;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;

public class LoadResults implements AudioLoadResultHandler {
    private final MusicServer server;
    private final Message message;
    private final Member member;
    private final boolean search;

    public LoadResults(final MusicServer server, final Message message) {
        this(server, message, false);
    }

    public LoadResults(final MusicServer server, final Message message, final boolean search) {
        this.server = server;
        this.message = message;
        this.member = message.getGuild().getMember(message.getAuthor());
        this.search = search;
    }

    @Override
    public final void loadFailed(final FriendlyException e) {
        if (e.getMessage().startsWith("The uploader has not made this video available")) {
            this.reply("That track is geo-blocked and cannot be played.");
        } else if (e.getMessage().startsWith("This video contains content from")) {
            this.reply("That track has been restricted by the copyright holder and cannot be played.");
        } else if (e.getMessage().startsWith("This video is not available")) {
            this.reply("That track is not available to me and cannot be played.");
        } else {
            Hilda.getLogger().log(Level.WARNING, "Couldn't load track in " + MusicManager.getFriendlyGuild(this.message.getGuild()), e);
            this.reply("I couldn't load that track: " + e.getMessage() + '.');
            Hilda.getLogger().log(Level.WARNING, "Couldn't load track in " + MusicManager.getFriendlyGuild(this.message.getGuild()), e);
        }

        this.server.prompt();
    }

    @Override
    public final void noMatches() {
        Hilda.getLogger().info("Failed to find anything for query " + this.message.getContentDisplay());
        this.reply("I couldn't find anything matching that query.");
        this.server.prompt();
    }

    @Override
    public final void playlistLoaded(final AudioPlaylist playlist) {
        Hilda.getLogger().fine("Loaded a playlist");

        if (this.search) {
            Hilda.getLogger().fine("Playlist came from a search query!");
            this.tryLoadTrack(playlist.getTracks().get(0));
            return;
        }

        if (MusicManager.isDJ(this.message)) {
            Hilda.getLogger().info("Queuing songs for DJ/admin...");
            long duration = 0L;
            final long previous = this.server.getDuration();
            int queued = 0;

            for (final AudioTrack track : playlist.getTracks()) {
                if (!this.server.isQueued(track) && !this.server.isQueueFull() && ((MusicManager.isDJ(this.message) && (track.getDuration() < MusicManager.DJ_TIME_LIMIT)) || (!MusicManager.isDJ(this.message) && (track.getDuration() < MusicManager.TIME_LIMIT)))) {
                    this.server.queue(new QueueItem(track, this.member.getUser().getId()));
                    queued++;
                    duration += track.getDuration();
                }
            }

            if (this.message.getGuild().getSelfMember().hasPermission(this.message.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                this.message.delete().reason("I automatically delete some command invocations. If you don't want this to happen, remove my manage messages permission in the channel.").queue();
            }

            final StringBuilder sb = new StringBuilder();
            sb.append("Queued ");

            if (queued < playlist.getTracks().size()) {
                sb.append(queued).append('/').append(playlist.getTracks().size());
            } else {
                sb.append(playlist.getTracks().size());
            }

            sb.append(" tracks for ").append(this.member.getEffectiveName());

            if ((this.server.getPlaying() == null) || (this.server.getPlayer().getPlayingTrack() == null)) {
                // Something's gone wrong
                sb.append("; up soon!");
            } else if (this.server.getPlaying().getTrack() == playlist.getTracks().get(0)) {
                sb.append("; up now! Playing for ").append(Util.getFriendlyTime(duration)).append('.');
            } else {
                sb.append("; playing for ").append(Util.getFriendlyTime(duration));
                sb.append(" in ").append(Util.getFriendlyTime(previous)).append('!');
            }

            this.reply(sb.toString());
            Hilda.getLogger().info("Queued " + queued + '/' + playlist.getTracks().size());
        } else {
            Hilda.getLogger().fine("Trying to queue first from playlist for non-DJ...");
            this.tryLoadTrack(playlist.getTracks().get(0));
        }
    }

    private void reply(final String message) {
        this.message.getChannel().sendMessage(message).queue();
    }

    @Override
    public final void trackLoaded(final AudioTrack track) {
        Hilda.getLogger().fine("Loaded a track");
        this.tryLoadTrack(track);
    }

    private void tryLoadTrack(final AudioTrack track) {
        if (this.server.isQueued(track)) {
            Hilda.getLogger().fine("Song already queued.");
            this.reply("That song is already queued.");
            this.server.prompt();
            return;
        }

        if (this.server.isQueueFull()) {
            Hilda.getLogger().fine("Queue full");
            this.reply("There is no space left in the queue!");
            this.server.prompt();
            return;
        }

        if ((MusicManager.isDJ(this.message) && (track.getDuration() > MusicManager.DJ_TIME_LIMIT)) || (!MusicManager.isDJ(this.message) && (track.getDuration() > MusicManager.TIME_LIMIT))) {
            Hilda.getLogger().fine("Song too long; " + track.getDuration() + ">" + MusicManager.TIME_LIMIT + ".");
            this.reply("The song is too long to be queued.");
            this.server.prompt();
            return;
        }

        if (this.message.getGuild().getSelfMember().hasPermission(this.message.getTextChannel(), Permission.MESSAGE_MANAGE)) {
            this.message.delete().reason("I automatically delete some command invocations. If you don't want this to happen, remove my manage messages permission in the channel.").queue();
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("Queued ").append(MusicManager.getFriendly(track)).append(" for ").append(this.member.getEffectiveName());

        if (this.server.getPlaying() == null) {
            sb.append("; up next!");
        } else if (this.server.getPlayer().getPlayingTrack() == null) {
            // Something's gone wrong
            sb.append("; up soon!");
        } else {
            sb.append("; playing in ").append(Util.getFriendlyTime(this.server.getDuration())).append('!');
        }

        final List<QueueItem> queue = this.server.getQueue();
        if (!queue.isEmpty()) {
            sb.append(" (Queue code ").append(queue.size() + 1).append(')');
        }

        this.reply(Util.sanitise(sb.toString()));
        this.server.queue(new QueueItem(track, this.member.getUser().getId()));
        Hilda.getLogger().fine("Queued a song");
    }

}
