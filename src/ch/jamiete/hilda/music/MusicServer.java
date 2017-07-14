/*******************************************************************************
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
 *******************************************************************************/
package ch.jamiete.hilda.music;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;
import com.google.gson.JsonElement;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import ch.jamiete.hilda.Start;
import ch.jamiete.hilda.Util;
import ch.jamiete.hilda.configuration.Configuration;
import ch.jamiete.hilda.events.EventHandler;
import ch.jamiete.hilda.music.tasks.MusicLeaveTask;
import ch.jamiete.hilda.runnables.GameSetTask;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceDeafenEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMuteEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;

/**
 * This class represents a {@link net.dv8tion.jda.core.entities.Guild Guild} that music is being played on.
 */
public class MusicServer extends AudioEventAdapter {
    private final MusicManager manager;
    private final AudioPlayer player;
    private final Configuration config;
    private final AudioPlayerSendHandler handler;

    private final Guild guild;
    private VoiceChannel channel;

    private boolean stopping = false;
    private ScheduledFuture<?> task;

    private final List<QueueItem> queue = Collections.synchronizedList(new ArrayList<QueueItem>());
    private final ArrayList<String> skips = new ArrayList<String>();

    private QueueItem now;

    private String lastplaying;

    public MusicServer(final MusicManager manager, final AudioPlayer player, final Guild guild) {
        this.manager = manager;
        this.player = player;
        this.player.addListener(this);
        this.handler = new AudioPlayerSendHandler(player);
        this.guild = guild;
        this.guild.getAudioManager().setSendingHandler(this.handler);
        this.config = this.manager.getHilda().getConfigurationManager().getConfiguration(this.manager.getPlugin(), this.guild.getId());
        this.manager.getHilda().getBot().addEventListener(this);
    }

    /**
     * Adds a skip to the currently playing song.
     * @param string The ID of the user skipping the song.
     */
    public void addSkip(final String string) {
        this.skips.add(string);
    }

    /**
     * Gets whether it is safe for the bot to shutdown. <p>
     * This checks whether the bot is in a server with someone sharing a mutual guild. A Discord bug will result in the mutual no longer being able to hear the bot until they rejoin the voice channel.
     * @return Whether it is safe.
     */
    public boolean canShutdown() {
        boolean clash = false;

        for (final MusicServer server : this.manager.getServers()) {
            if (server == this || server.getPlayer().getPlayingTrack() == null) {
                continue;
            }

            for (final Member member : server.getChannel().getMembers().stream().filter(m -> !m.getUser().isBot()).collect(Collectors.toList())) {
                if (this.guild.getMember(member.getUser()) != null) {
                    clash = true;
                }
            }
        }

        return clash;
    }

    /**
     * Gets the channel the server is playing to.
     * @return The channel the server is playing to or {@code null} if there is none.
     */
    public VoiceChannel getChannel() {
        return this.channel;
    }

    /**
     * Gets the configuration for this server.
     * @return This server's configuration.
     */
    public Configuration getConfig() {
        return this.config;
    }

    /**
     * Gets the remaining time until the queue as it stands will end.
     * @return The remaining time in ms
     */
    public long getDuration() {
        long duration = 0;
        final AudioTrack current = this.player.getPlayingTrack();

        duration += current.getDuration() - current.getPosition();

        synchronized (this.queue) {
            final Iterator<QueueItem> iterator = this.queue.iterator();

            while (iterator.hasNext()) {
                final QueueItem item = iterator.next();
                duration += item.getTrack().getDuration();
            }
        }

        return duration;
    }

    /**
     * Gets the guild the server is associated with.
     * @return The guild.
     */
    public Guild getGuild() {
        return this.guild;
    }

    /**
     * Gets the audio player used by the server.
     * @return The audio player.
     */
    public AudioPlayer getPlayer() {
        return this.player;
    }

    /**
     * Gets the QueueItem the server is currently playing.
     * @return The queue item.
     */
    public QueueItem getPlaying() {
        return this.now;
    }

    /**
     * Gets a copy of the queue the server contains.
     * @return An unmodifiable list of the queue.
     */
    public List<QueueItem> getQueue() {
        synchronized (this.queue) {
            return Collections.unmodifiableList(this.queue);
        }
    }

