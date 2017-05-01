package ch.jamiete.hilda.music.commands;

import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.commands.ChannelSeniorCommand;
import ch.jamiete.hilda.commands.ChannelSubCommand;
import ch.jamiete.hilda.music.MusicManager;
import ch.jamiete.hilda.music.MusicServer;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;

public class MusicShuffleCommand extends ChannelSubCommand {
    private final MusicManager manager;

    public MusicShuffleCommand(final Hilda hilda, final ChannelSeniorCommand senior, final MusicManager manager) {
        super(hilda, senior);

        this.manager = manager;

        this.setName("shuffle");
        this.setDescription("Shuffles the queue.");
    }

    @Override
    public void execute(final Message message, final String[] args, final String name) {
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

        server.shuffle();
        this.reply(message, "Shuffled queue!");
    }

}
