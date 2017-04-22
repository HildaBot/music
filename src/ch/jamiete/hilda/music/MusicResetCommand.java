package ch.jamiete.hilda.music;

import java.util.Arrays;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.commands.ChannelCommand;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;

public class MusicResetCommand extends ChannelCommand {
    private final MusicManager manager;

    public MusicResetCommand(final Hilda hilda, final MusicManager manager) {
        super(hilda);

        this.manager = manager;

        this.setName("reset");
        this.setAliases(Arrays.asList(new String[] { "clear", "disconnect", "restart" }));
        this.setDescription("Clears the music queue and disconnects.");
    }

    @Override
    public void execute(final Message message, final String[] args, final String label) {
        final Member member = message.getGuild().getMember(message.getAuthor());

        if (!member.hasPermission(message.getTextChannel(), Permission.MANAGE_SERVER) && !this.manager.isDJ(message)) {
            this.reply(message, "You must be a DJ to use this command.");
            return;
        }

        final MusicServer server = this.manager.getServer(message.getGuild());

        if (server == null) {
            if (message.getGuild().getAudioManager().isConnected()) {
                message.getGuild().getAudioManager().closeAudioConnection();
                this.reply(message, "Disconnected from the channel.");
            } else {
                this.reply(message, "There isn't anything playing.");
            }

            return;
        }

        server.shutdown();
        server.getPlayer().stopTrack();
        server.prompt();
        this.reply(message, "Music reset.");
    }

}
