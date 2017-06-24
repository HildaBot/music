package ch.jamiete.hilda.music;

import java.util.List;
import java.util.logging.Level;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.Util;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;

public class LoadResults implements AudioLoadResultHandler {
    private final MusicServer server;
    private final MusicManager manager;
    private final Message message;
    private final Member member;
    private final boolean search;

    public LoadResults(final MusicServer server, final MusicManager manager, final Message message) {
        this(server, manager, message, false);
    }

    public LoadResults(final MusicServer server, final MusicManager manager, final Message message, final boolean search) {
        this.server = server;
        this.manager = manager;
        this.message = message;
        this.member = message.getGuild().getMember(message.getAuthor());
        this.search = search;
    }

    @Override
    public void loadFailed(final FriendlyException e) {
        if (e.getMessage().startsWith("The uploader has not made this video available")) {
            this.reply("That track is geo-blocked and cannot be played.");
        } else if (e.getMessage().startsWith("This video contains content from")) {
            this.reply("That track has been restricted by the copyright holder and cannot be played.");
        } else if (e.getMessage().startsWith("This video is not available")) {
            this.reply("That track is not available to me and cannot be played.");
        } else {
            MusicManager.getLogger().log(Level.WARNING, "Couldn't load track", e);
            this.reply("I couldn't load that track: " + e.getMessage() + ".");
            Hilda.getLogger().log(Level.WARNING, "Couldn't load track", e);
        }

        this.server.prompt();
    }

    @Override
    public void noMatches() {
        MusicManager.getLogger().info("Failed to find anything for query " + this.message.getContent());
        this.reply("I couldn't find anything matching that query.");
        this.server.prompt();
    }

    @Override
    public void playlistLoaded(final AudioPlaylist playlist) {
        MusicManager.getLogger().fine("Loaded a playlist");

        if (this.search) {
            MusicManager.getLogger().fine("Playlist came from a search query!");
            this.tryLoadTrack(playlist.getTracks().get(0));
            return;
        }

        if (this.manager.isDJ(this.message) || this.member.hasPermission(this.message.getTextChannel(), Permission.MANAGE_SERVER)) {
            MusicManager.getLogger().info("Queuing songs for DJ/admin...");
            int queued = 0;

            for (final AudioTrack track : playlist.getTracks()) {
                if (!this.server.isQueued(track) && !this.server.isQueueFull() && this.manager.isDJ(this.message) && track.getDuration() < MusicManager.DJ_TIME_LIMIT || !this.manager.isDJ(this.message) && track.getDuration() < MusicManager.TIME_LIMIT) {
                    this.server.queue(new QueueItem(track, this.member.getUser().getId()));
                    queued++;
                }
            }

            if (this.message.getGuild().getSelfMember().hasPermission(this.message.getTextChannel(), Permission.MESSAGE_MANAGE)) {
                this.message.delete().queue();
            }

            final StringBuilder sb = new StringBuilder();
            sb.append("Queued ");

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
                sb.append("; playing in ").append(Util.getFriendlyTime(this.server.getDuration())).append("!");
            }

            this.reply(sb.toString());

            MusicManager.getLogger().info("Queued " + queued + "/" + playlist.getTracks().size());
        } else {
            MusicManager.getLogger().fine("Trying to queue first from playlist for non-DJ...");
            this.tryLoadTrack(playlist.getTracks().get(0));
        }
    }

    private void reply(final String message) {
        this.message.getChannel().sendMessage(message).queue();
    }

    @Override
    public void trackLoaded(final AudioTrack track) {
        MusicManager.getLogger().fine("Loaded a track");
        this.tryLoadTrack(track);
    }

    private void tryLoadTrack(final AudioTrack track) {
        if (this.server.isQueued(track)) {
            MusicManager.getLogger().fine("Song already queued.");
            this.reply("That song is already queued.");
            this.server.prompt();
            return;
        }

        if (this.server.isQueueFull()) {
            MusicManager.getLogger().fine("Queue full");
            this.reply("There is no space left in the queue!");
            this.server.prompt();
            return;
        }

        if (this.manager.isDJ(this.message) && track.getDuration() > MusicManager.DJ_TIME_LIMIT || !this.manager.isDJ(this.message) && track.getDuration() > MusicManager.TIME_LIMIT) {
            MusicManager.getLogger().fine("Song too long; " + track.getDuration() + ">" + MusicManager.TIME_LIMIT + ".");
            this.reply("The song is too long to be queued.");
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
            sb.append("; playing in ").append(Util.getFriendlyTime(this.server.getDuration())).append("!");
        }

        List<QueueItem> queue = this.server.getQueue();
        if (queue.size() > 0) {
            sb.append(" (Queue code " + (queue.size() + 1) + ")");
        }

        this.reply(sb.toString());
        this.server.queue(new QueueItem(track, this.member.getUser().getId()));
        MusicManager.getLogger().fine("Queued a song");
    }

}
