package ch.jamiete.hilda.music.commands;

import java.util.Arrays;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.commands.ChannelSeniorCommand;
import ch.jamiete.hilda.commands.ChannelSubCommand;
import ch.jamiete.hilda.music.MusicManager;
import ch.jamiete.hilda.music.MusicServer;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;

public class MusicSkipCommand extends ChannelSubCommand {
    private final MusicManager manager;

    public MusicSkipCommand(final Hilda hilda, final ChannelSeniorCommand senior, final MusicManager manager) {
        super(hilda, senior);

        this.manager = manager;

        this.setName("skip");
        this.setAliases(Arrays.asList(new String[] { "next" }));
        this.setDescription("Requests that the song be skipped.");
    }

    @Override
    public void execute(final Message message, final String[] args, final String label) {
        final Member member = message.getGuild().getMember(message.getAuthor());

        if (!member.getVoiceState().inVoiceChannel()) {
            MusicManager.getLogger().fine("Rejected command because user not in a voice channel");
            this.reply(message, "You must be in a voice channel to skip.");
            return;
        }

        if (member.getVoiceState().isDeafened()) {
            MusicManager.getLogger().fine("Rejected command because user deafened");
            this.reply(message, "You must undeafen to skip.");
            return;
        }

        final MusicServer server = this.manager.getServer(message.getGuild());

        if (server == null) {
            if (System.currentTimeMillis() - this.manager.getRecent(message.getGuild().getIdLong()) >= 60000) {
                this.reply(message, "There isn't anything playing.");
            }

            return;
        }

        if (member.getVoiceState().getChannel() != server.getChannel()) {
            MusicManager.getLogger().fine("Rejected command because user not in my voice channel");
            this.reply(message, "You must be in the same voice channel as me to skip.");
            return;
        }

        if (server != null && server.getPlayer().getPlayingTrack() == null && !server.getQueue().isEmpty()) {
            MusicManager.getLogger().info("The queue was stuck!");
            server.play(server.getQueue().get(0));
            this.reply(message, "Oops! Skipping...");
            return;
        }

        if (server == null || server != null && server.getPlayer().getPlayingTrack() == null) {
            MusicManager.getLogger().fine("Rejected command because no track playing");
            this.reply(message, "There isn't anything playing.");
            return;
        }

        if (server.getPlaying().getUserId().equals(member.getUser().getId())) {
            MusicManager.getLogger().info("Skipped song because user had requested it");
            this.reply(message, "Skipping...");
            server.getPlayer().stopTrack();
            return;
        }

        if (server.hasSkipped(member.getUser().getId())) {
            MusicManager.getLogger().fine("Rejected command because user already voted to skip");
            this.reply(message, "You've already voted to skip the current song.");
        } else {
            final StringBuilder sb = new StringBuilder();

            sb.append(member.getEffectiveName()).append(" has voted to skip the song!");
            server.addSkip(member.getUser().getId());

            if (server.shouldSkip()) {
                sb.append(" Skipping...");
            } else {
                final int needed = (int) Math.ceil((double) server.getUsers() / (double) 2);
                sb.append(" **").append(server.getSkips()).append("/").append(needed).append("**");
                MusicManager.getLogger().fine("Skips: " + server.getSkips() + "/" + needed);
            }

            this.reply(message, sb.toString());

            if (server.shouldSkip()) { // So that message sends before track changes
                MusicManager.getLogger().info("Skipping song...");
                server.getPlayer().stopTrack();
            }
        }
    }

}
