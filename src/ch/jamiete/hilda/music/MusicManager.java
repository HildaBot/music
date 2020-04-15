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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.Sanity;
import ch.jamiete.hilda.Util;
import ch.jamiete.hilda.music.tasks.MusicServerChecker;
import ch.jamiete.hilda.music.tasks.MusicStartupCheckerTask;
import ch.jamiete.hilda.plugins.HildaPlugin;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;

public class MusicManager {
    /**
     * The maximum number of {@link QueueItem}s that can be in a {@link MusicServer} queue.
     */
    public static final int QUEUE_LIMIT = 100;
    /**
     * The maximum milliseconds a song can be.
     */
    public static final long TIME_LIMIT = 3600000L; // 1 hour
    /**
     * The maximum milliseconds a song can be for a DJ.
     */
    public static final long DJ_TIME_LIMIT = 10800000L; // 3 hours

    /**
     * Converts an {@link AudioTrack} to a human-readable {@link String}.
     * @param track The track to convert.
     * @return The result of the conversion. Contains spaces and markdown formatting.
     */
    public static String getFriendly(final AudioTrack track) {
        final MessageBuilder mb = new MessageBuilder();

        if (track.getInfo().title != null) {
            mb.append(track.getInfo().title.replaceAll("\\*", "\\\\*"), MessageBuilder.Formatting.BOLD);

            if (track.getInfo().author != null) {
                mb.append(" by ").append(track.getInfo().author.replaceAll("\\*", "\\\\*"), MessageBuilder.Formatting.BOLD);
            }
        } else {
            mb.append(track.getIdentifier());
        }

        return mb.build().getContentRaw().trim();
    }

    public static String getFriendlyGuild(final Guild guild) {
        return guild.getName() + " (" + guild.getId() + ')';
    }

    /**
     * Converts an {@link AudioTrack} to a human-readable {@link String} containing the time of the track.
     * @param track The track to convert.
     * @return The result of the conversion. Contains spaces and markdown formatting.
     */
    public static String getFriendlyTime(final AudioTrack track) {
        final StringBuilder sb = new StringBuilder();

        if (!"null".equalsIgnoreCase("" + track.getDuration())) { // TODO Not use a hacky workaround
            sb.append(Util.getFriendlyTime(track.getDuration()));
        }

        return sb.toString().trim();
    }

    private int played;
    private int queued;

    private final Hilda hilda;
    private final HildaPlugin plugin;
    private final AudioPlayerManager playerManager;
    private final List<MusicServer> servers = new ArrayList<>();
    private final HashMap<Long, Long> recent = new HashMap<>();

    public MusicManager(final Hilda hilda, final HildaPlugin plugin) {
        this.hilda = hilda;
        this.plugin = plugin;

        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(this.playerManager);
        AudioSourceManagers.registerLocalSource(this.playerManager);

        this.hilda.getExecutor().scheduleAtFixedRate(new MusicServerChecker(this), 15L, 1L, TimeUnit.MINUTES);
    }

    public int getNumber() {
        synchronized (this.servers) {
            return this.servers.size();
        }
    }

    /**
     * Increment the number of songs played this session.
     */
    public final void addPlayed() {
        this.played++;
    }

    /**
     * Increment the number of songs queued this session.
     */
    public final void addQueued() {
        this.queued++;
    }

    /**
     * Add a recently shutdown server to the tracker.
     * @param id The server that recently shutdown.
     */
    public final void addRecent(final long id) {
        this.recent.put(id, System.currentTimeMillis());
    }

    /**
     * Creates a {@link MusicServer} for a {@link Guild}.
     * @param guild The Guild the server should represent.
     * @return A new {@link MusicServer}.
     * @throws IllegalArgumentException If the Guild already has a server associated with it.
     */
    public final MusicServer createServer(final Guild guild) {
        Sanity.truthiness(!this.hasServer(guild), "A server with the guild " + guild.getId() + " already exists.");

        final MusicServer server = new MusicServer(this, this.playerManager.createPlayer(), guild);

        synchronized (this.servers) {
            this.servers.add(server);
        }

        this.hilda.getExecutor().schedule(new MusicStartupCheckerTask(server), 90L, TimeUnit.SECONDS);

        return server;
    }

    /**
     * Gets the AudioPlayerManager instance.
     * @return The AudioPlayerManager instance.
     */
    public final AudioPlayerManager getAudioPlayerManager() {
        return this.playerManager;
    }

    public final Hilda getHilda() {
        return this.hilda;
    }

    /**
     * Get the number of songs played this session.
     * @return The number of songs played this session.
     */
    public final int getPlayed() {
        return this.played;
    }

    public final HildaPlugin getPlugin() {
        return this.plugin;
    }

    /**
     * Get the number of songs queued this session.
     * @return The number of songs queued this session.
     */
    public final int getQueued() {
        return this.queued;
    }

    /**
     * Get the time the server last shutdown.
     * @param id The server to check.
     * @return The time in milliseconds since the server last shutdown or {@code Long.MAX_VALUE} if it did not recently shutdown.
     */
    public final long getRecent(final long id) {
        return this.recent.getOrDefault(id, Long.MAX_VALUE);
    }

    /**
     * Searches for the server related to the guild.
     * @param guild The guild to test for.
     * @return The {@link MusicServer} related to the guild or {@code null} if no server exists.
     */
    public final MusicServer getServer(final Guild guild) {
        synchronized (this.servers) {
            for (final MusicServer server : this.servers) {
                if (server.getGuild() == guild) {
                    return server;
                }
            }
        }

        return null;
    }

    /**
     * Lists the servers registered.
     * @return A {@link MusicServer} array containing all registered servers.
     */
    public final List<MusicServer> getServers() {
        synchronized (this.servers) {
            return Util.unmodifiableList(this.servers);
        }
    }

    /**
     * Checks whether a server exists for the guild.
     * @param guild The guild to test for.
     * @return Whether a server exists.
     */
    public final boolean hasServer(final Guild guild) {
        return this.getServer(guild) != null;
    }

    /**
     * Checks whether the user who sent the message is a DJ in the {@link Guild} the message was sent to.
     * @param message The message to test.
     * @return Whether the user who sent the message is a DJ.
     */
    public static boolean isDJ(final Message message) {
        final Member member = message.getGuild().getMember(message.getAuthor());

        if (member.hasPermission(Permission.ADMINISTRATOR) || member.hasPermission(Permission.MANAGE_SERVER)) {
            return true;
        }

        for (final Role role : message.getGuild().getRolesByName("dj", true)) {
            if (member.getRoles().contains(role)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Remove the time the server last shutdown.
     * @param id The server to remove.
     */
    public final void removeRecent(final long id) {
        this.recent.remove(id);
    }

    /**
     * Forgets a server.
     * @param server The server to forget.
     */
    public final void removeServer(final MusicServer server) {
        synchronized (this.servers) {
            this.servers.remove(server);
        }
    }

}
