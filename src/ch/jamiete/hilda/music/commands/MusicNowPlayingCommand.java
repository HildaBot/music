package ch.jamiete.hilda.music.commands;

import java.util.Arrays;
import org.apache.commons.lang3.time.DurationFormatUtils;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.Util;
import ch.jamiete.hilda.commands.ChannelSeniorCommand;
import ch.jamiete.hilda.commands.ChannelSubCommand;
import ch.jamiete.hilda.music.MusicManager;
import ch.jamiete.hilda.music.MusicServer;
import ch.jamiete.hilda.music.QueueItem;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.MessageBuilder.Formatting;
import net.dv8tion.jda.core.entities.Message;

public class MusicNowPlayingCommand extends ChannelSubCommand {
    private final MusicManager manager;

    public MusicNowPlayingCommand(final Hilda hilda, final ChannelSeniorCommand senior, final MusicManager manager) {
        super(hilda, senior);

        this.manager = manager;

        this.setName("playing");
        this.setAliases(Arrays.asList(new String[] { "np", "nowplaying" }));
        this.setDescription("Lists information about the song currently playing.");
    }

    @Override
    public void execute(final Message message, final String[] args, final String label) {
        final MusicServer server = this.manager.getServer(message.getGuild());

        if (server == null || server != null && server.getPlaying() == null) {
            this.reply(message, "There isn't anything playing.");
            return;
        }

        final MessageBuilder mb = new MessageBuilder();
        final QueueItem playing = server.getPlaying();

        mb.append("Now playing ").append(Util.sanitise(MusicManager.getFriendly(playing.getTrack()))).append("\n");

        if (playing.getTrack().getInfo().length != 0) {
            mb.append("\n").append("Time: ", Formatting.BOLD);
            mb.append(DurationFormatUtils.formatDuration(playing.getTrack().getPosition(), "HH:mm:ss", true));
            mb.append("/");
            mb.append(DurationFormatUtils.formatDuration(playing.getTrack().getDuration(), "HH:mm:ss", true));
        }

        mb.append("\n");
        mb.append("Requestor: ", Formatting.BOLD).append(message.getGuild().getMemberById(playing.getUserId()).getEffectiveName());

        mb.append("\n");
        mb.append("Skip votes: ", Formatting.BOLD);
        final int needed = (int) Math.ceil((double) server.getUsers() / (double) 2);
        mb.append(server.getSkips()).append("/").append(needed);

        this.reply(message, mb.build());
    }

}