    /**
     * Gets the list of queue items in debug format. <br>
     * For example: [track=afds909,user=034092if09i]
     * @return The list of queue items.
     */
    public String getQueueNames() {
        final StringBuilder sb = new StringBuilder();

        synchronized (this.queue) {
            final Iterator<QueueItem> iterator = this.queue.iterator();

            while (iterator.hasNext()) {
                final QueueItem q = iterator.next();

                sb.append("[").append("track=");
                sb.append(q.getTrack().getIdentifier()).append(", ");
                sb.append("user=").append(q.getUserId());
                sb.append("] ");
            }
        }

        return sb.toString().trim();
    }

    /**
     * Helper method. <br>
     * Returns the self user of the bot in member form.
     * @return
     */
    public Member getSelf() {
        return this.guild.getMember(this.manager.getHilda().getBot().getSelfUser());
    }

    /**
     * Gets the number of skips currently registered.
     * @return The number of skips currently registered.
     */
    public int getSkips() {
        return this.skips.size();
    }

    /**
     * Gets the friendly name of the current song playing. If there is no song playing returns null.
     * @return the friendly name of the current song.
     */
    public String getSong() {
        if (this.now == null) {
            return null;
        }

        return new MessageBuilder().append(MusicManager.getFriendly(this.now.getTrack())).build().getStrippedContent();
    }

    /**
     * Gets the number of users in the server's channel that are not bots and are not defeaned.
     * @return The number of users in the server's channel that are not bots and are not defeaned.
     */
    public int getUsers() {
        if (this.channel == null || this.channel != null && this.channel.getMembers() == null) {
            return 0;
        }

        int users = 0;

        for (final Member member : this.channel.getMembers()) {
            if (!member.getUser().isBot() && !member.getVoiceState().isDeafened()) {
                users++;
            }
        }

        return users;
    }

    /**
     * Checks whether a user ID has sought that the current song be skipped.
     * @param string The user ID to be tested.
     * @return Whether the user ID has sought that the current song be skipped.
     */
    public boolean hasSkipped(final String string) {
        return this.skips.contains(string);
    }

    /**
     * Gets whether this server is queued to leave.
     * @return
     */
    public boolean isLeaveQueued() {
        return this.task != null;
    }

