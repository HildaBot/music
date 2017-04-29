package ch.jamiete.hilda.music;

import ch.jamiete.hilda.Hilda;
import ch.jamiete.hilda.music.commands.MusicBaseCommand;
import ch.jamiete.hilda.plugins.HildaPlugin;

public class MusicPlugin extends HildaPlugin {

    private MusicManager music;

    public MusicPlugin(Hilda hilda) {
        super(hilda);
    }

    @Override
    public void onEnable() {
        this.music = new MusicManager(this.getHilda(), this);
        this.getHilda().getCommandManager().registerChannelCommand(new MusicBaseCommand(this.getHilda(), this.music));
    }

    public MusicManager getMusicManager() {
        return music;
    }

}
