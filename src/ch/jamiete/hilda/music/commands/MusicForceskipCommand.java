package ch.jamiete.hilda.music.commands;

import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.commands.ChannelCommand;
import ch.jamiete.hilda.music.MusicManager;
import ch.jamiete.hilda.music.MusicServer;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;

public class MusicForceskipCommand extends ChannelCommand {
    private final MusicManager manager;

    public MusicForceskipCommand(final Hilda hilda, final MusicManager manager) {
        super(hilda);

        this.manager = manager;

        this.setName("forceskip");
        this.setDescription("Forces the song to be skipped.");
    }

    @Override
    public void execute(final Message message, final String[] args, final String label) {
        final Member member = message.getGuild().getMember(message.getAuthor());

        if (!member.hasPermission(Permission.MANAGE_SERVER) && !this.manager.isDJ(message)) {
            this.reply(message, "You must be a DJ to use this command.");
            return;
        }

        if (!member.getVoiceState().inVoiceChannel()) {
            this.reply(message, "You must be in a voice channel to skip.");
            return;
        }

        final MusicServer server = this.manager.getServer(message.getGuild());

        if (server == null) {
            this.reply(message, "There isn't anything playing.");
            return;
        }

        if (server.getPlayer().getPlayingTrack() == null) {
            MusicManager.getLogger().info("Queue was stuck!");
            server.play(server.getQueue().get(0));
            this.reply(message, "Oops! Skipping...");
        } else {
            this.reply(message, "Skipping...");
            server.getPlayer().stopTrack();
        }
    }

}