    /**
     * Checks whether an audio track is in the queue.
     * @param track The track to test.
     * @return Whether the track is in the queue.
     */
    public boolean isQueued(final AudioTrack track) {
        if (this.player.getPlayingTrack() != null && this.player.getPlayingTrack().getIdentifier().equals(track.getIdentifier())) {
            return true;
        }

        synchronized (this.queue) {
            final Iterator<QueueItem> iterator = this.queue.iterator();
            while (iterator.hasNext()) {
                if (track.getIdentifier().equalsIgnoreCase(iterator.next().getTrack().getIdentifier())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Checks whether the queue is full.
     * @return Whether the queue is full.
     */
    public boolean isQueueFull() {
        return this.queue.size() >= MusicManager.QUEUE_LIMIT;
    }

    /**
     * Gets whether the server is shutting down.
     * @return whether server is shutting down
     */
    public boolean isStopping() {
        return this.stopping;
    }

    @EventHandler
    public void onEvent(final Event e) {
        if (this.stopping) {
            return;
        }

        if (e instanceof GuildVoiceLeaveEvent) {
            final GuildVoiceLeaveEvent event = (GuildVoiceLeaveEvent) e;

            if (event.getChannelLeft() == this.channel) {
                if (this.getUsers() == 0) {
                    MusicManager.getLogger().fine("Stopping because all users left the channel");
                    this.shutdown();
                    this.getPlayer().stopTrack();
                    this.prompt();
                }

                if (this.hasSkipped(event.getMember().getUser().getId())) {
                    this.removeSkip(event.getMember().getUser().getId());
                }

                if (this.shouldSkip()) {
                    MusicManager.getLogger().fine("Skipping because a user left the channel");
                    this.sendMessage("Skipping because user leaving changed skip count...");
                    this.player.stopTrack();
                }
            }
        }

        if (e instanceof GuildVoiceMoveEvent) {
            final GuildVoiceMoveEvent event = (GuildVoiceMoveEvent) e;

            if (event.getMember() == this.getSelf()) {
                this.channel = event.getChannelJoined();

                if (this.getUsers() == 0) {
                    this.shutdown();
                }
            }

            if (event.getChannelLeft() == this.channel) {
                if (this.getUsers() == 0) {
                    MusicManager.getLogger().fine("Stopping because the last user moved from the channel");
                    this.shutdown();
                }

                if (this.hasSkipped(event.getMember().getUser().getId())) {
                    this.removeSkip(event.getMember().getUser().getId());
                }

                if (this.shouldSkip()) {
                    MusicManager.getLogger().fine("Skipping because a user moved from the channel");
                    this.sendMessage("Skipping because user leaving changed skip count...");
                    this.player.stopTrack();
                }
            }
        }

        if (e instanceof GuildVoiceMuteEvent) {
            final GuildVoiceMuteEvent event = (GuildVoiceMuteEvent) e;

            if (!event.isMuted()) {
                return;
            }

            if (event.getMember() == this.getSelf()) {
                if (this.getSelf().hasPermission(this.channel, Permission.VOICE_MUTE_OTHERS)) {
                    MusicManager.getLogger().fine("Skipping because I was muted");
                    this.sendMessage("Skipping song because I was muted...");
                    this.guild.getController().setMute(this.getSelf(), false).queue();
                    this.player.stopTrack();
                } else {
                    MusicManager.getLogger().fine("Stopping because I was muted and didn't have permission");
                    this.sendMessage("Stopping playback because I was muted...");
                    this.shutdown();
                }
            }
        }

        if (e instanceof GuildVoiceDeafenEvent) {
            final GuildVoiceDeafenEvent event = (GuildVoiceDeafenEvent) e;

            if (event.isDeafened() && event.getMember().getVoiceState().getChannel() == this.channel) {
                if (this.hasSkipped(event.getMember().getUser().getId())) {
                    this.removeSkip(event.getMember().getUser().getId());
                }

                if (this.shouldSkip()) {
                    MusicManager.getLogger().fine("Skipping because a user became deafened");
                    this.sendMessage("Skipping because user deafening changed skip count...");
                    this.player.stopTrack();
                }
            }
        }
    }

    @Override
    public void onTrackEnd(final AudioPlayer player, final AudioTrack track, final AudioTrackEndReason endReason) {
        MusicManager.getLogger().fine("Track ended " + track.getIdentifier());


        if (this.stopping) {
            MusicManager.getLogger().fine("Stopping, so giving up...");
            return;
        }

        if (this.queue.isEmpty()) {
            MusicManager.getLogger().fine("Queue was empty...");
            this.sendMessage("Queue concluded.");
            this.prompt();
            return;
        }

        if (this.isQueued(track)) {
            MusicManager.getLogger().fine("Track was still in queue; deleting.");
            this.queue.remove(track);
        }

        if (endReason.mayStartNext || endReason == AudioTrackEndReason.STOPPED) {
            MusicManager.getLogger().fine("Starting next song...");
            this.play(this.queue.get(0));
        }
    }

    @Override
    public void onTrackException(final AudioPlayer player, final AudioTrack track, final FriendlyException exception) {
        this.setGame(null);

        if (exception.getCause() instanceof UnsatisfiedLinkError) {
            MusicManager.getLogger().warning("Encountered song I didn't know how to play.");
            this.sendMessage("I don't know how to play that type of file; skipping.");
        } else if (exception.getMessage().startsWith("This video contains content from")) {
            this.sendMessage("That track has been restricted by the copyright holder and cannot be played.");
        } else {
            MusicManager.getLogger().log(Level.WARNING, "Encountered an exception while playing " + track.getIdentifier() + " in " + this.guild.getName() + "...", exception);
            this.sendMessage("Track exception (" + exception.getMessage() + "); skipping.");
        }

        this.play(this.queue.get(0));
    }

    @Override
    public void onTrackStart(final AudioPlayer player, final AudioTrack track) {
        MusicManager.getLogger().fine("Track began " + track.getIdentifier());

        this.skips.clear();
        this.manager.addPlayed();

        if (this.getSelf().getVoiceState().isGuildMuted() && this.getSelf().hasPermission(this.channel, Permission.VOICE_MUTE_OTHERS)) {
            this.guild.getController().setMute(this.getSelf(), false).queue();
        }

        // Ensure track gone from queue
        this.queue.removeIf(item -> item.getTrack().equals(track));

        this.prompt();
    }

    @Override
    public void onTrackStuck(final AudioPlayer player, final AudioTrack track, final long thresholdMs) {
        MusicManager.getLogger().warning("Track " + track.getIdentifier() + " got stuck in " + this.guild.getName() + "; skipping...");
        this.sendMessage("Track stuck; skipping.");
        this.play(this.queue.get(0));
    }

    /**
     * Attemts to play a queue item. If {@code null} is passed, the server will check if it should destroy itself.
     * @param item The item to play.
     */
    public void play(final QueueItem item) {
        MusicManager.getLogger().info("Playing a song in " + this.guild.getName() + " " + this.guild.getId() + " " + item);

        if (this.task != null) {
            this.task.cancel(false);
            this.task = null;
        }

        this.now = item;

        this.player.playTrack(item == null ? null : item.getTrack());

        if (item == null) {
            this.prompt();
        } else {
            this.sendMessage("Now playing " + MusicManager.getFriendly(item.getTrack()) + " as requested by " + this.guild.getMemberById(item.getUserId()).getEffectiveName() + ".");
            this.setGame(this.getSong());
        }

        if (this.isQueued(item.getTrack())) {
            this.queue.remove(item);
        }

        if (Start.DEBUG) {
            final StringBuilder sb = new StringBuilder();

            sb.append("There are ").append(this.queue.size()).append(" songs queued: ");
            sb.append(this.getQueueNames());

            MusicManager.getLogger().fine(sb.toString());
        }
    }

    /**
     * Prompt the server to check whether it should still exist. If no songs are playing and the queue is empty the server will shut itself down.
     */
    public void prompt() {
        if (this.stopping) {
            return;
        }

        MusicManager.getLogger().fine("Deciding whether to shut down...");

        if (this.player.getPlayingTrack() == null && this.queue.isEmpty()) {
            MusicManager.getLogger().fine("Shutting down because there's nothing playing...");
            this.shutdown();
            return;
        }

        MusicManager.getLogger().fine("Decided not to.");
    }

    /**
     * Adds an item to the end of the queue. If no song is currently playing, the song will be played.
     * @param queue The item to queue.
     */
    public void queue(final QueueItem queue) {
        this.queue(queue, false);
    }

    /**
     * Adds an item to the queue. If no song is currently playing, the song will be played.
     * @param queue The item to queue.
     * @param front Whether the item should be placed at the top of the queue.
     */
    public void queue(final QueueItem queue, final boolean front) {
        MusicManager.getLogger().fine("Queueing " + queue);

        if (this.now == null) {
            this.play(queue);
            return;
        }

        if (front) {
            this.queue.add(0, queue);
        } else {
            this.queue.add(queue);
        }

        this.manager.addQueued();

        if (Start.DEBUG) {
            final StringBuilder sb = new StringBuilder();

            sb.append("There are ").append(this.queue.size()).append(" songs queued: ");
            sb.append(this.getQueueNames());

            MusicManager.getLogger().fine(sb.toString());
        }
    }

    /**
     * Queues a bot shutdown.
     */
    public void queueShutdown() {
        this.task = this.manager.getHilda().getExecutor().schedule(new MusicLeaveTask(this), 5, TimeUnit.MINUTES);
    }

    /**
     * Removes a user ID from the skip list.
     * @param string The user ID to remove.
     */
    public void removeSkip(final String string) {
        this.skips.remove(string);
    }

    /**
     * Attempts to send a message. The bot will try the following channels in order: "bot", "bots", the default channel, the first channel that can be messaged.
     * The bot will pick the first of these that it can send a message to. If the bot cannot find any channels it will shutdown.
     * @param message
     */
    public void sendMessage(final String message) {
        TextChannel channel = null;

        final JsonElement output = this.config.get().get("output");

        if (output != null) {
            channel = this.guild.getTextChannelById(output.getAsString());
        }

        if (channel == null) {
            for (final TextChannel chan : this.guild.getTextChannels()) {
                if (chan.getName().equalsIgnoreCase("bot") && chan.canTalk()) {
                    channel = chan;
                }

                if (chan.getName().equalsIgnoreCase("bots") && chan.canTalk()) {
                    channel = chan;
                }
            }
        }

        if (channel == null) {
            if (this.guild.getPublicChannel().canTalk()) {
                channel = this.guild.getPublicChannel();
            } else {
                final Optional<TextChannel> chan = this.guild.getTextChannels().stream().filter(c -> c.canTalk()).findFirst();

                if (chan.isPresent()) {
                    channel = chan.get();
                } else {
                    MusicManager.getLogger().severe("Couldn't find any channels to talk to in " + this.guild.getName() + " " + this.guild.getId() + "; leaving...");
                    this.shutdown();
                    return;
                }
            }
        }

        MusicManager.getLogger().fine("Sending a message; decided to use " + channel.getName() + "...");
        channel.sendMessage(Util.sanitise(message)).queue();
    }

    /**
     * Sets the voice channel that the bot should use.
     * If the bot is already in a channel it will leave that channel.
     * If the bot cannot join the channel it will shutdown.
     * @param channel The channel to use.
     */
    public void setChannel(final VoiceChannel channel) {
        MusicManager.getLogger().fine("Setting channel to " + channel.getName());

        if (this.channel != null) {
            MusicManager.getLogger().fine("Was already in a channel; leaving...");

            if (this.guild.getAudioManager().isConnected()) {
                this.guild.getAudioManager().closeAudioConnection();
            }
        }

        this.guild.getAudioManager().setSelfDeafened(true);
        this.guild.getAudioManager().setAutoReconnect(true);

        try {
            this.guild.getAudioManager().openAudioConnection(channel);
        } catch (final PermissionException e) {
            MusicManager.getLogger().info("Couldn't connect to a voice channel in " + this.guild.getName() + " " + this.guild.getId() + "; shutting down...");
            this.sendMessage("I couldn't connect to the voice channel; aborting.");
            this.shutdown();
        }

        if (this.getSelf().getVoiceState().isGuildMuted() && this.getSelf().hasPermission(channel, Permission.VOICE_MUTE_OTHERS)) {
            this.guild.getController().setMute(this.getSelf(), false).queue();
        }

        this.channel = channel;
    }

    /**
     * Sets the name of the game displayed by the bot. <br>
     * If the parameter is null, the name will be reset if the current name is the name of the song last playing on this server.
     * @param set The name to be displayed, or null to reset.
     */
    private void setGame(final String set) {
        MusicManager.getLogger().fine("Queueing game to " + set);
        final Game game = this.manager.getHilda().getBot().getPresence().getGame();

        if (set == null && this.lastplaying == null) {
            return;
        }

        if (set == null) {
            if (game != null && game.getName().equalsIgnoreCase(this.lastplaying) && (this.queue.isEmpty() || this.stopping)) {
                this.manager.getHilda().getExecutor().execute(new GameSetTask(this.manager.getHilda(), set));
            }

            this.lastplaying = null;
        }

        this.manager.getHilda().getExecutor().execute(new GameSetTask(this.manager.getHilda(), set));
        this.lastplaying = set;
    }

    public void setLeave(final ScheduledFuture<?> task) {
        this.task = task;
    }

    /**
     * Checks whether the song should be skipped.
     * @return Whether the song should be skipped.
     */
    public boolean shouldSkip() {
        return this.skips.size() >= (int) Math.ceil((double) this.getUsers() / (double) 2);
    }

    /**
     * Shuffles the queue.
     */
    public void shuffle() {
        synchronized (this.queue) {
            Collections.shuffle(this.queue);
        }
    }

    /**
     * Will shut the bot down immediately (and leave the voice channel) if possible, or queue a shutdown.
     */
    public void shutdown() {
        if (this.canShutdown()) {
            this.shutdownNow(true);
        } else {
            this.queueShutdown();
        }
    }

    /**
     * Shuts down the bot immediately.
     */
    public void shutdownNow(final boolean leave) {
        if (this.stopping) {
            return;
        }

        MusicManager.getLogger().info("Shutting down " + this.guild.getName() + " " + this.guild.getId() + "...");

        this.stopping = true;
        this.manager.getHilda().getBot().removeEventListener(this);
        this.guild.getAudioManager().setSendingHandler(null);
        this.player.destroy();
        this.queue.clear();
        this.channel = null;

        this.setGame(null);

        if (this.guild.getAudioManager().isConnected() && leave) {
            this.guild.getAudioManager().closeAudioConnection();
        }

        this.manager.addRecent(this.guild.getIdLong());
        this.manager.removeServer(this);

        for (final MusicServer server : this.manager.getServers()) {
            if (server.isLeaveQueued()) {
                server.shutdown();
            }
        }
    }

    /**
     * Removes a QueueItem from the queue.
     * @param item The item to remove.
     */
    public void unqueue(final QueueItem item) {
        this.queue.remove(item);
    }

}
