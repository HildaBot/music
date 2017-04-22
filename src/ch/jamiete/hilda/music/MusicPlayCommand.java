package ch.jamiete.hilda.music;

import java.util.Arrays;
import java.util.logging.Level;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.Util;
import ch.jamiete.hilda.commands.ChannelCommand;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;

public class MusicPlayCommand extends ChannelCommand {

    private class LoadResults implements AudioLoadResultHandler {
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
        public void loadFailed(final FriendlyException e) {
            MusicManager.getLogger().log(Level.WARNING, "Couldn't load track", e);
            MusicPlayCommand.this.reply(this.message, "I couldn't load that track: " + e.getMessage() + ".");
            Hilda.getLogger().log(Level.WARNING, "Couldn't load track", e);
            this.server.prompt();
        }

        @Override
        public void noMatches() {
            MusicManager.getLogger().info("Failed to find anything for query " + this.message.getContent());
            MusicPlayCommand.this.reply(this.message, "I couldn't find anything matching that query.");
            this.server.prompt();
        }

        @Override
        public void playlistLoaded(final AudioPlaylist playlist) {
            MusicManager.getLogger().fine("Loaded a playlist");

            if (this.search) {
                MusicManager.getLogger().fine("Playlist came from a search query!");

                if (this.server.isQueued(playlist.getTracks().get(0))) {
                    MusicManager.getLogger().fine("Song already queued.");
                    MusicPlayCommand.this.reply(this.message, "That song is already queued.");
                    this.server.prompt();
                    return;
                }

                if (this.server.isQueueFull()) {
                    MusicManager.getLogger().fine("Queue full.");
                    MusicPlayCommand.this.reply(this.message, "There is no space left in the queue!");
                    this.server.prompt();
                    return;
                }

                if (MusicPlayCommand.this.manager.isDJ(message) && playlist.getTracks().get(0).getDuration() > MusicManager.DJ_TIME_LIMIT || !MusicPlayCommand.this.manager.isDJ(message) && playlist.getTracks().get(0).getDuration() > MusicManager.TIME_LIMIT) {
                    MusicManager.getLogger().fine("Song too long; " + playlist.getTracks().get(0).getDuration() + ">" + MusicManager.TIME_LIMIT + ".");
                    MusicPlayCommand.this.reply(this.message, "The song is too long to be queued.");
                    this.server.prompt();
                    return;
                }

                if (this.message.getGuild().getSelfMember().hasPermission(this.message.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                    this.message.delete().queue();
                }

                MusicManager.getLogger().info("Queueing search result.");

                final StringBuilder sb = new StringBuilder();
                sb.append("Queued ").append(MusicManager.getFriendly(playlist.getTracks().get(0))).append(" for ").append(this.member.getEffectiveName());

                if (this.server.getPlaying() == null) {
                    sb.append("; up next!");
                } else if (this.server.getPlayer().getPlayingTrack() == null) {
                    // Something's gone wrong
                    sb.append("; up soon!");
                } else {
                    long time = 0;

                    for (final QueueItem item : this.server.getQueue()) {
                        time += item.getTrack().getDuration();
                    }

                    time += this.server.getPlayer().getPlayingTrack().getDuration() - this.server.getPlayer().getPlayingTrack().getPosition();

                    sb.append("; playing in ").append(MusicManager.getFriendlyTime(time)).append("!");
                }

                MusicPlayCommand.this.reply(this.message, sb.toString());
                this.server.queue(new QueueItem(playlist.getTracks().get(0), this.member.getUser().getId()));

                return;
            }

            if (MusicPlayCommand.this.manager.isDJ(this.message) || this.member.hasPermission(this.message.getTextChannel(), Permission.MANAGE_SERVER)) {
                MusicManager.getLogger().info("Queuing songs for DJ/admin...");
                int queued = 0;

                for (final AudioTrack track : playlist.getTracks()) {
                    if (!this.server.isQueued(track) && !this.server.isQueueFull() && (MusicPlayCommand.this.manager.isDJ(message) && track.getDuration() < MusicManager.DJ_TIME_LIMIT) || (!MusicPlayCommand.this.manager.isDJ(message) && track.getDuration() < MusicManager.TIME_LIMIT)) {
                        this.server.queue(new QueueItem(track, this.member.getUser().getId()));
                        queued++;
                    }
                }

                if (this.message.getGuild().getSelfMember().hasPermission(this.message.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                    this.message.delete().queue();
                }

                final StringBuilder sb = new StringBuilder();
                sb.append("Queued ");

                MusicPlayCommand.this.reply(this.message, sb.toString());

                if (queued < playlist.getTracks().size()) {
                    sb.append(queued).append("/").append(playlist.getTracks().size());
                } else {
                    sb.append(playlist.getTracks().size());
                }

                sb.append(" tracks for ").append(this.member.getEffectiveName());

                if (this.server.getPlaying() == null) {
                    sb.append("; up next!");
                } else if (this.server.getPlayer().getPlayingTrack() == null) {
                    // Something's gone wrong
                    sb.append("; up soon!");
                } else {
                    long time = 0;

                    for (final QueueItem item : this.server.getQueue()) {
                        time += item.getTrack().getDuration();
                    }

                    time += this.server.getPlayer().getPlayingTrack().getDuration() - this.server.getPlayer().getPlayingTrack().getPosition();

                    sb.append("; playing in ").append(MusicManager.getFriendlyTime(time)).append("!");
                }

                MusicPlayCommand.this.reply(this.message, sb.toString());

                MusicManager.getLogger().info("Queued " + queued + "/" + playlist.getTracks().size());
            } else {
                MusicManager.getLogger().fine("Trying to queue first from playlist for non-DJ...");

                if (this.server.isQueued(playlist.getTracks().get(0))) {
                    MusicManager.getLogger().fine("Song already queued.");
                    MusicPlayCommand.this.reply(this.message, "That song is already queued.");
                    this.server.prompt();
                    return;
                }

                if (this.server.isQueueFull()) {
                    MusicManager.getLogger().fine("Queue full");
                    MusicPlayCommand.this.reply(this.message, "There is no space left in the queue!");
                    this.server.prompt();
                    return;
                }

                if (MusicPlayCommand.this.manager.isDJ(message) && playlist.getTracks().get(0).getDuration() < MusicManager.DJ_TIME_LIMIT || !MusicPlayCommand.this.manager.isDJ(message) && playlist.getTracks().get(0).getDuration() > MusicManager.TIME_LIMIT) {
                    MusicManager.getLogger().fine("Song too long; " + playlist.getTracks().get(0).getDuration() + ">" + MusicManager.TIME_LIMIT + ".");
                    MusicPlayCommand.this.reply(this.message, "The song is too long to be queued.");
                    this.server.prompt();
                    return;
                }

                if (this.message.getGuild().getSelfMember().hasPermission(this.message.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                    this.message.delete().queue();
                }

                final StringBuilder sb = new StringBuilder();
                sb.append("Queued ").append(MusicManager.getFriendly(playlist.getTracks().get(0))).append(" for ").append(this.member.getEffectiveName());

                if (this.server.getPlaying() == null) {
                    sb.append("; up next!");
                } else if (this.server.getPlayer().getPlayingTrack() == null) {
                    // Something's gone wrong
                    sb.append("; up soon!");
                } else {
                    long time = 0;

                    for (final QueueItem item : this.server.getQueue()) {
                        time += item.getTrack().getDuration();
                    }

                    time += this.server.getPlayer().getPlayingTrack().getDuration() - this.server.getPlayer().getPlayingTrack().getPosition();

                    sb.append("; playing in ").append(MusicManager.getFriendlyTime(time)).append("!");
                }

                MusicPlayCommand.this.reply(this.message, sb.toString());
                this.server.queue(new QueueItem(playlist.getTracks().get(0), this.member.getUser().getId()));
                MusicManager.getLogger().info("Queued first from playlist for non-DJ");
            }
        }

        @Override
        public void trackLoaded(final AudioTrack track) {
            MusicManager.getLogger().fine("Loaded a track");

            if (this.server.isQueued(track)) {
                MusicManager.getLogger().fine("Song already queued.");
                MusicPlayCommand.this.reply(this.message, "That song is already queued.");
                this.server.prompt();
                return;
            }

            if (this.server.isQueueFull()) {
                MusicManager.getLogger().fine("Queue full");
                MusicPlayCommand.this.reply(this.message, "There is no space left in the queue!");
                this.server.prompt();
                return;
            }

            if (MusicPlayCommand.this.manager.isDJ(message) && track.getDuration() > MusicManager.DJ_TIME_LIMIT || !MusicPlayCommand.this.manager.isDJ(message) && track.getDuration() > MusicManager.TIME_LIMIT) {
                MusicManager.getLogger().fine("Song too long; " + track.getDuration() + ">" + MusicManager.TIME_LIMIT + ".");
                MusicPlayCommand.this.reply(this.message, "The song is too long to be queued.");
                this.server.prompt();
                return;
            }

            if (this.message.getGuild().getSelfMember().hasPermission(this.message.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                this.message.delete().queue();
            }

            final StringBuilder sb = new StringBuilder();
            sb.append("Queued ").append(MusicManager.getFriendly(track)).append(" for ").append(this.member.getEffectiveName());

            if (this.server.getPlaying() == null) {
                sb.append("; up next!");
            } else if (this.server.getPlayer().getPlayingTrack() == null) {
                // Something's gone wrong
                sb.append("; up soon!");
            } else {
                long time = 0;

                for (final QueueItem item : this.server.getQueue()) {
                    time += item.getTrack().getDuration();
                }

                time += this.server.getPlayer().getPlayingTrack().getDuration() - this.server.getPlayer().getPlayingTrack().getPosition();

                sb.append("; playing in ").append(MusicManager.getFriendlyTime(time)).append("!");
            }

            MusicPlayCommand.this.reply(this.message, sb.toString());
            this.server.queue(new QueueItem(track, this.member.getUser().getId()));
            MusicManager.getLogger().fine("Queued a song");
        }

    }

    private final MusicManager manager;

    public MusicPlayCommand(final Hilda hilda, final MusicManager manager) {
        super(hilda);

        this.manager = manager;

        this.setName("play");
        this.setAliases(Arrays.asList(new String[] { "search", "p" }));
        this.setDescription("Queues a sound file to be played. Accepts URLs or YouTube search queries.");
    }

    @Override
    public void execute(final Message message, final String[] args, final String label) {
        final Member member = message.getGuild().getMember(message.getAuthor());

        if (args.length == 0) {
            this.usage(message, "<url/query string>", label);
            return;
        }

        // User requirement logic
        if (!member.getVoiceState().inVoiceChannel()) {
            MusicManager.getLogger().fine("Rejected command because user not in voice channel");
            this.reply(message, "You must be in a voice channel to queue music.");
            return;
        }

        final MusicServer server = this.manager.hasServer(message.getGuild()) ? this.manager.getServer(message.getGuild()) : this.manager.createServer(message.getGuild());

        if (server.getChannel() == null) {
            server.setChannel(member.getVoiceState().getChannel()); // Join channel
        } else {
            if (server.getChannel() != member.getVoiceState().getChannel()) {
                MusicManager.getLogger().fine("Rejected command because user not in same voice channel");
                this.reply(message, "You must be in the same voice channel as me to queue music.");
                return;
            }
        }

        // URL logic
        if (args.length == 1 && args[0].toLowerCase().startsWith("http")) {
            MusicManager.getLogger().info("Attempting to load URL " + args[0]);
            message.getChannel().sendTyping().queue();
            this.manager.getAudioPlayerManager().loadItemOrdered(server.getPlayer(), args[0], new LoadResults(server, message));
            return;
        }

        // Search logic
        final String search = Util.combineSplit(0, args, " ");
        MusicManager.getLogger().info("Attempting to search YouTube for " + search);
        message.getChannel().sendTyping().queue();
        this.manager.getAudioPlayerManager().loadItemOrdered(server.getPlayer(), "ytsearch:" + search, new LoadResults(server, message, true));
    }

}
