package ch.jamiete.hilda.music.commands;

import java.util.Arrays;
import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.commands.ChannelSeniorCommand;
import ch.jamiete.hilda.music.MusicManager;

public class MusicBaseCommand extends ChannelSeniorCommand {

    public MusicBaseCommand(final Hilda hilda, final MusicManager manager) {
        super(hilda);

        this.setName("music");
        this.setAliases(Arrays.asList(new String[] { "m" }));
        this.setDescription("Manages the music player.");

        this.registerSubcommand(new MusicAdminCommand(hilda, this, manager));
        this.registerSubcommand(new MusicForceskipCommand(hilda, this, manager));
        this.registerSubcommand(new MusicNowPlayingCommand(hilda, this, manager));
        this.registerSubcommand(new MusicPlayCommand(hilda, this, manager));
        this.registerSubcommand(new MusicQueueCommand(hilda, this, manager));
        this.registerSubcommand(new MusicRemoveCommand(hilda, this, manager));
        this.registerSubcommand(new MusicResetCommand(hilda, this, manager));
        this.registerSubcommand(new MusicShuffleCommand(hilda, this, manager));
        this.registerSubcommand(new MusicSkipCommand(hilda, this, manager));
        this.registerSubcommand(new MusicVolumeCommand(hilda, this, manager));
    }

}
